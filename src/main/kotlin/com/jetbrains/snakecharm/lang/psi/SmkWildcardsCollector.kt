package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.FUNCTIONS_BANNED_FOR_WILDCARDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_DEFINING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.stringLanguage.lang.callSimpleName
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLFile
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl

/**
 * For containers which includes:
 *  * rule, checkpoint, sections - collects wildcards in sections according to settings in constructor
 *  * call expression - ignore some methods, which don't introduce wildcards, e.g. 'expand'
 *  * string literals - collect all injections
 *
 *  @param visitDefiningSections Visit or not downstream sections which introduces new wildcards
 *  @param visitExpandingSections Visit or not downstream sections which allows wildcards usage w/o 'wildcards.' prefix
 *  @param visitAllSections If true visit all sections (including run) ignoring [visitDefiningSections] and [visitExpandingSections] flags
 *  @param visitedSections Overridden sections, which were already visited ('use rule' case)
 */
class SmkWildcardsCollector(
    private val visitDefiningSections: Boolean,
    private val visitExpandingSections: Boolean,
    private val visitAllSections: Boolean = false,
    private val visitedSections: MutableSet<String> = mutableSetOf()
) : SmkElementVisitor, PyRecursiveElementVisitor() {
    private val wildcardsElements = mutableListOf<WildcardDescriptor>()
    private var atLeastOneInjectionVisited = false
    private var atLeastOneSectionIgnored = false
    private var currentSectionIdx: Byte = -1
    private var currentSectionName: String? = null

    /**
     * @return List of all wildcard element usages and its names or null if no string literals were found
     *
     * No string literals found means that code doesn't contain wildcards injections and even candidate places
     * for them so user has not only zero wildcards, but now possible wildcards declarations
     */
    fun getWildcards(): List<WildcardDescriptor>? =
        if (atLeastOneInjectionVisited || atLeastOneSectionIgnored) wildcardsElements else null

    fun getWildcardsNames() = getWildcards()?.asSequence()?.map { it.text }?.distinct()?.toList()

    override val pyElementVisitor: PyElementVisitor
        get() = this

    override fun visitSmkRunSection(st: SmkRunSection) {
        if (visitAllSections) {
            currentSectionName = SnakemakeNames.SECTION_RUN
            super.visitSmkRunSection(st)
        }
    }

    override fun visitPyLambdaExpression(node: PyLambdaExpression) {
        // #249 do not collect wildcards in lambdas
        // Do nothing here
    }

    override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
        try {
            currentSectionIdx = WILDCARDS_DEFINING_SECTIONS_KEYWORDS.indexOf(st.sectionKeyword).toByte()

            if (
                visitAllSections ||
                (visitDefiningSections && st.isWildcardsDefiningSection()) ||
                (visitExpandingSections && st.isWildcardsExpandingSection())
            ) {
                if (st.sectionKeyword !in visitedSections) {
                    currentSectionName = st.sectionKeyword
                    visitedSections.add(st.sectionKeyword ?: return)
                    super.visitSmkRuleOrCheckpointArgsSection(st)
                } else {
                    atLeastOneSectionIgnored = true
                }
                return
            }
        } finally {
            currentSectionIdx = -1
        }
    }

    override fun visitPyCallExpression(node: PyCallExpression) {   //format
        if (node.callSimpleName() !in FUNCTIONS_BANNED_FOR_WILDCARDS) {
            super.visitPyCallExpression(node)
        }
    }

    override fun visitPyStringLiteralExpression(stringLiteral: PyStringLiteralExpression) {
        val languageManager = InjectedLanguageManager.getInstance(stringLiteral.project)
        val injectedFiles =
            languageManager.getInjectedPsiFiles(stringLiteral)
                ?.map { it.first }
                ?.filterIsInstance<SmkSLFile>()
                ?: return

        atLeastOneInjectionVisited = atLeastOneInjectionVisited || injectedFiles.isNotEmpty()
        injectedFiles.forEach { collectWildcardsNames(it) }
    }

    private fun collectWildcardsNames(file: SmkSLFile) {

        val injections = PsiTreeUtil.getChildrenOfType(file, SmkSLReferenceExpressionImpl::class.java)
        injections?.forEach { st ->
            wildcardsElements.add(
                WildcardDescriptor(
                    st, st.text,
                    if (currentSectionIdx == (-1).toByte()) WildcardDescriptor.UNDEFINED_SECTION_RATE else currentSectionIdx,
                    currentSectionName
                )
            )
        }
    }
}

data class WildcardDescriptor(
    val psi: SmkSLReferenceExpressionImpl,
    val text: String,
    val definingSectionRate: Byte,
    val sectionName: String?
) {
    companion object {
        const val UNDEFINED_SECTION_RATE: Byte = Byte.MAX_VALUE
    }
}

