package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_DEFINING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_EXPANDING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLElement

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

interface SmkReferenceExpression : SmkReferenceWithPyIdentifier

interface SmkSLReferenceExpression : SmkReferenceWithPyIdentifier, SmkSLElement {
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
}

interface SmkReferenceWithPyIdentifier : PsiNamedElement, PyExpression {
    override fun getName(): String? = getNameNode()?.text

    fun getNameNode() = node.findChildByType(PyTokenTypes.IDENTIFIER)
    fun getNameRange(): TextRange = getNameNode()?.textRange ?: TextRange.EMPTY_RANGE

    override fun setName(name: String): PsiElement {
        val nameElement = PyUtil.createNewName(this, name)
        val nameNode = getNameNode()
        if (nameNode != null) {
            node.replaceChild(nameNode, nameElement)
        }
        return this
    }
}
