package com.jetbrains.snakecharm.lang.formatter

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.jetbrains.snakecharm.SnakemakeBundle

class SmkCodeStyleConfigurable(settings: CodeStyleSettings, originalSettings: CodeStyleSettings)
  : CodeStyleAbstractConfigurable(settings, originalSettings, SnakemakeBundle.message("snakemake.settings.name")) {

  override fun createPanel(settings: CodeStyleSettings): CodeStyleAbstractPanel =
    SmkCodeStyleSettingsPanel(currentSettings, settings)
}