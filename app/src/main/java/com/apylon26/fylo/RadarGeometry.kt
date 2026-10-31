package com.apylon26.fylo

import kotlin.math.cos
import kotlin.math.sin

data class RadarPoint(val x: Float, val y: Float)

object RadarGeometry {
    fun axis(index: Int, fraction: Float = 1f): RadarPoint {
        val angle = Math.toRadians((index * 120.0) - 90.0)
        return RadarPoint(cos(angle).toFloat() * fraction, sin(angle).toFloat() * fraction)
    }
    // Interpolate around the triangular boundary, then scale towards the centre.
    // Unlike a circular orbit this cannot cross the three-sided web's edges.
    fun orbit(degrees: Float, fraction: Float): RadarPoint {
        val turn = ((degrees % 360f) + 360f) % 360f / 120f
        val index = turn.toInt()
        val blend = turn - index
        val a = axis(index)
        val b = axis((index + 1) % 3)
        val scale = fraction.coerceIn(0f, 1f)
        return RadarPoint((a.x + (b.x - a.x) * blend) * scale,
            (a.y + (b.y - a.y) * blend) * scale)
    }
}
