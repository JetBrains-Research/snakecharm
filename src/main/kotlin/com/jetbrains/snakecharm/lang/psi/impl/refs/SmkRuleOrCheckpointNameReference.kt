package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.impl.references.PyReferenceImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
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
        //results.addAll(SmkUsesType(null, smkFile).resolveMember(name, element, ctx, myContext))
        results.addAll(collectModulesAndResolveThem(smkFile, name))
        results.addAll(collectModuleFromUseSection(element))

        return results
    }

    /**
     * Collects all modules sections names from local file which name is [name]
     */
    private fun collectModulesAndResolveThem(smkFile: SmkFile, name: String): List<RatedResolveResult> {
        val modules = smkFile.collectModules().map { it.second }.filter { elem -> elem.name == name }
        if (modules.isEmpty()) {
            return emptyList()
        }
        return modules.map { element ->
            RatedResolveResult(SmkResolveUtil.RATE_NORMAL, element)
        }
    }

    /**
     * Resolve rule reference, which is declared in 'use' section.
     * It refers to module, which imports such rule.
     * If there no such module, return empty array.
     */
    private fun collectModuleFromUseSection(
        element: SmkReferenceExpression
    ): List<RatedResolveResult> {
        if (element.parent.elementType != SmkElementTypes.USE_IMPORTED_RULES_NAMES) {
            return emptyList()
        }
        val parent = element.parentOfType<SmkRuleOrCheckpoint>()
        if (parent != null && parent.elementType == SmkStubElementTypes.USE_DECLARATION_STATEMENT) {
            var moduleRef = element.parent.nextSibling
            while (moduleRef != null && moduleRef.elementType != SmkElementTypes.REFERENCE_EXPRESSION) {
                moduleRef = moduleRef.nextSibling
            }
            if (moduleRef != null) {
                return listOf(
                    RatedResolveResult(
                        SmkResolveUtil.RATE_NORMAL,
                        (moduleRef as SmkReferenceExpression).reference.resolve()
                    )
                )
            }
        }

        return emptyList()
    }
}