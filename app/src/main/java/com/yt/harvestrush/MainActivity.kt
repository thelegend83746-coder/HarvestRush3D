package com.yt.harvestrush

import android.os.Bundle
import android.view.WindowManager
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

/**
 * Main Android entry point for Harvest Rush 3D.
 * Configures libGDX with OpenGL ES 3.0 and full-screen immersive mode.
 */
class MainActivity : AndroidApplication() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val config = AndroidApplicationConfiguration().apply {
            useGL30 = true
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
            numSamples = 2 // 2x MSAA for crisp low-poly edges
        }

        initialize(HarvestGame(this), config)
    }
}
