package com.jetbrains.snakecharm.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.validation.PyAnnotationHolder
import com.jetbrains.snakecharm.lang.highlighter.SmkSyntaxAnnotator
import com.jetbrains.snakecharm.lang.highlighter.SmkWildcardsAnnotator
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.validation.SmkSyntaxErrorAnnotator

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-09
 */
abstract class SmkAnnotatorManager : Annotator, DumbAware {
    /**
     * Annotators bind their [PyAnnotationHolder] at construction since PyCharm 2026.2 (build 262)
     * removed `PyAnnotator`, so they are built per annotation pass rather than held as singletons.
     */
    abstract fun createAnnotators(holder: PyAnnotationHolder): List<PyElementVisitor>

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val file = element.containingFile
        if (file is SmkFile) {
            val pyHolder = PyAnnotationHolder(holder)
            createAnnotators(pyHolder).forEach { element.accept(it) }
        }
    }
}

class SmkStandardAnnotatorManager : SmkAnnotatorManager() {
    override fun createAnnotators(holder: PyAnnotationHolder): List<PyElementVisitor> = listOf(
        // NB: the "'return' outside of function" check that SmkReturnAnnotator used to permit inside
        // snakemake run/python blocks now lives in the platform's final PySyntaxAnnotator; the false
        // positive is suppressed by SmkReturnHighlightInfoFilter (a daemon.highlightInfoFilter) instead.
        SmkWildcardsAnnotator(holder) // requires resolve, that based on indexes access
    )
}

class SmkDumbAwareAnnotatorManager : SmkAnnotatorManager(), DumbAware {
    override fun createAnnotators(holder: PyAnnotationHolder): List<PyElementVisitor> = listOf(
        SmkSyntaxAnnotator(holder),
        SmkSyntaxErrorAnnotator(holder)
    )
}
