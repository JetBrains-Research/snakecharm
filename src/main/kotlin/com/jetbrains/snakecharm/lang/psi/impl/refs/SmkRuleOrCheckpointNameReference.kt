package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.module.ModuleUtilCore
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
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkModule
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression
import com.jetbrains.snakecharm.lang.psi.SmkUse
import com.jetbrains.snakecharm.lang.psi.SmkImportedRulesNames
import com.jetbrains.snakecharm.lang.psi.stubs.SmkModuleNameIndex
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType

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
        val itIsModuleMameReference = element.parent is SmkUse
        val parentIsImportedRuleNames = element.parent is SmkImportedRulesNames
        if (!parentIsImportedRuleNames && !itIsModuleMameReference) {
            return results
        }
        val allImportedFiles = smkFile.collectIncludedFiles()
        val potentialModule = moduleRef?.reference?.resolve() as? SmkModule
        return results.filter { resolveResult ->
            // If we resolve module references, there must be only SmkModules
            (resolveResult.element is SmkModule && itIsModuleMameReference) ||
                    // We don't want to suggest local resolve result for the reference of rule, which was imported
                    (moduleRef != null // Module name reference is defined and resolve result is from another file
                            && element.containingFile.originalFile != resolveResult.element?.containingFile?.originalFile
                            // Also, because of using indexes, we need to check if the resolve result file
                            // Connected to the file, which was declared in moduleRef, via 'include' or 'module'
                            // Later, allImportedFiles will be stored in cache
                            && resolveResult.element?.containingFile?.originalFile in ((potentialModule?.getPsiFile() as? SmkFile)?.collectIncludedFiles()
                        ?: listOf()))
                    // OR There are no 'from *name*' combination, so it hasn't been imported
                    || (moduleRef == null && resolveResult.element?.containingFile?.originalFile in allImportedFiles)
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
        val modules = if (module == null) {
            smkFile.collectModules().map { it.second }.filter { modulePsi -> modulePsi.name == target }
        } else {
            StubIndex.getElements(
                SmkModuleNameIndex.KEY,
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