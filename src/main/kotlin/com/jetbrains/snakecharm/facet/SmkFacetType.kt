package com.jetbrains.snakecharm.facet

import com.intellij.facet.*
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.jetbrains.python.PythonModuleTypeBase
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.SnakemakeIcons

class SmkFacetType : FacetType<SnakemakeFacet, SmkFacetConfiguration>(
    ID, ID.toString(), SnakemakeBundle.message("facet.display.name")
) {
    companion object {
        val ID = FacetTypeId<SnakemakeFacet>("SnakemakeFacetType")

        val INSTANCE: SmkFacetType
            get() = findInstance(SmkFacetType::class.java)

        /**
         * Manually create facet configuration, e.g. for PyCharm settings tab ('supported frameworks'), because
         * PyCharm doesn't show facets in UI
         */
        @JvmStatic
        fun createDefaultConfiguration(project: Project) =
            ProjectFacetManager.getInstance(project).createDefaultConfiguration(INSTANCE)!!

        /**
         * Manually create facet instance and add to module, e.g. for PyCharm settings tab ('supported frameworks'),
         * because pyCharm doesn't show facets in UI
         */
        @JvmStatic
        fun createAndAddFacet(module: Module, configuration: SmkFacetConfiguration) {
            val facetManager = FacetManager.getInstance(module)
            val model = facetManager.createModifiableModel()
            val facet = facetManager.createFacet(
                INSTANCE, INSTANCE.defaultFacetName, configuration, null
            )
            model.addFacet(facet)
            WriteAction.run<RuntimeException> { model.commit() }
        }

        private fun getSdk(module: Module): Sdk? = ModuleRootManager.getInstance(module).sdk
    }

    override fun createFacet(
        module: Module,
        name: String,
        configuration: SmkFacetConfiguration,
        underlyingFacet: Facet<*>?
    ) = SnakemakeFacet(this, module, name, configuration, underlyingFacet, getSdk(module))

    override fun createDefaultConfiguration() = SmkFacetConfiguration()

    override fun isSuitableModuleType(moduleType: ModuleType<*>?): Boolean {
        // XXX let's allow in python modules only, but actually some user could
        // also want this in other language module + python facet
        return moduleType is PythonModuleTypeBase
    }

    override fun getIcon() = SnakemakeIcons.FACET
}