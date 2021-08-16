package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.impl.ResolveResultList
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import org.jetbrains.yaml.psi.YAMLFile

class SmkPepConfigType(private val smkFile: SmkFile) : PyType {
    override fun resolveMember(
        name: String,
        location: PyExpression?,
        direction: AccessDirection,
        resolveContext: PyResolveContext
    ): List<RatedResolveResult> {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(location)) {
            return emptyList()
        }
        val resolveResult = ResolveResultList()
        sectionArgs
            .filter { it.text == name }
            .forEach {
                resolveResult.poke(it, SmkResolveUtil.RATE_NORMAL)
            }
        return resolveResult
    }

    override fun getCompletionVariants(
        completionPrefix: String?,
        location: PsiElement?,
        context: ProcessingContext?
    ): Array<LookupElement> {
        return sectionArgs.map {
            LookupElementBuilder
                .create(it.text)
        }.toTypedArray()

    }

    private val sectionArgs = getYamlKeys(getYamlFile(smkFile))

    override fun getName(): String? {
        return "pep.config"
    }

    override fun isBuiltin(): Boolean {
        return false
    }

    override fun assertValid(message: String?) {
    }

    companion object {
        private fun getYamlKeys(yamlFile: PsiFile): List<PsiElement> {
            val ymlFile = yamlFile as YAMLFile
            val completionList = mutableListOf<PsiElement>()
            ymlFile.documents.forEach { document ->
                document.topLevelValue!!.children.forEach { child ->
                    completionList.add(child.firstChild)
                }
            }
            return completionList
        }

        private fun getYamlFile(smkFile: SmkFile): PsiFile {
            val psiElement = smkFile.findPepfile()!!.getSectionKeywordNode() as LeafPsiElement
            val virtualFile =
                ProjectRootManager.getInstance(psiElement.project).contentRoots.firstNotNullOfOrNull { root ->
                    root.findFileByRelativePath(psiElement.nextSibling.lastChild.text.trim('"'))
                }
            return PsiManager.getInstance(psiElement.project).findFile(virtualFile!!)!!
        }
    }
}