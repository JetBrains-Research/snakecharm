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
    //val name: String,
    val lambdaArgs: List<String>,
    val keywordArgsAllowed: Boolean,
    val multipleArgsAllowed: Boolean,
    val isSection: Boolean,
    val isPlaceholderInjectionAllowed: Boolean,
    val isPlaceholderExpandedToWildcard: Boolean,
    val isAccessibleInRuleObj: Boolean,
    val isAccessibleAsPlaceholder: Boolean,
    val limitToSections: List<String>,
) {
    companion object {
        fun createFrom(rec: SmkAPIAnnParsingIntroductionRecord) = SmkKeywordIntroductionParams(
            //name = rec.name,
            lambdaArgs = rec.lambda_args,
            limitToSections = rec.limit_to_sections,
            keywordArgsAllowed = rec.keyword_args_allowed,
            multipleArgsAllowed = rec.multiple_args_allowed,
            isSection = rec.section,
            isPlaceholderInjectionAllowed = when (rec.placeholders_injection_allowed) {
                true -> true
                false -> false
                else -> {
                    // default: FALSE for functions, TRUE for sections
                    rec.type != SmkAPIAnnParsingContextType.FUNCTION.typeStr
                }
            },
            isPlaceholderExpandedToWildcard = rec.placeholders_resolved_as_wildcards,
            isAccessibleInRuleObj = rec.is_accessible_in_rule_obj,
            isAccessibleAsPlaceholder = rec.is_accessible_as_placeholder,
        )
    }
}