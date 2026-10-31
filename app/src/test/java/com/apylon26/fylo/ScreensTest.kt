package com.apylon26.fylo

import org.junit.Assert.*
import org.junit.Test

class ScreensTest {
    private class Disk : ScreenDisk {
        var text: String? = null
        var fail = false
        override fun read() = text
        override fun write(text: String) { check(!fail); this.text = text }
    }
    @Test fun signaturesAreUniqueAcrossDeletionAndRestarts() {
        val disk = Disk()
        val first = ScreenRepository(disk)
        first.add("Arbeit")
        first.delete("0002")
        val next = ScreenRepository(disk).add("Privat")
        assertEquals(listOf("0001", "0003"), next.screens.map { it.signature })
        assertEquals(4, next.nextSignature)
    }
    @Test fun occupiedScreenCannotBeDeletedEvenWhenModuleIsNotDiscovered() {
        val repo = ScreenRepository(Disk())
        repo.assign("example/module", "0001")
        assertThrows(IllegalArgumentException::class.java) { repo.delete("0001") }
        repo.assign("example/module", null)
        assertTrue(repo.delete("0001").screens.isEmpty())
    }
    @Test fun persistenceRoundTripKeepsNamesAssignmentsAndAssistant() {
        val disk = Disk()
        val repo = ScreenRepository(disk)
        repo.add("  Büro / Außendienst  ")
        repo.assign("example/module", "0002")
        val expected = repo.assistant("example/assistant")
        assertEquals(expected, ScreenRepository(disk).read())
        assertEquals("Büro / Außendienst", expected.screens.last().name)
    }
    @Test fun exhaustionAndInvalidNamesDoNotOverwriteData() {
        val disk = Disk().apply { text = ScreenCodec.encode(ScreenState(emptyList(), 10000)) }
        val repo = ScreenRepository(disk)
        val before = disk.text
        assertThrows(IllegalArgumentException::class.java) { repo.add("Noch einer") }
        assertThrows(IllegalArgumentException::class.java) { repo.add("  ") }
        assertEquals(before, disk.text)
    }
    @Test fun duplicatesAndDanglingAssignmentsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ScreenCodec.validate(ScreenState(listOf(FyloScreen("0001", "A"), FyloScreen("0001", "B")), 2))
        }
        val repo = ScreenRepository(Disk())
        assertThrows(IllegalArgumentException::class.java) { repo.assign("example/module", "9999") }
    }
    @Test fun failedSaveKeepsPreviousStateAndCorruptDataIsNeverReset() {
        val disk = Disk()
        val repo = ScreenRepository(disk)
        val before = repo.read()
        disk.fail = true
        assertThrows(IllegalStateException::class.java) { repo.add("Büro") }
        assertEquals(before, repo.read())
        val broken = Disk().apply { text = "broken" }
        assertThrows(Exception::class.java) { ScreenRepository(broken).read() }
        assertEquals("broken", broken.text)
    }
    @Test fun deletingLastScreenDoesNotRecreateItAfterRestart() {
        val disk = Disk()
        ScreenRepository(disk).delete("0001")
        assertTrue(ScreenRepository(disk).read().screens.isEmpty())
        assertEquals("0002", ScreenRepository(disk).add("Neu").screens.single().signature)
    }
    @Test fun concurrentCreationCannotAllocateDuplicateSignatures() {
        val repo = ScreenRepository(Disk())
        val workers = java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val signatures = workers.invokeAll((1..100).map { index ->
                java.util.concurrent.Callable { repo.add("Screen $index").screens.last().signature }
            }).map { it.get() }
            assertEquals(100, signatures.toSet().size)
            assertEquals(102, repo.read().nextSignature)
        } finally { workers.shutdownNow() }
    }
}
