package com.apylon26.fylo

import org.json.JSONArray
import org.json.JSONObject

data class FyloScreen(val signature: String, val name: String)
data class ScreenState(val screens: List<FyloScreen>, val nextSignature: Int,
    val assignments: Map<String, String> = emptyMap(), val assistant: String? = null)

object ScreenCodec {
    fun validate(state: ScreenState): ScreenState {
        require(state.nextSignature in 1..10000) { "Ungültiger Signaturzähler." }
        require(state.screens.map { it.signature }.distinct().size == state.screens.size) { "Doppelte Screen-Signatur." }
        state.screens.forEach {
            require(it.signature.matches(Regex("[0-9]{4}")) && it.signature.toInt() in 1 until state.nextSignature) { "Ungültige Screen-Signatur." }
            require(it.name.isNotBlank() && it.name.length <= 60 && it.name == it.name.trim()) { "Ungültige Screen-Bezeichnung." }
        }
        val signatures = state.screens.map { it.signature }.toSet()
        require(state.assignments.all { it.key.isNotBlank() && it.value in signatures }) { "Ungültige Modulzuordnung." }
        require(state.assistant == null || state.assistant.isNotBlank()) { "Ungültiger KI-Zugriff." }
        return state
    }
    fun encode(state: ScreenState): String {
        validate(state)
        return JSONObject().put("version", 1).put("nextSignature", state.nextSignature)
            .put("screens", JSONArray().apply { state.screens.forEach { put(JSONObject().put("signature", it.signature).put("name", it.name)) } })
            .put("assignments", JSONObject(state.assignments)).put("assistant", state.assistant ?: JSONObject.NULL).toString()
    }
    fun decode(text: String): ScreenState {
        val root = JSONObject(text)
        require(root.getInt("version") == 1) { "Unbekannte Screen-Datenversion." }
        val array = root.getJSONArray("screens")
        val screens = (0 until array.length()).map { index ->
            val row = array.getJSONObject(index)
            FyloScreen(row.getString("signature"), row.getString("name"))
        }
        val bindings = root.getJSONObject("assignments")
        val assignments = bindings.keys().asSequence().associateWith { bindings.getString(it) }
        return validate(ScreenState(screens, root.getInt("nextSignature"), assignments,
            if (root.isNull("assistant")) null else root.getString("assistant")))
    }
}

interface ScreenDisk { fun read(): String?; fun write(text: String) }

/** Serializes mutations and publishes new state only after durable writing succeeds. */
class ScreenRepository(private val disk: ScreenDisk) {
    private var state: ScreenState? = null
    @Synchronized fun read(): ScreenState {
        state?.let { return it }
        val stored = disk.read()
        val loaded = if (stored == null) ScreenState(listOf(FyloScreen("0001", "Start")), 2)
            else ScreenCodec.decode(stored)
        if (stored == null) disk.write(ScreenCodec.encode(loaded))
        state = loaded
        return loaded
    }
    private fun save(next: ScreenState): ScreenState {
        disk.write(ScreenCodec.encode(next))
        state = next
        return next
    }
    @Synchronized fun add(name: String): ScreenState {
        val previous = read()
        val trimmed = name.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 60) { "Bitte eine Bezeichnung mit 1 bis 60 Zeichen eingeben." }
        require(previous.nextSignature <= 9999) { "Alle vierstelligen Signaturen wurden bereits vergeben." }
        val screen = FyloScreen(previous.nextSignature.toString().padStart(4, '0'), trimmed)
        return save(previous.copy(screens = previous.screens + screen, nextSignature = previous.nextSignature + 1))
    }
    @Synchronized fun delete(signature: String): ScreenState {
        val previous = read()
        require(previous.screens.any { it.signature == signature }) { "Dieser Screen existiert nicht mehr." }
        require(signature !in previous.assignments.values) { "Zuerst alle Module von diesem Screen entfernen." }
        return save(previous.copy(screens = previous.screens.filterNot { it.signature == signature }))
    }
    @Synchronized fun assign(module: String, signature: String?): ScreenState {
        val previous = read()
        require(module.isNotBlank()) { "Ungültiges Modul." }
        require(signature == null || previous.screens.any { it.signature == signature }) { "Dieser Screen existiert nicht mehr." }
        return save(previous.copy(assignments = if (signature == null) previous.assignments - module
            else previous.assignments + (module to signature)))
    }
    @Synchronized fun assistant(module: String?): ScreenState = save(read().copy(assistant = module))
}
