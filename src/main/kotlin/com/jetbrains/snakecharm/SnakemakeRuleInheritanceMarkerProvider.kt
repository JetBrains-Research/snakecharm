package com.jetbrains.snakecharm

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseInheritedRulesIndex
import com.jetbrains.snakecharm.lang.psi.stubs.SmkUseInheritedRulesIndex.Companion.INHERITED_RULES_DECLARATION_VIA_WILDCARD

class SnakemakeRuleInheritanceMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element !is SmkRuleOrCheckpoint) {
            return
        }
        if (element is SmkUse) {
            collectOverridingRulesOrCheckpoint(element, result)
        }
        collectOverriddenRuleOrCheckpoint(element, result)
    }

    private fun collectOverridingRulesOrCheckpoint(
        use: SmkUse,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val name = (use.nameIdentifier as? SmkUseNameIdentifier)?.getNameBeforeWildcard() ?: return
        val inheritedRulesDeclaredExplicitly =
            use.getImportedRulesNames()?.resolveArguments() ?: emptyList()
        val results = inheritedRulesDeclaredExplicitly.ifEmpty {
            val ruleModule = use.getModuleName()?.reference?.resolve() as? SmkModule
            val importedFile = ruleModule?.getPsiFile() as? SmkFile
            importedFile?.advancedCollectRules(mutableSetOf())?.map { it.second } ?: emptyList()
        }
        if (results.isEmpty()) {
            return
        }
        val builder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridingMethod)
            .setTargets(results)
            .setTooltipText(SnakemakeBundle.message("smk.line.marker.provider.overriding.rules"))
        result.add(builder.createLineMarkerInfo(name))
    }

    private fun collectOverriddenRuleOrCheckpoint(
        element: SmkRuleOrCheckpoint,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val identifier = when (val identifier = element.nameIdentifier) {
            is SmkUseNameIdentifier -> identifier.getNameBeforeWildcard()
            else -> identifier
        } ?: return
        val module = element.let { ModuleUtilCore.findModuleForPsiElement(it.originalElement) } ?: return
        val descendantsDefinedExplicitly = StubIndex.getElements(
            SmkUseInheritedRulesIndex.KEY,
            identifier.text,
            module.project,
            GlobalSearchScope.moduleWithDependentsScope(module),
            SmkUse::class.java
        )
        val potentialDescendants = StubIndex.getElements(
            SmkUseInheritedRulesIndex.KEY,
            INHERITED_RULES_DECLARATION_VIA_WILDCARD,
            module.project,
            GlobalSearchScope.moduleWithDependentsScope(module),
            SmkUse::class.java
        )
        val overrides = mutableListOf<SmkRuleOrCheckpoint>()
        (descendantsDefinedExplicitly + potentialDescendants).forEach { descendant ->
            // We don't save it in stub because it requires 'resolve'
            // We compare resolve results even for descendantsDefinedExplicitly
            // Because there may be rules with the same names
            if (descendant.getImportedRulesAndResolveThem()?.contains(element) == true) {
                overrides.add(descendant)
            }
        }
        if (overrides.isEmpty()) {
            return
        }
        val builder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridenMethod)
            .setTargets(overrides)
            .setTooltipText(SnakemakeBundle.message("smk.line.marker.provider.overridden.rules"))
        result.add(builder.createLineMarkerInfo(identifier))
    }
}