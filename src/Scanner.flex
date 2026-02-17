/* --- Section 1: User Code --- */
import java.util.*;

%%

/* --- Section 2: Options and Declarations --- */
%class Lexer
%public
%unicode
%line
%column
%type Token

%{
    private Token createToken(TokenType type) {
        return new Token(type, yytext(), yyline + 1, yycolumn + 1);
    }

    private Token createError(String reason) {
        return new Token(TokenType.ERROR, yytext(), yyline + 1, yycolumn + 1, reason);
    }
%}

/* Whitespace */
Whitespace     = [ \t\r\n]+

/* Comments */
SingleComment  = "##" [^\n]*
MultiComment   = "#*" ([^*] | "*"+ [^*#])* "*"+ "#"

/* Identifiers */
Identifier     = [A-Z][a-z0-9_]{0,30}

/* Literals & Unicode Macros */
IntegerLiteral    = [+-]?[0-9]+
FloatLiteral      = [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?

/* Unicode and Escape Sequences */
UnicodeEscape     = \\u[0-9a-fA-F]{4}
InvalidEscape     = \\[^ntr\\\"u]
IncompleteUnicode = \\u[0-9a-fA-F]{0,3}

/* String/Char elements */
StringElement     = [^\"\\\n] | \\[\\\"ntr] | {UnicodeEscape}
// For multiline, we allow newlines but not unescaped backslashes or invalid unicode
MultiStringElement = [^\"\\] | \"[^\"] | \"\"[^\"] | \\[\\\"ntr] | {UnicodeEscape} | \n
CharElement       = [^\'\\\n] | \\[\\\'ntr] | {UnicodeEscape}

/* Final Literal Definitions */
StringLiteral      = \" {StringElement}* \"
MultilineString    = \"\"\" {MultiStringElement}* \"\"\"
CharLiteral        = \' {CharElement} \'

/* --- Section 3: Lexical Rules --- */
%%

/* 1. Comments and Whitespace */
{Whitespace}        { /* skip */ }
{MultiComment}      { /* skip */ }
{SingleComment}     { /* skip */ }

/* 2. Multi-character operators (Rule 3.12 Priority) */
"**"                { return createToken(TokenType.EXP); }
"=="                { return createToken(TokenType.EQUAL); }
"!="                { return createToken(TokenType.NOT_EQUAL); }
"<="                { return createToken(TokenType.LESS_EQUAL); }
">="                { return createToken(TokenType.GREATER_EQUAL); }
"&&"                { return createToken(TokenType.LOGICAL_AND); }
"||"                { return createToken(TokenType.LOGICAL_OR); }
"++"                { return createToken(TokenType.INC); }
"--"                { return createToken(TokenType.DEC); }
"+="                { return createToken(TokenType.ADD_ASSIGN); }
"-="                { return createToken(TokenType.SUB_ASSIGN); }
"*="                { return createToken(TokenType.MUL_ASSIGN); }
"/="                { return createToken(TokenType.DIV_ASSIGN); }

/* 3. Keywords & Booleans */
"start"             { return createToken(TokenType.START); }
"finish"            { return createToken(TokenType.FINISH); }
"loop"              { return createToken(TokenType.LOOP); }
"condition"         { return createToken(TokenType.CONDITION); }
"declare"           { return createToken(TokenType.DECLARE); }
"output"            { return createToken(TokenType.OUTPUT); }
"input"             { return createToken(TokenType.INPUT); }
"function"          { return createToken(TokenType.FUNCTION); }
"return"            { return createToken(TokenType.RETURN); }
"break"             { return createToken(TokenType.BREAK); }
"continue"          { return createToken(TokenType.CONTINUE); }
"else"              { return createToken(TokenType.ELSE); }
"true" | "false"    { return createToken(TokenType.BOOLEAN); }

/* 4. Valid Literals & Identifiers */
{FloatLiteral}      { return createToken(TokenType.FLOAT); }
{IntegerLiteral}    { return createToken(TokenType.INTEGER); }
{MultilineString}   { return createToken(TokenType.STRING); }
{StringLiteral}     { return createToken(TokenType.STRING); }
{CharLiteral}       { return createToken(TokenType.CHARACTER); }
{Identifier}        { return createToken(TokenType.IDENTIFIER); }

/* 5. Single-character operators & Punctuators */
"+"                 { return createToken(TokenType.ADD); }
"-"                 { return createToken(TokenType.SUB); }
"*"                 { return createToken(TokenType.MUL); }
"/"                 { return createToken(TokenType.DIV); }
"%"                 { return createToken(TokenType.MOD); }
"<"                 { return createToken(TokenType.LESS_THAN); }
">"                 { return createToken(TokenType.GREATER_THAN); }
"="                 { return createToken(TokenType.ASSIGN); }
"!"                 { return createToken(TokenType.LOGICAL_NOT); }
"("                 { return createToken(TokenType.LEFT_PAREN); }
")"                 { return createToken(TokenType.RIGHT_PAREN); }
"{"                 { return createToken(TokenType.LEFT_BRACE); }
"}"                 { return createToken(TokenType.RIGHT_BRACE); }
"["                 { return createToken(TokenType.LEFT_BRACKET); }
"]"                 { return createToken(TokenType.RIGHT_BRACKET); }
","                 { return createToken(TokenType.COMMA); }
";"                 { return createToken(TokenType.SEMICOLON); }
":"                 { return createToken(TokenType.COLON); }

/* --- Section 4: Specific Error Rules --- */

/* ERROR: Multiline String with Invalid Escape */
\"\"\"({MultiStringElement} | {InvalidEscape} | {IncompleteUnicode})* \"\"\" {
    return createError("Invalid Unicode escape in string literal");
}

/* ERROR: Single line String with Invalid Escape */
\"({StringElement} | {InvalidEscape} | {IncompleteUnicode})* \" {
    return createError("Invalid Unicode escape in string literal");
}

/* ERROR: Unterminated Multiline String */
\"\"\"({MultiStringElement} | {InvalidEscape} | {IncompleteUnicode})* {
    return createError("Unterminated string literal");
}

/* ERROR: Unterminated Single line String */
\"({StringElement} | {InvalidEscape} | {IncompleteUnicode})* {
    return createError("Unterminated string literal");
}

/* Other Errors */
"#*" ([^*] | "*"+ [^*#])* { return createError("Unclosed multi-line comment"); }
[0-9]+\.[0-9]*\.[0-9\.]+   { return createError("Malformed float"); }
[a-z][a-zA-Z0-9_]* { return createError("Invalid identifier (must start with Uppercase)"); }
[A-Z][a-zA-Z0-9_]{31} [a-zA-Z0-9_]+ { return createError("Identifier exceeds max length (31)"); }

/* Catch-all */
[^]                        { return createError("Invalid character"); }
<<EOF>>                    { return createToken(TokenType.EOF); }