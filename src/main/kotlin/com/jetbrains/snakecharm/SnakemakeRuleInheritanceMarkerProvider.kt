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
        // TODO: maybe change to visitor usage
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
        val name = (if (use.nameIdentifierIsWildcard()) use.nameIdentifier?.firstChild else use.nameIdentifier) ?: return

        val results =
            use.getDefinedReferencesOfImportedRuleNames()
                ?.mapNotNull { it.reference.resolve() } // Inherited rules are declared in list
                ?: ((use.getModuleName()?.reference?.resolve() as? SmkModule)?.getPsiFile() as? SmkFile)?.advancedCollectRules(
                    mutableSetOf() // Inherited rules are declared by '*' pattern
                )?.map { it.second } ?: return

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
        val identifier = (if (element is SmkUse && element.nameIdentifierIsWildcard()) element.nameIdentifier?.firstChild else element.nameIdentifier) ?: return
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
        currentFile.also {
            if (it is SmkFile) {
                files.add(it)
            }
        }
        val overrides = mutableListOf<SmkRuleOrCheckpoint>()
        files.forEach { file ->
            file.collectUses().forEach { pair ->
                if(pair.second.getImportedRules()?.firstOrNull{it == element} != null){
                    overrides.add(pair.second)
                }
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