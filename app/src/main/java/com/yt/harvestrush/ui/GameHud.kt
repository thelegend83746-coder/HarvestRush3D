package com.yt.harvestrush.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.core.GameInput
import com.yt.harvestrush.core.World

/**
 * In-game HUD overlay matching the reference screenshots:
 * - Top-left player percentage pill
 * - Cyan circular stopwatch (MM:SS) that pulses red when <10s
 * - Top-right live animated leaderboard
 * - Overhead combine name tags and mini progress bars
 * - Floating thumb joystick indicator
 */
class GameHud(
    private val world: World,
    private val skin: Skin,
    private val input: GameInput,
    private val onPauseClicked: () -> Unit
) {
    val stage: Stage = Stage(ScreenViewport())
    private val shapeRenderer = ShapeRenderer()

    private val playerPercentLabel: Label
    private val timerLabel: Label
    private val leaderboardLabels = Array(4) { Label("", skin) }

    private var pulseTimer = 0f
    private val screenPos = Vector3()

    init {
        val rootTable = Table().apply {
            setFillParent(true)
            top()
        }
        stage.addActor(rootTable)

        // Top Header Row
        val headerTable = Table().apply {
            pad(16f)
        }
        rootTable.add(headerTable).expandX().fillX()

        // Top-Left: Percentage Pill & Cyan Stopwatch
        val leftTable = Table()
        playerPercentLabel = Label("0.0%", skin, "big").apply {
            color = Color.WHITE
        }
        timerLabel = Label("02:00", skin, "cyan")

        leftTable.add(playerPercentLabel).padRight(16f)
        leftTable.add(timerLabel)
        headerTable.add(leftTable).left()

        // Spacer
        headerTable.add().expandX()

        // Top-Right: Leaderboard
        val leaderboardTable = Table().apply {
            right()
        }
        for (i in 0 until 4) {
            leaderboardTable.add(leaderboardLabels[i]).right().row()
        }
        headerTable.add(leaderboardTable).right()
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    fun update(delta: Float) {
        pulseTimer += delta

        // 1. Update Player Percentage
        val playerPct = world.cropGrid.getCutPercentage(0)
        playerPercentLabel.setText(String.format("%.1f%%", playerPct))

        // 2. Update Stopwatch Timer
        val time = world.matchTimeRemaining
        val minutes = (time / 60).toInt()
        val seconds = (time % 60).toInt()
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds))

        // Timer pulse red when <10 seconds
        if (time <= GameConfig.TIME_WARNING_THRESHOLD) {
            val pulse = (MathUtils.sin(pulseTimer * 10f) + 1f) * 0.5f
            timerLabel.color = Color(1f, 0.2f * pulse, 0.2f * pulse, 1f)
        } else {
            timerLabel.color = GameConfig.COLOR_CYAN_STOPWATCH
        }

        // 3. Update Live Leaderboard Rows
        val entries = world.leaderboard
        for (i in 0 until 4) {
            if (i < entries.size) {
                val entry = entries[i]
                val label = leaderboardLabels[i]
                label.setText(String.format("%.1f%%  %s", entry.percent, entry.combine.name))
                label.color = entry.combine.color
                label.isVisible = true
            } else {
                leaderboardLabels[i].isVisible = false
            }
        }

        stage.act(delta)
    }

    /**
     * Renders HUD stage, virtual joystick, and 3D overhead name tags.
     */
    fun render(camera: Camera) {
        // 1. Draw 3D overhead name pills & mini progress bars
        shapeRenderer.projectionMatrix = stage.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (combine in world.combines) {
            screenPos.set(combine.position.x, 2.8f, combine.position.z)
            camera.project(screenPos)

            // Convert project coordinates to UI viewport
            val uiX = screenPos.x
            val uiY = screenPos.y

            if (screenPos.z > 0 && screenPos.z < 1.0f) {
                val barW = 70f
                val barH = 8f
                val pct = world.cropGrid.getCutPercentage(combine.id) / 100f

                // Background bar
                shapeRenderer.color = Color(0f, 0f, 0f, 0.6f)
                shapeRenderer.rect(uiX - barW * 0.5f, uiY, barW, barH)

                // Fill bar
                shapeRenderer.color = combine.color
                shapeRenderer.rect(uiX - barW * 0.5f, uiY, barW * pct, barH)
            }
        }

        // 2. Virtual Joystick Visualizer (Touch thumb indicator)
        if (input.isTouching && !input.hasActiveSteering) {
            // Origin ring
            shapeRenderer.color = Color(1f, 1f, 1f, 0.25f)
            val joyY = Gdx.graphics.height - input.touchOrigin.y
            shapeRenderer.circle(input.touchOrigin.x, joyY, 48f)
        } else if (input.isTouching && input.hasActiveSteering) {
            shapeRenderer.color = Color(1f, 1f, 1f, 0.25f)
            val joyY = Gdx.graphics.height - input.touchOrigin.y
            shapeRenderer.circle(input.touchOrigin.x, joyY, 48f)

            // Inner thumb knob
            shapeRenderer.color = Color(0.18f, 0.85f, 0.95f, 0.7f)
            val thumbY = Gdx.graphics.height - input.touchCurrent.y
            shapeRenderer.circle(input.touchCurrent.x, thumbY, 24f)
        }

        shapeRenderer.end()

        // 3. Render 2D UI stage
        stage.draw()
    }

    fun dispose() {
        stage.dispose()
        shapeRenderer.dispose()
    }
}
