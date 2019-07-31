package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil.getIdentifierNode
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndex
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType


class SmkReferenceExpressionImpl(node: ASTNode): PyElementImpl(node), SmkReferenceExpression {
    override fun getName() = getNameNode()?.text

    override fun setName(name: String): PsiElement {
        val nameElement = PyUtil.createNewName(this, name)
        val nameNode = getNameNode()
        if (nameNode != null) {
            node.replaceChild(nameNode, nameElement)
        }
        return this
    }

    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? = null

    override fun getReference(): PsiReference? = SmkRuleOrCheckpointNameReference(this, TextRange(0, textLength))

    private fun getNameNode() = getIdentifierNode(node)

    private class SmkRuleOrCheckpointNameReference(
            element: PsiNamedElement,
            textRange: TextRange
    ) : PsiReferenceBase<PsiNamedElement>(element, textRange), PsiPolyVariantReference {
        private val key: String = element.text

        override fun resolve(): PsiElement? =
                multiResolve(false).firstOrNull()?.element

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {

            val rules = AbstractSmkRuleOrCheckpointType
                    .findAvailableRuleLikeElementByName(element, key, SmkRuleNameIndex.KEY, SmkRule::class.java)
                    { getRules().map{ (_, psi) -> psi }}
            val checkpoints = AbstractSmkRuleOrCheckpointType
                    .findAvailableRuleLikeElementByName(element, key, SmkCheckpointNameIndex.KEY, SmkCheckPoint::class.java)
                    { getCheckpoints().map{ (_, psi) -> psi }}

            val includedFiles = getIncludedFiles()

            return (rules + checkpoints)
                    .filter { it.containingFile == element.containingFile || it.containingFile in includedFiles }
                    .map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) }
                    .toTypedArray()
        }

        override fun getVariants(): Array<Any> =
                (getRules() + getCheckpoints()).map { (name, elem) ->
                    AbstractSmkRuleOrCheckpointType.createRuleLikeLookupItem(name, elem as SmkRuleOrCheckpoint)
                }.toTypedArray()

        override fun handleElementRename(newElementName: String): PsiElement =
                element.setName(newElementName)

        private fun getRules() = PsiTreeUtil.getParentOfType(element, SmkFile::class.java)
                ?.collectRules() ?: emptyList()

        private fun getCheckpoints() = PsiTreeUtil.getParentOfType(element, SmkFile::class.java)
                ?.collectCheckPoints()
                ?: emptyList()

        private fun getIncludedFiles(): List<SmkFile> {
            val includedFiles = mutableListOf<SmkFile>()
            getIncludedFilesForFile(element.containingFile as SmkFile, includedFiles, mutableSetOf())
            return includedFiles
        }

        private fun getIncludedFilesForFile(
                file: SmkFile,
                includedFiles: MutableList<SmkFile>,
                visitedFiles: MutableSet<SmkFile>
        ) {
            visitedFiles.add(file)
            val currentIncludes = file.collectIncludes()
                    .flatMap { it.references.toList() }
                    .map { it.resolve() }
                    .filterIsInstance<SmkFile>()
            includedFiles.addAll(currentIncludes)
            currentIncludes.forEach {
                if (it !in visitedFiles) {
                    getIncludedFilesForFile(it, includedFiles, visitedFiles)
                }
            }
        }
    }
}

