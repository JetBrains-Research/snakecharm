package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.jetbrains.snakecharm.lang.psi.SnakemakeFile

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-09
 */
class SnakemakeDumbAwareAnnotator: Annotator, DumbAware {
    private var myHolder: AnnotationHolder? = null

    companion object {
        val ANNOTATORS = listOf(SnakemakeSyntaxAnnotator)
    }

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile
        if (file is SnakemakeFile) {
            ANNOTATORS.forEach {
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

