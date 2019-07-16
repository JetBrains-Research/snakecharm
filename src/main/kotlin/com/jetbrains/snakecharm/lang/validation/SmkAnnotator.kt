package com.jetbrains.snakecharm.lang.validation

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.validation.PyAnnotator
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-09
 */
abstract class SmkAnnotator: PyAnnotator(), SmkElementVisitor {
    override val pyElementVisitor: PyElementVisitor
        get() = this
}
