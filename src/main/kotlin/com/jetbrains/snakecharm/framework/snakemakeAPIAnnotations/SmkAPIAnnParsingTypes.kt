package com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations

data class SmkAPIAnnParsingVersionRecord(
    val version: String = "",
    val introduced: List<SmkAPIAnnParsingIntroductionRecord> = emptyList(),
    val override: List<SmkAPIAnnParsingIntroductionRecord> = emptyList(),
    val deprecated: List<SmkAPIAnnParsingDeprecationRecord> = emptyList(),
    val removed: List<SmkAPIAnnParsingDeprecationRecord> = emptyList(),
)

interface SmkAPIAnnParsingAbstractRecord {
    val name: String
    val type: String
}

data class SmkAPIAnnParsingDeprecationRecord(
    override val name: String = "",
    override val type: String = "",
    val advice: String = "",
): SmkAPIAnnParsingAbstractRecord

data class SmkAPIAnnParsingIntroductionRecord(
    override val name: String = "",
    override val type: String = "",
    val advice: String = "",
    val lambda_args: List<String> = emptyList<String>(),
    val section: Boolean = true,
    val keyword_args_allowed: Boolean = true,
    val multiple_args_allowed: Boolean = true,
    val placeholders_injection_allowed: Boolean = true,
    val placeholders_resolved_as_wildcards: Boolean = false,
    val is_accessible_in_rule_obj: Boolean = false,
): SmkAPIAnnParsingAbstractRecord

data class SmkAPIAnnParsingConfig(
    val changelog: List<SmkAPIAnnParsingVersionRecord> = emptyList(),
    val defaultVersion: String = "0.0.0",
    val annotationsFormatVersion: Int = 0
)

enum class SmkAPIAnnParsingContextType(val typeStr: String) {
    TOP_LEVEL("top-level"),
    RULE_LIKE("rule-like"),
    FUNCTION("function"),
}
