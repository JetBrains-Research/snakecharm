package com.jetbrains.snakecharm.framework

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.snakecharm.SnakemakeBundle

/**
 * Shows [EditorNotificationPanel] if 'Enable Snakemake support' setting is disabled
 */
class SmkSupportDisabledBanner : EditorNotifications.Provider<EditorNotificationPanel>() {

    private companion object {
        val KEY = Key.create<EditorNotificationPanel>("smk.content.notification.panel")
        val DISABLE_NOTIFICATION = Key.create<Boolean>("smk.notification.disabled.hide")
        var hideNotification = false
    }

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(
        file: VirtualFile,
        fileEditor: FileEditor,
        project: Project
    ): EditorNotificationPanel? {
        val editor = (fileEditor as? TextEditor)?.editor ?: return null
        val settings = SmkSupportProjectSettings.getInstance(project)
        if (hideNotification || editor.getUserData(DISABLE_NOTIFICATION) == true || !settings.snakemakeSupportBannerEnabled) {
            editor.putUserData(DISABLE_NOTIFICATION, false) // IDK how to update EditorNotifications
            // after SmkFrameworkConfigurableProvider closure, so we just disable it once, and enable it then
            return null
        }

        if (settings.snakemakeSupportEnabled) {
            return null
        }
        val panel = EditorNotificationPanel(fileEditor)
        panel.text = SnakemakeBundle.message("banner.smk.framework.nor.configured.message")
        panel.icon(AllIcons.General.Warning)
        panel.createActionLabel(SnakemakeBundle.message("notifier.msg.framework.by.snakefile.action.configure")) {
            editor.putUserData(DISABLE_NOTIFICATION, true)
            ShowSettingsUtil.getInstance().showSettingsDialog(
                project,
                SmkFrameworkConfigurableProvider::class.java
            )
            EditorNotifications.getInstance(project).updateNotifications(file)
        }
        panel.createActionLabel(SnakemakeBundle.message("banner.smk.framework.nor.configured.hide")) {
            // Hides notification for current session
            hideNotification = true
            EditorNotifications.getInstance(project).updateNotifications(file)
        }
        panel.createActionLabel(SnakemakeBundle.message("banner.smk.framework.nor.configured.dont.show.again")) {
            // Hides notification in current project
            SmkSupportProjectSettings.hideSmkSupportBanner(project)
            EditorNotifications.getInstance(project).updateAllNotifications()
        }

        return panel
    }
}