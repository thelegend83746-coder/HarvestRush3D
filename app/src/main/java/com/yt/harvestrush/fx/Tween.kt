package com.yt.harvestrush.fx

import com.badlogic.gdx.math.MathUtils

/**
 * Lightweight Tween runner for numbers, arc movements, and UI count-up effects.
 */
class FloatTween(
    val start: Float,
    val target: Float,
    val duration: Float,
    val ease: (Float) -> Float = Easing::easeOutQuad,
    val onUpdate: (Float) -> Unit,
    val onComplete: (() -> Unit)? = null
) {
    private var elapsed: Float = 0f
    var isDone: Boolean = false
        private set

    fun update(delta: Float) {
        if (isDone) return
        elapsed += delta
        val t = MathUtils.clamp(elapsed / duration, 0f, 1f)
        val easedT = ease(t)
        val value = start + (target - start) * easedT
        onUpdate(value)

        if (t >= 1f) {
            isDone = true
            onComplete?.invoke()
        }
    }
}

/**
 * Parabolic 2D Arc Tween for flying resource icons (flying from field to HUD counters).
 */
class ArcTween(
    val startX: Float, val startY: Float,
    val endX: Float, val endY: Float,
    val peakHeight: Float,
    val duration: Float,
    val onUpdate: (x: Float, y: Float, scale: Float) -> Unit,
    val onComplete: (() -> Unit)? = null
) {
    private var elapsed: Float = 0f
    var isDone: Boolean = false
        private set

    fun update(delta: Float) {
        if (isDone) return
        elapsed += delta
        val t = MathUtils.clamp(elapsed / duration, 0f, 1f)

        // Linear X/Y path
        val curX = MathUtils.lerp(startX, endX, t)
        val linearY = MathUtils.lerp(startY, endY, t)

        // Parabolic arc arcHeight = 4 * peak * t * (1 - t)
        val arc = 4f * peakHeight * t * (1f - t)
        val curY = linearY + arc

        // Scale shrinks slightly as it enters the counter
        val scale = 1.0f - t * 0.35f

        onUpdate(curX, curY, scale)

        if (t >= 1f) {
            isDone = true
            onComplete?.invoke()
        }
    }
}
