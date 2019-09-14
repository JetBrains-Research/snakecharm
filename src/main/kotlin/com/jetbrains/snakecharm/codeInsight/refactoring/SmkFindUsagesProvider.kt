package com.jetbrains.snakecharm.codeInsight.refactoring

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.snakecharm.codeInsight.resolve.SmkFakePsiElement
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSection

/**
 * Provides correct usages types for Snakemake specific elements, should be executed before Python impl.
 */
class SmkFindUsagesProvider: FindUsagesProvider {
    override fun getWordsScanner() = SmkWordsScanner()

    override fun getNodeText(element: PsiElement, useFullName: Boolean) = getDescriptiveName(element)

    override fun getDescriptiveName(element: PsiElement) = if (element is PsiNamedElement) {
        element.name ?: "<unnamed>"
    } else {
        ""
    }

    override fun getType(element: PsiElement) = when (element) {
        is SmkRuleOrCheckpointArgsSection -> "rule section"
        is SmkSection -> "section"
        else -> ""
    }

    override fun getHelpId(psiElement: PsiElement) = null

    override fun canFindUsagesFor(element: PsiElement): Boolean {
       return element is SmkSection || element is SmkFakePsiElement
    }
}

class SmkWordsScanner : DefaultWordsScanner(
        SnakemakeLexer(),
        TokenSet.orSet(WORKFLOW_TOPLEVEL_DECORATORS, TokenSet.create(PyTokenTypes.IDENTIFIER)),
        TokenSet.create(PyTokenTypes.END_OF_LINE_COMMENT),
        PyTokenTypes.STRING_NODES
)