package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.text.StringUtil
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndex
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType
import com.jetbrains.snakecharm.string_language.SmkSL

class SmkRulesAndCheckpointsCompletionContributor : CompletionContributor() {
    init {
        extend(
                CompletionType.BASIC,
                SmkRuleNameReferenceCompletionProvider.IN_SMK_LOCALRULES_OR_RULEORDER_RULE_NAME_REFERENCE,
                SmkRuleNameReferenceCompletionProvider()
        )
        extend(
                CompletionType.BASIC,
                SmkRulesAndCheckpointsObjectsCompletionProvider.IN_SMK_RULES_OR_CHECKPOINTS_OBJECT,
                SmkRulesAndCheckpointsObjectsCompletionProvider()
        )
    }
}

private class SmkRuleNameReferenceCompletionProvider : CompletionProvider<CompletionParameters>() {
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
            result: CompletionResultSet
    ) {
        val variants = collectVariantsForElement(
                parameters.position,
                SmkRuleNameIndex.KEY,
                SmkRule::class.java,
                SmkFile::collectRules
        )
        variants.addAll(collectVariantsForElement(
                parameters.position,
                SmkCheckpointNameIndex.KEY,
                SmkCheckPoint::class.java,
                SmkFile::collectCheckPoints
        ))
        val references =
                (PsiTreeUtil.getParentOfType(parameters.position, SmkWorkflowRuleorderSection::class.java)
                        ?: PsiTreeUtil.getParentOfType(parameters.position, SmkWorkflowLocalrulesSection::class.java))
                        ?.argumentList
                        ?.arguments
                        ?.filterIsInstance<SmkReferenceExpression>()
                        ?.map { it.name }
        if (references != null) {
            variants.removeAll { it.first in references }
        }
        addVariantsToCompletionResultSet(variants, parameters, result)
    }
}

private class SmkRulesAndCheckpointsObjectsCompletionProvider : CompletionProvider<CompletionParameters>() {
    companion object {
        val IN_SMK_RULES_OR_CHECKPOINTS_OBJECT =
                psiElement()
                        .withParent(
                                psiElement(PyReferenceExpression::class.java)
                                        .withChild(psiElement().andOr(
                                                psiElement().withText(SnakemakeNames.SMK_VARS_CHECKPOINTS),
                                                psiElement().withText(SnakemakeNames.SMK_VARS_RULES)
                                        ))
                        )
    }

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        // position is a leaf psi element, and it's wrapped in a reference, which is what we want to get
        val parentElement = parameters.position.parent
        // get parent reference to `rules.` or `checkpoints.`
        val rulesOrCheckpointsObject = parameters.withPosition(parentElement, parentElement.textOffset)
                .originalPosition
                ?.parent as? PyReferenceExpression ?: return
        val variants = when (rulesOrCheckpointsObject.name) {
            SnakemakeNames.SMK_VARS_RULES -> collectVariantsForElement(
                    parameters.position,
                    SmkRuleNameIndex.KEY,
                    SmkRule::class.java,
                    SmkFile::collectRules
            )
            SnakemakeNames.SMK_VARS_CHECKPOINTS -> collectVariantsForElement(
                    parameters.position,
                    SmkCheckpointNameIndex.KEY,
                    SmkCheckPoint::class.java,
                    SmkFile::collectCheckPoints
            )
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
                PsiTreeUtil.getParentOfType(parentElement, SmkRunSection::class.java) == null) {
            variants.removeAll { it.second == originalContainingRuleOrCheckpoint }
        }
        addVariantsToCompletionResultSet(variants, parameters, result)
    }

    private fun PsiElement.isInLanguageInjection(): Boolean {
        val languageManager = InjectedLanguageManager.getInstance(project)
        return languageManager.getInjectionHost(this) != null
    }

    private fun getContainingDeclarationOfInjectedElement(element: PsiElement): SmkRuleOrCheckpoint? {
        val languageManager = InjectedLanguageManager.getInstance(element.project)
        return  PsiTreeUtil.getParentOfType(
                languageManager.getInjectionHost(element),
                SmkRuleOrCheckpoint::class.java
        )
    }
}


// the following functions can be used to implement completion for any section/object referring to rule names

private fun <Psi: SmkRuleOrCheckpoint> collectVariantsForElement(
        element: PsiElement,
        indexKey: StubIndexKey<String, Psi>,
        sectionClass: Class<Psi>,
        currentFileDeclarationsFunction: (SmkFile) -> List<Pair<String, Psi>>
): MutableList<Pair<String, SmkRuleOrCheckpoint>> {
    val variants = mutableListOf<Pair<String, SmkRuleOrCheckpoint>>()

    val module = ModuleUtilCore.findModuleForPsiElement(element)
    if (module != null) {
        val results = mutableListOf<Psi>()
        AbstractSmkRuleOrCheckpointType.addVariantFromIndex(indexKey, module, results, sectionClass)
        variants.addAll(results
                .filter { (it as SmkRuleOrCheckpoint).name != null }
                .map { (it as SmkRuleOrCheckpoint).name!! to it }
        )
    } else {
        variants.addAll(currentFileDeclarationsFunction.invoke(element.containingFile.originalFile as SmkFile))
    }

    return variants
}

private fun addVariantsToCompletionResultSet(
        variants: MutableList<Pair<String, SmkRuleOrCheckpoint>>,
        parameters: CompletionParameters,
        result: CompletionResultSet
) {
    /*
      auto popup invocation count: 0
      auto popup, then a manual invocation: 2
      manual invocation once: 1
      manual invocation twice: 2
    */
    val position = parameters.position
    val originalFile = when {
        SmkSL.isInsideSmkSLFile(position) -> {
            val originalPosition = parameters.originalPosition ?: return
            val languageManager = InjectedLanguageManager.getInstance(position.project)
            languageManager.getTopLevelFile(originalPosition)
        }
        SnakemakeLanguageDialect.isInsideSmkFile(position) -> parameters.originalFile
        else -> return
    }

    if (parameters.invocationCount <= 1) {
        val includedFiles = SmkResolveUtil.getIncludedFiles(originalFile as SmkFile)
        variants.retainAll {
            it.second.containingFile.originalFile == originalFile ||
                    it.second.containingFile.originalFile in includedFiles
        }
    }

    result.addAllElements(
            variants.map { (name, elem) ->
                AbstractSmkRuleOrCheckpointType.createRuleLikeLookupItem(name, elem)
            }
    )

    result.runRemainingContributors(parameters, false)

    val shortcut = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance()
            .getAction(IdeActions.ACTION_CODE_COMPLETION))
    if (StringUtil.isNotEmpty(shortcut)) {
        result.addLookupAdvertisement("Pressing $shortcut twice would show all rules and checkpoints in the module.")
    }
}