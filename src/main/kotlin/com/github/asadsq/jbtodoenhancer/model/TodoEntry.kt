package com.github.asadsq.jbtodoenhancer.model

import com.intellij.openapi.vfs.VirtualFile
import java.time.LocalDate

/**
 * A single enhanced TODO: parsed metadata plus its location in the project,
 * enough to render a table row and navigate to it in the editor.
 */
data class TodoEntry(
    val type: String,
    val description: String,
    val assignee: String?,
    val priority: TodoPriority,
    val tags: List<String>,
    val dueDate: LocalDate?,
    val file: VirtualFile,
    val presentablePath: String,
    val line: Int,      // 1-based, for display
    val offset: Int,    // 0-based char offset, for navigation
) {
    val location: String get() = "$presentablePath:$line"

    val tagsText: String get() = tags.joinToString(" ") { "#$it" }
}
