package features.glue

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
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.TestLookupElementPresentation
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.python.psi.PyStringLiteralExpression
import features.glue.SnakemakeWorld.getOffsetUnderCaret
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import junit.framework.TestCase
import java.lang.Integer.max
import kotlin.test.*

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-09
 */
class CompletionResolveSteps {
    @Then("^there should be no reference$")
    fun referenceShouldBeNull() {
        ApplicationManager.getApplication().invokeAndWait({
            val ref = getReferenceAtOffset()
            assertNull(ref)
        }, ModalityState.nonModal())
    }

    @Then("^reference should not resolve$")
    fun referenceShouldNotResolve() {
        ApplicationManager.getApplication().invokeAndWait({
            val ref = getReferenceAtOffset()
            assertNotNull(ref)
            assertUnresolvedReference(ref)
        }, ModalityState.nonModal())
    }


    @Then("^reference in injection should not resolve$")
    fun referenceInInjectionShouldNotResolve() {
        ApplicationManager.getApplication().invokeAndWait({
            val ref = getReferenceInInjectedLanguageAtOffset()
            assertNotNull(ref)
            assertUnresolvedReference(ref)
        }, ModalityState.nonModal())
    }

    @When("^I put the caret at (.+)$")
    fun iPutCaretAt(marker: String) {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait({
            application.runWriteAction {
                val editor = SnakemakeWorld.fixture().editor
                val position = getPositionBySignature(editor, marker, false)
                editor.caretModel.moveToOffset(position)
            }
        }, ModalityState.nonModal())
    }

    @When("^I put the caret after (.+)$")
    fun iPutCaretAfter(marker: String) {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait({
            application.runWriteAction {
                val editor = SnakemakeWorld.fixture().editor
                val position = getPositionBySignature(editor, marker, true)
                editor.caretModel.moveToOffset(position)
            }
        }, ModalityState.nonModal())
    }

    @When("^I change current file to <([^>]+)>\$")
    fun iChangeCurrentFileTo(file: String) {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait({
            application.runWriteAction {
                SnakemakeWorld.fixture().openFileInEditor(SnakemakeWorld.fixture().findFileInTempDir(file))
            }
        }, ModalityState.nonModal())
    }


    @Then("^reference should resolve to \"(.+)\" directory")
    fun referenceShouldResolveToDirectory(name: String) {
        ApplicationManager.getApplication().runReadAction {
            val ref = getReferenceAtOffset()
            assertNotNull(ref)
            val result = resolve(ref) as? PsiDirectory
            assertNotNull(result)
            assertEquals(result.name, name)
        }
    }

    @Then("^reference in injection should resolve to \"(.+)\" in \"(.+)\"$")
    fun referenceInInjectionShouldResolveToIn(targetPrefix: String, file: String) {
        DumbService.getInstance(SnakemakeWorld.fixture().project).waitForSmartMode()
        ApplicationManager.getApplication().runReadAction {
            tryToResolveRef(targetPrefix, targetPrefix, file, getReferenceInInjectedLanguageAtOffset())
        }
    }

    @Then("^reference in injection should resolve to \"(.+)\" in context \"(.+)\" in file \"(.+)\"$")
    fun referenceInInjectionShouldResolveToAtInFile(targetPrefix: String, context: String, file: String) {
        DumbService.getInstance(SnakemakeWorld.fixture().project).waitForSmartMode()
        ApplicationManager.getApplication().runReadAction {
            tryToResolveRef(targetPrefix, context, file, getReferenceInInjectedLanguageAtOffset())
        }
    }

    @Then("^reference should resolve to \"(.+)\" in context \"(.+)\" in file \"(.+)\"$")
    fun referenceShouldResolveToAtInFile(resultSubstr: String, context: String, file: String) {
        DumbService.getInstance(SnakemakeWorld.fixture().project).waitForSmartMode()
        ApplicationManager.getApplication().runReadAction {
            tryToResolveRef(resultSubstr, context, file, getReferenceAtOffset())
        }
    }

    @Then("^reference should resolve to \"(.+)\" in \"(.+)\"$")
    fun referenceShouldResolveToIn(targetPrefix: String, file: String) {
        DumbService.getInstance(SnakemakeWorld.fixture().project).waitForSmartMode()
        ApplicationManager.getApplication().runReadAction {
            tryToResolveRef(targetPrefix, targetPrefix, file, getReferenceAtOffset())
        }
    }

