package com.apylon26.fylo.feature.orb

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.PowerManager
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlin.math.cos
import kotlin.math.sin

/** Small Canvas scene: no textures, blur passes, physics engine or background service. */
@Composable
fun FyloOrb(active: Boolean, modifier: Modifier = Modifier) {
    var inViewport by remember { mutableStateOf(false) }
    AndroidView(
        factory = { OrbView(it) },
        modifier = modifier.onGloballyPositioned {
            val visible = it.boundsInWindow()
            inViewport = visible.width > 0f && visible.height > 0f
        },
        update = { it.setSceneActive(active && inViewport) }
    )
}

private class OrbView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blue = Color.rgb(36, 165, 255)
    private val orange = Color.rgb(255, 139, 36)
    private val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var sceneActive = false
    private var resumed = false
    private var touched = false
    private var angle = 0.0
    private var lastFrame = 0L
    private var owner: androidx.lifecycle.LifecycleOwner? = null
    private val observer = LifecycleEventObserver { source, _ ->
        resumed = source.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (!resumed) touched = false
        schedule()
    }
    private val frame = object : Runnable {
        override fun run() {
            if (!shouldAnimate()) { lastFrame = 0L; return }
            val now = SystemClock.uptimeMillis()
            if (lastFrame != 0L) angle = (angle + (now - lastFrame).coerceAtMost(100L) *
                if (touched) 0.0014 else 0.00032) % (Math.PI * 2)
            lastFrame = now
            invalidate()
            postDelayed(this, 34L) // At most about 30 updates per second.
        }
    }

    init {
        contentDescription = "FYLO-Kugel. Gedrückt halten für schnellere Bewegung und helleres Licht."
        isClickable = true
        isFocusable = true
    }

    fun setSceneActive(active: Boolean) {
        if (sceneActive != active) {
            sceneActive = active
            if (!active) touched = false
            schedule()
        }
    }

    private fun shouldAnimate() = sceneActive && resumed && isAttachedToWindow &&
        windowVisibility == VISIBLE && isShown && power.isInteractive && ValueAnimator.areAnimatorsEnabled()

    private fun schedule() {
        removeCallbacks(frame)
        lastFrame = 0L
        if (shouldAnimate()) post(frame)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        owner = findViewTreeLifecycleOwner()
        resumed = owner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
        owner?.lifecycle?.addObserver(observer)
        schedule()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(frame)
        owner?.lifecycle?.removeObserver(observer)
        owner = null
        resumed = false
        touched = false
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        // The framework may call this during View construction, before field initialization.
        if (owner != null) schedule()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { touched = true; invalidate(); return true }
            MotionEvent.ACTION_UP -> { touched = false; invalidate(); performClick(); return true }
            MotionEvent.ACTION_CANCEL -> { touched = false; invalidate(); return true }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    private fun sphere(canvas: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(x - radius * .3f, y - radius * .4f, radius * 1.5f,
            intArrayOf(Color.WHITE, color, Color.rgb(5, 18, 40)), floatArrayOf(0f, .28f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(x, y, radius, paint)
        paint.shader = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width / 3.5f, height / 2.7f)
        if (r <= 0f) return
        // A fixed gradient halo provides light without an expensive blur.
        paint.shader = RadialGradient(cx, cy, r * 1.5f,
            intArrayOf(if (touched) 0x6630AAFF else 0x3330AAFF, Color.TRANSPARENT), null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 1.5f, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = resources.displayMetrics.density
        paint.color = 0x5546ACFF
        canvas.drawOval(cx - r * 1.55f, cy - r * .58f, cx + r * 1.55f, cy + r * .58f, paint)
        paint.style = Paint.Style.FILL
        // Depth-sort satellites so the core hides those passing behind it.
        fun satellites(front: Boolean) {
            for (i in 0..3) {
                val theta = angle + i * Math.PI / 2
                val depth = sin(theta)
                if ((depth >= 0) == front) {
                    val x = cx + cos(theta).toFloat() * r * 1.55f
                    val y = cy + depth.toFloat() * r * .58f
                    sphere(canvas, x, y, r * (.075f + (depth.toFloat() + 1f) * .025f), if (i % 2 == 0) orange else blue)
                }
            }
        }
        satellites(false)
        sphere(canvas, cx, cy, r, blue)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * .035f
        paint.color = if (touched) Color.rgb(255, 184, 87) else orange
        canvas.drawArc(cx - r * .9f, cy - r * .9f, cx + r * .9f, cy + r * .9f, -75f, 105f, false, paint)
        paint.color = blue
        canvas.drawCircle(cx, cy, r * .31f, paint)
        paint.style = Paint.Style.FILL
        satellites(true)
    }
}
