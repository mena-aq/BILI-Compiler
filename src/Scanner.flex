%%

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
%}

/* Whitespace */
Whitespace     = [ \t\r\n]+

/* Comments */
SingleComment  = "##" [^\n]*
MultiComment   = "#*" ([^*] | "*"+ [^*#])* "*"+ "#"

/* Identifiers */
Identifier     = [A-Z][a-zA-Z0-9_]{0,30}

/* Literals */
IntegerLiteral = [+-]?[0-9]+
FloatLiteral   = [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
StringLiteral  = \"([^\"\\\n]|\\[\\\"ntr])*\"
CharLiteral    = \'([^\'\\\n]|\\[\\\'ntr])\'

%%

{MultiComment}      { /* skip or return COMMENT if needed */ }
{SingleComment}     { /* skip or return COMMENT if needed */ }

/* Keywords */
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

/* Boolean */
"true" | "false"    { return createToken(TokenType.BOOLEAN); }

/* Identifier */
{Identifier}        { return createToken(TokenType.IDENTIFIER); }

/* Literals */
{FloatLiteral}      { return createToken(TokenType.FLOAT); }
{IntegerLiteral}    { return createToken(TokenType.INTEGER); }
{StringLiteral}     { return createToken(TokenType.STRING); }
{CharLiteral}       { return createToken(TokenType.CHARACTER); }

/* Multi-character operators */
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

/* Single-character operators */
"+"                 { return createToken(TokenType.ADD); }
"-"                 { return createToken(TokenType.SUB); }
"*"                 { return createToken(TokenType.MUL); }
"/"                 { return createToken(TokenType.DIV); }
"%"                 { return createToken(TokenType.MOD); }
"<"                 { return createToken(TokenType.LESS_THAN); }
">"                 { return createToken(TokenType.GREATER_THAN); }
"="                 { return createToken(TokenType.ASSIGN); }
"!"                 { return createToken(TokenType.LOGICAL_NOT); }

/* Punctuators */
"("                 { return createToken(TokenType.LEFT_PAREN); }
")"                 { return createToken(TokenType.RIGHT_PAREN); }
"{"                 { return createToken(TokenType.LEFT_BRACE); }
"}"                 { return createToken(TokenType.RIGHT_BRACE); }
"["                 { return createToken(TokenType.LEFT_BRACKET); }
"]"                 { return createToken(TokenType.RIGHT_BRACKET); }
","                 { return createToken(TokenType.COMMA); }
";"                 { return createToken(TokenType.SEMICOLON); }
":"                 { return createToken(TokenType.COLON); }

/* Whitespace */
{Whitespace}        { /* skip */ }
/* --- Specific Error Rules --- */

/* Unclosed Multi-line Comment */
"#*" ([^*] | "*"+ [^*#])* { return createToken(TokenType.ERROR); }

/* Unterminated String */
\"([^\"\\\n]|\\[\\\"ntr])* { return createToken(TokenType.ERROR); }

/* Malformed Float (Multiple decimals or trailing dots) */
[0-9]+\.[0-9]*\.[0-9\.]+   { return createToken(TokenType.ERROR); }

/* Invalid Identifier (Starting with lowercase) */
[a-z][a-zA-Z0-9_]* { return createToken(TokenType.ERROR); }

/* Identifier Too Long (More than 31 chars) */
/* Identifier Too Long (Matches 32 or more characters) */
[A-Z][a-zA-Z0-9_]{31} [a-zA-Z0-9_]+ { return createToken(TokenType.ERROR); }

/* Invalid Characters (Catch-all) */
[^]                        { return createToken(TokenType.ERROR); }

<<EOF>>                    { return createToken(TokenType.EOF); }
