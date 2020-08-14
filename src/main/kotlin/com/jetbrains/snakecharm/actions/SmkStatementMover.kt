package com.jetbrains.snakecharm.actions

import com.intellij.codeInsight.editorActions.moveUpDown.LineRange
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.jetbrains.python.codeInsight.editorActions.moveUpDown.PyStatementMover
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.RULE_LIKE
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.impl.SmkRuleOrCheckpointArgsSectionImpl

open class SmkStatementMover: PyStatementMover() {
    override fun checkAvailable(editor: Editor, file: PsiFile, info: MoveInfo, down: Boolean): Boolean {

        if (file !is SmkFile) {
            return false
        }
        val offset = editor.caretModel.offset
        val selectionModel = editor.selectionModel
        val document = editor.document
        val lineNumber = document.getLineNumber(offset)
        var start = getLineStartSafeOffset(document, lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        var end = if (lineEndOffset == 0) 0 else lineEndOffset - 1

        if (selectionModel.hasSelection()) {
            start = selectionModel.selectionStart
            val selectionEnd = selectionModel.selectionEnd
            end = if (selectionEnd == 0) 0 else selectionEnd - 1
        }

        var elementToMove1 = PyUtil.findNonWhitespaceAtOffset(file, start) ?: return false
        var elementToMove2 = PyUtil.findNonWhitespaceAtOffset(file, end) ?: return false

        val pyStatementList = PsiTreeUtil.getParentOfType(elementToMove1, PyStatementList::class.java)
        val ruleOrCheckpointSection = PsiTreeUtil.getParentOfType(elementToMove1, SmkRuleOrCheckpointArgsSectionImpl::class.java)

        if (pyStatementList != null &&
                ruleOrCheckpointSection != null) {
            if (ruleOrCheckpointSection == pyStatementList.children[0] && !down) {
                return true
            }
        }

        if (elementToMove1.elementType in RULE_LIKE && elementToMove2.parent?.firstChild.elementType in RULE_LIKE){
            elementToMove1 = elementToMove1.parent
            elementToMove2 = elementToMove2.parent

            elementToMove1 = getCommentOrStatement(document, elementToMove1)
            elementToMove2 = getCommentOrStatement(document, elementToMove2)

            info.toMove = elementToMove2.let { MyLineRange(elementToMove1, it) }
            info.toMove2 = getDestinationScope(file, editor, (if (down) elementToMove2 else elementToMove1), down)

            info.indentTarget = false
            info.indentSource = false

            return true
        }

        return false
    }

    private fun getDestinationScope(file: PsiFile, editor: Editor,
                                         elementToMove: PsiElement, down: Boolean): LineRange? {
        val document = file.viewProvider.document ?: return null
        val offset = if (down) elementToMove.textRange.endOffset else elementToMove.textRange.startOffset
        val lineNumber = if (down) document.getLineNumber(offset) + 1 else document.getLineNumber(offset) - 1
        if (moveOutsideFile(document, lineNumber)) return null
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        val startOffset = document.getLineStartOffset(lineNumber)
        val statementList = getStatementList(elementToMove)
        val destination = getDestinationElement(elementToMove, document, lineEndOffset, down)
        val start = destination?.textRange?.startOffset ?: lineNumber
        val end = destination?.textRange?.endOffset ?: lineNumber
        val startLine = document.getLineNumber(start)
        val endLine = document.getLineNumber(end)
        if (elementToMove is PyClass || elementToMove is PyFunction) {
            val scope = statementList ?: elementToMove.containingFile as PyElement
            if (destination != null) return ScopeRange(scope, destination, !down, true)
        }
        val lineText = document.getText(TextRange.create(startOffset, lineEndOffset))
        val isEmptyLine = StringUtil.isEmptyOrSpaces(lineText)
        if (isEmptyLine && moveToEmptyLine(elementToMove, down)) return LineRange(lineNumber, lineNumber + 1)
        var scopeRange: LineRange? = moveOut(elementToMove, editor, down)
        if (scopeRange != null) return scopeRange
        scopeRange = moveInto(elementToMove, file, editor, down, lineEndOffset)
        if (scopeRange != null) return scopeRange
        if (elementToMove is PsiComment && PsiTreeUtil.isAncestor(destination, elementToMove, true) ||
                destination is PsiComment) {
            return LineRange(lineNumber, lineNumber + 1)
        }
        val scope = statementList ?: elementToMove.containingFile as PyElement
        return if (elementToMove is PyClass || elementToMove is PyFunction) ScopeRange(scope, scope.firstChild, !down, true) else LineRange(startLine, endLine + 1)
    }

    private fun moveOutsideFile(document: Document, lineNumber: Int): Boolean {
        return lineNumber < 0 || lineNumber >= document.lineCount
    }

    private fun moveToEmptyLine(elementToMove: PsiElement, down: Boolean): Boolean {
        val statementList = getStatementList(elementToMove)
        if (statementList != null) {
            if (down) {
                val child = statementList.lastChild
                if (elementToMove === child && PsiTreeUtil.getNextSiblingOfType(statementList.parent, PyStatementPart::class.java) != null
                        || child !== elementToMove) {
                    return true
                }
            } else {
                return true
            }
        }
        return statementList == null
    }

    private fun getStatementList(elementToMove: PsiElement): PyStatementList? {
        return PsiTreeUtil.getParentOfType(elementToMove, PyStatementList::class.java, true,
                PyStatementWithElse::class.java, PyLoopStatement::class.java,
                PyFunction::class.java, PyClass::class.java)
    }

    private fun moveOut(elementToMove: PsiElement, editor: Editor, down: Boolean): ScopeRange? {
        val statementList = getStatementList(elementToMove) ?: return null
        if ((!down || statementList.lastChild !== elementToMove) && (down || statementList.firstChild !== elementToMove)) {
            return null
        }
        val addBefore = !down
        val parent = statementList.parent
        val sibling = if (down) PsiTreeUtil.getNextSiblingOfType(parent, PyStatementPart::class.java) else PsiTreeUtil.getPrevSiblingOfType(parent, PyStatementPart::class.java)
        return if (sibling != null) {
            val list = sibling.statementList
            ScopeRange(list, if (down) list.firstChild else list.lastChild, !addBefore)
        } else {
            val scope = getScopeForComment(elementToMove, editor, parent, !down)
            val anchor: PsiElement? = PsiTreeUtil.getParentOfType(statementList, PyStatement::class.java)
            if (scope == null || anchor == null) null else ScopeRange(scope, anchor, addBefore)
        }
    }

    private fun getScopeForComment(elementToMove: PsiElement, editor: Editor,
                                        parent: PsiElement?, down: Boolean): PsiElement? {
        var scope: PsiElement? = PsiTreeUtil.getParentOfType(parent, PyStatementList::class.java, PyFile::class.java)
        val offset = elementToMove.textOffset
        var sibling: PsiElement? = elementToMove
        while (scope != null && elementToMove is PsiComment) { // stupid workaround for PY-6408. Related to PSI structure
            val prevSibling = (if (down) PsiTreeUtil.getNextSiblingOfType(sibling, PyStatement::class.java) else PsiTreeUtil.getPrevSiblingOfType(sibling, PyStatement::class.java))
                    ?: break
            if (editor.offsetToLogicalPosition(prevSibling.textOffset).column ==
                    editor.offsetToLogicalPosition(offset).column) break
            sibling = scope
            scope = PsiTreeUtil.getParentOfType(scope, PyStatementList::class.java, PyFile::class.java)
        }
        return scope
    }

    private fun moveInto(elementToMove: PsiElement, file: PsiFile,
                              editor: Editor, down: Boolean, offset: Int): LineRange? {
        val rawElement = PyUtil.findNonWhitespaceAtOffset(file, offset) ?: return null
        return if (down) moveDownInto(editor.document, rawElement) else moveUpInto(elementToMove, editor, rawElement, false)
    }

    private fun moveUpInto(elementToMove: PsiElement, editor: Editor,
                                rawElement: PsiElement, down: Boolean): LineRange? {
        val document = editor.document
//        var element: PsiElement? = getCommentOrStatement(document, rawElement)
        var element: PsiElement? = rawElement
        val statementList = getStatementList(elementToMove)
        val scopeForComment = if (statementList == null) null else getScopeForComment(elementToMove, editor, elementToMove, down)
        var statementList2 = getStatementList(element!!)
        val start1 = elementToMove.textOffset - document.getLineStartOffset(document.getLineNumber(elementToMove.textOffset))
        val start2 = element.textOffset - document.getLineStartOffset(document.getLineNumber(element.textOffset))
        if (start1 != start2) {
            var parent2 = PsiTreeUtil.getParentOfType(statementList2, PyStatementList::class.java)
            while (parent2 !== scopeForComment && parent2 != null) {
                element = PsiTreeUtil.getParentOfType(statementList2, PyStatement::class.java)
                statementList2 = parent2
                parent2 = PsiTreeUtil.getParentOfType(parent2, PyStatementList::class.java)
            }
        }
        return if (statementList2 != null && scopeForComment !== statementList2 &&
                (statementList2.lastChild === element || statementList2.lastChild === elementToMove) && element != null) {
            ScopeRange(statementList2, element, false)
        } else null
    }

    private fun moveDownInto(document: Document, rawElement: PsiElement): LineRange? {
        val element = getCommentOrStatement(document, rawElement)
        val statementList2 = getStatementList(element)
        if (statementList2 != null) {                     // move to one-line conditional/loop statement
            val number = document.getLineNumber(element.textOffset)
            val number2 = document.getLineNumber(statementList2.parent.textOffset)
            if (number == number2) {
                return ScopeRange(statementList2, statementList2.firstChild, true)
            }
        }
        val statementPart = PsiTreeUtil.getParentOfType(rawElement, PyStatementPart::class.java, true, PyStatement::class.java,
                PyStatementList::class.java)
        val functionDefinition = PsiTreeUtil.getParentOfType(rawElement, PyFunction::class.java, true, PyStatement::class.java,
                PyStatementList::class.java)
        val classDefinition = PsiTreeUtil.getParentOfType(rawElement, PyClass::class.java, true, PyStatement::class.java,
                PyStatementList::class.java)
        var list: PyStatementList? = null
        if (statementPart != null) list = statementPart.statementList else if (functionDefinition != null) list = functionDefinition.statementList else if (classDefinition != null) list = classDefinition.statementList
        return if (list != null) {
            ScopeRange(list, list.firstChild, true)
        } else null
    }

    private fun getDestinationElement(elementToMove: PsiElement, document: Document,
                                           lineEndOffset: Int, down: Boolean): PsiElement? {
        var destination = PyUtil.findPrevAtOffset(elementToMove.containingFile, lineEndOffset, PsiWhiteSpace::class.java)
        val sibling: PsiElement? = if (down) PsiTreeUtil.getNextSiblingOfType(elementToMove, PyStatement::class.java) else PsiTreeUtil.getPrevSiblingOfType(elementToMove, PyStatement::class.java)
        if (destination == null) {
            destination = if (elementToMove is PyClass) {
                sibling
            } else if (elementToMove is PyFunction) {
                if (sibling !is PyClass) sibling else null
            } else {
                return null
            }
        }
        if (destination is PsiComment) return destination
        destination = if (elementToMove is PyClass) {
            sibling
        } else if (elementToMove is PyFunction) {
            if (sibling !is PyClass) sibling else null
        } else {
            getCommentOrStatement(document, (sibling ?: destination)!!)
        }
        return destination
    }

    @Suppress("NAME_SHADOWING")
    private fun getCommentOrStatement(document: Document, destination: PsiElement): PsiElement {
        var destination = destination
        val statement = PsiTreeUtil.getParentOfType(destination, PyStatement::class.java, false)
                ?: return destination
        if (destination is PsiComment) {
            if (document.getLineNumber(destination.getTextOffset()) == document.getLineNumber(statement.textOffset)) {
                destination = statement
            }
        } else destination = statement
        return destination
    }

    override fun beforeMove(editor: Editor, info: MoveInfo, down: Boolean) {
        val toMove = info.toMove
        val toMove2 = info.toMove2
        if (toMove is MyLineRange && toMove2 is ScopeRange) {
            PostprocessReformattingAspect.getInstance(editor.project!!).disablePostprocessFormattingInside {
                val startToMove = toMove.myStartElement
                val endToMove = toMove.myEndElement
                val file = startToMove.containingFile
                val selectionModel = editor.selectionModel
                val caretModel = editor.caretModel
                val selectionStart = selectionModel.selectionStart
                val isSelectionStartAtCaret = caretModel.offset == selectionStart
                val selectionLen = getSelectionLenContainer(editor, toMove)
                val shift = getCaretShift(startToMove, endToMove, caretModel, isSelectionStartAtCaret)
                val hasSelection = selectionModel.hasSelection()
                val offset: Int
                offset = if (toMove2.isTheSameLevel) {
                    moveTheSameLevel(toMove2, toMove)
                } else {
                    moveInOut(toMove, editor, info)
                }
                restoreCaretAndSelection(file, editor, isSelectionStartAtCaret, hasSelection, selectionLen,
                        shift, offset, toMove)
                info.toMove2 = info.toMove //do not move further
            }
        }
    }

    private fun getSelectionLenContainer(editor: Editor, toMove: MyLineRange): SelectionContainer {
        val selectionModel = editor.selectionModel
        val startToMove = toMove.myStartElement
        val endToMove = toMove.myEndElement
        val selectionStart = selectionModel.selectionStart
        val selectionEnd = selectionModel.selectionEnd
        val range = startToMove.textRange
        val column = editor.offsetToLogicalPosition(selectionStart).column
        val additionalSelection = if (range.startOffset > selectionStart) range.startOffset - selectionStart else 0
        if (startToMove === endToMove) return SelectionContainer(selectionEnd - range.startOffset, additionalSelection, column == 0)
        var len = if (range.startOffset <= selectionStart) range.endOffset - selectionStart else startToMove.textLength
        var tmp = startToMove.nextSibling
        while (tmp !== endToMove && tmp != null) {
            if (tmp !is PsiWhiteSpace) len += tmp.textLength
            tmp = tmp.nextSibling
        }
        len = len + selectionEnd - endToMove.textOffset
        return SelectionContainer(len, additionalSelection, column == 0)
    }

    @Suppress("NAME_SHADOWING")
    private fun restoreCaretAndSelection(file: PsiFile, editor: Editor, selectionStartAtCaret: Boolean,
                                              hasSelection: Boolean, selectionContainer: SelectionContainer, shift: Int,
                                              offset: Int, toMove: MyLineRange) {
        var shift = shift
        val document = editor.document
        val selectionModel = editor.selectionModel
        val caretModel = editor.caretModel
        var selectionLen = selectionContainer.myLen
        val at = file.findElementAt(offset)
        if (at != null) {
            val added = getCommentOrStatement(document, at)
            var size = toMove.size
            if (size > 1) {
                var tmp = added.nextSibling
                while (size > 1 && tmp != null) {
                    if (tmp is PsiWhiteSpace) {
                        if (!selectionStartAtCaret) shift += tmp.getTextLength()
                        selectionLen += tmp.getTextLength()
                    }
                    tmp = tmp.nextSibling
                    size -= 1
                }
            }
            if (shift < 0) shift = 0
            val column = editor.offsetToLogicalPosition(added.textRange.startOffset).column
            selectionLen += if (selectionContainer.myAtTheBeginning || column < selectionContainer.myAdditional) {
                column
            } else {
                selectionContainer.myAdditional
            }
            if (selectionContainer.myAtTheBeginning && selectionStartAtCaret) shift = -column
        }
        val documentLength = document.textLength
        var newCaretOffset = offset + shift
        if (newCaretOffset >= documentLength) newCaretOffset = documentLength
        caretModel.moveToOffset(newCaretOffset)
        if (hasSelection) {
            if (selectionStartAtCaret) {
                val newSelectionEnd = newCaretOffset + selectionLen
                selectionModel.setSelection(newCaretOffset, newSelectionEnd)
            } else {
                val newSelectionStart = newCaretOffset - selectionLen
                selectionModel.setSelection(newSelectionStart, newCaretOffset)
            }
        }
    }

    private fun getCaretShift(startToMove: PsiElement, endToMove: PsiElement, caretModel: CaretModel, selectionStartAtCaret: Boolean): Int {
        var shift: Int
        if (selectionStartAtCaret) {
            shift = caretModel.offset - startToMove.textRange.startOffset
        } else {
            shift = caretModel.offset
            if (startToMove !== endToMove) {
                shift += startToMove.textLength
                var tmp = startToMove.nextSibling
                while (tmp !== endToMove && tmp != null) {
                    if (tmp !is PsiWhiteSpace) shift += tmp.textLength
                    tmp = tmp.nextSibling
                }
            }
            shift -= endToMove.textOffset
        }
        return shift
    }

    private fun moveTheSameLevel(toMove2: ScopeRange, toMove: MyLineRange): Int {
        val anchor = toMove2.anchor
        val anchorCopy = anchor.copy()
        val startToMove = toMove.myStartElement
        val endToMove = toMove.myEndElement
        val parent = anchor.parent
        val tmp = startToMove.nextSibling
        if (startToMove !== endToMove && tmp != null) {
            parent.addRangeAfter(tmp, endToMove, anchor)
        }
        val startCopy = startToMove.copy()
        startToMove.replace(anchorCopy)
        val addedElement = anchor.replace(startCopy)
        if (startToMove !== endToMove && tmp != null) {
            parent.deleteChildRange(tmp, endToMove)
        }
        return addedElement.textRange.startOffset
    }

    private fun moveInOut(toMove: MyLineRange, editor: Editor, info: MoveInfo): Int {
        var removePass = false
        val toMove2 = info.toMove2 as ScopeRange
        val scope = toMove2.scope
        val anchor = toMove2.anchor
        val project = scope.project
        val startElement = toMove.myStartElement
        val endElement = toMove.myEndElement
        val parent = startElement.parent
        if (scope is PyStatementList && !(startElement === endElement && startElement is PsiComment)) {
            val statements = scope.statements
            if (statements.size == 1 && statements[0] === anchor && statements[0] is PyPassStatement) {
                removePass = true
            }
        }
        val addedElement: PsiElement
        val nextSibling = startElement.nextSibling
        if (toMove2.isAddBefore) {
            val tmp = endElement.prevSibling
            if (startElement !== endElement && tmp != null) {
                addedElement = scope.addRangeBefore(startElement, tmp, anchor)
                scope.addBefore(endElement, anchor)
            } else {
                addedElement = scope.addBefore(endElement, anchor)
            }
        } else {
            if (startElement !== endElement && nextSibling != null) {
                scope.addRangeAfter(nextSibling, endElement, anchor)
            }
            addedElement = scope.addAfter(startElement, anchor)
        }
        addPassStatement(toMove, project)
        if (startElement !== endElement && nextSibling != null) {
            parent.deleteChildRange(nextSibling, endElement)
        }
        startElement.delete()
        val addedElementLine = editor.document.getLineNumber(addedElement.textOffset)
        val file = scope.containingFile
        adjustLineIndents(editor, scope, project, addedElement, toMove.size)
        if (removePass) {
            ApplicationManager.getApplication().runWriteAction {
                val document = editor.document
                val lineNumber = document.getLineNumber(anchor.textOffset)
                val endOffset = if (document.lineCount <= lineNumber + 1) document.getLineEndOffset(lineNumber) else document.getLineStartOffset(lineNumber + 1)
                document.deleteString(document.getLineStartOffset(lineNumber), endOffset)
                PsiDocumentManager.getInstance(startElement.project).commitAllDocuments()
            }
        }
        var offset = addedElement.textRange.startOffset
        val newLine = editor.document.getLineNumber(offset)
        if (newLine != addedElementLine && !removePass) {  // PsiComment gets broken after adjust indent
            var psiElement = PyUtil.findNonWhitespaceAtOffset(file, editor.document.getLineEndOffset(addedElementLine) - 1)
            if (psiElement != null) {
                psiElement = getCommentOrStatement(editor.document, psiElement)
                offset = psiElement.textRange.startOffset
            }
        }
        return offset
    }

    @Suppress("NAME_SHADOWING")
    private fun adjustLineIndents(editor: Editor, scope: PsiElement, project: Project,
                                  addedElement: PsiElement, size: Int) {
        var size = size
        val codeStyleManager = CodeStyleManager.getInstance(project)
        val document = editor.document
        if (scope !is PsiFile) {
            val line1 = editor.offsetToLogicalPosition(scope.textRange.startOffset).line
            val line2 = editor.offsetToLogicalPosition(scope.textRange.endOffset).line
            codeStyleManager.adjustLineIndent(scope.containingFile,
                    TextRange(document.getLineStartOffset(line1), document.getLineEndOffset(line2)))
        } else {
            val line1 = editor.offsetToLogicalPosition(addedElement.textRange.startOffset).line
            var end = addedElement
            while (size > 0) {
                val tmp = end.nextSibling ?: break
                size -= 1
                end = tmp
            }
            val endOffset = end.textRange.endOffset
            val line2 = editor.offsetToLogicalPosition(endOffset).line
            codeStyleManager.adjustLineIndent(scope.getContainingFile(),
                    TextRange(document.getLineStartOffset(line1), document.getLineEndOffset(line2)))
        }
    }

    private fun addPassStatement(toMove: MyLineRange, project: Project) {
        val startElement = toMove.myStartElement
        val endElement = toMove.myEndElement
        val initialScope = getStatementList(startElement)
        if (initialScope != null && !(startElement === endElement && startElement is PsiComment)) {
            if (initialScope.statements.size == toMove.statementsSize) {
                val passStatement = PyElementGenerator.getInstance(project).createPassStatement()
                initialScope.addAfter(passStatement, initialScope.statements[initialScope.statements.size - 1])
            }
        }
    }

    // use to keep elements
    internal class MyLineRange(val myStartElement: PsiElement, val myEndElement: PsiElement) : LineRange(myStartElement, myEndElement) {
        var size = 0
        var statementsSize = 0

        init {
            if (myStartElement === myEndElement) {
                size = 1
                statementsSize = 1
            } else {
                var counter: PsiElement? = myStartElement
                while (counter !== myEndElement && counter != null) {
                    size += 1
                    if (counter !is PsiWhiteSpace && counter !is PsiComment) statementsSize += 1
                    counter = counter.nextSibling
                }
                size += 1
                if (counter !is PsiWhiteSpace && counter !is PsiComment) statementsSize += 1
            }
        }
    }

    internal class SelectionContainer(val myLen: Int, val myAdditional: Int, val myAtTheBeginning: Boolean)

    // Use when element scope changed
    internal class ScopeRange : LineRange {
        val scope: PsiElement
        val anchor: PsiElement
        val isAddBefore: Boolean
        var isTheSameLevel = false
            private set

        constructor(scope: PsiElement, anchor: PsiElement, before: Boolean) : super(scope) {
            this.scope = scope
            this.anchor = anchor
            isAddBefore = before
        }

        constructor(scope: PyElement, anchor: PsiElement, before: Boolean, b: Boolean) : super(scope) {
            this.scope = scope
            this.anchor = anchor
            isAddBefore = before
            isTheSameLevel = b
        }

    }
}