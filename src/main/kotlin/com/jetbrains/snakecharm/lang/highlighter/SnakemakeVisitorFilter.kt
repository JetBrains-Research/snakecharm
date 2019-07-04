package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.psi.PsiFile
import com.jetbrains.python.inspections.PyUnreachableCodeInspection
import com.jetbrains.python.inspections.PythonVisitorFilter
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.validation.ReturnAnnotator

class SnakemakeVisitorFilter: PythonVisitorFilter {
    private val unsupportedClasses = listOf(
            /** Instead use [com.jetbrains.snakecharm.lang.validation.SmkReturnAnnotator] **/
            ReturnAnnotator::class.java,
            // [HACK] See https://github.com/JetBrains-Research/snakecharm/issues/14
            PyUnreachableCodeInspection::class.java
    )

    override fun isSupported(visitorClass: Class<out PyElementVisitor>, file: PsiFile) = !unsupportedClasses
        .any { unsupportedClass ->
            unsupportedClass === visitorClass
        }
}