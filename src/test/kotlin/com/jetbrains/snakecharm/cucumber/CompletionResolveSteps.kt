package com.jetbrains.snakecharm.cucumber

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.snakecharm.cucumber.SnakemakeWorld.getOffsetUnderCaret
import cucumber.api.DataTable
import cucumber.api.java.en.Then
import cucumber.api.java.en.When
import junit.framework.TestCase
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-09
 */
class CompletionResolveSteps {
    @Then("^reference should not resolve$")
    fun referenceShouldNotResolve() {
        ApplicationManager.getApplication().invokeAndWait({
            val ref = getReferenceAtOffset()
            assertNotNull(ref)
            assertUnresolvedReference(ref)
        }, ModalityState.NON_MODAL)
    }


    @When("^I put the caret at (.+)$")
    fun iPutCaretAt(marker: String) {
        ApplicationManager.getApplication().invokeAndWait({
            val editor = SnakemakeWorld.fixture().editor
            val position = getPositionBySignature(editor, marker, false)
            editor.caretModel.moveToOffset(position)
        }, ModalityState.NON_MODAL)
    }

    @When("^I put the caret after (.+)$")
    fun iPutCaretAfter(marker: String) {
        ApplicationManager.getApplication().invokeAndWait({
            val editor = SnakemakeWorld.fixture().editor
            val position = getPositionBySignature(editor, marker, true)
            editor.caretModel.moveToOffset(position)
        }, ModalityState.NON_MODAL)
    }


    @Then("^reference should resolve to \"(.+)\" in \"(.+)\"$")
    fun referenceShouldResolveToIn(marker: String, file: String) {
        ApplicationManager.getApplication().runReadAction {
            val ref = getReferenceAtOffset()
            assertNotNull(ref)
            val result = resolve(ref)
            assertNotNull(result)
            val containingFile = result.containingFile
            assertNotNull(containingFile)
            assertEquals(file, containingFile.name)

            val text = TextRange.from(result.textOffset, marker.length).substring(containingFile.text)
            assertEquals(marker, text)
        }
    }

    @Then("^reference should multi resolve to name, file, times\\[, class name\\]$")
    fun referenceShouldMultiResolveToIn(table: DataTable) {
        ApplicationManager.getApplication().runReadAction {
            val ref = getReferenceAtOffset()
            assertNotNull(ref)

            val results = multiResolve(ref)

            assertEquals(0, results.filter { it == null }.size)
            assertEquals(0, results.filter { it!!.containingFile == null }.size)

            val completionListKey2 = results
                    .map { result ->
                        when (result){
                            is PsiNamedElement -> {
                                "${result.name}"
                                // "${result.name} (${result.textRange})"
                            }
                            else -> result!!.text
                        } to result.containingFile.name
                    }
                    .groupBy { it }
                    .map { entry -> entry.key to entry.value.size }
                    .toMap()

            val completionListKey3 = results
                    .map { result ->
                        Triple(when (result) {
                            is PsiNamedElement -> {
                                "${result.name}"
                                // "${result.name} (${result.textRange})"
                            }
                            else -> result!!.text
                        }, result.javaClass.simpleName, result.containingFile.name)
                    }
                    .groupBy { it }
                    .map { entry -> entry.key to entry.value.size }
                    .toMap()

            val records = table.asLists(String::class.java)

            val actualRefsInfo = completionListKey3.entries.joinToString(separator = "\n") { (nameAndFile, times) ->
                "|${nameAndFile.first}| ${nameAndFile.third} | $times | ${nameAndFile.second} |"
            }

            records.forEach { row ->
                val expectedTimes = row[2].toInt()
                val expectedClassName = if (row.size >= 4) row[3] else null

                val (key, actualTimes) = if (expectedClassName == null) {
                    val key = row[0] to row[1]
                    key to completionListKey2.getOrDefault(key, 0)
                } else {
                    val key = Triple(row[0], expectedClassName, row[1])
                    key to completionListKey3.getOrDefault(key, 0)
                }

                assertEquals(
                        actualTimes, expectedTimes,
                        "Expected $expectedTimes but was $actualTimes occurrences of $key." +
                                " Actual refs:\n$actualRefsInfo\n"
                )
            }
        }
    }

    @Then("^reference should not multi resolve to files$")
    fun referenceShouldNotMultiResolveToIn(table: DataTable) {
        ApplicationManager.getApplication().runReadAction {
            val ref = getReferenceAtOffset()
            assertNotNull(ref)

            val results = multiResolve(ref)

            assertEquals(0, results.filter { it == null }.size)
            assertEquals(0, results.filter { it!!.containingFile == null }.size)

            val completionList = results.map { it!!.containingFile.name }
            val actualRefsInfo = results.joinToString(separator = "\n") {el ->
                "|${el!!.containingFile.name} | # ${ if (el is PsiNamedElement) el.name else el.text}"
            }

            table.asList(String::class.java).forEach { fileName ->
                assertTrue(
                        fileName !in completionList,
                        "Expected no items from $fileName in completion list." +
                                " Actual refs:\n$actualRefsInfo\n"
                )
            }
        }
    }


