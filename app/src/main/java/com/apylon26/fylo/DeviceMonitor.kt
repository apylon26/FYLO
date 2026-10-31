package com.apylon26.fylo

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.delay

data class DeviceReading(val battery: Int? = null, val charging: Boolean = false)

class DeviceMonitor(private val context: Context) {
    suspend fun observe(update: (DeviceReading) -> Unit) {
        while (true) {
            val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val charge = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val percentage = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else null
            update(DeviceReading(percentage, charge == BatteryManager.BATTERY_STATUS_CHARGING ||
                charge == BatteryManager.BATTERY_STATUS_FULL))
            delay(1000)
        }
    }
}
