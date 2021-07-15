package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.psi.PsiFile
import com.jetbrains.python.inspections.PyShadowingBuiltinsInspection
import com.jetbrains.python.inspections.PyUnboundLocalVariableInspection
import com.jetbrains.python.inspections.PyUnreachableCodeInspection
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PythonVisitorFilter
import com.jetbrains.python.validation.ReturnAnnotator

/**
 * See also: [com.jetbrains.snakecharm.inspections.SmkIgnorePyInspectionExtension]
 */
class SnakemakeVisitorFilter : PythonVisitorFilter {
    private val unsupportedClasses = listOf(
        /** Instead use [com.jetbrains.snakecharm.lang.validation.SmkReturnAnnotator] **/
        ReturnAnnotator::class.java,
        // [HACK] See https://github.com/JetBrains-Research/snakecharm/issues/14
        PyUnreachableCodeInspection::class.java,
        // TODO: Need API for: PyResolveUtil.allowForwardReferences(node)
        PyUnboundLocalVariableInspection::class.java,
        // See https://github.com/JetBrains-Research/snakecharm/issues/133, API required
        PyShadowingBuiltinsInspection::class.java

// other possible candidates to disable             
//            //inspections
//           PyCallByClassInspection.class,
//           PyCallingNonCallableInspection.class,
//           PyTypeCheckerInspection.class,
//           PyUnboundLocalVariableInspection.class,
//           PyUnusedLocalInspection.class,
//           PyOldStyleClassesInspection.class,
//           PyClassHasNoInitInspection.class,
//           PyArgumentListInspection.class,
//           PyRedeclarationInspection.class,
//           PyShadowingNamesInspection.class,
//           PyMethodMayBeStaticInspection.class,
//           PyNoneFunctionAssignmentInspection.class,
//           PyCompatibilityInspection.class,
//           //annotators
//           ParameterListAnnotator.class,
//           UnsupportedFeatures.class
    )

    override fun isSupported(visitorClass: Class<out PyElementVisitor>, file: PsiFile) = !unsupportedClasses
        .any { unsupportedClass ->
            unsupportedClass === visitorClass
        }
}