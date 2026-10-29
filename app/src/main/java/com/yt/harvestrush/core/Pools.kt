package com.yt.harvestrush.core

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Pool

/**
 * Global zero-allocation memory pools for the 60 FPS render loop.
 */
object GamePools {
    val vec2Pool: Pool<Vector2> = object : Pool<Vector2>(64) {
        override fun newObject(): Vector2 = Vector2()
        override fun reset(v: Vector2) { v.setZero() }
    }

    val vec3Pool: Pool<Vector3> = object : Pool<Vector3>(64) {
        override fun newObject(): Vector3 = Vector3()
        override fun reset(v: Vector3) { v.setZero() }
    }

    inline fun <R> obtainVec2(block: (Vector2) -> R): R {
        val v = vec2Pool.obtain()
        try {
            return block(v)
        } finally {
            vec2Pool.free(v)
        }
    }

    inline fun <R> obtainVec3(block: (Vector3) -> R): R {
        val v = vec3Pool.obtain()
        try {
            return block(v)
        } finally {
            vec3Pool.free(v)
        }
    }
}
