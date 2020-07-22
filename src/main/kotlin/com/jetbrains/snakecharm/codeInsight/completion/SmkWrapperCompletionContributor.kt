package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil.getParentOfType
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import java.io.File

class SmkWrapperCompletionContributor : CompletionContributor() {
    init {
        extend(
                CompletionType.BASIC,
                SmkWrapperCompletionProvider.CAPTURE,
                SmkWrapperCompletionProvider
        )
    }
}

object SmkWrapperCompletionProvider : CompletionProvider<CompletionParameters>() {

    val CAPTURE = PlatformPatterns.psiElement()
            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(SmkRuleOrCheckpointArgsSection::class.java)
            .inside(PyStringLiteralExpression::class.java)!!
    var WRAPPERS_PATH = "${System.getProperty("user.home")}/snakemake-wrappers"
    const val META_FILE_NAME = "metadata.csv"

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        if (getParentOfType(parameters.position, SmkRuleOrCheckpointArgsSection::class.java)?.name !=
                SnakemakeNames.SECTION_WRAPPER) {
            return
        }
        if (!File("$WRAPPERS_PATH/$META_FILE_NAME").exists()) {
            localWrapperParser()
        }
        File("$WRAPPERS_PATH/$META_FILE_NAME").forEachLine { str ->
            val substr = str.substringBefore(',')
            if (substr.contains(result.prefixMatcher.prefix, false)) {
//                TODO use regex that saves version if it is typed
                result.addElement(LookupElementBuilder.create("0.63.0/$substr").withIcon(PlatformIcons.PARAMETER_ICON))
            }
        }
    }
}

fun localWrapperParser() {
    val wrappers: MutableList<String> = mutableListOf<String>()
    val args: MutableList<MutableList<String>> = mutableListOf<MutableList<String>>()
    val mainFolder = File(SmkWrapperCompletionProvider.WRAPPERS_PATH)
    mainFolder.walkTopDown().forEach { child ->
        if (child.name.endsWith("wrapper.py", false)) {
            wrappers.add(child.toRelativeString(mainFolder).removeSuffix("/wrapper.py"))
            args.add(
                    Regex("\\{snakemake\\.\\S*}")
                            .findAll(child.readText()).map { str ->
                                str
                                        .value
                                        .drop(11)
                                        .dropLast(1)
                            }
                            .toSortedSet()
                            .toMutableList()
            )
        }
    }

    val output = File("${SmkWrapperCompletionProvider.WRAPPERS_PATH}/${SmkWrapperCompletionProvider.META_FILE_NAME}").outputStream().writer()
    wrappers.forEachIndexed() { i, name ->
        output.append(name)
        args[i].forEach { str ->
            output.append(",$str")
        }
        output.appendln("")
    }
    output.close()
}
