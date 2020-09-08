package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.validation.PyAnnotator
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor

abstract class AbstractSmkSLAnnotator : PyAnnotator(), SmkSLElementVisitor {
    override val pyElementVisitor: PyElementVisitor
        get() = this
}