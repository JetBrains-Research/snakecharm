package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.snakecharm.lang.psi.SnakemakeVisitor

abstract class SnakemakeInspectionVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
) : SnakemakeVisitor, PyInspectionVisitor(holder, session)