package com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations

data class SmkApiAnnotationParsingVersionRecord(
    val version: String = "",
    val introduced: List<SmkApiAnnotationParsingIntroductionRecord> = emptyList(),
    val override: List<SmkApiAnnotationParsingIntroductionRecord> = emptyList(),
    val deprecated: List<SmkApiAnnotationParsingDeprecationRecord> = emptyList(),
    val removed: List<SmkApiAnnotationParsingDeprecationRecord> = emptyList(),
)

interface SmkApiAnnotationParsingAbstractRecord {
    val name: String
    val type: String
}

data class SmkApiAnnotationParsingDeprecationRecord(
    override val name: String = "",
    override val type: String = "",
    val advice: String = "",
): SmkApiAnnotationParsingAbstractRecord

data class SmkApiAnnotationParsingIntroductionRecord(
    override val name: String = "",
    override val type: String = "",
    val advice: String = "",
    val docs_url: String = "",

    // for sections:
    val lambda_args: List<String> = emptyList<String>(),
    val args_section: Boolean = true,
    val keyword_args_allowed: Boolean = true,
    val multiple_args_allowed: Boolean = true,
    val placeholders_injection_allowed: Boolean? = null,
    val placeholders_resolved_as_wildcards: Boolean = false,
    val is_accessible_in_rule_obj: Boolean = false,
    val is_accessible_as_placeholder: Boolean = false,
    val execution_section: Boolean = false,

    // for functions:
    val limit_to_sections: List<String> = emptyList<String>(),
): SmkApiAnnotationParsingAbstractRecord

data class SmkApiAnnotationParsingConfig(
    val changelog: List<SmkApiAnnotationParsingVersionRecord> = emptyList(),
    val defaultVersion: String = "0.0.0",
    val annotationsFormatVersion: Int = 0
)

enum class SmkApiAnnotationParsingContextType(val typeStr: String) {
    TOP_LEVEL("top-level"),
    RULE_LIKE("rule-like"),
    FUNCTION("function"),
}
