package com.yt.harvestrush.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

/**
 * 100% Procedural Scene2D UI Skin.
 * Generates rounded pill buttons, outlines, progress bars, and fonts in code.
 * Zero external atlas or image files required.
 */
object SkinBuilder {

    fun buildSkin(): Skin {
        val skin = Skin()

        // 1. Safe BitmapFonts
        val font = try {
            BitmapFont().apply {
                data.setScale(1.5f)
                data.markupEnabled = true
            }
        } catch (e: Throwable) {
            BitmapFont()
        }

        val bigFont = try {
            BitmapFont().apply {
                data.setScale(2.2f)
                data.markupEnabled = true
            }
        } catch (e: Throwable) {
            font
        }

        val smallFont = try {
            BitmapFont().apply {
                data.setScale(1.1f)
                data.markupEnabled = true
            }
        } catch (e: Throwable) {
            font
        }

        skin.add("default-font", font, BitmapFont::class.java)
        skin.add("big-font", bigFont, BitmapFont::class.java)
        skin.add("small-font", smallFont, BitmapFont::class.java)

        // 2. Procedural Textures & Drawables
        val whitePixmap = Pixmap(8, 8, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }
        val whiteDrawable = TextureRegionDrawable(TextureRegion(Texture(whitePixmap)))
        skin.add("white", whiteDrawable)

        val blueBtn = createRoundedRect(160, 64, 24, Color(0.18f, 0.55f, 0.95f, 1f), Color.WHITE, 3)
        val blueBtnDown = createRoundedRect(160, 64, 24, Color(0.12f, 0.40f, 0.75f, 1f), Color.LIGHT_GRAY, 3)
        skin.add("btn-blue", blueBtn)
        skin.add("btn-blue-down", blueBtnDown)

        val greenBtn = createRoundedRect(160, 64, 24, Color(0.25f, 0.78f, 0.35f, 1f), Color.WHITE, 3)
        skin.add("btn-green", greenBtn)

        val pillBg = createRoundedRect(140, 48, 18, Color(0f, 0f, 0f, 0.55f), Color(1f, 1f, 1f, 0.8f), 2)
        skin.add("pill-bg", pillBg)

        val panelBg = createRoundedRect(280, 180, 14, Color(0.12f, 0.14f, 0.18f, 0.92f), Color(0.4f, 0.45f, 0.55f, 0.8f), 2)
        skin.add("panel-bg", panelBg)

        val progressBg = createRoundedRect(200, 24, 8, Color(0.15f, 0.18f, 0.22f, 0.85f), Color.CLEAR, 0)
        val progressFill = createRoundedRect(200, 24, 8, Color(0.98f, 0.85f, 0.25f, 1f), Color.CLEAR, 0)
        val progressFillCyan = createRoundedRect(200, 24, 8, Color(0.18f, 0.85f, 0.95f, 1f), Color.CLEAR, 0)
        skin.add("progress-bg", progressBg)
        skin.add("progress-fill", progressFill)
        skin.add("progress-fill-cyan", progressFillCyan)

        // 3. Styles
        val labelStyle = Label.LabelStyle(font, Color.WHITE)
        skin.add("default", labelStyle)

        val bigLabelStyle = Label.LabelStyle(bigFont, Color.WHITE)
        skin.add("big", bigLabelStyle)

        val cyanLabelStyle = Label.LabelStyle(bigFont, Color(0.18f, 0.88f, 0.98f, 1f))
        skin.add("cyan", cyanLabelStyle)

        val smallLabelStyle = Label.LabelStyle(smallFont, Color.WHITE)
        skin.add("small", smallLabelStyle)

        val btnStyle = TextButton.TextButtonStyle().apply {
            up = blueBtn
            down = blueBtnDown
            this.font = bigFont
            fontColor = Color.WHITE
        }
        skin.add("default", btnStyle)

        val greenBtnStyle = TextButton.TextButtonStyle().apply {
            up = greenBtn
            down = blueBtnDown
            this.font = bigFont
            fontColor = Color.WHITE
        }
        skin.add("green", greenBtnStyle)

        val pbStyle = ProgressBar.ProgressBarStyle(progressBg, null).apply {
            knobBefore = progressFill
        }
        skin.add("default-horizontal", pbStyle)

        val pbCyanStyle = ProgressBar.ProgressBarStyle(progressBg, null).apply {
            knobBefore = progressFillCyan
        }
        skin.add("cyan-horizontal", pbCyanStyle)

        return skin
    }

    private fun createRoundedRect(w: Int, h: Int, radius: Int, fillColor: Color, strokeColor: Color, strokeWidth: Int): TextureRegionDrawable {
        val pixmap = Pixmap(w, h, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.CLEAR)
        pixmap.fill()

        pixmap.setColor(fillColor)
        pixmap.fillRectangle(radius, 0, w - 2 * radius, h)
        pixmap.fillRectangle(0, radius, w, h - 2 * radius)
        pixmap.fillCircle(radius, radius, radius)
        pixmap.fillCircle(w - radius - 1, radius, radius)
        pixmap.fillCircle(radius, h - radius - 1, radius)
        pixmap.fillCircle(w - radius - 1, h - radius - 1, radius)

        if (strokeWidth > 0 && strokeColor.a > 0f) {
            pixmap.setColor(strokeColor)
            for (t in 0 until strokeWidth) {
                val r = radius - t
                if (r > 0) {
                    pixmap.drawCircle(radius, radius, r)
                    pixmap.drawCircle(w - radius - 1, radius, r)
                    pixmap.drawCircle(radius, h - radius - 1, r)
                    pixmap.drawCircle(w - radius - 1, h - radius - 1, r)
                }
                pixmap.drawRectangle(radius, t, w - 2 * radius, 1)
                pixmap.drawRectangle(radius, h - 1 - t, w - 2 * radius, 1)
                pixmap.drawRectangle(t, radius, 1, h - 2 * radius)
                pixmap.drawRectangle(w - 1 - t, radius, 1, h - 2 * radius)
            }
        }

        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegionDrawable(TextureRegion(texture))
    }
}
