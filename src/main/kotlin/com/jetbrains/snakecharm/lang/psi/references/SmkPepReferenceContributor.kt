//package com.jetbrains.snakecharm.lang.psi.references
//
//import com.intellij.patterns.PatternCondition
//import com.intellij.patterns.PlatformPatterns.psiElement
//import com.intellij.patterns.StandardPatterns.instanceOf
//import com.intellij.psi.*
//import com.intellij.util.ProcessingContext
//import com.jetbrains.python.psi.*
//import com.jetbrains.snakecharm.codeInsight.completion.SmkKeywordCompletionContributor
//import com.jetbrains.snakecharm.lang.psi.SmkFile
//import com.jetbrains.snakecharm.lang.psi.SmkPepConfigCollector
//
//class SmkPepReferenceContributor : PsiReferenceContributor() {
//    companion object {
//
//        val IN_PEP_CONFIG = psiElement(PyStringLiteralExpression::class.java)
//            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
//            .inside(true, instanceOf(PySubscriptionExpression::class.java))
//            .with(object : PatternCondition<PsiElement>("is after pep.config") {
//                override fun accepts(element: PsiElement, context: ProcessingContext): Boolean =
//                    element.parent.firstChild.text == "pep.config"
//            })
//            .inside(instanceOf(PyExpressionStatement::class.java))
//
//    }
//
//    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
//        registrar.registerReferenceProvider(
//            IN_PEP_CONFIG, SmkPepConfigInSubscriptionReferenceProvider
//        )
//    }
//}
//
//object SmkPepConfigInSubscriptionReferenceProvider : PsiReferenceProvider() {
//    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
//        return SmkPepConfigCollector.getYamlParseResult(element.containingFile as SmkFile).second.map{ key ->
//            //TODO
//            SmkPepConfigReference(key, key.text)
//        }.toTypedArray()
//    }
//
//}