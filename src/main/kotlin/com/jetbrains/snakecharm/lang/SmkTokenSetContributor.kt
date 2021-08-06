package com.jetbrains.snakecharm.lang

import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PythonDialectsTokenSetContributorBase
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATOR_KEYWORD
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes.SMK_PY_REFERENCE_EXPRESSION
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SmkTokenSetContributor : PythonDialectsTokenSetContributorBase() {
    override fun getStatementTokens() = TokenSet.create(
        SmkElementTypes.WORKFLOW_ARGS_SECTION_STATEMENT,
        SmkElementTypes.WORKFLOW_LOCALRULES_SECTION_STATEMENT,
        SmkElementTypes.WORKFLOW_RULEORDER_SECTION_STATEMENT,
        SmkElementTypes.WORKFLOW_PY_BLOCK_SECTION_STATEMENT,

        SmkStubElementTypes.RULE_DECLARATION_STATEMENT,
        SmkStubElementTypes.CHECKPOINT_DECLARATION_STATEMENT,
        SmkElementTypes.RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT,

        SmkStubElementTypes.SUBWORKFLOW_DECLARATION_STATEMENT,
        SmkElementTypes.SUBWORKFLOW_ARGS_SECTION_STATEMENT,

        SmkStubElementTypes.MODULE_DECLARATION_STATEMENT,
        SmkElementTypes.MODULE_ARGS_SECTION_STATEMENT,

        SmkStubElementTypes.USE_DECLARATION_STATEMENT,
        SmkElementTypes.USE_ARGS_SECTION_STATEMENT
    )

    override fun getExpressionTokens() = TokenSet.create(
        SmkElementTypes.REFERENCE_EXPRESSION, SMK_PY_REFERENCE_EXPRESSION
    )

    /**
     * keywords not here due to highlighting issues in context where they aren't keywords any more
     * Highlighter ignores tokens remapped by filter, so adds highlighting where not needed, e.g.
     * on 'rule' identifier in run section
     */
    override fun getKeywordTokens() = TokenSet.EMPTY!!

    override fun getParameterTokens() = TokenSet.EMPTY!!

    override fun getReferenceExpressionTokens() = TokenSet.create(SMK_PY_REFERENCE_EXPRESSION)

    override fun getFunctionDeclarationTokens() = TokenSet.EMPTY!!

    override fun getUnbalancedBracesRecoveryTokens(): TokenSet {
        return WORKFLOW_TOPLEVEL_DECORATORS
    }
}