    private fun tryToResolveRef(targetPrefix: String, context: String, file: String, ref: PsiReference?) {
        assertNotNull(ref)

        val result = resolve(ref)
        assertNotNull(result)

        val injectionHost = SnakemakeWorld.injectionFixture().injectedLanguageManager
            .getInjectionHost(result)
        val containingFile = (injectionHost ?: result).containingFile
        val msg = "Resolve result type: ${result::class.java.simpleName}," +
                " file: ${containingFile.virtualFile.path}\n" +
                "Psi element text: ${result.text}"
        require(context.isNotEmpty() && targetPrefix.isNotEmpty()) {
            msg
        }

        require(targetPrefix.isNotEmpty()) {
            "Target prefix is empty. $msg"
        }

        require(injectionHost == null || injectionHost is PyStringLiteralExpression) {
            "Injection only in strings are currently by this method, but was injection in ${injectionHost!!::class.java.simpleName}\n$msg"
        }
        val injectionStartOffset = if (injectionHost == null) {
            0
        } else {
            injectionHost.textOffset + (injectionHost as PyStringLiteralExpression).stringValueTextRange.startOffset
        }


        assertNotNull(containingFile)
        val actualPath = containingFile.virtualFile.path
        val actualSubString = actualPath.subSequence(
            max(0, actualPath.length - file.length),
            actualPath.length
        )
        assertEquals(file, actualSubString, msg)

        assertTrue(
            targetPrefix.length <= result.textLength,
            "Expected result prefix <$targetPrefix> is longer than element <${result.text}>\n$msg"
        )

        val elementText = TextRange.from(result.textOffset, targetPrefix.length).substring(result.containingFile.text)
//        val elementText = TextRange.from(0, targetPrefix.length).substring(result.text)
        assertEquals(targetPrefix, elementText, msg)

        val text =
            TextRange.from(injectionStartOffset + result.textOffset, context.length).substring(containingFile.text)
        assertEquals(context, text, msg)

    }

    @Then("^reference in injection should multi resolve to name, file in same order$")
    fun referenceInInjectionShouldMultiResolveToInOrder(table: DataTable) {
        checkMultiResolveInSameOrder(table) { getReferenceInInjectedLanguageAtOffset() }
    }

    @Then("^reference should multi resolve to name, file in same order$")
    fun referenceShouldMultiResolveToInOrder(table: DataTable) {
        checkMultiResolveInSameOrder(table) { getReferenceAtOffset() }
    }

    private fun checkMultiResolveInSameOrder(table: DataTable, refProvider: () -> PsiReference?) {
        ApplicationManager.getApplication().runReadAction {
            val ref = refProvider()
            assertNotNull(ref)

            val rawResults = multiResolve(ref)
            assertEquals(0, rawResults.filter { it == null }.size)

            val results = rawResults.filterNotNull()
            assertEquals(0, results.filter { it.containingFile == null }.size)

            val resolveResults = results.mapIndexed() { i, result ->
                (itemText(result) to result.containingFile.name) to i
            }.toMap()

            val rows = table.asLists()

            val actualRefsInfo = resolveResults.entries.joinToString(separator = "\n") { (textAndFileName, _) ->
                "|${textAndFileName.first}| ${textAndFileName.second} |"
            }

            var prev: Pair<Int, Pair<String, String>>? = null
            rows.forEach { row ->
                val key = row[0] to row[1]
                val idx = resolveResults[key]
                assertNotNull(
                    idx,
                    message = "Variant [${key.first}, ${key.second}] is missing in:\n$actualRefsInfo"
                )
                if (prev != null) {
                    val prevKey = prev!!.second
                    assertTrue(
                        prev!!.first < idx,
                        message = "Variant [${prevKey.first}, ${prevKey.second}] should be before" +
                                " [${key.first}, ${key.second}] in:\n$actualRefsInfo"
                    )
                }
                prev = idx to key
            }
        }
    }

    @Then("^reference in injection should multi resolve to name, file, times\\[, class name\\]$")
    fun referenceInInjectionShouldMultiResolveTo(table: DataTable) {
        checkMultiresolve(table) { getReferenceInInjectedLanguageAtOffset() }
    }

