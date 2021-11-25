package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.jetbrains.python.inspections.PyUnresolvedReferenceQuickFixProvider
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.inspections.quickfix.CreateEnvFile
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkUnresolvedReferenceInspectionExtension : PyUnresolvedReferenceQuickFixProvider {
    override fun registerQuickFixes(reference: PsiReference, existing: MutableList<LocalQuickFix>) {
        val sec = reference.element as? SmkRuleOrCheckpointArgsSection ?: return
        if (sec.sectionKeyword == SnakemakeNames.SECTION_CONDA) {
            handleCondaSection(sec, existing)
        }
    }

    private fun handleCondaSection(st: SmkRuleOrCheckpointArgsSection, existing: MutableList<LocalQuickFix>) {
        val arg = st.argumentList?.arguments
            ?.firstOrNull { it is PyStringLiteralExpression }
                as? PyStringLiteralExpression ?: return
        val name = arg.text.replace("\"", "")
        existing.add(CreateEnvFile(st, name))
    }
}