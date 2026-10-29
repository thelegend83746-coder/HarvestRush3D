package com.yt.harvestrush.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.MathUtils
import com.yt.harvestrush.data.PlayerStore
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 100% Procedural Audio Synthesizer.
 * Synthesizes engine hum, cutter swoosh, wagon pop, UI clicks, and jingles in memory.
 * No external audio files or downloads needed.
 */
class SynthAudio(private val playerStore: PlayerStore) {

    private var engineSound: Sound? = null
    private var cutSound: Sound? = null
    private var wagonPopSound: Sound? = null
    private var clickSound: Sound? = null
    private var winSound: Sound? = null
    private var loseSound: Sound? = null

    private var engineSoundId: Long = -1L

    init {
        try {
            engineSound = createSoundFromPcm(generateEngineTone())
            cutSound = createSoundFromPcm(generateCutSwoosh())
            wagonPopSound = createSoundFromPcm(generateWagonPop())
            clickSound = createSoundFromPcm(generateClick())
            winSound = createSoundFromPcm(generateWinJingle())
            loseSound = createSoundFromPcm(generateLoseJingle())
        } catch (e: Exception) {
            Gdx.app.error("SynthAudio", "Error synthesizing audio: ${e.message}")
        }
    }

    fun playClick() {
        if (!playerStore.soundEnabled) return
        clickSound?.play(0.6f)
    }

    fun playCut() {
        if (!playerStore.soundEnabled) return
        val pitch = MathUtils.random(0.9f, 1.15f)
        cutSound?.play(0.35f, pitch, 0f)
    }

    fun playWagonPop() {
        if (!playerStore.soundEnabled) return
        val pitch = MathUtils.random(0.95f, 1.2f)
        wagonPopSound?.play(0.85f, pitch, 0f)
    }

    fun startEngine() {
        if (!playerStore.soundEnabled) return
        if (engineSoundId == -1L && engineSound != null) {
            engineSoundId = engineSound!!.loop(0.25f, 1.0f, 0f)
        }
    }

    fun updateEnginePitch(speedRatio: Float) {
        if (engineSoundId != -1L && engineSound != null) {
            val pitch = MathUtils.clamp(0.8f + speedRatio * 0.5f, 0.7f, 1.6f)
            engineSound?.setPitch(engineSoundId, pitch)
        }
    }

    fun stopEngine() {
        if (engineSoundId != -1L) {
            engineSound?.stop(engineSoundId)
            engineSoundId = -1L
        }
    }

    fun playWin() {
        if (!playerStore.soundEnabled) return
        winSound?.play(0.8f)
    }

    fun playLose() {
        if (!playerStore.soundEnabled) return
        loseSound?.play(0.7f)
    }

    // --- Procedural PCM Waveform Generators ---

