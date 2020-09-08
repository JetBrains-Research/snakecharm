package com.jetbrains.snakecharm.stringLanguage.lang.parser;

import com.intellij.psi.tree.IElementType;
import com.intellij.lexer.FlexLexer;

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
WHITE_SPACE=\s+
IDENTIFIER={ID_LETTER}({ID_LETTER}|[:digit:])*
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

<WAITING_IDENTIFIER> {
    {IDENTIFIER}                   { yybegin(WAITING_AFTER_IDENTIFIER); return tokenTypes.getIDENTIFIER(); }
    {WHITE_SPACE}                  { return tokenTypes.getBAD_CHARACTER(); }
}

<WAITING_AFTER_IDENTIFIER> {
    \.                            { yybegin(WAITING_IDENTIFIER); return tokenTypes.getDOT(); }

    \[                            { yybegin(WAITING_ACCESS_KEY); return tokenTypes.getLBRACKET(); }

    {FORMAT_SPECIFIER}            { yybegin(WAITING_LANGUAGE_CLOSURE); return tokenTypes.getFORMAT_SPECIFIER();}

    //TODO report identifier/whitespace + fix parsing, to report errors more accurate
    // {IDENTIFIER}                  { return tokenTypes.getIDENTIFIER();}
    {IDENTIFIER}                   { return tokenTypes.getBAD_CHARACTER();}

    {WHITE_SPACE}                  { return tokenTypes.getBAD_CHARACTER(); }
}

<WAITING_ACCESS_KEY> {ACCESS_KEY} { yybegin(WAITING_ACCESS_CLOSURE); return tokenTypes.getACCESS_KEY(); }

<WAITING_REGEXP> {REGEXP}         { yybegin(WAITING_LANGUAGE_CLOSURE); return tokenTypes.getREGEXP(); }

<WAITING_ACCESS_KEY, WAITING_ACCESS_CLOSURE>
    \]                            { yybegin(WAITING_AFTER_IDENTIFIER); return tokenTypes.getRBRACKET(); }

    \}                            { yybegin(YYINITIAL); return tokenTypes.getRBRACE(); }

    \,                            { yybegin(WAITING_REGEXP); return tokenTypes.getCOMMA(); }

    [^]                           { yybegin(WAITING_IDENTIFIER); return tokenTypes.getBAD_CHARACTER(); }
