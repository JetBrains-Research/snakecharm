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
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.QualifiedName
import com.intellij.util.PlatformIcons
import com.intellij.util.SlowOperations
import com.jetbrains.python.extensions.inherits
import com.jetbrains.python.packaging.PyPackage
import com.jetbrains.python.packaging.common.PythonPackageManagementListener
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.pyRequirement
import com.jetbrains.python.packaging.requirement.PyRequirementRelation.LT
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.fromSdk
import com.jetbrains.python.psi.resolve.resolveQualifiedName
import com.jetbrains.python.psi.resolve.resolveTopLevelMember
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.GLOBAL_VARS_TO_CLASS_FQN
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.SECTION_ACCESSOR_CLASSES
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.SMK_API_VERS_6_1
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettingsListener
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_MODULE_NAME_IO
import java.util.*
import javax.swing.SwingUtilities

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-07
 */
@Suppress("UnstableApiUsage")
class SmkImplicitPySymbolsProvider(
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
            SlowOperations.knownIssue("https://github.com/JetBrains-Research/snakecharm/issues/533").use { ignore ->
                // XXX: workaround because default code throws 100+ exceptions:  java.lang.Throwable: Slow operations are prohibited on EDT. See SlowOperations.assertSlowOperationsAreAllowed javadoc.
                // SlowOperations.allowSlowOperations<Throwable> { .. }

                // TODO: 1) should be in background 2) note that it requires some read actions to access PSI
                // See: https://github.com/JetBrains-Research/snakecharm/issues/513
                doRefreshCache(project, sdk, forceClear, null)
            }
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
        // Implicit requires: e.g. 'os', 'sys'
        val resolveContext = fromSdk(project, sdk)
        listOf("os", "sys", "snakemake").forEach { moduleName ->
            var module = resolveQualifiedName(QualifiedName.fromDottedString(moduleName), resolveContext).filterIsInstance<PyFile>().firstOrNull()
            if (module == null) {
                module =
                    resolveQualifiedName(QualifiedName.fromDottedString("${moduleName}.__init__"), resolveContext).filterIsInstance<PyFile>()
                        .firstOrNull()
            }
            if (module != null) {
                usedFiles.add(module.virtualFile)

                syntheticElementsCache.add(
                    SmkCodeInsightScope.TOP_LEVEL to SmkCompletionUtil.createPrioritizedLookupElement(
                        moduleName,
                        module,
                        icon = PlatformIcons.VARIABLE_ICON,
                        typeText = moduleName,
                        priority = SmkCompletionUtil.WORKFLOW_GLOBALS_PRIORITY
                    )
                )
            }
        }
        // Add 'Path' from pathlib
        listOf("pathlib.Path").forEach() { fqn ->
            val qualifiedName = QualifiedName.fromDottedString(fqn)
            val pyClass = resolveTopLevelMember(qualifiedName, resolveContext) as? PyClass
            if (pyClass != null) {
                usedFiles.add(pyClass.containingFile.virtualFile)

                syntheticElementsCache.add(
                    SmkCodeInsightScope.TOP_LEVEL to SmkCompletionUtil.createPrioritizedLookupElement(
                        qualifiedName.lastComponent.toString(),
                        pyClass,
                        icon = PlatformIcons.CLASS_ICON,
                        typeText = fqn,
                        priority = SmkCompletionUtil.WORKFLOW_GLOBALS_PRIORITY
                    )
                )
            }
        }

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
        // E.g. expand, temp, .. from 'snakemake.io' (snakemake.io.py before 9.0.0 and snakemake.io.__init__.py after)
        collectTopLevelMethodsFrom(
            SNAKEMAKE_MODULE_NAME_IO, SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
        )
        progressIndicator?.checkCanceled()
        // E.g. flags 'update',.. from 'snakemake.ioflags'
        collectTopLevelMethodsFrom(
            "snakemake.ioflags", SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
        )
        progressIndicator?.checkCanceled()

        // see 'ioutils.register_in_globals` function
        // collect exists, etc
        // * XXX: before 9.0.0 syntax
        collectTopLevelMethodsFrom(
            "snakemake.ioutils", SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
        )
        // * XXX: starting from 9.0.0 syntax
        collectTopLevelMethodsMatchingModuleFromPackage(
            "snakemake.ioutils", SmkCodeInsightScope.TOP_LEVEL, usedFiles, sdk, elementsCache
        )

        elementsCache.firstOrNull() { it.fqn == "snakemake.io.expand"}?.let {
            elementsCache.add(ImplicitPySymbol(
                SnakemakeNames.SMK_FUN_EXPAND_ALIAS_COLLECT,"snakemake.ioutils.collect", it.psiDeclaration, it.scope, it.usageType
                ))
        }
        // TODO add 'collect' as exapnd - snakemake.ioutils.collect
        // elementsCache.add(ImplicitPySymbol(pyFun.name!!, pyFun, scope, ImplicitPySymbolUsageType.METHOD))


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
            listOf("snakemake.shell.shell"),
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
        collectTopLevelClassesInheritedFrom(
            SNAKEMAKE_MODULE_NAME_IO,
            "snakemake.io.Namedlist",
            SmkCodeInsightScope.RULELIKE_RUN_SECTION, usedFiles, sdk, elementsCache
        ) { classFqn ->
            val sectionName = SECTION_ACCESSOR_CLASSES[classFqn]
            sectionName ?: classFqn.split('.').last().lowercase(Locale.getDefault())
        }
        progressIndicator?.checkCanceled()


        ///////////////////////////////////////
        // peppy module: `pep` variable
        addPeppyGlobalVariables(usedFiles, sdk, syntheticElementsCache)
        progressIndicator?.checkCanceled()

        ////////////////////////////////////////
        // TODO: also all rules functions are accessible using `__{rule_name}`
        //  workflow.py
        //      ruleinfo.func.__name__ = f"__{rule.name}"
        //      self.globals[ruleinfo.func.__name__] = ruleinfo.func

        ////////////////////////////////////////
        // TODO: all modules names accessible by name:
        // modules.py
        //      self.workflow.globals[self.namespace] = namespace

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
        val requirementSnakemakeMinimal = pyRequirement("snakemake-minimal", LT, SMK_API_VERS_6_1)
        val requirementSnakemake = pyRequirement("snakemake", LT, SMK_API_VERS_6_1)

        if (ApplicationManager.getApplication().isUnitTestMode) {
            val workflowFile = collectPyFiles("snakemake.workflow", usedFiles, sdk).firstOrNull()?.virtualFile
            val versionFile = workflowFile?.parent?.findChild("vers.snakecharm.txt")
            if (versionFile != null) {
                val smkVersTestMode = VfsUtil.loadText(versionFile)
                return requirementSnakemakeMinimal.versionSpecs[0].matches(smkVersTestMode)
            }
            return false
        }

        val packages = PythonPackageManager.forSdk(project, sdk).installedPackages

        val pkgSnakemake = packages.firstOrNull { it.name == SnakemakeApi.SMK_API_PKG_NAME_SMK }
        if (pkgSnakemake != null) {
            return PyPackage(pkgSnakemake.name, pkgSnakemake.version).matches(requirementSnakemake)
        }
        val pkgSnakemakeMinimal = packages.firstOrNull { it.name == SnakemakeApi.SMK_API_PKG_NAME_SMK_MINIMAL }
        if (pkgSnakemakeMinimal != null) {
            return PyPackage(pkgSnakemakeMinimal.name, pkgSnakemakeMinimal.version).matches(requirementSnakemakeMinimal)
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
        connection.subscribe(PythonPackageManager.PACKAGE_MANAGEMENT_TOPIC, object : PythonPackageManagementListener {
            override fun packagesChanged(sdk: Sdk) {
                val settings = SmkSupportProjectSettings.getInstance(project)
                if (settings.snakemakeSupportEnabled) {
                    val activeSdk = settings.getActiveSdk()
                    if (sdk.name == activeSdk?.name) {
                        LOG.debug("[PACKAGE_MANAGER_TOPIC]: sdk == [$sdk]")

                        // This events is submitted on module settings closing even if no modifications
                        doRefresh(false)
                    }
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
                                attrib.qualifiedName,
                                attrib,
                                scope,
                                ImplicitPySymbolUsageType.VARIABLE
                            )
                        )
                    }
                }
        }
    }

    @Suppress("unused")
    private fun collectClassInstanceMethods(
        classFQN: List<String>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>,
        checkInheritedMethods: Boolean = false
    ) {
        processClasses(classFQN, usedFiles, sdk) { pyClass ->
            //val ctx = TypeEvalContext.userInitiated(
            //    pyClass.project,
            //    pyClass.originalElement.containingFile
            //)
            //val ctx = null
            // val ctx = TypeEvalContext.codeInsightFallback(pyClass.project)
            pyClass.methods.forEach { method ->
                if (method != null) {
                    elementsCache.add(ImplicitPySymbol(method.name!!, method.qualifiedName, method, scope, ImplicitPySymbolUsageType.METHOD))
                }
            }
        }
    }

    @Suppress("unused")
    private fun collectClassConstructors(
        classFQN: List<String>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>
    ) {

        processClasses(classFQN, usedFiles, sdk) { pyClass ->
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
                        constructor.qualifiedName,
                        constructor,
                        scope,
                        ImplicitPySymbolUsageType.METHOD
                    )
                )
            }
        }
    }

    private fun collectClasses(
        classFQN: List<String>,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>,
        usageType: ImplicitPySymbolUsageType
    ) {
        processClasses(classFQN, usedFiles, sdk) { pyClass ->
            elementsCache.add(ImplicitPySymbol(pyClass.name!!, pyClass.qualifiedName, pyClass, scope, usageType))
        }
    }

    private fun processClasses(
        classFQN: List<String>,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        processor: (PyClass) -> Unit
    ) {
        val resolveContext = fromSdk(project, sdk)
        classFQN.forEach { fqn ->
            val pyElement = resolveTopLevelMember(QualifiedName.fromDottedString(fqn), resolveContext)
            if (pyElement is PyClass) {
                usedFiles.add(pyElement.containingFile.virtualFile)
                processor(pyElement)
            }
        }
    }

    /**
     * Find module related file using 2 approaches:
     *      - Snakemake API, v < 9.0.0: e.g 'snakemake.io' -> 'snakemake/io.py'
     *      - Snakemake API, v >= 9.0.0: e.g 'snakemake.io' -> 'snakemake/io/__init__.py'
     *
     * @param project
     *          the project
     * @param scope
     *          the code insight scope
     * @param usedFiles
     *          the set of already processed files (to avoid processing the same file twice)
     * @param sdk
     */
    private fun collectTopLevelClassesInheritedFrom(
        pyModuleFqn: String,
        parentClassRequirement: String?,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>,
        classFqn2VarNameFun: (String) -> String
    ) {
        val pyFiles = collectFilesFromModulePyFileOrInitFile(pyModuleFqn, usedFiles, sdk)

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
                    val fqn = pyClass.qualifiedName ?: pyClass.name!!
                    val varName = classFqn2VarNameFun(fqn)
                    elementsCache.add(
                        ImplicitPySymbol(
                            varName, fqn,
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
                            pyFun.qualifiedName,
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

        val pyFiles = resolveQualifiedName(
            QualifiedName.fromDottedString(pyModuleFqn),
            resolveContext
        ).filterIsInstance<PyFile>()
        pyFiles.forEach { usedFiles.add(it.virtualFile) }
        return pyFiles
    }

    private fun collectPyFilesFromPsiDirectory(
        pyModuleFqn: String,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk
    ): List<PyFile> {
        val resolveContext = fromSdk(project, sdk)

        val pyFiles = resolveQualifiedName(
            QualifiedName.fromDottedString(pyModuleFqn),
            resolveContext
        ).filterIsInstance<PsiDirectory>().flatMap {
            it.files.filterIsInstance<PyFile>()
        }
        pyFiles.forEach { usedFiles.add(it.virtualFile) }
        return pyFiles
    }

    private fun collectTopLevelMethodsMatchingModuleFromPackage(
        pyModuleFqn: String,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>
    ) {
        val pyFiles = collectPyFilesFromPsiDirectory(pyModuleFqn, usedFiles, sdk)

        // collect top level methods matching file names
        pyFiles
            .asSequence()
            .filter { it.isValid }
            .mapNotNull { pyFile -> pyFile.findTopLevelFunction(pyFile.virtualFile.nameWithoutExtension) }
            .filter { it.name != null }
            .forEach { pyFun ->
                elementsCache.add(ImplicitPySymbol(pyFun.name!!, pyFun.qualifiedName, pyFun, scope, ImplicitPySymbolUsageType.METHOD))
            }
    }

    private fun collectTopLevelMethodsFrom(
        pyModuleFqn: String,
        scope: SmkCodeInsightScope,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk,
        elementsCache: MutableList<ImplicitPySymbol>
    ) {
        val pyFiles = collectFilesFromModulePyFileOrInitFile(pyModuleFqn, usedFiles, sdk)

        // collect top level methods:

        //val fqnComponents = methodsContainerFile.components.toTypedArray()
        pyFiles
            .asSequence()
            .filter { it.isValid }
            .flatMap { it.topLevelFunctions.asSequence() }
            .filter { it.name != null }
            .forEach { pyFun ->
                elementsCache.add(ImplicitPySymbol(pyFun.name!!, pyFun.qualifiedName, pyFun, scope, ImplicitPySymbolUsageType.METHOD))
            }
    }

    private fun collectFilesFromModulePyFileOrInitFile(
        pyModuleFqn: String,
        usedFiles: MutableSet<VirtualFile>,
        sdk: Sdk
    ): List<PyFile> {
        val pyFilesByModulePyFile = collectPyFiles(pyModuleFqn, usedFiles, sdk)

        return when {
            pyModuleFqn.endsWith("__init__") -> pyFilesByModulePyFile
            else -> pyFilesByModulePyFile + collectPyFiles("$pyModuleFqn.__init__", usedFiles, sdk)
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
        // TODO: take from api.getModuleArgsSectionKeywords() + help add into list + deprecation info

        // ====== snakemake module ========
        // See snakemake/workflow.py
        //
        // Workflow.__init__()
        //     _globals = globals()
        //
        //     _globals["shell"] = shell
        //     _globals["workflow"] = self
        //     _globals["checkpoints"] = Checkpoints()
        //     _globals["scatter"] = Scatter()
        //     _globals["gather"] = Gather()
        //     _globals["github"] = sourcecache.GithubFile
        //      _globals["gitlab"] = sourcecache.GitlabFile
        //      _globals["gitfile"] = sourcecache.LocalGitFile
        //     _globals["storage"] = self._storage_registry
        //     snakemake.ioutils.register_in_globals(_globals)
        //     snakemake.ioflags.register_in_globals(_globals)
        //     _globals["from_queue"] = from_queue

        //     _globals["cluster_config"] = copy.deepcopy(self.overwrite_clusterconfig)
        //     ...
        //     self.globals["config"] = copy.deepcopy(self.config_settings.overwrite_config)
        //
        // WorkflowModifier.__init__()
        //      self.rule_proxies = rule_proxies or Rules()
        //      self.globals["rules"] = self.rule_proxies

        val globals = HashMap<String, PsiElement?>()
        val resolveContext = fromSdk(project, sdk)
        GLOBAL_VARS_TO_CLASS_FQN.forEach { item, classFqn ->
            // * resolve module (file):
            // resolveQualifiedName(QualifiedName.fromDottedString("snakemake.common"), fromSdk(project, sdk))
            // * resolve class:
            // resolveTopLevelMember(QualifiedName.fromDottedString("snakemake.common.Rules"), fromSdk(project, sdk))
            // (resolveQualifiedName(QualifiedName.fromDottedString("snakemake.common"), resolveContext)[0] as PyFile).findTopLevelClass("Rules")
            val psiElement = classFqn?.let { resolveTopLevelMember(QualifiedName.fromDottedString(it), resolveContext) }
            psiElement?.containingFile?.virtualFile?.let() { usedFiles.add(it)}
            globals[item] = psiElement
        }

        // Storage - is an instance of StorageRegistry
        val workflowClass = resolveTopLevelMember(
            QualifiedName.fromDottedString("snakemake.workflow.Workflow"),
            resolveContext
        ) as? PyClass
        if (workflowClass != null) {
            val attr = workflowClass.findInstanceAttribute("_storage_registry", false)
            if (attr != null) {
                globals[SnakemakeNames.SMK_VARS_STORAGE] = attr
            }
        }
        if (SnakemakeNames.SMK_VARS_STORAGE !in globals) {
            // Fail safe:
            globals[SnakemakeNames.SMK_VARS_STORAGE] = resolveTopLevelMember(
                QualifiedName.fromDottedString("snakemake.storage.StorageRegistry"),
                resolveContext
            )
        }

        // log in onstart/onsuccess/onerror
        // TODO: 'log' var only in these sections, not on top-level, need new contxt for that?

        // Config
        val configSettingsClass = resolveTopLevelMember(
            QualifiedName.fromDottedString("snakemake.settings.types.ConfigSettings"),
            resolveContext
        ) as? PyClass
        if (configSettingsClass != null) {
            // ~8.0-8.1 feature
            val attr = configSettingsClass.findInstanceAttribute("overwrite_config", false)
            if (attr != null) {
                globals[SnakemakeNames.SMK_VARS_CONFIG] = attr
            }
        } else {
            globals[SnakemakeNames.SMK_VARS_CONFIG] = null
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////
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
                SnakemakeNames.SMK_VARS_PEP,
                pepObjectConstructor,
                typeText = SnakemakeBundle.message("TYPES.rule.run.workflow.globals.type.text"),
                priority = SmkCompletionUtil.WORKFLOW_GLOBALS_PRIORITY
            )
        )
    }

    companion object {
        private val LOG = logger<SmkImplicitPySymbolsProvider>() // TODO: cleanup
        fun instance(project: Project) = project.service<SmkImplicitPySymbolsProvider>()
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
            project.service<SmkImplicitPySymbolsProvider>().scheduleUpdate()
        }
        return validElements
    }
}
