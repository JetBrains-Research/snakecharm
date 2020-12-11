package com.jetbrains.snakecharm.lang.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.formatter.PyBlock
import com.jetbrains.python.formatter.PyBlockContext
import com.jetbrains.python.formatter.PythonFormattingModelBuilder
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes.RULE_LIKE_STATEMENTS

class SnakemakeFormattingModelBuilder: PythonFormattingModelBuilder() {
    companion object {
        const val DUMP_FORMATTING_AST = false;
    }
    override fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
        // TODO API: pass python language dialect into parent  !!!
        val commonSettings = settings.getCommonSettings(SnakemakeLanguageDialect)

        //val pySettings = settings.getCustomSettings(PyCodeStyleSettings::class.java)
        // static val STATEMENT_OR_DECLARATION = PythonDialectsTokenSetProvider.getInstance().getStatementTokens()
        // static val SINGLE_SPACE_KEYWORDS = TokenSet.create(IN_KEYWORD, AND_KEYWORD, OR_KEYWORD,..)
        val klb = commonSettings.KEEP_LINE_BREAKS
        val kblic = commonSettings.KEEP_BLANK_LINES_IN_CODE

        return SpacingBuilder(commonSettings)
            .afterInside(PyTokenTypes.COMMA, PyElementTypes.ARGUMENT_LIST).spacing(0, 0, 0, klb, kblic)
            .before(PyTokenTypes.END_OF_LINE_COMMENT).spacing(2, 0, 0,  klb, kblic)
            .after(PyTokenTypes.END_OF_LINE_COMMENT).spacing(0, 0, 1,  klb, kblic)
            .around(RULE_LIKE_STATEMENTS).blankLines(commonSettings.BLANK_LINES_AROUND_METHOD)

            // XXX:  ideas what to fix:
            //.aroundInside(PyTokenTypes.EQ, PyElementTypes.ASSIGNMENT_STATEMENT).spaceIf(commonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
            //.aroundInside(PyTokenTypes.EQ, PyElementTypes.NAMED_PARAMETER).spaceIf(pySettings.SPACE_AROUND_EQ_IN_NAMED_PARAMETER)
            //.aroundInside(PyTokenTypes.EQ, PyElementTypes.KEYWORD_ARGUMENT_EXPRESSION).spaceIf(pySettings.SPACE_AROUND_EQ_IN_KEYWORD_ARGUMENT)

            // XXX: examples
            //.between(STATEMENT_OR_DECLARATION, STATEMENT_OR_DECLARATION).spacing(0,  Integer.MAX_VALUE, 1, false, 1)
            //.between(PyTokenTypes.COLON, PyElementTypes.STATEMENT_LIST).spacing(1, Integer.MAX_VALUE, 0, true, 0)
            //.afterInside(
            //    PyTokenTypes.COLON,
            //    TokenSet.create(
            //        *arrayOf<IElementType>(
            //            PyElementTypes.KEY_VALUE_EXPRESSION,
            //            PyElementTypes.LAMBDA_EXPRESSION
            //        )
            //    )).spaceIf(pySettings.SPACE_AFTER_PY_COLON)
            //.beforeInside(PyElementTypes.ANNOTATION, PyElementTypes.FUNCTION_DECLARATION).spaces(1)
            //.beforeInside(PyElementTypes.ANNOTATION, PyElementTypes.NAMED_PARAMETER).none()
            //.afterInside(PyTokenTypes.COLON, PyElementTypes.ANNOTATION).spaces(1)
            //.afterInside(PyTokenTypes.LBRACE, PyElementTypes.DICT_LITERAL_EXPRESSION).spaceIf(pySettings.SPACE_WITHIN_BRACES, pySettings.DICT_NEW_LINE_AFTER_LEFT_BRACE)
            //.withinPair(PyTokenTypes.FSTRING_FRAGMENT_START, PyTokenTypes.FSTRING_FRAGMENT_END).spaces(0)
            // #TODO: no space before column for section name:s
            //.before(PyTokenTypes.COLON).spaceIf(pySettings.SPACE_BEFORE_PY_COLON)
            //.after(PyTokenTypes.COMMA).spaceIf(commonSettings.SPACE_AFTER_COMMA)
            //.before(PyTokenTypes.COMMA).spaceIf(commonSettings.SPACE_BEFORE_COMMA)
            //.around(SINGLE_SPACE_KEYWORDS).spaces(1)
                
            .append(super.createSpacingBuilder(settings))
    }

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val element = formattingContext.psiElement
        val settings = formattingContext.codeStyleSettings

        if (DUMP_FORMATTING_AST) {
            val fileNode = element.containingFile.node;
              println("AST tree for " + element.containingFile.name + ":");
              printAST(fileNode, 0);
        }

        val context = SmkBlockContext(settings, createSpacingBuilder(settings), formattingContext.formattingMode)
        val block = PyBlock(null, element.node, null, Indent.getNoneIndent(), null, context)
        if (DUMP_FORMATTING_AST) {
            FormattingModelDumper.dumpFormattingModel(block, 2, System.out)
        }

        return FormattingModelProvider.createFormattingModelForPsiFile(element.containingFile, block, settings);
    }

    private fun printAST(node: ASTNode?, indent: Int) {
        // TODO: make super.printAST(..) public

        var currNode: ASTNode? = node
        while (currNode != null) {
            for (i in 0 until indent) {
                print(" ")
            }
            println("$currNode ${currNode.textRange}")
            printAST(currNode.firstChildNode, indent + 2)
            currNode = currNode.treeNext
        }
    }
}
class SmkBlockContext(settings: CodeStyleSettings, builder: SpacingBuilder, mode: FormattingMode)
    : PyBlockContext(settings, builder, mode) {

    private val smkSpecificCommonSettings =  settings.getCommonSettings(SnakemakeLanguageDialect);

    // Override ALIGN_MULTILINE_PARAMETERS_IN_CALLS
    override fun getSettings() = smkSpecificCommonSettings
}