package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.validation.PyAnnotationHolder
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotatorVisitorBase
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.highlighter.SmkSLSyntaxHighlighter.Companion.HIGHLIGHTING_WILDCARDS_KEY
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression

class SmkSLWildcardsAnnotator: Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Registered against `language="Python"`, so this runs for every element of every Python
        // file. Since 2026.2 the annotator is built per call rather than being a shared singleton
        // (see SnakemakeAnnotatorVisitorBase), so check the file before allocating anything -- SmkSL is only
        // injected inside Snakemake files (see SmkSLInjector.isValidForInjection).
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(element)) {
            return
        }

        element.accept(SmkSLWildcardsAnnotatorVisitor(PyAnnotationHolder(holder)))
    }
}

class SmkSLWildcardsAnnotatorVisitor(holder: PyAnnotationHolder) : SnakemakeAnnotatorVisitorBase(holder), SmkSLElementVisitor {
    override val pyElementVisitor: PyElementVisitor
        get() = this

    override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
        val exprIdentifier = expr.nameIdentifier

        @Suppress("UnstableApiUsage")
        when {
            expr.isWildcard() -> {
                addHighlightingAnnotation(
                    expr, HIGHLIGHTING_WILDCARDS_KEY, HighlightSeverity.INFORMATION
                )
            }

            exprIdentifier != null -> {
                val qualifier = expr.qualifier
                if (qualifier != null && !DumbService.isDumb(expr.project)) {
                    val type = TypeEvalContext.codeAnalysis(expr.project, expr.containingFile).getType(qualifier)
                    if (type is SmkWildcardsType) {
                        addHighlightingAnnotation(
                            exprIdentifier, HIGHLIGHTING_WILDCARDS_KEY, HighlightSeverity.INFORMATION
                        )
                    }
                }
            }
        }
    }
}