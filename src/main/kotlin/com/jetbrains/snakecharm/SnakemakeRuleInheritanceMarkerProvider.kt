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
import com.jetbrains.snakecharm.lang.psi.stubs.SmkModuleNameIndex.Companion.KEY

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
        val name = use.getNewNamePattern()?.getNameBeforeWildcard() ?: return

        val inheritedRulesDeclaredExplicitly =
            use.getDefinedReferencesOfImportedRuleNames()?.mapNotNull { it.reference.resolve() } ?: emptyList()
        val ruleModule = use.getModuleName()?.reference?.resolve() as? SmkModule
        val importedFile = ruleModule?.getPsiFile() as? SmkFile
        val inheritedRulesDeclaredViaWildcard =
            importedFile?.advancedCollectRules(mutableSetOf())?.map { it.second } ?: emptyList()
        val results = inheritedRulesDeclaredExplicitly.ifEmpty { inheritedRulesDeclaredViaWildcard }
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
        val currentFile = element.containingFile
        val namePsi = when (val identifier = element.nameIdentifier) {
            is SmkUseNewNamePattern -> identifier.getNameBeforeWildcard()
            else -> identifier
        } ?: return
        val modulesFromStub = StubIndex.getInstance().getAllKeys(KEY, element.project)
        val module = element.let { ModuleUtilCore.findModuleForPsiElement(it.originalElement) } ?: return
        val files = mutableSetOf<SmkFile>()
        for (smkModule in modulesFromStub) {
            files.addAll(StubIndex.getElements(
                KEY,
                smkModule,
                module.project,
                GlobalSearchScope.moduleWithDependentsScope(module),
                SmkModule::class.java
            ).mapNotNull { it.containingFile as? SmkFile })
        }
        if (currentFile is SmkFile) {
            files.add(currentFile)
        }
        val overrides = mutableListOf<SmkRuleOrCheckpoint>()
        files.forEach { file ->
            file.collectUses().forEach { (_, psi) ->
                if (psi.getImportedRules()?.firstOrNull { it == element } != null) {
                    overrides.add(psi)
                }
            }
        }
        if (overrides.isEmpty()) {
            return
        }
        val builder = NavigationGutterIconBuilder.create(AllIcons.Gutter.OverridenMethod)
            .setTargets(overrides)
            .setTooltipText(SnakemakeBundle.message("smk.line.marker.provider.overridden.rules"))
        result.add(builder.createLineMarkerInfo(namePsi))
    }
}