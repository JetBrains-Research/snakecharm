package com.jetbrains.snakecharm.string_language.lang.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.snakecharm.string_language.SmkSLTokenTypes;

%%
%{
  private SmkSLTokenTypes tokenTypes = SmkSLTokenTypes.INSTANCE;
%}

%class _SmkSLLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

ID_LETTER=[:letter:]|\_
IDENTIFIER=(\s*)({ID_LETTER}({ID_LETTER}|[:digit:])*)*(\s*)
FORMAT_SPECIFIER=\:[^}]+
STRING_CONTENT=([^\{]|\{\{)+
REGEXP=([^{}]+ | \{\d+(,\d+)?\})*
ACCESS_KEY=[^\]]+

%state WAITING_IDENTIFIER
%state WAITING_REGEXP
%state WAITING_AFTER_IDENTIFIER
%state WAITING_ACCESS_KEY
%state WAITING_ACCESS_CLOSURE
%state WAITING_LANGUAGE_CLOSURE
%%

<YYINITIAL> {
    {STRING_CONTENT}              { yybegin(YYINITIAL); return tokenTypes.getSTRING_CONTENT(); }

    \{                            { yybegin(WAITING_IDENTIFIER); return tokenTypes.getLBRACE(); }
}

<WAITING_IDENTIFIER> {IDENTIFIER} { yybegin(WAITING_AFTER_IDENTIFIER); return tokenTypes.getIDENTIFIER(); }

<WAITING_AFTER_IDENTIFIER> {
    \.                            { yybegin(WAITING_IDENTIFIER); return tokenTypes.getDOT(); }

    \[                            { yybegin(WAITING_ACCESS_KEY); return tokenTypes.getLBRACKET(); }

    {FORMAT_SPECIFIER}            { yybegin(WAITING_LANGUAGE_CLOSURE); return tokenTypes.getFORMAT_SPECIFIER();}
}

<WAITING_ACCESS_KEY> {ACCESS_KEY} { yybegin(WAITING_ACCESS_CLOSURE); return tokenTypes.getACCESS_KEY(); }

<WAITING_REGEXP> {REGEXP}         { yybegin(WAITING_LANGUAGE_CLOSURE); return tokenTypes.getREGEXP(); }

<WAITING_ACCESS_KEY, WAITING_ACCESS_CLOSURE>
    \]                            { yybegin(WAITING_AFTER_IDENTIFIER); return tokenTypes.getRBRACKET(); }

    \}                            { yybegin(YYINITIAL); return tokenTypes.getRBRACE(); }

    \,                            { yybegin(WAITING_REGEXP); return tokenTypes.getCOMMA(); }

    [^]                           { yybegin(WAITING_IDENTIFIER); return tokenTypes.getUNEXPECTED_TOKEN(); }
