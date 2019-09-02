package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_DEFINING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_EXPANDING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLElement

interface SmkToplevelSection: SmkSection {
    override fun getParentRuleOrCheckPoint(): SmkRuleOrCheckpoint? = null
}

interface SmkRule: SmkRuleOrCheckpoint, StubBasedPsiElement<SmkRuleStub>

interface SmkCheckPoint: SmkRuleOrCheckpoint, StubBasedPsiElement<SmkCheckpointStub>

interface SmkSubworkflow: SmkRuleLike<SmkSubworkflowArgsSection>, StubBasedPsiElement<SmkSubworkflowStub>

interface SmkRuleOrCheckpointArgsSection : SmkArgsSection, PyTypedElement { // PyNamedElementContainer
    /**
     * Considers any variable as wildcard
     */
    fun isWildcardsExpandingSection() = sectionKeyword in WILDCARDS_EXPANDING_SECTIONS_KEYWORDS

    /**
     * Defines wildcards
     */
    fun isWildcardsDefiningSection() = sectionKeyword in WILDCARDS_DEFINING_SECTIONS_KEYWORDS

    override fun getParentRuleOrCheckPoint(): SmkRuleOrCheckpoint = super.getParentRuleOrCheckPoint()!!
}

interface SmkSubworkflowArgsSection: SmkArgsSection {
    override fun getParentRuleOrCheckPoint(): SmkRuleOrCheckpoint? = null
}

interface SmkWorkflowArgsSection: SmkArgsSection, SmkToplevelSection // PyNamedElementContainer

interface SmkRunSection: SmkSection, PyStatementListContainer, PyDocStringOwner {
    //ScopeOwner, // for control flow

    override fun getParentRuleOrCheckPoint(): SmkRuleOrCheckpoint = super.getParentRuleOrCheckPoint()!!
}

interface SmkWorkflowPythonBlockSection : SmkSection, SmkToplevelSection,
        ScopeOwner, // for control flow
        PyStatementListContainer, PyDocStringOwner

interface SmkWorkflowLocalrulesSection: PyStatement, SmkArgsSection, SmkToplevelSection
        // SmkArgsSection

interface SmkWorkflowRuleorderSection: PyStatement, SmkArgsSection, SmkToplevelSection

interface SmkReferenceExpression: PyExpression {
    fun getNameElement(): ASTNode? = node.findChildByType(PyTokenTypes.IDENTIFIER)
    fun getReferenceName() = getNameElement()?.text
}

interface BaseSmkSLReferenceExpression : PyReferenceExpression, SmkSLElement, PsiNameIdentifierOwner {
    fun containingRuleOrCheckpointSection(): SmkRuleOrCheckpointArgsSection? {
        val languageManager = InjectedLanguageManager.getInstance(project)
        val host = languageManager.getInjectionHost(this)
        return PsiTreeUtil.getParentOfType(host, SmkRuleOrCheckpointArgsSection::class.java)
    }

    fun containingSection(): SmkSection? {
        val languageManager = InjectedLanguageManager.getInstance(project)
        val host = languageManager.getInjectionHost(this)
        return PsiTreeUtil.getParentOfType(host, SmkSection::class.java)
    }

    override fun getNameIdentifier() = nameElement?.psi
    override fun setName(name: String) = SmkResolveUtil.renameNameNode(name, nameElement, this)

}
