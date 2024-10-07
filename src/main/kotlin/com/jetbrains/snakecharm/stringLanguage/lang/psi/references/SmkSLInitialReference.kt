package com.jetbrains.snakecharm.stringLanguage.lang.psi.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.codeInsight.dataflow.scope.ScopeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyBuiltinCache
import com.jetbrains.python.psi.impl.PyPsiUtils
import com.jetbrains.python.psi.impl.ResolveResultList
import com.jetbrains.python.psi.impl.references.PyQualifiedReference
import com.jetbrains.python.psi.resolve.*
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolUsageType
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SmkCodeInsightScope
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionVariantsProcessor
import com.jetbrains.snakecharm.codeInsight.resolve.SmkImplicitPySymbolsResolveProvider
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.SmkSection
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkPyReferenceImpl
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression
import java.util.*


class SmkSLInitialReference(
    expr: SmkSLReferenceExpression,
    private val parentDeclaration: SmkRuleOrCheckpoint?,
    context: PyResolveContext?,
) : PyQualifiedReference(expr, context), SmkSLBaseReference {

    override fun getElement() = myElement as SmkSLReferenceExpression

    @Suppress("UnstableApiUsage")
    override fun resolveInner(): MutableList<RatedResolveResult> {
        require(!element.isQualified) // this reference is supposed to be not qualified

        PyPsiUtils.assertValid(myElement)
        val ret = ResolveResultList()

        //TODO: change resolve order if in 'run:' section at least to support local variables like 'output=1' preference
        // 1. python local resolve
        // 2. if no result => sections, implicts
        // 3. else rest python + providers


        val referencedName = element.referencedName ?: return Collections.emptyList()

        // XXX: resolve names of supported sections available in rule into sections or snakemake python declaration?
        // XXX: (except threads, version, which doesn't have declaration so far)
        val section = collectAccessibleSectionsFromDeclaration().firstOrNull { it.name == referencedName }
        if (section != null) {
            ret.poke(section, SmkResolveUtil.RATE_IMPLICIT_SYMBOLS)
            return ret
        }

        val host = getHostElement(element)
        if (host != null) {
            if (referencedName == SMK_VARS_WILDCARDS) {
                val parentRule = PsiTreeUtil.getParentOfType(host, SmkRuleOrCheckpoint::class.java)
                if (parentRule != null) {
                    ret.poke(parentRule.wildcardsElement, SmkResolveUtil.RATE_IMPLICIT_SYMBOLS)
                    return ret
                }
            }

            // Implicit Symbols
            val contextScope = getSmkScopeForInjection(host)

            val cache = ImplicitPySymbolsProvider.instance(element.project).cache

            SmkImplicitPySymbolsResolveProvider.addSyntheticSymbols(contextScope, cache, referencedName, ret)

            SmkCodeInsightScope.entries.asSequence()
                .filter { symbolScope -> contextScope.includes(symbolScope) }
                .flatMap { symbolScope -> cache.filter(symbolScope, element.name!!).asSequence() }
                .filter { symbol -> symbol.usageType == ImplicitPySymbolUsageType.VARIABLE }
                .forEach {
                    ret.poke(it.psiDeclaration, SmkResolveUtil.RATE_IMPLICIT_SYMBOLS)
                }

            if (ret.isNotEmpty()) {
                return ret
            }

            // Resolve to python: : XXX consider possible reuse of PyReference resolve list + filter items
            val processor = PyResolveProcessor(referencedName)
            val scopeOwner = ScopeUtil.getScopeOwner(host) ?: host.containingFile   // not clear what should be here
            val topLevel = scopeOwner.containingFile
            PyResolveUtil.scopeCrawlUp(processor, host, referencedName, topLevel)

            val scopeControlFlowAnchor = host.containingFile  // not clear what should be here
            val resultList = getResultsFromProcessor(referencedName, processor, scopeControlFlowAnchor, topLevel)
            resultList.asSequence()
                .filter { isSupportedElementType(it.element) }
                .forEach { ret.add(it) }
        }

        // custom providers
        resolveByReferenceResolveProviders()
            .asSequence()
            .filter { isSupportedElementType(it.element) }
            .forEach { ret.add(it) }

        return ret
    }

    private fun getHostElement(e: PsiElement) =
        InjectedLanguageManager.getInstance(e.project).getInjectionHost(e)

    private fun isSupportedElementType(element: PsiElement?) =
        !(element is PyFunction || element is PyClass) &&
                !SmkPyReferenceImpl.shouldBeRemovedFromDefaultScopeCrawlUpResults(element, false, parentDeclaration)


    private fun resolveByReferenceResolveProviders(): List<RatedResolveResult> {
        val expression = element
        val context = myContext.typeEvalContext

        return PyReferenceResolveProvider.EP_NAME.extensionList.asSequence()
            .filterNot { it is PyOverridingReferenceResolveProvider }
            .flatMap { it.resolveName(expression, context).asSequence() }
            .toList()
    }

    override fun copyWithResolveContext(context: PyResolveContext?) =
        SmkSLInitialReference(element, parentDeclaration, context)

    override fun getVariants(): Array<LookupElement> {
        // val originalElement = CompletionUtil.getOriginalElement(myElement)

        val variants = mutableListOf<LookupElement>()

        // Accessible sections
        val accessibleSections = collectAccessibleSectionsFromDeclaration().toList()
        accessibleSections.forEach {
            variants.add(SmkCompletionUtil.createPrioritizedLookupElement(
                it.name!!, it,
                typeText = SnakemakeBundle.message("TYPES.rule.section.type.text"),
                priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
            ))
        }

        val host = getHostElement(element)
        if (host != null) {
            // Wildcards
            parentDeclaration?.let {
                val element = SmkCompletionUtil.createPrioritizedLookupElement(
                    SMK_VARS_WILDCARDS,
                    parentDeclaration.wildcardsElement,
                    typeText = SnakemakeBundle.message("TYPES.rule.wildcard.type.text"),
                    priority = SmkCompletionUtil.SECTIONS_KEYS_PRIORITY
                )
                variants.add(element)
            }

            // Implicit Symbols
            val seenImplicitSymbols = accessibleSections.asSequence().map { it.sectionKeyword }.toMutableSet()
            seenImplicitSymbols.add(SMK_VARS_WILDCARDS)

            val contextScope = getSmkScopeForInjection(host)
            // perhaps processor should be from 'element', not clear
            val implicitsProcessor = SmkCompletionVariantsProcessor(host)

            val cache = ImplicitPySymbolsProvider.instance(element.project).cache
            SmkCodeInsightScope.entries.forEach { symbolScope ->
                if (contextScope.includes(symbolScope)) {
                    variants.addAll(cache.getSynthetic(symbolScope))
                }
            }

            SmkCodeInsightScope.entries.asSequence()
                .filter { symbolScope -> contextScope.includes(symbolScope) }
                .flatMap { symbolScope -> cache[symbolScope].asSequence() }
                .filter { symbol -> symbol.usageType == ImplicitPySymbolUsageType.VARIABLE }
                .filter { symbol -> symbol.identifier !in seenImplicitSymbols }
                .forEach { symbol ->
                    // processor.execute(it, ResolveState.initial())
                    implicitsProcessor.addElement(symbol.identifier, symbol.psiDeclaration)
                }
            variants.addAll(implicitsProcessor.resultList)

            // Python: XXX consider possible reuse of PyReference completion list + filter items
            val builtinCache = PyBuiltinCache.getInstance(host)

            // perhaps processor should be from 'element', not clear
            val processor = CompletionVariantsProcessor(host, { e ->
                // ignore private things & imports from built-in (see [PyReferenceImpl])
                if (builtinCache.isBuiltin(e)) {
                    if (e is PyImportElement) {
                        return@CompletionVariantsProcessor false
                    }

                    val name = if (e is PyElement) e.name else null
                    if (PyUtil.getInitialUnderscores(name) == 1) {
                        return@CompletionVariantsProcessor false
                    }
                }
                return@CompletionVariantsProcessor true

            }, null)
            //TODO: [ScopeImpl.collectDeclarations.visitPyElement; PsiNameIdentifierOwner is namedElement => added to symbols..]
            PyResolveUtil.scopeCrawlUp(processor, host, null, null)
            processor.resultList
                .asSequence()
                .filter { isSupportedElementType(it.psiElement) }
                .forEach { variants.add(it) }

        }

        return variants.toTypedArray()
    }

    private fun getSmkScopeForInjection(host: PsiLanguageInjectionHost): SmkCodeInsightScope {
        val section = PsiTreeUtil.getParentOfType(host, SmkSection::class.java)
        val sectionKeyword = section?.sectionKeyword

        return if (section is SmkRunSection || sectionKeyword !in SnakemakeAPI.WILDCARDS_EXPANDING_SECTIONS_KEYWORDS) {
            SmkCodeInsightScope.RULELIKE_RUN_SECTION
        } else {
            SmkCodeInsightScope.TOP_LEVEL
        }
    }

    private fun collectAccessibleSectionsFromDeclaration(): Sequence<SmkSection> {
        if (parentDeclaration == null) {
            return emptySequence()
        }

        return parentDeclaration.statementList.statements
            .asSequence()
            .filterIsInstance<SmkSection>()
            .filter { it.sectionKeyword in SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS }
    }

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity? =
        HighlightSeverity.WEAK_WARNING

    override fun handleElementRename(newElementName: String) =
        element.setName(newElementName)
}
