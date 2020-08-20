package com.jetbrains.snakecharm.facet

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.FacetManager
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager

class SmkFacetConfiguration : FacetConfiguration {
    companion object {
        /**
         * A way how to change facet settings from API, not using settings.
         *
         * P.S: Not sure that we really need this
         */
        fun setStateAndFireEvent(facet: SnakemakeFacet, newState: SmkFacetState) {
            val publisher = facet.module.messageBus.syncPublisher(FacetManager.FACETS_TOPIC)
            // no before* event in facets topic
            facet.configuration.setStateInternal(newState)
            publisher.facetConfigurationChanged(facet)
            // no after* event in facets topic
        }
    }

    var state = SmkFacetState()
        private set

    /**
     * Change state from settings page and other specific places which fire proper facet configuration change event
     */
    internal fun setStateInternal(newState: SmkFacetState) {
        state = newState
    }

    override fun createEditorTabs(editorContext: FacetEditorContext, validatorsManager: FacetValidatorsManager): Array<FacetEditorTab> =
        arrayOf(SmkFacetEditorTab(this, editorContext, validatorsManager))
}