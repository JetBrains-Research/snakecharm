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
        val name = use.getIdentifierLeaf() ?: return
        val results =
            use.getImportedRuleNames()?.mapNotNull { it.reference.resolve() } // Inherited rules are declared in list
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
        val identifier = (if (element is SmkUse) element.getIdentifierLeaf() else element.nameIdentifier) ?: return
        val names = if (element is SmkUse) element.getProducedRulesNames().map { it.first } else listOf(
            element.name ?: return
        )
        val e = StubIndex.getInstance().getAllKeys(KEY, element.project)
        val module = element.let { ModuleUtilCore.findModuleForPsiElement(it.originalElement) } ?: return
        val files = mutableSetOf<SmkFile>()
        for (key in e) {
            files.addAll(StubIndex.getElements(
                KEY,
                key,
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
                val containsReferenceToTargetRule = pair.second.getImportedRuleNames()?.any { it.text in names }
                    ?: pair.second.hasPatternInDefinitionOfInheritedRules()
                val moduleReference = pair.second.getModuleName()
                val moduleReferToCurrentFileIfExists =
                    if (moduleReference != null) (moduleReference.reference?.resolve() as? SmkModule)?.getPsiFile() == currentFile else true
                if (containsReferenceToTargetRule && moduleReferToCurrentFileIfExists) {
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