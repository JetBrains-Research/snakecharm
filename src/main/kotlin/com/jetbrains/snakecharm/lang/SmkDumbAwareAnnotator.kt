package com.jetbrains.snakecharm.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElementVisitor
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
    private val visitorsKey = Key.create<List<PyElementVisitor>>(javaClass.name + ".annotators")

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element.containingFile !is SmkFile) {
            return
        }

        val session = holder.currentAnnotationSession
        // TODO: ask Petr.Golubev about 'PyLocalVariableHighlightingAnnotator'
        // TODO: previously were cached in annotator, now in session
        val visitors = session.getUserData(visitorsKey)
            ?: createAnnotators(PyAnnotationHolder(holder)).also { session.putUserData(visitorsKey, it) }

        visitors.forEach { element.accept(it) }
    }

    fun createAnnotators(holder: PyAnnotationHolder): List<PyElementVisitor> = listOf(
        SmkSyntaxAnnotatorVisitor(holder),
        SmkSyntaxErrorAnnotatorVisitor(holder)
    )
}
