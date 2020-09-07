package features.glue

import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.python.inspections.quickfix.PySuppressInspectionFix
import com.jetbrains.snakecharm.inspections.SmkInspectionsSuppressor
import io.cucumber.java.en.Then
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class SuppressSteps {
    companion object {
        const val FAKE_SUPPRESS_SMK_TOOL_ID="FAKE_SMK_QUICKFIX"
    }

    @Then("^Inspection \"(.+)\" (should|shouldn't) be suppressed$")
    fun suppressQuickFixShouldSee(toolId: String, mode:String) {
        ApplicationManager.getApplication().runReadAction {
            val elementUnderCaret = SnakemakeWorld.findPsiElementUnderCaret()
            requireNotNull(elementUnderCaret)

            val suppressedFor = SmkInspectionsSuppressor().isSuppressedFor(elementUnderCaret, toolId)
            if (mode == "should") {
               assertTrue("Inspection \"${toolId}\" should be suppressed") { suppressedFor }
            } else {
               assertFalse("Inspection \"${toolId}\" shouldn't be suppressed") { suppressedFor }
            }
        }
    }

    @Then("^should see \"(.+)\" quick fix for highlighted context:$")
    fun suppressQuickFixShouldBeRegistered(expectedQuickFixName: String, contextText: String) {
        ApplicationManager.getApplication().runReadAction {
            val elementUnderCaret = SnakemakeWorld.findPsiElementUnderCaret()
            requireNotNull(elementUnderCaret)

            val suppressor = SmkInspectionsSuppressor()
            val actions = suppressor.getSuppressActions(elementUnderCaret, FAKE_SUPPRESS_SMK_TOOL_ID)
            val filteredNames = actions.filter { expectedQuickFixName == it.toString() }
            if (filteredNames.isEmpty()) {
                fail("Cannot find \"${expectedQuickFixName}\" in: ${actions.joinToString() { it.toString() }}")
            } else {
                val quickFix = filteredNames.single()
                assertTrue(quickFix.isAvailable(elementUnderCaret.project, elementUnderCaret))
                if (contextText.isNotEmpty()) {
                    assertEquals(
                        contextText,
                        (quickFix as PySuppressInspectionFix).getContainer(elementUnderCaret)?.text ?: "N/A"
                    )
                }
            }
        }
    }

    @Then("^shouldn't see \"(.+)\" quick fix$")
    fun suppressQuickFixShouldNotBeRegistered(expectedQuickFixName: String) {
        ApplicationManager.getApplication().runReadAction {
            val elementUnderCaret = SnakemakeWorld.findPsiElementUnderCaret()
            requireNotNull(elementUnderCaret)

            val suppressor = SmkInspectionsSuppressor()
            val actions = suppressor.getSuppressActions(elementUnderCaret, FAKE_SUPPRESS_SMK_TOOL_ID)
            val filteredNames = actions.filter { expectedQuickFixName == it.toString() }
            assertTrue("Quick fix \"${expectedQuickFixName}\" not expected here") {
                filteredNames.isEmpty() || filteredNames.all {
                    !it.isAvailable(
                        elementUnderCaret.project,
                        elementUnderCaret
                    )
                }
            }
        }
    }
}