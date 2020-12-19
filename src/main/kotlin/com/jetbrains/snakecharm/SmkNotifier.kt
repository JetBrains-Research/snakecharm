package com.jetbrains.snakecharm

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.jetbrains.snakecharm.framework.SmkFrameworkConfigurableProvider

class SmkNotifier {
    companion object {
        val NOTIFICATION_GROUP = NotificationGroup(
            SnakemakeBundle.message("notifier.group.title"),
            NotificationDisplayType.BALLOON,
            true
        )
    }

    fun notifySnakefileDetected(module: Module) {
        NOTIFICATION_GROUP.createNotification(
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
        NOTIFICATION_GROUP.createNotification(content, type).also {
            it.notify(project)
        }
}