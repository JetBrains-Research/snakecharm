package com.jetbrains.snakecharm.lang.psi.stubs

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.jetbrains.snakecharm.lang.psi.SmkRule


class SmkRuleNameIndex : StringStubIndexExtension<SmkRule>() {
    override fun getKey() = SmkRuleNameIndexCompanion.KEY
}

object SmkRuleNameIndexCompanion {
    val KEY = StubIndexKey.createIndexKey<String, SmkRule>("Smk.rule.shortName")

    fun find(
        name: String,
        project: Project,
        scope: GlobalSearchScope = ProjectScope.getAllScope(project)
    ): Collection<SmkRule> =
        StubIndex.getElements(KEY, name, project, scope, SmkRule::class.java)

    // fun allKeys(project: Project) = StubIndex.getInstance().getAllKeys(KEY, project)
}