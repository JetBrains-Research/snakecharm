//package com.jetbrains.snakecharm.lang.psi
//
//import com.intellij.psi.PsiElement
//import com.intellij.psi.PsiFile
//import com.jetbrains.rd.util.getOrCreate
//import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
//import org.jetbrains.yaml.psi.YAMLFile
//import org.jetbrains.yaml.psi.YAMLMapping
//
//object SmkPepConfigCollector {
//    fun getYamlParseResult(smkFile: SmkFile): Pair<PsiFile?, List<PsiElement>> {
//        val yamlFile = YamlKeysCollector.getYamlFile(smkFile) ?: return null to emptyList()
//        val yamlFileKeys = cache.getOrCreate(yamlFile) { file ->
//            YamlFileKeys(YamlKeysCollector.getYamlKeys(file), file.modificationStamp)
//        }
//        val keys = when {
//            yamlFileKeys.modificationStamp != yamlFile.modificationStamp -> {
//                YamlKeysCollector.getYamlKeys(yamlFile)
//            }
//            else -> yamlFileKeys.keys
//        }
//        val resultKeys = keys.asSequence().filter { key ->
//            val keyName = key.text
//            val keyType = key.parent.lastChild
//            !(keyName in SnakemakeAPI.PEPPY_CONFIG_TEXT_KEYS && keyType is YAMLMapping ||
//                    keyName in SnakemakeAPI.PEPPY_CONFIG_MAPPING_KEYS && keyType !is YAMLMapping)
//        }.toList()
//        return yamlFile to resultKeys
//    }
//
//    // TODO: memory leak!!!!!!!!! us project component, smart pointers, etc
//    private val cache = mutableMapOf<YAMLFile, YamlFileKeys>()
//}
//
//data class YamlFileKeys(val keys: List<PsiElement>, val modificationStamp: Long)