package com.yt.harvestrush.field

import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.render.ModelFactory

/**
 * Builds the game environment around the crop field:
 * Wooden perimeter fences with gate, Red Barn, Farmhouse, Windmill,
 * Low-poly cubic Trees, Haystacks, and Ponds.
 */
class FieldFactory(private val modelFactory: ModelFactory) {

    // Visual model instances
    val propInstances = mutableListOf<ModelInstance>()

    // Fence collision bounding segments (minX, minZ, maxX, maxZ)
    val fenceBounds = Rectangle()
    val boundaryRadius: Float = (GameConfig.TOTAL_FIELD_WORLD_SIZE * 0.5f) - 1.5f

    init {
        fenceBounds.set(
            -boundaryRadius,
            -boundaryRadius,
            boundaryRadius * 2f,
            boundaryRadius * 2f
        )
    }

    /**
     * Spawns perimeter fence posts and rails enclosing the field.
     */
    fun buildPerimeterFences() {
        val fenceModel = modelFactory.createFenceModel()
        val halfW = boundaryRadius
        val step = 2.0f

        // North and South fence walls
        var x = -halfW
        while (x <= halfW) {
            // Leave a gate opening at the south center
            if (Math.abs(x) > 3.0f) {
                // South fence
                val instS = ModelInstance(fenceModel)
                instS.transform.setToTranslation(x, 0f, halfW)
                propInstances.add(instS)
            }
            // North fence
            val instN = ModelInstance(fenceModel)
            instN.transform.setToTranslation(x, 0f, -halfW)
            propInstances.add(instN)

            x += step
        }

        // East and West fence walls
        var z = -halfW
        while (z <= halfW) {
            val instE = ModelInstance(fenceModel)
            instE.transform.setToTranslation(halfW, 0f, z)
            instE.transform.rotate(Vector3.Y, 90f)
            propInstances.add(instE)

            val instW = ModelInstance(fenceModel)
            instW.transform.setToTranslation(-halfW, 0f, z)
            instW.transform.rotate(Vector3.Y, 90f)
            propInstances.add(instW)

            z += step
        }
    }

    /**
     * Builds farm props (barn, windmill, farmhouse, trees, pond) outside the fence.
     */
    fun buildEnvironmentProps() {
        val halfW = boundaryRadius

        // 1. Red Barn outside North-East corner
        val barn = ModelInstance(modelFactory.createBarnModel())
        barn.transform.setToTranslation(halfW + 12f, 0f, -halfW - 6f)
        barn.transform.rotate(Vector3.Y, -25f)
        propInstances.add(barn)

        // 2. Farmhouse outside North-West corner
        val house = ModelInstance(modelFactory.createFarmhouseModel())
        house.transform.setToTranslation(-halfW - 12f, 0f, -halfW - 4f)
        house.transform.rotate(Vector3.Y, 35f)
        propInstances.add(house)

        // 3. Windmill outside West fence
        val windmill = ModelInstance(modelFactory.createWindmillModel())
        windmill.transform.setToTranslation(-halfW - 15f, 0f, 6f)
        propInstances.add(windmill)

        // 4. Blue Pond outside East fence
        val pond = ModelInstance(modelFactory.createPondModel())
        pond.transform.setToTranslation(halfW + 14f, 0.05f, 10f)
        propInstances.add(pond)

        // 5. Haystacks near Barn
        val haystackModel = modelFactory.createHaystackModel()
        val h1 = ModelInstance(haystackModel)
        h1.transform.setToTranslation(halfW + 6f, 0f, -halfW + 4f)
        propInstances.add(h1)

        val h2 = ModelInstance(haystackModel)
        h2.transform.setToTranslation(halfW + 9f, 0f, -halfW + 7f)
        propInstances.add(h2)

        // 6. Perimeter Trees surrounding the farm
        val treeModel = modelFactory.createTreeModel()
        for (i in 0 until 24) {
            val angle = (i.toFloat() / 24f) * MathUtils.PI2
            val dist = halfW + MathUtils.random(8f, 22f)
            val tx = MathUtils.cos(angle) * dist
            val tz = MathUtils.sin(angle) * dist
            val tree = ModelInstance(treeModel)
            val scale = MathUtils.random(0.85f, 1.25f)
            tree.transform.setToTranslation(tx, 0f, tz)
            tree.transform.scale(scale, scale, scale)
            tree.transform.rotate(Vector3.Y, MathUtils.random(0f, 360f))
            propInstances.add(tree)
        }
    }

    fun renderProps(batch: ModelBatch) {
        for (prop in propInstances) {
            batch.render(prop)
        }
    }

    /**
     * Clamps position to keep combine within the fence boundaries with smooth slide.
     */
    fun clampToFence(pos: Vector3, radius: Float = GameConfig.COMBINE_COLLISION_RADIUS) {
        val limit = boundaryRadius - radius
        pos.x = MathUtils.clamp(pos.x, -limit, limit)
        pos.z = MathUtils.clamp(pos.z, -limit, limit)
    }

    fun dispose() {
        propInstances.clear()
    }
}
