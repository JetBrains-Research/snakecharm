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
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkSLInjector : LanguageInjector {
    override fun getLanguagesToInject(
            host: PsiLanguageInjectionHost,
            injectionPlacesRegistrar: InjectedLanguagePlaces) {
        if (!host.isStringLiteralInsideArgsSection()) {
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

    private fun PsiElement.isStringLiteralInsideArgsSection() =
            SnakemakeLanguageDialect.isInsideSmkFile(this) &&
            this is PyStringLiteralExpression &&
            PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpointArgsSection::class.java) != null &&
            PsiTreeUtil.getParentOfType(this, PyCallExpression::class.java) == null
}