package com.yt.harvestrush.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder

/**
 * Mobile-optimized blob shadow renderer.
 * Produces soft, dark ground contact shadows under combines and wagons.
 */
class ShadowBlob {
    private val model: Model
    private val instance: ModelInstance

    init {
        val mb = ModelBuilder()
        val attr = (Usage.Position or Usage.Normal or Usage.ColorUnpacked).toLong()
        val shadowColor = Color(0.12f, 0.15f, 0.18f, 0.45f)
        val mat = Material(
            ColorAttribute.createDiffuse(shadowColor),
            BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        )

        mb.begin()
        val part = mb.part("shadow", GL20.GL_TRIANGLES, attr, mat)
        part.setColor(shadowColor)
        // Flat oval/disc lifted 0.02 units above ground to avoid z-fighting
        part.cylinder(1.0f, 0.02f, 1.0f, 12)
        model = mb.end()
        instance = ModelInstance(model)
    }

    /**
     * Renders a soft shadow disc under an entity.
     */
    fun render(batch: ModelBatch, x: Float, z: Float, radiusX: Float, radiusZ: Float, alpha: Float = 0.45f) {
        instance.transform.idt()
        instance.transform.setToTranslation(x, 0.02f, z)
        instance.transform.scale(radiusX, 1.0f, radiusZ)
        batch.render(instance)
    }

    fun dispose() {
        model.dispose()
    }
}
