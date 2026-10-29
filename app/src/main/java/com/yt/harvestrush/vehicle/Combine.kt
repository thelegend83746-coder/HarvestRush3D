package com.yt.harvestrush.vehicle

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.yt.harvestrush.audio.SynthAudio
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.field.CropGrid
import com.yt.harvestrush.field.FieldFactory
import com.yt.harvestrush.fx.ParticleSystem
import com.yt.harvestrush.render.ShadowBlob

/**
 * Combine Harvester Vehicle.
 * Handles continuous forward drive, responsive arcade steering, body roll lean,
 * wheel spin, header cutting sweep, and unlimited trailer chain accumulation.
 */
class Combine(
    val id: Int,
    val name: String,
    val color: Color,
    val isPlayer: Boolean,
    val cropType: GameConfig.CropType,
    private val vehicleModels: VehicleModels,
    private val shadowBlob: ShadowBlob,
    private val audio: SynthAudio?,
    private val particleSystem: ParticleSystem?
) {
    val position = Vector3()
    var headingRad: Float = 0f // In radians, 0 = facing +X
    var targetHeadingRad: Float = 0f
    var speed: Float = GameConfig.COMBINE_MOVE_SPEED

    var currentRollRad: Float = 0f
    var wheelSpinAngleDeg: Float = 0f
    var reelSpinAngleDeg: Float = 0f

    // Harvested counters
    var harvestedCrops: Int = 0
        private set
    private var cropsTowardsNextWagon: Int = 0

    // Unlimited trailer chain
    val wagonChain = WagonChain(id, color, cropType, vehicleModels, shadowBlob)

    // Visual model instance
    private val modelInstance: ModelInstance = vehicleModels.getCombineInstance(color)

    // Forward direction vector
    val forward = Vector3(1f, 0f, 0f)

    fun reset(startX: Float, startZ: Float, startHeadingDeg: Float) {
        position.set(startX, 0f, startZ)
        headingRad = startHeadingDeg * MathUtils.degreesToRadians
        targetHeadingRad = headingRad
        speed = GameConfig.COMBINE_MOVE_SPEED
        harvestedCrops = 0
        cropsTowardsNextWagon = 0
        currentRollRad = 0f
        wheelSpinAngleDeg = 0f
        reelSpinAngleDeg = 0f

        updateForwardVector()
        wagonChain.reset(startX, startZ, startHeadingDeg)
    }

    private fun updateForwardVector() {
        forward.set(MathUtils.cos(headingRad), 0f, MathUtils.sin(headingRad))
    }

    /**
     * Steers the combine towards target angle smoothly.
     */
    fun setSteeringTarget(targetRad: Float) {
        targetHeadingRad = targetRad
    }

    /**
     * Turn incrementally (e.g. for tap steering or bots).
     */
    fun turn(rateDegreesPerSec: Float, delta: Float) {
        val maxTurn = GameConfig.COMBINE_TURN_SPEED * MathUtils.degreesToRadians * delta
        val deltaAngle = rateDegreesPerSec * MathUtils.degreesToRadians * delta
        targetHeadingRad += MathUtils.clamp(deltaAngle, -maxTurn, maxTurn)
    }

    fun update(
        delta: Float,
        cropGrid: CropGrid,
        fieldFactory: FieldFactory,
        onWagonAdded: (() -> Unit)? = null
    ) {
        // 1. Smooth Steering towards target heading
        val angleDiff = MathUtils.atan2(
            MathUtils.sin(targetHeadingRad - headingRad),
            MathUtils.cos(targetHeadingRad - headingRad)
        )
        val turnSpeed = GameConfig.COMBINE_TURN_SPEED * MathUtils.degreesToRadians
        val maxTurnThisFrame = turnSpeed * delta
        val turnStep = MathUtils.clamp(angleDiff * GameConfig.COMBINE_TURN_SMOOTHING * delta, -maxTurnThisFrame, maxTurnThisFrame)
        headingRad += turnStep

        // Combine body roll (lean into turns)
        val targetRoll = -MathUtils.clamp(turnStep / (turnSpeed * delta + 0.0001f), -1f, 1f) * GameConfig.COMBINE_ROLL_FACTOR
        currentRollRad = MathUtils.lerp(currentRollRad, targetRoll, delta * 10f)

        // 2. Drive Forward (Continuous forward movement)
        updateForwardVector()
        position.x += forward.x * speed * delta
        position.z += forward.z * speed * delta

        // Wheel and Header Reel Rotation
        val distMoved = speed * delta
        wheelSpinAngleDeg = (wheelSpinAngleDeg + distMoved * 120f) % 360f
        reelSpinAngleDeg = (reelSpinAngleDeg + distMoved * 180f) % 360f

        // Slide smoothly along fence boundary
        fieldFactory.clampToFence(position)

        // 3. Update Wagon Chain Path History
        wagonChain.updatePath(position.x, position.z, headingRad * MathUtils.radiansToDegrees, delta)

        // 4. Header Cutting Sweep
        val headerCenterX = position.x + forward.x * GameConfig.HEADER_FORWARD_OFFSET
        val headerCenterZ = position.z + forward.z * GameConfig.HEADER_FORWARD_OFFSET

        val cutCount = cropGrid.cutHeaderArea(
            harvesterId = id,
            centerX = headerCenterX,
            centerZ = headerCenterZ,
            headingRad = headingRad,
            width = GameConfig.HEADER_WIDTH,
            depth = GameConfig.HEADER_DEPTH
        ) { wx, wz ->
            // Spawn chip particles at cut crop location
            particleSystem?.spawnCropChips(wx, wz, cropType)
        }

        if (cutCount > 0) {
            harvestedCrops += cutCount
            cropsTowardsNextWagon += cutCount

            if (isPlayer) {
                audio?.playCut()
            }

            // Check if enough crops harvested to attach a new trailer
            while (cropsTowardsNextWagon >= GameConfig.CROPS_PER_WAGON) {
                cropsTowardsNextWagon -= GameConfig.CROPS_PER_WAGON
                wagonChain.addWagon()
                if (isPlayer) {
                    audio?.playWagonPop()
                }
                onWagonAdded?.invoke()
            }
        }

        // Exhaust smoke particle puff occasionally
        if (MathUtils.randomBoolean(0.35f)) {
            val exhaustX = position.x - forward.x * 1.3f
            val exhaustZ = position.z - forward.z * 1.3f
            particleSystem?.spawnExhaustPuff(exhaustX, exhaustZ)
        }
    }

    fun render(batch: ModelBatch, camera: Camera) {
        // Blob Shadow under combine
        shadowBlob.render(batch, position.x, position.z, 1.4f, 1.8f, 0.5f)

        // Combine Model
        modelInstance.transform.idt()
        modelInstance.transform.setToTranslation(position.x, 0f, position.z)
        modelInstance.transform.rotate(Vector3.Y, -headingRad * MathUtils.radiansToDegrees + 90f)
        modelInstance.transform.rotate(Vector3.Z, currentRollRad * MathUtils.radiansToDegrees)
        batch.render(modelInstance)

        // Render Unlimited Wagon Chain
        wagonChain.render(batch, camera)
    }
}
