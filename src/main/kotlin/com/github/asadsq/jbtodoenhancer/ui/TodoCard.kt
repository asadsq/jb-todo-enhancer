/*
 * The sleek front card of the stack: renders one `TodoEntry` as a rounded, modern note with a
 * priority chip, type badge, wrapped description, assignee/tags, due date, and its file:line
 * location. The whole card is clickable and invokes [onActivate] to jump to the code.
 */
package com.github.asadsq.jbtodoenhancer.ui

import com.github.asadsq.jbtodoenhancer.model.TodoEntry
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.LocalDate
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * A clickable card showing a single [TodoEntry]. Paints its own rounded background with a subtle
 * hover state; clicking (or pressing the card) calls [onActivate] so the caller can navigate.
 */
class TodoCard(
    entry: TodoEntry,
    private val onActivate: () -> Unit,
) : JPanel(BorderLayout()) {

    private var hovered = false

    init {
        isOpaque = false
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        border = JBUI.Borders.empty(16, 18)
        toolTipText = entry.location

        add(buildHeader(entry), BorderLayout.NORTH)
        add(buildDescription(entry), BorderLayout.CENTER)
        add(buildFooter(entry), BorderLayout.SOUTH)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = onActivate()
            override fun mouseEntered(e: MouseEvent) { hovered = true; repaint() }
            override fun mouseExited(e: MouseEvent) { hovered = false; repaint() }
        })
    }

    private fun buildHeader(entry: TodoEntry): JComponent {
        val badges = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            isOpaque = false
            add(PillLabel(entry.priority.label, PriorityColors.background(entry.priority), PriorityColors.foreground(entry.priority)))
            add(PillLabel(entry.type, NEUTRAL_BG, NEUTRAL_FG))
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(14)
            add(badges, BorderLayout.WEST)
            entry.dueDate?.let { add(dueLabel(it), BorderLayout.EAST) }
        }
    }

    private fun buildDescription(entry: TodoEntry): JComponent = JTextArea(entry.description).apply {
        isOpaque = false
        isEditable = false
        isFocusable = false
        lineWrap = true
        wrapStyleWord = true
        cursor = this@TodoCard.cursor
        font = JBFont.label().deriveFont(JBUI.scaleFontSize(14f).toFloat())
        foreground = JBColor.foreground()
        border = JBUI.Borders.empty()
        // Clicks on the text still activate the card.
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = onActivate()
        })
    }

    private fun buildFooter(entry: TodoEntry): JComponent {
        val left = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            isOpaque = false
            entry.assignee?.let { add(PillLabel("@$it", NEUTRAL_BG, NEUTRAL_FG)) }
            entry.tags.forEach { add(tagLabel(it)) }
        }
        val location = javax.swing.JLabel(entry.location).apply {
            font = JBFont.medium().deriveFont(Font.PLAIN)
            foreground = MUTED_FG
        }
        return JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.emptyTop(16)
            add(left, BorderLayout.WEST)
            add(location, BorderLayout.EAST)
        }
    }

    private fun dueLabel(due: LocalDate): JComponent {
        val overdue = due.isBefore(LocalDate.now())
        return javax.swing.JLabel("due $due").apply {
            font = JBFont.medium()
            foreground = if (overdue) PriorityColors.foreground(com.github.asadsq.jbtodoenhancer.model.TodoPriority.P1) else MUTED_FG
        }
    }

    private fun tagLabel(tag: String) = javax.swing.JLabel("#$tag").apply {
        font = JBFont.medium()
        foreground = TAG_FG
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val w = width - 1
            val h = height - 1
            val arc = JBUI.scale(16)
            g2.color = CARD_BG
            g2.fillRoundRect(0, 0, w, h, arc, arc)
            g2.color = if (hovered) CARD_BORDER_HOVER else CARD_BORDER
            g2.drawRoundRect(0, 0, w, h, arc, arc)
        } finally {
            g2.dispose()
        }
        super.paintComponent(g)
    }

    private companion object {
        val CARD_BG = JBColor(0xFFFFFF, 0x2F3134)
        val CARD_BORDER = JBColor(0xD8DBDF, 0x43454A)
        val CARD_BORDER_HOVER = JBColor(0xB4BAC2, 0x5A5D63)
        val NEUTRAL_BG = JBColor(0xEDEEF1, 0x3A3D40)
        val NEUTRAL_FG = JBColor(0x6B6F76, 0xB9BDC4)
        val MUTED_FG = JBColor(0x9295A0, 0x7E828A)
        val TAG_FG = JBColor(0x2E77B5, 0x8FC4EE)
    }
}
