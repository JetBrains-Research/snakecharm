package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.PlatformIcons
import com.jetbrains.python.codeInsight.completion.PythonCompletionWeigher
import javax.swing.Icon

class SmkCompletionUtil {
    companion object {
        val RULES_AND_CHECKPOINTS_PRIORITY = Double.MAX_VALUE
        const val WILDCARDS_LAMBDA_PARAMETER_PRIORITY = 1.0

        fun createPrioritizedLookupElement(
                name: String,
                icon: Icon = PlatformIcons.PROPERTY_ICON,
                typeText: String? = null,
                insertHandler: InsertHandler<LookupElement>? = null
        ): LookupElement {
            var elementBuilder = LookupElementBuilder.create(name).withIcon(icon)

            if (typeText != null) {
                elementBuilder = elementBuilder.withTypeText(typeText)
            }

            if (insertHandler  != null) {
                elementBuilder = elementBuilder.withInsertHandler(insertHandler)
            }

            return PrioritizedLookupElement.withPriority(
                    elementBuilder,
                    PythonCompletionWeigher.WEIGHT_DELTA.toDouble()
            )
        }
    }
}