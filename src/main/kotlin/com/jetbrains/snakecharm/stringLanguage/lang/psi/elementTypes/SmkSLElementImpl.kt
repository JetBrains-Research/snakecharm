package com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.Language

open class SmkSLElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), SmkSLElement