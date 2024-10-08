package com.jetbrains.snakecharm.actions

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndexCompanion
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndexCompanion

class SmkGotoSymbolContributor : ChooseByNameContributorEx {

    override fun processNames(
        processor: Processor<in String>,
        scope: GlobalSearchScope,
        filter: IdFilter?
    ) {
        StubIndex.getInstance().processAllKeys(SmkRuleNameIndexCompanion.KEY, processor, scope, filter)
        StubIndex.getInstance().processAllKeys(SmkCheckpointNameIndexCompanion.KEY, processor, scope, filter)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters
    ) {
        val project = parameters.project
        val scope = parameters.searchScope
        val index = StubIndex.getInstance()

        index.processElements(
            SmkRuleNameIndexCompanion.KEY, name,
            project, scope, parameters.idFilter,
            SmkRule::class.java,
            processor
        )
        index.processElements(
            SmkCheckpointNameIndexCompanion.KEY, name,
            project, scope, parameters.idFilter,
            SmkCheckPoint::class.java,
            processor
        )

    }
}