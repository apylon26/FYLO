package com.apylon26.fylo

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class RadarGeometryTest {
    @Test fun movingPointAlwaysStaysInsideTheThreeSidedWeb() {
        for (angle in -720..720) for (level in listOf(0f, 0.25f, 0.5f, 1f, 2f)) {
            val p = RadarGeometry.orbit(angle.toFloat(), level)
            assertTrue(p.y <= 0.50001f)
            assertTrue(p.y >= sqrt(3f) * p.x - 1.00001f)
            assertTrue(p.y >= -sqrt(3f) * p.x - 1.00001f)
        }
    }
    @Test fun noLoadStaysAtCentreAndRotationWrapsContinuously() {
        assertEquals(RadarPoint(0f, -0f), RadarGeometry.orbit(0f, 0f))
        val first = RadarGeometry.orbit(0f, 0.7f)
        val last = RadarGeometry.orbit(360f, 0.7f)
        assertEquals(first.x, last.x, 0.00001f)
        assertEquals(first.y, last.y, 0.00001f)
    }
}
