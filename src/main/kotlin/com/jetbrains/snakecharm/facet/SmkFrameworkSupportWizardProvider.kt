package com.jetbrains.snakecharm.facet

import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProviderBase
import com.intellij.ide.util.frameworkSupport.FrameworkVersion
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.libraries.Library
import com.jetbrains.snakecharm.SnakemakeBundle

/**
 * In IDEA this code tells new project dialog wizard that it should suggest add Snakemake framework (if project module type in python)
 *
 * Here it is possible to provide a settings panel comp via [SmkFrameworkSupportWizardProvider.createConfigurable],
 * e.g. we could use [SmkSupportSettings] like component. But there are 2 limitations:
 *  - Validation of settings cannot be implemented here, e.g. check that directory exists. We could validate only on
 *  - Project is always null, so be careful with impl
 */
class SmkFrameworkSupportWizardProvider : FrameworkSupportProviderBase(
    "framework:${SmkSupportFrameworkType.ID}", SnakemakeBundle.message("smk.framework.display.name")
) {

    override fun isEnabledForModuleType(moduleType: ModuleType<*>) =
        SmkSupportFrameworkType.isSuitableModuleType(moduleType)

    override fun addSupport(
        module: Module,
        rootModel: ModifiableRootModel,
        version: FrameworkVersion?,
        library: Library?
    ) {
        // Doesn't happen, see [SmkFrameworkSupportWizardConfigurable.addSupport)
    }

    override fun createConfigurable(model: FrameworkSupportModel) =
        SmkFrameworkSupportWizardConfigurable(model.project)
}

class SmkFrameworkSupportWizardConfigurable(project: Project?) : FrameworkSupportConfigurable() {
    private val settingsPanel = SmkFacetSettingsPanel(project)

    override fun getComponent() = settingsPanel

    override fun addSupport(module: Module, model: ModifiableRootModel, library: Library?) {
        // get settings
        val uiState = SmkSupportProjectSettings.State()
        uiState.snakemakeSupportEnabled = true
        settingsPanel.apply(uiState)

        // apply
        SmkProjectConfigurable.applyUIStateToProject(uiState, model.project)
    }

    override fun onFrameworkSelectionChanged(selected: Boolean) {
        if (selected) {
            settingsPanel.updateWrappersSrcPanelEnabledPropertyRecursively()
        }
    }
}