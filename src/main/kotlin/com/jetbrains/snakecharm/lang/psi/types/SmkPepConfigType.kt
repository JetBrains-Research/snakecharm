package com.jetbrains.snakecharm.lang.psi.types

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.elementType
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
import kotlinx.serialization.modules.EmptySerializersModule
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLScalarText
import org.jetbrains.yaml.psi.impl.YAMLMappingImpl
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

class SmkPepConfigType(smkFile: SmkFile) : PyType {
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
            .filter { it.second == name }
            .forEach {
                resolveResult.poke(it.first, SmkResolveUtil.RATE_NORMAL)
            }
        return resolveResult
    }

    override fun getCompletionVariants(
        completionPrefix: String?,
        location: PsiElement?,
        context: ProcessingContext?
    ): Array<LookupElement> = sectionArgs.map {
        LookupElementBuilder
            .create(it.second)
    }.toTypedArray()

    private val sectionArgs = getYamlKeys(getYamlFile(smkFile))

    override fun getName(): String = "pep.config"

    override fun isBuiltin(): Boolean = false

    override fun assertValid(message: String?) {
        sectionArgs.forEach {
            if (!it.first.isValid) {
                throw PsiInvalidElementAccessException(it.first, message)
            }
        }
    }

    companion object {
        private fun getYamlKeys(yamlFile: PsiFile?): List<Pair<PsiElement, String>> {
            if (yamlFile == null) return emptyList()
            val ymlFile = yamlFile as YAMLFile
            val completionList = mutableListOf<Pair<PsiElement, String>>()
            ymlFile.documents.forEach { document ->
                document.topLevelValue?.children?.asSequence()?.filter { child ->
                    // We can't refer to foo: boo: key
                    child.lastChild !is YAMLMappingImpl
                }?.forEach { child ->
                    completionList.add(
                        // In Yaml file can be key like {foo.boo: }, we can refer to this section
                        // like pep.config.foo, but not pep.config.foo.boo
                        child.firstChild to
                                child.firstChild.text.substringBefore('.')
                    )
                }
            }
            return completionList
        }

        private fun getYamlFile(smkFile: SmkFile): PsiFile? {
            val pepFileSection = smkFile.findPepfile() ?: return null
            val psiElement = pepFileSection.getSectionKeywordNode() as LeafPsiElement
            val virtualFile =
                ProjectRootManager.getInstance(psiElement.project).contentRoots.firstNotNullOfOrNull { root ->
                    root.findFileByRelativePath(psiElement.nextSibling.lastChild.text.trim('"'))
                } ?: return null
            return PsiManager.getInstance(psiElement.project).findFile(virtualFile)
        }
    }
}
