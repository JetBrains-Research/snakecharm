package com.jetbrains.snakecharm.codeInsight

import com.intellij.ProjectTopics
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleComponent
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.psi.PsiFile
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyGlobalStatement
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.resolve.fromModule
import com.jetbrains.python.psi.resolve.resolveQualifiedName
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.sdk.PythonSdkType
import javax.swing.SwingUtilities

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-07
 */
class ImplicitPySymbolsProvider(private val module: Module) : ModuleComponent {
    @Volatile
    var cache: ImplicitPySymbolsCache = ImplicitPySymbolsCache.emptyCache()
        private set

    companion object {
        private val LOG = logger<ImplicitPySymbolsProvider>() // TODO: cleanup
        fun instance(module: Module) = module.getComponent(ImplicitPySymbolsProvider::class.java)!!
    }


    override fun initComponent() {
        LOG.debug("Init module: $module")

        subscribeOnChanges()
        onChange(true)
    }

    private fun subscribeOnChanges() {
        val connection = module.messageBus.connect()

        // Listen SDK changed
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                LOG.debug("[PROJECT_ROOTS]: ${event.source}, mod=${module.name}, " +
                        "p=${module.project.name}," +
                        " sdk=${PythonSdkType.findPythonSdk(module)}")

                onChange(true)
            }
        })

        // Listen packages installed / removed
        connection.subscribe(PyPackageManager.PACKAGE_MANAGER_TOPIC, PyPackageManager.Listener { sdk ->
            val moduleSdk = PythonSdkType.findPythonSdk(module)
            if (sdk === moduleSdk) {
                LOG.debug("[PACKAGE_MANAGER_TOPIC]: sdk == [$sdk], module == [$module]")

                // This events is submitted on module settings closing even if no modifications
                onChange(false)
            }
        })
    }

    private fun onChange(forceClear: Boolean) {
        if (forceClear) {
            cache = ImplicitPySymbolsCache.emptyCache()
        }

        val project = module.project
        DumbService.getInstance(project).runWhenSmart {
            runReadAction {
                if (module.isDisposed) {
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
                collectWorkflowGlobalVariables(usedFiles, elementsCache)

                ///////////////////////////////////////
                // E.g. expand, temp, .. from 'snakemake.io'
                collectTopLevelMethodsFrom(
                        "snakemake.io", SmkCodeInsightScope.TOP_LEVEL, usedFiles, elementsCache
                )

                ///////////////////////////////////////
                // Collect hardcoded methods
                collectMethods(listOf(
                        "snakemake.utils" to "simplify_path",
                        "snakemake.wrapper" to "wrapper",
                        "snakemake.script" to "script"
                ), SmkCodeInsightScope.TOP_LEVEL, usedFiles, elementsCache)

                ///////////////////////////////////////
                // Collect hardcoded classes
                collectClasses(listOf(
                        "snakemake.shell" to "shell"
                ), SmkCodeInsightScope.TOP_LEVEL, usedFiles, elementsCache)

                ///////////////////////////////////////
                // Collect variables
                collectVars(listOf(
                        "snakemake.logging" to "logger"
                ), SmkCodeInsightScope.TOP_LEVEL, usedFiles, elementsCache)


                ////////////////////////////////////////
                /*
                //TODO: inside 'rule': input, output, wildcards, params
                collectClassInstances(listOf(
                        ("snakemake.io" to "InputFiles") to "input",
                        ("snakemake.io" to "OutputFiles") to "output",
                        ("snakemake.io" to "Params") to "params",
                        ("snakemake.io" to "Wildcards") to "wildcards"
                ), usedFiles, elementsCache, SMKContext.IN_RULE)
                */

                ////////////////////////////////////////
                val contentVersion = usedFiles.map { it.containingFile.virtualFile.timeStamp }.hashCode()
                val cachedContentVersion = cache.contentVersion
                if (cachedContentVersion == 0 || contentVersion != cachedContentVersion || forceClear) {
                    cache = ImplicitPySymbolsCacheImpl(module, elementsCache, contentVersion)

                    LOG.debug("[CACHE UPDATED]")

                    // rerun highlighting
                    ApplicationManager.getApplication().invokeLater(
                            Runnable {
                                LOG.debug("RESTART highlighting")
                                DaemonCodeAnalyzer.getInstance(project).restart()
                            },
                            project.disposed
                    )
                }
            }
        }
    }

    private fun collectVars(
            moduleAndVariable: List<Pair<String, String>>,
            scope: SmkCodeInsightScope,
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<ImplicitPySymbol>
    ) {
        moduleAndVariable.forEach { (pyModuleFqn, varName) ->
            collectPyFiles(pyModuleFqn, usedFiles)
                    .forEach { pyFile ->
                        val attrib = pyFile.findTopLevelAttribute(varName)
                        if (attrib != null && attrib.name != null) {
                            elementsCache.add(ImplicitPySymbol(attrib.name!!, attrib, scope))
                        }
                    }
        }
    }

    private fun collectClasses(
            moduleAndClass: List<Pair<String, String>>,
            scope: SmkCodeInsightScope,
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<ImplicitPySymbol>
    ) {
        moduleAndClass.forEach { (pyModuleFqn, className) ->
            val pyFiles = collectPyFiles(pyModuleFqn, usedFiles)

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
                                ))
                        if (constructor != null) {
                            elementsCache.add(ImplicitPySymbol(pyClass.name!!, constructor, scope))
                        }
//                      //XXX: todo do we need 'else' here like:  elementsCache.add(pyClass.name!! to pyClass) ?
                    }
        }
    }

    /*
    private fun collectClassInstances(
            moduleClassAndInstanceName: List<Pair<Pair<String, String>, String>>,
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<Pair<String, PyElement>>,
            context: SMKContext
    ) {
        moduleClassAndInstanceName.forEach { (moduleFqnAndClass, varName) ->
            val (pyModuleFqn, className) = moduleFqnAndClass
            val pyFiles = collectPyFiles(pyModuleFqn, usedFiles)

            pyFiles
                    .filter { it.isValid }
                    .mapNotNull { it.findTopLevelClass(className) }
                    .forEach { pyClass ->
                        val constructor = pyClass.findInitOrNew(
                                false,
                                TypeEvalContext.userInitiated(
                                        pyClass.project,
                                        pyClass.originalElement.containingFile
                                ))

                        elementsCache.add(Trinity(
                                pyClass.name!!,
                                (constructor ?: pyClass) as PyElement,
                                context
                        ))

                        // TODO: varName
                    }
        }
    }
     */

    private fun collectMethods(
            moduleAndMethod: List<Pair<String, String>>,
            scope: SmkCodeInsightScope,
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<ImplicitPySymbol>
    ) {
        moduleAndMethod.forEach { (pyModuleFqn, methodName) ->
            val pyFiles = collectPyFiles(pyModuleFqn, usedFiles)

            pyFiles
                    .asSequence()
                    .filter { it.isValid }
                    .mapNotNull { it.findTopLevelFunction(methodName) }
                    .filter { it.name != null }
                    .forEach { pyFun ->
                        elementsCache.add(ImplicitPySymbol(pyFun.name!!, pyFun, scope))
                    }
        }
    }

    private fun collectPyFiles(
            pyModuleFqn: String,
            usedFiles: MutableList<PsiFile>
    ): List<PyFile> {
        val pyFiles = resolveQualifiedName(
                QualifiedName.fromDottedString(pyModuleFqn),
                fromModule(module)
        ).filterIsInstance<PyFile>()

        usedFiles.addAll(pyFiles)
        return pyFiles
    }

    private fun collectTopLevelMethodsFrom(
            pyModuleFqn: String,
            scope: SmkCodeInsightScope,
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<ImplicitPySymbol>
    ) {
        val pyFiles = collectPyFiles(pyModuleFqn, usedFiles)

        // collect top level methods:

        //val fqnComponents = methodsContainerFile.components.toTypedArray()
        pyFiles
                .asSequence()
                .filter { it.isValid }
                .flatMap { it.topLevelFunctions.asSequence() }
                .filter { it.name != null }
                .forEach { pyFun ->
                    elementsCache.add(ImplicitPySymbol(pyFun.name!!, pyFun, scope))
                }
    }

    private fun collectWorkflowGlobalVariables(
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<ImplicitPySymbol>
    ) {
        // snakemake.workflow -> cluster_config [snakemake/workflow.py, global cluster_config]: dict
        // snakemake.workflow -> config [snakemake/workflow.py, global config]: dict
        // snakemake.rules -> config [snakemake/workflow.py, global rules] : snakemake.workflow.Rules
        // snakemake.checkpoints -> config [snakemake/workflow.py, global rules] : snakemake.checkpoints.Checkpoints

        val workflowFile = collectPyFiles("snakemake.workflow", usedFiles).firstOrNull()

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
                    elementsCache.add(ImplicitPySymbol(name, psi, SmkCodeInsightScope.TOP_LEVEL))
                }
            }
        }
    }

    fun scheduleUpdate() {
        SwingUtilities.invokeLater {
            onChange(true)
        }
    }
}

private class ImplicitPySymbolsCacheImpl(
        private val module: Module,
        symbols: List<ImplicitPySymbol>,
        override val contentVersion: Int = 0
): ImplicitPySymbolsCache {

    private val scope2Symbols = toMap(symbols)

    override operator fun get(scope: SmkCodeInsightScope) = validElements(scope2Symbols.getValue(scope))

    private fun validElements(elements: List<ImplicitPySymbol>): List<ImplicitPySymbol> {
        val validElements = elements.filter { it.psiDeclaration.isValid }
        if (validElements.size != elements.size) {
            ImplicitPySymbolsProvider.instance(module).scheduleUpdate()
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
