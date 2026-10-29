package com.yt.harvestrush.ai

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.field.FieldFactory
import com.yt.harvestrush.vehicle.Combine

/**
 * Intelligent bot steering AI for rival combines.
 * Drives lawnmower rows, seeks dense uncut crops, slides along fences,
 * detects being stuck, and introduces human-like steering behaviors.
 */
class BotController(
    val combine: Combine,
    val densityMap: DensityMap,
    val fieldFactory: FieldFactory,
    val difficulty: Int = 1 // 0 = Easy, 1 = Normal, 2 = Hard
) {
    enum class State {
        HARVESTING,
        AVOIDING_FENCE,
        UNSTICKING
    }

    private var state = State.HARVESTING
    private var stateTimer = 0f

    // Stuck detection
    private var lastX = 0f
    private var lastZ = 0f
    private var stuckCheckTimer = 0f

    // Target waypoint
    private val currentTarget = Vector2()
    private var hasTarget = false

    // Lawnmower row direction (-1 or 1)
    private var rowDirection = 1f
    private var rowTimer = 0f

    init {
        lastX = combine.position.x
        lastZ = combine.position.z
    }

    fun update(delta: Float) {
        stateTimer += delta
        stuckCheckTimer += delta
        rowTimer += delta

        // Stuck Detector: check distance every 0.6 seconds
        if (stuckCheckTimer > 0.6f) {
            val dx = combine.position.x - lastX
            val dz = combine.position.z - lastZ
            val dist = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()

            if (dist < 0.25f && state != State.UNSTICKING) {
                // Stuck! Initiate reverse-turn unstick maneuver
                state = State.UNSTICKING
                stateTimer = 0f
                combine.turn(140f, delta)
            }

            lastX = combine.position.x
            lastZ = combine.position.z
            stuckCheckTimer = 0f
        }

        // State Machine
        when (state) {
            State.UNSTICKING -> {
                // Turn sharply away
                combine.turn(160f, delta)
                if (stateTimer > 0.75f) {
                    state = State.HARVESTING
                    stateTimer = 0f
                }
            }

            State.AVOIDING_FENCE -> {
                // Steer towards center of field (0,0)
                val angleToCenter = MathUtils.atan2(-combine.position.z, -combine.position.x)
                combine.setSteeringTarget(angleToCenter)

                val distFromCenter = combine.position.len()
                if (distFromCenter < fieldFactory.boundaryRadius * 0.75f || stateTimer > 1.5f) {
                    state = State.HARVESTING
                    stateTimer = 0f
                }
            }

            State.HARVESTING -> {
                // 1. Check fence proximity
                val fenceLimit = fieldFactory.boundaryRadius - 4.5f
                if (Math.abs(combine.position.x) > fenceLimit || Math.abs(combine.position.z) > fenceLimit) {
                    state = State.AVOIDING_FENCE
                    stateTimer = 0f
                    return
                }

                // 2. Refresh target waypoint if needed
                if (!hasTarget || rowTimer > 4.0f) {
                    val searchRadius = when (difficulty) {
                        0 -> 25f
                        1 -> 45f
                        else -> 75f
                    }
                    if (densityMap.findBestTargetNear(combine.position.x, combine.position.z, searchRadius, currentTarget)) {
                        hasTarget = true
                    } else {
                        // Lawnmower pattern fallback
                        rowDirection = -rowDirection
                        currentTarget.set(
                            combine.position.x + rowDirection * 20f,
                            combine.position.z + MathUtils.random(-10f, 10f)
                        )
                        hasTarget = true
                    }
                    rowTimer = 0f
                }

                // 3. Steer towards target waypoint with human-like noise
                val targetAngle = MathUtils.atan2(
                    currentTarget.y - combine.position.z,
                    currentTarget.x - combine.position.x
                )

                // Human imperfection noise
                val noise = if (difficulty == 0) MathUtils.sin(stateTimer * 4f) * 0.15f else 0f
                combine.setSteeringTarget(targetAngle + noise)

                // Check arrival at waypoint
                val distSq = currentTarget.dst2(combine.position.x, combine.position.z)
                if (distSq < 16f) {
                    hasTarget = false
                }
            }
        }
    }
}
