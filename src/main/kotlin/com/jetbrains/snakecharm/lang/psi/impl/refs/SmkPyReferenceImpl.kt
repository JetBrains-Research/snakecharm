package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.ResolveResult
import com.intellij.util.containers.toArray
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.impl.references.PyReferenceImpl
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.snakecharm.codeInsight.resolve.SmkImplicitPySymbolsResolveProvider
import com.jetbrains.snakecharm.lang.psi.*

/**
 * This is fake reference which allow as to remove some results from resolve/completion
 *
 * 1. Remove [SmkRuleLike] elements which were added just as all [PsiNameIdentifierOwner] and
 *   [com.intellij.psi.PsiNamedElement]
 *      See [com.jetbrains.python.psi.resolve.PyResolveUtil.scopeCrawlUp] and `visitElement` in
 *      [com.jetbrains.python.codeInsight.dataflow.scope.impl.ScopeImpl.collectDeclarations]
 */
class SmkPyReferenceImpl(
        element: PyQualifiedExpression,
        context: PyResolveContext,
        val inRunSection: Boolean,
        val containingRuleOrCheckpoint: SmkRuleOrCheckpoint?
): PyReferenceImpl(element, context) {
    val USE_CACHE = true

    @Suppress("UsePropertyAccessSyntax")
    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult?> {
        if (USE_CACHE) {
            // XXX: default impl that is dependent on PyReferenceImpl.USE_CACHE
            return super.multiResolve(incompleteCode)
        }

        // DEBUG: Hack for debug to void using CACHING
        val referencedName = myElement.getReferencedName();
        if (referencedName == null) return ResolveResult.EMPTY_ARRAY;

        val targets: List<RatedResolveResult> = resolveInner();
        if (targets.isEmpty()) return ResolveResult.EMPTY_ARRAY;

        return RatedResolveResult.sorted(targets).toArray(ResolveResult.EMPTY_ARRAY)
    }

    override fun resolveInner(): MutableList<RatedResolveResult> {
        val results = super.resolveInner()
        val sizeBeforeFiltration = results.size
        val fitleredResults = results.filter {
            !shouldBeRemovedFromDefaultScopeCrawlUpResults(it?.element, inRunSection, containingRuleOrCheckpoint)
        }
        if (sizeBeforeFiltration != 0 && fitleredResults.isEmpty()) {
            // We've removed all elements from completion (likely named)
            // Our named elements processing inside PyReferenceImpl stops resolve
            // and doesn't collect required implicit symbols
            return resolveAsImplicitSymbol()
        }
        return fitleredResults.toMutableList()
    }

    private fun resolveAsImplicitSymbol(): MutableList<RatedResolveResult> {
        val context = myContext.typeEvalContext

        return PyReferenceResolveProvider.EP_NAME.extensionList.asSequence()
                .filter { it is SmkImplicitPySymbolsResolveProvider }
                .flatMap {  it.resolveName(myElement, context).asSequence() }
                .toMutableList()
    }

    override fun getVariants(): Array<Any> {
        val defaultVariants = super.getVariants()

        // remove rule like elements from basic completion (they are in the list because
        // python collects all named elements although we don't need this
        return defaultVariants.filter { v ->
            when (v) {
                is LookupElement -> !shouldBeRemovedFromDefaultScopeCrawlUpResults(
                        v.psiElement, inRunSection, containingRuleOrCheckpoint
                )
                is PsiElement -> !shouldBeRemovedFromDefaultScopeCrawlUpResults(
                        v, inRunSection, containingRuleOrCheckpoint
                )
                else -> true
            }
        }.toTypedArray()
    }

    companion object {
        fun shouldBeRemovedFromDefaultScopeCrawlUpResults(
                element: PsiElement?,
                inRunSection: Boolean,
                containingRuleOrCheckpoint: SmkRuleOrCheckpoint?
        ): Boolean {
            // TODO: re-implement using smk visitor
            var shouldBeRemoved = (element is SmkSection) && (element !is SmkSubworkflow)
            if (shouldBeRemoved && inRunSection) {
                if (element is SmkRuleOrCheckpointArgsSection) {
                    if (containingRuleOrCheckpoint != null &&
                            containingRuleOrCheckpoint.containingFile == element.containingFile &&
                            containingRuleOrCheckpoint == element.getParentRuleOrCheckPoint()) {
                        shouldBeRemoved = false
                    }
                }
            }
            return shouldBeRemoved
        }
    }
}