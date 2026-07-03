/*
 * Regex-based parser that converts raw TODO comment text into a `ParsedTodo` holding the
 * type, assignee, priority, tags, due date, and cleaned description. All metadata tokens
 * are optional and order-independent, and malformed values are ignored gracefully.
 */
package com.github.asadsq.jbtodoenhancer.parser

import com.github.asadsq.jbtodoenhancer.model.TodoPriority
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Structured metadata extracted from a single TODO comment.
 *
 * Supported syntax (all fields optional, order-independent):
 * ```
 * // TODO(@asad) !p1 #backend #parser due:2026-07-10 - refactor the parser
 * ```
 * - `type`        leading keyword (TODO, FIXME, ...), upper-cased
 * - `@name`       assignee (also `TODO(name)` / `TODO(@name)` right after the keyword)
 * - `!p1`         priority (`!p1..!p3`, or `!high`/`!medium`/`!low`)
 * - `#tag`        one or more tags
 * - `due:DATE`    ISO-8601 due date (`YYYY-MM-DD`)
 * - remaining text becomes [description], with metadata tokens and leading separators stripped.
 */
data class ParsedTodo(
    val type: String,
    val assignee: String?,
    val priority: TodoPriority,
    val tags: List<String>,
    val dueDate: LocalDate?,
    val description: String,
)

object TodoMetadataParser {

    private val TYPE = Regex("""^\s*([A-Za-z]+)""")
    private val PAREN_ASSIGNEE = Regex("""^\s*\(\s*@?([A-Za-z0-9_.\-]+)\s*\)""")
    private val ASSIGNEE = Regex("""(?<![\w@])@([A-Za-z0-9_.\-]+)""")
    private val PRIORITY = Regex("""(?<!\S)!(p[1-3]|[1-3]|high|urgent|critical|medium|med|normal|low|minor)\b""", RegexOption.IGNORE_CASE)
    private val TAG = Regex("""(?<!\S)#([A-Za-z0-9_./\-]+)""")
    private val DUE = Regex("""(?<!\S)due:\s*(\d{4}-\d{2}-\d{2})""", RegexOption.IGNORE_CASE)
    private val LEADING_SEPARATORS = Regex("""^[\s:\-–—]+""")
    private val WHITESPACE = Regex("""\s{2,}""")

    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Parses [rawTodoText] — the text of a TODO occurrence starting at (and including) its
     * keyword, e.g. `"TODO(@asad) !p1 - fix"`. Malformed metadata is ignored gracefully;
     * unrecognized text ends up in [ParsedTodo.description].
     */
    fun parse(rawTodoText: String): ParsedTodo {
        val text = rawTodoText.trim()

        val type = TYPE.find(text)?.groupValues?.get(1)?.uppercase() ?: "TODO"
        var body = text.removePrefix(TYPE.find(text)?.groupValues?.get(1).orEmpty())

        // Assignee written as TODO(name) directly after the keyword.
        var parenAssignee: String? = null
        PAREN_ASSIGNEE.find(body)?.let {
            parenAssignee = it.groupValues[1]
            body = body.removeRange(it.range)
        }

        val inlineAssignee = ASSIGNEE.find(body)?.groupValues?.get(1)
        val assignee = parenAssignee ?: inlineAssignee

        val priority = PRIORITY.find(body)
            ?.groupValues?.get(1)
            ?.removePrefix("p")?.removePrefix("P")
            ?.let { TodoPriority.fromToken(it) }
            ?: TodoPriority.NONE

        val tags = TAG.findAll(body).map { it.groupValues[1] }.distinct().toList()

        val dueDate = DUE.find(body)?.groupValues?.get(1)?.let { runCatching { LocalDate.parse(it, DATE_FORMAT) }.getOrNull() }

        // Build the human description by removing every recognized metadata token.
        var description = body
        description = ASSIGNEE.replace(description, "")
        description = PRIORITY.replace(description, "")
        description = TAG.replace(description, "")
        description = DUE.replace(description, "")
        description = LEADING_SEPARATORS.replace(description, "")
        description = WHITESPACE.replace(description, " ").trim()

        return ParsedTodo(
            type = type,
            assignee = assignee,
            priority = priority,
            tags = tags,
            dueDate = dueDate,
            description = description,
        )
    }
}
