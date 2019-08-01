package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.ProcessingContext
import com.intellij.util.Processors
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.python.codeInsight.completion.PythonCompletionWeigher
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint


abstract class AbstractSmkRuleOrCheckpointType<T: SmkRuleOrCheckpoint>(
        private val containingRule: T?,
        private val typeName: String,
        private val indexKey: StubIndexKey<String, T>,
        private val clazz: Class<T>
) : PyType {

    abstract val currentFileDeclarations: List<T>

    override fun getName() = typeName

    override fun getCompletionVariants(
            completionPrefix: String?,
            location: PsiElement,
            context: ProcessingContext?
    ): Array<Any> {
        if (!SnakemakeLanguageDialect.isInsideSmkFile(location)) {
            return emptyArray()
        }

        val results = ArrayList<T>()

        val module = ModuleUtilCore.findModuleForPsiElement(location.originalElement)
        if (module != null) {
            addVariantFromIndex(indexKey, module, results, clazz, searchScope(module))
        }

        if (results.isEmpty()) {
            // show at list current file:
            results.addAll(currentFileDeclarations)
        }

        return results.stream()
                .filter { it.name != null && it != containingRule }
                .map { createRuleLikeLookupItem(it.name!!, it) }
                .toArray()
    }

    override fun assertValid(message: String?) {
        // [romeo] Not sure is our type always valid or check whether any element is invalid
    }

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        if (!SnakemakeLanguageDialect.isInsideSmkFile(location) || location == null) {
            return emptyList()
        }

        val results = findAvailableRuleLikeElementByName(location.originalElement, name, indexKey, clazz) { currentFileDeclarations }

        if (results.isEmpty()) {
            return emptyList()
        }

        return results.map { element ->
            RatedResolveResult(RatedResolveResult.RATE_NORMAL, element)
        }
    }

    override fun isBuiltin() = false

    companion object {
        fun <T: SmkRuleOrCheckpoint> createRuleLikeLookupItem(name: String, elem: T): LookupElement =
                PrioritizedLookupElement.withPriority(
                        LookupElementBuilder
                                .createWithSmartPointer(name, elem)
                                .withTypeText(elem.containingFile.name)
                                .withIcon(elem.getIcon(0))
                        ,
                        PythonCompletionWeigher.WEIGHT_DELTA.toDouble()
                )

        fun <T: SmkRuleOrCheckpoint> findAvailableRuleLikeElementByName(
                location: PsiElement,
                name: String,
                indexKey: StubIndexKey<String, T>,
                clazz: Class<T>,
                getCurrentFileDeclarationsFunction: () -> List<T>
        ): Collection<PsiElement> {
            val module = location.let {  ModuleUtilCore.findModuleForPsiElement(it.originalElement) }
            return if (module != null) {
                val scope = searchScope(module)
                StubIndex.getElements(indexKey, name, module.project, scope, clazz)
            } else {
                // try local resolve
                getCurrentFileDeclarationsFunction().filter { elem ->
                    elem.name == name
                }
            }
        }

        fun <Psi: SmkRuleOrCheckpoint> addVariantFromIndex(
                indexKey: StubIndexKey<String, Psi>,
                module: Module,
                results: MutableList<Psi>,
                clazz: Class<Psi>,
                scope: GlobalSearchScope = searchScope(module)
        ) {
            val project = module.project
            val stubIndex = StubIndex.getInstance()
            val allKeys = ContainerUtil.newTroveSet<String>()
            stubIndex.processAllKeys(
                    indexKey, Processors.cancelableCollectProcessor<String>(allKeys), scope, null
            )

            for (key in allKeys) {
                results.addAll(StubIndex.getElements(indexKey, key, project, scope, clazz))
            }
        }

        private fun searchScope(module: Module): GlobalSearchScope {
            // module content root and all dependent modules:
            val scope = GlobalSearchScope.moduleWithDependentsScope(module)

            // all project w/o tests from project sdk roots
            // PyProjectScopeBuilder.excludeSdkTestsScope(targetFile);

            //Returns module scope including sources, tests, and dependencies, excluding libraries:
            // * if files in content root, not in source root - not visible in scope
            // GlobalSearchScope.moduleWithDependenciesScope(module)

            // project with all modules and libs:
            // GlobalSearchScope.allScope(project)
            return scope
        }

    }
}