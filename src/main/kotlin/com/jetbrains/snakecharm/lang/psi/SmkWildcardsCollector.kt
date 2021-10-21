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
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression
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
 *  @param collectWildcardLikeReferences If true also collects [SmkSLReferenceExpression]s that are not wildcards
 */
class SmkWildcardsCollector(
    private val visitDefiningSections: Boolean,
    private val visitExpandingSections: Boolean,
    private val visitAllSections: Boolean = false,
    private val collectWildcardLikeReferences: Boolean = false
) : SmkElementVisitor, PyRecursiveElementVisitor() {
    private val wildcardsElements = mutableListOf<WildcardDescriptor>()
    private var atLeastOneInjectionVisited = false
    private var currentSectionIdx: Byte = -1
    private var currentSectionName: String? = null

    /**
     * @return List of all wildcard element usages and its names or null if no string literals were found
     *
     * No string literals found means that code doesn't contain wildcards injections and even candidate places
     * for them so user has not only zero wildcards, but now possible wildcards declarations
     */
    fun getWildcards(): List<WildcardDescriptor>? =
        if (atLeastOneInjectionVisited) wildcardsElements else null

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
                currentSectionName = st.sectionKeyword
                super.visitSmkRuleOrCheckpointArgsSection(st)
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
            // We need to check whether is injection is wildcard because 'output' in:
            // 'shell: "{output}"' is SmkSLReferenceExpressionImpl but not a wildcard
            // We should try to resolve the reference because 'shell: "{wildcards.s}"'
            // Is not a wildcard, but it refers to it
            // On the same time we have to collect other references in some resolve/completion cases
            // (see wildcards_resolve.feature 'Resolve first part of qualified like wildcard references into itself')
            val potentialWildcardLike = if (collectWildcardLikeReferences || st.isWildcard()) st else {
                (st.reference.resolve() as? SmkSLReferenceExpression)?.let { resolveResult ->
                    if (resolveResult.isWildcard()) {
                        resolveResult
                    } else {
                        null
                    }
                }
            }
            if (potentialWildcardLike != null) {
                wildcardsElements.add(
                    WildcardDescriptor(
                        potentialWildcardLike, potentialWildcardLike.text,
                        if (currentSectionIdx == (-1).toByte()) WildcardDescriptor.UNDEFINED_SECTION_RATE else currentSectionIdx,
                        currentSectionName, st.isWildcard()
                    )
                )
            }
        }
    }
}

data class WildcardDescriptor(
    val psi: SmkSLReferenceExpression,
    val text: String,
    val definingSectionRate: Byte,
    val sectionName: String?,
    val realWildcard: Boolean = false
) {
    companion object {
        const val UNDEFINED_SECTION_RATE: Byte = Byte.MAX_VALUE
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WildcardDescriptor

        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }
}

/**
 * Manages collecting of wildcards from [ruleLike]. It is uses [cachedWildcardsByRule] to save and reads wildcards.
 *
 *  @param visitDefiningSections Visits or not downstream sections which introduces new wildcards
 *  @param visitExpandingSections Visits or not downstream sections which allows wildcards usage w/o 'wildcards.' prefix
 *  @param visitAllSections If true visits all sections (including run) ignoring [visitDefiningSections] and [visitExpandingSections] flags
 *  @param getIntersection If true takes intersection of sets of inherited wildcards. Otherwise, takes union of them
 *  @param collectWildcardLikeReferences If true also collects [SmkSLReferenceExpression]s that are not wildcards
 */
