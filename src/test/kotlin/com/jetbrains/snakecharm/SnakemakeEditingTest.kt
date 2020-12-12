package com.jetbrains.snakecharm

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SnakemakeEditingTest : SnakemakeTestCase() {
    fun testEnterAfterRuleParamColon() {
        doTypingTest("\na=")
    }
    fun testEnterAfterRuleParamsIncomplete() {
        doTypingTest("\nfoo")
    }

    // TODO: fix, test for #339
    // fun testEnterAfterRuleParamsIncomplete2() {
    //     doTypingTest("\nfoo")
    // }

    private fun doTypingTest(text: String) {
//        val testDataDir = SnakemakeTestUtil.getTestDataPath().resolve("folding")
//        fixture!!.testFolding("$testDataDir/${getTestName(true)}.smk")

        val testName = "editing/" + getTestName(true)
        fixture!!.configureByFile("$testName.smk")
        fixture!!.type(text)
        fixture!!.checkResultByFile("$testName.after.smk")
    }

//    private fun doTestTyping(text: String, offset: Int, character: Char): String {
//        val file = fixture!!.configureByText(SnakemakeFileType, text)
//        fixture!!.getEditor().getCaretModel().moveToOffset(offset)
//        fixture!!.type(character)
//        return fixture!!.getDocument(file).getText()
//    }
//
//    private fun doTestEnter(before: String, after: String) {
//        var before = before
//        val pos = before.indexOf("<caret>")
//        before = before.replace("<caret>", "")
//        doTestTyping(before, pos, '\n')
//        fixture!!.checkResult(after)
//    }

}