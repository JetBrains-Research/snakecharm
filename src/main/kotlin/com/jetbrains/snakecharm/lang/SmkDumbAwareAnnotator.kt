package com.jetbrains.snakecharm.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.jetbrains.python.validation.PyAnnotationHolder
import com.jetbrains.snakecharm.lang.highlighter.SmkSyntaxAnnotatorVisitor
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.validation.SmkSyntaxErrorAnnotatorVisitor

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-09
 *
 * XXX: Base on `PySyntaxAnnotator` (DumbAware) that also provides list of visitors (including 'PyReturnYieldAnnotatorVisitor')
 * that don't depend on resolve. According to java doc the visitors are planned to be converted into PyAstElementVisitor,
 * such change could be also desired here
 */
class SmkDumbAwareAnnotator : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Are called once per PSI element, PyCharm team thinks that no much profit to cache
        // visitors in session
        if (element.containingFile !is SmkFile) {
            return
        }
        @Suppress("JetBrainsInternalApiUsage") val holder = PyAnnotationHolder(holder)
        val visitors = listOf(
            SmkSyntaxAnnotatorVisitor(holder),
            SmkSyntaxErrorAnnotatorVisitor(holder)
        )
        visitors.forEach { element.accept(it) }
    }
}
