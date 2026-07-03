package com.github.asadsq.jbtodoenhancer.services

import com.github.asadsq.jbtodoenhancer.model.TodoEntry
import com.github.asadsq.jbtodoenhancer.model.TodoPriority
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class TodoScanServiceTest : BasePlatformTestCase() {

    private fun scan(): List<TodoEntry> =
        ApplicationManager.getApplication().runReadAction<List<TodoEntry>> {
            project.service<TodoScanService>().scan()
        }

    fun testScanFindsEnrichedTodo() {
        myFixture.configureByText(
            "Sample.java",
            """
            public class Sample {
                // TODO(@asad) !p1 #backend due:2026-07-10 - refactor the query builder
                void a() {}

                // FIXME plain problem here
                void b() {}
            }
            """.trimIndent(),
        )

        val entries = scan()

        val todo = entries.single { it.type == "TODO" }
        assertEquals("asad", todo.assignee)
        assertEquals(TodoPriority.P1, todo.priority)
        assertEquals(listOf("backend"), todo.tags)
        assertEquals("refactor the query builder", todo.description)
        assertEquals("Sample.java", todo.presentablePath.substringAfterLast('/'))

        assertTrue("expected a FIXME entry", entries.any { it.type == "FIXME" })
    }

    fun testNoTodosYieldsEmptyList() {
        myFixture.configureByText("Empty.java", "public class Empty {}")
        assertTrue(scan().isEmpty())
    }
}
