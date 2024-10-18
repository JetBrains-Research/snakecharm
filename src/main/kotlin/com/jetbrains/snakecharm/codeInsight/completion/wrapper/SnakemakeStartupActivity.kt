package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.snakecharm.codeInsight.SmkImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SnakemakeApiYamlAnnotationsService
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
class SnakemakeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        SnakemakeApiYamlAnnotationsService.getInstance() // other classes could use it implicitly

        val smkSettings = project.service<SmkSupportProjectSettings>()
        smkSettings.initOnStartup()

        SnakemakeApiService.getInstance(project).initOnStartup(smkSettings) // other classes could use it implicitly

        val smkWrapperStorage = project.service<SmkWrapperStorage>()
        smkWrapperStorage.initOnStartup()

        val implicitPySymbolsProvider = project.service<SmkImplicitPySymbolsProvider>()
        implicitPySymbolsProvider.initOnStartup()
    }
}