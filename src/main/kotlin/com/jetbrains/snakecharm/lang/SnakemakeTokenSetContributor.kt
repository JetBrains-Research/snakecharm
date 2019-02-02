package com.jetbrains.snakecharm.lang

import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PythonDialectsTokenSetContributorBase
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SnakemakeElementTypes

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeTokenSetContributor : PythonDialectsTokenSetContributorBase() {
    override fun getStatementTokens() = TokenSet.create(
            SnakemakeElementTypes.RULE_DECLARATION,
            SnakemakeElementTypes.RULE_PARAMETER_LIST_STATEMENT,
            SnakemakeElementTypes.WORKFLOW_PARAMETER_LIST_STATEMENT
    )

//    override fun getExpressionTokens(): TokenSet {
//        // return TokenSet.create(SnakemakeTokenTypes.RULE_KEYWORD)
//        return super.getExpressionTokens()
//    }

    override fun getKeywordTokens() = TokenSet.orSet(
            SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER,

            TokenSet.create(
                    SnakemakeTokenTypes.RULE_KEYWORD, SnakemakeTokenTypes.CHECKPOINT_KEYWORD,
                    SnakemakeTokenTypes.WORKFLOW_LOCALRULES_KEYWORD, SnakemakeTokenTypes.WORKFLOW_RULEORDER_KEYWORD
            )
    )

    //       @NotNull
//  @Override
//  public TokenSet getExpressionTokens() {
//    return TokenSet.create(REFERENCE_EXPRESSION, ADDRESS_EXPRESSION, TYPECAST_EXPRESSION, SIZEOF_EXPRESSION, NEW_EXPRESSION);
    //  }

// TODO
//    override fun getParameterTokens(): TokenSet {
//        return TokenSet.create(NAMED_PARAMETER)
//    }

    // TODO: uncomment + tests
//    override fun getUnbalancedBracesRecoveryTokens(): TokenSet {
//        return TokenSet.create(SnakemakeTokenTypes.RULE_KEYWORD)
//    }

// TODO
//    override fun getReferenceExpressionTokens(): TokenSet {
//        return TokenSet.create(REFERENCE_EXPRESSION)
//    }
}