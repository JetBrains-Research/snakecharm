package com.jetbrains.snakecharm.codeInsight.wrapper

class SmkWrapperUtil {
    companion object {
        const val SMK_WRAPPER_FILE_NAME = "wrapper.py"
        const val SMK_ENVIRONMENT_FILE_NAME = "environment.yaml"
        const val SMK_META_FILE_NAME = "meta.yaml"
        const val SMK_TEST_DIRECTORY_NAME = "test"
        const val SMK_TEST_SNAKEFILE_NAME = "Snakefile"

        const val TAG_NUMBER_REGEX_STRING = "v?(\\d*)\\.(\\d*)\\.(\\d*)"
        val TAG_NUMBER_REGEX = Regex("^$TAG_NUMBER_REGEX_STRING/")

        const val WRAPPER_PREFIX = "https://bitbucket.org/snakemake/snakemake-wrappers/raw/"
    }
}