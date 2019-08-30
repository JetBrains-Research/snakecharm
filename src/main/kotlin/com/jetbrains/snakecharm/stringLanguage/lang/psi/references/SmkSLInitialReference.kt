package com.jetbrains.snakecharm.stringLanguage.lang.psi.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.module.ModuleUtilCore
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
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolUsageType
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SmkCodeInsightScope
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionVariantsProcessor
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.SmkSection
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl
import java.util.*


class SmkSLInitialReference(
        expr: SmkSLReferenceExpressionImpl,
        private val parentDeclaration: SmkRuleOrCheckpoint?,
        context: PyResolveContext
) : PyQualifiedReference(expr, context) {

    override fun getElement() = super.getElement() as SmkSLReferenceExpressionImpl

    override fun resolveInner(): List<RatedResolveResult> {
        require(!element.isQualified) // this reference is supposed to be not qualified

        PyPsiUtils.assertValid(myElement)
        val ret = ResolveResultList()

        val referencedName = element.referencedName ?: return Collections.emptyList()

        // XXX: resolve names of supported sections available in rule into sections or snakemake python declaration?
        // XXX: (except threads, version, which doesn't have declaration so far)
        val section = collectAccessibleSectionsFromDeclaration().firstOrNull { it.name == referencedName }
        if (section != null) {
            ret.poke(section, RatedResolveResult.RATE_HIGH)
            return ret
        }

        val host = getHostElement(element)
        if (host != null) {
            // Implicit Symbols
            val module = ModuleUtilCore.findModuleForPsiElement(element)
            if (module != null) {
                val contextScope = getSmkScopeForInjection(host)

                val cache = ImplicitPySymbolsProvider.instance(module).cache
                SmkCodeInsightScope.values().asSequence()
                        .filter { symbolScope -> contextScope.includes(symbolScope) }
                        .flatMap { symbolScope -> cache.filter(symbolScope, element.name!!).asSequence() }
                        .filter { symbol -> symbol.usageType == ImplicitPySymbolUsageType.VARIABLE }
                        .forEach {
                            ret.poke(it.psiDeclaration, SmkResolveUtil.RATE_IMPLICIT_SYMBOLS)
                        }

                if (ret.isNotEmpty()) {
                    return ret
                }
            }

            // Resolve to python: : XXX consider possible reuse of PyReference resolve list + filter items
            val processor = PyResolveProcessor(referencedName)
            val scopeOwner = ScopeUtil.getScopeOwner(host) ?: host.containingFile   // not clear what should be here
            val topLevel = scopeOwner.containingFile
            PyResolveUtil.scopeCrawlUp(processor, host, referencedName, topLevel);

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

    private fun isSupportedElementType(result: PsiElement?) =
            !(result is PyFunction || result is PyClass)


    private fun resolveByReferenceResolveProviders(): List<RatedResolveResult> {
        val expression = element
        val context = myContext.typeEvalContext

        return PyReferenceResolveProvider.EP_NAME.extensionList.asSequence()
                .filterNot { it is PyOverridingReferenceResolveProvider }
                .flatMap {  it.resolveName(expression, context).asSequence() }
                .toList()
    }

    override fun getVariants(): Array<LookupElement> {
        // val originalElement = CompletionUtil.getOriginalElement(myElement)

        val variants = mutableListOf<LookupElement>()

        // Accessible sections
        val accessibleSections = collectAccessibleSectionsFromDeclaration().toList()
        accessibleSections.forEach {
            variants.add(SmkCompletionUtil.createPrioritizedLookupElement(it.name!!))
        }


        val host = getHostElement(element)
        if (host != null) {

            // Implicit Symbols
            val accessibleSectionsNames = accessibleSections.asSequence().map { it.sectionKeyword }.toSet()
            val module = ModuleUtilCore.findModuleForPsiElement(element)
            if (module != null) {
                val contextScope = getSmkScopeForInjection(host)
                // perhaps processor should be from 'element', not clear
                val processor = SmkCompletionVariantsProcessor(host)

                val cache = ImplicitPySymbolsProvider.instance(module).cache
                SmkCodeInsightScope.values().asSequence()
                        .filter { symbolScope -> contextScope.includes(symbolScope) }
                        .flatMap { symbolScope -> cache[symbolScope].asSequence() }
                        .filter { symbol -> symbol.usageType == ImplicitPySymbolUsageType.VARIABLE }
                        .filter { symbol -> symbol.identifier !in accessibleSectionsNames }
                        .forEach { symbol ->
                            // processor.execute(it, ResolveState.initial())
                            processor.addElement(symbol.identifier, symbol.psiDeclaration)
                        }
                variants.addAll(processor.resultList )
            }

            // Python: XXX consider possible reuse of PyReference completion list + filter items
            val builtinCache = PyBuiltinCache.getInstance(host);

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
            
            PyResolveUtil.scopeCrawlUp(processor, host, null, null);
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

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity? = HighlightSeverity.WEAK_WARNING
}