class AdvancedWildcardsCollector(
    private val visitDefiningSections: Boolean,
    private val visitExpandingSections: Boolean,
    private val visitAllSections: Boolean = false,
    private val getIntersection: Boolean = true,
    private val ruleLike: SmkRuleOrCheckpoint,
    private val cachedWildcardsByRule: HashMap<SmkRuleOrCheckpoint, Ref<List<WildcardDescriptor>>>?,
    private val collectWildcardLikeReferences: Boolean = false
) {
    private val visitedWildcardDefinitionSections = mutableSetOf<String>()
    private val wildcardsElementsDescriptors = mutableListOf<WildcardDescriptor>()
    private var wildcardsCollectedInAllOverriddenRules = true
    private val visitedRules = mutableSetOf<SmkRuleOrCheckpoint>()

    fun wildcardsCollectedInAllOverriddenRules() = wildcardsCollectedInAllOverriddenRules

    fun getDefinedWildcards(): List<WildcardDescriptor> {
        // Collects wildcards, defined in current rule-like section
        checkWildcardsAndUpdateThem(ruleLike, wildcardsElementsDescriptors, visitedWildcardDefinitionSections)
        if (ruleLike is SmkUse) {
            // Advanced collectIon of wildcards that were inherited
            getWildcardsIntersection(ruleLike, wildcardsElementsDescriptors, visitedWildcardDefinitionSections)
        }

        return wildcardsElementsDescriptors
    }

    private fun collectWildcards(
        ruleLike: SmkReferenceExpression,
        listOfDescriptors: MutableList<WildcardDescriptor>,
        visitedSections: MutableSet<String>
    ) {
        // Trying to resolve the reference
        var resolveResult = ruleLike.reference.resolve()
        while (resolveResult is SmkReferenceExpression) {
            // Collects wildcards in each step of the resolving
            val newUseRule = resolveResult.parentOfType<SmkUse>() ?: return
            checkWildcardsAndUpdateThem(newUseRule, listOfDescriptors, visitedSections)
            resolveResult = resolveResult.reference.resolve()
        }
        if (resolveResult in visitedRules) {
            return
        }
        // Collects wildcards from current section if it just a rule or checkpoint
        if (resolveResult !is SmkUse && resolveResult is SmkRuleOrCheckpoint) {
            checkWildcardsAndUpdateThem(resolveResult, listOfDescriptors, visitedSections)
            return
        }
        // Otherwise, it also produces wildcards collecting of inherited rules
        val newUseRule =
            (resolveResult as? SmkUse) ?: resolveResult?.parentOfType() ?: return
        checkWildcardsAndUpdateThem(newUseRule, listOfDescriptors, visitedSections)
        getWildcardsIntersection(newUseRule, listOfDescriptors, visitedSections)
    }

    private fun getWildcardsIntersection(
        use: SmkUse,
        listOfDescriptors: MutableList<WildcardDescriptor>,
        visitedSections: MutableSet<String>
    ) {
        // Get lists of inherited wildcards
        val wildcardsLists = use.getImportedRuleNames()
            ?.map {
                val listOfWildcards = mutableListOf<WildcardDescriptor>()
                collectWildcards(it, listOfWildcards, visitedSections.toMutableSet())
                listOfWildcards
            }
        var newDescriptors = wildcardsLists?.firstOrNull()?.toSet() ?: return
        if (getIntersection) {
            // Gets intersection in case of inspections
            for (listOfWildcards in wildcardsLists) {
                newDescriptors = newDescriptors intersect listOfWildcards.toSet()
            }
        } else {
            // Gets union in case of resolve and completion
            for (listOfWildcards in wildcardsLists) {
                newDescriptors = newDescriptors union listOfWildcards
            }
        }
        listOfDescriptors.addAll(newDescriptors)
    }

    private fun checkWildcardsAndUpdateThem(
        ruleLike: SmkRuleOrCheckpoint,
        listOfDescriptors: MutableList<WildcardDescriptor>,
        visitedSections: MutableSet<String>
    ) {
        // Collects wildcards from current ruleLike section
        visitedRules.add(ruleLike)
        // Reads wildcards from the cache if possible
        val descriptors = if (cachedWildcardsByRule != null && ruleLike in cachedWildcardsByRule) {
            cachedWildcardsByRule.getValue(ruleLike).get()
        } else {
            // Otherwise, uses WildcardsCollector to gather wildcards
            val wildcardsDefiningSectionsAvailable = ruleLike.getSections()
                .asSequence()
                .filterIsInstance(SmkRuleOrCheckpointArgsSection::class.java)
                .filter { it.isWildcardsDefiningSection() }.firstOrNull() != null
            val collector = SmkWildcardsCollector(
                visitDefiningSections = visitDefiningSections,
                visitExpandingSections = visitExpandingSections,
                visitAllSections = visitAllSections,
                collectWildcardLikeReferences = collectWildcardLikeReferences
            )
            ruleLike.accept(collector)
            if (visitDefiningSections && !visitExpandingSections && !wildcardsDefiningSectionsAvailable) {
                emptyList()
            } else {
                collector.getWildcards().also {
                    if (cachedWildcardsByRule != null) {
                        // Updates cache if possible
                        cachedWildcardsByRule[ruleLike] = Ref.create(it)
                    }
                }
            }
        }
        // Collects only those wildcards which were found in sections, that haven't been visited yet
        descriptors?.filter { it.sectionName !in visitedSections }
            ?.forEach { descriptor ->
                listOfDescriptors.add(descriptor)
                descriptor.sectionName?.also { visitedSections.add(it) }
            }

        if (descriptors == null) {
            // Highlights, that it was impossible to check some sections
            wildcardsCollectedInAllOverriddenRules = false
            return
        }
    }
}