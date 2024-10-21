package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.TailTypes
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
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
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.SUBWORKFLOW_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.USE_DECLARATION_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.SnakemakeApiYamlAnnotationsService
import com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations.SmkApiAnnotationParsingContextType
import com.jetbrains.snakecharm.lang.SmkLanguageVersion
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.RULE_LIKE
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkUseArgsSectionImpl

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-23
 */
class SmkKeywordCompletionContributor : CompletionContributor() {
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
        .inFile(SmkCompletionContributorPattern.IN_SNAKEMAKE)
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

        val project = parameters.position.project

        // use all possible keywords including deprecations and not yet introduced sections, will be filtered later
        val api = SnakemakeApiService.getInstance(project)

        val tokenType2Name = SnakemakeLexer.KEYWORD_LIKE_SECTION_TOKEN_TYPE_2_KEYWORD
        val colonAndWhiteSpaceTailKeys = WORKFLOW_TOPLEVEL_DECORATORS_WO_RULE_LIKE.types.mapNotNull { tt ->
            tokenType2Name[tt]
        } + api.getAllPossibleToplevelArgsSectionKeywords()
        val spaceTailKeys = RULE_LIKE.types.map { tt ->
            tokenType2Name[tt]!!
        }

        // top-level
        listOf(
            colonAndWhiteSpaceTailKeys to ColonAndWhiteSpaceTail,
            spaceTailKeys to TailTypes.spaceType(),
        ).forEach { (tokenSet, tail) ->

            filterByDeprecationAndAddLookupItems(project, tokenSet, result, isTopLevel=true,
                customTailTypes = tokenSet.filter { it == SnakemakeNames.USE_KEYWORD}.associate { it to RuleKeywordTail},
                defaultTailType = tail,
                priority = SmkCompletionUtil.KEYWORDS_PRIORITY) {
                SmkApiAnnotationParsingContextType.TOP_LEVEL.typeStr
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
        .inFile(SmkCompletionContributorPattern.IN_SNAKEMAKE)
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
        val element = parameters.position

        // use all possible keywords including deprecations and not yet introduced sections, will be filtered later
        val api = SnakemakeApiService.getInstance(element.project)
        val keywords = api.getAllPossibleRuleOrCheckpointArgsSectionKeywords()

        filterByDeprecationAndAddLookupItems(element.project, keywords, result, priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY){
            val smkRuleOrCheckpoint = PsiTreeUtil.getParentOfType(element, SmkRuleOrCheckpoint::class.java)
            requireNotNull(smkRuleOrCheckpoint) {
                "According to CAPTURE should be inside rule or checkpoint: <${element.text}> at ${element.textRange}"
            }
            smkRuleOrCheckpoint.sectionKeyword
        }
    }
}

object SubworkflowSectionKeywordsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = psiElement()
        .inFile(SmkCompletionContributorPattern.IN_SNAKEMAKE)
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
        .inFile(SmkCompletionContributorPattern.IN_SNAKEMAKE)
        .inside(psiElement().inside(SmkModule::class.java))
        .andNot(
            psiElement().insideOneOf(PyArgumentList::class.java, PsiComment::class.java)
        )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val element = parameters.position

        // use all possible keywords including deprecations and not yet introduced sections, will be filtered later
        val api = SnakemakeApiService.getInstance(element.project)
        val keywords = api.getAllPossibleModuleSectionKeywords()

        filterByDeprecationAndAddLookupItems(element.project, keywords, result, priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY) {
            val smkModule = PsiTreeUtil.getParentOfType(element, SmkModule::class.java)
            requireNotNull(smkModule) {
                "According to CAPTURE should be inside module: <${element.text}> at ${element.textRange}"
            }
            smkModule.sectionKeyword
        }
    }
}

object UseSectionKeywordsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = psiElement()
        .inFile(SmkCompletionContributorPattern.IN_SNAKEMAKE)
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
            val tail = if (s != SnakemakeNames.SMK_WITH_KEYWORD) TailTypes.spaceType() else TailTypes.caseColonType()
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

        // use all possible keywords including deprecations and not yet introduced sections, will be filtered later
        val api = SnakemakeApiService.getInstance(parameters.position.project)
        val keywords = api.getAllPossibleUseSectionKeywordsIncludingExecSections() - api.getExecutionSectionsKeyword()

        filterByDeprecationAndAddLookupItems(
            parameters.position.project,
            keywords,
            result,
            priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
        ) {
            SnakemakeNames.USE_KEYWORD
        }
    }
}

private fun filterByDeprecationAndAddLookupItems(
    project: Project,
    candidateKeywords: Iterable<String>,
    result: CompletionResultSet,
    priority: Double,
    isTopLevel: Boolean = false,
    customTailTypes: Map<String, TailType>? = null,
    defaultTailType: TailType = ColonAndWhiteSpaceTail,
    parentContextProvider: () -> String?
) {
    val deprecationProvider = SnakemakeApiYamlAnnotationsService.getInstance()
    val settings = SmkSupportProjectSettings.getInstance(project)
    val contextName = parentContextProvider()

    candidateKeywords.forEach { s ->
        val versionInfo: String?
        if (contextName != null) {
            val introducedVersion = when {
                isTopLevel -> deprecationProvider.getTopLevelIntroductionVersion(s)
                else -> deprecationProvider.getSubSectionIntroductionVersion(s, contextName)
            }
            val deprecatedVersion = when {
                isTopLevel -> deprecationProvider.getTopLevelDeprecationVersion(s)
                else -> deprecationProvider.getSubSectionDeprecationVersion(s, contextName)
            }
            val removedVersion = when {
                isTopLevel -> deprecationProvider.getTopLevelRemovedVersion(s)
                else -> deprecationProvider.getSubSectionRemovalVersion(s, contextName)
            }
            val currentVersionString = settings.snakemakeLanguageVersion
            val currentVersion = if (currentVersionString == null) null else SmkLanguageVersion(currentVersionString)

            val versAndParams = currentVersion?.let {
                when {
                    isTopLevel -> deprecationProvider.getTopLevelDeprecation(s, it)
                    else -> deprecationProvider.getSubsectionDeprecation(s, it, contextName)
                }
            }
            if (versAndParams?.second?.itemRemoved == true) {
                // removed in the current version
                return@forEach
            }

            // version info:
            val buf = StringBuffer()
            if (introducedVersion != null) {
                buf.append(">=${introducedVersion}")
            }
            if (deprecatedVersion != null) {
                if (buf.isNotEmpty()) {
                    buf.append(", ")
                }
                buf.append("deprecated $deprecatedVersion")
            }
            if (removedVersion != null) {
                if (buf.isNotEmpty()) {
                    buf.append(", ")
                }
                buf.append("removed $removedVersion")
            }
            versionInfo = if (buf.isEmpty()) null else buf.toString()
        } else {
            versionInfo = null
        }

        var tailType = defaultTailType
        if (customTailTypes != null && s in customTailTypes.keys) {
            tailType = customTailTypes[s]!!
        }
        result.addElement(
            SmkCompletionUtil.createPrioritizedLookupElement(
                TailTypeDecorator.withTail(
                    PythonLookupElement(s, null, versionInfo, true, PlatformIcons.PROPERTY_ICON, null),
                    tailType
                ),
                priority = priority
            )
        )
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
)!!