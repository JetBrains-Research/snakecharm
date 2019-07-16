package com.jetbrains.snakecharm.lang.structureView

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.structureView.PyStructureViewElement
import com.jetbrains.snakecharm.lang.psi.SmkSection

class SmkStructureViewElement: PyStructureViewElement {
    constructor(element: PyElement): super(element)

    private constructor(
            element: PyElement,
            visibility: Visibility,
            inherited: Boolean,
            field: Boolean
    ): super(element, visibility, inherited, field)

    override fun createChild(element: PyElement, visibility: Visibility, inherited: Boolean, field: Boolean) =
            SmkStructureViewElement(element, visibility, inherited, field)

    override fun isWorthyItem(element: PsiElement?, parent: PsiElement?): Boolean {
        if (super.isWorthyItem(element, parent)) {
            return true
        }
        return element is SmkSection
    }

    // TODO: maybe show as fields: configfile, workdir and other properties ?
//    override fun elementIsField(element: PyElement?): Boolean {
//        return super.elementIsField(element) || ...
//    }
}