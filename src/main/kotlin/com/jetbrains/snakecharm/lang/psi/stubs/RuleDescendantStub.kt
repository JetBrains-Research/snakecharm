package com.jetbrains.snakecharm.lang.psi.stubs

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.NamedStub

/**
 * Stub of rule like element which inherits at least one rule
 */
interface RuleDescendantStub<T : PsiNamedElement> : NamedStub<T> {
    fun getInheritedRulesNames(): List<String?>
}