package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity.INFORMATION
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.intellij.lang.regexp.RegExpHighlighter

class SmkSLRegExpHighlightingAnnotator : Annotator {
    companion object {
        val REGEXP_HIGHLIGHTER = RegExpHighlighter(null, null)
    }

    override fun annotate(
            element: PsiElement,
            holder: AnnotationHolder
    ) {

        if (element !is LeafPsiElement) {
            return
        }

        val highlights = REGEXP_HIGHLIGHTER.getTokenHighlights(element.node.elementType)
        if (highlights.isNotEmpty()) {
            holder.newSilentAnnotation(INFORMATION)
                .textAttributes(highlights.first())
                .range(element.textRange)
                .create()
        }
    }
}