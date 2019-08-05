package com.jetbrains.snakecharm.codeInsight.resolve

import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.snakecharm.lang.psi.SmkFile

class SmkResolveUtil {
    companion object {
        fun collectToplevelVariables(element: PyStringLiteralExpression): List<PyTargetExpression> {
            val toplevelVariables = mutableListOf<PyTargetExpression>()
            val file = element.containingFile as SmkFile

            toplevelVariables.addAll(file.topLevelAttributes)
            collectToplevelAttributesFromFile(file, mutableSetOf(), toplevelVariables)

            return toplevelVariables
        }


        private fun collectToplevelAttributesFromFile(
                file: SmkFile,
                visitedFiles: MutableSet<SmkFile>,
                availableVariables: MutableList<PyTargetExpression>
        ) {
            visitedFiles.add(file)
            availableVariables.addAll(file.topLevelAttributes)
            val includeStatements = file.collectIncludes()
            for (statement in includeStatements) {
                for (reference in statement.references) {
                    val resolvedFile = reference.resolve() as? SmkFile
                    if (resolvedFile != null && resolvedFile !in visitedFiles) {
                        collectToplevelAttributesFromFile(resolvedFile, visitedFiles, availableVariables)
                    }
                }
            }
        }
    }
}