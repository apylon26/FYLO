package com.apylon26.fylo

data class CpuTicks(val total: Long, val idle: Long)

object CpuMath {
    fun parse(line: String): CpuTicks? {
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.firstOrNull() != "cpu" || parts.size < 5) return null
        // guest and guest_nice are already counted in user/nice; do not double-count.
        val ticks = parts.drop(1).take(8).map { it.toLongOrNull() ?: return null }
        if (ticks.any { it < 0 }) return null
        return CpuTicks(ticks.sum(), ticks[3] + ticks.getOrElse(4) { 0 })
    }
    fun system(before: CpuTicks, after: CpuTicks): Float? {
        val total = after.total - before.total
        val idle = after.idle - before.idle
        if (total <= 0 || idle < 0 || idle > total) return null
        return (100f * (total - idle) / total).coerceIn(0f, 100f)
    }
    fun process(cpuDelta: Long, wallDelta: Long, cores: Int): Float? {
        if (cpuDelta < 0 || wallDelta <= 0 || cores <= 0) return null
        return (100f * cpuDelta / wallDelta / cores).coerceIn(0f, 100f)
    }
}

data class CpuStats(val current: Float? = null, val minimum: Float? = null, val maximum: Float? = null) {
    fun add(value: Float?): CpuStats {
        if (value == null || !value.isFinite() || value !in 0f..100f) return this
        return CpuStats(value, minOf(minimum ?: value, value), maxOf(maximum ?: value, value))
    }
}
