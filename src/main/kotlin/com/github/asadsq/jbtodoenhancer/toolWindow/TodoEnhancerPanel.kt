/*
 * Swing UI for the tool window: a sortable `TableView` of `TodoEntry` rows with toolbar
 * filters for free text, type, priority, and "only mine". Fetches data from
 * `TodoScanService` on a background read action and auto-refreshes with a debounce
 * whenever the PSI tree changes.
 */
package com.github.asadsq.jbtodoenhancer.toolWindow

import com.github.asadsq.jbtodoenhancer.TodoEnhancerBundle
import com.github.asadsq.jbtodoenhancer.model.TodoEntry
import com.github.asadsq.jbtodoenhancer.model.TodoPriority
import com.github.asadsq.jbtodoenhancer.services.TodoScanService
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.TableView
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.Callable
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Tool window content: a filterable, sortable table of all TODOs in the project.
 * Double-click (or Enter) navigates to the TODO in the editor. Refreshes on demand
 * and automatically (debounced) when the PSI changes.
 */
class TodoEnhancerPanel(
    private val project: Project,
    private val parentDisposable: Disposable,
) : SimpleToolWindowPanel(true, true) {

    private val service = project.service<TodoScanService>()

    private val model = ListTableModel<TodoEntry>(*buildColumns())
    private val table = TableView(model)

    private val searchField = SearchTextField(false)
    private val typeCombo = ComboBox<String>()
    private val priorityCombo = ComboBox(arrayOf<Any>(ANY_PRIORITY, TodoPriority.P1, TodoPriority.P2, TodoPriority.P3))
    private val mineCheck = JBCheckBox(TodoEnhancerBundle["filter.mineOnly"])
    private val statusLabel = JBLabel()

    private val refreshAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, parentDisposable)

    private var allEntries: List<TodoEntry> = emptyList()

    init {
        toolbar = buildToolbar()
        setContent(JBScrollPane(table))

        typeCombo.addItem(ALL_TYPES)
        typeCombo.selectedItem = ALL_TYPES

        installListeners()
        refresh()
    }

    private fun buildToolbar(): JPanel {
        val refreshButton = JButton(TodoEnhancerBundle["action.refresh.text"], AllIcons.Actions.Refresh).apply {
            addActionListener { refresh() }
        }
        searchField.textEditor.emptyText.text = TodoEnhancerBundle["filter.search.hint"]

        return JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(4))).apply {
            border = JBUI.Borders.empty(2, 4)
            add(refreshButton)
            add(searchField)
            add(typeCombo)
            add(priorityCombo)
            add(mineCheck)
            add(statusLabel)
        }
    }

    private fun installListeners() {
        val applyOnChange = { applyFilters() }

        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = applyOnChange()
            override fun removeUpdate(e: DocumentEvent) = applyOnChange()
            override fun changedUpdate(e: DocumentEvent) = applyOnChange()
        })
        typeCombo.addActionListener { applyFilters() }
        priorityCombo.addActionListener { applyFilters() }
        mineCheck.addActionListener { applyFilters() }

        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) navigateToSelected()
            }
        })
        table.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) navigateToSelected()
            }
        })

        // Debounced auto-refresh on edits.
        PsiManager.getInstance(project).addPsiTreeChangeListener(
            object : PsiTreeChangeAdapter() {
                override fun childAdded(event: PsiTreeChangeEvent) = scheduleRefresh()
                override fun childRemoved(event: PsiTreeChangeEvent) = scheduleRefresh()
                override fun childReplaced(event: PsiTreeChangeEvent) = scheduleRefresh()
                override fun childrenChanged(event: PsiTreeChangeEvent) = scheduleRefresh()
                override fun propertyChanged(event: PsiTreeChangeEvent) = scheduleRefresh()
            },
            parentDisposable,
        )
    }

    private fun scheduleRefresh() {
        refreshAlarm.cancelAllRequests()
        refreshAlarm.addRequest({ refresh() }, REFRESH_DELAY_MS)
    }

    fun refresh() {
        ReadAction.nonBlocking(Callable { service.scan() })
            .expireWith(parentDisposable)
            .finishOnUiThread(ModalityState.defaultModalityState()) { entries ->
                allEntries = entries
                refreshTypeCombo()
                applyFilters()
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun refreshTypeCombo() {
        val types = allEntries.map { it.type }.distinct().sorted()
        val previous = typeCombo.selectedItem
        typeCombo.removeAllItems()
        typeCombo.addItem(ALL_TYPES)
        types.forEach { typeCombo.addItem(it) }
        typeCombo.selectedItem = if (previous != null && (previous == ALL_TYPES || types.contains(previous))) previous else ALL_TYPES
    }

    private fun applyFilters() {
        val query = searchField.text.trim().lowercase()
        val selectedType = typeCombo.selectedItem as? String ?: ALL_TYPES
        val selectedPriority = priorityCombo.selectedItem as? TodoPriority
        val mineOnly = mineCheck.isSelected
        val me = System.getProperty("user.name")?.lowercase()

        val filtered = allEntries.filter { entry ->
            (selectedType == ALL_TYPES || entry.type == selectedType) &&
                (selectedPriority == null || entry.priority == selectedPriority) &&
                (!mineOnly || entry.assignee?.lowercase() == me) &&
                (query.isEmpty() || entry.matches(query))
        }

        model.items = filtered
        statusLabel.text = if (filtered.isEmpty()) {
            TodoEnhancerBundle["status.empty"]
        } else {
            TodoEnhancerBundle["status.count", filtered.size]
        }
    }

    private fun TodoEntry.matches(query: String): Boolean =
        description.lowercase().contains(query) ||
            type.lowercase().contains(query) ||
            tagsText.lowercase().contains(query) ||
            presentablePath.lowercase().contains(query) ||
            (assignee?.lowercase()?.contains(query) == true)

    private fun navigateToSelected() {
        val entry = table.selectedObject ?: return
        if (!entry.file.isValid) return
        OpenFileDescriptor(project, entry.file, entry.offset).navigate(true)
    }

    private companion object {
        const val ALL_TYPES = "All types"
        const val ANY_PRIORITY = "Any priority"
        const val REFRESH_DELAY_MS = 800

        fun buildColumns(): Array<ColumnInfo<TodoEntry, *>> = arrayOf(
            column(TodoEnhancerBundle["column.priority"], { it.priority.label }, compareBy { it.priority.rank }),
            column(TodoEnhancerBundle["column.type"], { it.type }, compareBy { it.type }),
            column(TodoEnhancerBundle["column.assignee"], { it.assignee ?: "" }, compareBy { it.assignee ?: "" }),
            column(TodoEnhancerBundle["column.tags"], { it.tagsText }, compareBy { it.tagsText }),
            column(TodoEnhancerBundle["column.due"], { it.dueDate?.toString() ?: "" },
                compareBy(nullsLast()) { it.dueDate }),
            column(TodoEnhancerBundle["column.location"], { it.location },
                compareBy<TodoEntry> { it.presentablePath }.thenBy { it.line }),
            column(TodoEnhancerBundle["column.description"], { it.description }, compareBy { it.description }),
        )

        fun column(
            name: String,
            value: (TodoEntry) -> String,
            comparator: Comparator<TodoEntry>,
        ): ColumnInfo<TodoEntry, String> = object : ColumnInfo<TodoEntry, String>(name) {
            override fun valueOf(item: TodoEntry): String = value(item)
            override fun getComparator(): Comparator<TodoEntry> = comparator
        }
    }
}
