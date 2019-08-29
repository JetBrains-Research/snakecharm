package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.extensions.python.toPsi
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SmkWrapperCompletionProvider
import com.jetbrains.snakecharm.codeInsight.wrapper.SmkWrapperUtil
import com.jetbrains.snakecharm.codeInsight.wrapper.WrapperStorage

class SmkWrapperReference(element: PyStringLiteralExpression) : PsiReferenceBase<PyStringLiteralExpression>(element) {
    override fun resolve(): PsiElement? {
        val wrappers = WrapperStorage.getInstance().getWrapperList()
        // return empty array in case there is no tag because urls without tags are not valid
        val referencedWrapperPath = element.text.replace("\"", "")
        val tag = SmkWrapperCompletionProvider.tagNumberRegex.find(referencedWrapperPath)?.value ?: return null
        val directoryPath = let {
            val path = referencedWrapperPath.substringAfter(tag)
            if (path.endsWith("/")) {
                path.trimEnd('/')
            } else {
                path
            }
        }
        if (tag.replace("/", "") in wrappers.map { it.repositoryTag }) {
            wrappers.find { it.pathToWrapperDirectory == directoryPath } ?: return null
        }

        val wrapperUrl = "${WRAPPER_PREFIX}$tag$directoryPath/${SmkWrapperUtil.SMK_WRAPPER_FILE_NAME}"
        return VirtualFileManager.getInstance().findFileByUrl(wrapperUrl)?.toPsi(element.project)
    }



    companion object {
        // TODO make it possible for user to change this when it's placed elsewhere in the settings
        private const val WRAPPER_PREFIX = "https://bitbucket.org/snakemake/snakemake-wrappers/raw/"
    }
}