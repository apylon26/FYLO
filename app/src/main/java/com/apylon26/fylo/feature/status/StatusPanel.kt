package com.apylon26.fylo.feature.status

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.view.View
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun StatusPanel(active: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    AndroidView(factory = { StatusView(it) }, modifier = modifier.onGloballyPositioned {
        val bounds = it.boundsInWindow()
        visible = bounds.width > 0 && bounds.height > 0
    }, update = { it.setActive(active && visible) })
}

private class StatusView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private val fontScale = resources.configuration.fontScale
    private val blue = Color.rgb(55, 169, 255)
    private val orange = Color.rgb(255, 139, 36)
    private val battery = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val samples = CpuSamples(Runtime.getRuntime().availableProcessors())
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var time = "--:--:--"
    private var batteryLevel: Int? = null
    private var active = false
    private var resumed = false
    private var owner: LifecycleOwner? = null
    private val observer = LifecycleEventObserver { source, _ ->
        resumed = source.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        schedule()
    }
    private val tick = object : Runnable {
        override fun run() {
            if (!shouldRun()) return
            formatter.timeZone = TimeZone.getDefault()
            time = formatter.format(Date())
            batteryLevel = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
            samples.sample(SystemClock.elapsedRealtime(), Process.getElapsedCpuTime())
            contentDescription = "Uhrzeit $time. Akku ${batteryLevel ?: "unbekannt"} Prozent. CPU FYLO: " +
                "Minimum ${percentage(samples.minimum)}, aktuell ${percentage(samples.current)}, Maximum ${percentage(samples.maximum)}."
            invalidate()
            postDelayed(this, 1000L)
        }
    }
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }

    fun setActive(value: Boolean) {
        if (active != value) { active = value; schedule() }
    }
    private fun shouldRun() = active && resumed && isAttachedToWindow && isShown &&
        windowVisibility == VISIBLE && power.isInteractive
    private fun schedule() {
        removeCallbacks(tick)
        samples.resetBaseline() // Never average a background/screen-off interval into a visible sample.
        if (shouldRun()) post(tick)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        owner = findViewTreeLifecycleOwner()
        resumed = owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
        owner?.lifecycle?.addObserver(observer)
        schedule()
    }
    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        owner?.lifecycle?.removeObserver(observer)
        owner = null
        resumed = false
        super.onDetachedFromWindow()
    }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (owner != null) schedule()
    }
    private fun percentage(value: Float?) = value?.let { String.format(Locale.GERMANY, "%.1f %%", it) } ?: "—"

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 11f * density
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(12, 28, 49)
        canvas.drawRoundRect(1f, 1f, w - 1f, h - 1f, 18f * density, 18f * density, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        paint.color = 0x8852AEFF.toInt()
        canvas.drawRoundRect(1f, 1f, w - 1f, h - 1f, 18f * density, 18f * density, paint)
        paint.style = Paint.Style.FILL
        fun text(value: String, x: Float, y: Float, sp: Float, color: Int, bold: Boolean = false) {
            paint.color = color
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            paint.textSize = sp * density * fontScale
            // Fit long translated values and large system fonts to this compact panel.
            val available = (w - pad - x).coerceAtLeast(1f)
            val measured = paint.measureText(value)
            if (measured > available) paint.textSize *= available / measured
            canvas.drawText(value, x, y * h, paint)
        }
        text("UHRZEIT", pad, .105f, 10f, blue)
        text(time, pad, .25f, 23f, Color.WHITE, true)
        text("AKKU", pad, .365f, 10f, blue)
        text(batteryLevel?.let { "$it %" } ?: "—", pad, .49f, 21f, Color.WHITE, true)
        text("CPU FYLO", pad, .61f, 11f, blue, true)
        val left = pad + 3f * density
        val right = w - pad - 3f * density
        val y = h * .71f
        val trackHeight = 4f * density
        paint.color = 0xFF263B55.toInt()
        canvas.drawRect(left, y, right, y + trackHeight, paint)
        val current = samples.current
        if (current != null) {
            paint.color = 0x5552AEFF
            val low = left + (right - left) * samples.minimum!! / 100f
            val high = left + (right - left) * samples.maximum!! / 100f
            canvas.drawRect(low, y, high, y + trackHeight, paint)
            // All three markers have exactly the same rectangular dimensions.
            listOf(samples.minimum!! to blue, samples.maximum!! to blue, current to orange).forEach { (value, color) ->
                val x = left + (right - left) * value / 100f
                paint.color = color
                canvas.drawRect(x - 2.5f * density, y - 4f * density, x + 2.5f * density, y + 8f * density, paint)
            }
        }
        val col = (w - 2 * pad) / 3f
        listOf("MIN" to samples.minimum, "AKT" to samples.current, "MAX" to samples.maximum).forEachIndexed { index, (label, value) ->
            val x = pad + col * index
            text(label, x, .855f, 8f, if (index == 1) orange else blue)
            paint.textSize = 9f * density * fontScale
            paint.color = Color.WHITE
            paint.typeface = Typeface.DEFAULT_BOLD
            val v = percentage(value)
            val measured = paint.measureText(v)
            if (measured > col - density) paint.textSize *= (col - density) / measured
            canvas.drawText(v, x, h * .95f, paint)
        }
    }
}
