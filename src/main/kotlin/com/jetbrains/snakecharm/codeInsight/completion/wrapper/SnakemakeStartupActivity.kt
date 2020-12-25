package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import kotlinx.serialization.ExperimentalSerializationApi

class SnakemakeStartupActivity : StartupActivity {
    @ExperimentalSerializationApi
    override fun runActivity(project: Project) {
        val smkSettings = project.service<SmkSupportProjectSettings>()
        smkSettings.initOnStartup()
        val smkWrapperStorage = project.service<SmkWrapperStorage>()
        smkWrapperStorage.initOnStartup()
        val implicitPySymbolsProvider = project.service<ImplicitPySymbolsProvider>()
        implicitPySymbolsProvider.initOnStartup()
    }
}