    @When("^I invoke autocompletion popup$")
    fun iInvokeAutocompletionPopup() {
        Registry.get("ide.completion.variant.limit").setValue(10000)
        doComplete()
    }

    @Then("^completion list should contain:$")
    fun completionListShouldContain(table: DataTable) {
        completionListShouldContainMethods(table.asList(String::class.java))
    }

    @Then("^completion list should contain items (.+)$")
    fun completionListShouldContainMethods(lookupItems: List<String>) {
        assertHasElements(SnakemakeWorld.completionList(), lookupItems)
    }

    @Then("^completion list shouldn't contain:$")
    fun completionListShouldNotContain(table: DataTable) {
        val lookupStrings = table.asList(String::class.java)
        assertNotInCompletionList(SnakemakeWorld.completionList(), lookupStrings)
    }

    @Then("^I invoke autocompletion popup, select \"([^\"]+)\" lookup item and see a text:")
    fun iInvokeAutocompletionPopupAndSelectItem(lookupText: String, text: String) {
        autoCompleteAndCheck(lookupText, text, Lookup.NORMAL_SELECT_CHAR)
    }

    @Then("^I invoke autocompletion popup, select \"([^\"]+)\" lookup item in (normal|replace|statement|auto) mode and see a text:$")
    fun iInvokeAutocompletionPopupAndSelectItemWithChar(lookupText: String, mode: String, text: String) {
        val ch = when (mode) {
            "normal" -> Lookup.NORMAL_SELECT_CHAR
            "replace" -> Lookup.REPLACE_SELECT_CHAR
            "statement" -> Lookup.COMPLETE_STATEMENT_SELECT_CHAR
            "auto" -> Lookup.AUTO_INSERT_SELECT_CHAR
            else -> error("Unsupported mode: $mode")
        }

        autoCompleteAndCheck(lookupText, text, ch)
    }

    private fun autoCompleteAndCheck(lookupText: String, text: String, ch: Char) {
        iInvokeAutocompletionPopup()

        val fixture = SnakemakeWorld.fixture()
        val lookupElements = fixture.lookupElements?.filterNotNull()?.toTypedArray()


        ApplicationManager.getApplication().invokeAndWait(
                {
                    checkCompletionResult(
                            LookupFilter.create(lookupText), lookupElements,
                            fixture, false,
                            StringUtil.convertLineSeparators(text),
                            ch
                    )
                },
                ModalityState.NON_MODAL)
    }
    /*
    @When("^I press Enter$")
    fun iPressEnter() {
        // TODO: SnakemakeWorld.fixture().type("") ?

        ApplicationManager.getApplication().runWriteAction {
            val fixture = SnakemakeWorld.fixture()
            val project = fixture.project
            val action = Runnable {
                fixture.performEditorAction("EditorEnter")
            }
            CommandProcessor.getInstance().executeCommand(project, action, "SnakeCharmTestCmd", null)
        }
    }

    @When("^I press Space$")
    fun iPressSpace() {
        // TODO: SnakemakeWorld.fixture().type("") ? + ApplicationManager.getApplication().invokeAndWait( ..)
        SwingUtilities.invokeAndWait() {
//        SwingUtilities.invokeLater {
            SnakemakeWorld.fixture().type(' ')
        }
    }
    */

    private fun resolve(ref: PsiReference) = when (ref) {
        is PsiPolyVariantReference -> {
            val results = ref.multiResolve(false)
            assertNotNull(results)
            assertEquals(1, results.size.toLong())
            results[0].element
        }
        else -> ref.resolve()
    }

    private fun multiResolve(ref: PsiReference) = when (ref) {
        is PsiPolyVariantReference -> {
            val results = ref.multiResolve(false)
            assertNotNull(results)
            assertTrue(results.isNotEmpty())
            results.map { it.element}
        }
        else -> listOf(ref.resolve())
    }

    private fun doComplete() {
        val fixture = SnakemakeWorld.fixture()
        fixture.complete(CompletionType.BASIC)
        SnakemakeWorld.myCompletionList = fixture.lookupElementStrings
    }


    private fun assertUnresolvedReference(ref: PsiReference) {
        when (ref) {
            is PsiPolyVariantReference -> {
                val results = ref.multiResolve(false)
                assertNotNull(results)
                assertEquals(0, results.size.toLong())
            }
            else -> TestCase.assertNull(ref.resolve())
        }
    }

    private fun assertNotInCompletionList(
            actualLookupItems: List<String>,
            expectedMissingVariants: List<String>
    ) {
        val lookupElementsSet = HashSet(actualLookupItems)
        val unexpectedVariants = ArrayList<String>()
        for (variant in expectedMissingVariants) {
            if (lookupElementsSet.contains(variant)) {
                unexpectedVariants.add(variant)
            }
        }
        if (unexpectedVariants.isEmpty()) {
            return
        }
        org.junit.Assert.fail(
                """
                    The following variants aren't expected in completion list.
                    Unexpected variants:
                    ${UsefulTestCase.toString(unexpectedVariants)}
                    Completion list:
                    ${UsefulTestCase.toString(actualLookupItems)}
                """.trimIndent())
    }

