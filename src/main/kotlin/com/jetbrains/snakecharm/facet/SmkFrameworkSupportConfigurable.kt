package com.jetbrains.snakecharm.facet

import com.intellij.facet.ProjectFacetManager
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.libraries.Library

/**
 * Facet configuration step settings in new project wizard
 */
class SmkFrameworkSupportConfigurable(model: FrameworkSupportModel) : FrameworkSupportConfigurable() {
    val settingsPanel = SmkFacetSettingsPanel(model.project)

    override fun getComponent() = settingsPanel

    override fun addSupport(module: Module, model: ModifiableRootModel, library: Library?) {
        val facetType = SmkFacetType.INSTANCE
        val config =  ProjectFacetManager.getInstance(module.project).createDefaultConfiguration(facetType);
        settingsPanel.apply(config)

        SmkFacetType.createAndAddFacet(module, config)
    }

    override fun onFrameworkSelectionChanged(selected: Boolean) {
        if (selected) {
            settingsPanel.updateWrappersSrcPanelEnabled()
        }
    }
}
