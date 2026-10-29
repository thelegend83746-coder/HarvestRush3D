package com.yt.harvestrush.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.yt.harvestrush.HarvestGame
import com.yt.harvestrush.config.GameConfig

/**
 * Home Screen with interactive 3D Diorama:
 * - Rotating player combine on miniature farm diorama
 * - Fenced "Unlock next field" panel with 4 persistent resource slots
 * - Field Best % label and progress bar
 * - Player Name Pill (tap to edit)
 * - Big blue circular Play button with pulse
 * - Field selector (swipe or prev/next arrows)
 */
class HomeScreen(val game: HarvestGame) : ScreenAdapter() {

    private val stage = Stage(ScreenViewport())
    private val modelBatch = ModelBatch()
    private val dioramaCam = PerspectiveCamera(50f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    private var selectedFieldIndex = 0
    private var dioramaAngle = 0f
    private var playButtonPulse = 0f

    // 3D diorama models
    private val combineInstance: ModelInstance = game.vehicleModels.getCombineInstance(GameConfig.COMBINE_COLORS[0])
    private val haystackInstance: ModelInstance = ModelInstance(game.modelFactory.createHaystackModel())
    private val treeInstance: ModelInstance = ModelInstance(game.modelFactory.createTreeModel())

    // UI Elements
    private val bestPercentLabel: Label
    private val bestProgressBar: ProgressBar
    private val fieldNameLabel: Label
    private val namePillButton: TextButton
    private val playButton: TextButton
    private val unlockPanelTable: Table
    private val resourceLabels = Array(4) { Label("", game.skin, "small") }

    init {
        dioramaCam.position.set(0f, 4.5f, 7.5f)
        dioramaCam.lookAt(0f, 1.2f, 0f)
        dioramaCam.near = 1f
        dioramaCam.far = 50f
        dioramaCam.update()

        haystackInstance.transform.setToTranslation(-2.5f, 0f, -1.2f)
        treeInstance.transform.setToTranslation(2.8f, 0f, -1.5f)

        val root = Table().apply {
            setFillParent(true)
            top()
        }
        stage.addActor(root)

        // Top Bar: Settings Button
        val topBar = Table().apply { pad(16f) }
        val settingsBtn = TextButton("⚙", game.skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.audio.playClick()
                    game.showSettingsScreen()
                }
            })
        }
        topBar.add(settingsBtn).left().size(54f, 54f)
        topBar.add().expandX()
        root.add(topBar).fillX().row()

        // "Unlock next field" panel with 4 resource counters
        unlockPanelTable = Table(game.skin).apply {
            background = game.skin.getDrawable("panel-bg")
            pad(12f)
        }
        val panelTitle = Label("UNLOCK NEXT FIELD", game.skin, "small").apply {
            color = Color(0.95f, 0.85f, 0.4f, 1f)
        }
        unlockPanelTable.add(panelTitle).colspan(4).padBottom(6f).row()

        for (i in 0 until 4) {
            unlockPanelTable.add(resourceLabels[i]).pad(4f)
        }
        root.add(unlockPanelTable).padTop(8f).row()

        // Field Name & Best % Progress Bar
        fieldNameLabel = Label("", game.skin, "big")
        root.add(fieldNameLabel).padTop(16f).row()

        bestPercentLabel = Label("Best 0.0%", game.skin)
        root.add(bestPercentLabel).padTop(4f).row()

        bestProgressBar = ProgressBar(0f, 100f, 0.1f, false, game.skin)
        root.add(bestProgressBar).width(260f).height(18f).padTop(4f).row()

        // Spacer pushing controls to bottom
        root.add().expandY().row()

        // Field Switcher Arrows (Prev / Next)
        val switcherTable = Table()
        val prevBtn = TextButton("<", game.skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.audio.playClick()
                    selectField(selectedFieldIndex - 1)
                }
            })
        }
        val nextBtn = TextButton(">", game.skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.audio.playClick()
                    selectField(selectedFieldIndex + 1)
                }
            })
        }
        switcherTable.add(prevBtn).size(60f, 50f).padRight(40f)
        switcherTable.add(nextBtn).size(60f, 50f)
        root.add(switcherTable).padBottom(12f).row()

        // Name Pill (Tap to edit)
        namePillButton = TextButton("👤 " + game.playerStore.playerName, game.skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.audio.playClick()
                    showNameInputDialog()
                }
            })
        }
        root.add(namePillButton).height(44f).padBottom(18f).row()

        // Big Circular Blue Play Button
        playButton = TextButton("PLAY", game.skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    game.audio.playClick()
                    val field = GameConfig.FIELDS[selectedFieldIndex]
                    game.startMatch(field)
                }
            })
        }
        root.add(playButton).size(180f, 68f).padBottom(36f)

        selectField(0)
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
        namePillButton.setText("👤 " + game.playerStore.playerName)
        refreshUnlockPanel()
    }

    private fun selectField(idx: Int) {
        selectedFieldIndex = MathUtils.clamp(idx, 0, GameConfig.FIELDS.size - 1)
        val field = GameConfig.FIELDS[selectedFieldIndex]
        fieldNameLabel.setText(field.name)

        val best = game.playerStore.getBestPercent(field.id)
        bestPercentLabel.setText(String.format("Best %.1f%%", best))
        bestProgressBar.value = best

        val isUnlocked = game.playerStore.isFieldUnlocked(field.id)
        if (isUnlocked) {
            playButton.setText("PLAY")
            playButton.color = Color.WHITE
        } else {
            playButton.setText("LOCKED")
            playButton.color = Color.GRAY
        }

        refreshUnlockPanel()
    }

    private fun refreshUnlockPanel() {
        // Next field to unlock
        val nextField = GameConfig.FIELDS.getOrNull(selectedFieldIndex + 1)
        if (nextField != null) {
            unlockPanelTable.isVisible = true
            resourceLabels[0].setText(String.format("🌾 %d/%d", game.playerStore.resWheat, nextField.reqWheat))
            resourceLabels[1].setText(String.format("🥕 %d/%d", game.playerStore.resCarrot, nextField.reqCarrot))
            resourceLabels[2].setText(String.format("🥬 %d/%d", game.playerStore.resCabbage, nextField.reqCabbage))
            resourceLabels[3].setText(String.format("🌽 %d/%d", game.playerStore.resCorn, nextField.reqCorn))
        } else {
            unlockPanelTable.isVisible = false
        }
    }

    private fun showNameInputDialog() {
        Gdx.input.getTextInput(object : Input.TextInputListener {
            override fun input(text: String?) {
                if (!text.isNullOrBlank()) {
                    game.playerStore.setPlayerName(text.trim())
                    namePillButton.setText("👤 " + game.playerStore.playerName)
                }
            }
            override fun canceled() {}
        }, "Enter Player Name", game.playerStore.playerName, "e.g. Bill")
    }

    override fun render(delta: Float) {
        dioramaAngle += delta * 24f
        playButtonPulse += delta * 4f

        // Play button subtle scale pulse
        val scale = 1.0f + MathUtils.sin(playButtonPulse) * 0.04f
        playButton.setScale(scale)

        // Clear screen
        Gdx.gl.glClearColor(0.55f, 0.78f, 0.95f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // 3D Rotating Combine & Props in Diorama
        combineInstance.transform.idt()
        combineInstance.transform.setToTranslation(0f, 0.2f, 0f)
        combineInstance.transform.rotate(Vector3.Y, dioramaAngle)

        modelBatch.begin(dioramaCam)
        modelBatch.render(haystackInstance)
        modelBatch.render(treeInstance)
        modelBatch.render(combineInstance)
        modelBatch.end()

        // 2D UI
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        dioramaCam.viewportWidth = width.toFloat()
        dioramaCam.viewportHeight = height.toFloat()
        dioramaCam.update()
    }

    override fun dispose() {
        stage.dispose()
        modelBatch.dispose()
    }
}
