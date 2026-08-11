package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.validation.PyAnnotationHolder
import com.jetbrains.snakecharm.lang.validation.SmkAnnotatorBase
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor

abstract class AbstractSmkSLAnnotator(holder: PyAnnotationHolder) :
    SmkAnnotatorBase(holder), SmkSLElementVisitor {
    override val pyElementVisitor: PyElementVisitor
        get() = this
}
