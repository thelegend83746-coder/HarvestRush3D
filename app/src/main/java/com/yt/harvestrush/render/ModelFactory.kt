package com.yt.harvestrush.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.yt.harvestrush.config.GameConfig

/**
 * Procedural low-poly 3D mesh generator.
 * Builds all combines, trailers, crops, buildings, and environmental props in code.
 * Zero external 3D asset files required.
 * Uses Usage.ColorPacked for maximum GPU compatibility with libGDX DefaultShader.
 */
class ModelFactory {
    private val modelBuilder = ModelBuilder()
    private val createdModels = mutableListOf<Model>()

    private val attr = (Usage.Position or Usage.Normal or Usage.ColorPacked).toLong()

    /**
     * Creates a colored material with diffuse and ambient color.
     */
    private fun mat(color: Color): Material {
        return Material(
            ColorAttribute.createDiffuse(color),
            ColorAttribute.createAmbient(color.cpy().mul(0.6f, 0.6f, 0.6f, 1f))
        )
    }

    /**
     * Procedural combine harvester model with header, cab, wheels, and side marker.
     */
    fun createCombineModel(bodyColor: Color): Model {
        modelBuilder.begin()

        // 1. Main chassis / body
        val bodyBuilder = modelBuilder.part("body", GL20.GL_TRIANGLES, attr, mat(bodyColor))
        bodyBuilder.setColor(bodyColor)
        bodyBuilder.box(0f, 0.75f, 0f, 1.8f, 1.1f, 2.6f)

        // 2. Glass cabin
        val cabColor = Color(0.65f, 0.88f, 0.98f, 0.85f)
        val cabBuilder = modelBuilder.part("cab", GL20.GL_TRIANGLES, attr, mat(cabColor))
        cabBuilder.setColor(cabColor)
        cabBuilder.box(0f, 1.6f, 0.35f, 1.4f, 0.85f, 1.3f)

        // Cab roof
        val roofColor = Color(0.9f, 0.9f, 0.9f, 1f)
        val roofBuilder = modelBuilder.part("roof", GL20.GL_TRIANGLES, attr, mat(roofColor))
        roofBuilder.setColor(roofColor)
        roofBuilder.box(0f, 2.05f, 0.35f, 1.55f, 0.12f, 1.45f)

        // 3. Wide Cutter Bar / Header at front
        val headerColor = Color(0.35f, 0.38f, 0.42f, 1f)
        val headerBuilder = modelBuilder.part("header", GL20.GL_TRIANGLES, attr, mat(headerColor))
        headerBuilder.setColor(headerColor)
        headerBuilder.box(0f, 0.32f, GameConfig.HEADER_FORWARD_OFFSET, GameConfig.HEADER_WIDTH, 0.45f, 0.65f)

        // Rotating reel cylinder bar (white/silver)
        val reelColor = Color(0.92f, 0.92f, 0.95f, 1f)
        val reelBuilder = modelBuilder.part("reel", GL20.GL_TRIANGLES, attr, mat(reelColor))
        reelBuilder.setColor(reelColor)
        reelBuilder.cylinder(0.4f, GameConfig.HEADER_WIDTH * 0.96f, 0.4f, 8)

        // Red/Orange triangular marker flag on side of the header
        val markerColor = Color(0.95f, 0.25f, 0.1f, 1f)
        val markerBuilder = modelBuilder.part("marker", GL20.GL_TRIANGLES, attr, mat(markerColor))
        markerBuilder.setColor(markerColor)
        markerBuilder.cone(0.25f, 0.4f, 0.25f, 4)

        // 4. Wheels
        val tireColor = Color(0.12f, 0.13f, 0.14f, 1f)
        val rimColor = Color(0.95f, 0.95f, 0.95f, 1f)
        val tireBuilder = modelBuilder.part("tires", GL20.GL_TRIANGLES, attr, mat(tireColor))
        tireBuilder.setColor(tireColor)
        tireBuilder.cylinder(0.95f, 0.42f, 0.95f, 12)

        val rimBuilder = modelBuilder.part("rims", GL20.GL_TRIANGLES, attr, mat(rimColor))
        rimBuilder.setColor(rimColor)
        rimBuilder.cylinder(0.55f, 0.44f, 0.55f, 8)

        // 5. Rear tail lights (red)
        val lightColor = Color(1.0f, 0.1f, 0.1f, 1f)
        val lightBuilder = modelBuilder.part("lights", GL20.GL_TRIANGLES, attr, mat(lightColor))
        lightBuilder.setColor(lightColor)
        lightBuilder.box(0.65f, 0.85f, -1.32f, 0.22f, 0.15f, 0.08f)
        lightBuilder.box(-0.65f, 0.85f, -1.32f, 0.22f, 0.15f, 0.08f)

        // Exhaust pipe
        val exhaustColor = Color(0.25f, 0.25f, 0.25f, 1f)
        val exhaustBuilder = modelBuilder.part("exhaust", GL20.GL_TRIANGLES, attr, mat(exhaustColor))
        exhaustBuilder.setColor(exhaustColor)
        exhaustBuilder.cylinder(0.12f, 0.8f, 0.12f, 6)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    /**
     * Procedural 2-wheel wagon cart with drawbar.
     */
    fun createWagonCartModel(color: Color): Model {
        modelBuilder.begin()

        // Wagon tub/tray
        val bodyBuilder = modelBuilder.part("tray", GL20.GL_TRIANGLES, attr, mat(color))
        bodyBuilder.setColor(color)
        bodyBuilder.box(0f, 0.45f, 0f, 1.25f, 0.45f, 1.35f)

        // Drawbar coupling arm extending forward
        val drawbarColor = Color(0.3f, 0.3f, 0.32f, 1f)
        val barBuilder = modelBuilder.part("drawbar", GL20.GL_TRIANGLES, attr, mat(drawbarColor))
        barBuilder.setColor(drawbarColor)
        barBuilder.box(0f, 0.28f, 0.85f, 0.14f, 0.12f, 0.65f)

        // Wagon wheels
        val wheelColor = Color(0.15f, 0.15f, 0.16f, 1f)
        val wheelBuilder = modelBuilder.part("wheels", GL20.GL_TRIANGLES, attr, mat(wheelColor))
        wheelBuilder.setColor(wheelColor)
        wheelBuilder.cylinder(0.55f, 0.2f, 0.55f, 10)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createWheatCargoModel(): Model {
        modelBuilder.begin()
        val baleBuilder = modelBuilder.part("bale", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_WHEAT))
        baleBuilder.setColor(GameConfig.COLOR_WHEAT)
        baleBuilder.cylinder(0.85f, 0.95f, 0.85f, 12)

        val strapColor = Color(0.35f, 0.25f, 0.15f, 1f)
        val strapBuilder = modelBuilder.part("strap", GL20.GL_TRIANGLES, attr, mat(strapColor))
        strapBuilder.setColor(strapColor)
        strapBuilder.cylinder(0.87f, 0.14f, 0.87f, 12)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createCarrotCargoModel(): Model {
        modelBuilder.begin()
        val crateColor = GameConfig.COLOR_WOOD
        val crateBuilder = modelBuilder.part("crate", GL20.GL_TRIANGLES, attr, mat(crateColor))
        crateBuilder.setColor(crateColor)
        crateBuilder.box(0f, 0.25f, 0f, 0.95f, 0.45f, 1.05f)

        val carrotBuilder = modelBuilder.part("carrots", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_CARROT))
        carrotBuilder.setColor(GameConfig.COLOR_CARROT)
        carrotBuilder.cone(0.22f, 0.5f, 0.22f, 5)

        val greenBuilder = modelBuilder.part("tops", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_CARROT_TOP))
        greenBuilder.setColor(GameConfig.COLOR_CARROT_TOP)
        greenBuilder.cone(0.18f, 0.3f, 0.18f, 4)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createCabbageCargoModel(): Model {
        modelBuilder.begin()
        val crateBuilder = modelBuilder.part("crate", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_WOOD))
        crateBuilder.setColor(GameConfig.COLOR_WOOD)
        crateBuilder.box(0f, 0.25f, 0f, 0.95f, 0.45f, 1.05f)

        val cabbageBuilder = modelBuilder.part("heads", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_CABBAGE))
        cabbageBuilder.setColor(GameConfig.COLOR_CABBAGE)
        cabbageBuilder.box(0f, 0.45f, 0f, 0.75f, 0.45f, 0.75f)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createCornCargoModel(): Model {
        modelBuilder.begin()
        val crateBuilder = modelBuilder.part("crate", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_WOOD))
        crateBuilder.setColor(GameConfig.COLOR_WOOD)
        crateBuilder.box(0f, 0.25f, 0f, 0.95f, 0.45f, 1.05f)

