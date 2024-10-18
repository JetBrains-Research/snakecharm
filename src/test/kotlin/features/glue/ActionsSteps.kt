package features.glue

import com.google.common.collect.ImmutableMap
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.icons.AllIcons
import com.intellij.ide.util.gotoByName.GotoSymbolModel2
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.snakecharm.FakeSnakemakeInjector
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperCrawler
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.inspections.SmkUnrecognizedSectionInspection
import com.jetbrains.snakecharm.lang.highlighter.SmkColorSettingsPage
import com.jetbrains.snakecharm.lang.highlighter.SnakemakeSyntaxHighlighterAttributes
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.stringLanguage.lang.highlighter.SmkSLSyntaxHighlighter
import features.glue.SnakemakeWorld.findPsiElementUnderCaret
import features.glue.SnakemakeWorld.fixture
import features.glue.SnakemakeWorld.getOffsetUnderCaret
import features.glue.SnakemakeWorld.myFixture
import features.glue.SnakemakeWorld.myGeneratedDocPopupText
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.Assert
import java.io.File.separator
import java.util.regex.Pattern
import javax.swing.Icon
import kotlin.test.*


class ActionsSteps {
    @When("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> with message$")
    fun iExpectInspectionOn(level: String, signature: String, message: String) {
        iExpectInspectionOnIn(level, signature, signature, message)
    }

    @When("^I expect inspection (error|warning|info|TYPO|weak warning) on pattern <([^>]+)> with message$")
    fun iExpectInspectionOnPattern(level: String, pattern: String, message: String) {
        val signature = pattern
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
        iExpectInspectionOnIn(level, signature, signature, message)
    }

    @Given("^I expect no inspection (error|warning|info|TYPO|weak warning)s$")
    fun iExpectNoInspection(level: String) {
        //Fake step, do nothing "check highlighting" step will show errors in such case
        //This step just for more readable tests
        require(SnakemakeWorld.myInspectionProblemsCounts == null) {
            "Do not mix expected inspection steps with expect no inspections step. " +
                    "Other inspections: ${SnakemakeWorld.myInspectionProblemsCounts!!.entries}"
        }

        SnakemakeWorld.myInspectionProblemsCounts = ImmutableMap.of(
            level, -1
        )
    }

    @Given("^I expect inspection (error|warning|info|TYPO|weak warning) with message \"(.*)\" on")
    fun iExpectInspectionWithMessageOn(level: String, message: String, signature: String) {
        val fixedSignature = signature.replace("\r", "")
        iExpectInspectionOnIn(level, fixedSignature, fixedSignature, message)
    }

