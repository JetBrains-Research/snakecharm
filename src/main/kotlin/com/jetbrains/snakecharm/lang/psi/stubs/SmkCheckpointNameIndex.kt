package com.jetbrains.snakecharm.lang.psi.stubs

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint

class SmkCheckpointNameIndex: StringStubIndexExtension<SMKCheckPoint>() {
    override fun getKey() = KEY

    companion object {
        val KEY = StubIndexKey.createIndexKey<String, SMKCheckPoint>("Smk.checkpoint.shortName")

        fun find(
                name: String,
                project: Project,
                scope: GlobalSearchScope = ProjectScope.getAllScope(project)
        ): Collection<SMKCheckPoint> =
                StubIndex.getElements(KEY, name, project, scope, SMKCheckPoint::class.java)
    }
}
