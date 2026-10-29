package com.yt.harvestrush.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "harvest_rush_prefs")

/**
 * High-performance persistence layer:
 * Uses fast synchronous SharedPreferences for zero-latency game loop lookups (no ANR, no deadlocks)
 * while synchronizing with Jetpack DataStore in the background.
 */
class PlayerStore(context: Context) {
    private val appContext = context.applicationContext ?: context
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sp: SharedPreferences = appContext.getSharedPreferences("harvest_rush_prefs", Context.MODE_PRIVATE)

    var playerName: String = sp.getString("player_name", "Bill") ?: "Bill"
        private set
    var soundEnabled: Boolean = sp.getBoolean("sound_enabled", true)
        private set
    var musicEnabled: Boolean = sp.getBoolean("music_enabled", true)
        private set
    var vibrationEnabled: Boolean = sp.getBoolean("vibration_enabled", true)
        private set
    var tapSteering: Boolean = sp.getBoolean("tap_steering", false)
        private set
    var rivalCollisions: Boolean = sp.getBoolean("rival_collisions", false)
        private set
    var graphicsQuality: String = sp.getString("graphics_quality", "High") ?: "High"
        private set

    var resWheat: Int = sp.getInt("res_wheat", 0)
        private set
    var resCarrot: Int = sp.getInt("res_carrot", 0)
        private set
    var resCabbage: Int = sp.getInt("res_cabbage", 0)
        private set
    var resCorn: Int = sp.getInt("res_corn", 0)
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
        val unlockedStr = sp.getString("unlocked_fields", "0") ?: "0"
        unlockedStr.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach {
            unlockedFields.add(it)
        }

        for (i in 0..10) {
            bestPercentages[i] = sp.getFloat("best_field_$i", 0f)
        }
    }

    fun isFieldUnlocked(fieldId: Int): Boolean = unlockedFields.contains(fieldId)

    fun unlockField(fieldId: Int) {
        unlockedFields.add(fieldId)
        val str = unlockedFields.joinToString(",")
        sp.edit().putString("unlocked_fields", str).apply()
        scope.launch {
            try {
                appContext.dataStore.edit { it[KEY_UNLOCKED_FIELDS] = str }
            } catch (e: Throwable) {}
        }
    }

    fun getBestPercent(fieldId: Int): Float = bestPercentages[fieldId] ?: 0f

    fun recordBestPercent(fieldId: Int, percent: Float) {
        val current = getBestPercent(fieldId)
        if (percent > current) {
            bestPercentages[fieldId] = percent
            sp.edit().putFloat("best_field_$fieldId", percent).apply()
            val key = floatPreferencesKey("best_field_$fieldId")
            scope.launch {
                try {
                    appContext.dataStore.edit { it[key] = percent }
                } catch (e: Throwable) {}
            }
        }
    }

    fun addResources(wheat: Int, carrot: Int, cabbage: Int, corn: Int) {
        resWheat += wheat
        resCarrot += carrot
        resCabbage += cabbage
        resCorn += corn

        sp.edit()
            .putInt("res_wheat", resWheat)
            .putInt("res_carrot", resCarrot)
            .putInt("res_cabbage", resCabbage)
            .putInt("res_corn", resCorn)
            .apply()

        scope.launch {
            try {
                appContext.dataStore.edit {
                    it[KEY_RES_WHEAT] = resWheat
                    it[KEY_RES_CARROT] = resCarrot
                    it[KEY_RES_CABBAGE] = resCabbage
                    it[KEY_RES_CORN] = resCorn
                }
            } catch (e: Throwable) {}
        }
    }

    fun spendResources(wheat: Int, carrot: Int, cabbage: Int, corn: Int): Boolean {
        if (resWheat >= wheat && resCarrot >= carrot && resCabbage >= cabbage && resCorn >= corn) {
            resWheat -= wheat
            resCarrot -= carrot
            resCabbage -= cabbage
            resCorn -= corn

            sp.edit()
                .putInt("res_wheat", resWheat)
                .putInt("res_carrot", resCarrot)
                .putInt("res_cabbage", resCabbage)
                .putInt("res_corn", resCorn)
                .apply()

            scope.launch {
                try {
                    appContext.dataStore.edit {
                        it[KEY_RES_WHEAT] = resWheat
                        it[KEY_RES_CARROT] = resCarrot
                        it[KEY_RES_CABBAGE] = resCabbage
                        it[KEY_RES_CORN] = resCorn
                    }
                } catch (e: Throwable) {}
            }
            return true
        }
        return false
    }

    fun setPlayerName(name: String) {
        playerName = name.ifBlank { "Bill" }
        sp.edit().putString("player_name", playerName).apply()
        scope.launch {
            try {
                appContext.dataStore.edit { it[KEY_PLAYER_NAME] = playerName }
            } catch (e: Throwable) {}
        }
    }

    fun setSoundEnabled(v: Boolean) {
        soundEnabled = v
        sp.edit().putBoolean("sound_enabled", v).apply()
        scope.launch {
            try { appContext.dataStore.edit { it[KEY_SOUND] = v } } catch (e: Throwable) {}
        }
    }

    fun setMusicEnabled(v: Boolean) {
        musicEnabled = v
        sp.edit().putBoolean("music_enabled", v).apply()
        scope.launch {
            try { appContext.dataStore.edit { it[KEY_MUSIC] = v } } catch (e: Throwable) {}
        }
    }

    fun setVibrationEnabled(v: Boolean) {
        vibrationEnabled = v
        sp.edit().putBoolean("vibration_enabled", v).apply()
        scope.launch {
            try { appContext.dataStore.edit { it[KEY_VIBRATION] = v } } catch (e: Throwable) {}
        }
    }

    fun setTapSteering(v: Boolean) {
        tapSteering = v
        sp.edit().putBoolean("tap_steering", v).apply()
        scope.launch {
            try { appContext.dataStore.edit { it[KEY_TAP_STEERING] = v } } catch (e: Throwable) {}
        }
    }

    fun setRivalCollisions(v: Boolean) {
        rivalCollisions = v
        sp.edit().putBoolean("rival_collisions", v).apply()
        scope.launch {
            try { appContext.dataStore.edit { it[KEY_RIVAL_COLLISIONS] = v } } catch (e: Throwable) {}
        }
    }

    fun setGraphicsQuality(q: String) {
        graphicsQuality = q
        sp.edit().putString("graphics_quality", q).apply()
        scope.launch {
            try { appContext.dataStore.edit { it[KEY_GRAPHICS] = q } } catch (e: Throwable) {}
        }
    }

    fun resetAllProgress() {
        resWheat = 0
        resCarrot = 0
        resCabbage = 0
        resCorn = 0
        unlockedFields.clear()
        unlockedFields.add(0)
        bestPercentages.clear()

        sp.edit().clear().apply()

        scope.launch {
            try {
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
            } catch (e: Throwable) {}
        }
    }
}
