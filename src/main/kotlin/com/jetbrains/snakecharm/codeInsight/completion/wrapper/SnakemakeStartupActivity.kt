package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.framework.SmkFrameworkDeprecationProvider
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
class SnakemakeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val smkSettings = project.service<SmkSupportProjectSettings>()
        smkSettings.initOnStartup()
        val smkWrapperStorage = project.service<SmkWrapperStorage>()
        smkWrapperStorage.initOnStartup()

        val implicitPySymbolsProvider = project.service<ImplicitPySymbolsProvider>()
        implicitPySymbolsProvider.initOnStartup()

        ApplicationManager.getApplication().getService(SmkFrameworkDeprecationProvider::class.java)
    }
}