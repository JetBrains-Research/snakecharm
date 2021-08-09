package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.vfs.impl.http.HttpVirtualFile
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes

class SmkUnresolvedReferenceInUseSectionInspection : SnakemakeInspection() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkUse(use: SmkUse) {
            val references = use.node.findChildByType(SmkElementTypes.USE_IMPORTED_RULES_NAMES) ?: return
            var file = use.containingFile as SmkFile
            val moduleRef = use.getModuleReference()
            if (moduleRef != null) {
                val module = moduleRef.reference.resolve() as? SmkModule
                val importedFile = module?.getPsiFile() as? SmkFile
                if (importedFile == null || importedFile.virtualFile is HttpVirtualFile) {
                    references.psi.children.forEach {
                        if (it is SmkReferenceExpression) {
                            registerProblem(
                                it,
                                SnakemakeBundle.message("INSP.NAME.probably.unresolved.use.reference"),
                                ProblemHighlightType.WEAK_WARNING
                            )
                        }
                    }
                    return
                }
                file = importedFile
            }
            val uses = file.advancedCollectUseSectionsWithWildcards(mutableSetOf())
            references.psi.children.forEach { reference ->
                if (reference is SmkReferenceExpression && reference.reference.resolve() == null) {
                    if (uses.any { (first) ->
                            val pattern = first.replaceFirst("*", "(?<name>\\w+)").replace("*", "\\k<name>") + '$'
                            pattern.toRegex().matches(reference.text)
                        }) {
                        registerProblem(
                            reference,
                            SnakemakeBundle.message("INSP.NAME.probably.unresolved.use.reference"),
                            ProblemHighlightType.WEAK_WARNING
                        )
                    } else {
                        registerProblem(
                            reference,
                            SnakemakeBundle.message("INSP.NAME.unresolved.use.reference", reference.text),
                            ProblemHighlightType.GENERIC_ERROR
                        )
                    }
                }
            }
        }
    }
}