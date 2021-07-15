package features.glue

import com.intellij.openapi.application.runReadAction
import com.jetbrains.python.psi.PyRecursiveElementVisitor
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionIndexKeyExpressionImpl
import io.cucumber.java.en.When
import kotlin.test.assertTrue

class OnetimeUsedSteps {
    @When("^validate issue 380$")
    fun validateIssue380() {
        runReadAction {
            val psiElement = SnakemakeWorld.fixture().elementAtCaret
            require(psiElement is SmkSLSubscriptionIndexKeyExpressionImpl) {
                "Actual class: ${psiElement.javaClass.simpleName}"
            }
            require(psiElement.text.equals("proportion")) {
                "Actual text: ${psiElement.text}"
            }
            var counter = 0
            val visitor = object : PyRecursiveElementVisitor() {
                override fun visitPyReferenceExpression(node: PyReferenceExpression) {
                    counter++
                    node.qualifier?.accept(this)
                }
            }
            psiElement.accept(visitor)
            // Assert no SOE, i.e. stack not deep:
            assertTrue(counter < 5, "Actual counter: $counter")
        }
    }
}