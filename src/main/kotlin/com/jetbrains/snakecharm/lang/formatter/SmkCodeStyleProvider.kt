package com.jetbrains.snakecharm.lang.formatter

import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.psi.codeStyle.CodeStyleConfigurable
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

class SmkCodeStyleProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage() = SnakemakeLanguageDialect

    @org.intellij.lang.annotations.Language("Snakemake")
    override fun getCodeSample(settingsType: SettingsType): String {
        val fileName = when (settingsType) {
            //SettingsType.INDENT_SETTINGS -> "indent_settings.md"
            SettingsType.BLANK_LINES_SETTINGS -> "blank_lines_settings.smk"
            //SettingsType.SPACING_SETTINGS -> "spacing_settings.md"
            else -> "default.smk"
        }
        return this::class.java.getResourceAsStream(fileName).bufferedReader().use {
            it.readText()
        }
    }

    override fun getConfigurableDisplayName() = SnakemakeBundle.message("snakemake.settings.name")

    override fun createConfigurable(
        baseSettings: CodeStyleSettings,
        modelSettings: CodeStyleSettings
    ): CodeStyleConfigurable = SmkCodeStyleConfigurable(baseSettings, modelSettings)

    override fun getIndentOptionsEditor() = SmartIndentOptionsEditor()

    override fun createCustomSettings(settings: CodeStyleSettings) = SmkCustomCodeStyleSettings(settings)

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        // E.g. see MarkdownCodeStyleSettingsProvider
        // todo: USE_CONTINUATION_INDENT_FOR_ARGUMENTS
        when (settingsType) {
            SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {
                consumer.showStandardOptions(
                    "RIGHT_MARGIN",
                    "WRAP_ON_TYPING",
                    "KEEP_LINE_BREAKS",
                    "KEEP_BLANK_LINES_IN_CODE",
                    // "WRAP_LONG_LINES", - // TODO: not working
                    "ALIGN_MULTILINE_PARAMETERS_IN_CALLS"
                )
                consumer.renameStandardOption(
                    "ALIGN_MULTILINE_PARAMETERS_IN_CALLS",
                    SnakemakeBundle.message("snakemake.settings.wrapping.and.braces.align.multiline.params.in.calls")
                )
            }
            SettingsType.BLANK_LINES_SETTINGS -> {
                consumer.showStandardOptions(
                    "BLANK_LINES_AROUND_METHOD",
                    "KEEP_BLANK_LINES_IN_DECLARATIONS",
                     // "KEEP_BLANK_LINES_IN_CODE" //seems better use Python settings for this option
                )
                consumer.renameStandardOption(
                    "BLANK_LINES_AROUND_METHOD",
                             SnakemakeBundle.message("snakemake.settings.blank.lines.around.rulelike")
                )
            }
            // SettingsType.SPACING_SETTINGS -> {}
            else -> {
            }
        }
    }
}