package com.jetbrains.snakecharm.lang

import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PythonDialectsTokenSetContributorBase
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SnakemakeElementTypes

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeTokenSetContributor : PythonDialectsTokenSetContributorBase() {
    override fun getStatementTokens() = TokenSet.create(
            SmkStubElementTypes.RULE_DECLARATION,
            SnakemakeElementTypes.RULE_PARAMETER_LIST_STATEMENT,
            SnakemakeElementTypes.WORKFLOW_PARAMETER_LIST_STATEMENT
    )

//    override fun getExpressionTokens(): TokenSet {
//        // return TokenSet.create(SnakemakeTokenTypes.RULE_KEYWORD)
//        return super.getExpressionTokens()
//    }

    override fun getKeywordTokens() = TokenSet.orSet(
            TokenSet.create(
                    SnakemakeTokenTypes.RULE_KEYWORD, SnakemakeTokenTypes.CHECKPOINT_KEYWORD,
                    SnakemakeTokenTypes.SUBWORKFLOW_KEYWORD
                    // other keywords not here due to highlighting issues in context where they
                    // aren't keywords any more

                    // TODO:  seems this keywords also better to highlight using annotator because
                    // they are not keywords in some contexts (e.g. python code)
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