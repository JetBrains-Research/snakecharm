package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.util.elementType
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.impl.references.PyReferenceImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseNameIndex
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType
import com.jetbrains.snakecharm.lang.psi.types.SmkUsesType

// Now it looks like SmkRuleLikeNameReference, can we rename it?
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
        //results.addAll(SmkUsesType(null, smkFile).resolveMember(name, element, ctx, myContext)) // Idk why it doesn't work
        results.addAll(collectModulesAndResolveThem(smkFile, name))
        results.addAll(collectUseRulesAndResolveThem(smkFile, element))

        return results
    }

    /**
     * Currently we can't create new class, as we did with rules and checkpoints (SmkRulesType, SmkCheckpointType)
     * because SmkModule does not implement [com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint],
     * so we use this method instead, to collect all modules and resolve module reference
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
     * Resolve rule reference, which is declared in use section.
     * It's trying to find rule reference into other use declarations.
     * If there none, it refers to module, which imports such rule.
     * If there no such module, return empty array.
     */
    private fun collectUseRulesAndResolveThem(
        smkFile: SmkFile,
        element: SmkReferenceExpression
    ): List<RatedResolveResult> {
        if (element.parent.elementType == SmkStubElementTypes.USE_DECLARATION_STATEMENT) {
            val uses = smkFile.collectUses().map { it.second }.filter { elem -> elem.name == element.name }
            if (uses.isNotEmpty()) {
                return uses.map {
                    RatedResolveResult(SmkResolveUtil.RATE_NORMAL, it)
                }
            }

            var moduleRef = element.nextSibling
            while (moduleRef != null && moduleRef.elementType != element.elementType) {
                moduleRef = moduleRef.nextSibling
            }
            if (moduleRef != null) {
                return SmkRuleOrCheckpointNameReference(moduleRef as SmkReferenceExpression, myContext).resolveInner()
            }
        }

        return emptyList()
    }
}