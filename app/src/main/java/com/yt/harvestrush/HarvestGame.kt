package com.yt.harvestrush

import android.content.Context
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.yt.harvestrush.audio.SynthAudio
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.core.GameInput
import com.yt.harvestrush.core.GameLoop
import com.yt.harvestrush.core.World
import com.yt.harvestrush.data.PlayerStore
import com.yt.harvestrush.fx.ScreenShake
import com.yt.harvestrush.render.CameraRig
import com.yt.harvestrush.render.ModelFactory
import com.yt.harvestrush.render.ShadowBlob
import com.yt.harvestrush.ui.GameHud
import com.yt.harvestrush.ui.HomeScreen
import com.yt.harvestrush.ui.ResultScreen
import com.yt.harvestrush.ui.SettingsScreen
import com.yt.harvestrush.ui.SkinBuilder
import com.yt.harvestrush.vehicle.VehicleModels

/**
 * Root libGDX Game class.
 * Coordinates screens, global services (audio, persistence, models),
 * and lifecycle transitions.
 */
class HarvestGame(val context: Context) : Game() {

    lateinit var playerStore: PlayerStore
        private set
    lateinit var modelFactory: ModelFactory
        private set
    lateinit var vehicleModels: VehicleModels
        private set
    lateinit var shadowBlob: ShadowBlob
        private set
    lateinit var audio: SynthAudio
        private set
    lateinit var skin: Skin
        private set

    private var homeScreen: HomeScreen? = null

    override fun create() {
        playerStore = PlayerStore(context)
        modelFactory = ModelFactory()
        vehicleModels = VehicleModels(modelFactory)
        shadowBlob = ShadowBlob()
        audio = SynthAudio(playerStore)
        skin = SkinBuilder.buildSkin()

        showHomeScreen()
    }

    fun showHomeScreen() {
        if (homeScreen == null) {
            homeScreen = HomeScreen(this)
        }
        setScreen(homeScreen)
    }

    fun startMatch(fieldInfo: GameConfig.FieldInfo) {
        val shake = ScreenShake()
        val world = World(
            fieldInfo = fieldInfo,
            playerStore = playerStore,
            modelFactory = modelFactory,
            vehicleModels = vehicleModels,
            shadowBlob = shadowBlob,
            audio = audio,
            screenShake = shake
        )
        val cameraRig = CameraRig()
        val input = GameInput(playerStore)

        val hud = GameHud(world, skin, input) {
            // Pause clicked
        }

        val gameLoop = GameLoop(this, world, cameraRig, input, hud)

        val multiplexer = InputMultiplexer()
        multiplexer.addProcessor(hud.stage)
        multiplexer.addProcessor(input)
        Gdx.input.inputProcessor = multiplexer

        audio.startEngine()

        // Match Screen wrapper
        val matchScreen = object : ScreenAdapter() {
            override fun render(delta: Float) {
                gameLoop.render(delta)
            }

            override fun resize(width: Int, height: Int) {
                gameLoop.resize(width, height)
            }

            override fun hide() {
                audio.stopEngine()
            }

            override fun dispose() {
                audio.stopEngine()
                gameLoop.dispose()
            }
        }

        setScreen(matchScreen)
    }

    fun showResultScreen(world: World) {
        audio.stopEngine()
        setScreen(ResultScreen(this, world))
    }

    fun showSettingsScreen() {
        setScreen(SettingsScreen(this))
    }

    override fun dispose() {
        super.dispose()
        homeScreen?.dispose()
        skin.dispose()
        audio.dispose()
        shadowBlob.dispose()
        vehicleModels.dispose()
        modelFactory.dispose()
    }
}
