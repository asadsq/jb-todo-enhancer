/*
 * A lightweight container that paints a soft, rounded-rectangle "card" background behind
 * its children. Used to give the tool window toolbar a modern widget look with nice corners
 * instead of the flat, edge-to-edge default panel.
 */
package com.github.asadsq.jbtodoenhancer.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LayoutManager
import java.awt.RenderingHints
import javax.swing.JPanel

/**
 * A [JPanel] that renders a filled, rounded card with a subtle 1px border. Non-opaque so the
 * tool window background shows in the margin around the rounded corners.
 */
class RoundedPanel(
    layout: LayoutManager,
    private val arc: Int = JBUI.scale(14),
) : JPanel(layout) {

    init {
        isOpaque = false
        background = CARD_BACKGROUND
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val w = width - 1
            val h = height - 1
            g2.color = background
            g2.fillRoundRect(0, 0, w, h, arc, arc)
            g2.color = CARD_BORDER
            g2.drawRoundRect(0, 0, w, h, arc, arc)
        } finally {
            g2.dispose()
        }
        super.paintComponent(g)
    }

    private companion object {
        val CARD_BACKGROUND = JBColor(0xF6F7F9, 0x2B2D30)
        val CARD_BORDER = JBColor(0xE1E4E8, 0x393B40)
    }
}
