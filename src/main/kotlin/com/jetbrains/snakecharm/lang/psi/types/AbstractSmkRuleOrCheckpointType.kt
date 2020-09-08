package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.ProcessingContext
import com.intellij.util.Processors
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import gnu.trove.THashSet


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
    ): Array<Any> = emptyArray()

    override fun assertValid(message: String?) {
        // [romeo] Not sure is our type always valid or check whether any element is invalid
        containingRule?.let {
            if (!it.isValid) {
                throw PsiInvalidElementAccessException(it, message)
            }
        }
    }

    override fun resolveMember(
            name: String,
            location: PyExpression?,
            direction: AccessDirection,
            resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location) || location == null) {
            return emptyList()
        }

        val results = findAvailableRuleLikeElementByName(
                location.originalElement, name, indexKey, clazz
        ) { currentFileDeclarations }

        if (results.isEmpty()) {
            return emptyList()
        }

        return results.map { element ->
            RatedResolveResult(SmkResolveUtil.RATE_NORMAL, element)
        }
    }

    override fun isBuiltin() = false

    companion object {
        fun <T: SmkRuleOrCheckpoint> createRuleLikeLookupItem(name: String, elem: T): LookupElement {
            val containingFileName = elem.containingFile.name
            /*
              it is important to use originalFile to access virtualFile
              because a light copy of the file is created during code completion
              and containingFile.virtualFile returns null for that copy
            */
            val virtualFile = elem.containingFile.originalFile.virtualFile
            val displayPath = if (virtualFile == null) {
                containingFileName
            } else {
                val fileContentRootDirectory = ProjectRootManager.getInstance(elem.project)
                        .fileIndex
                        .getContentRootForFile(virtualFile)
                if (fileContentRootDirectory == null) {
                    containingFileName
                } else {
                    VfsUtil.getRelativePath(virtualFile, fileContentRootDirectory) ?: virtualFile.presentableUrl
                }
            }

            return PrioritizedLookupElement.withPriority(
                    LookupElementBuilder
                            .createWithSmartPointer(name, elem)
                            .withTypeText(displayPath)
                            .withIcon(elem.getIcon(0)),
                    SmkCompletionUtil.RULES_AND_CHECKPOINTS_PRIORITY
            )
        }

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

        fun <Psi: SmkRuleOrCheckpoint> getVariantsFromIndex(
                indexKey: StubIndexKey<String, Psi>,
                module: Module,
                clazz: Class<Psi>,
                scope: GlobalSearchScope = searchScope(module)
        ): List<Psi> {
            val results = mutableListOf<Psi>()
            val project = module.project
            val stubIndex = StubIndex.getInstance()
            val allKeys = THashSet<String>()
            stubIndex.processAllKeys(
                    indexKey, Processors.cancelableCollectProcessor<String>(allKeys), scope, null
            )

            for (key in allKeys) {
                results.addAll(StubIndex.getElements(indexKey, key, project, scope, clazz))
            }
            return results
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