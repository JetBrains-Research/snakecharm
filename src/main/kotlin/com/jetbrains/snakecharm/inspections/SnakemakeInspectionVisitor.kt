package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor

abstract class SnakemakeInspectionVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
) : SmkElementVisitor, PyInspectionVisitor(holder, session) {
    override val pyElementVisitor: PyElementVisitor
        get() = this
}