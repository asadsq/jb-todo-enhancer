/*
 * Swing UI for the tool window: a cyclable stack of TODO cards driven by `CardStackPanel`, with a
 * slim search field and refresh in the toolbar and prev/next navigation below. Fetches data from
 * `TodoScanService` on a background read action and auto-refreshes (debounced) on PSI changes.
 */
package com.github.asadsq.jbtodoenhancer.toolWindow

import com.github.asadsq.jbtodoenhancer.TodoEnhancerBundle
import com.github.asadsq.jbtodoenhancer.model.TodoEntry
import com.github.asadsq.jbtodoenhancer.services.TodoScanService
import com.github.asadsq.jbtodoenhancer.ui.CardStackPanel
import com.github.asadsq.jbtodoenhancer.ui.RoundedButton
import com.github.asadsq.jbtodoenhancer.ui.RoundedPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.util.concurrent.Callable
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Tool window content: a stack of TODO cards you can flip through. Clicking a card (or pressing
 * Enter on the nav buttons) navigates to the TODO in the editor. The slim search filters which
 * cards are in the deck; the deck refreshes on demand and automatically (debounced) on PSI edits.
 */
class TodoEnhancerPanel(
    private val project: Project,
    private val parentDisposable: Disposable,
) : SimpleToolWindowPanel(true, true) {

    private val service = project.service<TodoScanService>()

    private val searchField = SearchTextField(false)
    private val stack = CardStackPanel { entry -> navigateTo(entry) }
    private val counterLabel = JBLabel("", SwingConstants.CENTER)
    private val prevButton = RoundedButton("", AllIcons.Actions.Back)
    private val nextButton = RoundedButton("", AllIcons.Actions.Forward)

    private val refreshAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, parentDisposable)

    private var allEntries: List<TodoEntry> = emptyList()

    init {
        // A portrait, sticky-note-ish hint for the initial floating-window size.
        preferredSize = JBUI.size(340, 460)
        toolbar = buildToolbar()
        setContent(buildContent())

        stack.onIndexChanged = { updateCounter() }
        installListeners()
        updateCounter()
        refresh()
    }

    private fun buildToolbar(): JComponent {
        val refreshButton = RoundedButton(TodoEnhancerBundle["action.refresh.text"], AllIcons.Actions.Refresh).apply {
            addActionListener { refresh() }
        }
        searchField.textEditor.emptyText.text = TodoEnhancerBundle["filter.search.hint"]
        searchField.textEditor.columns = 18

        val card = RoundedPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), JBUI.scale(6))).apply {
            border = JBUI.Borders.empty(6, 10)
            add(searchField)
            add(refreshButton)
        }
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(8, 8, 4, 8)
            add(card, BorderLayout.CENTER)
        }
    }

    private fun buildContent(): JComponent {
        prevButton.addActionListener { stack.prev() }
        nextButton.addActionListener { stack.next() }
        prevButton.toolTipText = TodoEnhancerBundle["nav.prev"]
        nextButton.toolTipText = TodoEnhancerBundle["nav.next"]
        counterLabel.foreground = JBUI.CurrentTheme.Label.disabledForeground()

        val navBar = JPanel(FlowLayout(FlowLayout.CENTER, JBUI.scale(14), 0)).apply {
            add(prevButton)
            add(counterLabel.apply { border = JBUI.Borders.empty(0, 8) })
            add(nextButton)
        }

        // Wheel over the deck flips cards, like thumbing through a physical pile.
        stack.addMouseWheelListener { e ->
            if (e.wheelRotation > 0) stack.next() else if (e.wheelRotation < 0) stack.prev()
        }

        val content = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(10, 14, 12, 14)
            add(stack, BorderLayout.CENTER)
            add(navBar, BorderLayout.SOUTH)
        }
        installArrowKeys(content)
        return content
    }

    private fun installArrowKeys(content: JComponent) {
        // Scoped to the content subtree, so left/right in the search field still moves the caret.
        val inputMap = content.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        val actionMap = content.actionMap
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "prevCard")
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "nextCard")
        actionMap.put("prevCard", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) = stack.prev()
        })
        actionMap.put("nextCard", object : javax.swing.AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) = stack.next()
        })
    }

    private fun installListeners() {
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = applyFilter()
            override fun removeUpdate(e: DocumentEvent) = applyFilter()
            override fun changedUpdate(e: DocumentEvent) = applyFilter()
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
                applyFilter()
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun applyFilter() {
        val query = searchField.text.trim().lowercase()
        val filtered = if (query.isEmpty()) allEntries else allEntries.filter { it.matches(query) }
        stack.setEntries(filtered)
    }

    private fun updateCounter() {
        val count = stack.count
        counterLabel.text = if (count == 0) "0 / 0" else TodoEnhancerBundle["status.position", stack.position, count]
        prevButton.isEnabled = count > 1
        nextButton.isEnabled = count > 1
    }

    private fun TodoEntry.matches(query: String): Boolean =
        description.lowercase().contains(query) ||
            type.lowercase().contains(query) ||
            tagsText.lowercase().contains(query) ||
            presentablePath.lowercase().contains(query) ||
            (assignee?.lowercase()?.contains(query) == true)

    private fun navigateTo(entry: TodoEntry) {
        if (!entry.file.isValid) return
        OpenFileDescriptor(project, entry.file, entry.offset).navigate(true)
    }

    private companion object {
        const val REFRESH_DELAY_MS = 800
    }
}
