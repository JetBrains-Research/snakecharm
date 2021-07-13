package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ex.InspectionProfileModifiableModel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.InspectionProfileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.SmkUnrecognizedSectionInspection

class AddIgnoredElementQuickFix(element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    private val elementName = element.text

    override fun getFamilyName(): String {
        return SnakemakeBundle.message("INSP.NAME.section.unrecognized.ignored.add", elementName)
    }

    override fun getText(): String {
        return SnakemakeBundle.message("INSP.NAME.section.unrecognized.ignored.add", elementName)
    }

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val virtualFile = startElement.containingFile.virtualFile
        if (virtualFile != null) {
            val text = startElement.text
            val inspectionProfileManager = InspectionProfileManager.getInstance(startElement.project)
            val inspectionProfileImpl = inspectionProfileManager.currentProfile
            val model = InspectionProfileModifiableModel(inspectionProfileImpl)
            model.modifyProfile {
                val inspection = it.getUnwrappedTool(
                    SmkUnrecognizedSectionInspection::class.java.simpleName,
                    file
                ) as SmkUnrecognizedSectionInspection
                if (text !in inspection.ignoredItems) {
                    inspection.ignoredItems.add(startElement.text)
                }
            }
            model.commit()
            assert(editor != null)
            TemplateBuilderImpl(startElement).run(editor!!, false)
        }
    }
}