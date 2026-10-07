package com.jetbrains.snakecharm.lang.validation

import com.intellij.lang.ASTNode
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.validation.PyAnnotationHolder
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor

/**
 * Base for our annotators, replacing the platform's `PyAnnotator`, which was removed in PyCharm
 * 2026.2 (build 262). The platform's own annotators moved to the same shape: a plain
 * [PyElementVisitor] holding a [PyAnnotationHolder] passed in at construction (see e.g.
 * `PyReturnYieldAnnotatorVisitor`), instead of a base class carrying a mutable holder set per call.
 *
 * The `addHighlightingAnnotation` helpers below are the ones `PyAnnotator` used to provide, kept so
 * that annotator subclasses read as before.
 */
abstract class SmkAnnotatorBase(protected val holder: PyAnnotationHolder) : PyElementVisitor() {
    @Suppress("UnstableApiUsage")
    protected fun addHighlightingAnnotation(target: PsiElement, key: TextAttributesKey) =
        holder.addHighlightingAnnotation(target, key)

    @Suppress("UnstableApiUsage")
    protected fun addHighlightingAnnotation(
        target: PsiElement,
        key: TextAttributesKey,
        severity: HighlightSeverity
    ) = holder.addHighlightingAnnotation(target, key, severity)

    @Suppress("UnstableApiUsage")
    protected fun addHighlightingAnnotation(target: ASTNode, key: TextAttributesKey) =
        holder.addHighlightingAnnotation(target, key)
}

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-09
 */
abstract class SmkAnnotator(holder: PyAnnotationHolder) : SmkAnnotatorBase(holder), SmkElementVisitor {
    override val pyElementVisitor: PyElementVisitor
        get() = this
}
