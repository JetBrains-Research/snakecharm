package com.jetbrains.snakecharm.lang.psi.stubs

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint

class SmkCheckpointNameIndex: StringStubIndexExtension<SmkCheckPoint>() {
    override fun getKey() = KEY

    companion object {
        val KEY = StubIndexKey.createIndexKey<String, SmkCheckPoint>("Smk.checkpoint.shortName")

        fun find(
                name: String,
                project: Project,
                scope: GlobalSearchScope = ProjectScope.getAllScope(project)
        ): Collection<SmkCheckPoint> =
                StubIndex.getElements(KEY, name, project, scope, SmkCheckPoint::class.java)
    }
}
