package com.jetbrains.snakecharm.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.jetbrains.python.validation.PyAnnotator
import com.jetbrains.snakecharm.lang.highlighter.SmkSyntaxAnnotator
import com.jetbrains.snakecharm.lang.highlighter.SmkWildcardsAnnotator
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.validation.SmkSyntaxErrorAnnotator

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-09
 */
abstract class SmkAnnotatorManager : Annotator, DumbAware {
    private var myHolder: AnnotationHolder? = null

    abstract val annotators: List<PyAnnotator>

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile
        if (file is SmkFile) {
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

class SmkStandardAnnotatorManager : SmkAnnotatorManager() {
    override val annotators: List<PyAnnotator> = listOf(
        // NB: the "'return' outside of function" check that SmkReturnAnnotator used to permit inside
        // snakemake run/python blocks now lives in the platform's final PySyntaxAnnotator; the false
        // positive is suppressed by SmkReturnHighlightInfoFilter (a daemon.highlightInfoFilter) instead.
        SmkWildcardsAnnotator // requires resolve, that based on indexes access
    )
}

class SmkDumbAwareAnnotatorManager : SmkAnnotatorManager(), DumbAware {
    override val annotators = listOf(
            SmkSyntaxAnnotator,
            SmkSyntaxErrorAnnotator
    )
}
