package com.jetbrains.snakecharm.lang.psi

import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.yaml.psi.YAMLFile

object YamlKeysCollector {
    fun getYamlKeys(yamlFile: YAMLFile): List<PsiElement> {
        yamlFile.modificationStamp
        val resultList = mutableListOf<PsiElement>()
        yamlFile.documents.forEach { document ->
            val topLevelValue = document.topLevelValue
            // TODO: use high level API!
            topLevelValue?.children?.forEach { child ->
                resultList.add(child.firstChild)
            }
        }
        return resultList
    }

    fun getYamlFile(smkFile: SmkFile): YAMLFile? {
        val pepFileSection = smkFile.findPepfile() ?: return null
        // TODO: use high level API!
        //       The whole method 1) is using low level PSI access and parser impl details 2) duplicates some code from SmkPepfileReference reference 3) doesn't work properly if reference uses single quoting
        //        Re-implement this using high-level PSI access (e.g. without LeafPsiElement, nextSibling, lastChild, text methods). Use the fact, that if everyting is ok, you already have SmkPepfileReference reference here and it is know how to resolve it's target, no need to copy resolve implementation here. Before commiting this change please show me new impl in Slack.
        val psiElement = pepFileSection.getSectionKeywordNode() as LeafPsiElement
        val virtualFile =
            ProjectRootManager.getInstance(psiElement.project).contentRoots.firstNotNullOfOrNull { root ->
                root.findFileByRelativePath(psiElement.nextSibling.lastChild.text.trim('"'))
            } ?: return null
        val psiFile = PsiManager.getInstance(psiElement.project).findFile(virtualFile)
        return if (psiFile is YAMLFile) psiFile else null
    }
}