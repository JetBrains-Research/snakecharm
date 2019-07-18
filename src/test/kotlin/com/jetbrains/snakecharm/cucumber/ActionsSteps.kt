package com.jetbrains.snakecharm.cucumber

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.ide.util.gotoByName.GotoSymbolModel2
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.snakecharm.cucumber.SnakemakeWorld.findPsiElementUnderCaret
import com.jetbrains.snakecharm.cucumber.SnakemakeWorld.fixture
import com.jetbrains.snakecharm.cucumber.SnakemakeWorld.myGeneratedDocPopupText
import cucumber.api.DataTable
import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import java.io.File.separator
import java.util.regex.Pattern
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail


class ActionsSteps {
    @When("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> with message$")
    fun iExpectInspectionOn(level: String, signature: String, message: String) {
        iExpectInspectionOnIn(level, signature, signature, message)
    }

    @Given("^I expect no inspection (error|warning|info|TYPO)$")
    fun iExpectNoInspection(level: String) {
        //Fake step, do nothing "check highlighting" step will show errors in such case
        //This step just for more readable tests
    }

    @Given("^I expect inspection (error|warning|info|TYPO|weak warning) with message \"(.*)\" on")
    fun iExpectInspectionWithMessageOn(level: String, message: String, signature: String) {
        val fixedSignature = signature.replace("\r", "")
        iExpectInspectionOnIn(level, fixedSignature, fixedSignature, message)
    }

    @Given("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> in <(.+)> with message$")
    fun iExpectInspectionOnIn(level: String, text: String, signature: String, message: String) {
        require(!Pattern.compile("(^|[^\\\\])\"").matcher(message).find()) {
            "Quotes should be escaped in message: $message"
        }

        val tag = level.replace(' ', '_')
        val newText = "<$tag descr=\"$message\">$text</$tag>"
        val fixture = SnakemakeWorld.fixture()
        val psiFile = fixture.file
        val project = psiFile.project
        val document = PsiDocumentManager.getInstance(fixture.project).getDocument(fixture.file)!!
        val pos = document.text.indexOf(signature)
        assertTrue(
                pos >= 0,
                "Signature <$signature> wasn't found in the file ${psiFile.name}."

        )

        val posInSignature = signature.indexOf(text)
        assertTrue(posInSignature >= 0)

        ApplicationManager.getApplication().invokeAndWait {
            performAction(project, Runnable {
                val startPos = pos + posInSignature
                fixture.editor.document.replaceString(startPos, startPos + text.length, newText)
            })
        }
    }

    @When("^I check highlighting (errors|warnings|infos|weak warnings)$")
    fun iCheckHighlighting(type: String) {
        val fixture = SnakemakeWorld.fixture()
        when (type) {
            "errors" -> fixture.checkHighlighting(false, false, false)
            "warnings" -> fixture.checkHighlighting(true, false, false)
            "infos" -> fixture.checkHighlighting(false, true, false)
            "weak warnings" -> fixture.checkHighlighting(false, false, true)
            else -> fail("Unknown highlighting type: $type")
        }
    }

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
                text in docPopupText,
                "Expected <$text> to be in <$docPopupText>"
        )
    }

    @When("^I invoke rename with name \"(.+)\"$")
    fun iInvokeRenameWithName(newName: String) {
        ApplicationManager.getApplication().invokeAndWait {
                SnakemakeWorld.fixture().renameElementAtCaret(newName)
        }
    }

    /*@When("^I invoke quick fix ([^\\]]+)$")
    fun iInvokeQuickFix(quickFixName: String) {

    }*/

    @Then("^I invoke quick fix ([^\\]]+) and see text:")
    fun iInvokeQuickFixAndSeeText(quickFixName: String, text: String) {
        val fixture = SnakemakeWorld.fixture()
        val quickFix = fixture.getAllQuickFixes().first { it.familyName == quickFixName }
        ApplicationManager.getApplication().invokeAndWait {
            fixture.launchAction(quickFix)
        }
        fixture.checkResult(text)
    }

    @Then("^go to symbol should contain:$")
    fun completionListShouldContain(table: DataTable) {
        val names = ApplicationManager.getApplication().runReadAction(Computable {
            val model = GotoSymbolModel2(SnakemakeWorld.fixture().project)
            model.getNames(false).toList()
        })
        val expected = table.asList(String::class.java)
        assertHasElements(names, expected)
    }


    private fun findTargetElementFor(element: PsiElement, editor: Editor) =
            DocumentationManager.getInstance(element.project)
                    .findTargetElement(editor, element.containingFile, element)

    private fun generateDocumentation(generateQuickDoc: Boolean) {
        ApplicationManager.getApplication().invokeAndWait {
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
        }
    }

    companion object {
        fun performAction(project: Project, action: Runnable) {
            ApplicationManager.getApplication().runWriteAction {
                CommandProcessor.getInstance().executeCommand(
                        project, action, "SnakeCharmTestCmd", null
                )
            }
        }
    }
}