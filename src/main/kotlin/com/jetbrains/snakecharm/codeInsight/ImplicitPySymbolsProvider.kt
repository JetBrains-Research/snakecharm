package com.jetbrains.snakecharm.codeInsight

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
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
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.QualifiedName
import com.jetbrains.extensions.python.inherits
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyGlobalStatement
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.resolve.fromSdk
import com.jetbrains.python.psi.resolve.resolveQualifiedName
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettingsListener
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

        onChange(true)
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
            runReadAction {
                if (project.isDisposed) {
                    return@runReadAction
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

                val usedFiles = arrayListOf<PsiFile>()

                //val elementsCache = ArrayList<Pair<QualifiedName, PyElement>>()
                val elementsCache = ArrayList<ImplicitPySymbol>()

                ///////////////////////////////////////
                // E.g. rules, config, ... defined in Workflow code as global variables
                collectWorkflowGlobalVariables(usedFiles, sdk, elementsCache)

                ///////////////////////////////////////
                // E.g. expand, temp, .. from 'snakemake.io'
                collectTopLevelMethodsFrom(
                    "snakemake.io", SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
                )

                ///////////////////////////////////////
                // Collect hardcoded methods
                collectMethods(
                    listOf(
                        "snakemake.utils" to "simplify_path",
                        "snakemake.wrapper" to "wrapper",
                        "snakemake.script" to "script"
                    ), SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
                )

                ///////////////////////////////////////
                // Collect hardcoded classes
                collectClasses(
                    listOf(
                        "snakemake.shell" to "shell"
                    ), SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache, ImplicitPySymbolUsageType.METHOD
                )

                ///////////////////////////////////////
                // Collect variables
                collectVars(
                    listOf(
                        "snakemake.logging" to "logger"
                    ), SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
                )


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
                        else -> className.toLowerCase()
                    }
                }

                ////////////////////////////////////////
                val contentVersion = usedFiles.map { it.containingFile.virtualFile.timeStamp }.hashCode()
                val cachedContentVersion = cache.contentVersion
                if (cachedContentVersion == 0 || contentVersion != cachedContentVersion || forceClear) {
                    cache = ImplicitPySymbolsCacheImpl(project, elementsCache, contentVersion)

                    LOG.debug("[CACHE UPDATED]")

                    // rerun highlighting/caches
                    refreshAfterSymbolCachesUpdated(project)
                }
            }
        }
    }

    private fun refreshAfterSymbolCachesUpdated(project: Project) {
        val action = {
            LOG.debug("RESTART highlighting")
            ResolveCache.getInstance(project).clearCache(true);
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
        usedFiles: MutableList<PsiFile>,
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

    private fun collectClassConstuctors(
        moduleAndClass: List<Pair<String, String>>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableList<PsiFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>,
        usageType: ImplicitPySymbolUsageType
    ) {
        moduleAndClass.forEach { (pyModuleFqn, className) ->
            val pyFiles = collectPyFiles(pyModuleFqn, usedFiles, sdk)

            pyFiles
                .asSequence()
                .filter { it.isValid }
                .mapNotNull { it.findTopLevelClass(className) }
                .filter { it.name != null }
                .forEach { pyClass ->
                    val constructor = pyClass.findInitOrNew(
                        false,
                        TypeEvalContext.userInitiated(
                            pyClass.project,
                            pyClass.originalElement.containingFile
                        )
                    )
                    if (constructor != null) {
                        elementsCache.add(ImplicitPySymbol(pyClass.name!!, constructor, scope, usageType))
                    }
//                      //XXX: todo do we need 'else' here like:  elementsCache.add(pyClass.name!! to pyClass) ?
                }
        }
    }

    private fun collectClasses(
        moduleAndClass: List<Pair<String, String>>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableList<PsiFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>,
        usageType: ImplicitPySymbolUsageType
    ) {
        moduleAndClass.forEach { (pyModuleFqn, className) ->
            val pyFiles = collectPyFiles(pyModuleFqn, usedFiles, sdk)

            pyFiles
                .asSequence()
                .filter { it.isValid }
                .mapNotNull { it.findTopLevelClass(className) }
                .filter { it.name != null }
                .forEach { pyClass ->
                    elementsCache.add(ImplicitPySymbol(pyClass.name!!, pyClass, scope, usageType))
                }
        }
    }

    private fun collectTopLevelClassesInheretedFrom(
        pyModuleFqn: String,
        parentClassRequirement: String?,
        scope: SmkCodeInsightScope,
        usedFiles: MutableList<PsiFile>,
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
        usedFiles: MutableList<PsiFile>,
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
        usedFiles: MutableList<PsiFile>,
        sdk: Sdk
    ): List<PyFile> {
        val resolveContext = fromSdk(project, sdk)
        ////////////////

        val pyFiles = resolveQualifiedName(
            QualifiedName.fromDottedString(pyModuleFqn),
            resolveContext
        ).filterIsInstance<PyFile>()

        usedFiles.addAll(pyFiles)
        return pyFiles
    }

    private fun collectTopLevelMethodsFrom(
        pyModuleFqn: String,
        scope: SmkCodeInsightScope,
        usedFiles: MutableList<PsiFile>,
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
        usedFiles: MutableList<PsiFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>
    ) {
        // snakemake.workflow -> cluster_config [snakemake/workflow.py, global cluster_config]: dict
        // snakemake.workflow -> config [snakemake/workflow.py, global config]: dict
        // snakemake.rules -> config [snakemake/workflow.py, global rules] : snakemake.workflow.Rules
        // snakemake.checkpoints -> config [snakemake/workflow.py, global rules] : snakemake.checkpoints.Checkpoints

        val workflowFile = collectPyFiles("snakemake.workflow", usedFiles, sdk).firstOrNull()

        if (workflowFile != null) {
            usedFiles.add(workflowFile)

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

private class ImplicitPySymbolsCacheImpl(
    private val project: Project,
    symbols: List<ImplicitPySymbol>,
    override val contentVersion: Int = 0
) : ImplicitPySymbolsCache {

    private val scope2Symbols = toMap(symbols)

    override operator fun get(scope: SmkCodeInsightScope) = validElements(scope2Symbols.getValue(scope))

    private fun validElements(elements: List<ImplicitPySymbol>): List<ImplicitPySymbol> {
        val validElements = elements.filter { it.psiDeclaration.isValid }
        if (validElements.size != elements.size) {
            project.service<ImplicitPySymbolsProvider>().scheduleUpdate()
        }
        return validElements
    }

    companion object {
        private fun toMap(symbols: List<ImplicitPySymbol>): Map<SmkCodeInsightScope, List<ImplicitPySymbol>> {
            val map = SmkCodeInsightScope.values().associate { it to arrayListOf<ImplicitPySymbol>() }
            symbols.forEach { s ->
                map.getValue(s.scope).add(s)
            }
            return map
        }
    }
}
