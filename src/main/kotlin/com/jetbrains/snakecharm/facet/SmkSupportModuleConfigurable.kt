package com.jetbrains.snakecharm.facet

import com.intellij.application.options.ModuleAwareProjectConfigurable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.snakecharm.SnakemakeBundle

/**
 * Registers project settings tab for facet. E.g. for PyCharm project settings tab ('supported frameworks'), because
 * PyCharm doesn't show facets in UI.
 */
class SmkSupportModuleConfigurable(project: Project) : ModuleAwareProjectConfigurable<SmkConfigurable>(
    project, SnakemakeBundle.message("facet.configurable.display.name"), "snakemake_support"
) {
    override fun createModuleConfigurable(module: Module): SmkConfigurable = SmkConfigurable(module)
}