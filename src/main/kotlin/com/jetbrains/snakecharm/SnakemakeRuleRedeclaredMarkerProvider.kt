package com.jetbrains.snakecharm

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.snakecharm.inspections.SmkRuleRedeclarationInspection
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.RULE_OR_CHECKPOINT
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

class SnakemakeRuleRedeclaredMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        // TODO if leaf element -> get real element at offset?
        if (element is SmkRuleOrCheckpoint && (element is SmkRule || element is SmkCheckPoint)) {
            collectOverloadingRulesOrCheckpoint(element, result)
        // TODO: use?
        } else if (element.node.elementType in RULE_OR_CHECKPOINT) {
            // Not clear why, sometimes we get 'rule' keyword, not SmkRule and gutter won't be shown by the
            // above code
            val parent = element.parent
            if (parent is SmkRuleOrCheckpoint) {
                collectOverloadingRulesOrCheckpoint(parent, result)
            }
        }

        element is LeafPsiElement
    }

    private fun collectOverloadingRulesOrCheckpoint(
        ruleLike: SmkRuleOrCheckpoint,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val containingFile = ruleLike.containingFile
        if (containingFile !is SmkFile) {
            return
        }
        val nameToCheck = ruleLike.name ?: return
        val nameIdentifier = ruleLike.nameIdentifier ?: return

        // TODO: reuse in SmkRuleRedeclarationInspection
        // TODO: reuse in SmkRuleRedeclarationInspection
        val localRules = PyUtil.getParameterizedCachedValue(containingFile, "localRules") {
            containingFile.collectRules().map { it.second }
        }
        val localCheckpoints = PyUtil.getParameterizedCachedValue(containingFile, "localCheckpoints") {
            containingFile.collectCheckPoints().map { it.second }
        }

//        val localUses = PyUtil.getParameterizedCachedValue(containingFile, "localUses") {
//            containingFile.collectUses().map { it.second }
//        }


        val resolveResults = SmkRuleRedeclarationInspection.collectRuleLikeWithSameName(
            ruleLike, nameToCheck,
            localRules = { localRules },
            localCheckpoints = { localCheckpoints },
//            localUses = { localUses }
            localUses = null
        )

        if (resolveResults.isEmpty()) {
            return
        }

        if (resolveResults.size == 1 && resolveResults.single() == ruleLike) {
            return
        }

        val ruleRedeclaresElements = mutableListOf<PsiElement>()
        val ruleRedeclaredInItems = mutableListOf<PsiElement>()
        val textOffset = ruleLike.textOffset
        resolveResults.forEach { res ->
            if (res.containingFile == containingFile) {
                val resOffset = res.textOffset
                if (resOffset < textOffset) {
                    ruleRedeclaresElements.add(res)
                } else if (resOffset > textOffset) {
                    ruleRedeclaredInItems.add(res)
                }
            } else {
                // Icon: AllIcons.Gutter.Unique ?
                ruleRedeclaresElements.add(res)
            }
        }

        if (ruleRedeclaresElements.isNotEmpty()) {
            result.add(
                NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementingMethod)
                    .setTargets(ruleRedeclaresElements)
                    .setTooltipText(SnakemakeBundle.message("smk.line.marker.provider.rule.redeclares"))
                    .createLineMarkerInfo(nameIdentifier)
            )
        }

        if (ruleRedeclaredInItems.isNotEmpty()) {
            result.add(
                NavigationGutterIconBuilder.create(AllIcons.Gutter.ImplementedMethod)
                    .setTargets(ruleRedeclaredInItems)
                    .setTooltipText(SnakemakeBundle.message("smk.line.marker.provider.rule.is.redeclared.in"))
                    .createLineMarkerInfo(nameIdentifier)
            )
        }
    }

}