package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi
import com.jetbrains.snakecharm.codeInsight.completion.InUseSectionProvider.Companion.IN_USE_SECTION_PROVIDER_CAPTURE
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionAfterRulesAndCheckpointsObjectProvider.Companion.IN_SMK_RULES_OR_CHECKPOINTS_OBJECT
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionInLocalRulesAndRuleOrderSectionsProvider.Companion.IN_SMK_LOCALRULES_OR_RULEORDER_RULE_NAME_REFERENCE
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndexCompanion
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndexCompanion
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseNameIndexCompanion
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType.Companion.getVariantsFromIndex
import com.jetbrains.snakecharm.stringLanguage.SmkSLanguage

class SmkRulesAndCheckpointsCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            IN_SMK_LOCALRULES_OR_RULEORDER_RULE_NAME_REFERENCE,
            SmkCompletionInLocalRulesAndRuleOrderSectionsProvider()
        )
        extend(
            CompletionType.BASIC,
            IN_SMK_RULES_OR_CHECKPOINTS_OBJECT,
            SmkCompletionAfterRulesAndCheckpointsObjectProvider()
        )
        extend(
            CompletionType.BASIC,
            IN_USE_SECTION_PROVIDER_CAPTURE,
            InUseSectionProvider()
        )
    }
}

private class SmkCompletionInLocalRulesAndRuleOrderSectionsProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        val IN_SMK_LOCALRULES_OR_RULEORDER_RULE_NAME_REFERENCE =
            psiElement()
                .andOr(
                    psiElement().inside(psiElement(SmkWorkflowLocalrulesSection::class.java)),
                    psiElement().inside(psiElement(SmkWorkflowRuleorderSection::class.java))
                )
                .withParent(SmkReferenceExpression::class.java)!!
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val rules = collectVariantsForElement(position, isCheckPoint = false)
        val checkpoints = collectVariantsForElement(position, isCheckPoint = true)
        val elements = rules + checkpoints

        val ruleorderSection = PsiTreeUtil.getParentOfType(position, SmkWorkflowRuleorderSection::class.java)
        val ruleOrderOrLocalRulesSection: SmkArgsSection? =
            (ruleorderSection ?: PsiTreeUtil.getParentOfType(position, SmkWorkflowLocalrulesSection::class.java))

        @Suppress("UnstableApiUsage")
        val references = ruleOrderOrLocalRulesSection?.argumentList?.arguments
            ?.filterIsInstance<SmkReferenceExpression>()
            ?.map { it.name }

        val variants = if (references != null) {
            // filter already mentioned `localrules` and `ruleorder` args
            elements.filterNot { it.first in references }
        } else {
            elements
        }

        addVariantsToCompletionResultSet(variants, parameters, result)
        result.stopHere()
    }
}

private class SmkCompletionAfterRulesAndCheckpointsObjectProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        val IN_SMK_RULES_OR_CHECKPOINTS_OBJECT =
            psiElement()
                .withParent(
                    psiElement(PyReferenceExpression::class.java)
                        .withChild(
                            psiElement().andOr(
                                psiElement().withText(SnakemakeApi.SMK_VARS_CHECKPOINTS),
                                psiElement().withText(SnakemakeApi.SMK_VARS_RULES)
                            )
                        )
                )
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        // position is a leaf psi element, and it's wrapped in a reference, which is what we want to get
        val parentElement = parameters.position.parent

        // get parent reference to `rules.` or `checkpoints.`
        val rulesOrCheckpointsObject = parameters.withPosition(parentElement, parentElement.textOffset)
            .originalPosition
            ?.parent as? PyReferenceExpression ?: return

        @Suppress("UnstableApiUsage")
        val variants = when (rulesOrCheckpointsObject.name) {
            SnakemakeApi.SMK_VARS_RULES -> collectVariantsForElement(
                parameters.position,
                isCheckPoint = false
            ) + collectVariantsForElement(parameters.position, isCheckPoint = true)

            SnakemakeApi.SMK_VARS_CHECKPOINTS -> collectVariantsForElement(parameters.position, isCheckPoint = true)
            else -> return
        }

        // we need to obtain containing rule/checkpoint from the original file
        // which is why we obtain it from an element present both in copy and original file
        val originalContainingRuleOrCheckpoint =
            PsiTreeUtil.getParentOfType(
                parameters.withPosition(parentElement, parentElement.textOffset).originalPosition,
                SmkRuleOrCheckpoint::class.java
            ) ?: getContainingDeclarationOfInjectedElement(parentElement)

        if (!parentElement.isInLanguageInjection() &&
            PsiTreeUtil.getParentOfType(parentElement, SmkRunSection::class.java) == null
        ) {
            addVariantsToCompletionResultSet(
                variants.filterNot { it.second == originalContainingRuleOrCheckpoint },
                parameters,
                result
            )
            result.stopHere()
        } else {
            addVariantsToCompletionResultSet(variants, parameters, result)
            result.stopHere()
        }
    }

    private fun PsiElement.isInLanguageInjection(): Boolean {
        val languageManager = InjectedLanguageManager.getInstance(project)
        return languageManager.getInjectionHost(this) != null
    }

    private fun getContainingDeclarationOfInjectedElement(element: PsiElement): SmkRuleOrCheckpoint? {
        val languageManager = InjectedLanguageManager.getInstance(element.project)
        return PsiTreeUtil.getParentOfType(
            languageManager.getInjectionHost(element),
            SmkRuleOrCheckpoint::class.java
        )
    }
}


