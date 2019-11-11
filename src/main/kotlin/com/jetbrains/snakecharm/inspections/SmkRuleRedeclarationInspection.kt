package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR
import com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.quickfix.RenameElementWithoutUsagesQuickFix
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndex
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType

class SmkRuleRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val localRules by lazy {
            holder.file.let { psiFile ->
                when (psiFile) {
                    is SmkFile -> {
                        psiFile.collectRules().map { it.second }
                    }
                    else -> emptyList()
                }
            }
        }
        private val localCheckpoints by lazy {
            holder.file.let { psiFile ->
                when (psiFile) {
                    is SmkFile -> {
                        psiFile.collectCheckPoints().map { it.second }
                    }
                    else -> emptyList()
                }
            }
        }

        override fun visitSmkRule(rule: SmkRule) {
            visitSMKRuleLike(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            visitSMKRuleLike(checkPoint)
        }

        private fun visitSMKRuleLike(ruleLike: SmkRuleLike<SmkRuleOrCheckpointArgsSection>) {
            val containingFile = ruleLike.containingFile
            val nameToCheck = ruleLike.name ?: return

            val ruleResolveResults = AbstractSmkRuleOrCheckpointType.findAvailableRuleLikeElementByName(
                ruleLike, nameToCheck, SmkRuleNameIndex.KEY, SmkRule::class.java
            ) { localRules }

            val cpResolveResults = AbstractSmkRuleOrCheckpointType.findAvailableRuleLikeElementByName(
                ruleLike, nameToCheck, SmkCheckpointNameIndex.KEY, SmkCheckPoint::class.java
            ) { localCheckpoints }

            val resolveResults = ruleResolveResults + cpResolveResults

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
                            isTopLevelDeclaration -> SnakemakeBundle.message("INSP.NAME.rule.redeclaration") to GENERIC_ERROR
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
            ruleLike: SmkRuleLike<SmkRuleOrCheckpointArgsSection>,
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