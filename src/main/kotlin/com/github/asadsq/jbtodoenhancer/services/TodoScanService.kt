package com.github.asadsq.jbtodoenhancer.services

import com.github.asadsq.jbtodoenhancer.model.TodoEntry
import com.github.asadsq.jbtodoenhancer.parser.TodoMetadataParser
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.PsiTodoSearchHelper
import com.intellij.openapi.vfs.VirtualFile

/**
 * Collects all TODO items in the project by reusing the platform's TODO index
 * ([PsiTodoSearchHelper]) and enriching each occurrence with structured metadata
 * parsed by [TodoMetadataParser].
 *
 * [scan] must be invoked inside a read action (e.g. `ReadAction.nonBlocking { ... }`).
 */
@Service(Service.Level.PROJECT)
class TodoScanService(private val project: Project) {

    fun scan(): List<TodoEntry> {
        val helper = PsiTodoSearchHelper.getInstance(project)
        val documentManager = PsiDocumentManager.getInstance(project)
        val basePath = project.basePath
        val result = mutableListOf<TodoEntry>()

        helper.processFilesWithTodoItems { psiFile ->
            val vFile = psiFile.virtualFile
            val document = documentManager.getDocument(psiFile)
            if (vFile != null && document != null) {
                for (item in helper.findTodoItems(psiFile)) {
                    val startOffset = item.textRange.startOffset
                    if (startOffset < 0 || startOffset > document.textLength) continue

                    val lineNumber = document.getLineNumber(startOffset)
                    val lineEnd = document.getLineEndOffset(lineNumber)
                    val rawText = document.getText(TextRange(startOffset, lineEnd)).trim()

                    val parsed = TodoMetadataParser.parse(rawText)
                    result += TodoEntry(
                        type = parsed.type,
                        description = parsed.description,
                        assignee = parsed.assignee,
                        priority = parsed.priority,
                        tags = parsed.tags,
                        dueDate = parsed.dueDate,
                        file = vFile,
                        presentablePath = presentablePath(basePath, vFile),
                        line = lineNumber + 1,
                        offset = startOffset,
                    )
                }
            }
            true
        }
        return result
    }

    private fun presentablePath(basePath: String?, file: VirtualFile): String {
        val path = file.path
        return when {
            basePath != null && path.startsWith("$basePath/") -> path.substring(basePath.length + 1)
            else -> file.name
        }
    }
}
