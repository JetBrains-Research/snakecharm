package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.NamedStub
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyNames.UNNAMED_ELEMENT
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyBaseElementImpl
import com.jetbrains.python.psi.impl.PyElementPresentation
import com.jetbrains.python.psi.impl.PyPsiUtils
import com.jetbrains.snakecharm.SnakemakeIcons
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike
import com.jetbrains.snakecharm.lang.psi.SmkSection
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil.getIdentifierNode
import javax.swing.Icon

abstract class SmkRuleLikeImpl<StubT : NamedStub<PsiT>, PsiT: SmkRuleLike<S>, out S : SmkSection>
    : PyBaseElementImpl<StubT>, SmkRuleLike<S>

    //TODO: PyNamedElementContainer; PyStubElementType<SMKRuleStub, SmkRule>
    // SnakemakeNamedElement, SnakemakeScopeOwner

{
    constructor(node: ASTNode): super(node)
    constructor(stub: StubT, nodeType: IStubElementType<StubT, PsiT>): super(stub, nodeType)

    override fun getName(): String? {
        val stub = stub
        if (stub != null) {
            return stub.name
        }

        return getNameNode()?.text
    }

    override fun setName(name: String): PsiElement {
        val newNameNode = PyUtil.createNewName(this, name)
        getNameNode()?.let {
            node.replaceChild(it, newNameNode)
        }
        return this
    }

    override fun getSectionKeywordNode()= node.findChildByType(sectionTokenType)

    override fun getNameIdentifier() = getNameNode()?.psi

    /**
     * Use name start offset here, required for navigation & find usages, e.g. when ask for usages on name identifier
     */
    override fun getTextOffset() = getNameNode()?.startOffset ?: super.getTextOffset()

    private fun getNameNode() = getIdentifierNode(node)

    override fun getSectionByName(sectionName: String): S? {
        require(sectionName != SnakemakeNames.SECTION_RUN) {
            "Run section not supported here"
        }

        return getSections().find {
            it.sectionKeyword == sectionName
        } as? S
    }

    override fun getStatementList() = childToPsiNotNull<PyStatementList>(PyElementTypes.STATEMENT_LIST)

    // iterate over children, not statements, since SMKRuleRunParameter isn't a statement
    override fun getSections(): List<SmkSection> = statementList.children.filterIsInstance<SmkSection>()

    override fun getIcon(flags: Int): Icon {
        PyPsiUtils.assertValid(this)
        return SnakemakeIcons.FILE
    }

    override fun getPresentation() = object: PyElementPresentation(this) {
        override fun getPresentableText() =
                "${SnakemakeLexer.KEYWORDS_2_TEXT[sectionTokenType]}: ${name ?: UNNAMED_ELEMENT}"

        override fun getLocationString() = "(${containingFile.name})"
    }
}