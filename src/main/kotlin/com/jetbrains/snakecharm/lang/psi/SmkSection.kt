package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.util.parentOfType
import com.intellij.util.PlatformIcons
import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementPresentation

// rule section is an 'input' or 'run' section in a Snakemake rule
// which corresponds to SmkRuleOrCheckpointArgsSection and SMKRuleRunParameter respectively
// thus the need for an interface: to group the two together
interface SmkSection: PyStatement {
    // , PyDocStringOwner

    val sectionKeyword: String?
        get() = getSectionKeywordNode()?.text

    fun getSectionKeywordNode(): ASTNode?

    override fun getPresentation(): ItemPresentation? {
        return object: PyElementPresentation(this) {
            override fun getPresentableText() = sectionKeyword ?: PyNames.UNNAMED_ELEMENT
            override fun getLocationString(): String {
                val containingRuleLike = parentOfType<SmkRuleLike<*>>()
                if (containingRuleLike != null) {
                    val ruleLikeName = containingRuleLike.name ?: PyNames.UNNAMED_ELEMENT
                    return "($ruleLikeName in ${containingFile.name})"
                }
                return "(${containingFile.name})"
            }
        }
    }

    override fun getIcon(flags: Int) = PlatformIcons.PROPERTY_ICON!!
}