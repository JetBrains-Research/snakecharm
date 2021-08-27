package com.jetbrains.snakecharm.lang.psi

import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.psi.YAMLFile

object YamlKeysCollector {
    fun getYamlKeys(yamlFile: PsiFile?): List<PsiElement> {
        if (yamlFile == null) return emptyList()
        val ymlFile = yamlFile as YAMLFile
        ymlFile.modificationStamp
        val resultList = mutableListOf<PsiElement>()
        ymlFile.documents.forEach { document ->
            document.topLevelValue?.children?.forEach { child ->
                resultList.add(child.firstChild)
            }
        }
        return resultList
    }

    fun getYamlFile(smkFile: SmkFile): PsiFile? {
        val pepFileSection = smkFile.findPepfile() ?: return null
        val psiElement = pepFileSection.getSectionKeywordNode() as LeafPsiElement
        val virtualFile =
            ProjectRootManager.getInstance(psiElement.project).contentRoots.firstNotNullOfOrNull { root ->
                root.findFileByRelativePath(psiElement.nextSibling.lastChild.text.trim('"'))
            } ?: return null
        return PsiManager.getInstance(psiElement.project).findFile(virtualFile)
    }
}