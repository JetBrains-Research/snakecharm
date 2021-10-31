package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.impl.ResolveResultList
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyStructuralType
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkPepConfigCollector
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionIndexKeyExpressionImpl

class SmkPepConfigType(val smkFile: SmkFile) : PyStructuralType(emptySet(), false), SmkAvailableForSubscriptionType {
    override fun getPositionArgsPreviews(location: PsiElement): List<String?> {
        return emptyList()
    }

    override fun getCompletionVariantsAndPriority(
        completionPrefix: String?,
        location: PsiElement,
        context: ProcessingContext?
    ): Pair<List<LookupElementBuilder>, Double> = TODO()

    override fun resolveMemberByIndex(
        idx: Int,
        location: PyExpression?,
        direction: AccessDirection,
        resolveContext: PyResolveContext
    ): List<RatedResolveResult> = TODO()

    override fun resolveMember(
        name: String,
        location: PyExpression?,
        direction: AccessDirection,
        resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return emptyList()
        }
        val resolveResult = ResolveResultList()
        SmkPepConfigCollector.getYamlParseResult(smkFile).second
            .filter { it.text == name }
            .forEach {
                resolveResult.poke(it, SmkResolveUtil.RATE_NORMAL)
            }
        return resolveResult
    }

    override fun getCompletionVariants(
        completionPrefix: String?,
        location: PsiElement?,
        context: ProcessingContext?
    ): Array<LookupElement> = getVariants(location is SmkSLSubscriptionIndexKeyExpressionImpl).map {
        SmkCompletionUtil.createPrioritizedLookupElement(it.second, it.first)
    }.toTypedArray()


    override fun getName(): String = "pep.config"

    override fun isBuiltin(): Boolean = false

    override fun assertValid(message: String?) {
        SmkPepConfigCollector.getYamlParseResult(smkFile).second.forEach { psiElement ->
            if (!psiElement.isValid) {
                throw PsiInvalidElementAccessException(psiElement, message)
            }
        }
    }

    private fun getVariants(isSubscriptionForm: Boolean): List<Pair<PsiElement, String>> {
        var containPepVersion = false
        val resultList = mutableListOf<Pair<PsiElement, String>>()
        val yamlParseResult = SmkPepConfigCollector.getYamlParseResult(smkFile)
        yamlParseResult.second.forEach { key ->
            val keyName = key.text
            if (keyName == SnakemakeAPI.PEPPY_CONFIG_PEP_VERSION) containPepVersion = true
            if (isSubscriptionForm ||
                keyName in SnakemakeAPI.PEPPY_CONFIG_TEXT_KEYS ||
                keyName in SnakemakeAPI.PEPPY_CONFIG_MAPPING_KEYS ||
                KEY_NAME_PATTERN.matches(keyName)
            ) resultList.add(key to keyName)
        }
        if (!containPepVersion && yamlParseResult.first != null) {
            resultList.add(
                yamlParseResult.first!! to
                        SnakemakeAPI.PEPPY_CONFIG_PEP_VERSION
            )
        }
        return resultList
        //SmkSLSubscriptionKeyExpression
    }

    companion object {
        private val KEY_NAME_PATTERN = """[\w&&[^\d]]+[\w]*""".toRegex()
    }
}
