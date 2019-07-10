package com.jetbrains.snakecharm.codeInsight

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

enum class SmkCodeInsightScope {
    TOP_LEVEL,
    IN_RULE_OR_CHECKPOINT;

    fun includes(second: SmkCodeInsightScope) = when (this) {
         TOP_LEVEL -> second == TOP_LEVEL
         IN_RULE_OR_CHECKPOINT -> second == TOP_LEVEL || second == IN_RULE_OR_CHECKPOINT
    }

    companion object {
        operator fun get(anchor: PsiElement) = when {
            anchor.parentOfType<SmkRuleOrCheckpoint>() != null -> IN_RULE_OR_CHECKPOINT
            else -> TOP_LEVEL
        }
    }
}

//private fun List<Pair<SMKContext, PyElement>>.filterBySnakeMakeContext(context: SMKContext) = mapNotNull { (ctx, psi) ->
//      when (context) {
//          SMKContext.IN_RULE_OR_CHECKPOINT -> psi // both contexts
//          SMKContext.TOP_LEVEL -> if (ctx == SMKContext.TOP_LEVEL) psi else null
//      }
//  }