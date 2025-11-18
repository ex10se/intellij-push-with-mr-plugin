package org.jetbrains.plugins.git.custompush.actions

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.ui.components.JBOptionButton
import org.jetbrains.plugins.git.custompush.executors.CustomCommitExecutor
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

class CustomCommitAndPushAction : AnAction(), CustomComponentAction {
    private val gitRepoAction: GitRepoAction = GitRepoAction()

    override fun actionPerformed(e: AnActionEvent) {
        commitAndPush(e.dataContext)
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val mainAction = createMainButtonAction(presentation)
        val options = createOptionsArray()

        return JBOptionButton(mainAction, options).apply {
            text = presentation.text
            icon = presentation.icon
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    private fun createMainButtonAction(presentation: Presentation) = object : AbstractAction(presentation.text) {
        override fun actionPerformed(e: ActionEvent) {
            val dataContext = DataManager.getInstance().getDataContext(e.source as JComponent)
            commitAndPush(dataContext)
        }
    }

    private fun commitAndPush(dataContext: DataContext) {
        val commitWorkflow = dataContext.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER)
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        commitWorkflow?.execute(
            project?.getService(CustomCommitExecutor::class.java) ?: error("No project found")
        )
    }

    private fun createOptionsArray(): Array<Action> = arrayOf(
        object : AbstractAction("Push-MR") {
            override fun actionPerformed(e: ActionEvent) {
                val dataContext = DataManager.getInstance().getDataContext(e.source as JComponent)
                val project = CommonDataKeys.PROJECT.getData(dataContext)
                gitRepoAction.perform(project ?: error("No project found"))
            }
        }
    )

    override fun update(e: AnActionEvent) {
        e.presentation.apply {
            text = "Commit-Push-MR"
        }
    }
}