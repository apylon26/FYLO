package com.apylon26.fylo.feature.status

import org.junit.Assert.*
import org.junit.Test

class CpuSamplesTest {
    @Test fun measuresCpuAcrossCoresAndTracksExtrema() {
        val s = CpuSamples(8)
        s.sample(1000, 100)
        assertNull(s.current)
        s.sample(2000, 900)
        assertEquals(10f, s.current!!, .001f)
        s.sample(3000, 1300)
        assertEquals(5f, s.minimum!!, .001f)
        assertEquals(10f, s.maximum!!, .001f)
        s.sample(4000, 2900)
        assertEquals(20f, s.current!!, .001f)
        assertEquals(5f, s.minimum!!, .001f)
        assertEquals(20f, s.maximum!!, .001f)
    }
    @Test fun resumingExcludesBackgroundIntervalAndRetainsExtrema() {
        val s = CpuSamples(1)
        s.sample(0, 0); s.sample(1000, 100)
        s.resetBaseline()
        s.sample(100000, 5000)
        assertEquals(10f, s.current!!, .001f)
        s.sample(101000, 5000)
        assertEquals(0f, s.current!!, .001f)
        assertEquals(10f, s.maximum!!, .001f)
    }
    @Test fun rejectsInvalidDeltasAndClampsCounterOvershoot() {
        val s = CpuSamples(0)
        s.sample(100, 100); s.sample(100, 200)
        assertNull(s.current)
        s.sample(200, 10)
        assertNull(s.current)
        s.sample(300, 1000)
        assertEquals(100f, s.current!!, .001f)
    }
}
