package com.jetbrains.snakecharm.codeInsight.wrapper

import java.util.regex.Pattern

class SmkWrapperUtil {
    companion object {
        const val SMK_WRAPPER_FILE_NAME = "wrapper.py"
        const val SMK_ENVIRONMENT_FILE_NAME = "environment.yaml"
        const val SMK_META_FILE_NAME = "meta.yaml"
        const val SMK_TEST_DIRECTORY_NAME = "test"
        const val SMK_TEST_SNAKEFILE_NAME = "Snakefile"

        private const val TAG_NUMBER_REGEX_STRING = "v?(\\d*)\\.(\\d*)\\.(\\d*)"
        val TAG_NUMBER_REGEX = Regex("^$TAG_NUMBER_REGEX_STRING/")

        const val WRAPPER_PREFIX = "https://bitbucket.org/snakemake/snakemake-wrappers/raw/"

        fun sortTags(tags: List<String>): List<String> {
            val tagNumberPattern = Pattern.compile(TAG_NUMBER_REGEX_STRING)
            return tags.sortedWith(compareBy({ tag ->
                val matcher = tagNumberPattern.matcher(tag)
                matcher.find()
                matcher.group(1).toInt()
            }, { tag ->
                val matcher = tagNumberPattern.matcher(tag)
                matcher.find()
                matcher.group(2).toInt()
            }, { tag ->
                val matcher = tagNumberPattern.matcher(tag)
                matcher.find()
                matcher.group(3).toInt()
            }))
        }
    }
}