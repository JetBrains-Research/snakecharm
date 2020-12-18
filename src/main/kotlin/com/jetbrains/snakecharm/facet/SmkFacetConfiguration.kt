package com.jetbrains.snakecharm.facet

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.FacetManager
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.xmlb.annotations.Attribute

//@State(name = "SmkFacetConfiguration", storages = [(Storage("snakemake-facet-config.xml"))])
class SmkFacetConfiguration : FacetConfiguration, PersistentStateComponent<SmkFacetConfiguration.State> {
    private var internalState = State()

    val wrappersCustomSourcesFolder: String
        get() {
            return internalState.wrappersCustomSourcesFolder!!
        }

    val useBundledWrappersInfo: Boolean
        get() {
            return internalState.useBundledWrappersInfo
        }

    override fun createEditorTabs(
        editorContext: FacetEditorContext,
        validatorsManager: FacetValidatorsManager
    ): Array<FacetEditorTab> =
        arrayOf(SmkFacetEditorTab(this, editorContext, validatorsManager))

    /**
     * Used for serialization to project settings
     */
    override fun getState(): State {
        return internalState
    }

    /**
     * Internal method, do not use it in user code, use [SmkFacetConfiguration.setStateAndFireEvent]
     *
     * Change state from serialized state, settings page and other specific places which fire proper
     * facet configuration change event
     */
    override fun loadState(state: State) {
        // Use 'XmlSerializerUtil.copyBean(internalState, state);' instead of assignment
        internalState = state
    }

    companion object {
        /**
         * A way how to change facet settings from API, not using settings.
         *
         * P.S: Not sure that we really need this
         */
        fun setStateAndFireEvent(facet: SnakemakeFacet, newState: State) {
            val publisher = facet.module.messageBus.syncPublisher(FacetManager.FACETS_TOPIC)
            // no before* event in facets topic
            facet.configuration.loadState(newState)
            publisher.facetConfigurationChanged(facet)
            // no after* event in facets topic
        }
    }

    class State : BaseState() {
        @get:Attribute("custom_wrappers")
        var useBundledWrappersInfo by property(true)

        @get:Attribute("use_custom_wrappers")
        var wrappersCustomSourcesFolder by string("")
    }

    /*
    @kotlinx.serialization.Serializable
    data class SmkFacetState(
        var useBundledWrappersInfo: Boolean = true,
        var wrappersCustomSourcesFolder: String = ""
    )

     */
    /*
    data class SmkFacetState(
        @JvmField var useBundledWrappersInfo: Boolean = true,
        @JvmField var wrappersCustomSourcesFolder: String = ""
    ) : Serializable {
        fun getWrappersCustomSourcesFolder() = wrappersCustomSourcesFolder
        fun getUseBundledWrappersInfo() = useBundledWrappersInfo
    }
    */
}