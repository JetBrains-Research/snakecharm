package com.jetbrains.snakecharm.lang.formatter

import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

class SmkCodeStyleSettingsPanel (currentSettings: CodeStyleSettings, settings:CodeStyleSettings)
  : TabbedLanguageCodeStylePanel(SnakemakeLanguageDialect, currentSettings, settings) {

  override fun initTabs(settings: CodeStyleSettings?) {
    addWrappingAndBracesTab(settings)
    addIndentOptionsTab(settings)
    addBlankLinesTab(settings)
    //addSpacesTab(settings)
  }
}
