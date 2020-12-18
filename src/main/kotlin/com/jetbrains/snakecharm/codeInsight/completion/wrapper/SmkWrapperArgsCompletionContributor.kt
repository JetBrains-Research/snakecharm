package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType

class SmkWrapperArgsCompletionContributor : CompletionContributor() {
    init {
        extend(
                CompletionType.BASIC,
                SmkWrapperArgsCompletionProvider.CAPTURE,
                SmkWrapperArgsCompletionProvider
        )
    }
}