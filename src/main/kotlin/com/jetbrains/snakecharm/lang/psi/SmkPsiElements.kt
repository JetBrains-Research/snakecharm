package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBasedPsiElement
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_DEFINING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_EXPANDING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.stubs.*

interface SmkToplevelSection : SmkSection {
    override fun getParentRuleOrCheckPoint(): SmkRuleOrCheckpoint? = null
}

interface SmkRule : SmkRuleOrCheckpoint, StubBasedPsiElement<SmkRuleStub>

interface SmkCheckPoint : SmkRuleOrCheckpoint, StubBasedPsiElement<SmkCheckpointStub>

interface SmkSubworkflow : SmkRuleLike<SmkSubworkflowArgsSection>, StubBasedPsiElement<SmkSubworkflowStub>

interface SmkModule : SmkRuleLike<SmkModuleArgsSection>, StubBasedPsiElement<SmkModuleStub> {
    /**
     * Returns the PsiFile, which is defined in 'snakefile' section if there is such a local file
     */
    fun getPsiFile(): PsiFile?
}

interface SmkUse : SmkRuleOrCheckpoint, StubBasedPsiElement<SmkUseStub> {
    /**
     * Returns names and corresponded [PsiElement]s which are produced by this section.
     * [visitedFiles] is set of [PsiFile]s which are already visited.
     */
    fun getProducedRulesNames(visitedFiles: MutableSet<PsiFile> = mutableSetOf()): List<Pair<String, PsiElement>>

    /**
     * Returns [PsiElement] contains module name which imports rules
     */
    fun getModuleName(): PsiElement?

    /**
     * Returns an array of  [SmkReferenceExpression] which were defined as inherited rules
     */
    fun getDefinedReferencesOfImportedRuleNames(): Array<SmkReferenceExpression>?

    /**
     * Returns list of [SmkRuleOrCheckpoint] from defined module. Returns null if there are no module reference, module section or file
     */
    fun getImportedRules(): List<SmkRuleOrCheckpoint>?

    /**
     * Returns True if  "as" name is wildcard with '*', e.g:
     * use rule A,B from M as new_*
     */
    fun nameIdentifierIsWildcard(): Boolean
}

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

    /**
     * Checks if section argument list starts from new line
     */
    fun multilineSectionDefinition(): Boolean
}

interface SmkSubworkflowArgsSection : SmkArgsSection {
    override fun getParentRuleOrCheckPoint(): SmkRuleOrCheckpoint? = null
}

interface SmkModuleArgsSection : SmkArgsSection {
    override fun getParentRuleOrCheckPoint(): SmkRuleOrCheckpoint? = null
}

interface SmkWorkflowArgsSection : SmkArgsSection, SmkToplevelSection // PyNamedElementContainer

interface SmkRunSection : SmkSection, PyStatementListContainer, PyDocStringOwner {
    //ScopeOwner, // for control flow

    override fun getParentRuleOrCheckPoint(): SmkRuleOrCheckpoint = super.getParentRuleOrCheckPoint()!!
}

interface SmkWorkflowPythonBlockSection : SmkSection, SmkToplevelSection,
    ScopeOwner, // for control flow
    PyStatementListContainer, PyDocStringOwner

interface SmkWorkflowLocalrulesSection : PyStatement, SmkArgsSection, SmkToplevelSection
// SmkArgsSection

interface SmkWorkflowRuleorderSection : PyStatement, SmkArgsSection, SmkToplevelSection

interface SmkReferenceExpression : PyReferenceExpression {
    override fun getNameElement(): ASTNode? = node.findChildByType(PyTokenTypes.IDENTIFIER)
}