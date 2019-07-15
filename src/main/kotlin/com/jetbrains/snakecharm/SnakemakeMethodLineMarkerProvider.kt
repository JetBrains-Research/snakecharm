package com.jetbrains.snakecharm

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.impl.LineMarkersPass
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.psi.PsiElement
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SnakemakeMethodLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS) {
            if (isSeparatorAllowed(element)) {
                return LineMarkersPass.createMethodSeparatorLineMarker(
                        element,
                        EditorColorsManager.getInstance()
                )
            }
        }
        return null
    }

    override fun collectSlowLineMarkers(elements: MutableList<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {
        // Do nothing
    }

    private fun isSeparatorAllowed(element: PsiElement) = element is SmkRuleLike<*>
}