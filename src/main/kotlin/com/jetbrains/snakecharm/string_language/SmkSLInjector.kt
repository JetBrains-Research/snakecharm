package com.jetbrains.snakecharm.string_language

import com.intellij.psi.InjectedLanguagePlaces
import com.intellij.psi.LanguageInjector
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyFormattedStringElement
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkSLInjector : LanguageInjector {
    override fun getLanguagesToInject(
            host: PsiLanguageInjectionHost,
            injectionPlacesRegistrar: InjectedLanguagePlaces) {
        if (!host.isValidForInjection()) {
            return
        }

        val stringElements = (host as PyStringLiteralExpression).stringElements
        stringElements.forEach {element->
            if (element is PyFormattedStringElement ||
                element.content.isEmpty()) {
                return@forEach
            }

            injectionPlacesRegistrar.addPlace(
                    SmkStringLanguage,
                    element.contentRange
                           .shiftRight(element.startOffsetInParent),
                    null,
                    null
            )
        }
    }

    private fun PsiElement.isInValidArgsSection(): Boolean {
        val parentSection =
                PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpointArgsSection::class.java)
        return parentSection != null && parentSection.name != SnakemakeNames.SECTION_SHADOW
    }

    private fun PsiElement.isValidForInjection() =
        SnakemakeLanguageDialect.isInsideSmkFile(this) &&
        this is PyStringLiteralExpression &&
        PsiTreeUtil.getParentOfType(this, PyCallExpression::class.java) == null &&
        isInValidArgsSection()
}