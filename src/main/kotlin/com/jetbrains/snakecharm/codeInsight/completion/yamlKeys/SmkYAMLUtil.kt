package com.jetbrains.snakecharm.codeInsight.completion.yamlKeys

import org.jetbrains.yaml.psi.*

class SmkYAMLUtil {
    companion object {

        fun getKeyValuePsiElementInFile(origin: YAMLKeyValue, path: List<String>): YAMLPsiElement? {
            var yamlPsiElement: YAMLPsiElement? = origin
            // Walks through YAML Psi tree from the child node of rop level key
            for (node in path) {
                // We're getting value here instead of getting it in 'is YAMLMapping'
                // In order to return YAMLKeyValue and produce correct resolve
                yamlPsiElement = (yamlPsiElement as? YAMLKeyValue)?.value ?: yamlPsiElement
                yamlPsiElement = when (yamlPsiElement) {
                    is YAMLMapping -> yamlPsiElement.keyValues.firstOrNull { it.key?.text == node }
                    is YAMLSequence -> {
                        val index =
                            Regex("\\[([0-9]*)]").find(node)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
                        yamlPsiElement.items.getOrNull(index)?.value
                    }
                    else -> return null
                }
            }
            return yamlPsiElement
        }
    }
}