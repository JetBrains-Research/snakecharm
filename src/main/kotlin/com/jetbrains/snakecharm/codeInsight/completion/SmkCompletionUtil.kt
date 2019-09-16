package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.util.PlatformIcons
import javax.swing.Icon

class SmkCompletionUtil {
    companion object {
        val RULES_AND_CHECKPOINTS_PRIORITY = Double.MAX_VALUE
        const val WILDCARDS_LAMBDA_PARAMETER_PRIORITY = 1.0
        const val SECTIONS_KEYS_PRIORITY = 30.0
        const val SUBSCRIPTION_INDEXES_PRIORITY = 20.0

        const val PYTHON_PRIORITY_WEIGHT = 5.0
        // < 2019.2
        // const val PYTHON_PRIORITY_WEIGHT = PythonCompletionWeigher.WEIGHT_DELTA.toDouble()
        // >= 2019.3
        // const val PYTHON_PRIORITY_WEIGHT = PythonCompletionWeigher.PRIORITY_WEIGHT.toDouble()
        
        fun createPrioritizedLookupElement(
                item: LookupElement,
                priority: Double = PYTHON_PRIORITY_WEIGHT
        ): LookupElement = PrioritizedLookupElement.withPriority(item, priority)

        fun createPrioritizedLookupElement(
                name: String,
                psiElement: PsiElement?,
                icon: Icon = PlatformIcons.PROPERTY_ICON,
                priority: Double = PYTHON_PRIORITY_WEIGHT,
                typeText: String? = null,
                insertHandler: InsertHandler<LookupElement>? = null
        ): LookupElement {
            var elementBuilder = LookupElementBuilder.create(name).withPsiElement(psiElement).withIcon(icon)

            if (typeText != null) {
                elementBuilder = elementBuilder.withTypeText(typeText)
            }

            if (insertHandler  != null) {
                elementBuilder = elementBuilder.withInsertHandler(insertHandler)
            }

            return createPrioritizedLookupElement(elementBuilder, priority)
        }
    }
}