package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

open class SmkSLElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), SmkSLElement