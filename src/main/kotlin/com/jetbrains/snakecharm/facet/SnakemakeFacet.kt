package com.jetbrains.snakecharm.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetManagerAdapter
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Disposer
import com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperCrawler

class SnakemakeFacet(
    facetType: SmkFacetType,
    module: Module,
    name: String,
    configuration: SmkFacetConfiguration,
    underlyingFacet: Facet<*>?,
    sdk: Sdk?
) : Facet<SmkFacetConfiguration>(facetType, module, name, configuration, underlyingFacet) {

    companion object {
        @JvmStatic
        fun getInstance(module: Module) = FacetManager.getInstance(module).getFacetByType(SmkFacetType.ID)

        fun isPresent(module: Module?) = module != null && getInstance(module) != null
    }


    init {
        // TODO [romeo]: set sdk to facet settings: configuration.setSdk(sdk)
    }

    override fun initFacet() {
        super.initFacet()

        // TODO: subscribe on SDK changed: is old != new sdk => run implicit cache indexing activity
        // if (Project)event.getSource().isInitialized() => background, else register post startup activity
        // BG action: ProjectQueues.getInstance(project).queue("...",  new Task.Backgroundable(project, "...", module.getName()), true) { run() {..}}
        // configuration.subscribe(ProjectTopics.PROJECT_ROOTS, moduleRootListener)

        // Create events bus connection, e.g.
        val connection = module.messageBus.connect()

        // Subscribe on facet added/removed/facetConfigurationChanged (e.g. wrappers settings changed changed)
        connection.subscribe(FacetManager.FACETS_TOPIC, object : FacetManagerAdapter() {
            override fun facetConfigurationChanged(facet: Facet<*>) {
                SmkWrapperCrawler.runActivityModule(facet.module, true)
            }
        })

        Disposer.register(this, connection)
    }

    override fun disposeFacet() {
        //Do cleanup if needed, e.g. unsubscribe events
    }
}