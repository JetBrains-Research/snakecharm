package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor

abstract class SmkSLInspectionVisitor(
    holder: ProblemsHolder,
    context: TypeEvalContext,
) : SmkSLElementVisitor, PyInspectionVisitor(holder, context) {
    override val pyElementVisitor: PyElementVisitor
        get() = this
}