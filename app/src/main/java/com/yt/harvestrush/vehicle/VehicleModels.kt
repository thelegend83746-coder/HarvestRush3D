package com.yt.harvestrush.vehicle

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.render.ModelFactory

/**
 * Manages procedural models and visual representation for combines,
 * trailer wagons, and crop cargo loads.
 */
class VehicleModels(private val modelFactory: ModelFactory) {

    private val combineModels = mutableMapOf<Color, Model>()
    private val wagonModels = mutableMapOf<Color, Model>()

    private val wheatCargo: Model by lazy { modelFactory.createWheatCargoModel() }
    private val carrotCargo: Model by lazy { modelFactory.createCarrotCargoModel() }
    private val cabbageCargo: Model by lazy { modelFactory.createCabbageCargoModel() }
    private val cornCargo: Model by lazy { modelFactory.createCornCargoModel() }

    fun getCombineInstance(color: Color): ModelInstance {
        val model = combineModels.getOrPut(color) {
            modelFactory.createCombineModel(color)
        }
        return ModelInstance(model)
    }

    fun getWagonInstance(color: Color): ModelInstance {
        val model = wagonModels.getOrPut(color) {
            modelFactory.createWagonCartModel(color)
        }
        return ModelInstance(model)
    }

    fun getCargoInstance(cropType: GameConfig.CropType): ModelInstance {
        val model = when (cropType) {
            GameConfig.CropType.WHEAT -> wheatCargo
            GameConfig.CropType.CARROT -> carrotCargo
            GameConfig.CropType.CABBAGE -> cabbageCargo
            GameConfig.CropType.CORN -> cornCargo
        }
        return ModelInstance(model)
    }

    fun dispose() {
        // Models are tracked and disposed inside ModelFactory
    }
}
