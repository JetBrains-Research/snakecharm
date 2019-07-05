package com.jetbrains.snakecharm.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.jetbrains.python.validation.PyAnnotator
import com.jetbrains.snakecharm.lang.highlighter.SnakemakeSyntaxAnnotator
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile
import com.jetbrains.snakecharm.lang.validation.SmkReturnAnnotator
import com.jetbrains.snakecharm.lang.validation.SnakemakeSyntaxErrorAnnotator

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-09
 */
abstract class SnakemakeAnnotatorManager : Annotator, DumbAware {
    private var myHolder: AnnotationHolder? = null

    abstract val annotators: List<PyAnnotator>

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile
        if (file is SnakemakeFile) {
            annotators.forEach {
                myHolder = holder
                try {
                    it.annotateElement(element, holder)
                } finally {
                    myHolder = null
                }
            }
        }

    }
}

class SmkStandardAnnotatorManager : SnakemakeAnnotatorManager(), DumbAware {
    override val annotators = listOf(
            SmkReturnAnnotator
    )
}

class SmkDumbAwareAnnotatorManager : SnakemakeAnnotatorManager(), DumbAware {
    override val annotators = listOf(
            SnakemakeSyntaxAnnotator,
            SnakemakeSyntaxErrorAnnotator
    )
}
