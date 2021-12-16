package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.jetbrains.python.inspections.PyUnresolvedReferenceQuickFixProvider
import com.jetbrains.snakecharm.inspections.quickfix.CreateMissedFile
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkFileReference

class SmkUnresolvedReferenceInspectionExtension : PyUnresolvedReferenceQuickFixProvider {

    override fun registerQuickFixes(reference: PsiReference, existing: MutableList<LocalQuickFix>) {
        val section = reference.element as? SmkArgsSection ?: return
        val sectionName = section.sectionKeyword ?: return
        if (CreateMissedFile.supportedSections.containsKey(sectionName)) {
            val fileReference = (section.reference as? SmkFileReference) ?: return
            if (!fileReference.hasAppropriateSuffix()) {
                return
            }
            val name = fileReference.path
            existing.add(CreateMissedFile(section, name, sectionName, fileReference.searchRelativelyToCurrentFolder))
        }
    }
}