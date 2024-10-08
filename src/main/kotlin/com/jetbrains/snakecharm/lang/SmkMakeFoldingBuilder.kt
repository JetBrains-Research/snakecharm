package com.jetbrains.snakecharm.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.FoldingGroup
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonFoldingBuilder
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes
import java.util.*

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */

private val FOLDED_ELEMENTS = TokenSet.create(
    SmkStubElementTypes.RULE_DECLARATION_STATEMENT,
    SmkStubElementTypes.CHECKPOINT_DECLARATION_STATEMENT,
    SmkElementTypes.WORKFLOW_PY_BLOCK_SECTION_STATEMENT,
    // Sections
    SmkElementTypes.RULE_OR_CHECKPOINT_RUN_SECTION_STATEMENT,
    SmkElementTypes.RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT
)

class SmkMakeFoldingBuilder : PythonFoldingBuilder() {
//    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
////        // TODO: snakemake specific settings fot this
////        return super.isRegionCollapsedByDefault(node)
//    }

    override fun buildLanguageFoldRegions(
            descriptors: MutableList<FoldingDescriptor>,
            root: PsiElement,
            document: Document,
            quick: Boolean
    ) {
        if (root !is SmkFile || root.virtualFile !is LightVirtualFile) {
            collectDescriptors(root.node, descriptors)
        }
    }

    private fun collectDescriptors(node: ASTNode, descriptors: MutableList<FoldingDescriptor>) {
        val type = node.elementType
        if (type == SmkElementTypes.RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT ) {
            val argumentList = (node.psi as SmkRuleOrCheckpointArgsSection).argumentList
            val argListNode = argumentList?.node
            val colon = argListNode?.findChildByType(PyTokenTypes.COLON)
            if (colon != null) {
                val startElement = colon.psi
                val endOffset = argListNode.textRange.endOffset
                val range = TextRange(getVisibleTextOffset(startElement), endOffset)

                if (!range.isEmpty) {
                    if (argumentList.arguments.size > 2 && argumentList.textContains('\n')) {
                        descriptors.add(
                            FoldingDescriptor(
                                argListNode, range,
                                FoldingGroup.newGroup("group"),
                                "...",
                                isRegionCollapsedByDefault(argListNode),
                                Collections.emptySet()
                            )
                        )
                    }
                }
            }
        } else if (type in FOLDED_ELEMENTS) {
            val colon = node.findChildByType(PyTokenTypes.COLON)
            if (colon != null) {
                val endOffset = node.textRange.endOffset
                val startElement = colon.psi
                val range = TextRange(getVisibleTextOffset(startElement), endOffset)

                if (!range.isEmpty) {
                    descriptors.add(
                        FoldingDescriptor(
                            node, range,
                            FoldingGroup.newGroup("group"),
                            "...",
                            isRegionCollapsedByDefault(node),
                            Collections.emptySet()
                        )
                    )
                }
            }
        }
        val children = node.getChildren(null)
        // Process line comments
        // processLineCommentRanges(node, children, descriptors)
        // Recursively process elements
        for (child in children) {
            collectDescriptors(child, descriptors)
        }
    }

    private fun getVisibleTextOffset(element: PsiElement): Int {
        return element.textRange.startOffset + element.textLength
    }

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
        val type = node.elementType
        return when (type) {
            in FOLDED_ELEMENTS -> node.text
            else -> super.getLanguagePlaceholderText(node, range)
        }
    }
}