    private fun getReferenceAtOffset() = SnakemakeWorld.fixture()
            .file.findReferenceAt(getOffsetUnderCaret())

    private fun getPositionBySignature(editor: Editor, marker: String, after: Boolean): Int {
        val text = editor.document.text
        val pos = text.indexOf(marker)
        require(pos >= 0) {
            "Marker <$marker> not found in <$text>"
        }
        require(pos == text.lastIndexOf(marker)) { "Multiple marker entries" }
        return if (after) pos + marker.length else pos
    }

    private fun checkCompletionResult(
            lookupFilter: LookupFilter,
            lookupElements: Array<LookupElement>?,
            fixture: CodeInsightTestFixture,
            checkByFilePath: Boolean,
            completionResultTextOrFileRelativePath: String,
            completionSelectChar: Char
    ) {
        // If completion list contained only one variant completion list will be closed and
        // variant will be automatically inserted
        if (lookupElements == null && LookupManager.getInstance(fixture.project).activeLookup == null) {
            if (checkByFilePath) {
                fixture.checkResultByFile(completionResultTextOrFileRelativePath)
            } else {
                fixture.checkResult(completionResultTextOrFileRelativePath)
            }
            return
        }

        // zero or several variants
        assertNotNull(lookupElements)

        selectItem(lookupFilter.findElement(lookupElements), completionSelectChar, fixture.project)
        if (checkByFilePath) {
            fixture.checkResultByFile(completionResultTextOrFileRelativePath)
        } else {
            fixture.checkResult(completionResultTextOrFileRelativePath)
        }

    }

    private fun selectItem(item: LookupElement, ch: Char, project: Project) {
        val lookup = LookupManager.getInstance(project).activeLookup as LookupImpl?
        assertNotNull(lookup, message = "Lookup didn't show")
        lookup.currentItem = item
        UIUtil.invokeLaterIfNeeded {
            CommandProcessor.getInstance().executeCommand(
                    project, { lookup.finishLookup(ch) }, "", null)
        }
    }
}

class LookupFilter private constructor(
        private val myLookupString: String,
        private val myTypeString: String?
) : Condition<LookupElement> {

    override fun value(lookupElement: LookupElement?): Boolean {
        return accept(lookupElement)
    }

    fun accept(lookupElement: LookupElement?): Boolean {
        if (lookupElement == null) {
            return false
        }

        val presentation = LookupElementPresentation()
        lookupElement.renderElement(presentation)

        return if (myLookupString != presentation.itemText) {
            false
        } else StringUtil.isEmpty(myTypeString) || myTypeString == presentation.typeText
    }

    override fun toString(): String {
        return "Text: \"" + myLookupString + "\"; Type: \"" + (myTypeString ?: "any") + "\""
    }

    fun findElement(lookupElements: Array<LookupElement>): LookupElement {
        val filteredElements = ContainerUtil.filter(lookupElements, this)
        if (filteredElements.isEmpty()) {
            org.junit.Assert.fail(toString() + " - isn't in autocompletion popup")
        } else if (filteredElements.size > 1) {
            val error = StringBuilder()
            error.append("Several elements with the same conditions: ").append(toString())
            filteredElements.forEach { element -> error.append("\n - ").append(dumpElement(element)) }
            org.junit.Assert.fail(error.toString())
        }
        return filteredElements[0]
    }

    companion object {

        fun create(lookupString: String): LookupFilter {
            return LookupFilter(lookupString, null)
        }

        fun create(lookupString: String, typeString: String): LookupFilter {
            return LookupFilter(lookupString, typeString)
        }

        private fun dumpElement(element: LookupElement?): String {
            if (element == null) {
                return "null element"
            }
            val sb = StringBuilder()

            val elementPresentation = LookupElementPresentation()
            element.renderElement(elementPresentation)
            sb.append("Text: \"").append(elementPresentation.itemText).append("\"; ")
            sb.append("Tail: \"").append(elementPresentation.tailText).append("\"; ")
            sb.append("Type: \"").append(elementPresentation.typeText).append("\"; ")
            return sb.toString()
        }
    }
}


fun assertHasElements(
        actualLookupItems: List<String>,
        expectedVariants: List<String>
) {
    val unmetElements = ArrayList<String>()

    for (variant in expectedVariants) {
        if (!actualLookupItems.contains(variant)) {
            unmetElements.add(variant)
        }
    }
    if (unmetElements.isNotEmpty()) {
        org.junit.Assert.fail(
                """
            "Not all elements were found in real collection. Following elements were missed :[
            ${UsefulTestCase.toString(unmetElements)}] in collection:[
            ${UsefulTestCase.toString(actualLookupItems)}]
            """.trimIndent()
        )
    }
}