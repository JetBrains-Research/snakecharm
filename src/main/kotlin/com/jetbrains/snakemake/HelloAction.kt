package com.jetbrains.snakemake

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages


/**
 * @author Roman.Chernyatchik
 * @date 2018-12-30
 */
class HelloAction : AnAction("Hello") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project
        Messages.showMessageDialog(project, "Hello world!", "Greeting", Messages.getInformationIcon())
    }
}