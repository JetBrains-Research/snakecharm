package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.PsiReference
import com.jetbrains.python.inspections.PyUnresolvedReferenceQuickFixProvider
import com.jetbrains.snakecharm.inspections.quickfix.CreateMissedFileQuickFix
import com.jetbrains.snakecharm.inspections.quickfix.CreateMissedFileUndoableAction
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkFileReference

class SmkUnresolvedReferenceQuickFixProvider : PyUnresolvedReferenceQuickFixProvider {

    override fun registerQuickFixes(reference: PsiReference, existing: MutableList<LocalQuickFix>) {
        val section = reference.element as? SmkArgsSection ?: return
        val sectionName = section.sectionKeyword ?: return
        if (CreateMissedFileUndoableAction.sectionToDefaultFileContent.containsKey(sectionName)) {
            val fileReference = (section.reference as? SmkFileReference) ?: return
            if (!fileReference.hasAppropriateSuffix()) {
                return
            }
            val name = fileReference.path
            existing.add(CreateMissedFileQuickFix(section,
                    name,
                    sectionName,
                    fileReference.searchRelativelyToCurrentFolder))
        }
    }
}