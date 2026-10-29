package com.yt.harvestrush.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.yt.harvestrush.HarvestGame
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.render.CameraRig
import com.yt.harvestrush.ui.GameHud

/**
 * Active match gameplay loop.
 * Connects GameInput, World, CameraRig, ModelBatch, and GameHud.
 */
class GameLoop(
    val game: HarvestGame,
    val world: World,
    val cameraRig: CameraRig,
    val input: GameInput,
    val hud: GameHud
) {
    private val modelBatch = ModelBatch()
    var isPaused: Boolean = false
    private var hasTransitionedToResult: Boolean = false

    init {
        cameraRig.snapTo(
            world.playerCombine.position.x,
            world.playerCombine.position.z,
            world.playerCombine.forward.x,
            world.playerCombine.forward.z
        )
    }

    fun render(delta: Float) {
        // 1. Process Player Input
        if (!isPaused && !world.isMatchFinished) {
            if (game.playerStore.tapSteering) {
                val turnRate = input.getTapTurnRate()
                if (turnRate != 0f) {
                    world.playerCombine.turn(turnRate * GameConfig.COMBINE_TURN_SPEED, delta)
                }
            } else if (input.hasActiveSteering) {
                world.playerCombine.setSteeringTarget(input.targetHeadingRad)
            }

            // 2. Update World Simulation
            world.update(delta)
            game.audio.updateEnginePitch(world.playerCombine.speed / GameConfig.COMBINE_MOVE_SPEED)
        }

        // 3. Update Camera Follow & Screenshake
        cameraRig.shakeOffset.set(world.screenShake.offset)
        cameraRig.update(
            delta,
            world.playerCombine.position.x,
            world.playerCombine.position.z,
            world.playerCombine.forward.x,
            world.playerCombine.forward.z,
            world.playerCombine.wagonChain.getCount()
        )

        // 4. Clear GL Screen (Soft pastel sky)
        Gdx.gl.glClearColor(0.55f, 0.78f, 0.95f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // 5. Render 3D Ground & Crops
        world.renderGroundAndCrops(cameraRig.camera, delta)

        // 6. Render 3D Props & Vehicles
        modelBatch.begin(cameraRig.camera)
        world.renderEntities(modelBatch, cameraRig.camera)
        modelBatch.end()

        // 7. Render 3D Particles
        world.renderParticles(cameraRig.camera)

        // 8. Update & Render HUD
        hud.update(delta)
        hud.render(cameraRig.camera)

        // 9. Single Transition to Result Screen
        if (world.isMatchFinished && !hasTransitionedToResult) {
            hasTransitionedToResult = true
            game.showResultScreen(world)
        }
    }

    fun resize(width: Int, height: Int) {
        cameraRig.resize(width, height)
        hud.resize(width, height)
    }

    fun dispose() {
        modelBatch.dispose()
        world.dispose()
        hud.dispose()
    }
}
