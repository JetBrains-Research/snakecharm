package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSection
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLWildcardReference

@Suppress("UnstableApiUsage")
interface SmkSLReferenceExpression : PyReferenceExpression, SmkSLExpression, PsiNameIdentifierOwner {
    override fun getName(): String?

    fun containingRuleOrCheckpointSection(): SmkRuleOrCheckpointArgsSection? =
        PsiTreeUtil.getParentOfType(injectionHost(), SmkRuleOrCheckpointArgsSection::class.java)

    fun containingSection(): SmkSection? = PsiTreeUtil.getParentOfType(injectionHost(), SmkSection::class.java)

    override fun getNameIdentifier() = nameElement?.psi
    override fun setName(name: String) = SmkResolveUtil.renameNameNode(name, nameElement, this)

    fun isWildcard() = reference is SmkSLWildcardReference
}
