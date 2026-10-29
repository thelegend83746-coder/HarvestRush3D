package com.yt.harvestrush.field

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.yt.harvestrush.config.GameConfig
import java.util.BitSet

/**
 * High-performance 2D grid storing 25,600+ crop cell states.
 * Uses BitSets for memory efficiency and chunk dirtiness tracking
 * to eliminate GC pressure and achieve 60 FPS.
 */
class CropGrid(
    val gridSize: Int = GameConfig.FIELD_GRID_SIZE,
    val cellSize: Float = GameConfig.CROP_CELL_SIZE,
    val chunkSize: Int = GameConfig.CHUNK_SIZE
) {
    val totalCells: Int = gridSize * gridSize
    val numChunksX: Int = (gridSize + chunkSize - 1) / chunkSize
    val numChunksZ: Int = (gridSize + chunkSize - 1) / chunkSize

    // BitSet: true = crop is present and ready to harvest, false = cut or dirt path
    val cropsBitSet = BitSet(totalCells)
    // BitSet: true = path/road cutting through field (never has crops)
    val pathsBitSet = BitSet(totalCells)

    // Tracks which chunks need instance mesh rebuilds
    val dirtyChunks = BooleanArray(numChunksX * numChunksZ)

    var totalActiveCrops: Int = 0
        private set
    var initialCropCount: Int = 0
        private set

    // Harvest counts per harvester (0 = player, 1..3 = bots)
    private val harvesterCutCounts = IntArray(8)

    // Temp vectors for cutting test without allocations
    private val p1 = Vector2()
    private val p2 = Vector2()
    private val p3 = Vector2()
    private val p4 = Vector2()
    private val pt = Vector2()

    fun initGrid(generatePaths: Boolean = true) {
        cropsBitSet.clear()
        pathsBitSet.clear()
        harvesterCutCounts.fill(0)

        // Generate natural curved / cross dirt paths cutting through the field
        if (generatePaths) {
            val center = gridSize / 2
            for (z in 0 until gridSize) {
                for (x in 0 until gridSize) {
                    val idx = z * gridSize + x
                    // Cross paths + gentle winding S-curve
                    val pathWidth = 4
                    val curve = (MathUtils.sin(z * 0.08f) * 12f).toInt()
                    val isPath = (Math.abs(x - (center + curve)) <= pathWidth / 2) ||
                            (Math.abs(z - center) <= pathWidth / 2)
                    if (isPath) {
                        pathsBitSet.set(idx, true)
                    }
                }
            }
        }

        // Fill remaining field with crops
        var count = 0
        for (i in 0 until totalCells) {
            if (!pathsBitSet.get(i)) {
                cropsBitSet.set(i, true)
                count++
            }
        }
        totalActiveCrops = count
        initialCropCount = count

        // Mark all chunks dirty initially
        dirtyChunks.fill(true)
    }

    inline fun getIndex(x: Int, z: Int): Int = z * gridSize + x

    fun isCropPresent(x: Int, z: Int): Boolean {
        if (x !in 0 until gridSize || z !in 0 until gridSize) return false
        return cropsBitSet.get(getIndex(x, z))
    }

    fun isPath(x: Int, z: Int): Boolean {
        if (x !in 0 until gridSize || z !in 0 until gridSize) return false
        return pathsBitSet.get(getIndex(x, z))
    }

    fun markChunkDirty(cx: Int, cz: Int) {
        if (cx in 0 until numChunksX && cz in 0 until numChunksZ) {
            dirtyChunks[cz * numChunksX + cx] = true
        }
    }

    /**
     * Converts world XZ coordinate to grid cell coordinate.
     */
    fun worldToGridX(worldX: Float): Int {
        val halfSize = (gridSize * cellSize) * 0.5f
        return MathUtils.floor((worldX + halfSize) / cellSize)
    }

    fun worldToGridZ(worldZ: Float): Int {
        val halfSize = (gridSize * cellSize) * 0.5f
        return MathUtils.floor((worldZ + halfSize) / cellSize)
    }

    fun gridToWorldX(gx: Int): Float {
        val halfSize = (gridSize * cellSize) * 0.5f
        return gx * cellSize - halfSize + cellSize * 0.5f
    }

    fun gridToWorldZ(gz: Int): Float {
        val halfSize = (gridSize * cellSize) * 0.5f
        return gz * cellSize - halfSize + cellSize * 0.5f
    }

    /**
     * Sweeps an oriented rectangular cutter bar against the crop grid.
     * Removes cut crops, marks dirty chunks, and increments the harvester's cut count.
     * @return Number of crops cut in this step.
     */
    fun cutHeaderArea(
        harvesterId: Int,
        centerX: Float,
        centerZ: Float,
        headingRad: Float,
        width: Float,
        depth: Float,
        onCropCut: ((worldX: Float, worldZ: Float) -> Unit)? = null
    ): Int {
        val cos = MathUtils.cos(headingRad)
        val sin = MathUtils.sin(headingRad)

        val halfW = width * 0.5f
        val halfD = depth * 0.5f

        // Four corners of header rectangle
        val rX = -sin * halfW
        val rZ = cos * halfW
        val fX = cos * halfD
        val fZ = sin * halfD

        p1.set(centerX + fX + rX, centerZ + fZ + rZ)
        p2.set(centerX + fX - rX, centerZ + fZ - rZ)
        p3.set(centerX - fX - rX, centerZ - fZ - rZ)
        p4.set(centerX - fX + rX, centerZ - fZ + rZ)

        // Axis-aligned bounding box for grid lookup
        val minWorldX = minOf(p1.x, p2.x, p3.x, p4.x)
        val maxWorldX = maxOf(p1.x, p2.x, p3.x, p4.x)
        val minWorldZ = minOf(p1.y, p2.y, p3.y, p4.y)
        val maxWorldZ = maxOf(p1.y, p2.y, p3.y, p4.y)

        val minGX = MathUtils.clamp(worldToGridX(minWorldX), 0, gridSize - 1)
        val maxGX = MathUtils.clamp(worldToGridX(maxWorldX), 0, gridSize - 1)
        val minGZ = MathUtils.clamp(worldToGridZ(minWorldZ), 0, gridSize - 1)
        val maxGZ = MathUtils.clamp(worldToGridZ(maxWorldZ), 0, gridSize - 1)

        var cutCount = 0

        for (gz in minGZ..maxGZ) {
            val cz = gz / chunkSize
            for (gx in minGX..maxGX) {
                val idx = getIndex(gx, gz)
                if (cropsBitSet.get(idx)) {
                    val wx = gridToWorldX(gx)
                    val wz = gridToWorldZ(gz)
                    pt.set(wx, wz)

                    // Point inside oriented rectangle test
                    if (isPointInOrientedRect(pt, centerX, centerZ, cos, sin, halfW, halfD)) {
                        cropsBitSet.set(idx, false)
                        totalActiveCrops--
                        cutCount++
                        if (harvesterId in harvesterCutCounts.indices) {
                            harvesterCutCounts[harvesterId]++
                        }

                        val cx = gx / chunkSize
                        markChunkDirty(cx, cz)

                        onCropCut?.invoke(wx, wz)
                    }
                }
            }
        }

        return cutCount
    }

    private fun isPointInOrientedRect(
        p: Vector2,
        cx: Float,
        cz: Float,
        cos: Float,
        sin: Float,
        halfW: Float,
        halfD: Float
    ): Boolean {
        // Transform point into local space of rectangle
        val dx = p.x - cx
        val dz = p.y - cz
        val localX = dx * sin - dz * cos
        val localZ = dx * cos + dz * sin
        return (Math.abs(localX) <= halfW && Math.abs(localZ) <= halfD)
    }

    fun getCutPercentage(harvesterId: Int): Float {
        if (initialCropCount == 0) return 0f
        val cuts = if (harvesterId in harvesterCutCounts.indices) harvesterCutCounts[harvesterId] else 0
        return (cuts.toFloat() / initialCropCount) * 100f
    }

    fun getTotalHarvestedPercentage(): Float {
        if (initialCropCount == 0) return 0f
        val cutTotal = initialCropCount - totalActiveCrops
        return (cutTotal.toFloat() / initialCropCount) * 100f
    }
}
