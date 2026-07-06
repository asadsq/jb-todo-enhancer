/*
 * The stacked-cards widget: shows the current `TodoEntry` as a front `TodoCard` with up to two
 * offset "peek" cards painted behind it so the deck reads as a physical pile of notes. Exposes
 * next/prev cycling, the current position, and an activation callback for jumping to the code.
 */
package com.github.asadsq.jbtodoenhancer.ui

import com.github.asadsq.jbtodoenhancer.TodoEnhancerBundle
import com.github.asadsq.jbtodoenhancer.model.TodoEntry
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Renders a cyclable stack of TODO cards. Call [setEntries] to load the deck, [next]/[prev] to
 * flip through it, and observe [onIndexChanged] to refresh external UI (e.g. a counter). Clicking
 * the front card invokes [onActivate] with the visible entry.
 */
class CardStackPanel(
    private val onActivate: (TodoEntry) -> Unit,
) : JPanel(null) {

    private var entries: List<TodoEntry> = emptyList()
    private var index = 0
    private var card: TodoCard? = null

    private val emptyLabel = JBLabel(TodoEnhancerBundle["status.empty"], SwingConstants.CENTER).apply {
        foreground = MUTED_FG
    }

    /** Invoked after the visible card or deck size changes, so callers can update a counter. */
    var onIndexChanged: (() -> Unit)? = null

    init {
        isOpaque = false
        add(emptyLabel)
    }

    /** 1-based position of the visible card, or 0 when the deck is empty. */
    val position: Int get() = if (entries.isEmpty()) 0 else index + 1

    /** Total number of cards currently in the deck. */
    val count: Int get() = entries.size

    fun setEntries(newEntries: List<TodoEntry>) {
        entries = newEntries
        index = if (newEntries.isEmpty()) 0 else index.coerceIn(0, newEntries.size - 1)
        rebuild()
    }

    fun next() {
        if (entries.size > 1) {
            index = (index + 1) % entries.size
            rebuild()
        }
    }

    fun prev() {
        if (entries.size > 1) {
            index = (index - 1 + entries.size) % entries.size
            rebuild()
        }
    }

    fun activateCurrent() {
        entries.getOrNull(index)?.let(onActivate)
    }

    private fun rebuild() {
        card?.let { remove(it) }
        card = null

        val current = entries.getOrNull(index)
        if (current == null) {
            emptyLabel.isVisible = true
        } else {
            emptyLabel.isVisible = false
            val newCard = TodoCard(current) { onActivate(current) }
            add(newCard)
            card = newCard
        }

        onIndexChanged?.invoke()
        revalidate()
        repaint()
    }

    override fun doLayout() {
        val peek = JBUI.scale(PEEK)
        card?.setBounds(0, 0, (width - 2 * peek).coerceAtLeast(0), (height - 2 * peek).coerceAtLeast(0))
        emptyLabel.setBounds(0, 0, width, height)
    }

    override fun getPreferredSize(): Dimension {
        val peek = JBUI.scale(PEEK)
        val base = card?.preferredSize ?: Dimension(JBUI.scale(340), JBUI.scale(150))
        return Dimension(base.width + 2 * peek, base.height + 2 * peek)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (entries.isEmpty()) return

        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val peek = JBUI.scale(PEEK)
            val arc = JBUI.scale(16)
            val cardW = width - 2 * peek - 1
            val cardH = height - 2 * peek - 1
            // Paint the deepest peek first so nearer cards overlap it; the front card is a child
            // component and paints on top of everything drawn here.
            val depth = minOf(entries.size - 1, 2)
            for (i in depth downTo 1) {
                val x = i * peek
                val y = i * peek
                g2.color = if (i == 1) PEEK_BG_NEAR else PEEK_BG_FAR
                g2.fillRoundRect(x, y, cardW, cardH, arc, arc)
                g2.color = PEEK_BORDER
                g2.drawRoundRect(x, y, cardW, cardH, arc, arc)
            }
        } finally {
            g2.dispose()
        }
    }

    private companion object {
        const val PEEK = 7
        val PEEK_BG_NEAR = JBColor(0xF3F4F6, 0x2A2C2E)
        val PEEK_BG_FAR = JBColor(0xEBECEF, 0x27282A)
        val PEEK_BORDER = JBColor(0xDADDE1, 0x3C3E42)
        val MUTED_FG = JBColor(0x9295A0, 0x7E828A)
    }
}
