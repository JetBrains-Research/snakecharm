package com.jetbrains.snakecharm

class SnakemakeRulesNameCompletionTest : SnakemakeTestCase() {
    private fun doTest(fileExtension: String = ".smk") {
        val testName = getTestName(true)
        fixture?.testCompletion("completion/rules/$testName$fileExtension",
                "completion/rules/$testName.after$fileExtension")
    }

    private fun checkCompletionListForString(vararg strings: String) {
        val testName = getTestName(true)
        // it's necessary to do this to get the lookup first, then we can address lookup elements for the fixture
        val variants = fixture?.getCompletionVariants("completion/rules/$testName.smk") ?: emptyList()
        for (string in strings) {
            assertTrue(variants.contains(string))
        }
    }

    fun testSingleRuleNameInRules() {
        doTest()
    }

    fun testMultipleRuleNamesInRules() {
        checkCompletionListForString("aaa", "bbb", "ccc")
    }
}