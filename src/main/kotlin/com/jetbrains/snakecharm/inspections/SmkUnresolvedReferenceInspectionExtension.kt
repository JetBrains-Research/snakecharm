package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.jetbrains.python.inspections.PyUnresolvedReferenceQuickFixProvider
import com.jetbrains.snakecharm.inspections.quickfix.CreateEnvFile
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkFileReference
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkUnresolvedReferenceInspectionExtension : PyUnresolvedReferenceQuickFixProvider {
    override fun registerQuickFixes(reference: PsiReference, existing: MutableList<LocalQuickFix>) {
        val sec = reference.element as? SmkRuleOrCheckpointArgsSection ?: return
        if (sec.sectionKeyword == SnakemakeNames.SECTION_CONDA) {
            val name = (sec.reference as? SmkFileReference)?.getReferencePath() ?: return
            existing.add(CreateEnvFile(sec, name))
        }
    }
}