package com.jetbrains.snakecharm.actions

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.stubs.SmkCheckpointNameIndex
import com.jetbrains.snakecharm.lang.psi.stubs.SmkRuleNameIndex

class SmkGotoSymbolContributor: ChooseByNameContributorEx{
    override fun getItemsByName(name: String, pattern: String?, project: Project?, includeNonProjectItems: Boolean): Array<NavigationItem> {
        val result = ArrayList<NavigationItem>()
        processElementsWithName(
                name, result::add,
                FindSymbolParameters(
                        "", "", FindSymbolParameters.searchScopeFor(project, includeNonProjectItems),
                        IdFilter.getProjectIdFilter(project, includeNonProjectItems)
                )
        )
        return if (result.isEmpty()) NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY else result.toTypedArray()
    }

    override fun getNames(project: Project?, includeNonProjectItems: Boolean): Array<String> {
        val result = ArrayList<String>()
        processNames(result::add, FindSymbolParameters.searchScopeFor(project, includeNonProjectItems), null)
        return result.toTypedArray()
    }

    override fun processNames(
            processor: Processor<String>,
            scope: GlobalSearchScope,
            filter: IdFilter?
    ) {
        StubIndex.getInstance().processAllKeys(SmkRuleNameIndex.KEY, processor, scope, filter)
        StubIndex.getInstance().processAllKeys(SmkCheckpointNameIndex.KEY, processor, scope, filter)
    }

    override fun processElementsWithName(
            name: String,
            processor: Processor<NavigationItem>,
            parameters: FindSymbolParameters
    ) {
        val project = parameters.project
        val scope = parameters.searchScope
        val index = StubIndex.getInstance()
        
        index.processElements(
                SmkRuleNameIndex.KEY, name,
                project, scope, parameters.idFilter,
                SMKRule::class.java,
                processor
        )
        index.processElements(
                SmkCheckpointNameIndex.KEY, name,
                project, scope, parameters.idFilter,
                SMKCheckPoint::class.java,
                processor
        )

    }
}