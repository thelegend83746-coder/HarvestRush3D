package com.yt.harvestrush.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.yt.harvestrush.config.GameConfig

/**
 * High 3rd-person arcade camera rig with positional damping,
 * look-ahead smoothing, dynamic zoom for growing wagon chains, and shake.
 */
class CameraRig {
    val camera: PerspectiveCamera = PerspectiveCamera(
        GameConfig.CAMERA_FOV,
        Gdx.graphics.width.toFloat(),
        Gdx.graphics.height.toFloat()
    )

    private val currentTarget = Vector3()
    private val desiredTarget = Vector3()
    private val desiredCamPos = Vector3()
    val shakeOffset = Vector3()

    private var currentZoom = 0.0f
    private var targetZoom = 0.0f

    init {
        camera.near = 1.0f
        camera.far = 180.0f
        camera.update()
    }

    fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }

    /**
     * Resets camera to target immediately without smoothing (e.g. at match start).
     */
    fun snapTo(targetX: Float, targetZ: Float, forwardX: Float, forwardZ: Float) {
        currentTarget.set(targetX, 0f, targetZ)
        updatePositions(0f, targetX, targetZ, forwardX, forwardZ, snap = true)
    }

    /**
     * Updates camera smoothly following the target combine.
     * @param wagonCount Used to gently zoom camera out as the trailer chain grows.
     */
    fun update(
        delta: Float,
        targetX: Float,
        targetZ: Float,
        forwardX: Float,
        forwardZ: Float,
        wagonCount: Int
    ) {
        // Zoom out slowly based on wagon count (clamped to CAMERA_MAX_ZOOM_OUT)
        targetZoom = MathUtils.clamp(wagonCount * 0.15f, 0f, GameConfig.CAMERA_MAX_ZOOM_OUT)
        currentZoom = MathUtils.lerp(currentZoom, targetZoom, delta * 2.0f)

        updatePositions(delta, targetX, targetZ, forwardX, forwardZ, snap = false)
    }

    private fun updatePositions(
        delta: Float,
        targetX: Float,
        targetZ: Float,
        forwardX: Float,
        forwardZ: Float,
        snap: Boolean
    ) {
        // Look-ahead target slightly ahead of combine in driving direction
        desiredTarget.set(
            targetX + forwardX * GameConfig.CAMERA_LOOKAHEAD_FACTOR,
            0.5f,
            targetZ + forwardZ * GameConfig.CAMERA_LOOKAHEAD_FACTOR
        )

        val totalDist = GameConfig.CAMERA_BASE_DISTANCE + currentZoom
        val totalHeight = GameConfig.CAMERA_BASE_HEIGHT + currentZoom * 0.75f

        // Camera stays behind the field view at a high ~55° pitch angle
        desiredCamPos.set(
            desiredTarget.x,
            totalHeight,
            desiredTarget.z + totalDist
        )

        if (snap) {
            currentTarget.set(desiredTarget)
            camera.position.set(desiredCamPos)
        } else {
            val factor = MathUtils.clamp(delta * GameConfig.CAMERA_SMOOTH_FACTOR, 0f, 1f)
            currentTarget.lerp(desiredTarget, factor)
            camera.position.lerp(desiredCamPos, factor)
        }

        // Apply screenshake
        camera.position.add(shakeOffset)
        camera.lookAt(currentTarget.x + shakeOffset.x, currentTarget.y, currentTarget.z + shakeOffset.z)
        camera.up.set(0f, 1f, 0f)
        camera.update()
    }
}
