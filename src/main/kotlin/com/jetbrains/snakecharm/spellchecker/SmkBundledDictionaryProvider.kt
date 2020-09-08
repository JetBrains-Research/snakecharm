package com.jetbrains.snakecharm.spellchecker

import com.intellij.spellchecker.BundledDictionaryProvider

class SmkBundledDictionaryProvider : BundledDictionaryProvider {
    override fun getBundledDictionaries() = arrayOf("snakemake.dic")
}