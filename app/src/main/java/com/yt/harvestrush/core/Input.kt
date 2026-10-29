package com.yt.harvestrush.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.yt.harvestrush.data.PlayerStore

/**
 * Arcade touch controller supporting:
 * 1. Floating Virtual Joystick (drag anywhere, drag direction = heading)
 * 2. Tap-left / Tap-right steering mode
 */
class GameInput(private val playerStore: PlayerStore) : InputAdapter() {

    // Virtual Joystick coordinates
    val touchOrigin = Vector2()
    val touchCurrent = Vector2()
    val dragVector = Vector2()

    var isTouching: Boolean = false
        private set
    var targetHeadingRad: Float = 0f
        private set
    var hasActiveSteering: Boolean = false
        private set

    // Tap-left / Tap-right states
    private var tapTurnDirection: Float = 0f

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer != 0) return false
        isTouching = true

        if (playerStore.tapSteering) {
            val halfW = Gdx.graphics.width * 0.5f
            tapTurnDirection = if (screenX < halfW) 1.0f else -1.0f // Left or Right turn
            hasActiveSteering = true
        } else {
            touchOrigin.set(screenX.toFloat(), screenY.toFloat())
            touchCurrent.set(screenX.toFloat(), screenY.toFloat())
            dragVector.setZero()
            hasActiveSteering = false
        }
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer != 0) return false
        if (playerStore.tapSteering) {
            val halfW = Gdx.graphics.width * 0.5f
            tapTurnDirection = if (screenX < halfW) 1.0f else -1.0f
            hasActiveSteering = true
            return true
        }

        touchCurrent.set(screenX.toFloat(), screenY.toFloat())
        dragVector.set(touchCurrent).sub(touchOrigin)

        val deadZone = 12f
        if (dragVector.len2() > deadZone * deadZone) {
            hasActiveSteering = true
            // Convert screen coordinates (Y down) to 3D world heading (XZ plane)
            // Screen DX = World X, Screen -DY = World -Z
            targetHeadingRad = MathUtils.atan2(-dragVector.y, dragVector.x)
        }
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer != 0) return false
        isTouching = false
        hasActiveSteering = false
        tapTurnDirection = 0f
        return true
    }

    fun getTapTurnRate(): Float = tapTurnDirection
}
