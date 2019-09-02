package com.jetbrains.snakecharm.cucumber

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.cucumber.CompletionResolveSteps.Companion.getReferenceInInjectedLanguageAtOffset
import cucumber.api.DataTable
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import kotlin.test.assertEquals


class FindUsagesSteps {
//    @Given("^I enable usages highlighting$")
//    fun i_enable_usages_highlighting() {
//        SeveritiesProvider.EP_NAME.getPoint(null).registerExtension(SEVERITIES_PROVIDER)
//    }

    @Given("^I invoke find usages$")
    fun i_invoke_find_usages() {
        ApplicationManager.getApplication().invokeAndWait {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(Runnable {
                ApplicationManager.getApplication().runReadAction() {
                    val flags = TargetElementUtil.ELEMENT_NAME_ACCEPTED or TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED
                    var element = TargetElementUtil.findTargetElement(
                            SnakemakeWorld.fixture().editor, flags
                    )

                    if (element == null) {
                        element = getReferenceInInjectedLanguageAtOffset()?.resolve()
                    }

                    if (element == null) {
                        element = getReferenceInInjectedLanguageAtOffset()?.resolve()
                    }
                    
                    requireNotNull(element) {
                        "Target element not found"
                    }
                    SnakemakeWorld.myFoundUsages = SnakemakeWorld.fixture().findUsages(element).toList()
                    SnakemakeWorld.myFoundRefs = ReferencesSearch.search(element).findAll().toList()
                }
            }, "Find Usages Test", false, SnakemakeWorld.fixture().project)
        }
    }

    @Then("^find usages shows me following references:$")
    fun find_usages_shows_me_following_references(dataTable: DataTable) {
        //TODO: re-implement with SnakemakeWorld.myFoundUsages

        ApplicationManager.getApplication().runReadAction() {
            val expectedUsages = dataTable.asList(Ref::class.java)
            val actualRefs = SnakemakeWorld.myFoundRefs.sortedBy { it.element.containingFile.name }

            val actualRefsHint = "Actual refs:\n" + actualRefs.joinToString(separator = "\n") { ref ->
                val (elem, textOffset, textLength) = getOriginalElementOffsetAndLengthForPossibleInjection(ref)
                val psiFile = elem.containingFile

                val content = TextRange.from(textOffset, textLength).substring(psiFile.text)
                "|${psiFile.name}|$textOffset|$textLength|\n" +
                        "Content: <$content>"

            }
            assertEquals(
                    expectedUsages.size, actualRefs.size,
                    "The number of found usages doesn't match. $actualRefsHint"
            )

            expectedUsages.zip(actualRefs).forEach { (expected, actual) ->
                val (actualPsi, actualOffset, actualLength) = getOriginalElementOffsetAndLengthForPossibleInjection(actual)

                assertEquals(
                        expected.file, actualPsi.containingFile.name,
                        "File containing the usage is incorrect. $actualRefsHint"
                )

                val text = actualPsi.containingFile.text

                val expectedOffset = expected.offset!!.toInt()
                val expectedLength = expected.length!!.toInt()
                val expectedEndOffset = expectedOffset + expectedLength
                val expectedContent = if (expectedEndOffset <= text.length) {
                    TextRange.from(expectedOffset, expectedLength).substring(text)
                } else {
                    "Expected range out of bounds: $expectedEndOffset > ${text.length}"
                }

                val actualContent = TextRange.from(actualOffset, actualLength).substring(text)

                val errorMsg = "Element offset doesn't match. Expected content <$expectedContent>, " +
                        "actual content <$actualContent>. $actualRefsHint\n"

                assertEquals(
                        expectedOffset to expectedLength,
                        actualOffset to actualLength,
                        errorMsg
                )
            }

            assertEquals(expectedUsages.size, SnakemakeWorld.myFoundUsages.size)
        }
    }

    private fun getOriginalElementOffsetAndLengthForPossibleInjection(ref: PsiReference): Triple<PsiElement, Int, Int> {
        val element = ref.element
        val host = SnakemakeWorld.injectionFixture().injectedLanguageManager.getInjectionHost(element)

        return if (host is PyStringLiteralExpression) {
            Triple(
                    host,
                    host.stringValueTextRange.startOffset + host.textOffset + element.textOffset,
                    element.textLength
            )
        } else {
            Triple(element, element.textOffset, element.textLength)
        }
    }

    private class Ref {
        internal var file: String? = null
        internal var offset: String? = null
        internal var length: String? = null
    }

//    companion object {
//        val SEVERITIES_PROVIDER = object : SeveritiesProvider() {
//            override fun getSeveritiesHighlightInfoTypes() =
//                    listOf(ELEMENT_UNDER_CARET_READ, ELEMENT_UNDER_CARET_WRITE)
//
//            override fun isGotoBySeverityEnabled(minSeverity: HighlightSeverity?): Boolean {
//                return minSeverity == HighlightSeverity.INFORMATION
//            }
//        }
//    }
}