    @Then("^reference should multi resolve to name, file, times\\[, class name\\]$")
    fun referenceShouldMultiResolveTo(table: DataTable) {
        checkMultiresolve(table) { getReferenceAtOffset() }
    }

    private fun checkMultiresolve(table: DataTable, refProvider: () -> PsiReference?) {
        ApplicationManager.getApplication().runReadAction {
            val ref = refProvider()
            assertNotNull(ref)

            val rawResults = multiResolve(ref)
            assertEquals(0, rawResults.filter { it == null }.size)

            val results = rawResults.filterNotNull()
            assertEquals(0, results.filter { it.containingFile == null }.size)

            val completionListKey2 = results
                .map { result ->
                    itemText(result) to result.containingFile.name
                }
                .groupBy { it }
                .map { entry -> entry.key to entry.value.size }
                .toMap()

            val completionListKey3 = results
                .map { result ->
                    Triple(itemText(result), result.javaClass.simpleName, result.containingFile.name)
                }
                .groupBy { it }
                .map { entry -> entry.key to entry.value.size }
                .toMap()

            val rows = table.asLists()

            val actualRefsInfo = completionListKey3.entries.joinToString(separator = "\n") { (nameAndFile, times) ->
                "|${nameAndFile.first}| ${nameAndFile.third} | $times | ${nameAndFile.second} |"
            }

            rows.forEach { row ->
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

    private fun itemText(result: PsiElement): String = when (result) {
        is PsiNamedElement -> {
            "${result.name}"
            // "${result.name} (${result.textRange})"
        }
        else -> result.text
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
            val actualRefsInfo = results.joinToString(separator = "\n") { el ->
                "|${el!!.containingFile.name} | # ${if (el is PsiNamedElement) el.name else el.text}"
            }

            table.asList().forEach { fileName ->
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

    @When("^I invoke autocompletion popup (\\d+) times$")
    fun iInvokeAutocompletionPopupNTimes(invocationCount: Int) {
        Registry.get("ide.completion.variant.limit").setValue(10000)
        doComplete(invocationCount)
    }

    @Then("^completion list should contain:$")
    fun completionListShouldContain(table: DataTable) {
        // table format:
        //  | item text | tail text | type text |
        val nCols = table.width()
        if (nCols == 1) {
            // lookup strings:
            assertHasElements(SnakemakeWorld.completionList(), table.column(0).toList())
            return
        }

        val expected = table.asLists().map { row ->
            row.joinToString(separator = "|", prefix = "|", postfix = "|")
        }

        val buff = StringBuilder()
        val actual = SnakemakeWorld.completionList().zip(SnakemakeWorld.completionListPresentations())
            .map { (lookupString, p) ->
                buff.clear()
                buff.append('|')
                buff.append(lookupString) //XXX: 'p.itemText' could be wrapped in extra quotes for some reason
                if (nCols > 1) {
                    buff.append('|')
                    buff.append(p.tailText)
                }
                if (nCols > 2) {
                    buff.append('|')
                    buff.append(p.typeText)
                }
                buff.append('|')
                buff.toString()
            }
        assertHasElements(actual, expected)
    }

    @Then("^completion list should be empty")
    fun completionListShouldBeEmpty() {
        assertEquals(
            0, SnakemakeWorld.completionList().size,
            "Unexpected items: ${
                SnakemakeWorld.completionList().take(20)
                    .joinToString(separator = "\n")
            }\n"
        )
    }

    @Then("^completion list should only contain:$")
    fun completionListShouldOnlyContain(table: DataTable) {
        completionListShouldOnlyContainMethods(table.asList())
    }

    @Then("^completion list should only contain items (.+)$")
    fun completionListShouldOnlyContainMethods(lookupItems: List<String>) {
        assertHasOnlyElements(SnakemakeWorld.completionList(), lookupItems)
    }

    @Then("^completion list should contain these items with type text:$")
    fun completionListShouldContainWithTypeText(table: DataTable) {
        val actualLookupItems = getCompletionListWithTypeText()
        val expectedLookupItems = table.asLists().filter { it.size >= 2 }.map { it[0] to it[1] }
        assertHasElements(actualLookupItems, expectedLookupItems)
    }

    @Then("^completion list should contain items (.+)$")
    fun completionListShouldContainMethods(str: String) {
        val lookupItems: List<String> = str.split(",").map { it.trim() }
        assertHasElements(SnakemakeWorld.completionList(), lookupItems)
    }

    @Then("^completion list shouldn't contain:$")
    fun completionListShouldNotContain(table: DataTable) {
        val lookupStrings = table.asList()
        assertNotInCompletionList(SnakemakeWorld.completionList(), lookupStrings)
    }

    @Then("^I invoke autocompletion popup, select \"([^\"]+)\" lookup item and see a text:")
    fun iInvokeAutocompletionPopupAndSelectItem(lookupText: String, text: String) {
        autoCompleteAndCheck(lookupText, text, Lookup.NORMAL_SELECT_CHAR)
    }

    @Then("^I invoke autocompletion popup and see a text:")
    fun iInvokeAutocompletionPopupAndSelectNoItem(text: String) {
        autoCompleteAndCheck(null, text, Lookup.NORMAL_SELECT_CHAR)
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

    private fun autoCompleteAndCheck(lookupText: String?, text: String, ch: Char) {
        iInvokeAutocompletionPopup()

        val fixture = SnakemakeWorld.fixture()
        val lookupElements = fixture.lookupElements?.filterNotNull()?.toTypedArray()

        ApplicationManager.getApplication().invokeAndWait(
            {
                if (lookupText == null) {
                    if (lookupElements != null) {
                        fail("Autocompletion to a single possible variant didn't happen " +
                                "because either the completion list was not empty, or given " +
                                "prefix didn't match any variants. Lookup elements: <${
                                    lookupElements.joinToString { le ->
                                        "${le.lookupString} [${le.psiElement?.javaClass?.simpleName}]"
                                    }
                                }>\n")
                    }
                } else {
                    assertNotNull(
                        lookupElements,
                        message = "Completion resulted in a single possible variant, so it was impossible " +
                                "to check whether \"$lookupText\" was in the completion list. " +
                                "Try using this step: Then I invoke autocompletion popup and see a text"
                    )
                    assertTrue(lookupElements.isNotEmpty(), message = "Completion list was empty")

                    selectItem(LookupFilter.create(lookupText).findElement(lookupElements), ch, fixture.project)
                }

                val cleanText = StringUtil.convertLineSeparators(
                    text.replace("\${TEST_DATA}", FileUtil.toSystemIndependentName(fixture.testDataPath))
                )
                checkCompletionResult(fixture, false, cleanText)
            },
            ModalityState.nonModal()
        )
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
            results.map { it.element }
        }
        else -> listOf(ref.resolve())
    }

    private fun doComplete(invocationCount: Int = 1) {
        val fixture = SnakemakeWorld.fixture()
        fixture.complete(CompletionType.BASIC, invocationCount)
        SnakemakeWorld.myCompletionList = fixture.lookupElementStrings
        SnakemakeWorld.myCompletionListPresentations =
            fixture.lookupElements?.map { TestLookupElementPresentation.renderReal(it) }
    }

    private fun getCompletionItemsPresentation(): List<LookupElementPresentation> {
        val fixture = SnakemakeWorld.fixture()
        fixture.complete(CompletionType.BASIC)
        val completionList = fixture.lookupElements
        return completionList?.map {
            val presentation = LookupElementPresentation()
            it!!.renderElement(presentation)
            presentation
        } ?: emptyList()
    }

    private fun getCompletionListWithTypeText() = getCompletionItemsPresentation().map { it.itemText to it.typeText }


    private fun assertUnresolvedReference(ref: PsiReference) {
        when (ref) {
            is PsiPolyVariantReference -> {
                val results = ref.multiResolve(false)
                assertNotNull(results)
                assertEquals(
                    0, results.size.toLong(),
                    "Unexpected results: ${
                        results.joinToString(separator = "\n") {
                            "${it.element?.javaClass?.simpleName}: [${it.element?.text}]"
                        }
                    }"
                )
            }
            else -> TestCase.assertNull(ref.resolve())
        }
    }

    private fun assertNotInCompletionList(
        actualLookupItems: List<String>,
        expectedMissingVariants: List<String>,
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
                """.trimIndent()
        )
    }

    private fun checkCompletionResult(
        fixture: CodeInsightTestFixture,
        checkByFilePath: Boolean,
        completionResultTextOrFileRelativePath: String,
    ) {
        if (checkByFilePath) {
            fixture.checkResultByFile(completionResultTextOrFileRelativePath)
        } else {
            fixture.checkResult(completionResultTextOrFileRelativePath)
        }
    }

    /*private fun checkCompletionResult(
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
        assertNotNull(lookupElements, message= "Well, this assertion happened")

        selectItem(lookupFilter.findElement(lookupElements), completionSelectChar, fixture.project)
        if (checkByFilePath) {
            fixture.checkResultByFile(completionResultTextOrFileRelativePath)
        } else {
            fixture.checkResult(completionResultTextOrFileRelativePath)
        }

    }*/

    private fun selectItem(item: LookupElement, ch: Char, project: Project) {
        val lookup = LookupManager.getInstance(project).activeLookup as LookupImpl?
        assertNotNull(lookup, message = "Lookup didn't show")
        lookup.currentItem = item
        UIUtil.invokeLaterIfNeeded {
            CommandProcessor.getInstance().executeCommand(
                project, { lookup.finishLookup(ch) }, "", null
            )
        }
    }

    companion object {
        fun getReferenceAtOffset() = SnakemakeWorld.fixture()
            .file.findReferenceAt(getOffsetUnderCaret())

        fun getReferenceInInjectedLanguageAtOffset(): PsiReference? {
            val fixture = SnakemakeWorld.injectionFixture()
            val element = fixture.injectedElement ?: return null
            val host =
                fixture.injectedLanguageManager.getInjectionHost(element) as PyStringLiteralExpression? ?: return null
            val offset = getOffsetUnderCaret() - (host.stringValueTextRange.startOffset + host.textOffset)
            return element.containingFile?.findReferenceAt(offset)
        }

        fun getPositionBySignature(editor: Editor, marker: String, after: Boolean): Int {
            val text = editor.document.text
            val pos = text.indexOf(marker)
            require(pos >= 0) {
                "Marker <$marker> not found in <$text>"
            }
            require(pos == text.lastIndexOf(marker)) { "Multiple marker entries" }
            return if (after) pos + marker.length else pos
        }
    }
}

class LookupFilter private constructor(
    private val myLookupString: String,
    private val myTypeString: String?,
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
        return "Text: <" + myLookupString + ">; Type text: <" + (myTypeString ?: "any") + ">"
    }

    fun findElement(lookupElements: Array<LookupElement>): LookupElement {
        val filteredElements = ContainerUtil.filter(lookupElements, this)
        when {
            filteredElements.isEmpty() -> {
                val itemsDump = dumpLookupItem(lookupElements)
                org.junit.Assert.fail(toString() + " - isn't in autocompletion popup, all elements:$itemsDump\n")
            }
            filteredElements.size > 1 -> {
                val msg = generatedDetailedMsg(
                    "Several elements with the same conditions: ",
                    filteredElements
                )
                org.junit.Assert.fail(msg)
            }
        }
        return filteredElements[0]
    }

    private fun generatedDetailedMsg(prefix: String, filteredElements: List<LookupElement>): String {
        val error = StringBuilder("$prefix\n Filter:${toString()}\n")
        filteredElements.forEach { element ->
            error.append("\n - ").append(dumpElement(element))
        }
        return error.toString()
    }

    private fun dumpLookupItem(lookupElements: Array<LookupElement>): String {
        val itemsDump = lookupElements.joinToString { lookupElement ->
            val itemPresentableText = LookupElementPresentation()
                .also { lookupElement.renderElement(it) }
                .itemText
            "${lookupElement.lookupString} [$itemPresentableText]"
        }
        return itemsDump
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


fun <T> assertHasElements(
    actualLookupItems: List<T>,
    expectedVariants: List<T>,
) {
    val unmetElements = ArrayList<T>()

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

fun assertHasOnlyElements(
    actualLookupItems: List<String>,
    expectedVariants: List<String>,
) {
    val extraElements = ArrayList<String>()

    for (item in actualLookupItems) {
        if (!expectedVariants.contains(item)) {
            extraElements.add(item)
        }
    }

    if (extraElements.isNotEmpty()) {
        org.junit.Assert.fail(
            """
            "Real collection contains unwanted elements. Following elements were extra :[
            ${UsefulTestCase.toString(extraElements)}] in collection:[
            ${UsefulTestCase.toString(actualLookupItems)}]
            """.trimIndent()
        )
    }
}