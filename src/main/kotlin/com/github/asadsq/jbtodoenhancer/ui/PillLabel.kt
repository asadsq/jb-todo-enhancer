/*
 * A small rounded "pill" label that paints its own tinted, fully-rounded background behind bold
 * text. Reused across the TODO card for priority chips, the type badge, and tag chips so every
 * inline badge shares one consistent, theme-aware shape.
 */
package com.github.asadsq.jbtodoenhancer.ui

import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JLabel

/**
 * A [JLabel] rendered as a pill: rounded tinted background ([pillBackground]) with a matching
 * text color. Sizes itself to the text plus horizontal padding.
 */
class PillLabel(
    text: String,
    private val pillBackground: Color,
    pillForeground: Color,
) : JLabel(text) {

    private val hPad = JBUI.scale(10)
    private val vPad = JBUI.scale(3)

    init {
        isOpaque = false
        foreground = pillForeground
        font = font.deriveFont(Font.BOLD, JBUI.scaleFontSize(11f).toFloat())
        border = JBUI.Borders.empty(vPad, hPad)
    }

    override fun getPreferredSize(): Dimension {
        val fm = getFontMetrics(font)
        return Dimension(fm.stringWidth(text) + hPad * 2, fm.height + vPad * 2)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = pillBackground
            g2.fillRoundRect(0, 0, width, height, height, height)
        } finally {
            g2.dispose()
        }
        super.paintComponent(g)
    }
}
