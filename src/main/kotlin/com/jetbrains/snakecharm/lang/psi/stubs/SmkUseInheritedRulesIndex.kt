package com.jetbrains.snakecharm.lang.psi.stubs

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import com.jetbrains.snakecharm.lang.psi.SmkUse

class SmkUseInheritedRulesIndex :
    StringStubIndexExtension<SmkUse>() {
    override fun getKey() = KEY

    companion object {
        const val INHERITED_RULES_DECLARATION_VIA_WILDCARD = "*"

        val KEY = StubIndexKey.createIndexKey<String, SmkUse>("Smk.use.inheritedRules")
    }
}