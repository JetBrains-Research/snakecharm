package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import com.intellij.util.io.write
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import java.io.File
import java.nio.file.Paths

object SmkWrapperCrawler {
    @ExperimentalSerializationApi
    @JvmStatic
    fun main(args: Array<String>) {
        println("Usage: SmkWrapperCrawler {WRAPPERS_SRC_ROOT_FOLDER} {WRAPPERS_INFO_CBOR_OUTPUT}")
        println("Smk wrapper crawler args: ${args.joinToString()}")
        require(args.size == 2) {
            "2 input args expected, but was: ${args.size}"
        }

        val wrappersFolder = args[0]
        val wrappersFolderPath = Paths.get(wrappersFolder)
        require(wrappersFolderPath.exists()) {
            "Wrappers src folder doesn't exist: $wrappersFolder"
        }
        require(wrappersFolderPath.isDirectory()) {
            "Wrappers src folder isn't a folder: $wrappersFolder"
        }
        require(Paths.get("$wrappersFolder/bio").exists()) {
            "Wrappers src folder doesn't contain \"bio\" folder: $wrappersFolder"
        }

        val outputFile = args[1]

        println("Launching smk wrappers crawler...")
        val wrappers = localWrapperParser(wrappersFolder, true)
        wrappers.forEach { wrapper ->
            println(wrapper.path)
        }

        println("Found ${wrappers.size} wrappers")
        Paths.get(outputFile).write(Cbor.encodeToByteArray(wrappers))
    }


    fun localWrapperParser(folder: String, relativePath: Boolean = false): List<SmkWrapperStorage.Wrapper> {
        val wrappers = mutableListOf<SmkWrapperStorage.Wrapper>()
        val mainFolder = File(folder)

        mainFolder.walkTopDown()
            .filter { it.isFile && it.name.startsWith("wrapper") }
            .forEach { wrapperFile ->
                val metaYaml = wrapperFile.resolveSibling("meta.yaml")

                if (!metaYaml.exists()) {
                    // not a wrapper
                    return@forEach
                }

                val path = if (relativePath) {
                    wrapperFile.parentFile.toRelativeString(mainFolder)
                } else {
                    wrapperFile.parentFile.absolutePath
                }

                val args = when (wrapperFile.extension.toLowerCase()) {
                    "py" -> parseArgsPython(wrapperFile.readText())
                    "r" -> parseArgsR(wrapperFile.readText())
                    else -> emptyMap()
                }


                val description = metaYaml.readText()

                wrappers.add(
                    SmkWrapperStorage.Wrapper(
                        path = path,
                        args = args,
                        description = description
                    )
                )
            }

        return wrappers.toList()
    }

    fun parseArgsPython(text: String): Map<String, List<String>> {
        return Regex("(?<!from\\s|import\\s)snakemake\\.\\w*(\\.(get\\(\"\\w*\"|[^get]\\w*)|\\[\\d+\\])?")
            .findAll(text).map { str ->
                str.value
                    .substringAfter("snakemake.")
            }
            .toSortedSet()
            .toList()
            .map {
                val chunks = it.split('.', ignoreCase = false, limit = 2)
                when {
                    chunks.size < 2 -> {
                        chunks[0].substringBefore('[') to ""
                    }
                    chunks[1].startsWith("get") -> {
                        chunks[0] to chunks[1].removeSurrounding("get(\"", "\"")
                    }
                    else -> {
                        chunks[0] to chunks[1]
                    }
                }
            }
            .filter { it.first in SnakemakeAPI.RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS }
            .groupBy({ it.first }, { it.second })
    }

    fun parseArgsR(text: String): Map<String, List<String>> {
        return Regex("(?<!from\\s)snakemake@\\w*(\\[\\[\"\\w*\"\\]\\])?")
            .findAll(text).map { str ->
                str.value
                    .substringAfter("snakemake@")
            }
            .toSortedSet()
            .toList()
            .map {
                val chunks = it.split('[', ignoreCase = false, limit = 2)
                if (chunks.size == 2) {
                    chunks[0] to chunks[1].substringAfter('"').substringBefore('"')
                } else {
                    chunks[0] to ""
                }
            }
            .filter { it.first in SnakemakeAPI.RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS }
            .groupBy({ it.first }, { it.second })
    }
}