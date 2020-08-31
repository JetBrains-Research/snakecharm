package com.jetbrains.snakecharm.codeInsight.completion.wrapper

import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import com.intellij.util.io.write
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

        println("Found ${wrappers.size} wrappers")
        Paths.get(outputFile).write(Cbor.encodeToByteArray(wrappers))
    }


    fun localWrapperParser(folder: String, relative: Boolean = false): List<SmkWrapperStorage.Wrapper> {
        val wrappers = mutableListOf<SmkWrapperStorage.Wrapper>()
        val mainFolder = File(folder)

        mainFolder.walkTopDown()
            .filter { it.isFile && it.name.startsWith("wrapper") }
            .forEach { wrapperFile ->

                val path = if (relative) {
                    wrapperFile.parentFile.absolutePath
                } else {
                    wrapperFile.parentFile.toRelativeString(mainFolder)
                }

                val args = when (wrapperFile.extension) {
                    "py" -> parseArgsPython(wrapperFile.readText())
                    "R" -> parseArgsR(wrapperFile.readText())
                    else -> emptyMap()
                }

                val metaYaml = wrapperFile.resolveSibling("meta.yaml")
                val description = when {
                    metaYaml.exists() -> metaYaml.readText()
                    else -> "N/A"
                }

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
        return Regex("snakemake\\.\\w*(\\.(get\\(\"\\w*\"|[^get]\\w*)|\\[\\d+\\])?")
            .findAll(text).map { str ->
                str.value
                    .substringAfter("snakemake.")
            }
            .toSortedSet()
            .toList()
            .map {
                val splitted = it.split('.', ignoreCase = false, limit = 2)
                when {
                    splitted.size != 2
                    -> splitted[0].substringBefore('[') to ""
                    splitted[1].startsWith("get")
                    -> splitted[0] to splitted[1].removeSurrounding("get(\"", "\"")
                    else
                    -> splitted[0] to splitted[1]
                }
            }
            .groupBy({ it.first }, { it.second })
    }

    fun parseArgsR(text: String): Map<String, List<String>> {
        return Regex("snakemake@\\w*(\\[\\[\"\\w*\"\\]\\])?")
            .findAll(text).map { str ->
                str.value
                    .substringAfter("snakemake@")
            }
            .toSortedSet()
            .toList()
            .map {
                val splitted = it.split('[', ignoreCase = false, limit = 2)
                if (splitted.size == 2) {
                    splitted[0] to splitted[1].substringAfter('"').substringBefore('"')
                } else {
                    splitted[0] to ""
                }
            }
            .groupBy({ it.first }, { it.second })
    }
}