package com.jetbrains.snakecharm.codeInsight

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.QualifiedName
import com.intellij.util.SlowOperations
import com.jetbrains.extensions.python.inherits
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.pyRequirement
import com.jetbrains.python.packaging.requirement.PyRequirementRelation.LT
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.fromSdk
import com.jetbrains.python.psi.resolve.resolveQualifiedName
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_API_VERS_6_1
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettingsListener
import java.util.*
import javax.swing.SwingUtilities

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-07
 */
class ImplicitPySymbolsProvider(
    val project: Project,
) : Disposable {

    private var projectJDKChangedListener: (() -> Unit)? = null

    @Volatile
    var cache: ImplicitPySymbolsCache = ImplicitPySymbolsCache.emptyCache()
        private set

    fun initOnStartup() {
        LOG.debug("Init: $project")
        subscribeOnEvents()
        scheduleUpdate()
    }

    fun scheduleUpdate() {
        SwingUtilities.invokeLater {
            onChange(true)
        }
    }

    private fun onChange(forceClear: Boolean) {
        val sdk = SmkSupportProjectSettings.getInstance(project).getActiveSdk()
        if (sdk == null) {
            cache = ImplicitPySymbolsCache.emptyCache()
            refreshAfterSymbolCachesUpdated(project)
            return
        }

        if (forceClear) {
            cache = ImplicitPySymbolsCache.emptyCache()
        }

        val project = project

        DumbService.getInstance(project).runWhenSmart {
            SlowOperations.allowSlowOperations<Throwable> { doRefreshCache(project, sdk, forceClear, null) }
        }
    }

    private fun doRefreshCache(
        project: Project,
        sdk: Sdk,
        forceClear: Boolean,
        progressIndicator: ProgressIndicator?
    ) {
        if (project.isDisposed) {
            return
        }

        /**
         * TODO 1:
         *      Runtime uses Workflow().self.globals from snakemake/workflow.py
         *      so let's parse 'includes' and globals instead of hardcoded includes
         **/

        /**
         * TODO 2:
         *      At the moment we recalculate cache on events, but replace only if smth change.
         *      Maybe we could recalculate cache less often
         **/
        val usedFiles = mutableSetOf<VirtualFile>()

        val elementsCache = ArrayList<ImplicitPySymbol>()
        val syntheticElementsCache = ArrayList<Pair<SmkCodeInsightScope, LookupElement>>()

        ///////////////////////////////////////
        // E.g. rules, config, ... defined in Workflow code as global variables

        if (isSmkVersLT_6_1(usedFiles, sdk)) {
            // legacy globals API in workflow, used before 6.1
            collectWorkflowGlobalVariables(usedFiles, sdk, elementsCache)
        } else {
            addWorkflowGlobalVariables(usedFiles, sdk, syntheticElementsCache)
        }
        progressIndicator?.checkCanceled()

        // xxx: maybe  properties from 'Workflow'

        ///////////////////////////////////////
        // E.g. expand, temp, .. from 'snakemake.io'
        collectTopLevelMethodsFrom(
            "snakemake.io", SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
        )
        progressIndicator?.checkCanceled()

        ///////////////////////////////////////
        // Collect hardcoded methods
        collectMethods(
            listOf(
                "snakemake.utils" to "simplify_path",
                "snakemake.wrapper" to "wrapper",
                "snakemake.script" to "script"
            ), SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
        )
        progressIndicator?.checkCanceled()

        ///////////////////////////////////////
        // Collect hardcoded classes

        // in order to get properly working inspection on shell class constructor, e.g ignore first 'self' arg
        // let's resolve to class here:
        collectClasses(
            listOf("snakemake.shell" to "shell"),
            SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache, ImplicitPySymbolUsageType.METHOD
        )
        progressIndicator?.checkCanceled()

        ///////////////////////////////////////
        // Collect variables
        collectVars(
            listOf(
                "snakemake.logging" to "logger"
            ), SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
        )
        progressIndicator?.checkCanceled()


        ////////////////////////////////////////
        // inside 'rule run': input, output, wildcards, params
        // snakemake.io.InputFiles
        // snakemake.io.OutputFiles
        // snakemake.io.Params
        // snakemake.io.Log
        // snakemake.io.Wildcards
        // snakemake.io.Resources
        collectTopLevelClassesInheretedFrom(
            "snakemake.io",
            "snakemake.io.Namedlist",
            SmkCodeInsightScope.RULELIKE_RUN_SECTION, usedFiles, sdk, elementsCache
        ) { className ->
            when (className) {
                "InputFiles" -> "input"
                "OutputFiles" -> "output"
                else -> className.lowercase(Locale.getDefault())
            }
        }
        progressIndicator?.checkCanceled()


        ///////////////////////////////////////
        // peppy module: `pep` variable
        addPeppyGlobalVariables(usedFiles, sdk, syntheticElementsCache)
        progressIndicator?.checkCanceled()

        ////////////////////////////////////////
        val contentVersion = usedFiles.sortedBy { it.path }.map { it.timeStamp }.hashCode()
        val cachedContentVersion = cache.contentVersion
        if (cachedContentVersion == 0 || contentVersion != cachedContentVersion || forceClear) {
            cache = ImplicitPySymbolsCacheImpl(project, elementsCache, syntheticElementsCache, contentVersion)

            LOG.debug("[CACHE UPDATED]")

            // rerun highlighting/caches
            refreshAfterSymbolCachesUpdated(project)
        }
    }

    @Suppress("FunctionName")
    private fun isSmkVersLT_6_1(
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk
    ): Boolean {
        val requirement1 = pyRequirement("snakemake-minimal", LT, SMK_API_VERS_6_1)
        val requirement2 = pyRequirement("snakemake", LT, SMK_API_VERS_6_1)

        if (ApplicationManager.getApplication().isUnitTestMode) {
            val workflowFile = collectPyFiles("snakemake.workflow", usedFiles, sdk).firstOrNull()?.virtualFile
            val versionFile = workflowFile?.parent?.findChild("vers.snakecharm.txt")
            if (versionFile != null) {
                val smkVersTestMode = VfsUtil.loadText(versionFile)
                return requirement1.versionSpecs[0].matches(smkVersTestMode)
            }
            return false
        }

        val packages = PyPackageManager.getInstance(sdk).packages

        val pkg1 = packages?.firstOrNull { it.name == SnakemakeAPI.SMK_API_PKG_NAME_SMK }
        if (pkg1 != null) {
            return pkg1.matches(requirement2)
        }
        val pkg2 = packages?.firstOrNull { it.name == SnakemakeAPI.SMK_API_PKG_NAME_SMK_MINIMAL }
        if (pkg2 != null) {
            return pkg2.matches(requirement1)
        }
        return false
    }

    private fun refreshAfterSymbolCachesUpdated(project: Project) {
        val action = {
            LOG.debug("RESTART highlighting")
            ResolveCache.getInstance(project).clearCache(true)
            DaemonCodeAnalyzer.getInstance(project).restart()
        }

        if (ApplicationManager.getApplication().isUnitTestMode) {
            // Do now
            action()
            return
        }

        ApplicationManager.getApplication().invokeLater(
            action,
            project.disposed
        )
    }

    private fun doRefresh(forceClear: Boolean) {
        val action = {
            onChange(forceClear)
        }

        if (ApplicationManager.getApplication().isUnitTestMode) {
            // Do now, in in BG
            action()
            return
        }

        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(object : Task.Backgroundable(
                project,
                SnakemakeBundle.message("wrappers.parsing.progress.collecting.data"),
                forceClear
            ) {
                override fun run(indicator: ProgressIndicator) {
                    action()
                }
            })
        }
    }

    private fun subscribeOnEvents() {

        val connection = project.messageBus.connect()
        connection.subscribe(SmkSupportProjectSettings.TOPIC, object : SmkSupportProjectSettingsListener {
            override fun stateChanged(
                newSettings: SmkSupportProjectSettings,
                oldState: SmkSupportProjectSettings.State,
                sdkRenamed: Boolean,
                sdkRemoved: Boolean
            ) {
                if (!newSettings.snakemakeSupportEnabled) {
                    // otherwise update later on enabled
                    return
                }

                val sdkNameNotChanged = sdkRenamed || (oldState.pythonSdkName == newSettings.sdkName)
                if (sdkNameNotChanged && !sdkRemoved) {
                    return
                }

                doRefresh(true)
            }

            override fun enabled(newSettings: SmkSupportProjectSettings) {
                doRefresh(true)
            }

            override fun disabled(newSettings: SmkSupportProjectSettings) {
                doRefresh(true)
            }
        })

        // Listen packages installed / removed
        connection.subscribe(PyPackageManager.PACKAGE_MANAGER_TOPIC, PyPackageManager.Listener { sdk ->
            val settings = SmkSupportProjectSettings.getInstance(project)
            if (settings.snakemakeSupportEnabled) {
                val activeSdk = settings.getActiveSdk()
                if (sdk.name == activeSdk?.name) {
                    LOG.debug("[PACKAGE_MANAGER_TOPIC]: sdk == [$sdk]")

                    // This events is submitted on module settings closing even if no modifications
                    doRefresh(false)
                }
            }
        })

        Disposer.register(this, connection)

        // Listen Project SDK changed
        require(projectJDKChangedListener == null)
        projectJDKChangedListener = {
            LOG.debug(
                "[PROJECT_SDK]: updated"
            )

            val settings = SmkSupportProjectSettings.getInstance(project)
            if (settings.snakemakeSupportEnabled && settings.useProjectSdk) {
                doRefresh(true)
            }
        }

        ProjectRootManagerEx.getInstanceEx(project).addProjectJdkListener(projectJDKChangedListener!!)
    }

    private fun collectVars(
        moduleAndVariable: List<Pair<String, String>>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>
    ) {
        moduleAndVariable.forEach { (pyModuleFqn, varName) ->
            collectPyFiles(pyModuleFqn, usedFiles, sdk)
                .forEach { pyFile ->
                    val attrib = pyFile.findTopLevelAttribute(varName)
                    if (attrib != null && attrib.name != null) {
                        elementsCache.add(
                            ImplicitPySymbol(
                                attrib.name!!,
                                attrib,
                                scope,
                                ImplicitPySymbolUsageType.VARIABLE
                            )
                        )
                    }
                }
        }
    }

    private fun collectClassInstanceMethods(
        moduleAndClass: List<Pair<String, String>>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>,
        checkInheritedMethods: Boolean = false
    ) {
        processClasses(moduleAndClass, usedFiles, sdk) { pyClass ->
            //val ctx = TypeEvalContext.userInitiated(
            //    pyClass.project,
            //    pyClass.originalElement.containingFile
            //)
            val ctx = null
            // val ctx = TypeEvalContext.codeInsightFallback(pyClass.project)
            pyClass.methods.forEach { method ->
                if (method != null) {
                    elementsCache.add(ImplicitPySymbol(method.name!!, method, scope, ImplicitPySymbolUsageType.METHOD))
                }
            }
        }
    }

    private fun collectClassConstructors(
        moduleAndClass: List<Pair<String, String>>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>
    ) {

        processClasses(moduleAndClass, usedFiles, sdk) { pyClass ->
            //val ctx = TypeEvalContext.userInitiated(
            //    pyClass.project,
            //    pyClass.originalElement.containingFile
            //)
            val ctx = null
            // val ctx = TypeEvalContext.codeInsightFallback(pyClass.project)
            val constructor = pyClass.findInitOrNew(false, ctx)
            if (constructor != null) {
                elementsCache.add(
                    ImplicitPySymbol(
                        pyClass.name!!,
                        constructor,
                        scope,
                        ImplicitPySymbolUsageType.METHOD
                    )
                )
            }
        }
    }

    private fun collectClasses(
        moduleAndClass: List<Pair<String, String>>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>,
        usageType: ImplicitPySymbolUsageType
    ) {
        processClasses(moduleAndClass, usedFiles, sdk) { pyClass ->
            elementsCache.add(ImplicitPySymbol(pyClass.name!!, pyClass, scope, usageType))
        }
    }

    private fun processClasses(
        moduleAndClass: List<Pair<String, String>>,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        processor: (PyClass) -> Unit
    ) {
        moduleAndClass.forEach { (pyModuleFqn, className) ->
            val pyFiles = collectPyFiles(pyModuleFqn, usedFiles, sdk)

            pyFiles
                .asSequence()
                .filter { it.isValid }
                .mapNotNull { it.findTopLevelClass(className) }
                .filter { it.name != null }
                .forEach { pyClass ->
                    processor(pyClass)
                }
        }
    }

    private fun collectTopLevelClassesInheretedFrom(
        pyModuleFqn: String,
        parentClassRequirement: String?,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>,
        className2VarNameFun: (String) -> String
    ) {
        val pyFiles = collectPyFiles(pyModuleFqn, usedFiles, sdk)

        // collect top level classes inherited from [parentClassRequirement]:
        pyFiles
            .asSequence()
            .filter { it.isValid }
            .flatMap { it.topLevelClasses.asSequence() }
            .filter { it.name != null }
            .forEach { pyClass ->
                val typeEvalContext = TypeEvalContext.userInitiated(
                    pyClass.project,
                    pyClass.originalElement.containingFile
                )

                if (parentClassRequirement == null || pyClass.inherits(typeEvalContext, parentClassRequirement)) {
                    val varName = className2VarNameFun(pyClass.name!!)
                    elementsCache.add(
                        ImplicitPySymbol(
                            varName,
                            pyClass, scope, ImplicitPySymbolUsageType.VARIABLE
                        )
                    )
                }
            }
    }

    private fun collectMethods(
        moduleAndMethod: List<Pair<String, String>>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>
    ) {
        moduleAndMethod.forEach { (pyModuleFqn, methodName) ->
            val pyFiles = collectPyFiles(pyModuleFqn, usedFiles, sdk)

            pyFiles
                .asSequence()
                .filter { it.isValid }
                .mapNotNull { it.findTopLevelFunction(methodName) }
                .filter { it.name != null }
                .forEach { pyFun ->
                    elementsCache.add(
                        ImplicitPySymbol(
                            pyFun.name!!,
                            pyFun,
                            scope,
                            ImplicitPySymbolUsageType.METHOD
                        )
                    )
                }
        }
    }

    private fun collectPyFiles(
        pyModuleFqn: String,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk
    ): List<PyFile> {
        val resolveContext = fromSdk(project, sdk)
        ////////////////


        val pyFiles = resolveQualifiedName(
            QualifiedName.fromDottedString(pyModuleFqn),
            resolveContext
        ).filterIsInstance<PyFile>()
        pyFiles.forEach { usedFiles.add(it.virtualFile) }
        return pyFiles
    }

    private fun collectTopLevelMethodsFrom(
        pyModuleFqn: String,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>
    ) {
        val pyFiles = collectPyFiles(pyModuleFqn, usedFiles, sdk)

        // collect top level methods:

        //val fqnComponents = methodsContainerFile.components.toTypedArray()
        pyFiles
            .asSequence()
            .filter { it.isValid }
            .flatMap { it.topLevelFunctions.asSequence() }
            .filter { it.name != null }
            .forEach { pyFun ->
                elementsCache.add(ImplicitPySymbol(pyFun.name!!, pyFun, scope, ImplicitPySymbolUsageType.METHOD))
            }
    }

    private fun collectWorkflowGlobalVariables(
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>
    ) {
        // snakemake.workflow -> cluster_config [snakemake/workflow.py, global cluster_config]: dict
        // snakemake.workflow -> config [snakemake/workflow.py, global config]: dict
        // snakemake.rules -> config [snakemake/workflow.py, global rules] : snakemake.workflow.Rules
        // snakemake.checkpoints -> config [snakemake/workflow.py, global rules] : snakemake.checkpoints.Checkpoints

        val workflowFile = collectPyFiles("snakemake.workflow", usedFiles, sdk).firstOrNull()

        if (workflowFile != null) {
            usedFiles.add(workflowFile.virtualFile)

            val globals = HashMap<String, PyElement>()

            val workflowClass = workflowFile.findTopLevelClass("Workflow")
            if (workflowClass != null) {
                workflowClass.acceptChildren(object : PyRecursiveElementVisitor() {
                    override fun visitPyGlobalStatement(node: PyGlobalStatement) {
                        for (expression in node.globals) {
                            val name = expression.referencedName!!
                            globals[name] = expression
                        }
                        super.visitPyGlobalStatement(node)
                    }
                })
                globals.forEach { (name, psi) ->
                    elementsCache.add(
                        ImplicitPySymbol(
                            name,
                            psi,
                            SmkCodeInsightScope.TOP_LEVEL,
                            ImplicitPySymbolUsageType.VARIABLE
                        )
                    )
                }
            }
        }
    }

    private fun addWorkflowGlobalVariables(
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<Pair<SmkCodeInsightScope, LookupElement>>
    ) {
        // ====== snakemake module ========
        // See snakemake/workflow.py
        //
        // Workflow.__init__()
        //     _globals = globals()
        //     _globals["workflow"] = self
        //     _globals["cluster_config"] = copy.deepcopy(self.overwrite_clusterconfig)
        //     _globals["rules"] = Rules()
        //     _globals["checkpoints"] = Checkpoints()
        //     _globals["scatter"] = Scatter()
        //     _globals["gather"] = Gather()
        //     ...
        //     self.globals["config"]
        val globals = HashMap<String, PyElement?>()

        val workflowFile = collectPyFiles("snakemake.workflow", usedFiles, sdk).firstOrNull()
        if (workflowFile != null) {
            usedFiles.add(workflowFile.virtualFile)
        }
        globals["workflow"] = workflowFile?.findTopLevelClass("Workflow")

        // Snakemake >= 6.5
        var commonFile = collectPyFiles("snakemake.common.__init__", usedFiles, sdk).firstOrNull()
        if (commonFile == null) {
            // Snakemake 6.1 .. 6.4.x
            commonFile = collectPyFiles("snakemake.common", usedFiles, sdk).firstOrNull()
        }
        if (commonFile != null) {
            usedFiles.add(commonFile.virtualFile)
        }

        globals[SnakemakeAPI.SMK_VARS_CHECKPOINTS] = commonFile?.findTopLevelClass("Checkpoints")
        globals[SnakemakeAPI.SMK_VARS_RULES] = commonFile?.findTopLevelClass("Rules")
        globals[SnakemakeAPI.SMK_VARS_SCATTER] = commonFile?.findTopLevelClass("Scatter")
        globals[SnakemakeAPI.SMK_VARS_GATHER] = commonFile?.findTopLevelClass("Gather")
        globals[SnakemakeAPI.SMK_VARS_CONFIG] = null

        val checkpointsFile = collectPyFiles("snakemake.checkpoints", usedFiles, sdk).firstOrNull()
        if (checkpointsFile != null) {
            usedFiles.add(checkpointsFile.virtualFile)
        }
        // TODO: do we need this ?
        globals["checkpoints"] = checkpointsFile?.findTopLevelClass("Checkpoints")

        globals.forEach { (name, psi) ->
            elementsCache.add(
                SmkCodeInsightScope.TOP_LEVEL to SmkCompletionUtil.createPrioritizedLookupElement(
                    name,
                    psi,
                    typeText = SnakemakeBundle.message("TYPES.rule.run.workflow.globals.type.text"),
                    priority = SmkCompletionUtil.WORKFLOW_GLOBALS_PRIORITY
                )
            )
        }
    }

    private fun addPeppyGlobalVariables(
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<Pair<SmkCodeInsightScope, LookupElement>>
    ) {
        // ====== peppy module ========
        val pepFile = collectPyFiles("peppy.project", usedFiles, sdk).firstOrNull()
        val pepObjectConstructor = pepFile?.findTopLevelClass("Project")?.findInitOrNew(false, null)

        elementsCache.add(
            SmkCodeInsightScope.TOP_LEVEL to SmkCompletionUtil.createPrioritizedLookupElement(
                SnakemakeAPI.SMK_VARS_PEP,
                pepObjectConstructor,
                typeText = SnakemakeBundle.message("TYPES.rule.run.workflow.globals.type.text"),
                priority = SmkCompletionUtil.WORKFLOW_GLOBALS_PRIORITY
            )
        )
    }

    companion object {
        private val LOG = logger<ImplicitPySymbolsProvider>() // TODO: cleanup
        fun instance(project: Project) = project.service<ImplicitPySymbolsProvider>()
    }

    override fun dispose() {
        projectJDKChangedListener?.let {
            ProjectRootManagerEx.getInstanceEx(project).removeProjectJdkListener(it)
        }
    }
}

