package com.jetbrains.snakecharm.codeInsight

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyCallSiteExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SMKKeywordCompletionContributor
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SMKWorkflowParameterListStatement
import com.jetbrains.snakecharm.lang.psi.SmkFileReference

class SmkWorkflowReferenceContributor : PsiReferenceContributor() {
    companion object {
        val SUPPORTED_WORKFLOWS = setOf(SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD,
                SnakemakeNames.WORKFLOW_INCLUDE_KEYWORD, SnakemakeNames.WORKFLOW_REPORT_KEYWORD)
    }

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns
                        .psiElement(PyStringLiteralExpression::class.java)
                        .inFile(SMKKeywordCompletionContributor.IN_SNAKEMAKE)
                        .inside(SMKWorkflowParameterListStatement::class.java)
                        .andNot(PlatformPatterns.psiElement().inside(PyCallSiteExpression::class.java)),

                object : PsiReferenceProvider() {

                    override fun getReferencesByElement(
                            element: PsiElement,
                            context: ProcessingContext
                    ): Array<PsiReference> {
                        val parentListStatement = PsiTreeUtil.getParentOfType(element, SMKWorkflowParameterListStatement::class.java)!!
                        if (parentListStatement.keywordName !in SUPPORTED_WORKFLOWS) {
                            return emptyArray()
                        }

                        return arrayOf(SmkFileReference(element, TextRange(0, element.textLength)))
                    }
                })
    }
}