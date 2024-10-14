package com.jetbrains.snakecharm.framework

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import com.jetbrains.python.PyNames
import com.jetbrains.snakecharm.SnakemakeIcons

@Service(Service.Level.PROJECT)
@State(name = "SmkProjectSettings", storages = [Storage("snakemake-settings.xml")])
class SmkSupportProjectSettings(val project: Project) : PersistentStateComponent<SmkSupportProjectSettings.State>,
    Disposable {
    private var internalState = State()

    /**
     * Please do not use this, use [stateSnapshot] instead or filed getters. This method is only platform API endpoint
     * for serialization to project settings
     */
    override fun getState(): State = internalState

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

    val useProjectSdk: Boolean
        get() {
            return internalState.pythonSdkName.isNullOrEmpty()
        }

    val sdkName: String?
        get() {
            return internalState.pythonSdkName
        }

    val snakemakeSupportBannerEnabled: Boolean
        get() {
            return internalState.snakemakeSupportBannerEnabled
        }

    val snakemakeLanguageVersion: String?
        get() {
            return internalState.snakemakeLanguageVersion
        }

    fun getActiveSdk() = when {
        !snakemakeSupportEnabled -> null
        else -> findPythonSdk(project, internalState.pythonSdkName)
    }

    fun stateSnapshot(): State {
        val snapshot = State()
        snapshot.copyFrom(state)
        return snapshot
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

    fun initOnStartup() {
        val connection = project.messageBus.connect()

        // Listen SDK removed changed
        connection.subscribe(ProjectJdkTable.JDK_TABLE_TOPIC, object : ProjectJdkTable.Listener {
            override fun jdkNameChanged(jdk: Sdk, previousName: String) {
                if (sdkName == previousName) {
                    val newState = stateSnapshot()
                    newState.pythonSdkName = jdk.name
                    
                    updateStateAndFireEvent(project, newState, sdkRenamed = true, sdkRemoved = false)
                }
            }

            override fun jdkRemoved(jdk: Sdk) {
                // happens before sdk removal
                if (sdkName == jdk.name) {
                    // leave invalid settings here
                    updateStateAndFireEvent(project, stateSnapshot(), sdkRenamed = false, sdkRemoved = true)
                }
            }

        })

        Disposer.register(this, connection)
    }

    class State : BaseState() {
        @get:Attribute("enabled")
        var snakemakeSupportEnabled by property(false)

        @get:Attribute("custom_wrappers")
        var useBundledWrappersInfo by property(true)

        @get:Attribute("use_custom_wrappers")
        var wrappersCustomSourcesFolder by string("")

        @get:Attribute("sdk")
        var pythonSdkName by string("")

        @get:Attribute("smk_support_banner_enabled")
        var snakemakeSupportBannerEnabled by property(true)

        @get:Attribute("smk_language_version")
        var snakemakeLanguageVersion by string(SnakemakeFrameworkAPIProvider.getInstance().getDefaultVersion())
    }

    companion object {
        val TOPIC = Topic(SmkSupportProjectSettingsListener::class.java, Topic.BroadcastDirection.TO_PARENT)

        fun getInstance(project: Project) = project.getService(SmkSupportProjectSettings::class.java)!!

        fun addSupport(project: Project) {
            val newState = State()
            newState.snakemakeSupportEnabled = true
            updateStateAndFireEvent(project, newState)
        }

        fun hideSmkSupportBanner(project: Project) {
            val newState = State()
            newState.snakemakeSupportBannerEnabled = false
            updateStateAndFireEvent(project, newState)
        }

        fun updateStateAndFireEvent(project: Project, newState: State) {
            ApplicationManager.getApplication().runWriteAction {
                updateStateAndFireEvent(project, newState, false, false)
            }
        }

        private fun updateStateAndFireEvent(
            project: Project, newState: State,
            sdkRenamed: Boolean,
            sdkRemoved: Boolean
        ) {
            val settings = getInstance(project)

            val publisher = project.messageBus.syncPublisher(TOPIC)
            // no before* event in facets topic

            val oldState = settings.stateSnapshot()

            settings.loadState(newState)

            if (oldState.snakemakeSupportEnabled && !newState.snakemakeSupportEnabled) {
                // disabled
                publisher.disabled(settings)
            } else if (!oldState.snakemakeSupportEnabled && newState.snakemakeSupportEnabled) {
                // enabled
                publisher.enabled(settings)
            } else {
                publisher.stateChanged(settings, oldState, sdkRenamed, sdkRemoved)
            }

            // no after* event in facets topic
            DaemonCodeAnalyzer.getInstance(project).restart()
        }

        fun findPythonSdk(project: Project, sdkName: String?): Sdk? {
            val sdk: Sdk? = if (sdkName.isNullOrEmpty()) {
                ProjectRootManager.getInstance(project).projectSdk
            } else {
                ProjectJdkTable.getInstance().findJdk(sdkName)
            }
            return if (sdk != null && isPythonSdk(sdk)) sdk else null
        }

        fun isPythonSdk(sdk: Sdk) = PyNames.PYTHON_SDK_ID_NAME == sdk.sdkType.name
        fun getIcon() = SnakemakeIcons.FACET
    }


    override fun dispose() {
        // Do nothing
    }
}

interface SmkSupportProjectSettingsListener {
    fun stateChanged(
        newSettings: SmkSupportProjectSettings,
        oldState: SmkSupportProjectSettings.State,
        sdkRenamed: Boolean,
        sdkRemoved: Boolean
    ) {}
    fun enabled(newSettings: SmkSupportProjectSettings)  {}
    fun disabled(newSettings: SmkSupportProjectSettings) {}
}