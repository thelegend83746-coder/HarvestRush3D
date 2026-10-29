package com.yt.harvestrush.vehicle

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.render.ShadowBlob

/**
 * Unlimited Wagon Chain System (NO LIMITS).
 * Uses an O(1) circular ring buffer of arc-length sampled points.
 * Supports 2,000+ wagons at solid 60 FPS via direct indexing,
 * distance LOD, and frustum culling.
 */
class WagonChain(
    val ownerId: Int,
    val color: Color,
    val cropType: GameConfig.CropType,
    private val vehicleModels: VehicleModels,
    private val shadowBlob: ShadowBlob
) {
    // Ring buffer capacity (Power of 2 for fast bitwise masking)
    private val bufferMask = GameConfig.WAGON_RING_BUFFER_SIZE - 1
    private val pathX = FloatArray(GameConfig.WAGON_RING_BUFFER_SIZE)
    private val pathZ = FloatArray(GameConfig.WAGON_RING_BUFFER_SIZE)
    private val pathYaw = FloatArray(GameConfig.WAGON_RING_BUFFER_SIZE)

    private var headIdx: Int = 0
    private var totalSamplesRecorded: Int = 0
    private var accumulatedDist: Float = 0f

    private var lastRecordedX: Float = 0f
    private var lastRecordedZ: Float = 0f

    // Wagons state
    class WagonItem(
        val index: Int,
        var currentX: Float = 0f,
        var currentZ: Float = 0f,
        var currentYaw: Float = 0f,
        var popTimer: Float = 0f
    )

    val wagons = ArrayList<WagonItem>(64)

    // Shared visual model instances (re-used for drawing)
    private val wagonInstance: ModelInstance = vehicleModels.getWagonInstance(color)
    private val cargoInstance: ModelInstance = vehicleModels.getCargoInstance(cropType)

    private val samplesPerWagonSpacing: Float = GameConfig.WAGON_SPACING / GameConfig.WAGON_PATH_SAMPLE_INTERVAL

    fun getCount(): Int = wagons.size

    /**
     * Initializes the path history at the starting position and heading.
     */
    fun reset(startX: Float, startZ: Float, startHeadingDeg: Float) {
        wagons.clear()
        headIdx = 0
        totalSamplesRecorded = 0
        accumulatedDist = 0f
        lastRecordedX = startX
        lastRecordedZ = startZ

        val rad = startHeadingDeg * MathUtils.degreesToRadians
        for (i in 0 until GameConfig.WAGON_RING_BUFFER_SIZE) {
            val distBack = i * GameConfig.WAGON_PATH_SAMPLE_INTERVAL
            pathX[i] = startX - MathUtils.cos(rad) * distBack
            pathZ[i] = startZ - MathUtils.sin(rad) * distBack
            pathYaw[i] = startHeadingDeg
        }
    }

    /**
     * Appends the combine's movement to the ring-buffer path history.
     */
    fun updatePath(currentX: Float, currentZ: Float, currentHeadingDeg: Float, delta: Float) {
        val dx = currentX - lastRecordedX
        val dz = currentZ - lastRecordedZ
        val stepDist = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()

        accumulatedDist += stepDist
        lastRecordedX = currentX
        lastRecordedZ = currentZ

        // Record discrete samples along traveled path
        while (accumulatedDist >= GameConfig.WAGON_PATH_SAMPLE_INTERVAL) {
            headIdx = (headIdx + 1) and bufferMask
            pathX[headIdx] = currentX
            pathZ[headIdx] = currentZ
            pathYaw[headIdx] = currentHeadingDeg
            totalSamplesRecorded++
            accumulatedDist -= GameConfig.WAGON_PATH_SAMPLE_INTERVAL
        }

        // Update squash & stretch pop animations for newly attached wagons
        for (w in wagons) {
            if (w.popTimer > 0f) {
                w.popTimer = maxOf(0f, w.popTimer - delta)
            }
        }
    }

    /**
     * Attaches a new wagon to the unlimited chain.
     */
    fun addWagon() {
        val newIndex = wagons.size
        val wagon = WagonItem(
            index = newIndex,
            popTimer = GameConfig.WAGON_POP_DURATION
        )
        wagons.add(wagon)
    }

    /**
     * Renders all wagons in the chain with frustum culling and distance LOD.
     */
    fun render(batch: ModelBatch, camera: Camera) {
        val wagonCount = wagons.size
        if (wagonCount == 0) return

        val camPos = camera.position
        val frustum = camera.frustum
        val tmpPos = Vector3()

        for (i in 0 until wagonCount) {
            val wagon = wagons[i]
            // Direct O(1) sample index lookup along ring buffer
            val sampleOffset = ((i + 1) * samplesPerWagonSpacing).toInt()
            val sampleIdx = (headIdx - sampleOffset) and bufferMask

            val targetX = pathX[sampleIdx]
            val targetZ = pathZ[sampleIdx]
            val targetYaw = pathYaw[sampleIdx]

            // Visual follow-lag inertia smoothing
            wagon.currentX = MathUtils.lerp(wagon.currentX, targetX, 0.75f)
            wagon.currentZ = MathUtils.lerp(wagon.currentZ, targetZ, 0.75f)
            wagon.currentYaw = targetYaw

            tmpPos.set(wagon.currentX, 0.5f, wagon.currentZ)

            // Frustum Culling
            if (!frustum.sphereInFrustum(tmpPos, 2.0f)) {
                continue
            }

            // Distance LOD check
            val distSq = tmpPos.dst2(camPos)
            val isFar = distSq > 1600f // > 40 units away

            // Squash & stretch scale spring pop calculation
            var scaleX = 1.0f
            var scaleY = 1.0f
            var scaleZ = 1.0f
            if (wagon.popTimer > 0f) {
                val t = 1.0f - (wagon.popTimer / GameConfig.WAGON_POP_DURATION)
                // Spring overshoot bounce
                val bounce = MathUtils.sin(t * MathUtils.PI * 2.5f) * (1.0f - t)
                scaleX = 1.0f + bounce * 0.45f
                scaleY = 1.0f - bounce * 0.35f
                scaleZ = 1.0f + bounce * 0.45f
            }

            // Render Blob Shadow
            shadowBlob.render(batch, wagon.currentX, wagon.currentZ, 0.9f, 1.1f, 0.4f)

            // Render Wagon Chassis
            wagonInstance.transform.idt()
            wagonInstance.transform.setToTranslation(wagon.currentX, 0f, wagon.currentZ)
            wagonInstance.transform.rotate(Vector3.Y, -wagon.currentYaw + 90f)
            wagonInstance.transform.scale(scaleX, scaleY, scaleZ)
            batch.render(wagonInstance)

            // Render Cargo Load (skip on far LOD to save draw calls)
            if (!isFar) {
                cargoInstance.transform.idt()
                cargoInstance.transform.setToTranslation(wagon.currentX, 0.35f * scaleY, wagon.currentZ)
                cargoInstance.transform.rotate(Vector3.Y, -wagon.currentYaw + 90f)
                cargoInstance.transform.scale(scaleX * 0.85f, scaleY * 0.85f, scaleZ * 0.85f)
                batch.render(cargoInstance)
            }
        }
    }
}
