package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.impl.references.PyReferenceImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.stubs.SmkModuleNameIndexCompanion
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType

@Suppress("UnstableApiUsage")
class SmkRuleOrCheckpointNameReference(
    element: SmkReferenceExpression,
    context: PyResolveContext
) : PyReferenceImpl(element, context) { //.Poly<SmkReferenceExpression>(element, textRange, true), PsiReferenceEx {
    override fun getElement() = super.getElement() as SmkReferenceExpression

    /**
     *  In case of undefined name after 'rules.' or 'checkpoints.'
     *  Severity is handled by [SmkPyQualifiedReference]
     */
    override fun getUnresolvedDescription() = SnakemakeBundle.message(
        "INSP.py.unresolved.ref.rule.like.ref.message", element.name ?: "<unnamed>"
    )


    override fun getUnresolvedHighlightSeverity(typeEvalContext: TypeEvalContext?) = HighlightSeverity.ERROR!!


    override fun resolveInner(): MutableList<RatedResolveResult> {
        val smkFile = element.containingFile
        if (smkFile !is SmkFile) {
            return mutableListOf()
        }


        val name = element.text
        val results = arrayListOf<RatedResolveResult>()
        val ctx = AccessDirection.of(this.myElement)

        results.addAll(SmkRulesType(null, smkFile).resolveMember(name, element, ctx, myContext))
        results.addAll(SmkCheckpointType(null, smkFile).resolveMember(name, element, ctx, myContext))
        results.addAll(collectModulesAndResolveThem(smkFile, element))

        val moduleRef = element.parentOfType<SmkUse>()?.getModuleName() as? SmkReferenceExpression
        val itIsModuleNameReference = element.parent is SmkUse
        val parentIsImportedRuleNames = element.parent is SmkImportedRulesNamesList
        if (!parentIsImportedRuleNames && !itIsModuleNameReference) {
            return results
        }
        val allImportedFiles = smkFile.collectIncludedFilesRecursively()
        return results.filter { resolveResult ->
            // If we resolve module references, there must be only SmkModules
            val target = resolveResult.element
            val targetFile = target?.containingFile?.originalFile
            (target is SmkModule && itIsModuleNameReference) ||
                    // We don't want to suggest local resolve result for the reference of rule, which was imported
                    (moduleRef != null // Module name reference is defined and resolve result is from another file
                            && element.containingFile.originalFile != targetFile)
                    // OR There are no 'from *name*' combination, so it hasn't been imported
                    || (moduleRef == null && targetFile in allImportedFiles)
        }.toMutableList()
    }

    /**
     * Collects all modules sections names from local file or using indexes which name is [element] name
     */
    private fun collectModulesAndResolveThem(
        smkFile: SmkFile,
        element: SmkReferenceExpression
    ): List<RatedResolveResult> {
        val module = element.let { ModuleUtilCore.findModuleForPsiElement(it.originalElement) }
        val target = element.referencedName ?: return emptyList()
        val modules = if (module == null || DumbService.isDumb(smkFile.project)) {
            // file out of project or in dumb mode, resolve by local file PSE
            smkFile.collectModules().map { it.second }.filter { modulePsi -> modulePsi.name == target }
        } else {
            StubIndex.getElements(
                SmkModuleNameIndexCompanion.KEY,
                target,
                module.project,
                GlobalSearchScope.moduleWithDependentsScope(module),
                SmkModule::class.java
            )
        }
        if (modules.isEmpty()) {
            return emptyList()
        }
        return modules.map { variant ->
            RatedResolveResult(SmkResolveUtil.RATE_NORMAL, variant)
        }
    }
}