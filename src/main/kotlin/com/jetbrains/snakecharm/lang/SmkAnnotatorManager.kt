package com.jetbrains.snakecharm.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
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
     * removed `PyAnnotator`, so they cannot be singletons the way they used to be.
     *
     * They are not per-pass either: [annotate] is called once per PSI *element*, so building them
     * there allocates the whole set (plus a [PyAnnotationHolder]) for every element of every
     * Snakefile on every highlighting pass. They are therefore cached on the annotation session,
     * which is exactly the scope of the holder they capture -- one file, one pass. The visitors keep
     * no state between elements, so sharing them within a pass is safe.
     */
    abstract fun createAnnotators(holder: PyAnnotationHolder): List<PyElementVisitor>

    private val annotatorsKey = Key.create<List<PyElementVisitor>>(javaClass.name + ".annotators")

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.containingFile !is SmkFile) {
            return
        }

        val session = holder.currentAnnotationSession
        val annotators = session.getUserData(annotatorsKey)
            ?: createAnnotators(PyAnnotationHolder(holder)).also { session.putUserData(annotatorsKey, it) }

        annotators.forEach { element.accept(it) }
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
