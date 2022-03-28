package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.StubBasedPsiElement
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyElementImpl
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
     * Returns [SmkImportedRulesNamesList] if it is declared
     */
    fun getImportedNamesList(): SmkImportedRulesNamesList?

    /**
     * Returns list of resolved [SmkRuleOrCheckpoint] from defined module. Returns null if there are no module reference, module section or file
     */
    fun getImportedRulesAndResolveThem(): List<SmkRuleOrCheckpoint>?

    /**
     * Returns a [SmkUseNewNamePattern] which may sets pattern of produced rule names or may be a simple name identifier
     *
     * Examples:
     *   `use rule N form M as A with ..` Here 'A' is use name identifier
     *   `use rule N form M as with ..` no use name identifier
     *   `use rule N form M with ..` no use name identifier
     */
    fun getNewNamePattern(): SmkUseNewNamePattern?
}

interface SmkUseNewNamePattern : PyElement {
    /**
     * Returns True if  "as" name is wildcard with '*', e.g:
     * use rule A,B from M as new_*
     */
    fun isWildcard(): Boolean
    fun getNameBeforeWildcard(): PsiElement
}

interface SmkImportedRulesNamesList : PyElement {
    /**
     * Returns array of explicitly declared [SmkReferenceExpression]
     */
    fun arguments(): Array<SmkReferenceExpression>?

    /**
     * Returns names of explicitly declared [SmkReferenceExpression]
     */
    fun argumentsNames(): List<String>?

    /**
     * Resolves explicitly declared [SmkReferenceExpression]
     */
    fun resolveArguments(): List<SmkRuleOrCheckpoint>?
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