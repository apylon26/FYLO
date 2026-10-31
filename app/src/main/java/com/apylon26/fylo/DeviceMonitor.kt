package com.apylon26.fylo

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Process
import android.os.SystemClock
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class DeviceReading(val battery: Int? = null, val charging: Boolean = false,
    val cpu: CpuStats = CpuStats(), val cpuSource: String = "Messung läuft …", val cpuAvailable: Boolean = false)

class DeviceMonitor(private val context: Context) {
    private var stats = CpuStats()
    private var source: String? = null
    private fun systemTicks() = runCatching {
        File("/proc/stat").bufferedReader().use { CpuMath.parse(it.readLine() ?: "") }
    }.getOrNull()

    suspend fun observe(update: (DeviceReading) -> Unit) {
        var previousSystem = withContext(Dispatchers.IO) { systemTicks() }
        val useSystem = previousSystem != null
        val nextSource = if (useSystem) "CPU · Gerät" else "CPU · FYLO"
        if (source != nextSource) stats = CpuStats()
        source = nextSource
        var previousCpu = Process.getElapsedCpuTime()
        var previousWall = SystemClock.elapsedRealtime()
        while (true) {
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val charge = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val percentage = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else null
            update(DeviceReading(percentage, charge == BatteryManager.BATTERY_STATUS_CHARGING ||
                charge == BatteryManager.BATTERY_STATUS_FULL, stats, nextSource, stats.current != null))
            delay(1000)
            val wall = SystemClock.elapsedRealtime()
            val cpu = Process.getElapsedCpuTime()
            val measured = if (useSystem) {
                val next = withContext(Dispatchers.IO) { systemTicks() }
                val value = if (previousSystem != null && next != null) CpuMath.system(previousSystem!!, next) else null
                previousSystem = next
                value
            } else CpuMath.process(cpu - previousCpu, wall - previousWall, Runtime.getRuntime().availableProcessors())
            stats = stats.add(measured)
            // A temporarily unavailable sample must never masquerade as a live measurement.
            if (measured == null) stats = stats.copy(current = null)
            previousCpu = cpu
            previousWall = wall
        }
    }
}
