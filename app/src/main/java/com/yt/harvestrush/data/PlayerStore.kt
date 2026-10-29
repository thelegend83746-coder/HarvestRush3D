package com.yt.harvestrush.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "harvest_rush_prefs")

/**
 * High-performance persistence layer using Jetpack DataStore.
 * Synchronously caches state for zero-latency lookups on the 60 FPS libGDX game loop.
 */
class PlayerStore(private val context: Context) {
    private val appContext = context.applicationContext ?: context
    private val scope = CoroutineScope(Dispatchers.IO)

    var playerName: String = "Bill"
        private set
    var soundEnabled: Boolean = true
        private set
    var musicEnabled: Boolean = true
        private set
    var vibrationEnabled: Boolean = true
        private set
    var tapSteering: Boolean = false
        private set
    var rivalCollisions: Boolean = false
        private set
    var graphicsQuality: String = "High"
        private set

    var resWheat: Int = 0
        private set
    var resCarrot: Int = 0
        private set
    var resCabbage: Int = 0
        private set
    var resCorn: Int = 0
        private set

    private val unlockedFields = mutableSetOf(0)
    private val bestPercentages = mutableMapOf<Int, Float>()

    companion object {
        val KEY_PLAYER_NAME = stringPreferencesKey("player_name")
        val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        val KEY_MUSIC = booleanPreferencesKey("music_enabled")
        val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
        val KEY_TAP_STEERING = booleanPreferencesKey("tap_steering")
        val KEY_RIVAL_COLLISIONS = booleanPreferencesKey("rival_collisions")
        val KEY_GRAPHICS = stringPreferencesKey("graphics_quality")

        val KEY_RES_WHEAT = intPreferencesKey("res_wheat")
        val KEY_RES_CARROT = intPreferencesKey("res_carrot")
        val KEY_RES_CABBAGE = intPreferencesKey("res_cabbage")
        val KEY_RES_CORN = intPreferencesKey("res_corn")

        val KEY_UNLOCKED_FIELDS = stringPreferencesKey("unlocked_fields")
    }

    init {
        try {
            runBlocking(Dispatchers.IO) {
                val prefs = appContext.dataStore.data.first()
                playerName = prefs[KEY_PLAYER_NAME] ?: "Bill"
                soundEnabled = prefs[KEY_SOUND] ?: true
                musicEnabled = prefs[KEY_MUSIC] ?: true
                vibrationEnabled = prefs[KEY_VIBRATION] ?: true
                tapSteering = prefs[KEY_TAP_STEERING] ?: false
                rivalCollisions = prefs[KEY_RIVAL_COLLISIONS] ?: false
                graphicsQuality = prefs[KEY_GRAPHICS] ?: "High"

                resWheat = prefs[KEY_RES_WHEAT] ?: 0
                resCarrot = prefs[KEY_RES_CARROT] ?: 0
                resCabbage = prefs[KEY_RES_CABBAGE] ?: 0
                resCorn = prefs[KEY_RES_CORN] ?: 0

                val unlockedStr = prefs[KEY_UNLOCKED_FIELDS] ?: "0"
                unlockedStr.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach {
                    unlockedFields.add(it)
                }

                for (i in 0..10) {
                    val key = floatPreferencesKey("best_field_$i")
                    bestPercentages[i] = prefs[key] ?: 0f
                }
            }
        } catch (e: Exception) {
            // Safe fallback if dataStore isn't ready
        }
    }

    fun isFieldUnlocked(fieldId: Int): Boolean = unlockedFields.contains(fieldId)

    fun unlockField(fieldId: Int) {
        unlockedFields.add(fieldId)
        val str = unlockedFields.joinToString(",")
        scope.launch {
            appContext.dataStore.edit { it[KEY_UNLOCKED_FIELDS] = str }
        }
    }

    fun getBestPercent(fieldId: Int): Float = bestPercentages[fieldId] ?: 0f

    fun recordBestPercent(fieldId: Int, percent: Float) {
        val current = getBestPercent(fieldId)
        if (percent > current) {
            bestPercentages[fieldId] = percent
            val key = floatPreferencesKey("best_field_$fieldId")
            scope.launch {
                appContext.dataStore.edit { it[key] = percent }
            }
        }
    }

    fun addResources(wheat: Int, carrot: Int, cabbage: Int, corn: Int) {
        resWheat += wheat
        resCarrot += carrot
        resCabbage += cabbage
        resCorn += corn

        scope.launch {
            appContext.dataStore.edit {
                it[KEY_RES_WHEAT] = resWheat
                it[KEY_RES_CARROT] = resCarrot
                it[KEY_RES_CABBAGE] = resCabbage
                it[KEY_RES_CORN] = resCorn
            }
        }
    }

    fun spendResources(wheat: Int, carrot: Int, cabbage: Int, corn: Int): Boolean {
        if (resWheat >= wheat && resCarrot >= carrot && resCabbage >= cabbage && resCorn >= corn) {
            resWheat -= wheat
            resCarrot -= carrot
            resCabbage -= cabbage
            resCorn -= corn
            scope.launch {
                appContext.dataStore.edit {
                    it[KEY_RES_WHEAT] = resWheat
                    it[KEY_RES_CARROT] = resCarrot
                    it[KEY_RES_CABBAGE] = resCabbage
                    it[KEY_RES_CORN] = resCorn
                }
            }
            return true
        }
        return false
    }

    fun setPlayerName(name: String) {
        playerName = name.ifBlank { "Bill" }
        scope.launch {
            appContext.dataStore.edit { it[KEY_PLAYER_NAME] = playerName }
        }
    }

    fun setSoundEnabled(v: Boolean) {
        soundEnabled = v
        scope.launch { appContext.dataStore.edit { it[KEY_SOUND] = v } }
    }

    fun setMusicEnabled(v: Boolean) {
        musicEnabled = v
        scope.launch { appContext.dataStore.edit { it[KEY_MUSIC] = v } }
    }

    fun setVibrationEnabled(v: Boolean) {
        vibrationEnabled = v
        scope.launch { appContext.dataStore.edit { it[KEY_VIBRATION] = v } }
    }

    fun setTapSteering(v: Boolean) {
        tapSteering = v
        scope.launch { appContext.dataStore.edit { it[KEY_TAP_STEERING] = v } }
    }

    fun setRivalCollisions(v: Boolean) {
        rivalCollisions = v
        scope.launch { appContext.dataStore.edit { it[KEY_RIVAL_COLLISIONS] = v } }
    }

    fun setGraphicsQuality(q: String) {
        graphicsQuality = q
        scope.launch { appContext.dataStore.edit { it[KEY_GRAPHICS] = q } }
    }

    fun resetAllProgress() {
        resWheat = 0
        resCarrot = 0
        resCabbage = 0
        resCorn = 0
        unlockedFields.clear()
        unlockedFields.add(0)
        bestPercentages.clear()

        scope.launch {
            appContext.dataStore.edit {
                it[KEY_RES_WHEAT] = 0
                it[KEY_RES_CARROT] = 0
                it[KEY_RES_CABBAGE] = 0
                it[KEY_RES_CORN] = 0
                it[KEY_UNLOCKED_FIELDS] = "0"
                for (i in 0..10) {
                    it.remove(floatPreferencesKey("best_field_$i"))
                }
            }
        }
    }
}
