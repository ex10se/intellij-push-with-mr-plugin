package org.jetbrains.plugins.git.custompush.executors

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.changes.CommitSession
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import org.jetbrains.annotations.Nls
import org.jetbrains.plugins.git.custompush.actions.GitRepoAction

@Service(Service.Level.PROJECT)
class CustomCommitExecutor : CommitExecutor {
    var isCustomPushAfterCommit: Boolean = false
    
    @Nls
    override fun getActionText(): String = "Commit-Push-MR"
    
    override fun useDefaultAction(): Boolean = false
    
    override fun requiresSyncCommitChecks(): Boolean = true
    
    override fun getId(): String = ID
    
    override fun supportsPartialCommit(): Boolean = true
    
    override fun createCommitSession(commitContext: CommitContext): CommitSession {
        isCustomPushAfterCommit = true
        return CommitSession.VCS_COMMIT
    }
    
    companion object {
        internal const val ID = "Git.Commit.And.Push.Executor"
    }
}

class CustomCheckHandler(private val project: Project) : CheckinHandler() {
    override fun checkinSuccessful() {
        val executor = project.getService(CustomCommitExecutor::class.java)
        if (executor.isCustomPushAfterCommit) {
            GitRepoAction().perform(project)
            executor.isCustomPushAfterCommit = false
        }
    }
}

class CustomCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return CustomCheckHandler(panel.project)
    }
}