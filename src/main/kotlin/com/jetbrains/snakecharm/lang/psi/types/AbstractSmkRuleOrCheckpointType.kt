package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.intellij.util.Processors
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndexCompanion
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseNameIndexCompanion


abstract class AbstractSmkRuleOrCheckpointType<T : SmkRuleOrCheckpoint>(
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

        var results = findAvailableRuleLikeElementByName(
            location.originalElement, name, indexKey, clazz
        ) { currentFileDeclarations }

        if (location.text == SMK_VARS_RULES) {
            results += findAvailableRuleLikeElementByName(
                location.originalElement,
                name,
                SmkCheckpointNameIndexCompanion.KEY,
                SmkCheckPoint::class.java
            ) { (containingRule?.containingFile as? SmkFile)?.filterCheckPointsPsi()?.map { it.second } ?: emptyList() }
        }

        return results.map { element ->
            // XXX: Or ? SmkResolveUtil.RATE_NORMAL
            RatedResolveResult(RatedResolveResult.RATE_LOW, element)
        } + getUseSections(name, location)
    }

    override fun isBuiltin() = false

    protected open fun getUseSections(name: String, location: PyExpression): List<RatedResolveResult> {
        val result = mutableListOf<RatedResolveResult>()
        if (location.parent is SmkUse) {
            // Current reference is module reference
            //
            // XXX: We use `location.parent` instead of `containgRule`, because here we want to ignore only
            //  the module reference in use section, and do not want to ignore e.g.
            //  references inside USE_IMPORTED_RULES_NAMES, but both references would have SmkUse as containingRule,
            //  so in case of module reference we want to check type of the first parent. Also the containingRule
            //  is null when it is called from SmkRuleOrCheckpointNameReference so that's another reason
            return result
        }
        val module = location.let { ModuleUtilCore.findModuleForPsiElement(it.originalElement) }
        val parent = location.parentOfType<SmkRuleOrCheckpoint>()
        val useExcludedNamesList = PsiTreeUtil.getParentOfType(
            location, SmkExcludedRulesNamesList::class.java, SmkRuleLike::class.java
        )
        val useToIgnore = (useExcludedNamesList as? SmkExcludedRulesNamesList)?.getParentUse()
        when {
            (module == null) || DumbService.isDumb(location.project) -> (location.containingFile.originalFile as SmkFile)
                .filterUsePsi()
                .map { it.second }
            else -> getVariantsFromIndex(SmkUseNameIndexCompanion.KEY, module, SmkUse::class.java)
        }.forEach { use ->
            val candidates = if (use == useToIgnore) {
                use.collectImportedRuleNameAndPsi(mutableSetOf(), false)
                    ?: emptyList<Pair<String, SmkRuleOrCheckpoint>>()
            } else {
                use.getProducedRulesNames()
            }
            val referTo = candidates
                .firstOrNull { (candName, candPsi) ->
                    when {
                        candName != name -> false
                        else -> {
                            val candRuleOrCp = if (candPsi is SmkRuleOrCheckpoint) candPsi else candPsi.parentOfType()
                            candRuleOrCp != parent
                        }
                    }
                }
            if (referTo != null) {
                result.add(RatedResolveResult(SmkResolveUtil.RATE_NORMAL, use))
            }
        }

        // Checks rule name patterns, produced by 'use' sections
        if (result.isEmpty()) {
            val namePattern = (location.containingFile as? SmkFile)?.resolveByRuleNamePattern(name)
            if (namePattern != null) {
                result.add(RatedResolveResult(RatedResolveResult.RATE_LOW, namePattern))
            }
        }

        return result
    }

    companion object {
        fun <T : SmkRuleOrCheckpoint> createRuleLikeLookupItem(name: String, elem: T): LookupElement {
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

        fun <T : SmkRuleOrCheckpoint> findAvailableRuleLikeElementByName(
            location: PsiElement,
            name: String,
            indexKey: StubIndexKey<String, T>,
            clazz: Class<T>,
            getCurrentFileDeclarationsFunction: () -> List<T>
        ): Collection<PsiElement> {
            val module = location.let { ModuleUtilCore.findModuleForPsiElement(it.originalElement) }
            if (module != null) {
                val scope = searchScope(module)
                if (!DumbService.isDumb(module.project)) {
                    return StubIndex.getElements(indexKey, name, module.project, scope, clazz)
                }
            }

            // try local resolve
            return getCurrentFileDeclarationsFunction().filter { elem ->
                elem.name == name
            }
        }

        fun <Psi : SmkRuleOrCheckpoint> getVariantsFromIndex(
            indexKey: StubIndexKey<String, Psi>,
            module: Module,
            clazz: Class<Psi>,
            scope: GlobalSearchScope = searchScope(module)
        ): List<Psi> {
            val results = mutableListOf<Psi>()
            val project = module.project
            val stubIndex = StubIndex.getInstance()
            val allKeys = HashSet<String>()
            stubIndex.processAllKeys(
                indexKey, Processors.cancelableCollectProcessor(allKeys), scope, null
            )

            for (key in allKeys) {
                results.addAll(StubIndex.getElements(indexKey, key, project, scope, clazz))
            }
            return results
        }

        private fun searchScope(module: Module): GlobalSearchScope {
            // module content root and all dependent modules:
            @Suppress("UnnecessaryVariable") val scope = globalSearchScope(module)

            // all project w/o tests from project sdk roots
            // PyProjectScopeBuilder.excludeSdkTestsScope(targetFile);

            //Returns module scope including sources, tests, and dependencies, excluding libraries:
            // * if files in content root, not in source root - not visible in scope
            // GlobalSearchScope.moduleWithDependenciesScope(module)

            // project with all modules and libs:
            // GlobalSearchScope.allScope(project)
            return scope
        }

        private fun globalSearchScope(module: Module): GlobalSearchScope {
            val scope = GlobalSearchScope.moduleWithDependentsScope(module)
            return scope
        }
    }
}