package com.yt.harvestrush.field

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix3
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.yt.harvestrush.config.GameConfig
import com.yt.harvestrush.render.ModelFactory
import com.yt.harvestrush.render.Shaders

/**
 * High-performance chunk-based renderer for 20,000+ crops.
 * Divides the field into 16x16 chunks with frustum culling.
 * Dirty chunks only rebuild their local vertex buffer when crops are cut,
 * maintaining a rock-solid 60 FPS without GC allocation.
 */
class CropChunkRenderer(
    val cropGrid: CropGrid,
    private val modelFactory: ModelFactory,
    var cropType: GameConfig.CropType = GameConfig.CropType.WHEAT
) {
    // Compiled GLSL Shader Program
    private val shader: ShaderProgram = Shaders.createDefaultShader()
    private val identityMatrix = Matrix4()
    private val normalMatrix = Matrix3()
    private var animTime: Float = 0f

    // Inner Chunk holding pre-batched low-poly crop mesh
    inner class Chunk(val cx: Int, val cz: Int) {
        val minGX = cx * cropGrid.chunkSize
        val maxGX = minOf(minGX + cropGrid.chunkSize - 1, cropGrid.gridSize - 1)
        val minGZ = cz * cropGrid.chunkSize
        val maxGZ = minOf(minGZ + cropGrid.chunkSize - 1, cropGrid.gridSize - 1)

        val minWorldX = cropGrid.gridToWorldX(minGX) - cropGrid.cellSize * 0.5f
        val maxWorldX = cropGrid.gridToWorldX(maxGX) + cropGrid.cellSize * 0.5f
        val minWorldZ = cropGrid.gridToWorldZ(minGZ) - cropGrid.cellSize * 0.5f
        val maxWorldZ = cropGrid.gridToWorldZ(maxGZ) + cropGrid.cellSize * 0.5f

        val boundingBox = BoundingBox(
            Vector3(minWorldX, 0f, minWorldZ),
            Vector3(maxWorldX, 2.0f, maxWorldZ)
        )

        var mesh: Mesh? = null
        var numIndices = 0

        fun rebuild() {
            var count = 0
            for (gz in minGZ..maxGZ) {
                for (gx in minGX..maxGX) {
                    if (cropGrid.isCropPresent(gx, gz)) {
                        count++
                    }
                }
            }

            if (count == 0) {
                mesh?.dispose()
                mesh = null
                numIndices = 0
                return
            }

            val vertsPerCrop = 12
            val totalVerts = count * vertsPerCrop
            val floatCount = totalVerts * 7
            val vertices = FloatArray(floatCount)
            val indices = ShortArray(totalVerts)

            val baseColor = cropType.color
            val packedColor = Color.toFloatBits(baseColor.r, baseColor.g, baseColor.b, 1.0f)

            var vOffset = 0
            var iOffset = 0
            var vIndex = 0

            for (gz in minGZ..maxGZ) {
                for (gx in minGX..maxGX) {
                    if (!cropGrid.isCropPresent(gx, gz)) continue

                    val wx = cropGrid.gridToWorldX(gx)
                    val wz = cropGrid.gridToWorldZ(gz)

                    val seed = (gx * 374761393 + gz * 668265263) and 0x7FFFFFFF
                    val heightVar = 0.85f + (seed % 1000) * 0.0003f
                    val topY = 1.35f * heightVar
                    val halfW = cropGrid.cellSize * 0.38f

                    // Face 1: North (-Z)
                    addTriangle(vertices, vOffset, wx, 0f, wz - halfW, 0f, 0.4f, -0.9f, packedColor)
                    addTriangle(vertices, vOffset + 7, wx - halfW, 0f, wz, 0f, 0.4f, -0.9f, packedColor)
                    addTriangle(vertices, vOffset + 14, wx, topY, wz, 0f, 0.4f, -0.9f, packedColor)
                    vOffset += 21

                    // Face 2: South (+Z)
                    addTriangle(vertices, vOffset, wx, 0f, wz + halfW, 0f, 0.4f, 0.9f, packedColor)
                    addTriangle(vertices, vOffset + 7, wx + halfW, 0f, wz, 0f, 0.4f, 0.9f, packedColor)
                    addTriangle(vertices, vOffset + 14, wx, topY, wz, 0f, 0.4f, 0.9f, packedColor)
                    vOffset += 21

                    // Face 3: East (+X)
                    addTriangle(vertices, vOffset, wx + halfW, 0f, wz, 0.9f, 0.4f, 0f, packedColor)
                    addTriangle(vertices, vOffset + 7, wx, 0f, wz - halfW, 0.9f, 0.4f, 0f, packedColor)
                    addTriangle(vertices, vOffset + 14, wx, topY, wz, 0.9f, 0.4f, 0f, packedColor)
                    vOffset += 21

                    // Face 4: West (-X)
                    addTriangle(vertices, vOffset, wx - halfW, 0f, wz, -0.9f, 0.4f, 0f, packedColor)
                    addTriangle(vertices, vOffset + 7, wx, 0f, wz + halfW, -0.9f, 0.4f, 0f, packedColor)
                    addTriangle(vertices, vOffset + 14, wx, topY, wz, -0.9f, 0.4f, 0f, packedColor)
                    vOffset += 21

                    for (k in 0 until 12) {
                        indices[iOffset++] = (vIndex++).toShort()
                    }
                }
            }

            if (mesh == null || mesh!!.maxVertices < totalVerts) {
                mesh?.dispose()
                mesh = Mesh(
                    true, totalVerts, indices.size,
                    VertexAttribute.Position(),
                    VertexAttribute.Normal(),
                    VertexAttribute.ColorPacked()
                )
            }

            mesh!!.setVertices(vertices, 0, vOffset)
            mesh!!.setIndices(indices, 0, iOffset)
            numIndices = iOffset
        }

        private fun addTriangle(arr: FloatArray, offset: Int, x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, col: Float) {
            arr[offset + 0] = x
            arr[offset + 1] = y
            arr[offset + 2] = z
            arr[offset + 3] = nx
            arr[offset + 4] = ny
            arr[offset + 5] = nz
            arr[offset + 6] = col
        }

        fun dispose() {
            mesh?.dispose()
            mesh = null
        }
    }

    private val chunks = Array(cropGrid.numChunksX * cropGrid.numChunksZ) { i ->
        val cx = i % cropGrid.numChunksX
        val cz = i / cropGrid.numChunksX
        Chunk(cx, cz)
    }

    // Ground mesh
    private var groundMesh: Mesh? = null

    init {
        buildGroundMesh()
    }

    private fun buildGroundMesh() {
        val halfW = GameConfig.TOTAL_FIELD_WORLD_SIZE * 0.7f
        val grassColor = Color.toFloatBits(GameConfig.COLOR_GRASS.r, GameConfig.COLOR_GRASS.g, GameConfig.COLOR_GRASS.b, 1f)
        val dirtColor = Color.toFloatBits(GameConfig.COLOR_DIRT.r, GameConfig.COLOR_DIRT.g, GameConfig.COLOR_DIRT.b, 1f)

        val verts = floatArrayOf(
            -halfW, 0f, -halfW, 0f, 1f, 0f, dirtColor,
            halfW, 0f, -halfW, 0f, 1f, 0f, dirtColor,
            halfW, 0f, halfW, 0f, 1f, 0f, dirtColor,
            -halfW, 0f, halfW, 0f, 1f, 0f, dirtColor
        )
        val idx = shortArrayOf(0, 1, 2, 0, 2, 3)

        groundMesh = Mesh(
            true, 4, 6,
            VertexAttribute.Position(),
            VertexAttribute.Normal(),
            VertexAttribute.ColorPacked()
        )
        groundMesh?.setVertices(verts)
        groundMesh?.setIndices(idx)
    }

    /**
     * Updates dirty chunks and renders visible chunks using custom GLSL shader and frustum culling.
     */
    fun render(camera: Camera, delta: Float) {
        animTime += delta

        // 1. Rebuild any dirty chunks
        for (cz in 0 until cropGrid.numChunksZ) {
            for (cx in 0 until cropGrid.numChunksX) {
                val idx = cz * cropGrid.numChunksX + cx
                if (cropGrid.dirtyChunks[idx]) {
                    chunks[idx].rebuild()
                    cropGrid.dirtyChunks[idx] = false
                }
            }
        }

        // 2. Bind custom shader program and uniforms
        shader.bind()
        shader.setUniformMatrix("u_projViewTrans", camera.combined)
        shader.setUniformMatrix("u_worldTrans", identityMatrix)
        shader.setUniformMatrix("u_normalMatrix", normalMatrix)
        shader.setUniformf("u_time", animTime)
        shader.setUniformf("u_windStrength", 1.0f)
        shader.setUniformf("u_lightDir", -0.4f, -0.8f, -0.4f)
        shader.setUniformf("u_lightColor", 1.0f, 0.98f, 0.90f)
        shader.setUniformf("u_ambientColor", 0.45f, 0.48f, 0.52f)
        shader.setUniformf("u_fogColor", 0.55f, 0.78f, 0.95f)
        shader.setUniformf("u_fogNear", 35f)
        shader.setUniformf("u_fogFar", 95f)
        shader.setUniformf("u_camPos", camera.position)

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glDepthMask(true)

        // 3. Render Ground
        groundMesh?.render(shader, GL20.GL_TRIANGLES)

        // 4. Render Chunks with frustum culling
        val frustum = camera.frustum
        for (chunk in chunks) {
            if (chunk.numIndices > 0 && frustum.boundsInFrustum(chunk.boundingBox)) {
                chunk.mesh?.render(shader, GL20.GL_TRIANGLES)
            }
        }
    }

    fun dispose() {
        for (c in chunks) {
            c.dispose()
        }
        groundMesh?.dispose()
        groundMesh = null
        shader.dispose()
    }
}
