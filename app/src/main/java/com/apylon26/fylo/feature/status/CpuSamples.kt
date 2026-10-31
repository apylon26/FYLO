package com.apylon26.fylo.feature.status

/** FYLO CPU time as a percentage of total logical CPU capacity, not system CPU load. */
class CpuSamples(private val cores: Int) {
    var current: Float? = null
        private set
    var minimum: Float? = null
        private set
    var maximum: Float? = null
        private set
    private var previousWall: Long? = null
    private var previousCpu: Long? = null

    fun resetBaseline() { previousWall = null; previousCpu = null }

    fun sample(wallMillis: Long, cpuMillis: Long) {
        val wall = previousWall
        val cpu = previousCpu
        previousWall = wallMillis
        previousCpu = cpuMillis
        if (wall == null || cpu == null || wallMillis <= wall || cpuMillis < cpu) return
        val value = ((cpuMillis - cpu).toDouble() * 100.0 /
            ((wallMillis - wall).toDouble() * cores.coerceAtLeast(1))).toFloat().coerceIn(0f, 100f)
        current = value
        minimum = minimum?.let { minOf(it, value) } ?: value
        maximum = maximum?.let { maxOf(it, value) } ?: value
    }
}
