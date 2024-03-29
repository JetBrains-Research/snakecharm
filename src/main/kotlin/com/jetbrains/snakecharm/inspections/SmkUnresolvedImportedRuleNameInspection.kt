package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkModule
import com.jetbrains.snakecharm.lang.psi.SmkUse
import com.jetbrains.snakecharm.lang.psi.SmkUseNewNamePattern
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType

class SmkUnresolvedImportedRuleNameInspection : SnakemakeInspection() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkUse(use: SmkUse) {
            val references = use.getDefinedReferencesOfImportedRuleNames() ?: return
            val moduleRef = use.getModuleName()?.reference
            if (moduleRef != null) {
                // If there are 'from' construction
                // And module doesn't refer to local
                // Highlight rule references
                val module = moduleRef.resolve() as? SmkModule
                val importedFile = module?.getPsiFile() as? SmkFile
                if (importedFile == null || importedFile.virtualFile is HttpVirtualFile) {
                    references.forEach {
                        registerProblem(
                            it,
                            SnakemakeBundle.message("INSP.NAME.probably.unresolved.use.reference")
                        )
                    }
                    return
                }
            }
            references.forEach { reference ->
                checkReference(reference)
            }
        }

        override fun visitPyReferenceExpression(node: PyReferenceExpression) {
            val childQualified = node.qualifier ?: return
            val childType = myTypeEvalContext.getType(childQualified)
            if (childType is SmkRulesType) {
                checkReference(node)
            }
        }

        private fun checkReference(reference: PyReferenceExpression) {
            if (reference.reference.resolve() is SmkUseNewNamePattern) {
                registerProblem(
                    reference,
                    SnakemakeBundle.message("INSP.NAME.probably.unresolved.use.reference")
                )
            }
        }
    }
}