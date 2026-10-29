package com.yt.harvestrush.fx

import com.badlogic.gdx.math.MathUtils

/**
 * Standard easing functions for juice, UI transitions, and squash-stretch animations.
 */
object Easing {

    fun linear(t: Float): Float = t

    fun easeOutQuad(t: Float): Float = 1f - (1f - t) * (1f - t)

    fun easeInOutQuad(t: Float): Float =
        if (t < 0.5f) 2f * t * t else 1f - Math.pow((-2f * t + 2f).toDouble(), 2.0).toFloat() / 2f

    fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        return 1f + c3 * Math.pow((t - 1f).toDouble(), 3.0).toFloat() + c1 * Math.pow((t - 1f).toDouble(), 2.0).toFloat()
    }

    fun easeOutBounce(t: Float): Float {
        var n1 = 7.5625f
        var d1 = 2.75f

        return when {
            t < 1f / d1 -> n1 * t * t
            t < 2f / d1 -> {
                val t2 = t - 1.5f / d1
                n1 * t2 * t2 + 0.75f
            }
            t < 2.5f / d1 -> {
                val t2 = t - 2.25f / d1
                n1 * t2 * t2 + 0.9375f
            }
            else -> {
                val t2 = t - 2.625f / d1
                n1 * t2 * t2 + 0.984375f
            }
        }
    }

    fun easeOutElastic(t: Float): Float {
        val c4 = (2f * MathUtils.PI) / 3f
        return if (t == 0f) 0f else if (t == 1f) 1f else
            Math.pow(2.0, (-10f * t).toDouble()).toFloat() * MathUtils.sin((t * 10f - 0.75f) * c4) + 1f
    }
}
