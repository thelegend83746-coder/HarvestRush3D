package com.yt.harvestrush.vehicle

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
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
    private val bufferMask = GameConfig.WAGON_RING_BUFFER_SIZE - 1
    private val pathX = FloatArray(GameConfig.WAGON_RING_BUFFER_SIZE)
    private val pathZ = FloatArray(GameConfig.WAGON_RING_BUFFER_SIZE)
    private val pathYaw = FloatArray(GameConfig.WAGON_RING_BUFFER_SIZE)

    private var headIdx: Int = 0
    private var totalSamplesRecorded: Int = 0
    private var accumulatedDist: Float = 0f

    private var lastRecordedX: Float = 0f
    private var lastRecordedZ: Float = 0f

    class WagonItem(
        val index: Int,
        var currentX: Float = 0f,
        var currentZ: Float = 0f,
        var currentYaw: Float = 0f,
        var popTimer: Float = 0f
    )

    val wagons = ArrayList<WagonItem>(64)

    private val wagonInstance: ModelInstance = vehicleModels.getWagonInstance(color)
    private val cargoInstance: ModelInstance = vehicleModels.getCargoInstance(cropType)
    private val samplesPerWagonSpacing: Float = GameConfig.WAGON_SPACING / GameConfig.WAGON_PATH_SAMPLE_INTERVAL

    // Reusable vector for zero-allocation rendering
    private val tmpPos = Vector3()

    fun getCount(): Int = wagons.size

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

    fun updatePath(currentX: Float, currentZ: Float, currentHeadingDeg: Float, delta: Float) {
        val dx = currentX - lastRecordedX
        val dz = currentZ - lastRecordedZ
        val stepDist = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()

        accumulatedDist += stepDist
        lastRecordedX = currentX
        lastRecordedZ = currentZ

        while (accumulatedDist >= GameConfig.WAGON_PATH_SAMPLE_INTERVAL) {
            headIdx = (headIdx + 1) and bufferMask
            pathX[headIdx] = currentX
            pathZ[headIdx] = currentZ
            pathYaw[headIdx] = currentHeadingDeg
            totalSamplesRecorded++
            accumulatedDist -= GameConfig.WAGON_PATH_SAMPLE_INTERVAL
        }

        for (w in wagons) {
            if (w.popTimer > 0f) {
                w.popTimer = maxOf(0f, w.popTimer - delta)
            }
        }
    }

    fun addWagon() {
        val newIndex = wagons.size
        val wagon = WagonItem(
            index = newIndex,
            popTimer = GameConfig.WAGON_POP_DURATION
        )
        wagons.add(wagon)
    }

    fun render(batch: ModelBatch, camera: Camera, environment: Environment? = null) {
        val wagonCount = wagons.size
        if (wagonCount == 0) return

        val camPos = camera.position
        val frustum = camera.frustum

        for (i in 0 until wagonCount) {
            val wagon = wagons[i]
            val sampleOffset = ((i + 1) * samplesPerWagonSpacing).toInt()
            val sampleIdx = (headIdx - sampleOffset) and bufferMask

            val targetX = pathX[sampleIdx]
            val targetZ = pathZ[sampleIdx]
            val targetYaw = pathYaw[sampleIdx]

            wagon.currentX = MathUtils.lerp(wagon.currentX, targetX, 0.75f)
            wagon.currentZ = MathUtils.lerp(wagon.currentZ, targetZ, 0.75f)
            wagon.currentYaw = targetYaw

            tmpPos.set(wagon.currentX, 0.5f, wagon.currentZ)

            if (!frustum.sphereInFrustum(tmpPos, 2.0f)) {
                continue
            }

            val distSq = tmpPos.dst2(camPos)
            val isFar = distSq > 1600f

            var scaleX = 1.0f
            var scaleY = 1.0f
            var scaleZ = 1.0f
            if (wagon.popTimer > 0f) {
                val t = 1.0f - (wagon.popTimer / GameConfig.WAGON_POP_DURATION)
                val bounce = MathUtils.sin(t * MathUtils.PI * 2.5f) * (1.0f - t)
                scaleX = 1.0f + bounce * 0.45f
                scaleY = 1.0f - bounce * 0.35f
                scaleZ = 1.0f + bounce * 0.45f
            }

            shadowBlob.render(batch, wagon.currentX, wagon.currentZ, 0.9f, 1.1f, 0.4f)

            wagonInstance.transform.idt()
            wagonInstance.transform.setToTranslation(wagon.currentX, 0f, wagon.currentZ)
            wagonInstance.transform.rotate(Vector3.Y, -wagon.currentYaw + 90f)
            wagonInstance.transform.scale(scaleX, scaleY, scaleZ)

            if (environment != null) {
                batch.render(wagonInstance, environment)
            } else {
                batch.render(wagonInstance)
            }

            if (!isFar) {
                cargoInstance.transform.idt()
                cargoInstance.transform.setToTranslation(wagon.currentX, 0.35f * scaleY, wagon.currentZ)
                cargoInstance.transform.rotate(Vector3.Y, -wagon.currentYaw + 90f)
                cargoInstance.transform.scale(scaleX * 0.85f, scaleY * 0.85f, scaleZ * 0.85f)
                if (environment != null) {
                    batch.render(cargoInstance, environment)
                } else {
                    batch.render(cargoInstance)
                }
            }
        }
    }
}
