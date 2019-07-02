package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.completion.PythonLookupElement
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes.RULE_LIKE
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakecharm.lang.psi.SMKRuleRunParameter

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-23
 */
class SMKKeywordCompletionContributor: CompletionContributor() {
    companion object {
        val IN_SNAKEMAKE = PlatformPatterns.psiFile().withLanguage(SnakemakeLanguageDialect)
        val IN_RULE = psiElement().inside(SMKRule::class.java)!!
    }

    init {
        extend(
                CompletionType.BASIC,
                WorkflowTopLevelKeywordsProvider.CAPTURE,
                WorkflowTopLevelKeywordsProvider
        )

        extend(
                CompletionType.BASIC,
                RuleSectionKeywordsProvider.CAPTURE,
                RuleSectionKeywordsProvider
        )
      }
}

object WorkflowTopLevelKeywordsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = psiElement()
            .inFile(SMKKeywordCompletionContributor.IN_SNAKEMAKE)
            .andNot(SMKKeywordCompletionContributor.IN_RULE)

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        val tokenType2Name = SnakemakeLexer.KEYWORDS
                .map { (k, v) -> v to k }
                .toMap()

        listOf(
                WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE to ColonAndWhiteSpaceTail,
                RULE_LIKE to TailType.SPACE
        ).forEach { (tokenSet, tail) ->
            tokenSet.types.forEach { tt ->
                val s = tokenType2Name[tt]!!
                val lookupElement = PythonLookupElement(s, true, null)

                result.addElement(TailTypeDecorator.withTail(lookupElement, tail))
            }
        }
    }
}

object ColonAndWhiteSpaceTail : TailType() {
    // TODO: TailType.CASE_COLON instead of ColonAndWhiteSpaceTail + fix formatter options?

    override fun processTail(editor: Editor, tailOffset: Int): Int {
        val iterator = (editor as EditorEx).highlighter.createIterator(tailOffset)
        if (!iterator.atEnd() && iterator.tokenType === PyTokenTypes.COLON) {
            iterator.advance()
            if (!iterator.atEnd() && iterator.tokenType === PyTokenTypes.COLON) {
                iterator.advance()
            }
            // only move caret
            return moveCaret(editor, tailOffset, iterator.start - tailOffset + 1)
        }

        // insert
        editor.document.insertString(tailOffset, ": ")

        return moveCaret(editor, tailOffset, 2)
    }
}

object RuleSectionKeywordsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = psiElement()
            .inFile(SMKKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(SMKRule::class.java)
            .andNot(psiElement().inside(PyArgumentList::class.java))

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        (SMKRuleParameterListStatement.PARAMS_NAMES + setOf(SMKRuleRunParameter.PARAM_NAME)).forEach { s ->
            result.addElement(TailTypeDecorator.withTail(
                    PythonLookupElement(s, true, PlatformIcons.PROPERTY_ICON),
                    ColonAndWhiteSpaceTail
            ))
        }
    }
}