package com.apylon26.fylo

import org.junit.Assert.*
import org.junit.Test

class CpuTest {
    @Test fun linuxGuestTimeIsNotCountedTwice() {
        assertEquals(CpuTicks(110, 65), CpuMath.parse("cpu 10 5 20 60 5 2 3 5 10 5"))
    }
    @Test fun rejectsMalformedAndCounterResets() {
        assertNull(CpuMath.parse("cpu0 1 2 3 4"))
        assertNull(CpuMath.parse("cpu 1 2 bad 4"))
        assertNull(CpuMath.system(CpuTicks(100, 50), CpuTicks(90, 40)))
        assertNull(CpuMath.system(CpuTicks(100, 50), CpuTicks(100, 50)))
        assertNull(CpuMath.system(CpuTicks(100, 50), CpuTicks(110, 70)))
    }
    @Test fun computesBusyPercent() {
        assertEquals(40f, CpuMath.system(CpuTicks(100, 50), CpuTicks(200, 110))!!, 0.001f)
        assertEquals(10f, CpuMath.process(800, 1000, 8)!!, 0.001f)
        assertNull(CpuMath.process(0, 0, 8))
    }
    @Test fun extremaStartWithFirstRealSampleAndIgnoreUnavailableSamples() {
        var stats = CpuStats()
        assertNull(stats.minimum)
        stats = stats.add(23f).add(null).add(45f).add(12f).add(Float.NaN).add(-1f)
        assertEquals(12f, stats.current!!, 0f)
        assertEquals(12f, stats.minimum!!, 0f)
        assertEquals(45f, stats.maximum!!, 0f)
    }
}
