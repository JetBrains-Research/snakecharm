package com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations

data class SmkKeywordDeprecationParams(
    val name: String,
    val itemRemoved: Boolean,
    val advice: String?,
) {
    companion object {
        fun createFrom(itemRemoved: Boolean, record: SmkAPIAnnParsingDeprecationRecord) = SmkKeywordDeprecationParams(
            name = record.name,
            itemRemoved = itemRemoved,
            advice = record.advice.ifEmpty { null },
        )
    }
}

data class SmkKeywordIntroductionParams(
    val lambdaArgs: List<String>,
    val keywordArgsAllowed: Boolean,
    val multipleArgsAllowed: Boolean,
    val isSection: Boolean,
    val isPlaceholderInjectionAllowed: Boolean,
    val isPlaceholderExpandedToWildcard: Boolean,
    val isAccessibleInRuleObj: Boolean,
    val isAccessibleAsPlaceholder: Boolean,
) {
    companion object {
        fun createFrom(rec: SmkAPIAnnParsingIntroductionRecord) = SmkKeywordIntroductionParams(
            lambdaArgs = rec.lambda_args,
            keywordArgsAllowed = rec.keyword_args_allowed,
            multipleArgsAllowed = rec.multiple_args_allowed,
            isSection = rec.section,
            isPlaceholderInjectionAllowed = rec.placeholders_injection_allowed,
            isPlaceholderExpandedToWildcard = rec.placeholders_resolved_as_wildcards,
            isAccessibleInRuleObj = rec.is_accessible_in_rule_obj,
            isAccessibleAsPlaceholder = rec.is_accessible_as_placeholder,
        )
    }
}