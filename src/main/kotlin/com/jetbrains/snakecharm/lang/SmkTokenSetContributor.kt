package com.jetbrains.snakecharm.lang

import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PythonDialectsTokenSetContributorBase
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
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
            SmkElementTypes.SUBWORKFLOW_ARGS_SECTION_STATEMENT
    )

    override fun getExpressionTokens() = TokenSet.create(
            SmkElementTypes.REFERENCE_EXPRESSION
    )

    override fun getKeywordTokens() = TokenSet.orSet(
            // XXX: after changing set of keywords tokens think of highlighting them via annotator and review
            // [smk_syntax_annotator.feature]

            TokenSet.create(
                    SnakemakeTokenTypes.RULE_KEYWORD, SnakemakeTokenTypes.CHECKPOINT_KEYWORD,
                    SnakemakeTokenTypes.SUBWORKFLOW_KEYWORD

                    // other keywords not here due to highlighting issues in context where they
                    // aren't keywords any more
            )
    )

// TODO
//    override fun getParameterTokens(): TokenSet {
//        return TokenSet.create(NAMED_PARAMETER)
//    }

    override fun getUnbalancedBracesRecoveryTokens(): TokenSet {
        return WORKFLOW_TOPLEVEL_DECORATORS
    }
}