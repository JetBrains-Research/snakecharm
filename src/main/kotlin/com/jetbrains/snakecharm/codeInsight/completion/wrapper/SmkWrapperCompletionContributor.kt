package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.codeInsight.completion.*

class SmkWrapperCompletionContributor : CompletionContributor() {
    init {
        extend(
                CompletionType.BASIC,
                SmkWrapperCompletionProvider.CAPTURE,
                SmkWrapperCompletionProvider
        )
    }
}
