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
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleStub
import com.jetbrains.snakecharm.lang.psi.stubs.SmkSubworkflowStub
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLElement

interface SmkRule: SmkRuleOrCheckpoint, StubBasedPsiElement<SmkRuleStub>

interface SmkCheckPoint: SmkRuleOrCheckpoint, StubBasedPsiElement<SmkCheckpointStub>

interface SmkSubworkflow: SmkRuleLike<SmkSubworkflowArgsSection>, StubBasedPsiElement<SmkSubworkflowStub>

interface SmkRuleOrCheckpointArgsSection : SmkArgsSection, PyTypedElement { // PyNamedElementContainer
    companion object {
        val EXECUTION_KEYWORDS = setOf(
                SnakemakeNames.SECTION_SHELL, SnakemakeNames.SECTION_SCRIPT,
                SnakemakeNames.SECTION_WRAPPER, SnakemakeNames.SECTION_CWL
        )

        val SINGLE_ARGUMENT_KEYWORDS = setOf(
                SnakemakeNames.SECTION_SHELL, SnakemakeNames.SECTION_SCRIPT, SnakemakeNames.SECTION_WRAPPER,
                SnakemakeNames.SECTION_CWL, SnakemakeNames.SECTION_BENCHMARK, SnakemakeNames.SECTION_VERSION,
                SnakemakeNames.SECTION_MESSAGE, SnakemakeNames.SECTION_THREADS, SnakemakeNames.SECTION_SINGULARITY,
                SnakemakeNames.SECTION_PRIORITY, SnakemakeNames.SECTION_CONDA, SnakemakeNames.SECTION_GROUP,
                SnakemakeNames.SECTION_SHADOW
        )

        val PARAMS_NAMES = setOf(
                SnakemakeNames.SECTION_OUTPUT, SnakemakeNames.SECTION_INPUT, SnakemakeNames.SECTION_PARAMS, SnakemakeNames.SECTION_LOG, SnakemakeNames.SECTION_RESOURCES,
                SnakemakeNames.SECTION_BENCHMARK, SnakemakeNames.SECTION_VERSION, SnakemakeNames.SECTION_MESSAGE, SnakemakeNames.SECTION_SHELL, SnakemakeNames.SECTION_THREADS, SnakemakeNames.SECTION_SINGULARITY,
                SnakemakeNames.SECTION_PRIORITY, SnakemakeNames.SECTION_WILDCARD_CONSTRAINTS, SnakemakeNames.SECTION_GROUP, SnakemakeNames.SECTION_SHADOW,
                SnakemakeNames.SECTION_CONDA,
                SnakemakeNames.SECTION_SCRIPT, SnakemakeNames.SECTION_WRAPPER, SnakemakeNames.SECTION_CWL
        )

        val KEYWORDS_CONTAINING_WILDCARDS = setOf(
                SnakemakeNames.SECTION_INPUT, SnakemakeNames.SECTION_OUTPUT, SnakemakeNames.SECTION_CONDA,
                SnakemakeNames.SECTION_RESOURCES, SnakemakeNames.SECTION_GROUP, SnakemakeNames.SECTION_BENCHMARK,
                SnakemakeNames.SECTION_LOG, SnakemakeNames.SECTION_PARAMS
        )

        val SECTIONS_DEFINING_WILDCARDS = setOf(
                SnakemakeNames.SECTION_OUTPUT, SnakemakeNames.SECTION_LOG, SnakemakeNames.SECTION_BENCHMARK
        )
    }

    fun isWildcardsAllowedSection() = sectionKeyword in KEYWORDS_CONTAINING_WILDCARDS
    fun isWildcardsDefiningSection() = sectionKeyword in SECTIONS_DEFINING_WILDCARDS

    fun getParentRuleOrCheckPoint() = PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpoint::class.java)!!
}


interface SmkSubworkflowArgsSection: SmkArgsSection {
    companion object {
        val PARAMS_NAMES = setOf(
                "workdir", "snakefile", "configfile"
        )
    }
}

interface SmkWorkflowArgsSection: SmkArgsSection // PyNamedElementContainer

interface SmkRunSection: SmkSection, PyStatementListContainer, PyDocStringOwner
    //ScopeOwner, // for control flow

interface SmkWorkflowPythonBlockSection : SmkSection,
        ScopeOwner, // for control flow
        PyStatementListContainer, PyDocStringOwner

interface SmkWorkflowLocalrulesSection: PyStatement, SmkArgsSection
        // SmkArgsSection

interface SmkWorkflowRuleorderSection: PyStatement, SmkArgsSection

interface SmkReferenceExpression : SmkReferenceWithPyIdentifier

interface SmkSLReferenceExpression : SmkReferenceWithPyIdentifier, SmkSLElement {
    fun containingRuleOrCheckpointSection(): SmkRuleOrCheckpointArgsSection? {
        val languageManager = InjectedLanguageManager.getInstance(project)
        val host = languageManager.getInjectionHost(this)
        return PsiTreeUtil.getParentOfType(host, SmkRuleOrCheckpointArgsSection::class.java)
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
