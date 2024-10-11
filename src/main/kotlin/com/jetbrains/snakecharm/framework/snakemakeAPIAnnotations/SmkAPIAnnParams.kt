package com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations

import com.jetbrains.snakecharm.lang.SmkLanguageVersion

data class SmkKeywordDeprecationParams(
    val type: SmkAPIAnnDeprecationType,
    val advice: String?,
) {
    companion object {
        fun createFrom(type: SmkAPIAnnDeprecationType, record: SmkAPIAnnParsingDeprecationRecord) = SmkKeywordDeprecationParams(
            type = type,
            advice = record.advice.ifEmpty { null },
        )
    }
}

data class SmkKeywordIntroductionParams(
    val lambdaArgs: List<String>,
    val keywordArgsAllowed: Boolean,
    val multipleArgsAllowed: Boolean,
    val isSection: Boolean,
) {
    companion object {
        fun createFrom(rec: SmkAPIAnnParsingIntroductionRecord) = SmkKeywordIntroductionParams(
            lambdaArgs = rec.lambda_args,
            keywordArgsAllowed = rec.keyword_args_allowed,
            multipleArgsAllowed = rec.multiple_args_allowed,
            isSection = rec.section,
        )
    }
}

enum class SmkAPIAnnDeprecationType {
    REMOVED, DEPRECATED
}

data class SmkAPIAnnDeprecationInfo(
    val updateType: SmkAPIAnnDeprecationType,
    val advice: String?,
    val version: SmkLanguageVersion,
    val isGlobalChange: Boolean // XXX seems we don't need it any more, was for `global` subsections mainly
)