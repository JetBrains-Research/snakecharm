package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.completion.PythonLookupElement
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.MODULE_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.RULE_OR_CHECKPOINT_SECTION_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SUBWORKFLOW_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.TOPLEVEL_ARGS_SECTION_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.USE_DECLARATION_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.USE_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.RULE_LIKE
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkUseArgsSectionImpl
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-23
 */
class SmkKeywordCompletionContributor : CompletionContributor() {
    companion object {
        val IN_SNAKEMAKE = PlatformPatterns.psiFile().withLanguage(SnakemakeLanguageDialect)
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

        extend(
            CompletionType.BASIC,
            SubworkflowSectionKeywordsProvider.CAPTURE,
            SubworkflowSectionKeywordsProvider
        )

        extend(
            CompletionType.BASIC,
            ModuleSectionKeywordsProvider.CAPTURE,
            ModuleSectionKeywordsProvider
        )

        extend(
            CompletionType.BASIC,
            UseSectionKeywordsProvider.CAPTURE,
            UseSectionKeywordsProvider
        )
    }
}

object RuleKeywordTail : TailType() {
    override fun processTail(editor: Editor, tailOffset: Int): Int {
        editor.document.insertString(tailOffset, " ${SnakemakeNames.RULE_KEYWORD} ")
        return moveCaret(editor, tailOffset, 2 + SnakemakeNames.RULE_KEYWORD.length)
    }
}

object WorkflowTopLevelKeywordsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = psiElement()
        .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
        .andNot(
            psiElement().insideOneOf(
                SmkSection::class.java, PsiComment::class.java
            )
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val expression = PsiTreeUtil.getParentOfType(
            parameters.position, PyExpression::class.java
        ) as? PyReferenceExpression ?: return

        val statement = PsiTreeUtil.getParentOfType(expression, PyStatement::class.java)
        if (statement is PyImportStatementBase) {
            return
        }

        if (statement?.parent !is SmkFile && PsiTreeUtil.getParentOfType(
                statement,
                PyStatementListContainer::class.java
            ) !is PyStatementPart
        ) {
            return
        }

        if (partOfSomeComplexReference(parameters.position)) {
            return
        }

        val tokenType2Name = SnakemakeLexer.KEYWORD_LIKE_SECTION_TOKEN_TYPE_2_KEYWORD
        val colonAndWhiteSpaceTailKeys = WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE.types.mapNotNull { tt ->
            tokenType2Name[tt]
        } + TOPLEVEL_ARGS_SECTION_KEYWORDS
        val spaceTailKeys = RULE_LIKE.types.map { tt ->
            tokenType2Name[tt]!!
        }
        listOf(
            colonAndWhiteSpaceTailKeys to ColonAndWhiteSpaceTail,
            spaceTailKeys to TailType.SPACE,
        ).forEach { (tokenSet, tail) ->
            tokenSet.forEach { s ->
                val modifiedTail = if (s == SnakemakeNames.USE_KEYWORD) RuleKeywordTail else tail
                result.addElement(
                    SmkCompletionUtil.createPrioritizedLookupElement(
                        TailTypeDecorator.withTail(
                            PythonLookupElement(s, true, null), modifiedTail
                        ),
                        SmkCompletionUtil.KEYWORDS_PRIORITY
                    )
                )
            }
        }
    }

    private fun partOfSomeComplexReference(element: PsiElement): Boolean {
        val parent = element.parent
        if (parent is PyReferenceExpression && (parent.firstChild != element || parent.lastChild != element)) {
            return true
        }
        return false
    }
}

object ColonAndWhiteSpaceTail : TailType() {
    // TODO: TailType.CASE_COLON instead of ColonAndWhiteSpaceTail + fix formatter options?

    override fun processTail(editor: Editor, tailOffset: Int): Int {
        val iterator = (editor as EditorEx).highlighter.createIterator(tailOffset)
        // if already ": " after item (e.g. replace completion) => just move caret
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
        .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
        .inside(SmkRuleOrCheckpoint::class.java)!!
        .andNot(
            psiElement().insideOneOf(
                PyArgumentList::class.java,
                SmkRunSection::class.java,
                PsiComment::class.java,
                SmkUseArgsSectionImpl::class.java
            )
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        RULE_OR_CHECKPOINT_SECTION_KEYWORDS.forEach { s ->

            result.addElement(
                SmkCompletionUtil.createPrioritizedLookupElement(
                    TailTypeDecorator.withTail(
                        PythonLookupElement(s, true, PlatformIcons.PROPERTY_ICON),
                        ColonAndWhiteSpaceTail
                    ),
                    priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                )
            )
        }
    }
}

object SubworkflowSectionKeywordsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = psiElement()
        .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
        .inside(psiElement().inside(SmkSubworkflow::class.java))
        .andNot(
            psiElement().insideOneOf(PyArgumentList::class.java, PsiComment::class.java)
        )


    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        SUBWORKFLOW_SECTIONS_KEYWORDS.forEach { s ->
            result.addElement(
                SmkCompletionUtil.createPrioritizedLookupElement(
                    TailTypeDecorator.withTail(
                        PythonLookupElement(s, true, PlatformIcons.PROPERTY_ICON),
                        ColonAndWhiteSpaceTail
                    ),
                    priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                )
            )
        }
    }
}

object ModuleSectionKeywordsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = psiElement()
        .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
        .inside(psiElement().inside(SmkModule::class.java))
        .andNot(
            psiElement().insideOneOf(PyArgumentList::class.java, PsiComment::class.java)
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        MODULE_SECTIONS_KEYWORDS.forEach { s ->
            result.addElement(
                SmkCompletionUtil.createPrioritizedLookupElement(
                    TailTypeDecorator.withTail(
                        PythonLookupElement(s, true, PlatformIcons.PROPERTY_ICON),
                        ColonAndWhiteSpaceTail
                    ),
                    priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                )
            )
        }
    }
}

object UseSectionKeywordsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = psiElement()
        .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
        .inside(psiElement().inside(SmkUse::class.java))
        .andNot(
            psiElement().insideOneOf(PyArgumentList::class.java, PsiComment::class.java)
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        USE_DECLARATION_KEYWORDS.forEach { s ->
            val tail = if (s != SnakemakeNames.SMK_WITH_KEYWORD) TailType.SPACE else TailType.CASE_COLON
            result.addElement(
                SmkCompletionUtil.createPrioritizedLookupElement(
                    TailTypeDecorator.withTail(
                        PythonLookupElement(s, true, PlatformIcons.PROPERTY_ICON),
                        tail
                    ),
                    priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                )
            )
        }

        USE_SECTIONS_KEYWORDS.forEach { s ->
            result.addElement(
                SmkCompletionUtil.createPrioritizedLookupElement(
                    TailTypeDecorator.withTail(
                        PythonLookupElement(s, true, PlatformIcons.PROPERTY_ICON),
                        ColonAndWhiteSpaceTail
                    ),
                    priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                )
            )
        }

        val file = parameters.position.containingFile as? SmkFile
        val data = file?.advancedCollectRules(mutableSetOf())
        data?.forEach { (first, second) ->
            result.addElement(
                AbstractSmkRuleOrCheckpointType.createRuleLikeLookupItem(first, second)
            )
        }
    }
}

fun PsiElementPattern.Capture<PsiElement>.insideOneOf(
    vararg classes: Class<out PsiElement>
) = inside(
    StandardPatterns.or(
        *classes.map {
            StandardPatterns.instanceOf(it)
        }.toTypedArray()
    )
)