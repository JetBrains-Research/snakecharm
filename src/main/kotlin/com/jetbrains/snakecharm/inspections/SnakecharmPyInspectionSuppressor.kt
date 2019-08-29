package com.jetbrains.snakecharm.inspections

import com.intellij.openapi.vfs.impl.http.HttpVirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.python.inspections.PyInspectionsSuppressor
import com.jetbrains.python.psi.PyFile

class SnakecharmPyInspectionSuppressor : PyInspectionsSuppressor() {
    private val suppressedForDownloadedFilesTools = setOf("SpellCheckingInspection")

    override fun isSuppressedFor(element: PsiElement, toolId: String) =
            if (element is PyFile &&
                    element.parent?.virtualFile is HttpVirtualFile &&
                    toolId in suppressedForDownloadedFilesTools) {
                true
            } else {
                super.isSuppressedFor(element, toolId)
            }
}