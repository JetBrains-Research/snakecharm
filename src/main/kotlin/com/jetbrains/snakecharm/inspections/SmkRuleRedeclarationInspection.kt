package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.quickfix.RenameElementWithoutUsagesQuickFix
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndexCompanion
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndexCompanion
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseNameIndexCompanion
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType

class SmkRuleRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        private val localRules by lazy {
            holder.file.let { psiFile ->
                when (psiFile) {
                    is SmkFile -> {
                        psiFile.filterRulesPsi().map { it.second }
                    }
                    else -> emptyList()
                }
            }
        }
        private val localCheckpoints by lazy {
            holder.file.let { psiFile ->
                when (psiFile) {
                    is SmkFile -> {
                        psiFile.filterCheckPointsPsi().map { it.second }
                    }
                    else -> emptyList()
                }
            }
        }
        private val localUses by lazy {
            holder.file.let { psiFile ->
                when (psiFile) {
                    is SmkFile -> {
                        psiFile.filterUsePsi().map { it.second }
                    }
                    else -> emptyList()
                }
            }
        }

        override fun visitSmkRule(rule: SmkRule) {
            visitSmkRuleLike(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            visitSmkRuleLike(checkPoint)
        }

        override fun visitSmkUse(use: SmkUse) {
            visitSmkRuleLike(use)
        }

        private fun visitSmkRuleLike(ruleLike: SmkRuleLike<SmkRuleOrCheckpointArgsSection>) {
            val containingFile = ruleLike.containingFile
            val nameToCheck = ruleLike.name ?: return

            val ruleResolveResults = AbstractSmkRuleOrCheckpointType.findAvailableRuleLikeElementByName(
                ruleLike, nameToCheck, SmkRuleNameIndexCompanion.KEY, SmkRule::class.java
            ) { localRules }

            val cpResolveResults = AbstractSmkRuleOrCheckpointType.findAvailableRuleLikeElementByName(
                ruleLike, nameToCheck, SmkCheckpointNameIndexCompanion.KEY, SmkCheckPoint::class.java
            ) { localCheckpoints }

            val usesResolveResults: Collection<PsiElement> =
                AbstractSmkRuleOrCheckpointType.findAvailableRuleLikeElementByName(
                    ruleLike, nameToCheck, SmkUseNameIndexCompanion.KEY, SmkUse::class.java
                ) { localUses }

            val resolveResults = ruleResolveResults + cpResolveResults + usesResolveResults

            if (resolveResults.isEmpty()) {
                return
            }

            if (resolveResults.size == 1 && resolveResults.single() == ruleLike) {
                return
            }

            var redeclaredInSomeOtherFile = false
            val textOffset = ruleLike.textOffset
            resolveResults.forEach { res ->
                if (res.containingFile == containingFile) {
                    if (res.textOffset < textOffset) {
                        val isTopLevelDeclaration = PsiTreeUtil.getParentOfType(
                            ruleLike, PyStatement::class.java, true, SmkFile::class.java
                        ) == null

                        val (msg, severity) = when {
                            isTopLevelDeclaration -> SnakemakeBundle.message("INSP.NAME.rule.redeclaration") to GENERIC_ERROR_OR_WARNING
                            else -> SnakemakeBundle.message("INSP.NAME.rule.redeclaration.possible") to WEAK_WARNING
                        }
                        registerProblem(ruleLike, msg, severity)
                        return
                    }
                } else {
                    redeclaredInSomeOtherFile = true
                }
            }

            if (redeclaredInSomeOtherFile) {
                val msg = SnakemakeBundle.message("INSP.NAME.rule.redeclaration.other.file.possible")
                registerProblem(ruleLike, msg, WEAK_WARNING)
            }
        }

        private fun registerProblem(
            ruleLike: SmkSection,
            msg: String,
            severity: ProblemHighlightType
        ) {
            val problemElement = ruleLike.nameIdentifier ?: return

            registerProblem(
                problemElement, msg, severity, null,
                RenameElementWithoutUsagesQuickFix(
                    ruleLike,
                    problemElement.textRangeInParent.startOffset,
                    problemElement.textRangeInParent.endOffset
                )
            )
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.rule.redeclaration")
}