package com.jetbrains.snakecharm

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.jetbrains.snakecharm.framework.SmkFrameworkConfigurableProvider

object SmkNotifier {
    private const val NOTIFICATION_GROUP_ID = "SnakeCharmPluginNotifier" // see plugin xml

    fun notifySnakefileDetected(module: Module) {
        NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID).createNotification(
            title = SnakemakeBundle.message("notifier.msg.framework.by.snakefile.title"),
            content = SnakemakeBundle.message("notifier.msg.framework.by.snakefile", module.name)
        ).addAction(object : NotificationAction(
            SnakemakeBundle.message("notifier.msg.framework.by.snakefile.action.configure")
        ) {
            override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(
                    module.project,
                    SmkFrameworkConfigurableProvider::class.java
                )
            }
        }).notify(module.project)
    }

    fun notify(content: String, type: NotificationType = NotificationType.INFORMATION, project: Project? = null) =
        NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(content, type).also {
                it.notify(project)
            }
}