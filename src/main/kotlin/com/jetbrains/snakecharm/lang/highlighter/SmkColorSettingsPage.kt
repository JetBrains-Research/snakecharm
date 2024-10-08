package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.codeHighlighting.RainbowHighlighter
import com.intellij.lang.Language
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.RainbowColorSettingsPage
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.highlighting.PyRainbowVisitor
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.SnakemakeIcons
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.stringLanguage.lang.highlighter.SmkSLSyntaxHighlighter
import javax.swing.Icon

class SmkColorSettingsPage : RainbowColorSettingsPage {
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.keyword"),
            SnakemakeSyntaxHighlighterAttributes.SMK_KEYWORD
        ),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.definition"),
            SnakemakeSyntaxHighlighterAttributes.SMK_FUNC_DEFINITION
        ),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.subsection"),
            SnakemakeSyntaxHighlighterAttributes.SMK_DECORATOR
        ),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.run"),
            SnakemakeSyntaxHighlighterAttributes.SMK_PREDEFINED_DEFINITION
        ),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.keyword.arg"),
            SnakemakeSyntaxHighlighterAttributes.SMK_KEYWORD_ARGUMENT
        ),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.string.text"),
            SnakemakeSyntaxHighlighterAttributes.SMK_TEXT
        ),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.string.tqs"),
            SnakemakeSyntaxHighlighterAttributes.SMK_TRIPLE_QUOTED_STRING
        ),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.string.SL.content"),
            SmkSLSyntaxHighlighter.STRING_CONTENT
        ),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.braces"), SmkSLSyntaxHighlighter.BRACES),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.comma"), SmkSLSyntaxHighlighter.COMMA),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.string.SL.format"),
            SmkSLSyntaxHighlighter.FORMAT_SPECIFIER
        ),
        AttributesDescriptor(SnakemakeBundle.message("smk.color.string.SL.key"), SmkSLSyntaxHighlighter.ACCESS_KEY),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.string.SL.reference"),
            SmkSLSyntaxHighlighter.IDENTIFIER
        ),
        AttributesDescriptor(
            SnakemakeBundle.message("smk.color.string.SL.wildcard"),
            SmkSLSyntaxHighlighter.HIGHLIGHTING_WILDCARDS_KEY
        )
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
        <keyword>configfile</keyword>: <text>"config/config.yaml"</text>
        <keyword>localrules</keyword>: NAME
        """.trimIndent() +
                "\n<keyword>rule</keyword> <funcDef>NAME</funcDef>:\n" +
                "    <TQS>\"\"\"\n" +
                "    Syntax Highlighting Demo" +
                RainbowHighlighter.generatePaletteExample("\n    ") +
                "\n    \"\"\"</TQS>\n" +
                """    
            <sectionName>input</sectionName>: 
                <text>"</text><injectedText>file_</injectedText><braces>{</braces><wildcard>number</wildcard><braces>}</braces><injectedText>.txt</injectedText><text>"</text>,
                <keywordArg>arg</keywordArg> = <text>"file_1.txt"</text>
            <sectionName>output</sectionName>: 
                <text>"</text><injectedText>file_</injectedText><braces>{</braces><wildcard>number</wildcard><braces>}</braces><injectedText>.txt</injectedText><text>"</text>
            <run>run</run>:
                <localVar>x</localVar> = 2
                shell(<text>"</text><injectedText>touch </injectedText><braces>{</braces><reference>output</reference><braces>}</braces><text>"</text>)
        
        <localVar>number</localVar> = 0.451 # Python elements are configured in Python color settings
        
        <keyword>use</keyword> <keyword>rule</keyword> * <keyword>from</keyword> M <keyword>exclude</keyword> NAME
        
        <keyword>use</keyword> <keyword>rule</keyword> NAME <keyword>as</keyword> <funcDef>NAME_2</funcDef> <keyword>with</keyword>:
            <sectionName>message</sectionName>:  
                <text>"</text><injectedText>Float number: </injectedText><braces>{</braces><reference>number</reference><formatSpecifier>:2f</formatSpecifier><braces>}</braces><text>"</text>
        """.trimIndent()

    override fun getAdditionalHighlightingTagToDescriptorMap(): MutableMap<String, TextAttributesKey> =
        mutableMapOf<String, TextAttributesKey>().also {
            it["keyword"] = SnakemakeSyntaxHighlighterAttributes.SMK_KEYWORD
            it["funcDef"] = SnakemakeSyntaxHighlighterAttributes.SMK_FUNC_DEFINITION

            it["sectionName"] = SnakemakeSyntaxHighlighterAttributes.SMK_DECORATOR
            it["run"] = SnakemakeSyntaxHighlighterAttributes.SMK_PREDEFINED_DEFINITION
            it["text"] = SnakemakeSyntaxHighlighterAttributes.SMK_TEXT
            it["TQS"] = SnakemakeSyntaxHighlighterAttributes.SMK_TRIPLE_QUOTED_STRING
            it["keywordArg"] = SnakemakeSyntaxHighlighterAttributes.SMK_KEYWORD_ARGUMENT

            it["injectedText"] = SmkSLSyntaxHighlighter.STRING_CONTENT
            it["braces"] = SmkSLSyntaxHighlighter.BRACES
            it["comma"] = SmkSLSyntaxHighlighter.COMMA
            it["formatSpecifier"] = SmkSLSyntaxHighlighter.FORMAT_SPECIFIER
            it["accessKey"] = SmkSLSyntaxHighlighter.ACCESS_KEY
            it["reference"] = SmkSLSyntaxHighlighter.IDENTIFIER
            it["wildcard"] = SmkSLSyntaxHighlighter.HIGHLIGHTING_WILDCARDS_KEY

            it["localVar"] = DefaultLanguageHighlighterColors.LOCAL_VARIABLE
            it.putAll(RainbowHighlighter.createRainbowHLM())
        }

    override fun isRainbowType(type: TextAttributesKey?): Boolean =
        PyRainbowVisitor.Holder.HIGHLIGHTING_KEYS.contains(type)

    override fun getLanguage(): Language = SnakemakeLanguageDialect
}