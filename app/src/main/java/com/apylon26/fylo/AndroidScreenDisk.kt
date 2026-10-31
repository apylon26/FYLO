package com.apylon26.fylo

import android.content.Context
import android.util.AtomicFile
import java.io.File

/** One repository/lock survives Activity recreation within the host's single process. */
object ScreenStores {
    @Volatile private var instance: ScreenRepository? = null
    fun get(context: Context): ScreenRepository = instance ?: synchronized(this) {
        instance ?: ScreenRepository(AndroidScreenDisk(File(context.filesDir, "screens-v1.json"))).also { instance = it }
    }
}

class AndroidScreenDisk(file: File) : ScreenDisk {
    private val atomic = AtomicFile(file)
    override fun read(): String? {
        if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) return null
        return atomic.readFully().toString(Charsets.UTF_8)
    }
    override fun write(text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val output = atomic.startWrite()
        try {
            output.write(bytes)
            atomic.finishWrite(output)
            check(atomic.readFully().contentEquals(bytes)) { "Die Screen-Daten konnten nicht vollständig gespeichert werden." }
        } catch (error: Exception) {
            atomic.failWrite(output)
            throw error
        }
    }
}
