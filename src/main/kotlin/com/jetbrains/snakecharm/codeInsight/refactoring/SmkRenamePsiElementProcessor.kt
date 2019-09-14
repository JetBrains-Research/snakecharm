package com.jetbrains.snakecharm.codeInsight.refactoring

import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import com.jetbrains.snakecharm.lang.psi.SmkSection

class SmkRenamePsiElementProcessor: RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement) = element is SmkSection
    override fun renameElement(
            element: PsiElement,
            newName: String,
            usages: Array<out UsageInfo>,
            listener: RefactoringElementListener?) {
        throw IncorrectOperationException("Section keyword rename not allowed.")
    }
}