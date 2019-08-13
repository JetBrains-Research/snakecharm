package com.jetbrains.snakecharm.string_language.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.intellij.lang.regexp.RegExpHighlighter

class SmkSLRegExpHighlightingAnnotator : Annotator {
    companion object {
        val REGEXP_HIGHLIGHTER = RegExpHighlighter(null, null)
    }

    override fun annotate(
            element: PsiElement,
            holder: AnnotationHolder) {
        if (element !is LeafPsiElement) {
            return
        }

        val annotation = holder.createAnnotation(
                HighlightSeverity.INFORMATION,
                element.textRange,
                null)
        annotation.textAttributes = REGEXP_HIGHLIGHTER.getTokenHighlights(element.node.elementType).first()
    }
}