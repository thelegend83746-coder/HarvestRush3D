package com.yt.harvestrush.fx

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3

/**
 * Screen shake trauma system for tactile cutting feedback.
 */
class ScreenShake {
    private var trauma: Float = 0f
    val offset = Vector3()

    fun addTrauma(amount: Float) {
        trauma = MathUtils.clamp(trauma + amount, 0f, 1f)
    }

    fun update(delta: Float) {
        if (trauma > 0f) {
            trauma = maxOf(0f, trauma - delta * 2.5f)
            val shake = trauma * trauma // Non-linear falloff
            offset.x = MathUtils.random(-1f, 1f) * shake * 0.45f
            offset.y = MathUtils.random(-1f, 1f) * shake * 0.35f
            offset.z = MathUtils.random(-1f, 1f) * shake * 0.45f
        } else {
            offset.setZero()
        }
    }
}
