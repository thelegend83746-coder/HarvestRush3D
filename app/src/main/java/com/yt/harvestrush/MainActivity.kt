package com.yt.harvestrush

import android.os.Bundle
import android.view.WindowManager
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/**
 * Main Android entry point for Harvest Rush 3D.
 * Configured for maximum compatibility across all Android devices (OpenGL ES 2.0/3.0).
 */
class MainActivity : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            // Ignore if window flags not permitted
        }

        val config = AndroidApplicationConfiguration().apply {
            useGL30 = false // GLES 2.0/3.0 universal compatibility
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
            // Do NOT set numSamples = 2, causes fatal "No EGLConfig chosen" crash on Android devices
        }

        initialize(HarvestGame(this), config)
    }
}