class AdvanceWildcardsCollector(
    private val visitDefiningSections: Boolean,
    private val visitExpandingSections: Boolean,
    private val visitAllSections: Boolean = false,
    private val collectDescriptors: Boolean = false,
    private val ruleLike: SmkRuleOrCheckpoint,
    private val cachedWildcardsByRule: HashMap<SmkRuleOrCheckpoint, Ref<List<String>>>?
) {
    private val visitedWildcardDefinitionSections = mutableSetOf<String>()
    private val wildcardsElementsDescriptors = mutableSetOf<WildcardDescriptor>()
    private val collectedWildcards = mutableSetOf<String>()
    private var wildcardsCollectedInAllOverriddenRules = true

    fun wildcardsCollectedInAllOverriddenRules() = wildcardsCollectedInAllOverriddenRules

    fun getDefinedWildcardDescriptors(): Set<WildcardDescriptor> {
        if (collectedWildcards.isEmpty()) {
            getDefinedWildcards()
        }

        return wildcardsElementsDescriptors
    }

    fun getDefinedWildcards(): Set<String> {
        if (collectedWildcards.isEmpty()) {
            checkWildcardsAndUpdateThem(ruleLike, collectedWildcards, visitedWildcardDefinitionSections)
            if (ruleLike is SmkUse) {
                getWildcardsIntersection(ruleLike, collectedWildcards, visitedWildcardDefinitionSections)
            }
        }

        return collectedWildcards
    }

    private fun updateInfo(
        ruleOrCheckpoint: SmkRuleOrCheckpoint,
        visitedSections: MutableSet<String>
    ) {
        val wildcardsDefiningSectionsAvailable = ruleOrCheckpoint.getSections()
            .asSequence()
            .filterIsInstance(SmkRuleOrCheckpointArgsSection::class.java)
            .filter { it.isWildcardsDefiningSection() }.firstOrNull() != null
        val collector = SmkWildcardsCollector(
            visitDefiningSections = visitDefiningSections,
            visitExpandingSections = visitExpandingSections,
            visitAllSections = visitAllSections,
            visitedSections = visitedSections
        )
        ruleOrCheckpoint.accept(collector)
        if (collectDescriptors) {
            collector.getWildcards()?.apply {
                wildcardsElementsDescriptors.addAll(this)
            }
        }
        if (cachedWildcardsByRule == null) {
            return
        }
        val wildcards = when {
            // Case 1: searching for defining section, but there is no defining section
            visitDefiningSections && !visitExpandingSections && !wildcardsDefiningSectionsAvailable -> emptyList()
            // Case 2: there is at least one defining section
            // It is null, if no wildcards in such sections
            else -> collector.getWildcardsNames()
        }
        cachedWildcardsByRule[ruleOrCheckpoint] = Ref.create(wildcards)
    }

    private fun collectWildcards(
        ruleLike: SmkReferenceExpression,
        set: MutableSet<String>,
        visitedSections: MutableSet<String>
    ) {
        var resolveResult = ruleLike.reference.resolve()
        while (resolveResult is SmkReferenceExpression) {
            val newUseRule = resolveResult.parentOfType<SmkUse>() ?: return
            checkWildcardsAndUpdateThem(newUseRule, set, visitedSections)
            resolveResult = resolveResult.reference.resolve()
        }
        if (resolveResult !is SmkUse && resolveResult is SmkRuleOrCheckpoint) {
            checkWildcardsAndUpdateThem(resolveResult, set, visitedSections)
        } else {
            val newUseRule =
                (resolveResult as? SmkUse) ?: resolveResult?.parentOfType() ?: return
            checkWildcardsAndUpdateThem(newUseRule, set, visitedSections)
            getWildcardsIntersection(newUseRule, set, visitedSections)
        }
    }

    private fun getWildcardsIntersection(
        use: SmkUse,
        list: MutableSet<String>,
        visitedSections: MutableSet<String>
    ) {
        val wildcardsLists = use.getImportedRuleNames()
            ?.map {
                val listOfWildcards = mutableSetOf<String>()
                collectWildcards(it, listOfWildcards, visitedSections.toMutableSet())
                listOfWildcards
            }
        var intersectiion = wildcardsLists?.firstOrNull()?.toSet() ?: return
        for (listOfWildcards in wildcardsLists) {
            intersectiion = intersectiion intersect listOfWildcards
        }
        list.addAll(intersectiion)
    }

    private fun checkWildcardsAndUpdateThem(
        ruleLike: SmkRuleOrCheckpoint,
        list: MutableSet<String>,
        visitedSections: MutableSet<String>
    ) {
        if (collectDescriptors || (cachedWildcardsByRule != null && ruleLike !in cachedWildcardsByRule)) {
            updateInfo(ruleLike, visitedSections)
        }
        if (cachedWildcardsByRule == null) {
            return
        }
        val wildcards = cachedWildcardsByRule.getValue(ruleLike).get()
        if (wildcards != null) {
            list.addAll(wildcards)
        } else {
            wildcardsCollectedInAllOverriddenRules = false
        }
    }
}