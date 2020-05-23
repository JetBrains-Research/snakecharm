package com.jetbrains.snakecharm

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.impl.LineMarkersPass
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SnakemakeMethodLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<PsiElement>? =
            if (DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS && isSeparatorAllowed(element)) {
                LineMarkersPass.createMethodSeparatorLineMarker(
                        PsiTreeUtil.getDeepestFirst(element),
                        EditorColorsManager.getInstance()
                )
            } else {
                null
            }

    private fun isSeparatorAllowed(element: PsiElement?): Boolean {
        if (element is SmkRuleLike<*>)  {
            return true
        }
        return element?.firstChild == null && element?.parent is SmkRuleLike<*>
    }
}