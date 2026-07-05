/*
 * Factory registered in plugin.xml that builds the "TODO Enhancer" tool window when the
 * IDE first opens it. Creates a `TodoEnhancerPanel` tied to the tool window's disposable
 * so its listeners are cleaned up automatically, and floats it in its own window the first
 * time it is shown so the card stack reads as a detached sticky-note widget.
 */
package com.github.asadsq.jbtodoenhancer.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowType
import com.intellij.ui.content.ContentFactory

class TodoEnhancerToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setIcon(AllIcons.Toolwindows.ToolWindowTodo)

        val panel = TodoEnhancerPanel(project, toolWindow.disposable)
        val content = ContentFactory.getInstance().createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)

        // The first time this project ever opens the tool window, detach it into its own
        // window so the deck feels like a floating sticky note. After that we leave the type
        // alone, so if the user re-docks it the platform remembers that choice on later opens.
        val props = PropertiesComponent.getInstance(project)
        if (!props.getBoolean(WINDOWED_DEFAULT_APPLIED_KEY, false)) {
            props.setValue(WINDOWED_DEFAULT_APPLIED_KEY, true)
            toolWindow.setType(ToolWindowType.WINDOWED, null)
        }
    }

    override fun shouldBeAvailable(project: Project) = true

    private companion object {
        const val WINDOWED_DEFAULT_APPLIED_KEY = "todoEnhancer.windowedDefaultApplied"
    }
}
