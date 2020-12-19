package com.jetbrains.snakecharm.framework

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.SnakemakeIcons
import com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperLoaderStartupActivity
import com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperStorage

@State(name = "SmkProjectSettings", storages=[Storage("snakemake-settings.xml")] )
class SmkSupportProjectSettings(project: Project) : PersistentStateComponent<SmkSupportProjectSettings.State> {
    private var internalState = State()

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(TOPIC, object : SmkSupportProjectSettingsListener {
            override fun stateChanged(newSettings: SmkSupportProjectSettings) {
                if (!newSettings.snakemakeSupportEnabled) {
                    // clean wrappers
                    project.getService(SmkWrapperStorage::class.java).initFrom(
                        "", emptyList()
                    )
                    return
                }

                if (ApplicationManager.getApplication().isUnitTestMode) {
                    // Do now, in in BG
                    forceLoadOrCollectWrappers(project)
                    return
                }
                ApplicationManager.getApplication().invokeLater {
                    ProgressManager.getInstance().run(object : Task.Backgroundable(
                        project,
                        SnakemakeBundle.message("wrappers.parsing.progress.collecting.data"),
                        true
                    ) {
                        override fun run(indicator: ProgressIndicator) {
                            forceLoadOrCollectWrappers(project)
                        }
                    })
                }
            }

            private fun forceLoadOrCollectWrappers(project: Project) {
                SmkWrapperLoaderStartupActivity.loadOrCollectLocalWrappers(project, true)
            }


        })
        Disposer.register(project, connection)
    }

    /**
     * Used for serialization to project settings
     */
    override fun getState(): State {
        return internalState
    }

    val snakemakeSupportEnabled: Boolean
        get() {
            return internalState.snakemakeSupportEnabled
        }

    val wrappersCustomSourcesFolder: String
        get() {
            return internalState.wrappersCustomSourcesFolder!!
        }

    val useBundledWrappersInfo: Boolean
        get() {
            return internalState.useBundledWrappersInfo
        }

    /**
     * Internal method, do not use it in user code, use [SmkSupportProjectSettings.updateStateAndFireEvent]
     *
     * Change state from serialized state, settings page and other specific places which fire proper
     * facet configuration change event
     */
    override fun loadState(state: State) {
        // Use 'XmlSerializerUtil.copyBean(internalState, state);' instead of assignment
        internalState = state
    }

    class State : BaseState() {
        @get:Attribute("enabled")
        var snakemakeSupportEnabled by property(false)

        @get:Attribute("custom_wrappers")
        var useBundledWrappersInfo by property(true)

        @get:Attribute("use_custom_wrappers")
        var wrappersCustomSourcesFolder by string("")
    }

    companion object {
        private val TOPIC = Topic(SmkSupportProjectSettingsListener::class.java, Topic.BroadcastDirection.TO_PARENT)

        fun getInstance(project: Project) = project.getService(SmkSupportProjectSettings::class.java)!!

        fun addSupport(project: Project) {
            val newState = State()
            newState.snakemakeSupportEnabled = true
            updateStateAndFireEvent(project, newState)
        }

        fun updateStateAndFireEvent(project: Project, newState: State) {
            val settings = getInstance(project)

            val publisher = project.messageBus.syncPublisher(TOPIC)
            // no before* event in facets topic
            settings.loadState(newState)
            publisher.stateChanged(settings)
            // no after* event in facets topic
        }

        fun getIcon() = SnakemakeIcons.FACET
    }
}

interface SmkSupportProjectSettingsListener {
    fun stateChanged(
        newSettings: SmkSupportProjectSettings
    )
}