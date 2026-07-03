/*
 * Table cell renderer that draws a TODO's priority as a colored, rounded "pill" badge instead
 * of plain text. Colors are theme-aware (light/dark) so P1/P2/P3 read as red/amber/blue chips
 * while honoring the table's selection background.
 */
package com.github.asadsq.jbtodoenhancer.ui

import com.github.asadsq.jbtodoenhancer.model.TodoPriority
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.TableCellRenderer

/**
 * Renders the priority column as a centered pill. Uses a soft tinted background with a
 * saturated foreground per priority; [TodoPriority.NONE] renders as a muted gray chip.
 */
class PriorityBadgeRenderer : JLabel(), TableCellRenderer {

    private var pillColor: Color = NONE_BG
    private var selected: Boolean = false
    private var selectionBackground: Color = JBColor.background()

    init {
        horizontalAlignment = SwingConstants.CENTER
        isOpaque = false
        font = font.deriveFont(Font.BOLD)
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): Component {
        val label = value?.toString().orEmpty()
        text = label
        selected = isSelected
        selectionBackground = table.selectionBackground
        val priority = TodoPriority.values().firstOrNull { it.label == label } ?: TodoPriority.NONE
        pillColor = backgroundFor(priority)
        foreground = foregroundFor(priority)
        return this
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            if (selected) {
                g2.color = selectionBackground
                g2.fillRect(0, 0, width, height)
            }
            val inset = JBUI.scale(4)
            val fm = getFontMetrics(font)
            val textWidth = fm.stringWidth(text.ifEmpty { "—" })
            val pillWidth = minOf(width - inset * 2, textWidth + JBUI.scale(20))
            val pillHeight = height - inset * 2
            if (pillWidth > 0 && pillHeight > 0) {
                val x = (width - pillWidth) / 2
                val y = inset
                g2.color = pillColor
                g2.fillRoundRect(x, y, pillWidth, pillHeight, pillHeight, pillHeight)
            }
        } finally {
            g2.dispose()
        }
        super.paintComponent(g)
    }

    private companion object {
        val P1_BG = JBColor(0xFBE1E1, 0x53302F)
        val P1_FG = JBColor(0xC0392B, 0xF3A6A0)
        val P2_BG = JBColor(0xFCEFD6, 0x4C3C22)
        val P2_FG = JBColor(0xB07714, 0xEEC680)
        val P3_BG = JBColor(0xDCEBFA, 0x263B4E)
        val P3_FG = JBColor(0x2E77B5, 0x8FC4EE)
        val NONE_BG = JBColor(0xEBECEE, 0x3A3D40)
        val NONE_FG = JBColor(0x8A8D91, 0x9BA0A6)

        fun backgroundFor(priority: TodoPriority): Color = when (priority) {
            TodoPriority.P1 -> P1_BG
            TodoPriority.P2 -> P2_BG
            TodoPriority.P3 -> P3_BG
            TodoPriority.NONE -> NONE_BG
        }

        fun foregroundFor(priority: TodoPriority): Color = when (priority) {
            TodoPriority.P1 -> P1_FG
            TodoPriority.P2 -> P2_FG
            TodoPriority.P3 -> P3_FG
            TodoPriority.NONE -> NONE_FG
        }
    }
}
