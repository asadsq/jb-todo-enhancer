/*
 * A flat, rounded-corner button with hover and pressed states, painted manually so it looks
 * modern regardless of the active IDE Look-and-Feel. Replaces the stock JButton in the tool
 * window toolbar to give the "better buttons" polish.
 */
package com.github.asadsq.jbtodoenhancer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon
import javax.swing.JButton

/**
 * A borderless [JButton] that paints its own rounded background and reacts to hover/press.
 * Content-area filling and focus painting are disabled so only our custom shape shows.
 */
class RoundedButton(text: String, icon: Icon? = null) : JButton(text, icon) {

    init {
        isContentAreaFilled = false
        isFocusPainted = false
        isBorderPainted = false
        isRolloverEnabled = true
        isOpaque = false
        border = JBUI.Borders.empty(4, 12)
        iconTextGap = JBUI.scale(6)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val fill = when {
                !isEnabled -> NORMAL
                model.isPressed -> PRESSED
                model.isRollover -> HOVER
                else -> NORMAL
            }
            g2.color = fill
            g2.fillRoundRect(0, 0, width, height, JBUI.scale(10), JBUI.scale(10))
        } finally {
            g2.dispose()
        }
        super.paintComponent(g)
    }

    private companion object {
        val NORMAL = JBColor(0xEBEDF0, 0x3C3F41)
        val HOVER = JBColor(0xDFE3E8, 0x4A4E51)
        val PRESSED = JBColor(0xD2D7DE, 0x565A5D)
    }
}
