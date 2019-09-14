package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.PlatformIcons
import com.jetbrains.python.PyNames
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementPresentation

// rule section is an 'input' or 'run' section in a Snakemake rule
// which corresponds to SmkRuleOrCheckpointArgsSection and SMKRuleRunParameter respectively
// thus the need for an interface: to group the two together
interface SmkSection: PyStatement, PsiNameIdentifierOwner {
    // , PyDocStringOwner

    val sectionKeyword: String?
        get() = getSectionKeywordNode()?.text

    override fun getNameIdentifier() = getSectionKeywordNode()?.psi

    override fun setName(name: String): PsiElement {
        //TODO: maybe allow?
        // Not clear what is expected use case for such renaming, looks like user
        // really don't want to do this
        // also see SmkFindUsagesProvider impl
        throw IncorrectOperationException("Section keyword rename not allowed.")
    }

    fun getSectionKeywordNode(): ASTNode?

    // override fun getPresentation() = getPresentation(this) // XXX see #145:
    //override fun getIcon(flags: Int) = PlatformIcons.PROPERTY_ICON!! // XXX see #145:

    fun getParentRuleOrCheckPoint(): SmkRuleOrCheckpoint? = PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpoint::class.java)!!
}

fun getIcon(section: SmkSection, flags: Int) = PlatformIcons.PROPERTY_ICON!!

fun getPresentation(section: SmkSection) = object : PyElementPresentation(section) {
    override fun getPresentableText() = section.sectionKeyword ?: PyNames.UNNAMED_ELEMENT
    override fun getLocationString(): String {
        val containingRuleLike = PsiTreeUtil.getParentOfType(section, SmkRuleLike::class.java)
        if (containingRuleLike != null) {
            val ruleLikeName = containingRuleLike.name ?: PyNames.UNNAMED_ELEMENT
            return "($ruleLikeName in ${section.containingFile.name})"
        }
        return "(${section.containingFile.name})"
    }
}