package com.jetbrains.snakecharm.lang.validation

import com.intellij.lang.annotation.HighlightSeverity.ERROR
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyStarArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.inspections.quickfix.IntroduceKeywordArgument
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.*

object SmkSyntaxErrorAnnotator : SmkAnnotator() {
    override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
        findAndHighlightIncorrectArguments(st)
    }

    override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
        findAndHighlightIncorrectArguments(st)
    }

    private fun findAndHighlightIncorrectArguments(argsSection: SmkArgsSection) {
        if (!SnakemakeLanguageDialect.isInsideSmkFile(argsSection)) {
            return
        }

        val seenKeywords2Value = HashMap<String, String>()
        var encounteredKeywordArgument = false

        argsSection.argumentList?.arguments?.forEach { arg ->
            when (arg) {
                is PyKeywordArgument -> {
                    @Suppress("UnstableApiUsage")
                    arg.keyword?.let { keyword ->
                        val keywordValue = seenKeywords2Value[keyword]
                        if (keywordValue == null) {
                            // arg value is nullable, let's take arg text which isn't null
                            seenKeywords2Value[keyword] = arg.text
                        } else {
                            holder.newAnnotation(
                                ERROR,
                                SnakemakeBundle.message("ANN.keyword.argument.already.provided", keywordValue)
                            ).range(arg.keywordNode ?: return@let).create()
                        }
                    }
                    encounteredKeywordArgument = true
                }
                !is PyStarArgument -> if (encounteredKeywordArgument) {
                    holder.newAnnotation(
                        ERROR, SnakemakeBundle.message("ANN.positional.argument.after.keyword.argument")
                    ).range(arg).withFix(IntroduceKeywordArgument(arg)).create()
                }
            }
        }
    }

    override fun visitSmkRule(rule: SmkRule) {
        checkMultipleExecutionSections(rule)
    }

    override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
        checkMultipleExecutionSections(checkPoint)
    }

    private fun checkMultipleExecutionSections(ruleOrCheckpoint: SmkRuleOrCheckpoint) {
        var seenExecutionSection: String? = null

        val api = SnakemakeApiService.getInstance(ruleOrCheckpoint.project)
        val sections = ruleOrCheckpoint.getSections()
        for (st in sections) {
            val sectionName = st.sectionKeyword
            val isExecutionSection = sectionName in api.getExecutionSectionsKeyword()

            if (isExecutionSection) {
                if (seenExecutionSection != null) {
                    holder.newAnnotation(
                        ERROR, SnakemakeBundle.message("ANN.multiple.execution.sections", seenExecutionSection)
                    ).range(st).create()
                } else {
                    seenExecutionSection = sectionName
                }
            }
        }
    }
}