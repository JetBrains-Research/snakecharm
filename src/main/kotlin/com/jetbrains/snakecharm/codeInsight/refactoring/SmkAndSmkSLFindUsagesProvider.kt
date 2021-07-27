package com.jetbrains.snakecharm.codeInsight.refactoring

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.findUsages.PythonFindUsagesProvider
import com.jetbrains.snakecharm.codeInsight.resolve.SmkFakePsiElement
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATOR_KEYWORD
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSection
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl

/**
 * Provides correct usages types for Snakemake specific elements, should be executed before Python impl.
 */
class SmkAndSmkSLFindUsagesProvider : PythonFindUsagesProvider() {
    override fun getWordsScanner() = SmkWordsScanner()

    override fun getNodeText(element: PsiElement, useFullName: Boolean) = getDescriptiveName(element)

    override fun getType(element: PsiElement) = when (element) {
        is SmkRuleOrCheckpointArgsSection -> "rule section"
        is SmkRule -> "rule"
        is SmkCheckPoint -> "checkpoint"
        is SmkSection -> "section"
        is SmkFakePsiElement -> "element"
        is SmkSLReferenceExpressionImpl -> {
            if (element.isWildcard()) {
                "wildcard"
            } else {
                super.getType(element)
            }
        }
        else -> super.getType(element)
    }

    override fun getHelpId(psiElement: PsiElement) = null

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element is SmkSection || element is SmkFakePsiElement || element is SmkSLReferenceExpressionImpl
    }
}

class SmkWordsScanner : DefaultWordsScanner(
    SnakemakeLexer(),
    TokenSet.orSet(
        WORKFLOW_TOPLEVEL_DECORATORS,
        TokenSet.create(WORKFLOW_TOPLEVEL_DECORATOR_KEYWORD),
        TokenSet.create(PyTokenTypes.IDENTIFIER)
    ),
    TokenSet.create(PyTokenTypes.END_OF_LINE_COMMENT),
    PyTokenTypes.STRING_NODES
)