/*
 * Central, theme-aware color palette mapping each `TodoPriority` to a soft pill background and
 * a saturated foreground. Shared by the card UI so P1/P2/P3 read as red/amber/blue chips while
 * NONE stays a muted gray, consistently in both light and dark themes.
 */
package com.github.asadsq.jbtodoenhancer.ui

import com.github.asadsq.jbtodoenhancer.model.TodoPriority
import com.intellij.ui.JBColor
import java.awt.Color

/**
 * Theme-aware pill colors for TODO priorities. [background]/[foreground] return the tinted fill
 * and matching text color for a given [TodoPriority].
 */
object PriorityColors {

    private val P1_BG = JBColor(0xFBE1E1, 0x53302F)
    private val P1_FG = JBColor(0xC0392B, 0xF3A6A0)
    private val P2_BG = JBColor(0xFCEFD6, 0x4C3C22)
    private val P2_FG = JBColor(0xB07714, 0xEEC680)
    private val P3_BG = JBColor(0xDCEBFA, 0x263B4E)
    private val P3_FG = JBColor(0x2E77B5, 0x8FC4EE)
    private val NONE_BG = JBColor(0xEBECEE, 0x3A3D40)
    private val NONE_FG = JBColor(0x8A8D91, 0x9BA0A6)

    fun background(priority: TodoPriority): Color = when (priority) {
        TodoPriority.P1 -> P1_BG
        TodoPriority.P2 -> P2_BG
        TodoPriority.P3 -> P3_BG
        TodoPriority.NONE -> NONE_BG
    }

    fun foreground(priority: TodoPriority): Color = when (priority) {
        TodoPriority.P1 -> P1_FG
        TodoPriority.P2 -> P2_FG
        TodoPriority.P3 -> P3_FG
        TodoPriority.NONE -> NONE_FG
    }
}
