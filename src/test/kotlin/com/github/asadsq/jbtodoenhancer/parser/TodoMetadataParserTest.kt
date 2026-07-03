/*
 * JUnit 4 unit tests for `TodoMetadataParser`, covering each metadata token (assignee,
 * priority, tags, due date) individually and in combination. Also exercises edge cases
 * such as invalid dates and email addresses that must not be mistaken for assignees.
 */
package com.github.asadsq.jbtodoenhancer.parser

import com.github.asadsq.jbtodoenhancer.model.TodoPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TodoMetadataParserTest {

    @Test
    fun `plain todo keeps its description and has no metadata`() {
        val parsed = TodoMetadataParser.parse("TODO: fix this later")
        assertEquals("TODO", parsed.type)
        assertNull(parsed.assignee)
        assertEquals(TodoPriority.NONE, parsed.priority)
        assertTrue(parsed.tags.isEmpty())
        assertNull(parsed.dueDate)
        assertEquals("fix this later", parsed.description)
    }

    @Test
    fun `fixme keyword is recognized and upper-cased`() {
        val parsed = TodoMetadataParser.parse("fixme handle null input")
        assertEquals("FIXME", parsed.type)
        assertEquals("handle null input", parsed.description)
    }

    @Test
    fun `inline assignee is extracted`() {
        val parsed = TodoMetadataParser.parse("TODO @asad wire up the service")
        assertEquals("asad", parsed.assignee)
        assertEquals("wire up the service", parsed.description)
    }

    @Test
    fun `parenthesized assignee right after keyword is extracted`() {
        val parsed = TodoMetadataParser.parse("TODO(@asad) refactor parser")
        assertEquals("asad", parsed.assignee)
        assertEquals("refactor parser", parsed.description)
    }

    @Test
    fun `parenthesized assignee without at sign is extracted`() {
        val parsed = TodoMetadataParser.parse("TODO(bob): ship it")
        assertEquals("bob", parsed.assignee)
        assertEquals("ship it", parsed.description)
    }

    @Test
    fun `numeric and named priorities map correctly`() {
        assertEquals(TodoPriority.P1, TodoMetadataParser.parse("TODO !p1 x").priority)
        assertEquals(TodoPriority.P1, TodoMetadataParser.parse("TODO !high x").priority)
        assertEquals(TodoPriority.P2, TodoMetadataParser.parse("TODO !2 x").priority)
        assertEquals(TodoPriority.P3, TodoMetadataParser.parse("TODO !low x").priority)
    }

    @Test
    fun `tags are extracted without the hash and de-duplicated`() {
        val parsed = TodoMetadataParser.parse("TODO #backend #perf #backend tune query")
        assertEquals(listOf("backend", "perf"), parsed.tags)
        assertEquals("tune query", parsed.description)
    }

    @Test
    fun `due date is parsed as a local date`() {
        val parsed = TodoMetadataParser.parse("TODO due:2026-07-10 finalize notes")
        assertEquals(LocalDate.of(2026, 7, 10), parsed.dueDate)
        assertEquals("finalize notes", parsed.description)
    }

    @Test
    fun `invalid due date is ignored gracefully`() {
        val parsed = TodoMetadataParser.parse("TODO due:2026-13-99 broken date")
        assertNull(parsed.dueDate)
    }

    @Test
    fun `combined metadata is fully extracted and stripped from description`() {
        val parsed = TodoMetadataParser.parse("TODO(@asad) !p1 #backend #db due:2026-07-10 - refactor the query builder")
        assertEquals("TODO", parsed.type)
        assertEquals("asad", parsed.assignee)
        assertEquals(TodoPriority.P1, parsed.priority)
        assertEquals(listOf("backend", "db"), parsed.tags)
        assertEquals(LocalDate.of(2026, 7, 10), parsed.dueDate)
        assertEquals("refactor the query builder", parsed.description)
    }

    @Test
    fun `an email address is not mistaken for an assignee`() {
        val parsed = TodoMetadataParser.parse("TODO ping user@example.com about this")
        assertNull(parsed.assignee)
    }
}
