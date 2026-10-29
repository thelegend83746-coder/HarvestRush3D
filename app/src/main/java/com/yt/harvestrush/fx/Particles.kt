package com.yt.harvestrush.fx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.yt.harvestrush.config.GameConfig

/**
 * High-performance pooled particle system for:
 * 1. Crop-chip bursts on cutting (yellow/orange/green)
 * 2. Exhaust puffs from combines
 * 3. Dust puffs
 * 4. Victory confetti
 */
class ParticleSystem(private val maxParticles: Int = 1024) {

    class Particle {
        var active: Boolean = false
        val pos = Vector3()
        val vel = Vector3()
        var life: Float = 0f
        var maxLife: Float = 0.5f
        var scale: Float = 0.2f
        var colorBits: Float = 0f
    }

    private val pool = Array(maxParticles) { Particle() }
    private var activeCount = 0

    private val vertices = FloatArray(maxParticles * 4 * 4)
    private val indices = ShortArray(maxParticles * 6)
    private var mesh: Mesh? = null

    // Dedicated Particle Shader Program
    private val shader: ShaderProgram

    init {
        var vIdx = 0
        var iIdx = 0
        for (i in 0 until maxParticles) {
            indices[iIdx++] = (vIdx + 0).toShort()
            indices[iIdx++] = (vIdx + 1).toShort()
            indices[iIdx++] = (vIdx + 2).toShort()
            indices[iIdx++] = (vIdx + 0).toShort()
            indices[iIdx++] = (vIdx + 2).toShort()
            indices[iIdx++] = (vIdx + 3).toShort()
            vIdx += 4
        }

        mesh = Mesh(
            false, maxParticles * 4, indices.size,
            VertexAttribute.Position(),
            VertexAttribute.ColorPacked()
        )
        mesh?.setIndices(indices)

        val vertSrc = """
            attribute vec3 a_position;
            attribute vec4 a_color;
            uniform mat4 u_projViewTrans;
            varying vec4 v_color;
            void main() {
                v_color = a_color;
                gl_Position = u_projViewTrans * vec4(a_position, 1.0);
            }
        """.trimIndent()

        val fragSrc = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;
            void main() {
                gl_FragColor = v_color;
            }
        """.trimIndent()

        shader = ShaderProgram(vertSrc, fragSrc)
        if (!shader.isCompiled) {
            Gdx.app.error("ParticleSystem", "Particle shader compilation error: " + shader.log)
        }
    }

    fun spawnCropChips(x: Float, z: Float, cropType: GameConfig.CropType) {
        val color = when (cropType) {
            GameConfig.CropType.WHEAT -> GameConfig.COLOR_WHEAT
            GameConfig.CropType.CARROT -> if (MathUtils.randomBoolean(0.6f)) GameConfig.COLOR_CARROT else GameConfig.COLOR_CARROT_TOP
            GameConfig.CropType.CABBAGE -> GameConfig.COLOR_CABBAGE
            GameConfig.CropType.CORN -> if (MathUtils.randomBoolean(0.5f)) GameConfig.COLOR_CORN else GameConfig.COLOR_CORN_STALK
        }
        val colBits = Color.toFloatBits(color.r, color.g, color.b, 0.95f)

        for (i in 0 until 4) {
            spawn(
                x = x + MathUtils.random(-0.25f, 0.25f),
                y = MathUtils.random(0.3f, 0.8f),
                z = z + MathUtils.random(-0.25f, 0.25f),
                vx = MathUtils.random(-2.5f, 2.5f),
                vy = MathUtils.random(2.0f, 4.5f),
                vz = MathUtils.random(-2.5f, 2.5f),
                life = MathUtils.random(0.25f, 0.45f),
                scale = MathUtils.random(0.12f, 0.22f),
                colBits = colBits
            )
        }
    }

    fun spawnExhaustPuff(x: Float, z: Float) {
        val colBits = Color.toFloatBits(0.85f, 0.85f, 0.88f, 0.45f)
        spawn(
            x = x,
            y = 1.1f,
            z = z,
            vx = MathUtils.random(-0.4f, 0.4f),
            vy = MathUtils.random(0.8f, 1.6f),
            vz = MathUtils.random(-0.4f, 0.4f),
            life = 0.4f,
            scale = 0.2f,
            colBits = colBits
        )
    }

    fun spawnConfetti(centerX: Float, centerZ: Float) {
        val colors = listOf(
            Color.RED, Color.YELLOW, Color.CYAN, Color.GREEN, Color.MAGENTA, Color.ORANGE
        )
        for (i in 0 until 64) {
            val c = colors[MathUtils.random(0, colors.size - 1)]
            val colBits = Color.toFloatBits(c.r, c.g, c.b, 1f)
            spawn(
                x = centerX + MathUtils.random(-4f, 4f),
                y = MathUtils.random(3f, 7f),
                z = centerZ + MathUtils.random(-4f, 4f),
                vx = MathUtils.random(-3f, 3f),
                vy = MathUtils.random(1f, 3f),
                vz = MathUtils.random(-3f, 3f),
                life = MathUtils.random(1.5f, 3.0f),
                scale = 0.25f,
                colBits = colBits
            )
        }
    }

    private fun spawn(
        x: Float, y: Float, z: Float,
        vx: Float, vy: Float, vz: Float,
        life: Float, scale: Float, colBits: Float
    ) {
        for (p in pool) {
            if (!p.active) {
                p.active = true
                p.pos.set(x, y, z)
                p.vel.set(vx, vy, vz)
                p.life = life
                p.maxLife = life
                p.scale = scale
                p.colorBits = colBits
                activeCount++
                break
            }
        }
    }

    fun update(delta: Float) {
        val gravity = -9.8f * delta
        for (p in pool) {
            if (!p.active) continue
            p.life -= delta
            if (p.life <= 0f) {
                p.active = false
                activeCount--
                continue
            }
            p.vel.y += gravity
            p.pos.add(p.vel.x * delta, p.vel.y * delta, p.vel.z * delta)
            if (p.pos.y < 0.05f) {
                p.pos.y = 0.05f
                p.vel.setZero()
            }
        }
    }

    fun render(camera: Camera) {
        if (activeCount == 0) return

        val right = Vector3(camera.up).crs(camera.direction).nor()
        val up = Vector3(camera.up).nor()

        var vIdx = 0
        var drawnParticles = 0

        for (p in pool) {
            if (!p.active) continue

            val alpha = p.life / p.maxLife
            val size = p.scale * (0.5f + alpha * 0.5f)

            val rx = right.x * size
            val ry = right.y * size
            val rz = right.z * size

            val ux = up.x * size
            val uy = up.y * size
            val uz = up.z * size

            // 4 Quad Vertices
            vertices[vIdx + 0] = p.pos.x - rx - ux
            vertices[vIdx + 1] = p.pos.y - ry - uy
            vertices[vIdx + 2] = p.pos.z - rz - uz
            vertices[vIdx + 3] = p.colorBits

            vertices[vIdx + 4] = p.pos.x + rx - ux
            vertices[vIdx + 5] = p.pos.y + ry - uy
            vertices[vIdx + 6] = p.pos.z + rz - uz
            vertices[vIdx + 7] = p.colorBits

            vertices[vIdx + 8] = p.pos.x + rx + ux
            vertices[vIdx + 9] = p.pos.y + ry + uy
            vertices[vIdx + 10] = p.pos.z + rz + uz
            vertices[vIdx + 11] = p.colorBits

            vertices[vIdx + 12] = p.pos.x - rx + ux
            vertices[vIdx + 13] = p.pos.y - ry + uy
            vertices[vIdx + 14] = p.pos.z - rz + uz
            vertices[vIdx + 15] = p.colorBits

            vIdx += 16
            drawnParticles++
            if (drawnParticles >= maxParticles) break
        }

        if (drawnParticles > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            Gdx.gl.glDepthMask(false)

            shader.bind()
            shader.setUniformMatrix("u_projViewTrans", camera.combined)

            mesh?.setVertices(vertices, 0, vIdx)
            mesh?.render(shader, GL20.GL_TRIANGLES, 0, drawnParticles * 6)

            Gdx.gl.glDepthMask(true)
        }
    }

    fun dispose() {
        mesh?.dispose()
        mesh = null
        shader.dispose()
    }
}
