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
import com.yt.harvestrush.core.World
import com.yt.harvestrush.fx.FloatTween

/**
 * Result Screen shown at the end of a match:
 * - Match Podium / Final Ranking
 * - Animated count-up of harvest percentage
 * - Resources earned summary
 * - Retry, Next Field, and Home buttons
 */
class ResultScreen(val game: HarvestGame, val world: World) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private val percentLabel: Label
    private var tween: FloatTween? = null

    init {
        val root = Table().apply {
            setFillParent(true)
            pad(24f)
        }
        stage.addActor(root)

        // Title: VICTORY or MATCH FINISHED
        val playerWon = world.leaderboard.firstOrNull()?.combine?.isPlayer == true
        val titleText = if (playerWon) "🏆 VICTORY!" else "MATCH FINISHED"
        val titleLabel = Label(titleText, game.skin, "big").apply {
            color = if (playerWon) Color(0.98f, 0.85f, 0.2f, 1f) else Color.WHITE
        }
        root.add(titleLabel).padBottom(16f).row()

        // Count-up Animated Harvested Percentage
        val playerPct = world.cropGrid.getCutPercentage(0)
        percentLabel = Label("0.0%", game.skin, "big").apply {
            color = Color(0.18f, 0.88f, 0.98f, 1f)
        }
        root.add(percentLabel).padBottom(24f).row()

        // Tween to count-up from 0% to playerPct
        tween = FloatTween(0f, playerPct, 1.4f, onUpdate = { v ->
            percentLabel.setText(String.format("%.1f%%", v))
        })

        // Podium / Leaderboard Table
        val podiumTable = Table(game.skin).apply {
            background = game.skin.getDrawable("panel-bg")
            pad(16f)
        }
        for (entry in world.leaderboard) {
            val rankIcon = when (entry.rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "4."
            }
            val rowLabel = Label(
                String.format("%s  %s  -  %.1f%%", rankIcon, entry.combine.name, entry.percent),
                game.skin
            ).apply {
                color = entry.combine.color
            }
            podiumTable.add(rowLabel).left().pad(6f).row()
        }
        root.add(podiumTable).width(300f).padBottom(24f).row()

        // Resources Earned
        val earnedCrops = world.playerCombine.harvestedCrops
        val resLabel = Label(String.format("Earned: +%d %s", earnedCrops, world.fieldInfo.cropType.displayName), game.skin)
        root.add(resLabel).padBottom(32f).row()

        // Buttons: Retry / Home
        val btnTable = Table()
        val retryBtn = TextButton("RETRY", game.skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.audio.playClick()
                    game.startMatch(world.fieldInfo)
                }
            })
        }
        val homeBtn = TextButton("HOME", game.skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.audio.playClick()
                    game.showHomeScreen()
                }
            })
        }
        btnTable.add(retryBtn).size(130f, 56f).padRight(16f)
        btnTable.add(homeBtn).size(130f, 56f)
        root.add(btnTable).row()
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        tween?.update(delta)

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