private class InUseSectionProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        val IN_USE_SECTION_PROVIDER_CAPTURE = psiElement()
            .inFile(SmkCompletionContributorPattern.IN_SNAKEMAKE)
            .inside(psiElement().inside(SmkUse::class.java))
            .andNot(
                psiElement().insideOneOf(PyStatementList::class.java, PsiComment::class.java)
            )
    }

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val position = parameters.position
        val file = position.containingFile
        if (file !is SmkFile) {
            return
        }
        val excludePsi =
            PsiTreeUtil.getParentOfType(position, SmkExcludedRulesNamesList::class.java, true, SmkUse::class.java)

        val collectRules: List<Pair<String, SmkRuleOrCheckpoint>> = when {
            excludePsi != null -> {
                // Only rules from referenced Module and completion requested once:
                excludePsi.getParentUse().collectImportedRuleNameAndPsi(mutableSetOf(), true) ?: emptyList()
            }

            else -> file.collectRules(mutableSetOf())
        }
        // TODO: for left part - same
        collectRules.forEach { (first, second) ->
            result.addElement(
                AbstractSmkRuleOrCheckpointType.createRuleLikeLookupItem(first, second)
            )
        }
        // XXX: optionally all rules on second completion
    }
}

// the following functions can be used to implement completion for any section/object referring to rule names

private fun collectVariantsForElement(
    element: PsiElement,
    isCheckPoint: Boolean,
): List<Pair<String, SmkRuleOrCheckpoint>> {
    val module = ModuleUtilCore.findModuleForPsiElement(element)

    if ((module == null) || DumbService.isDumb(element.project)) {
        // if no module is given: try to collect local files
        val smkFile = element.containingFile.originalFile as SmkFile
        return if (isCheckPoint) smkFile.filterCheckPointsPsi() else smkFile.filterRulesPsi()
    }
    val results: List<SmkRuleOrCheckpoint> = when {
        isCheckPoint -> getVariantsFromIndex(SmkCheckpointNameIndexCompanion.KEY, module, SmkCheckPoint::class.java)
        else -> getVariantsFromIndex(SmkRuleNameIndexCompanion.KEY, module, SmkRule::class.java)
    }

    val resultList = results.mapNotNull { psi ->
        val name = psi.name
        if (name != null) name to psi else null
    }
    val usesRules = mutableListOf<Pair<String, SmkRuleOrCheckpoint>>()
    if (!isCheckPoint) {
        getVariantsFromIndex(SmkUseNameIndexCompanion.KEY, module, SmkUse::class.java).forEach { use ->
            use.getProducedRulesNames().forEach { (first) ->
                usesRules.add(first to use)
            }
        }
    }
    return resultList + usesRules
}

private fun addVariantsToCompletionResultSet(
    completionVariants: List<Pair<String, SmkRuleOrCheckpoint>>,
    parameters: CompletionParameters,
    result: CompletionResultSet,
) {
    /*
      auto popup invocation count: 0
      auto popup, then a manual invocation: 2
      manual invocation once: 1
      manual invocation twice: 2
    */
    val position = parameters.position
    val originalFile = when {
        SmkSLanguage.isInsideSmkSLFile(position) -> {
            val originalPosition = parameters.originalPosition ?: return
            val languageManager = InjectedLanguageManager.getInstance(position.project)
            languageManager.getTopLevelFile(originalPosition)
        }

        SnakemakeLanguageDialect.isInsideSmkFile(position) -> parameters.originalFile
        else -> return
    }

    val variants = if (parameters.invocationCount <= 1) {
        val includedFiles = SmkResolveUtil.getIncludedFiles(originalFile as SmkFile)
        completionVariants.filter { (_, ruleLike) ->
            val psiFile = ruleLike.containingFile.originalFile
            psiFile == originalFile || psiFile in includedFiles
        }
    } else {
        completionVariants
    }

    result.addAllElements(
        variants.map { (name, elem) ->
            AbstractSmkRuleOrCheckpointType.createRuleLikeLookupItem(name, elem)
        }
    )

    result.runRemainingContributors(parameters, false)

    val shortcut = KeymapUtil.getFirstKeyboardShortcutText(
        ActionManager.getInstance()
            .getAction(IdeActions.ACTION_CODE_COMPLETION)
    )
    if (StringUtil.isNotEmpty(shortcut)) {
        result.addLookupAdvertisement(SnakemakeBundle.message("snakemake.completion.twice.for.all.rules", shortcut))
    }
}