package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.SnakemakeIcons
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.stringLanguage.lang.highlighter.SmkSLSyntaxHighlighter
import javax.swing.Icon

class SmkColorSettingsPage : ColorSettingsPage {
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor(SnakemakeBundle.message("smk.color.keyword"), SnakemakeSyntaxHighlighterFactory.SMK_KEYWORD),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.definition"), SnakemakeSyntaxHighlighterFactory.SMK_FUNC_DEFINITION),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.subsection"), SnakemakeSyntaxHighlighterFactory.SMK_DECORATOR),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.run"), SnakemakeSyntaxHighlighterFactory.SMK_PREDEFINED_DEFINITION),

        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.text"), SnakemakeSyntaxHighlighterFactory.SMK_TEXT),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.content"), SmkSLSyntaxHighlighter.STRING_CONTENT),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.braces"), SmkSLSyntaxHighlighter.BRACES),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.comma"), SmkSLSyntaxHighlighter.COMMA),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.format"), SmkSLSyntaxHighlighter.FORMAT_SPECIFIER),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.key"), SmkSLSyntaxHighlighter.ACCESS_KEY),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.reference"), SmkSLSyntaxHighlighter.IDENTIFIER),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.wildcard"), SmkSLSyntaxHighlighter.HIGHLIGHTING_WILDCARDS_KEY)
    )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = SnakemakeBundle.message("snakemake.settings.name")

    override fun getIcon(): Icon = SnakemakeIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter {
        val lang = SnakemakeLanguageDialect.baseLanguage ?: PythonLanguage.getInstance()
        val factory = SyntaxHighlighterFactory.LANGUAGE_FACTORY.forLanguage(lang)
        if (factory is SnakemakeSyntaxHighlighterFactory) {
            return factory.getSyntaxHighlighterForLanguageLevel(LanguageLevel.getLatest())
        }
        return factory.getSyntaxHighlighter(null, null)
    }

    override fun getDemoText(): String =
        """
            <keyword>rule</keyword> <identifiers>NAME</identifiers>:
                <sectionName>input</sectionName>: 
                    <text>"file_1.txt"</text>
                <sectionName>output</sectionName>: 
                    <text>"file_2.txt"</text>
                <sectionName>shell</sectionName>: 
                    <text>"</text><injectedText>touch </injectedText><braces>{</braces><reference>output</reference><braces>}</braces><text>"</text>
            
            number = 0.451 # Python elements are configured in Python color settings
            
            <keyword>use</keyword> <keyword>rule</keyword> NAME <keyword>as</keyword> <identifiers>NAME_2</identifiers> <keyword>with</keyword>:
                <sectionName>input</sectionName>: 
                    <text>"{<wildcard>name</wildcard>}.bin"</text>
                <sectionName>output</sectionName>: 
                    <text>"{<wildcard>name</wildcard>}.bin"</text>
                <sectionName>message</sectionName>:  
                    <text>"</text><injectedText>Float number: </injectedText><braces>{</braces><reference>number</reference><formatSpecifier>:2f</formatSpecifier><braces>}</braces><text>"</text>
                <sectionName>threads</sectionName>: 
                    1
            
            <keyword>rule</keyword> <identifiers>NAME3</identifiers>:
             <sectionName>output</sectionName>: 
                    <text>"</text><injectedText>file_</injectedText><braces>{</braces><reference>number</reference><comma>,</comma> \d+<braces>}</braces><text>"</text>,
                    <accessKey>arg</accessKey> = <text>"file_1.txt"</text>
                <run>run</run>:
                    x = 2
        """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey> =
        mutableMapOf<String, TextAttributesKey>().also {
            it["keyword"] = SnakemakeSyntaxHighlighterFactory.SMK_KEYWORD
            it["identifiers"] = SnakemakeSyntaxHighlighterFactory.SMK_FUNC_DEFINITION
            it["sectionName"] = SnakemakeSyntaxHighlighterFactory.SMK_DECORATOR
            it["run"] = SnakemakeSyntaxHighlighterFactory.SMK_PREDEFINED_DEFINITION
            it["text"] = SnakemakeSyntaxHighlighterFactory.SMK_TEXT

            it["injectedText"] = SmkSLSyntaxHighlighter.STRING_CONTENT
            it["braces"] = SmkSLSyntaxHighlighter.BRACES
            it["comma"] = SmkSLSyntaxHighlighter.COMMA
            it["formatSpecifier"] = SmkSLSyntaxHighlighter.FORMAT_SPECIFIER
            it["accessKey"] = SmkSLSyntaxHighlighter.ACCESS_KEY
            it["reference"] = SmkSLSyntaxHighlighter.IDENTIFIER
            it["wildcard"] = SmkSLSyntaxHighlighter.HIGHLIGHTING_WILDCARDS_KEY
        }
}