    private fun generateEngineTone(): ShortArray {
        val sampleRate = 22050
        val duration = 0.5f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            // Rich multi-harmonic low rumble
            val s1 = MathUtils.sin(2f * MathUtils.PI * 65f * t)
            val s2 = MathUtils.sin(2f * MathUtils.PI * 130f * t) * 0.5f
            val s3 = MathUtils.sin(2f * MathUtils.PI * 195f * t) * 0.25f
            val noise = MathUtils.random(-0.1f, 0.1f)
            val total = (s1 + s2 + s3 + noise) * 0.45f
            samples[i] = (total * 32767).toInt().toShort()
        }
        return samples
    }

    private fun generateCutSwoosh(): ShortArray {
        val sampleRate = 22050
        val duration = 0.12f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = MathUtils.sin(t * MathUtils.PI) // bell envelope
            val noise = MathUtils.random(-1f, 1f) * 0.6f
            val tone = MathUtils.sin(2f * MathUtils.PI * 340f * (i.toFloat() / sampleRate)) * 0.4f
            val total = (noise + tone) * env * 0.7f
            samples[i] = (total * 32767).toInt().toShort()
        }
        return samples
    }

    private fun generateWagonPop(): ShortArray {
        val sampleRate = 22050
        val duration = 0.18f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = (1.0f - t) * (1.0f - t) // fast decay
            // Pitch drops rapidly from 480Hz to 160Hz for cartoon "bloop/pop"
            val freq = MathUtils.lerp(480f, 160f, t)
            val tone = MathUtils.sin(2f * MathUtils.PI * freq * (i.toFloat() / sampleRate))
            samples[i] = (tone * env * 0.85f * 32767).toInt().toShort()
        }
        return samples
    }

    private fun generateClick(): ShortArray {
        val sampleRate = 22050
        val duration = 0.04f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / numSamples
            val env = 1.0f - t
            val tone = MathUtils.sin(2f * MathUtils.PI * 880f * (i.toFloat() / sampleRate))
            samples[i] = (tone * env * 0.5f * 32767).toInt().toShort()
        }
        return samples
    }

    private fun generateWinJingle(): ShortArray {
        val sampleRate = 22050
        val duration = 0.9f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
        val noteLen = numSamples / 4
        for (i in 0 until numSamples) {
            val noteIdx = MathUtils.clamp(i / noteLen, 0, 3)
            val freq = notes[noteIdx]
            val localT = (i % noteLen).toFloat() / noteLen
            val env = 1f - localT * 0.65f
            val tone = MathUtils.sin(2f * MathUtils.PI * freq * (i.toFloat() / sampleRate))
            samples[i] = (tone * env * 0.55f * 32767).toInt().toShort()
        }
        return samples
    }

    private fun generateLoseJingle(): ShortArray {
        val sampleRate = 22050
        val duration = 0.8f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        val notes = floatArrayOf(440f, 415.3f, 392f, 349.2f) // Descending
        val noteLen = numSamples / 4
        for (i in 0 until numSamples) {
            val noteIdx = MathUtils.clamp(i / noteLen, 0, 3)
            val freq = notes[noteIdx]
            val localT = (i % noteLen).toFloat() / noteLen
            val env = 1f - localT * 0.5f
            val tone = MathUtils.sin(2f * MathUtils.PI * freq * (i.toFloat() / sampleRate))
            samples[i] = (tone * env * 0.5f * 32767).toInt().toShort()
        }
        return samples
    }

    private fun createSoundFromPcm(pcm: ShortArray): Sound {
        val sampleRate = 22050
        val byteBuf = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in pcm) {
            byteBuf.putShort(s)
        }
        val pcmBytes = byteBuf.array()

        val baos = ByteArrayOutputStream()
        val out = DataOutputStream(baos)
        // Standard RIFF / WAV Header
        out.writeBytes("RIFF")
        out.writeInt(Integer.reverseBytes(36 + pcmBytes.size))
        out.writeBytes("WAVE")
        out.writeBytes("fmt ")
        out.writeInt(Integer.reverseBytes(16)) // Subchunk1Size (16 for PCM)
        out.writeShort(java.lang.Short.reverseBytes(1)) // AudioFormat 1 = PCM
        out.writeShort(java.lang.Short.reverseBytes(1)) // NumChannels = 1
        out.writeInt(Integer.reverseBytes(sampleRate))
        out.writeInt(Integer.reverseBytes(sampleRate * 2)) // ByteRate
        out.writeShort(java.lang.Short.reverseBytes(2)) // BlockAlign
        out.writeShort(java.lang.Short.reverseBytes(16)) // BitsPerSample
        out.writeBytes("data")
        out.writeInt(Integer.reverseBytes(pcmBytes.size))
        out.write(pcmBytes)
        out.flush()

        val wavData = baos.toByteArray()
        val handle = object : FileHandle() {
            override fun read() = wavData.inputStream()
            override fun length() = wavData.size.toLong()
            override fun exists() = true
            override fun name() = "synth.wav"
        }
        return Gdx.audio.newSound(handle)
    }

    fun dispose() {
        stopEngine()
        engineSound?.dispose()
        cutSound?.dispose()
        wagonPopSound?.dispose()
        clickSound?.dispose()
        winSound?.dispose()
        loseSound?.dispose()
    }
}
