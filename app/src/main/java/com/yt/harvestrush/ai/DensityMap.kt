package com.yt.harvestrush.ai

import com.badlogic.gdx.math.Vector2
import com.yt.harvestrush.field.CropGrid

/**
 * Coarse density evaluation map of uncut crop clusters.
 * Enables AI rivals to intelligently seek rich uncut patches of crops.
 */
class DensityMap(val cropGrid: CropGrid, val coarseDivisions: Int = 16) {

    private val density = IntArray(coarseDivisions * coarseDivisions)
    private val coarseStep = cropGrid.gridSize / coarseDivisions

    fun refresh() {
        density.fill(0)
        for (gz in 0 until cropGrid.gridSize) {
            val cz = gz / coarseStep
            if (cz >= coarseDivisions) continue
            for (gx in 0 until cropGrid.gridSize) {
                val cx = gx / coarseStep
                if (cx >= coarseDivisions) continue
                if (cropGrid.isCropPresent(gx, gz)) {
                    density[cz * coarseDivisions + cx]++
                }
            }
        }
    }

    /**
     * Finds the world position of the densest uncut cell near a query position.
     */
    fun findBestTargetNear(
        queryWorldX: Float,
        queryWorldZ: Float,
        searchRadiusUnits: Float,
        outTarget: Vector2
    ): Boolean {
        var maxCount = -1
        var bestWX = queryWorldX
        var bestWZ = queryWorldZ

        for (cz in 0 until coarseDivisions) {
            for (cx in 0 until coarseDivisions) {
                val count = density[cz * coarseDivisions + cx]
                if (count > maxCount && count > 0) {
                    val midGX = cx * coarseStep + (coarseStep / 2)
                    val midGZ = cz * coarseStep + (coarseStep / 2)
                    val wx = cropGrid.gridToWorldX(midGX)
                    val wz = cropGrid.gridToWorldZ(midGZ)

                    val dx = wx - queryWorldX
                    val dz = wz - queryWorldZ
                    val dstSq = dx * dx + dz * dz

                    if (dstSq <= searchRadiusUnits * searchRadiusUnits || maxCount == -1) {
                        maxCount = count
                        bestWX = wx
                        bestWZ = wz
                    }
                }
            }
        }

        if (maxCount > 0) {
            outTarget.set(bestWX, bestWZ)
            return true
        }
        return false
    }
}
