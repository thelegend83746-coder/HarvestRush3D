package com.yt.harvestrush.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.math.MathUtils
import com.yt.harvestrush.data.PlayerStore
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 100% Procedural Audio Synthesizer.
 * Synthesizes engine hum, cutter swoosh, wagon pop, UI clicks, and jingles in memory.
 * Saves to local cache files so Android's SoundPool can load them with zero crash.
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
            engineSound = createSoundFromPcm("engine", generateEngineTone())
            cutSound = createSoundFromPcm("cut", generateCutSwoosh())
            wagonPopSound = createSoundFromPcm("pop", generateWagonPop())
            clickSound = createSoundFromPcm("click", generateClick())
            winSound = createSoundFromPcm("win", generateWinJingle())
            loseSound = createSoundFromPcm("lose", generateLoseJingle())
        } catch (e: Throwable) {
            Gdx.app.error("SynthAudio", "Error initializing audio: ${e.message}")
        }
    }

    fun playClick() {
        if (!playerStore.soundEnabled) return
        try {
            clickSound?.play(0.6f)
        } catch (e: Throwable) {}
    }

    fun playCut() {
        if (!playerStore.soundEnabled) return
        try {
            val pitch = MathUtils.random(0.9f, 1.15f)
            cutSound?.play(0.35f, pitch, 0f)
        } catch (e: Throwable) {}
    }

    fun playWagonPop() {
        if (!playerStore.soundEnabled) return
        try {
            val pitch = MathUtils.random(0.95f, 1.2f)
            wagonPopSound?.play(0.85f, pitch, 0f)
        } catch (e: Throwable) {}
    }

    fun startEngine() {
        if (!playerStore.soundEnabled) return
        try {
            if (engineSoundId == -1L && engineSound != null) {
                engineSoundId = engineSound!!.loop(0.25f, 1.0f, 0f)
            }
        } catch (e: Throwable) {}
    }

    fun updateEnginePitch(speedRatio: Float) {
        try {
            if (engineSoundId != -1L && engineSound != null) {
                val pitch = MathUtils.clamp(0.8f + speedRatio * 0.5f, 0.7f, 1.6f)
                engineSound?.setPitch(engineSoundId, pitch)
            }
        } catch (e: Throwable) {}
    }

    fun stopEngine() {
        try {
            if (engineSoundId != -1L) {
                engineSound?.stop(engineSoundId)
                engineSoundId = -1L
            }
        } catch (e: Throwable) {}
    }

    fun playWin() {
        if (!playerStore.soundEnabled) return
        try {
            winSound?.play(0.8f)
        } catch (e: Throwable) {}
    }

    fun playLose() {
        if (!playerStore.soundEnabled) return
        try {
            loseSound?.play(0.7f)
        } catch (e: Throwable) {}
    }

    private fun generateEngineTone(): ShortArray {
        val sampleRate = 22050
        val duration = 0.5f
        val numSamples = (sampleRate * duration).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
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
            val env = MathUtils.sin(t * MathUtils.PI)
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
            val env = (1.0f - t) * (1.0f - t)
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
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
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
        val notes = floatArrayOf(440f, 415.3f, 392f, 349.2f)
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

    private fun writeShortLE(out: DataOutputStream, value: Int) {
        out.writeByte(value and 0xFF)
        out.writeByte((value shr 8) and 0xFF)
    }

    private fun writeIntLE(out: DataOutputStream, value: Int) {
        out.writeByte(value and 0xFF)
        out.writeByte((value shr 8) and 0xFF)
        out.writeByte((value shr 16) and 0xFF)
        out.writeByte((value shr 24) and 0xFF)
    }

    private fun createSoundFromPcm(name: String, pcm: ShortArray): Sound? {
        return try {
            val sampleRate = 22050
            val byteBuf = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm) {
                byteBuf.putShort(s)
            }
            val pcmBytes = byteBuf.array()

            val baos = ByteArrayOutputStream()
            val out = DataOutputStream(baos)
            out.writeBytes("RIFF")
            writeIntLE(out, 36 + pcmBytes.size)
            out.writeBytes("WAVE")
            out.writeBytes("fmt ")
            writeIntLE(out, 16)
            writeShortLE(out, 1)
            writeShortLE(out, 1)
            writeIntLE(out, sampleRate)
            writeIntLE(out, sampleRate * 2)
            writeShortLE(out, 2)
            writeShortLE(out, 16)
            out.writeBytes("data")
            writeIntLE(out, pcmBytes.size)
            out.write(pcmBytes)
            out.flush()

            val wavData = baos.toByteArray()
            val fileHandle = Gdx.files.local("synth_$name.wav")
            fileHandle.writeBytes(wavData, false)
            Gdx.audio.newSound(fileHandle)
        } catch (e: Throwable) {
            Gdx.app.error("SynthAudio", "Could not load sound $name: ${e.message}")
            null
        }
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
