package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.codeInsight.completion.PyFunctionInsertHandler
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.resolve.CompletionVariantsProcessor
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SmkCodeInsightScope
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.SnakemakeNames.RUN_SECTION_VARIABLE_RULE
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SMKImplicitPySymbolsCompletionContributor : CompletionContributor() {
    companion object {
        val IN_PY_REF = psiElement().inside(PyReferenceExpression::class.java)

        private val REF_CAPTURE = psiElement()
                .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
                .and(IN_PY_REF)
                .with(object : PatternCondition<PsiElement>("isFirstChild") {
                    override fun accepts(element: PsiElement, context: ProcessingContext): Boolean {
                        // check that this element is "first" in parent PyReferenceExpression, e.g. that
                        // we don't have some prefix with '.', e.g. element is 'exp<caret>', not 'foo.exp<caret>'
                        
                        val refExpression = PsiTreeUtil.getParentOfType(element, PyReferenceExpression::class.java)
                        return !(refExpression?.isQualified ?: true)
                    }
                })
    }

    init {
        extend(CompletionType.BASIC, REF_CAPTURE, SMKImplicitPySymbolsCompletionProvider())
    }
}

class SMKImplicitPySymbolsCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters,
                                context: ProcessingContext,
                                result: CompletionResultSet) {

        val contextElement = parameters.position
        val contextScope = SmkCodeInsightScope[contextElement]

        val module = ModuleUtilCore.findModuleForPsiElement(parameters.originalFile)
        val cache = module?.let { ImplicitPySymbolsProvider.instance(module).cache }

        if (contextScope == SmkCodeInsightScope.RULELIKE_RUN_SECTION) {
            val ruleOrCheckpoint = PsiTreeUtil.getParentOfType(contextElement, SmkRuleOrCheckpoint::class.java)!!

            // wildcards
            result.addElement(
                SmkCompletionUtil.createPrioritizedLookupElement(
                    SnakemakeAPI.SMK_VARS_WILDCARDS,
                    ruleOrCheckpoint.wildcardsElement,
                    typeText = SnakemakeBundle.message("TYPES.rule.wildcard.type.text"),
                    priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                )
            )

            // if section isn't provided, let's still add it to completion list
            // * Do not add item if section exists (existing sections are in completion list as Named Elements)
            //   let's do not duplicate this
            // * If section in implicit provider => also avoid duplicated items
            for (sectionName in SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS) {
                if (cache != null) {
                    if (cache.contains(SmkCodeInsightScope.RULELIKE_RUN_SECTION, sectionName)) {
                        // avoid duplicated items in completion list
                        continue
                    }
                }

                val psiSection = ruleOrCheckpoint.statementList.statements.asSequence()
                    .filterIsInstance<SmkRuleOrCheckpointArgsSection>()
                    .filter { it.name == sectionName }.firstOrNull()

                if (psiSection != null) {
                    // Will be added by PyCharm API control flow because sections are Named Elements
                    continue
                }

                result.addElement(
                    SmkCompletionUtil.createPrioritizedLookupElement(
                        sectionName,
                        psiSection,
                        typeText = SnakemakeBundle.message("TYPES.rule.section.type.text"),
                        priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                    )
                )
            }

            // Fake variable: jobid
            result.addElement(
                SmkCompletionUtil.createPrioritizedLookupElement(
                    SnakemakeNames.RUN_SECTION_VARIABLE_JOBID,
                    null,
                    typeText = SnakemakeBundle.message("TYPES.rule.run.variable.type.text"),
                    priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                )
            )

            // Fake variable: rule
            result.addElement(
                SmkCompletionUtil.createPrioritizedLookupElement(
                    RUN_SECTION_VARIABLE_RULE,
                    null,
                    typeText = SnakemakeBundle.message("TYPES.rule.run.variable.type.text"),
                    priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                )
            )
        }


        if (module != null) {
            val processor = SmkCompletionVariantsProcessor(contextElement)
            requireNotNull(cache)

            SmkCodeInsightScope.values().asSequence()
                    .filter { symbolScope -> contextScope.includes(symbolScope) }
                    .flatMap { symbolScope -> cache[symbolScope].asSequence() }
                    .forEach { symbol ->
                        // processor.execute(it, ResolveState.initial())
                        processor.addElement(symbol.identifier, symbol.psiDeclaration)
                    }

            val processorResults = processor.resultList

            // let's make 'shell' class to have insert handler like for function
            val idx = processorResults.indexOfFirst { item ->
                item is LookupElementBuilder && item.psiElement is PyClass && item.lookupString == SnakemakeNames.SECTION_SHELL
            }
            if (idx != -1) {
                val lookupItem = processorResults[idx] as LookupElementBuilder
                processorResults[idx] = lookupItem.withInsertHandler(PyFunctionInsertHandler.INSTANCE)
            }

            result.addAllElements(processorResults)
        }
    }
}

/**
 * Just makes `addElement` public. PyCharm API required here
 */
class SmkCompletionVariantsProcessor(context: PsiElement) : CompletionVariantsProcessor(context) {
    // todo: actually need setupItem()
    public override fun addElement(name: String, element: PsiElement) = super.addElement(name, element)
}