package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor

abstract class SmkSLInspectionVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
) : SmkSLElementVisitor, PyInspectionVisitor(holder, session) {
    override val pyElementVisitor: PyElementVisitor
        get() = this
}