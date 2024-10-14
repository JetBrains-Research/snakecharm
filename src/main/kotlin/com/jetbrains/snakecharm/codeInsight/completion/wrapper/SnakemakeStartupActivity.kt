package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPIProjectService
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SnakemakeFrameworkAPIProvider
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
class SnakemakeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        SnakemakeFrameworkAPIProvider.getInstance() // other classes could use it implicitly

        val smkSettings = project.service<SmkSupportProjectSettings>()
        smkSettings.initOnStartup()

        SnakemakeAPIProjectService.getInstance(project).initOnStartup(smkSettings) // other classes could use it implicitly

        val smkWrapperStorage = project.service<SmkWrapperStorage>()
        smkWrapperStorage.initOnStartup()

        val implicitPySymbolsProvider = project.service<ImplicitPySymbolsProvider>()
        implicitPySymbolsProvider.initOnStartup()
    }
}