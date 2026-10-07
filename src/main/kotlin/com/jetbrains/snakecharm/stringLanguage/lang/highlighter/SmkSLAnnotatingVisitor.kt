package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.jetbrains.python.validation.PyAnnotationHolder
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil

class SmkSLAnnotatingVisitor : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Registered against `language="Python"`, so this runs for every element of every Python
        // file. Since 2026.2 the annotator is built per call rather than being a shared singleton
        // (see SmkAnnotatorBase), so check the file before allocating anything -- SmkSL is only
        // injected inside Snakemake files (see SmkSLInjector.isValidForInjection).
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(element)) {
            return
        }
        element.accept(SmkSLWildcardsAnnotator(PyAnnotationHolder(holder)))
    }
}
