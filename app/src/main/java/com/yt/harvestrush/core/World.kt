package com.yt.harvestrush.core

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.math.MathUtils
import com.yt.harvestrush.ai.BotController
import com.yt.harvestrush.ai.DensityMap
import com.yt.harvestrush.audio.SynthAudio
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.data.PlayerStore
import com.yt.harvestrush.field.CropChunkRenderer
import com.yt.harvestrush.field.CropGrid
import com.yt.harvestrush.field.FieldFactory
import com.yt.harvestrush.fx.ParticleSystem
import com.yt.harvestrush.fx.ScreenShake
import com.yt.harvestrush.render.ModelFactory
import com.yt.harvestrush.render.ShadowBlob
import com.yt.harvestrush.vehicle.Combine
import com.yt.harvestrush.vehicle.VehicleModels

/**
 * Game World managing the field, crops, player combine,
 * 3 AI rivals, match timing, and match rules.
 */
class World(
    val fieldInfo: GameConfig.FieldInfo,
    val playerStore: PlayerStore,
    private val modelFactory: ModelFactory,
    private val vehicleModels: VehicleModels,
    private val shadowBlob: ShadowBlob,
    private val audio: SynthAudio?,
    val screenShake: ScreenShake
) {
    val cropGrid = CropGrid()
    val cropRenderer = CropChunkRenderer(cropGrid, modelFactory, fieldInfo.cropType)
    val fieldFactory = FieldFactory(modelFactory)
    val densityMap = DensityMap(cropGrid)
    val particleSystem = ParticleSystem()

    var matchTimeRemaining: Float = GameConfig.DEFAULT_MATCH_DURATION
    var isMatchFinished: Boolean = false
        private set

    val combines = mutableListOf<Combine>()
    val botControllers = mutableListOf<BotController>()

    lateinit var playerCombine: Combine
        private set

    data class LeaderboardEntry(val rank: Int, val combine: Combine, val percent: Float)
    val leaderboard = mutableListOf<LeaderboardEntry>()

    private var densityRefreshTimer = 0f

    init {
        fieldFactory.buildPerimeterFences()
        fieldFactory.buildEnvironmentProps()
        initMatch()
    }

    fun initMatch() {
        cropGrid.initGrid(generatePaths = true)
        cropRenderer.cropType = fieldInfo.cropType
        densityMap.refresh()

        combines.clear()
        botControllers.clear()
        matchTimeRemaining = GameConfig.DEFAULT_MATCH_DURATION
        isMatchFinished = false

        // 1. Spawn Player Combine (ID 0)
        val playerColor = GameConfig.COMBINE_COLORS[0]
        playerCombine = Combine(
            id = 0,
            name = playerStore.playerName,
            color = playerColor,
            isPlayer = true,
            cropType = fieldInfo.cropType,
            vehicleModels = vehicleModels,
            shadowBlob = shadowBlob,
            audio = audio,
            particleSystem = particleSystem
        )
        playerCombine.reset(-18f, 18f, -45f)
        combines.add(playerCombine)

        // 2. Spawn 3 Rival AI Bots
        val botNames = GameConfig.BOT_NAMES.shuffled()
        val botSpawnPositions = listOf(
            Triple(18f, 18f, -135f),
            Triple(18f, -18f, 135f),
            Triple(-18f, -18f, 45f)
        )

        for (i in 0 until 3) {
            val botColor = GameConfig.COMBINE_COLORS[i + 1]
            val botCombine = Combine(
                id = i + 1,
                name = botNames[i],
                color = botColor,
                isPlayer = false,
                cropType = fieldInfo.cropType,
                vehicleModels = vehicleModels,
                shadowBlob = shadowBlob,
                audio = null,
                particleSystem = particleSystem
            )
            val spawn = botSpawnPositions[i]
            botCombine.reset(spawn.first, spawn.second, spawn.third)
            combines.add(botCombine)

            val botCtrl = BotController(
                combine = botCombine,
                densityMap = densityMap,
                fieldFactory = fieldFactory,
                difficulty = i
            )
            botControllers.add(botCtrl)
        }

        updateLeaderboard()
    }

    fun update(delta: Float) {
        if (isMatchFinished) return

        matchTimeRemaining = maxOf(0f, matchTimeRemaining - delta)
        if (matchTimeRemaining <= 0f || cropGrid.getTotalHarvestedPercentage() >= 99.8f) {
            finishMatch()
        }

        densityRefreshTimer += delta
        if (densityRefreshTimer > 1.2f) {
            densityMap.refresh()
            densityRefreshTimer = 0f
        }

        for (bot in botControllers) {
            bot.update(delta)
        }

        for (combine in combines) {
            combine.update(delta, cropGrid, fieldFactory) {
                if (combine.isPlayer) {
                    screenShake.addTrauma(0.12f)
                }
            }
        }

        particleSystem.update(delta)
        screenShake.update(delta)

        updateLeaderboard()
    }

    private fun updateLeaderboard() {
        leaderboard.clear()
        val sorted = combines.sortedByDescending { cropGrid.getCutPercentage(it.id) }
        sorted.forEachIndexed { index, combine ->
            val pct = cropGrid.getCutPercentage(combine.id)
            leaderboard.add(LeaderboardEntry(index + 1, combine, pct))
        }
    }

    private fun finishMatch() {
        isMatchFinished = true
        updateLeaderboard()

        val playerCuts = playerCombine.harvestedCrops
        val earnedWheat = if (fieldInfo.cropType == GameConfig.CropType.WHEAT) playerCuts else 0
        val earnedCarrot = if (fieldInfo.cropType == GameConfig.CropType.CARROT) playerCuts else 0
        val earnedCabbage = if (fieldInfo.cropType == GameConfig.CropType.CABBAGE) playerCuts else 0
        val earnedCorn = if (fieldInfo.cropType == GameConfig.CropType.CORN) playerCuts else 0

        playerStore.addResources(earnedWheat, earnedCarrot, earnedCabbage, earnedCorn)
        playerStore.recordBestPercent(fieldInfo.id, cropGrid.getCutPercentage(0))

        if (leaderboard.firstOrNull()?.combine?.isPlayer == true) {
            audio?.playWin()
            particleSystem.spawnConfetti(playerCombine.position.x, playerCombine.position.z)
        } else {
            audio?.playLose()
        }
    }

    /**
     * Renders ground & instanced crops with custom shader.
     */
    fun renderGroundAndCrops(camera: Camera, delta: Float) {
        cropRenderer.render(camera, delta)
    }

    /**
     * Renders 3D models (props, combines, wagon chains) through ModelBatch.
     */
    fun renderEntities(batch: ModelBatch, camera: Camera) {
        fieldFactory.renderProps(batch)
        for (c in combines) {
            c.render(batch, camera)
        }
    }

    /**
     * Renders particle effects.
     */
    fun renderParticles(camera: Camera) {
        particleSystem.render(camera)
    }

    fun dispose() {
        cropRenderer.dispose()
        fieldFactory.dispose()
        particleSystem.dispose()
    }
}
