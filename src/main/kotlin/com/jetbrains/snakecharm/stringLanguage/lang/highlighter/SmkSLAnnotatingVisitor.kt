package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class SmkSLAnnotatingVisitor : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        SmkSLSyntaxAnnotator.annotateElement(element, holder)
    }
}
