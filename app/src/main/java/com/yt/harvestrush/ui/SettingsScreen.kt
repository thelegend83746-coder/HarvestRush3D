package com.yt.harvestrush.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.yt.harvestrush.HarvestGame

/**
 * Settings Screen:
 * - Sound FX toggle
 * - Music toggle
 * - Haptic Vibration toggle
 * - Steering Control Mode toggle (Virtual Joystick vs Tap Steering)
 * - Graphics Quality selector (Low / Med / High)
 * - Trailer collision toggle
 * - Reset All Progress button
 */
class SettingsScreen(val game: HarvestGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())

    init {
        val root = Table().apply {
            setFillParent(true)
            pad(24f)
        }
        stage.addActor(root)

        val title = Label("SETTINGS", game.skin, "big")
        root.add(title).padBottom(24f).row()

        val card = Table(game.skin).apply {
            background = game.skin.getDrawable("panel-bg")
            pad(20f)
        }
        root.add(card).width(320f).padBottom(24f).row()

        // 1. Sound Toggle
        val soundBtn = TextButton("Sound: " + if (game.playerStore.soundEnabled) "ON" else "OFF", game.skin)
        soundBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.playerStore.setSoundEnabled(!game.playerStore.soundEnabled)
                soundBtn.setText("Sound: " + if (game.playerStore.soundEnabled) "ON" else "OFF")
                game.audio.playClick()
            }
        })
        card.add(soundBtn).fillX().padBottom(10f).row()

        // 2. Control Mode (Virtual Joystick vs Tap Steering)
        val ctrlBtn = TextButton("Controls: " + if (game.playerStore.tapSteering) "Tap Steer" else "Joystick", game.skin)
        ctrlBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.playerStore.setTapSteering(!game.playerStore.tapSteering)
                ctrlBtn.setText("Controls: " + if (game.playerStore.tapSteering) "Tap Steer" else "Joystick")
                game.audio.playClick()
            }
        })
        card.add(ctrlBtn).fillX().padBottom(10f).row()

        // 3. Graphics Quality
        val gfxBtn = TextButton("Quality: " + game.playerStore.graphicsQuality, game.skin)
        gfxBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                val next = when (game.playerStore.graphicsQuality) {
                    "Low" -> "Med"
                    "Med" -> "High"
                    else -> "Low"
                }
                game.playerStore.setGraphicsQuality(next)
                gfxBtn.setText("Quality: $next")
                game.audio.playClick()
            }
        })
        card.add(gfxBtn).fillX().padBottom(10f).row()

        // 4. Trailer Collision Toggle
        val colBtn = TextButton("Rival Collision: " + if (game.playerStore.rivalCollisions) "ON" else "OFF", game.skin)
        colBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.playerStore.setRivalCollisions(!game.playerStore.rivalCollisions)
                colBtn.setText("Rival Collision: " + if (game.playerStore.rivalCollisions) "ON" else "OFF")
                game.audio.playClick()
            }
        })
        card.add(colBtn).fillX().padBottom(10f).row()

        // 5. Reset Progress
        val resetBtn = TextButton("Reset All Progress", game.skin).apply {
            color = Color(0.9f, 0.3f, 0.3f, 1f)
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.playerStore.resetAllProgress()
                    game.audio.playClick()
                }
            })
        }
        card.add(resetBtn).fillX().row()

        // Back Button
        val backBtn = TextButton("BACK", game.skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.audio.playClick()
                    game.showHomeScreen()
                }
            })
        }
        root.add(backBtn).size(160f, 56f)
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.12f, 0.14f, 0.18f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
    }
}
