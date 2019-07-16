package com.jetbrains.snakecharm.lang.structureView

import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.jetbrains.snakecharm.lang.psi.SmkFile

class SmkStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile) =
            object : TreeBasedStructureViewBuilder() {
                override fun createStructureViewModel(editor: Editor?) =
                        SmkStructureViewModel(psiFile as SmkFile, editor)
            }
}
