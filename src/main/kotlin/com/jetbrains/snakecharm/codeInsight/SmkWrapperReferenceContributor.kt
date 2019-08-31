package com.jetbrains.snakecharm.codeInsight

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SmkKeywordCompletionContributor
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkWrapperReference

class SmkWrapperReferenceContributor : PsiReferenceContributor() {
    private val wrapperSectionPattern =
            PlatformPatterns.psiElement(PyStringLiteralExpression::class.java)
                    .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
                    .inside(SmkRuleOrCheckpointArgsSection::class.java)

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(wrapperSectionPattern, object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                val containingSection = PsiTreeUtil.getParentOfType(element, SmkRuleOrCheckpointArgsSection::class.java)
                if (containingSection?.sectionKeyword != SnakemakeNames.SECTION_WRAPPER) {
                    return emptyArray()
                }

                return arrayOf(SmkWrapperReference(element as PyStringLiteralExpression))
            }
        })
    }
}