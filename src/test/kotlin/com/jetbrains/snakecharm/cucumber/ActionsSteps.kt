package com.jetbrains.snakecharm.cucumber

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.snakecharm.cucumber.SnakemakeWorld.findPsiElementUnderCaret
import com.jetbrains.snakecharm.cucumber.SnakemakeWorld.myGeneratedDocPopupText
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import java.io.File.separator
import java.util.regex.Pattern


class ActionsSteps {
    @When("^I invoke quick documentation popup$")
    fun iInvokeQuickDocumentationPopup() {
        generateDocumentation(false)
    }

    @When("^I invoke quick navigation info$")
    fun iInvokeQuickNavigationInfo() {
        // On: Ctrl + hover
        generateDocumentation(true)
    }

    @Then("^Documentation text should contain (.*)$")
    fun documentationShouldContain(text: String) {
        documentationTextShouldContain(text)
    }

    @Then("^Documentation text should contain$")
    fun documentationTextShouldContain(subStr: String) {
        var text = StringUtil.convertLineSeparators(subStr)
        val ptn = Pattern.compile(".*^(in.*smk)", Pattern.MULTILINE)
        val location = ContainerUtil.getFirstItem(StringUtil.findMatches(text, ptn))

        if (location != null) {
            val systemDependentLocation = location.replace("/", separator)
            text = text.replace(location, systemDependentLocation)
        }
        val docPopupText = myGeneratedDocPopupText
        assertNotNull(docPopupText)
        assertTrue(
                "Expected <$text> to be in <$docPopupText>",
                text in docPopupText!!
        )
    }

    private fun findTargetElementFor(element: PsiElement, editor: Editor) =
            DocumentationManager.getInstance(element.project)
                    .findTargetElement(editor, element.containingFile, element)

    private fun generateDocumentation(generateQuickDoc: Boolean) {
        UIUtil.invokeAndWaitIfNeeded(Runnable {
            val element = findPsiElementUnderCaret()
            requireNotNull(element)

            val editor = SnakemakeWorld.fixture().editor
            val targetElement = findTargetElementFor(element, editor)
            val documentationProvider = DocumentationManager.getProviderFromElement(targetElement)

            myGeneratedDocPopupText = when {
                generateQuickDoc -> documentationProvider.getQuickNavigateInfo(
                        targetElement, element
                )
                else -> documentationProvider.generateDoc(
                        targetElement, element
                )
            }
        })
    }

}