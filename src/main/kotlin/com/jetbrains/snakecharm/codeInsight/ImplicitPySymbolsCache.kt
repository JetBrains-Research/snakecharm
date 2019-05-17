package com.jetbrains.snakecharm.codeInsight

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleServiceManager
import com.jetbrains.python.psi.PyElement

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-07
 */
class ImplicitPySymbolsCache(private val module: Module) {
    // Update replaces cache with new value
    @Volatile
    private var versAndCache: Pair<Int, Map<String, List<PyElement>>> = 0 to emptyMap()

    companion object {

        fun instance(module: Module) = ModuleServiceManager.getService(
                module, ImplicitPySymbolsCache::class.java
        )!!
    }

    fun find(name: String) = versAndCache.second
            .filter { it.key == name }
            .flatMap { it.value }

    fun all() = versAndCache.second.flatMap { (name, elements) ->
        validElements(elements).map { name to it }
    }


    fun clear() {
        replaceWith(0, emptyMap())
    }

    fun contentVersion() = versAndCache.first

    fun replaceWith(contentVersion: Int, newContent: Map<String, List<PyElement>>) {
        versAndCache = contentVersion to newContent
    }

    private fun validElements(elements: List<PyElement>): List<PyElement> {
        val validElements = elements.filter { it.isValid }
        if (validElements.size != elements.size) {
            ImplicitPySymbolsProvider.instance(module).scheduleUpdate()
        }
        return validElements
    }
}

