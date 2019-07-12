package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyStatement

// rule section is an 'input' or 'run' section in a Snakemake rule
// which corresponds to SmkRuleOrCheckpointArgsSection and SMKRuleRunParameter respectively
// thus the need for an interface: to group the two together
interface SmkSection: PyStatement {
    // , PyDocStringOwner

    val sectionKeyword: String?
        get() = getSectionKeywordNode()?.text

    fun getSectionKeywordNode(): ASTNode?
}