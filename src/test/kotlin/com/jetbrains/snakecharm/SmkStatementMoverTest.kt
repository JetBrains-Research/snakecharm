package com.jetbrains.snakecharm

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.jetbrains.python.psi.LanguageLevel
import features.glue.SnakemakeWorld.myFixture

class SmkStatementMoverTest: SnakemakeTestCase() {
    private fun doTest() {
        val testName: String = getTestName(true)

        myFixture?.configureByFile("mover/$testName.smk")
        myFixture?.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_UP_ACTION)
        myFixture?.checkResultByFile("mover/" + testName + "_afterUp.smk", true)
        myFixture?.getFile()?.let { myFixture?.getDocument(it)?.let { FileDocumentManager.getInstance().reloadFromDisk(it) } }
        myFixture?.configureByFile("mover/" + getTestName(true) + ".smk")
        myFixture?.performEditorAction(IdeActions.ACTION_MOVE_STATEMENT_DOWN_ACTION)
        myFixture?.checkResultByFile("mover/" + testName + "_afterDown.smk", true)
    }

    fun testSimple() {
        doTest()
    }

    fun testCommentUp() {
        doTest()
    }

    fun testTryExcept() {
        doTest()
    }

    fun testInnerIf() {
        doTest()
    }

    fun testNestedIfUp() {
        doTest()
    }

    fun testCommentOut() {  //PY-5527
        doTest()
    }

    fun testMoveDownOut() {
        doTest()
    }

    fun testIndentedOneLine() { //PY-5268
        doTest()
    }

    fun testComment() {   //PY-5270
        doTest()
    }

    fun testOneStatementInFunction() {
        doTest()
    }

    fun testOutsideStatement() {
        doTest()
    }

    fun testInsideStatement() {
        doTest()
    }

    fun testFunctions() {
        doTest()
    }

    fun testBetweenStatementParts() {
        doTest()
    }

    fun testMoveStatement() {
        doTest()
    }

    fun testDoubleIf() {
        doTest()
    }

    fun testOneStatementInClass() {
        doTest()
    }

    fun testMoveOut() {
        doTest()
    }

    fun testSimpleBlankLines() {
        doTest()
    }

    fun testPy950() {
        doTest()
    }

    fun testIndent() {
        doTest()
    }

    fun testDecorator() {
        doTest()
    }

    fun testLastLine() { // PY-5017
        doTest()
    }

    fun testNestedBlock() { // PY-1343
        doTest()
    }

    fun testNestedBlockDown() { // PY-5221
        doTest()
    }

    fun testFunctionDown() { // PY-5195
        doTest()
    }

    fun testContinueBreak() { // PY-5193
        doTest()
    }

    fun testNestedTry() { // PY-5192
        doTest()
    }

    fun testUpInNested() { // PY-5192
        doTest()
    }

    fun testClass() { // PY-5196
        doTest()
    }

    fun testExceptElse() { // PY-6482
        doTest()
    }

    fun testOneLineCompound() { // PY-5198
        doTest()
    }

    fun testEmptyLine() { // PY-5197
        doTest()
    }

    fun testDocstring() { // PY-5203
        doTest()
    }

    fun testOneLineCompoundOutside() { // PY-5201
        doTest()
    }

    fun testFunctionBlock() { // PY-6163
        doTest()
    }

    fun testCommentIntoCompound() { // PY-6133
        doTest()
    }

    fun testEmptyLineInIf() { // PY-6271
        doTest()
    }

    fun testRemovePass() { // PY-6282
        doTest()
    }

    fun testSameLevelInIf() {
        doTest()
    }

    fun testLastComment() { // PY-6408
        doTest()
    }

    fun testLastComment1() {   //PY-6408
        doTest()
    }

    fun testMultiCompound() {   //PY-7658
        doTest()
    }

    fun testMultiLineSelection() {
        doTest()
    }

    fun testMultiLineSelection1() {
        doTest()
    }

    fun testMultiLineSelection2() {
        doTest()
    }

    fun testMultiLineSelection3() {
        doTest()
    }

    fun testMultiLineSelection4() {
        doTest()
    }

    fun testMultiLineSelection5() {             //0
        doTest()
    }

    fun testMultiLineSelection6() {
        doTest()
    }

    fun testMultiLineSelection7() {
        doTest()
    }

    fun testMultiLineSelection8() {
        doTest()
    }

    fun testMultiLineSelection9() {
        doTest()
    }

    fun testMultiLineSelection10() {
        doTest()
    }

    fun testTheSameLevelMultiple() { //PY-10947
        doTest()
    }

    fun testInsideDocComment() { //PY-11595
        doTest()
    }

    fun testOutsideFromDict() {
        doTest()
    }

    fun testSameLevelAsDict() {
        doTest()
    }

}