package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyPsiBundle
import com.jetbrains.python.psi.PyReturnStatement
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowPythonBlockSection

/**
 * Suppresses the platform's "'return' outside of function" error for `return` statements that live
 * inside a snakemake `run:` section or a top-level python block. Those blocks are compiled by
 * snakemake into the body of a generated function, so `return` is legal there even though the PSI
 * has no enclosing [com.jetbrains.python.psi.PyFunction].
 *
 * Historically this was done by disabling the stock `com.jetbrains.python.validation.ReturnAnnotator`
 * via [SnakemakeVisitorFilter] and re-adding a permissive copy (`SmkReturnAnnotator`). Since 2026.1
 * (build 261) that annotator was removed: the check moved into the `final` `PySyntaxAnnotator`, which
 * `PyCompositeAnnotator` runs unconditionally (it does not consult `PythonVisitorFilter`). So the only
 * remaining place to veto the resulting highlight is a [HighlightInfoFilter].
 *
 * The filter is intentionally narrow: it only drops an ERROR whose message matches the platform's
 * "return outside of function" message AND that sits on a `return` inside a run/python block. A genuine
 * top-level `return` in a `.smk` file (outside any run/python block) is still reported, as before. If a
 * future platform reworks that message the worst case is the false positive reappearing — never a real
 * error being hidden.
 */
class SmkReturnHighlightInfoFilter : HighlightInfoFilter {
    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        if (file !is SmkFile) {
            return true
        }
        if (highlightInfo.severity !== HighlightSeverity.ERROR) {
            return true
        }
        if (highlightInfo.description != PyPsiBundle.message("ANN.return.outside.of.function")) {
            return true
        }

        val element = file.findElementAt(highlightInfo.actualStartOffset) ?: return true
        val returnStatement = PsiTreeUtil.getParentOfType(element, PyReturnStatement::class.java) ?: return true

        val inRunOrPythonBlock = PsiTreeUtil.getParentOfType(
            returnStatement, SmkRunSection::class.java, SmkWorkflowPythonBlockSection::class.java
        ) != null

        // Reject (hide) the highlight only when the 'return' is inside a run/python block.
        return !inRunOrPythonBlock
    }
}
