package com.jetbrains.snakecharm.lang.structureView

import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.openapi.editor.Editor
import com.jetbrains.python.structureView.PyStructureViewModel
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkSection

class SmkStructureViewModel(
        smkFile: SmkFile,
        editor: Editor?
): PyStructureViewModel(smkFile, editor, SmkStructureViewElement(smkFile)) {
    init {
        withSorters(Sorter.ALPHA_SORTER)
        
        // rule like & rule like sections & workflow sections
        withSuitableClasses(SmkSection::class.java)
    }
}