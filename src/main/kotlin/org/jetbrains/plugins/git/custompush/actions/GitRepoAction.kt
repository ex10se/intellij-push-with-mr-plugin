package org.jetbrains.plugins.git.custompush.actions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vcs.changes.ChangeListManager
import git4idea.GitUtil
import git4idea.GitVcs
import git4idea.commands.*
import git4idea.repo.GitBranchTrackInfo
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import java.awt.Desktop
import java.net.URI

class GitRepoAction {
    private lateinit var repository: GitRepository
    private var trackInfo: GitBranchTrackInfo? = null
    private val outputLines = mutableListOf<String>()

    fun perform(project: Project) {
        outputLines.clear() // Очищаем перед каждым push
        try {
            getRepositoryAndTrackInfo(project)
            val pushOptions = generatePushOptions()
            runPush(project, pushOptions)
        } catch (e: Exception) {
            ApplicationManager.getApplication().invokeLater {
                VcsNotifier.getInstance(project).notifyError("git.custompush.error", "Error", e.message ?: "Unknown error")
            }
        }
    }

    private fun getRepositoryAndTrackInfo(project: Project) {
        val repositories = GitRepositoryManager.getInstance(project).repositories
        val projectDir = project.guessProjectDir()

        // Ищем корневой репозиторий (не сабмодуль)
        repository = repositories.find { repo ->
            repo.root == projectDir
        } ?: repositories.firstOrNull() ?: error("No git repository found")
        trackInfo = GitUtil.getTrackInfoForCurrentBranch(repository)
    }

    private fun generatePushOptions(): List<String> {
        val options = mutableListOf<String>()
        options.addAll(listOf("-o", "merge_request.create"))
        val changelistTitle = getCurrentChangeListTitle()
        if (changelistTitle.isNotEmpty()) {
            options.addAll(listOf("-o", "merge_request.title=$changelistTitle"))
        }
        if (repository.currentBranchName?.startsWith("hotfix/") == true) {
            options.addAll(listOf("-o", "merge_request.target=master"))
        }
        return options
    }

    private fun getCurrentChangeListTitle(): String {
        return try {
            ChangeListManager.getInstance(repository.project).defaultChangeList.name
        } catch (_: Exception) {
            ""
        }
    }

    private fun runPush(project: Project, pushOptions: List<String>) {
        val task = object : Task.Backgroundable(project, "Git Push with MR options") {
            override fun run(indicator: ProgressIndicator) {
                val result = push(repository, pushOptions)
                handleResult(project, result)
            }
        }
        GitVcs.runInBackground(task)
    }

    private fun handleResult(project: Project, result: GitCommandResult) {
        val notifier = VcsNotifier.getInstance(project)
        if (result.success()) {
            notifier.notifySuccess("git.custompush.success", "Push success", "Commits pushed successfully")
            // Парсим URL merge request из собранных строк вывода
            val output = outputLines.joinToString("\n")
            val mergeRequestUrl = extractMergeRequestUrl(output)
            if (mergeRequestUrl != null) {
                ApplicationManager.getApplication().invokeLater {
                    openUrlInBrowser(mergeRequestUrl)
                }
            }
        } else {
            notifier.notifyError("git.custompush.error", "Push error", result.errorOutputAsJoinedString)
        }
    }

    private fun extractMergeRequestUrl(output: String): String? {
        // Паттерн для "View merge request" - URL на следующей строке после "remote:"
        val pattern = Regex(
            """(?i)(?m)remote:\s*View merge request for[^\n]*\n\s*remote:\s*(https?://[^\s\n]+/-/merge_requests/\d+)"""
        )
        val match = pattern.find(output)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        // Альтернативный паттерн - прямая ссылка
        val directPattern = Regex("""(https?://[^\s\n]+/-/merge_requests/\d+)""")
        return directPattern.find(output)?.value?.trim()
    }

    private fun openUrlInBrowser(url: String) {
        try {
            val uri = URI(url)
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri)
            } else {
                openUrlFallback(url)
            }
        } catch (_: Exception) {
            // Игнорируем ошибки открытия браузера
        }
    }

    private fun openUrlFallback(url: String) {
        try {
            val osName = System.getProperty("os.name").lowercase()
            val command = when {
                osName.contains("windows") -> "rundll32 url.dll,FileProtocolHandler $url"
                osName.contains("mac") -> "open $url"
                else -> "xdg-open $url"
            }
            Runtime.getRuntime().exec(command)
        } catch (_: Exception) {
            // Игнорируем ошибки
        }
    }

    private fun push(repository: GitRepository, pushOptions: List<String>): GitCommandResult {
        val remote = trackInfo?.remote ?: repository.remotes.firstOrNull()
        val url = remote?.firstUrl
        val handler = GitLineHandler(repository.project, repository.root, GitCommand.PUSH)
        if (url != null) handler.setUrl(url)
        handler.addParameters("--set-upstream")
        handler.addParameters("origin")
        repository.currentBranchName?.let { handler.addParameters(it) }
        handler.addParameters("--progress")
        pushOptions.forEach { handler.addParameters(it) }
        handler.setSilent(false)
        handler.setStdoutSuppressed(false)

        // Добавляем listener для сбора всех строк вывода
        handler.addLineListener { line, _ -> outputLines.add(line) }
        handler.addLineListener(GitStandardProgressAnalyzer.createListener(EmptyProgressIndicator()))

        return Git.getInstance().runCommand { handler }
    }
}
