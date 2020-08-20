package com.jetbrains.snakecharm.facet

import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel
import com.intellij.ide.util.frameworkSupport.FrameworkVersion
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel

/**
 * In IDEA this code tells new project dialog wizard that it should suggest add Snakemake facet (if project module type in python)
 */
class SmkFrameworkSupportProvider : FacetBasedFrameworkSupportProvider<SnakemakeFacet>(
    SmkFacetType.INSTANCE
) {
    override fun isSupportAlreadyAdded(module: Module) = SnakemakeFacet.isPresent(module)

    override fun createConfigurable(model: FrameworkSupportModel): FrameworkSupportConfigurable {
        return SmkFrameworkSupportConfigurable(model)    // Shows settings in project creation wizard
    }

    /**
     * N/A: Only used if default createConfigurable is used.
     * In our case see: [SmkFrameworkSupportConfigurable.addSupport]
     */
    override fun setupConfiguration(
        facet: SnakemakeFacet,
        rootModel: ModifiableRootModel,
        version: FrameworkVersion?
    ) {
        // N/A
    }

    override fun onFacetCreated(facet: SnakemakeFacet?, rootModel: ModifiableRootModel?, version: FrameworkVersion?) {
        super.onFacetCreated(facet, rootModel, version)
        // Do some actions if needed
    }
}
