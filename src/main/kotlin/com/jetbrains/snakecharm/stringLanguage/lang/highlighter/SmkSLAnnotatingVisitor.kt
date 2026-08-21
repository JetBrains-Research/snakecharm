package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.jetbrains.python.validation.PyAnnotationHolder

class SmkSLAnnotatingVisitor : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        element.accept(SmkSLWildcardsAnnotator(PyAnnotationHolder(holder)))
    }
}