        val cornBuilder = modelBuilder.part("cobs", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_CORN))
        cornBuilder.setColor(GameConfig.COLOR_CORN)
        cornBuilder.cylinder(0.24f, 0.7f, 0.24f, 6)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createFenceModel(): Model {
        modelBuilder.begin()
        val fencePart = modelBuilder.part("fence", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_WOOD))
        fencePart.setColor(GameConfig.COLOR_WOOD)
        fencePart.box(0f, 0.6f, 0f, 0.22f, 1.2f, 0.22f)
        fencePart.box(1.0f, 0.85f, 0f, 2.0f, 0.12f, 0.1f)
        fencePart.box(1.0f, 0.42f, 0f, 2.0f, 0.12f, 0.1f)
        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createBarnModel(): Model {
        modelBuilder.begin()
        val walls = modelBuilder.part("walls", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_BARN_RED))
        walls.setColor(GameConfig.COLOR_BARN_RED)
        walls.box(0f, 2.5f, 0f, 6.0f, 5.0f, 8.0f)

        val roofColor = Color(0.92f, 0.92f, 0.94f, 1f)
        val roof = modelBuilder.part("roof", GL20.GL_TRIANGLES, attr, mat(roofColor))
        roof.setColor(roofColor)
        roof.box(0f, 5.5f, 0f, 6.6f, 1.5f, 8.4f)

        val doorColor = Color(1f, 1f, 1f, 1f)
        val doors = modelBuilder.part("doors", GL20.GL_TRIANGLES, attr, mat(doorColor))
        doors.setColor(doorColor)
        doors.box(0f, 1.6f, 4.02f, 2.4f, 3.2f, 0.1f)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createFarmhouseModel(): Model {
        modelBuilder.begin()
        val houseColor = Color(0.78f, 0.65f, 0.52f, 1f)
        val walls = modelBuilder.part("house_walls", GL20.GL_TRIANGLES, attr, mat(houseColor))
        walls.setColor(houseColor)
        walls.box(0f, 2.2f, 0f, 5.2f, 4.4f, 6.2f)

        val roofColor = Color(0.38f, 0.42f, 0.46f, 1f)
        val roof = modelBuilder.part("house_roof", GL20.GL_TRIANGLES, attr, mat(roofColor))
        roof.setColor(roofColor)
        roof.box(0f, 4.8f, 0f, 5.8f, 1.4f, 6.8f)

        val brickColor = Color(0.68f, 0.28f, 0.22f, 1f)
        val chimney = modelBuilder.part("chimney", GL20.GL_TRIANGLES, attr, mat(brickColor))
        chimney.setColor(brickColor)
        chimney.box(1.5f, 5.6f, -1.2f, 0.7f, 2.2f, 0.7f)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createWindmillModel(): Model {
        modelBuilder.begin()
        val towerColor = Color(0.92f, 0.90f, 0.86f, 1f)
        val tower = modelBuilder.part("tower", GL20.GL_TRIANGLES, attr, mat(towerColor))
        tower.setColor(towerColor)
        tower.cone(2.8f, 7.5f, 2.8f, 8)

        val capColor = Color(0.35f, 0.25f, 0.18f, 1f)
        val cap = modelBuilder.part("cap", GL20.GL_TRIANGLES, attr, mat(capColor))
        cap.setColor(capColor)
        cap.box(0f, 7.5f, 0f, 1.6f, 1.1f, 1.6f)

        val bladeColor = Color(0.96f, 0.96f, 0.96f, 1f)
        val blades = modelBuilder.part("blades", GL20.GL_TRIANGLES, attr, mat(bladeColor))
        blades.setColor(bladeColor)
        blades.box(0f, 7.5f, 1.0f, 0.35f, 5.2f, 0.08f)
        blades.box(0f, 7.5f, 1.0f, 5.2f, 0.35f, 0.08f)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createTreeModel(): Model {
        modelBuilder.begin()
        val trunkColor = Color(0.42f, 0.28f, 0.16f, 1f)
        val trunk = modelBuilder.part("trunk", GL20.GL_TRIANGLES, attr, mat(trunkColor))
        trunk.setColor(trunkColor)
        trunk.box(0f, 1.2f, 0f, 0.55f, 2.4f, 0.55f)

        val leavesColor = Color(0.24f, 0.65f, 0.24f, 1f)
        val leaves = modelBuilder.part("leaves", GL20.GL_TRIANGLES, attr, mat(leavesColor))
        leaves.setColor(leavesColor)
        leaves.box(0f, 3.2f, 0f, 2.2f, 2.2f, 2.2f)

        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createHaystackModel(): Model {
        modelBuilder.begin()
        val part = modelBuilder.part("haystack", GL20.GL_TRIANGLES, attr, mat(GameConfig.COLOR_WHEAT))
        part.setColor(GameConfig.COLOR_WHEAT)
        part.cone(2.2f, 2.5f, 2.2f, 8)
        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun createPondModel(): Model {
        modelBuilder.begin()
        val pondColor = Color(0.35f, 0.72f, 0.92f, 0.9f)
        val pond = modelBuilder.part("pond", GL20.GL_TRIANGLES, attr, mat(pondColor))
        pond.setColor(pondColor)
        pond.cylinder(6.5f, 0.1f, 5.0f, 12)
        val model = modelBuilder.end()
        createdModels.add(model)
        return model
    }

    fun dispose() {
        for (m in createdModels) {
            m.dispose()
        }
        createdModels.clear()
    }
}
