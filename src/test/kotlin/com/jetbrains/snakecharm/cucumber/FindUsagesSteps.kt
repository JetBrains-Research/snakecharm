package com.jetbrains.snakecharm.cucumber

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.searches.ReferencesSearch
import cucumber.api.DataTable
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import kotlin.test.assertEquals


class FindUsagesSteps {
    @Given("^I invoke find usages$")
    fun i_invoke_find_usages() {
        ApplicationManager.getApplication().invokeAndWait {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(Runnable {
                ApplicationManager.getApplication().runReadAction() {
                    val element = TargetElementUtil.findTargetElement(
                            SnakemakeWorld.fixture().editor,
                            TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED or TargetElementUtil.ELEMENT_NAME_ACCEPTED
                    )
                    requireNotNull(element)
                    SnakemakeWorld.myFoundRefs = ReferencesSearch.search(element).findAll().toList()
                }
            }, "Find Usages Test", false, SnakemakeWorld.fixture().project)
        }
    }

    @Then("^find usages shows me following references:$")
    fun find_usages_shows_me_following_references(dataTable: DataTable) {
        ApplicationManager.getApplication().runReadAction() {
            val expectedRefs = dataTable.asList(Ref::class.java)
            val actualRefs = SnakemakeWorld.myFoundRefs
            assertEquals(
                    expectedRefs.size, actualRefs.size,
                    "The number of found usages doesn't match"
            )

            val sortedActualRefs = actualRefs.sortedBy { it.element.containingFile.name }
            expectedRefs.zip(sortedActualRefs).forEach { (expected, actual) ->
                val actualPsi = actual.element

                assertEquals(
                        expected.file, actualPsi.containingFile.name,
                        "File containing the usage is incorrect"
                )

                val text = actualPsi.containingFile.text

                val expectedOffset = expected.offset!!.toInt()
                val expectedLength = expected.length!!.toInt()
                val expectedContent = TextRange.from(expectedOffset, expectedLength).substring(text)

                val actualOffset = actualPsi.textOffset
                val actualLength = actualPsi.textLength
                val actualContent = TextRange.from(actualOffset, actualLength).substring(text)

                val errorMsg = "Element offset doesn't match. Expected content <$expectedContent>, " +
                        "actual content <$actualContent>."

                assertEquals(expectedOffset, actualOffset, errorMsg)
                assertEquals(expectedLength, actualLength, errorMsg)
            }
        }
    }

    private class Ref {
        internal var file: String? = null
        internal var offset: String? = null
        internal var length: String? = null
    }
}