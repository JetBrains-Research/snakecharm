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
        val cache = ImplicitPySymbolsCache.instance(module)
        if (forceClear) {
            cache.clear()
        }

        val project = module.project
        DumbService.getInstance(project).runWhenSmart {
            runReadAction {
                if (module.isDisposed) {
                    return@runReadAction
                }
                
                // TODO: Runtime uses Workflow().self.globals from snakemake/workflow.py

                val usedFiles = arrayListOf<PsiFile>()

                //val elementsCache = ArrayList<Pair<QualifiedName, PyElement>>()
                val elementsCache = ArrayList<Pair<String, PyElement>>()

                ///////////////////////////////////////
                // E.g. rules, config, ... defined in Workflow code as global variables
                collectWorkflowGlobalVariables(usedFiles, elementsCache)

                ///////////////////////////////////////
                // E.g. expand, temp, .. from 'snakemake.io'
                collectTopLevelMethodsFrom(
                        "snakemake.io", usedFiles, elementsCache
                )

                ///////////////////////////////////////
                // Collect hardcoded methods
                collectMethods(listOf(
                        "snakemake.utils" to "simplify_path",
                        "snakemake.wrapper" to "wrapper",
                        "snakemake.script" to "script"
                ), usedFiles, elementsCache)

                ///////////////////////////////////////
                // Collect hardcoded classes
                collectClasses(listOf(
                        "snakemake.shell" to "shell"
                ), usedFiles, elementsCache)

                ///////////////////////////////////////
                // Collect variables
                collectVars(listOf(
                        "snakemake.logging" to "logger"
                ), usedFiles, elementsCache)


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
                val cacheContentVers = cache.contentVersion()
                if (cacheContentVers == 0 || contentVersion != cacheContentVers || forceClear) {
                    val newContent = elementsCache.groupBy(
                            keySelector = { it.first },
                            valueTransform = { it.second }
                    )
                    cache.replaceWith(contentVersion, newContent)

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
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<Pair<String, PyElement>>
    ) {
        moduleAndVariable.forEach { (pyModuleFqn, varName) ->
            collectPyFiles(pyModuleFqn, usedFiles)
                    .forEach { pyFile ->
                        val attrib = pyFile.findTopLevelAttribute(varName)
                        if (attrib != null) {
                            elementsCache.add(attrib.name!! to attrib)
                        }
                    }
        }
    }

    private fun collectClasses(
            moduleAndClass: List<Pair<String, String>>,
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<Pair<String, PyElement>>
    ) {
        moduleAndClass.forEach { (pyModuleFqn, className) ->
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
                        if (constructor != null) {
                            elementsCache.add(pyClass.name!! to constructor)
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
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<Pair<String, PyElement>>
    ) {
        moduleAndMethod.forEach { (pyModuleFqn, methodName) ->
            val pyFiles = collectPyFiles(pyModuleFqn, usedFiles)

            pyFiles
                    .filter { it.isValid }
                    .mapNotNull { it.findTopLevelFunction(methodName) }
                    .forEach { pyFun ->
                        elementsCache.add(pyFun.name!! to pyFun)
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
        ).filter { it is PyFile } as List<PyFile>

        usedFiles.addAll(pyFiles)
        return pyFiles
    }

    private fun collectTopLevelMethodsFrom(
            pyModuleFqn: String,
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<Pair<String, PyElement>>
    ) {
        val pyFiles = collectPyFiles(pyModuleFqn, usedFiles)

        // collect top level methods:

        //val fqnComponents = methodsContainerFile.components.toTypedArray()
        pyFiles
                .filter { it.isValid }
                .flatMap { it.topLevelFunctions }
                .forEach { pyFun ->
                    elementsCache.add(pyFun.name!! to pyFun)
                }
    }

    private fun collectWorkflowGlobalVariables(
            usedFiles: MutableList<PsiFile>,
            elementsCache: MutableList<Pair<String, PyElement>>
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
                    elementsCache.add(name to psi)
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