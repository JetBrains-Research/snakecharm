package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor

abstract class SnakemakeInspectionVisitor(
    holder: ProblemsHolder,
    context: TypeEvalContext,
) : SmkElementVisitor, PyInspectionVisitor(holder, context) {
    override val pyElementVisitor: PyElementVisitor
        get() = this
}