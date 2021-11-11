//package com.jetbrains.snakecharm.lang.psi.references
//
//import com.intellij.lang.annotation.HighlightSeverity
//import com.intellij.psi.*
//import com.jetbrains.python.psi.PsiReferenceEx
//import com.jetbrains.python.psi.types.TypeEvalContext
//import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
//import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLSubscriptionKeyReference
//
//class SmkPepConfigReference(element: PsiElement, val text: String) :
//    PsiReferenceBase<PsiElement>(element), PsiReferenceEx {
//    override fun resolve(): PsiElement? {
//        TODO()
//    }
//
//    override fun getVariants(): Array<Any> {
//        return arrayOf(SmkCompletionUtil.createPrioritizedLookupElement(text, element))
//    }
//
//    override fun getUnresolvedHighlightSeverity(p0: TypeEvalContext?): HighlightSeverity =
//        SmkSLSubscriptionKeyReference.INSPECTION_SEVERITY
//
//    override fun getUnresolvedDescription(): String =
//        SmkSLSubscriptionKeyReference.unresolvedErrorMsg(element)
//
//}