    @Given("^I expect reference highlighting on \"(.+)\"")
    fun iExpectReferenceHighlighting(signature: String) {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = fixture()
            val psiFile = fixture.file
            val document = PsiDocumentManager.getInstance(fixture.project).getDocument(psiFile)!!
            val pos = document.text.indexOf(signature)
            assertTrue(
                pos >= 0,
                "Signature <$signature> wasn't found in the file ${psiFile.name}."

            )
            val reference = fixture.getReferenceAtCaretPosition()
            assertNotNull(reference, message = "There is no reference at the caret position")
            assertEquals(
                signature,
                reference.canonicalText,
                message = "Expected highlighted text wasn't equal to the actual one."
            )
        }
    }


    @Given("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> in <(.+)> with message$")
    fun iExpectInspectionOnIn(level: String, text: String, signature: String, message: String) {
        wrapTextInHighlightingTags(level, text, signature, message)
    }

    @When("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> with messages$")
    fun iExpectInspectionsOnWithMessages(level: String, signature: String, messages: List<String>) {
        iExpectInspectionsOnInWithMessages(level, signature, signature, messages)
    }

    @Given("^I expect inspection (error|warning|info|TYPO|weak warning) on <([^>]+)> in <(.+)> with messages$")
    fun iExpectInspectionsOnInWithMessages(level: String, text: String, signature: String, messages: List<String>) {
        messages.forEach { message ->
            wrapTextInHighlightingTags(level, text, signature, message, true)
        }
    }

    @Then("^I expect marker of overriding section with references:\$")
    fun iExpectMarkerOfOverridingSectionWithReferences(table: DataTable) {
//        val gutters = fixture().findAllGutters()
//        for(gutter in gutters){
//            println(gutter.tooltipText)
//        }
        checkLineMarkersWithIconOnTheElement(AllIcons.Gutter.OverridingMethod, table)
    }

    @Then("^I expect marker of overridden section with references:\$")
    fun iExpectMarkerOfOverriddenSectionWithReferences(table: DataTable) {
        checkLineMarkersWithIconOnTheElement(AllIcons.Gutter.OverridenMethod, table)
    }

    @Then("^I expect no markers")
    fun iExpectNoMarkers() {
        checkLineMarkersWithIconOnTheElement(null, DataTable.emptyDataTable(), true)
    }

    private fun checkLineMarkersWithIconOnTheElement(icon: Icon?, table: DataTable, expectNoGutters: Boolean = false) {
        fixture().doHighlighting()
        var currentElement: PsiElement? = null
        val application = ApplicationManager.getApplication()
        val targetRulesOrCheckpoints = mutableListOf<SmkRuleOrCheckpoint>()
        application.invokeAndWait({
            application.runReadAction {
                // Firstly, we're reading current rule or checkpoint name
                // And all its gutter icons
                currentElement = fixture().file.findElementAt(getOffsetUnderCaret())
                targetRulesOrCheckpoints.addAll(
                    ((fixture().findGuttersAtCaret()
                        .firstOrNull { it.icon == icon } as? LineMarkerInfo.LineMarkerGutterIconRenderer<*>)?.lineMarkerInfo?.navigationHandler as? NavigationGutterIconRenderer)?.targetElements?.map { it as SmkRuleOrCheckpoint }
                        ?: emptyList())
            }
        }, ModalityState.nonModal())
        // Checks, that there are an appropriate number of markers
        if (expectNoGutters) {
            assertTrue(targetRulesOrCheckpoints.isEmpty(), "${currentElement?.text} should not have markers.")
            return
        } else {
            assertTrue(targetRulesOrCheckpoints.isNotEmpty(), "${currentElement?.text} has no appropriate markers.")
        }
        // Finally, we check if all defined targets are in the marker list
        // And if there are no targets missed in the test
        application.invokeAndWait({
            application.runWriteAction {
                for (marker in table.asLists()) {
                    val appropriateTargetRule =
                        targetRulesOrCheckpoints.find { rule -> rule.name == marker.first() && rule.containingFile.name == marker.last() }
                    assertNotNull(
                        appropriateTargetRule,
                        "Missed target in ${currentElement?.text}: ${marker.first()} in the file ${marker.last()}}"
                    )
                    targetRulesOrCheckpoints.remove(appropriateTargetRule)
                }
                assertTrue(
                    targetRulesOrCheckpoints.isEmpty(),
                    "Extras: ${targetRulesOrCheckpoints.joinToString { "${it.name} in file ${it.containingFile.name}" }}"
                )
            }
        }, ModalityState.nonModal())
    }

    @Then("^I expect the tag highlighting to be the same as the annotator highlighting$")
    fun iExpectTheTagHighlightingToBeTheSameAsTheAnnotatorHighlighting() {
        val level = "info"
        val attributesToCheck = arrayOf(
            // Currently, IDK how to check all used tags
            // Snakemake:
            SnakemakeSyntaxHighlighterAttributes.SMK_KEYWORD,
            SnakemakeSyntaxHighlighterAttributes.SMK_FUNC_DEFINITION,
            SnakemakeSyntaxHighlighterAttributes.SMK_DECORATOR,
            SnakemakeSyntaxHighlighterAttributes.SMK_PREDEFINED_DEFINITION,
            SnakemakeSyntaxHighlighterAttributes.SMK_KEYWORD_ARGUMENT,
            // SnakemakeSL:
            SmkSLSyntaxHighlighter.HIGHLIGHTING_WILDCARDS_KEY
        )
        val page = SmkColorSettingsPage()
        val fixture = fixture()
        val tagPattern = Pattern.compile("<(.+)>(\\w+)</(\\1)>")
        val itemPattern = Pattern.compile(">(\\w+)<")
        val linesPage = page.demoText.lines()
        var problemsCounter = 0
        for (ind in 0 until linesPage.count()) {
            val lineMatcher = tagPattern.matcher(linesPage[ind])
            while (lineMatcher.find()) {
                val itemMatcher = itemPattern.matcher(lineMatcher.group(0))
                if (itemMatcher.find()) {
                    val item = itemMatcher.group(1)
                    val tag = lineMatcher.group(1)
                    val attributesKey = page.additionalHighlightingTagToDescriptorMap[tag] ?: continue
                    if (attributesKey in attributesToCheck) {
                        ++problemsCounter
                        wrapTextInHighlightingTags(
                            level,
                            item,
                            fixture.editor.document.text.lines()[ind],
                            attributesKey.externalName
                        )
                    }
                }
            }
        }
        if (SnakemakeWorld.myInspectionProblemsCounts == null) {
            SnakemakeWorld.myInspectionProblemsCounts = mutableMapOf(level to problemsCounter)
        } else {
            val counts = SnakemakeWorld.myInspectionProblemsCounts!!
            counts[level] = problemsCounter
        }
    }

    private fun updatedInspectionProblemsCounter(level: String) {
        if (SnakemakeWorld.myInspectionProblemsCounts == null) {
            SnakemakeWorld.myInspectionProblemsCounts = mutableMapOf(level to 1)
        } else {
            val counts = SnakemakeWorld.myInspectionProblemsCounts!!
            counts[level] = counts.getOrDefault(level, 0) + 1
        }

    }

    private fun wrapTextInHighlightingTags(
        highlightingLevel: String,
        text: String,
        signature: String,
        message: String,
        searchInTags: Boolean = false,
    ) {
        require(!Pattern.compile("(^|[^\\\\])\"").matcher(message).find()) {
            "Quotes (\") should be escaped (with \\) in message: $message"
        }

        val tag = highlightingLevel.replace(' ', '_')
        val newText = "<$tag descr=\"$message\">$text</$tag>"
        val fixture = fixture()
        val psiFile = fixture.file
        val project = psiFile.project
        val document = PsiDocumentManager.getInstance(fixture.project).getDocument(fixture.file)!!

        val posInsideTags = if (searchInTags) findTextPositionInsideTags(tag, text, document.text) else -1
        val startPos = if (posInsideTags == -1) {
            findTextPositionWithSignature(text, signature, document, psiFile)
        } else {
            posInsideTags
        }

        updatedInspectionProblemsCounter(highlightingLevel)
        ApplicationManager.getApplication().invokeAndWait {
            var annotation = newText
            val smkLangLevel = SmkSupportProjectSettings.getInstance(fixture.project).snakemakeLanguageVersion
            if (smkLangLevel != null) {
                annotation = annotation.replace("CURR_SMK_LANG_VERS", smkLangLevel)
            }
            performAction(project) {
                fixture.editor.document.replaceString(
                    startPos, startPos + text.length, annotation
                )
            }
        }
    }

    private fun findTextPositionInsideTags(
        tag: String,
        text: String,
        fullText: String,
    ): Int {
        val textInsideTagsPattern = Pattern.compile("<$tag descr=.+?>([^<>]+)</$tag>")
        val matcher = textInsideTagsPattern.matcher(fullText)
        return if (matcher.find() && matcher.group(1) == text) matcher.start(1) else -1
    }

    private fun findTextPositionWithSignature(
        text: String,
        signature: String,
        document: Document,
        psiFile: PsiFile,
    ): Int {
        val pos = document.text.indexOf(signature)
        assertTrue(
            pos >= 0,
            "Signature <$signature> wasn't found in the file ${psiFile.name}."
        )

        val posInSignature = signature.indexOf(text)
        assertTrue(posInSignature >= 0)
        return pos + posInSignature
    }

    @When("^I check highlighting (error|warning|info|weak warning)s$")
    fun iCheckHighlighting(level: String) {
        checkHighlighting(level, false)
    }

    @When("^I check highlighting (error|warning|info|weak warning)s ignoring extra highlighting$")
    fun iCheckHighlightingIgnoreExtra(type: String) {
        checkHighlighting(type, true)
    }

    private fun checkHighlighting(level: String, ignoreExtra: Boolean) {
        val problemsCounts = SnakemakeWorld.myInspectionProblemsCounts
        SnakemakeWorld.myInspectionProblemsCounts =
            null // reset counter after check in order to validate in teardown that assertion step was called

        requireNotNull(problemsCounts) {
            "No expected inspections steps in test. Add 'I expect no inspection ..' step if no inspection" +
                    " should be triggered."
        }
        val acceptedLevels = setOf(level, "error", "TYPO").sorted()
        require(acceptedLevels.mapNotNull { problemsCounts[it] }.isNotEmpty()) {
            "Noting to check for severity level '$level'. Expected at least 1 inspection with severity: $acceptedLevels." +
                    " Test expects inspections problems: ${problemsCounts.entries}. "
        }

        val fixture = fixture()
        ApplicationManager.getApplication().invokeAndWait {
            when (level) {
                "error" -> fixture.checkHighlighting(false, false, false, ignoreExtra)
                "warning" -> fixture.checkHighlighting(true, false, false, ignoreExtra)
                "info" -> fixture.checkHighlighting(false, true, false, ignoreExtra)
                "weak warning" -> fixture.checkHighlighting(false, false, true, ignoreExtra)
                else -> fail("Unknown highlighting type: $level")
            }
        }
        SnakemakeWorld.myInspectionChecked = true
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

    @Then("^Documentation text should be equal to (.*)")
    fun documentationTextShouldBeEqualTo(str: String) {
        documentationTextShouldBeEqual(str)
    }

    @Then("^Documentation text should be equal to$")
    fun documentationTextShouldBeEqual(str: String) {
        val text = StringUtil.convertLineSeparators(str)
        val docPopupText = myGeneratedDocPopupText
        assertNotNull(docPopupText)
        assertEquals(text, docPopupText, "Expected <$text> to be equal to  <$docPopupText>")
    }

    @Then("^Documentation text should contain a substring: (.*)$")
    fun documentationShouldContain(text: String) {
        documentationTextShouldContain(text)
    }
    @Then("^Documentation text should contain substrings:$")
    fun documentationShouldContainItems(table: DataTable) {
        val substrings = table.asList()
        substrings.forEach { substring -> documentationTextShouldContain(substring) }
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

    @When("^I invoke rename with name \"([^\"]+)\"$")
    fun iInvokeRenameWithName(newName: String) {
        ApplicationManager.getApplication().invokeAndWait {
            fixture().renameElementAtCaret(newName)
        }
    }

    @When("^I invoke rename with name \"(.+)\" and get error \"(.+)\"$")
    fun iInvokeRenameWithNameAndGetError(newName: String, expectedError: String) {
        ApplicationManager.getApplication().invokeAndWait {
            var actualError: String? = null
            try {
                fixture().renameElementAtCaret(newName)
            } catch (e: RuntimeException) {
                if (e.cause is IncorrectOperationException) {
                    actualError = e.cause!!.message
                } else {
                    throw e
                }
            }
            assertNotNull(actualError, "Error not occurred")
            assertEquals(expectedError, actualError)
        }
    }

    @Then("^I invoke quick fix ([^\\]]+) and see text:")
    fun iInvokeQuickFixAndSeeText(quickFixFamilyName: String, text: String) {
        val quickFix = findQuickFix(quickFixFamilyName)

        ApplicationManager.getApplication().invokeAndWait {
            fixture().launchAction(quickFix)
        }
        fixture().checkResult(text.replace("\r", ""))
    }

    @Then("^I see available quick fix: ([^\\]]+)")
    fun iSeeAvailableQuickFixt(quickFixFamilyName: String) {
        findQuickFix(quickFixFamilyName)
        // already not-null, if <null> find quick fix will provoid user-friendly message
    }

    private fun findQuickFix(quickFixFamilyName: String): IntentionAction {
        require(SnakemakeWorld.myInspectionChecked) {
            "First call step: I check highlighting ..."
        }
        val allQuickFixes = fixture().getAllQuickFixes()
        return allQuickFixes.firstOrNull { it.familyName == quickFixFamilyName }
            ?: fail(
                "Cannot find quickfix '${quickFixFamilyName}', available quick fixes:[\n${
                    allQuickFixes.joinToString(separator = "\n") { it.familyName }
                }\n]"
            )
    }

    @Given("^I emulate quick fix apply: ignore unresolved item '(.*)'")
    fun applyFixAddIgnoredElementManually(sectionName: String) {
        val list = (LocalInspectionEP.LOCAL_INSPECTION.extensionList
            .first { it.shortName == "SmkUnrecognizedSectionInspection" }
            .instance as SmkUnrecognizedSectionInspection).ignoredItems
        if (sectionName in list) {
            fail("Section \"${sectionName}\" is already here, but it shouldn't be")
        }
        list.add(sectionName)
    }

    @Then("^go to symbol should contain:$")
    fun completionListShouldContain(table: DataTable) {
        val names = ApplicationManager.getApplication().runReadAction(Computable {
            val disposable = Disposer.newDisposable()
            val model = GotoSymbolModel2(fixture().project, disposable)
            val namesList = model.getNames(false).toList()
            disposable.dispose()
            namesList
        })
        val expected = table.asList()
        assertHasElements(names, expected)
    }


    @Then("^I expect backward brace matching before \"(.+)\"")
    fun iExpectBraceMatchingBefore(str: String) {
        iExpectBraceMatchingAt(str, false)
    }

    @Then("^I expect forward brace matching after \"(.+)\"")
    fun iExpectBraceMatchingAfter(str: String) {
        iExpectBraceMatchingAt(str, true)
    }

    private fun iExpectBraceMatchingAt(str: String, forward: Boolean) {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = fixture()
            var offset = fixture.file.text.indexOf(str)

            if (forward) {
                offset += str.length - 1
            }

            val matchOffset = BraceMatchingUtil.getMatchedBraceOffset(fixture.editor, forward, fixture.file)
            assertEquals(offset, matchOffset)
        }
    }

    @Then("^I expect language injection on \"(.*)\"")
    fun iExpectLanguageInjectionOn(str: String) {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = SnakemakeWorld.injectionFixture()
            val injectedFile = fixture.injectedElement?.containingFile
            assertNotNull(injectedFile, "No language was injected at caret position")
            assertEquals(str, injectedFile.text)
        }
    }

    @Then("^I expect language (.+) injection on \"(.*)\"")
    fun iExpectCertainLanguageInjectionOn(language: String, str: String) {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = SnakemakeWorld.injectionFixture()
            val injectedFile = fixture.injectedElement?.containingFile
            assertNotNull(injectedFile, "No language was injected at caret position")
            assertEquals(language, injectedFile.language.displayName)
            assertEquals(str, injectedFile.text)
        }
    }

    @Then("^I inject SmkSL at a caret")
    fun iInjectSmkSLAtCaret() {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = fixture()
            val offsetUnderCaret = SnakemakeWorld.getOffsetUnderCaret()
            val elementAtOffset = fixture.file.findElementAt(offsetUnderCaret)
            requireNotNull(elementAtOffset) { "No element at a caret offset: $offsetUnderCaret" }

            // auto unregister when fixture is disposed
            InjectedLanguageManager.getInstance(fixture.project).registerMultiHostInjector(
                FakeSnakemakeInjector(offsetUnderCaret), fixture.projectDisposable
            )
        }
    }

    @Given("^I invoke (EditorCodeBlockStart|EditorCodeBlockEnd) action$")
    fun iInvokeCodeBlockSelectionAction(actionId: String) {
        ApplicationManager.getApplication().invokeAndWait({
            ApplicationManager.getApplication().runWriteAction {
                myFixture?.performEditorAction(actionId)
            }
        }, ModalityState.nonModal())
    }

    @Given("^I expect caret (at|after) (.+)$")
    fun iExpectCaretAtAfterStart(placeHolder: String, marker: String) {
        ApplicationManager.getApplication().invokeAndWait({
            val editor = fixture().editor
            val caretModel: CaretModel = editor.caretModel

            val pos = CompletionResolveSteps.getPositionBySignature(
                editor, marker, "after" == placeHolder
            )

            assertEquals(pos, caretModel.offset)
        }, ModalityState.nonModal())
    }

    @Then("^I expect no language injection")
    fun iExpectNoLanguageInjection() {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = SnakemakeWorld.injectionFixture()
            val element = fixture.injectedElement
            assertNull(element, "${element?.language} language was injected at caret position")
        }
    }

    @Given("^I invoke (MoveStatementUp|MoveStatementDown) action$")
    fun iInvokeLineMoverAction(actionId: String) {
        // See ids in: IdeActions
        ApplicationManager.getApplication().invokeAndWait({
            ApplicationManager.getApplication().runWriteAction {
                myFixture?.performEditorAction(actionId)
            }
        }, ModalityState.nonModal())
    }

    @Given("^editor content will be$")
    fun editorContentWillBe(text: String) {
        ApplicationManager.getApplication().invokeAndWait({
            ApplicationManager.getApplication().runWriteAction {
                fixture().checkResult(text)
            }
        }, ModalityState.nonModal())
    }

    private fun findTargetElementFor(element: PsiElement, editor: Editor) =
        // TODO: See new API in JavaDocumentationTest,HtmlDocumentationTest
        DocumentationManager.getInstance(element.project)
            .findTargetElement(editor, element.containingFile, element)

    private fun generateDocumentation(generateQuickDoc: Boolean) {
        ApplicationManager.getApplication().invokeAndWait {
            val element = findPsiElementUnderCaret()
            requireNotNull(element)

            val editor = fixture().editor
            val targetElement = findTargetElementFor(element, editor)!!
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

    @When("^Parse wrapper args for \"meta.yaml\" and \"wrapper.(py|r)\" result is:$")
    fun checkWrapperArgsParsing(ext: String, text: String) {
        ApplicationManager.getApplication().invokeAndWait {
            val fixture = fixture()

            val wrapperFileContent = fixture.findFileInTempDir("wrapper.$ext")?.let {
                FileDocumentManager.getInstance().getDocument(it)!!.text
            } ?: ""

            val metaYamlContent = fixture.findFileInTempDir("meta.yaml")?.let {
                FileDocumentManager.getInstance().getDocument(it)!!.text
            } ?: ""

            val api = SnakemakeApiService.getInstance(fixture.project)
            val info = SmkWrapperCrawler.collectWrapperInfo(
                "wrapper", wrapperFileContent, ext, metaYamlContent, api.getAllPossibleRuleOrCheckpointArgsSectionKeywords()
            )

            val args = info.args
            val mapped = args.keys.sorted().joinToString(separator = "\n") { key ->
                val values = args[key]!!
                "$key:${values.joinToString(", ", prefix = "(", postfix = ")") { "'$it'" }}"
            }

            Assert.assertEquals(StringUtil.convertLineSeparators(text.trim()), mapped.trim())
        }
    }

    @Then("^I check ignored element <([^>]+)>")
    fun checkIgnoredElementInInspectionList(el: String) {
        Assert.assertTrue(el in (LocalInspectionEP.LOCAL_INSPECTION.extensionList
            .first { it.shortName == "SmkUnrecognizedSectionInspection" }
            .instance as SmkUnrecognizedSectionInspection).ignoredItems)
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