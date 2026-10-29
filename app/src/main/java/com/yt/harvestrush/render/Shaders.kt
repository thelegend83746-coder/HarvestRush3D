package com.yt.harvestrush.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShaderProgram

/**
 * Custom GLSL Shaders for low-poly rendering with directional lighting,
 * wind sway in the vertex shader, and distance fog.
 */
object Shaders {

    /**
     * Standard low-poly vertex shader with wind sway animation for vegetation.
     */
    val VERTEX_SHADER = """
        #ifdef GL_ES
        precision highp float;
        #endif

        attribute vec3 a_position;
        attribute vec3 a_normal;
        attribute vec4 a_color;

        uniform mat4 u_projViewTrans;
        uniform mat4 u_worldTrans;
        uniform mat3 u_normalMatrix;
        uniform float u_time;
        uniform float u_windStrength;

        varying vec4 v_color;
        varying vec3 v_normal;
        varying vec3 v_worldPos;

        void main() {
            vec3 pos = a_position;

            // Wind sway for upper vertices (vegetation sway)
            if (pos.y > 0.4 && u_windStrength > 0.0) {
                float wave = sin(u_time * 3.5 + pos.x * 1.5 + pos.z * 1.2) * 0.08 * u_windStrength;
                pos.x += wave * (pos.y * 0.8);
                pos.z += wave * 0.5 * (pos.y * 0.8);
            }

            vec4 worldPos = u_worldTrans * vec4(pos, 1.0);
            v_worldPos = worldPos.xyz;
            v_normal = normalize(u_normalMatrix * a_normal);
            v_color = a_color;

            gl_Position = u_projViewTrans * worldPos;
        }
    """.trimIndent()

    /**
     * Low-poly fragment shader with directional sun light, ambient, and soft horizon fog.
     */
    val FRAGMENT_SHADER = """
        #ifdef GL_ES
        precision mediump float;
        #endif

        varying vec4 v_color;
        varying vec3 v_normal;
        varying vec3 v_worldPos;

        uniform vec3 u_lightDir;
        uniform vec3 u_lightColor;
        uniform vec3 u_ambientColor;
        uniform vec3 u_fogColor;
        uniform float u_fogNear;
        uniform float u_fogFar;
        uniform vec3 u_camPos;

        void main() {
            vec3 n = normalize(v_normal);
            vec3 l = normalize(-u_lightDir);

            // Diffuse directional light with half-Lambert wrap for bright arcade feel
            float diff = max(dot(n, l), 0.0);
            diff = diff * 0.65 + 0.35; // Soft wrap

            vec3 lighting = u_ambientColor + u_lightColor * diff;
            vec3 finalColor = v_color.rgb * lighting;

            // Distance fog calculation
            float dist = length(v_worldPos - u_camPos);
            float fogFactor = clamp((dist - u_fogNear) / (u_fogFar - u_fogNear), 0.0, 1.0);
            finalColor = mix(finalColor, u_fogColor, fogFactor * 0.75);

            gl_FragColor = vec4(finalColor, v_color.a);
        }
    """.trimIndent()

    /**
     * Instanced vertex shader for rendering thousands of crop blades/trailers in one draw call.
     */
    val INSTANCED_VERTEX_SHADER = """
        #ifdef GL_ES
        precision highp float;
        #endif

        attribute vec3 a_position;
        attribute vec3 a_normal;
        attribute vec4 a_color;

        // Per-instance attributes (packed transform & color)
        attribute vec4 a_instancePosScale; // xyz = position, w = uniform scale
        attribute vec4 a_instanceRotColor; // x = yaw rotation, yzw = tint multiplier

        uniform mat4 u_projViewTrans;
        uniform float u_time;
        uniform float u_windStrength;

        varying vec4 v_color;
        varying vec3 v_normal;
        varying vec3 v_worldPos;

        void main() {
            vec3 pos = a_position;

            // Apply wind sway if above ground
            if (pos.y > 0.3 && u_windStrength > 0.0) {
                float wave = sin(u_time * 3.5 + a_instancePosScale.x * 1.5 + a_instancePosScale.z * 1.2) * 0.08 * u_windStrength;
                pos.x += wave * pos.y;
                pos.z += wave * 0.5 * pos.y;
            }

            // Instance scale
            pos *= a_instancePosScale.w;

            // Instance rotation around Y axis
            float yaw = a_instanceRotColor.x;
            float cosY = cos(yaw);
            float sinY = sin(yaw);
            vec3 rotPos;
            rotPos.x = pos.x * cosY - pos.z * sinY;
            rotPos.y = pos.y;
            rotPos.z = pos.x * sinY + pos.z * cosY;

            // Instance translation
            vec3 worldPos = rotPos + a_instancePosScale.xyz;
            v_worldPos = worldPos;

            // Normal rotation
            vec3 rotNorm;
            rotNorm.x = a_normal.x * cosY - a_normal.z * sinY;
            rotNorm.y = a_normal.y;
            rotNorm.z = a_normal.x * sinY + a_normal.z * cosY;
            v_normal = normalize(rotNorm);

            // Instance color tint
            v_color = vec4(a_color.rgb * a_instanceRotColor.yzw, a_color.a);

            gl_Position = u_projViewTrans * vec4(worldPos, 1.0);
        }
    """.trimIndent()

    fun createDefaultShader(): ShaderProgram {
        val shader = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (!shader.isCompiled) {
            Gdx.app.error("Shaders", "Shader compilation error: " + shader.log)
        }
        return shader
    }

    fun createInstancedShader(): ShaderProgram {
        val shader = ShaderProgram(INSTANCED_VERTEX_SHADER, FRAGMENT_SHADER)
        if (!shader.isCompiled) {
            Gdx.app.error("Shaders", "Instanced shader compilation error: " + shader.log)
        }
        return shader
    }
}
