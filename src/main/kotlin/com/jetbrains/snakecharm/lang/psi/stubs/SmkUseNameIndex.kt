package com.jetbrains.snakecharm.lang.psi.stubs

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.jetbrains.snakecharm.lang.psi.SmkUse

class SmkUseNameIndex : StringStubIndexExtension<SmkUse>() {
    override fun getKey() = SmkUseNameIndexCompanion.KEY
}

object SmkUseNameIndexCompanion {
    val KEY = StubIndexKey.createIndexKey<String, SmkUse>("Smk.use.shortName")

    fun find(
        name: String,
        project: Project,
        scope: GlobalSearchScope = ProjectScope.getAllScope(project)
    ): Collection<SmkUse> =
        StubIndex.getElements(KEY, name, project, scope, SmkUse::class.java)
}