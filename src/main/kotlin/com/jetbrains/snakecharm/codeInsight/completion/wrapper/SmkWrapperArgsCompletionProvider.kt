package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfTypes
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.completion.SmkKeywordCompletionContributor
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

object SmkWrapperArgsCompletionProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = PlatformPatterns.psiElement()
            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(SmkRuleOrCheckpointArgsSection::class.java)
            .inside(PyArgumentList::class.java)!!

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        if (parameters.position.elementType != PyTokenTypes.IDENTIFIER) {
            // e.g string literal
            return
        }
        // First ensure, that arg is in args list of section,  not just some other call inside section args expressions
        val containingExpression = parameters.position.parentOfTypes(PyExpression::class) ?: return

        val containingArgList = containingExpression.parentOfTypes(PyArgumentList::class)
        if (containingArgList == null || containingArgList != containingExpression.parent) {
            return
        }

        val containingSection = containingArgList.parentOfTypes(SmkRuleOrCheckpointArgsSection::class)
        if (containingSection == null || containingSection.argumentList != containingArgList) {
            return
        }

        val containingRuleLike = containingSection.parentOfTypes(SmkRuleOrCheckpoint::class)
        val sectionKeyword = containingSection.sectionKeyword ?: return

        val wrapperSection = containingRuleLike?.getSectionByName(SnakemakeNames.SECTION_WRAPPER) ?: return

        val wrappers = SmkWrapperStorage.getInstance(parameters.position.project)?.wrappers ?: return

        val wrapperFstArg = wrapperSection.argumentList?.arguments?.firstOrNull()
        if (wrapperFstArg !is PyStringLiteralExpression) {
            return
        }
        val wrapperNameText = wrapperFstArg.stringValue
        val matchingWrappers = wrappers.filter { wrapperNameText.endsWith(it.path) }
        require(matchingWrappers.size <= 1)
        if (matchingWrappers.size == 1) {
            val wrapperInfo = matchingWrappers.first()
            if (sectionKeyword in wrapperInfo.args.keys) {
                wrapperInfo.args[sectionKeyword]?.forEach {
                    result.addElement(
                        SmkCompletionUtil.createPrioritizedLookupElement(
                            it,
                            null,
                            typeText = SnakemakeBundle.message("TYPES.rule.section.arg.type.text"),
                            priority = SmkCompletionUtil.SECTIONS_ARGS_PRIORITY
                        )
                    )
                }
            }
        }
    }
}