package com.yt.harvestrush.config

import com.badlogic.gdx.graphics.Color

/**
 * Global game balance, tuning constants, and "NO LIMITS" rules definitions.
 */
object GameConfig {
    // === NO LIMITS RULES ===
    // Infinite trailer capacity: no slow down, no weight penalty, no capacity cap
    const val MAX_TRAILER_COUNT: Int = Int.MAX_VALUE
    const val MAX_LOAD_CAPACITY: Float = Float.MAX_VALUE
    const val SPEED_WEIGHT_PENALTY_FACTOR: Float = 0.0f // Zero penalty regardless of wagon count
    const val TURN_WEIGHT_PENALTY_FACTOR: Float = 0.0f  // Zero turn penalty
    const val MAX_CUTTING_SPEED: Float = Float.MAX_VALUE

    // === MATCH SETTINGS ===
    const val DEFAULT_MATCH_DURATION: Float = 120f // 2 minutes in seconds
    const val TIME_WARNING_THRESHOLD: Float = 10f   // Flashes red when under 10 seconds

    // === COMBINE TUNING ===
    const val COMBINE_MOVE_SPEED: Float = 7.5f     // Units per second forward
    const val COMBINE_BOOST_SPEED: Float = 10.5f
    const val COMBINE_TURN_SPEED: Float = 180f     // Degrees per second maximum turn rate
    const val COMBINE_TURN_SMOOTHING: Float = 8.0f // Lerp factor for smooth steering
    const val COMBINE_ROLL_FACTOR: Float = 0.12f   // Roll angle in radians when turning sharply
    const val HEADER_WIDTH: Float = 2.8f           // Cutting width of combine cutter bar
    const val HEADER_FORWARD_OFFSET: Float = 1.3f  // Center of cutting bar in front of combine center
    const val HEADER_DEPTH: Float = 0.7f           // Depth of cutting area box
    const val COMBINE_COLLISION_RADIUS: Float = 1.2f

    // === WAGON CHAIN TUNING ===
    const val WAGON_SPACING: Float = 1.6f          // Distance between wagons along the path
    const val WAGON_PATH_SAMPLE_INTERVAL: Float = 0.05f // Distance between recorded path samples
    const val WAGON_RING_BUFFER_SIZE: Int = 131072 // Large power-of-two ring buffer for unlimited path history
    const val WAGON_SWAY_LAG: Float = 0.18f        // Visual follow-lag inertia factor
    const val WAGON_POP_DURATION: Float = 0.35f    // Squash & stretch spring pop duration
    const val WAGON_POP_SCALE_X: Float = 1.45f     // Horizontal stretch factor on spawn
    const val WAGON_POP_SCALE_Y: Float = 0.65f     // Vertical squash factor on spawn

    // Harvest amount needed per new wagon attached
    const val CROPS_PER_WAGON: Int = 18

    // === CAMERA TUNING ===
    const val CAMERA_FOV: Float = 52f
    const val CAMERA_BASE_PITCH: Float = 55f       // High 3rd person angle in degrees
    const val CAMERA_BASE_DISTANCE: Float = 14.5f  // Base distance behind combine
    const val CAMERA_BASE_HEIGHT: Float = 13.0f    // Base height above combine
    const val CAMERA_SMOOTH_FACTOR: Float = 6.0f   // Positional damping
    const val CAMERA_LOOKAHEAD_FACTOR: Float = 1.5f// Look ahead in direction of movement
    const val CAMERA_MAX_ZOOM_OUT: Float = 8.0f    // Extra distance clamped for long wagon chains

    // === FIELD & CROPS ===
    const val FIELD_GRID_SIZE: Int = 160           // 160x160 cells = 25,600 crop cells per field
    const val CHUNK_SIZE: Int = 16                 // 10x10 chunks
    const val CROP_CELL_SIZE: Float = 0.65f        // Physical 3D size of each cell
    const val TOTAL_FIELD_WORLD_SIZE: Float = FIELD_GRID_SIZE * CROP_CELL_SIZE // ~104 units wide

    // === COLORS (Low-poly palette matching reference) ===
    val COLOR_GRASS = Color(0.48f, 0.78f, 0.28f, 1f)     // Lime green outside field
    val COLOR_DIRT = Color(0.88f, 0.60f, 0.44f, 1f)      // Peach/salmon cut dirt
    val COLOR_DIRT_PATH = Color(0.82f, 0.54f, 0.38f, 1f) // Paths cutting through fields
    val COLOR_WHEAT = Color(0.96f, 0.82f, 0.28f, 1f)     // Golden yellow
    val COLOR_CARROT = Color(0.98f, 0.50f, 0.12f, 1f)    // Bright orange
    val COLOR_CARROT_TOP = Color(0.26f, 0.72f, 0.22f, 1f)// Carrot green leaves
    val COLOR_CABBAGE = Color(0.20f, 0.55f, 0.25f, 1f)   // Dark leafy green
    val COLOR_CORN = Color(0.95f, 0.88f, 0.18f, 1f)      // Corn yellow
    val COLOR_CORN_STALK = Color(0.35f, 0.70f, 0.20f, 1f)// Stalk green
    val COLOR_WOOD = Color(0.55f, 0.35f, 0.20f, 1f)      // Wooden fences & barns
    val COLOR_BARN_RED = Color(0.80f, 0.22f, 0.20f, 1f)  // Barn red
    val COLOR_CYAN_STOPWATCH = Color(0.18f, 0.82f, 0.90f, 1f)

    // Combine player & rival colors
    val COMBINE_COLORS = listOf(
        Color(0.90f, 0.20f, 0.20f, 1f), // Red (Player default)
        Color(0.20f, 0.60f, 0.95f, 1f), // Bright Blue
        Color(0.25f, 0.80f, 0.35f, 1f), // Bright Green
        Color(0.95f, 0.40f, 0.75f, 1f), // Pink / Magenta
        Color(0.98f, 0.55f, 0.15f, 1f), // Vibrant Orange
        Color(0.65f, 0.30f, 0.90f, 1f), // Purple
        Color(0.22f, 0.24f, 0.28f, 1f)  // Black
    )

    // Bot Names Pool
    val BOT_NAMES = listOf(
        "Emma", "Cris", "Henry", "David", "Alex", "Bob", "John", "Adelle", "Xan", "Camila"
    )

    // Field Definitions & Progression Resource Requirements
    enum class CropType(val displayName: String, val color: Color) {
        WHEAT("Wheat", COLOR_WHEAT),
        CARROT("Carrot", COLOR_CARROT),
        CABBAGE("Cabbage", COLOR_CABBAGE),
        CORN("Corn", COLOR_CORN)
    }

    data class FieldInfo(
        val id: Int,
        val name: String,
        val cropType: CropType,
        val reqWheat: Int,
        val reqCarrot: Int,
        val reqCabbage: Int,
        val reqCorn: Int
    )

    val FIELDS = listOf(
        FieldInfo(0, "Sunny Wheat Field", CropType.WHEAT, 0, 0, 0, 0),             // Starting field
        FieldInfo(1, "Orange Ridge", CropType.CARROT, 150, 0, 0, 0),
        FieldInfo(2, "Verdant Cabbage", CropType.CABBAGE, 150, 130, 0, 0),
        FieldInfo(3, "Golden Corn Valley", CropType.CORN, 150, 140, 120, 0),
        FieldInfo(4, "Master Meadows", CropType.WHEAT, 250, 200, 180, 160)
    )
}
