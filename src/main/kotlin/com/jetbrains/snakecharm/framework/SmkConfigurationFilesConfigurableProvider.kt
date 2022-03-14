package com.jetbrains.snakecharm.framework

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.jetbrains.snakecharm.SnakemakeBundle
import javax.swing.JComponent

/**
 * Registers project settings tab for snakemake configuration files settings
 */
class SmkConfigurationFilesConfigurableProvider(private val project: Project) : Configurable, Configurable.NoScroll {
    private lateinit var projConfigurable: SearchableConfigurable

    override fun isModified() = projConfigurable.isModified
    override fun apply() = projConfigurable.apply()
    override fun reset() = projConfigurable.reset()
    override fun getDisplayName() =
        SnakemakeBundle.message("smk.framework.configurable.configuration.files.display.name")

    override fun createComponent(): JComponent? {
        if (project.isDefault) {
            return null
        }

        projConfigurable =
            SmkConfigurationFilesConfigurable(project)
        return projConfigurable.createComponent()
    }
}