/**
 * @param project Project
 * @param symbols Real elements in code which are inserted on runtime in snakemake execution context
 * @param syntheticSymbols Elements that doesn't have PSI definition in snakemake files but also are dynamically
 *  inserted in execution context at runtime, e.g. global variables of workflow (rules, config, ..). The could be
 *  resolved to some PSI elements (e.g. related classes, etc) which cannot be used as definitions in code insight engine.
 */
private class ImplicitPySymbolsCacheImpl(
    private val project: Project,
    symbols: List<ImplicitPySymbol>,
    syntheticSymbols: List<Pair<SmkCodeInsightScope, LookupElement>>,
    override val contentVersion: Int = 0
) : ImplicitPySymbolsCache {

    private val scope2Symbols = symbols.groupBy { it.scope }
    private val scope2SyntheticSymbols = syntheticSymbols.groupBy({ it.first }, { it.second })

    override operator fun get(scope: SmkCodeInsightScope) = validElements(scope2Symbols[scope] ?: emptyList())
    override fun getSynthetic(scope: SmkCodeInsightScope) = scope2SyntheticSymbols[scope] ?: emptyList()

    private fun validElements(elements: List<ImplicitPySymbol>): List<ImplicitPySymbol> {
        val validElements = elements.filter { it.psiDeclaration.isValid }
        if (validElements.size != elements.size) {
            project.service<ImplicitPySymbolsProvider>().scheduleUpdate()
        }
        return validElements
    }
}
