package com.jetbrains.snakecharm.framework

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import java.util.function.Function

class SmkSupportDisabledBannerNotification {
    var hiddenUntilRestart = false
    companion object {
        fun getInstance(): SmkSupportDisabledBannerNotification =
            ApplicationManager.getApplication().getService(SmkSupportDisabledBannerNotification::class.java)
    }
}

/**
 * Shows [EditorNotificationPanel] if 'Enable Snakemake support' setting is disabled
 */
class SmkSupportDisabledBannerProvider : EditorNotificationProvider {
    private companion object {
        val DISABLE_NOTIFICATION = Key.create<Boolean>("smk.notification.disabled.hide")
    }

    private fun createNotificationPanel(
        file: VirtualFile,
        fileEditor: FileEditor,
        project: Project,
    ): EditorNotificationPanel? {
        val editor = (fileEditor as? TextEditor)?.editor ?: return null
        val psiFile = PsiManager.getInstance(project).findFile(file)
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(psiFile)) {
            return null
        }
        val settings = SmkSupportProjectSettings.getInstance(project)
        val isHidden = SmkSupportDisabledBannerNotification.getInstance().hiddenUntilRestart
        if (isHidden || editor.getUserData(DISABLE_NOTIFICATION) == true || !settings.snakemakeSupportBannerEnabled) {
            editor.putUserData(DISABLE_NOTIFICATION, false) // IDK how to update EditorNotifications
            // after SmkFrameworkConfigurableProvider closure, so we just disable it once, and enable it then
            return null
        }

        if (settings.snakemakeSupportEnabled) {
            return null
        }
        val panel = EditorNotificationPanel(fileEditor)
        panel.text = SnakemakeBundle.message("banner.smk.framework.not.configured.message")
        panel.icon(AllIcons.General.Warning)
        panel.createActionLabel(SnakemakeBundle.message("notifier.msg.framework.by.snakefile.action.configure")) {
            editor.putUserData(DISABLE_NOTIFICATION, true)
            ShowSettingsUtil.getInstance().showSettingsDialog(
                project,
                SmkFrameworkConfigurableProvider::class.java
            )
            EditorNotifications.getInstance(project).updateNotifications(file)
        }
        panel.createActionLabel(SnakemakeBundle.message("banner.smk.framework.not.configured.hide")) {
            // Hides notification for current session
            SmkSupportDisabledBannerNotification.getInstance().hiddenUntilRestart = true
            EditorNotifications.getInstance(project).updateNotifications(file)
        }
        panel.createActionLabel(SnakemakeBundle.message("banner.smk.framework.not.configured.dont.show.again")) {
            // Hides notification for current project
            SmkSupportProjectSettings.hideSmkSupportBanner(project)
            EditorNotifications.getInstance(project).updateAllNotifications()
        }

        return panel
    }

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ) = Function { fileEditor: FileEditor? ->
        createNotificationPanel(file, fileEditor!!, project)
    }
}