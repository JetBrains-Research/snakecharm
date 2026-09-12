package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.psi.PsiFile
import com.jetbrains.python.inspections.PyShadowingBuiltinsInspection
import com.jetbrains.python.inspections.PyUnboundLocalVariableInspection
import com.jetbrains.python.inspections.PyUnreachableCodeInspection
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PythonVisitorFilter

/**
 * See also: [com.jetbrains.snakecharm.inspections.SmkIgnorePyInspectionExtension]
 *
 * Note: the "'return' outside of function" check used to be a standalone, filterable
 * `com.jetbrains.python.validation.ReturnAnnotator`. Since 2026.1 (build 261) it is folded into the
 * final `PySyntaxAnnotator`, which is run by `PyCompositeAnnotator` without consulting this filter,
 * so it can no longer be suppressed here. The false positive for snakemake `run:` / python blocks is
 * now handled by [com.jetbrains.snakecharm.lang.highlighter.SmkReturnHighlightInfoFilter] instead.
 */
class SnakemakeVisitorFilter : PythonVisitorFilter {
    private val unsupportedClasses = listOf(
        // [HACK] See https://github.com/JetBrains-Research/snakecharm/issues/14
        PyUnreachableCodeInspection::class.java,
        // TODO: Need API for: e.g. EP in PyResolveUtil.allowForwardReferences(node)
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