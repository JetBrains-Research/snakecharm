package com.jetbrains.snakecharm.codeInsight.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.impl.FakePsiElement
import com.intellij.util.IncorrectOperationException
import javax.swing.Icon

/**
 * Optionally consider RenameableFakePsiElement superclass
 */
open class SmkFakePsiElement(
        private val myParent: PsiElement,
        private val myName: String,
        private val myIcon: Icon?
): FakePsiElement(), PsiNamedElement {
    override fun getName() = myName
    override fun setName(name: String): PsiElement {
        throw  IncorrectOperationException()
    }

    override fun getIcon(open: Boolean) = myIcon

    override fun getParent(): PsiElement  = myParent
    override fun getNavigationElement() = parent

    override fun getTextOffset() = parent.textOffset
    override fun getTextLength() = parent.textLength

}