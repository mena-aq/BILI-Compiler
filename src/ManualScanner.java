import java.util.*;

public class ManualScanner {

    private String input; // source code to parse
    private int position; // curr char pos in input
    private int line; // curr line
    private int column; // curr col in line

    private List<Token> tokens; // list of generated tokens
    private Map<TokenType, Integer> tokenCounts; //counts per token type
    private SymbolTable symbolTable; //to store symbol table

    // handle errors by calling error handler
    private Token errorToken(String lexeme, int line, int col, String reason) {
        Token t = new Token(TokenType.ERROR, lexeme, line, col, reason);
        return t;
    }

    public ManualScanner(String input) {
        this.input = input;
        this.position = 0;
        this.line = 1;
        this.column = 1;
        this.tokens = new ArrayList<>();
        this.tokenCounts = new HashMap<>();
        this.symbolTable = new SymbolTable();
    }

    private String preprocess(String input) {
        StringBuilder out = new StringBuilder();

        boolean inString = false;
        boolean inChar = false;
        boolean inSingleLineComment = false; // ##
        boolean inMultiLineComment = false;  // #* ... *#
        boolean lineHasContent = false;      // empty lines b/w code blocks

        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);

            if (!inString && !inChar && !inSingleLineComment && !inMultiLineComment) {
                if (c == '#' && (i + 1) < input.length()) {
                    char n = input.charAt(i + 1);
                    if (n == '#') { // single-line comment start
                        inSingleLineComment = true;
                        out.append(c).append(n);
                        i += 2;
                        lineHasContent = true; // comment content counts as content
                        continue;
                    } else if (n == '*') { // multi-line comment start
                        inMultiLineComment = true;
                        out.append(c).append(n);
                        i += 2;
                        lineHasContent = true; // comment counts as content
                        continue;
                    }
                }
            }
            // no whietspace removal in comments
            if (inSingleLineComment) {
                out.append(c);
                if (c == '\n') {
                    inSingleLineComment = false;
                    lineHasContent = false; // new line starts empty
                }
                i++;
                continue;
            }
            if (inMultiLineComment) {
                out.append(c);
                if (c == '*' && (i + 1) < input.length() && input.charAt(i + 1) == '#') {
                    out.append('#');
                    i += 2;
                    inMultiLineComment = false;
                    continue;
                }
                if (c == '\n') {
                    lineHasContent = false; // new line within comment
                } else if (!Character.isWhitespace(c)) {
                    lineHasContent = true;
                }
                i++;
                continue;
            }

            // toggle whether in string/char literal
            if (!inChar && c == '"') {
                inString = !inString;
                out.append(c);
                lineHasContent = true;
                i++;
                continue;
            }
            if (!inString && c == '\'') {
                inChar = !inChar;
                out.append(c);
                lineHasContent = true;
                i++;
                continue;
            }
            // no whitespace removal in str and char literal
            if (inString || inChar) {
                if (c == '\\') {
                    out.append(c);
                    if (i + 1 < input.length()) {
                        out.append(input.charAt(i + 1));
                        i += 2;
                    } else {
                        i++;
                    }
                    lineHasContent = true;
                    continue;
                }
                out.append(c);
                if (inChar && c == '\'') inChar = false;
                if (inString && c == '"') inString = false;
                if (c == '\n') {
                    // newline inside literal remains; reset content tracker for next line
                    lineHasContent = false;
                } else if (!Character.isWhitespace(c)) {
                    lineHasContent = true;
                }
                i++;
                continue;
            }

            // else collapse spaces/tabs; drop empty lines
            if (c == ' ' || c == '\t' || c == '\r') {
                // collapse to a single space if we already have content on this line (skip over)
                if (lineHasContent) {
                    out.append(' ');
                }
                i++;
                while (i < input.length()) {
                    char d = input.charAt(i);
                    if (d == ' ' || d == '\t' || d == '\r') {
                        i++;
                    } else {
                        break;
                    }
                }
                continue;
            }

            //drop if current line has no content (i.e., empty line)
            if (c == '\n') {
                if (lineHasContent) {
                    out.append('\n');
                }
                // Start a new line with no content
                lineHasContent = false;
                i++;
                continue;
            }

            out.append(c);
            if (!Character.isWhitespace(c)) {
                lineHasContent = true;
            }
            i++;
        }
        return out.toString();
    }

    // scan the input char by char
    // i think we need to remove comments ??
    // does that mean just dont add to token list or is that for preprocess?
    public List<Token> scanTokens() {

        while (!endOfSource()) {
            int prevPosition = position; // guard against no-progress iterations

            Token token = scanToken();
            if (token != null && !(token.getType()==TokenType.WHITESPACE||token.getType() == TokenType.COMMENT||token.getType() == TokenType.EOF)) {
                tokens.add(token);
                updateTokenCount(token.getType());
                if (token.getType() == TokenType.IDENTIFIER) {
                    symbolTable.add(token.getLexeme(), token.getLine(), token.getColumn());
                }
            }

            // if scanToken didn't consume anything, force advance to avoid infinite loop
            if (position == prevPosition) {
                advance();
            }
        }

        // append EOF token at end of file
        //tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private Token scanToken() {

        int startLine = line;
        int startCol = column;
        char c = advance();

        // check in order of precedence:
        // 1. multi-line comments
        if (c == '#' && peek() == '*') {
            return scanMultiLineComment(startLine, startCol);
        }

        // 2. single-line comments
        if (c == '#' && peek() == '#') {
            return scanSingleLineComment(startLine, startCol);
        }

        // 3. multi-character operators
        // just peek ahead to confirm classification as multichar
        if (c == '*' && peek() == '*') {
            advance();
            return new Token(TokenType.EXP, "**", startLine, startCol);
        }
        if (c == '=' && peek() == '=') {
            advance();
            return new Token(TokenType.EQUAL, "==", startLine, startCol);
        }
        if (c == '!' && peek() == '=') {
            advance();
            return new Token(TokenType.NOT_EQUAL, "!=", startLine, startCol);
        }
        if (c == '<' && peek() == '=') {
            advance();
            return new Token(TokenType.LESS_EQUAL, "<=", startLine, startCol);
        }
        if (c == '>' && peek() == '=') {
            advance();
            return new Token(TokenType.GREATER_EQUAL, ">=", startLine, startCol);
        }
        if (c == '&' && peek() == '&') {
            advance();
            return new Token(TokenType.LOGICAL_AND, "&&", startLine, startCol);
        }
        if (c == '|' && peek() == '|') {
            advance();
            return new Token(TokenType.LOGICAL_OR, "||", startLine, startCol);
        }
        if (c == '+' && peek() == '+') {
            advance();
            return new Token(TokenType.INC, "++", startLine, startCol);
        }
        if (c == '-' && peek() == '-') {
            advance();
            return new Token(TokenType.DEC, "--", startLine, startCol);
        }
        if (c == '+' && peek() == '=') {
            advance();
            return new Token(TokenType.ADD_ASSIGN, "+=", startLine, startCol);
        }
        if (c == '-' && peek() == '=') {
            advance();
            return new Token(TokenType.SUB_ASSIGN, "-=", startLine, startCol);
        }
        if (c == '*' && peek() == '=') {
            advance();
            return new Token(TokenType.MUL_ASSIGN, "*=", startLine, startCol);
        }
        if (c == '/' && peek() == '=') {
            advance();
            return new Token(TokenType.DIV_ASSIGN, "/=", startLine, startCol);
        }

        // 4. keywords or identifiers (any alpha start)
        if (isAlpha(c)) {
            return scanWordOrIdentifier(c, startLine, startCol);
        }

        // 7. floating-point literals 8. integer literals
        if (isDigit(c) || c == '+' || c == '-') {
            return scanNumber(c, startLine, startCol);
        }

        // 9. string literals
        if (c == '"') {
            return scanString(startLine, startCol);
        }

        // 10. character literals
        if (c == '\'' ) {
            return scanCharacter(startLine, startCol);
        }

        // 11. single-character operators
        switch (c) {
            case '+': return new Token(TokenType.ADD, "+", startLine, startCol);
            case '-': return new Token(TokenType.SUB, "-", startLine, startCol);
            case '*': return new Token(TokenType.MUL, "*", startLine, startCol);
            case '/': return new Token(TokenType.DIV, "/", startLine, startCol);
            case '%': return new Token(TokenType.MOD, "%", startLine, startCol);
            case '<': return new Token(TokenType.LESS_THAN, "<", startLine, startCol);
            case '>': return new Token(TokenType.GREATER_THAN, ">", startLine, startCol);
            case '!': return new Token(TokenType.LOGICAL_NOT, "!", startLine, startCol);
            case '=': return new Token(TokenType.ASSIGN, "=", startLine, startCol);
        }
        // 12. punctuators
        switch (c) {
            case '(': return new Token(TokenType.LEFT_PAREN, "(", startLine, startCol);
            case ')': return new Token(TokenType.RIGHT_PAREN, ")", startLine, startCol);
            case '{': return new Token(TokenType.LEFT_BRACE, "{", startLine, startCol);
            case '}': return new Token(TokenType.RIGHT_BRACE, "}", startLine, startCol);
            case '[': return new Token(TokenType.LEFT_BRACKET, "[", startLine, startCol);
            case ']': return new Token(TokenType.RIGHT_BRACKET, "]", startLine, startCol);
            case ',': return new Token(TokenType.COMMA, ",", startLine, startCol);
            case ';': return new Token(TokenType.SEMICOLON, ";", startLine, startCol);
            case ':': return new Token(TokenType.COLON, ":", startLine, startCol);
        }

        // 12. whitespace
        if (Character.isWhitespace(c)) {
            return scanWhitespace(c, startLine, startCol);
        }

        // if none match it must be an invalid character
        return errorToken(String.valueOf(c), startLine, startCol, "Invalid character: '" + c + "'");
    }


    // ------------------------------------ helpers ------------------------------------

    // to check if end of input reached
    private boolean endOfSource() {
        return position >= input.length();
    }

    // to move to next char (update col and line) & return current char
    private char advance() {
        if (endOfSource()) return '\0';
        char c = input.charAt(position++);
        // if its a newline reset column=1 & line++
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    // current char without consuming it
    private char peek() {
        if (endOfSource()) return '\0';
        return input.charAt(position);
    }

    // lookahead k=1 for peeking without consuming
    private char peekNext() {
        if (position + 1 >= input.length()) return '\0';
        return input.charAt(position + 1);
    }

    // check alphanumeric etc
    private boolean isAlpha(char c) {
        return Character.isLetter(c);
    }
    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
    private boolean isAlphaNumericOrUnderscore(char c) {
        return isAlphaNumeric(c) || c == '_' || c == ' ';
    }

    // count
    private void updateTokenCount(TokenType type) {
        tokenCounts.put(type, tokenCounts.getOrDefault(type, 0) + 1);
    }

    // ------------------------------------ DFAs ------------------------------------

    private Token scanMultiLineComment(int startLine, int startCol) {
        StringBuilder lexeme = new StringBuilder("#*");
        advance(); //consume full #*

        while (!endOfSource()) {
            char c = advance();
            lexeme.append(c);

            if (c == '*') {
                // * loop
                while (peek() == '*' && !endOfSource()) {
                    c = advance();
                    lexeme.append(c);
                }
                // if after *-># reach final state, return comment token
                if (peek() == '#') {
                    advance();
                    lexeme.append('#');
                    return new Token(TokenType.COMMENT, lexeme.toString(), startLine, startCol);
                }
            }
        }
        // unclosed multiline comment
        return errorToken(lexeme.toString(), startLine, startCol, "Unclosed multi-line comment (missing *#)");
    }

    private Token scanSingleLineComment(int startLine, int startCol) {
        StringBuilder lexeme = new StringBuilder("##");
        advance(); // consume full ##

        // stay in comment dfa until \n reached
        while (peek() != '\n' && !endOfSource()) {
            lexeme.append(advance());
        }

        return new Token(TokenType.COMMENT, lexeme.toString(), startLine, startCol);
    }

    // KEYWORD or IDENTIFIER
    private Token scanWordOrIdentifier(char firstChar, int startLine, int startCol) {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append(firstChar);

        // consume all valid identifier/keyword characters (letters, digits, underscores)
        while (!endOfSource() && (isAlpha(peek()) || isDigit(peek()) || peek() == '_')) {
            lexeme.append(advance());
        }
        String text = lexeme.toString();

        // check keywords first
        switch (text) {
            case "start": return new Token(TokenType.START, text, startLine, startCol);
            case "finish": return new Token(TokenType.FINISH, text, startLine, startCol);
            case "loop": return new Token(TokenType.LOOP, text, startLine, startCol);
            case "condition": return new Token(TokenType.CONDITION, text, startLine, startCol);
            case "declare": return new Token(TokenType.DECLARE, text, startLine, startCol);
            case "output": return new Token(TokenType.OUTPUT, text, startLine, startCol);
            case "input": return new Token(TokenType.INPUT, text, startLine, startCol);
            case "function": return new Token(TokenType.FUNCTION, text, startLine, startCol);
            case "return": return new Token(TokenType.RETURN, text, startLine, startCol);
            case "break": return new Token(TokenType.BREAK, text, startLine, startCol);
            case "continue": return new Token(TokenType.CONTINUE, text, startLine, startCol);
            case "else": return new Token(TokenType.ELSE, text, startLine, startCol);
            case "true": case "false": return new Token(TokenType.BOOLEAN, text, startLine, startCol);
        }
        // else check identifier
        if (Character.isUpperCase(text.charAt(0))) {
            if (text.length() > 31) {
                return errorToken(text, startLine, startCol, "Identifier exceeds max length (31)");
            }
            return new Token(TokenType.IDENTIFIER, text, startLine, startCol);
        }
        // if not a keyword or valid identifier, it's an invalid identifier
        return errorToken(text, startLine, startCol, "Invalid identifier (must start with Uppercase or be a keyword)");
    }


    //  INTEGER & FLOAT
    private Token scanNumber(char firstChar, int startLine, int startCol) {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append(firstChar);

        if (firstChar == '+' || firstChar == '-') {
            if (!isDigit(peek())) {
                // not a signed number, maybe part of single char operator
                // no need to check multichar operator here bc its alr checked higher precedence in scanToken()
                position--; // backtrack
                column--;
                return scanSingleCharOperator(firstChar, startLine, startCol);
            }
        }

        while (isDigit(peek())) {
            lexeme.append(advance());
        }

        // check if float
        if (peek() == '.') {
            lexeme.append(advance());

            // if another '.' immediately appears its malformed
            if (peek() == '.') {
                lexeme.append(advance());
                return errorToken(lexeme.toString(), startLine, startCol, "Malformed float: multiple decimal points");
            }
            if (!isDigit(peek())) {
                // invalid float w no digits after .
                return errorToken(lexeme.toString(), startLine, startCol, "Malformed float: missing digits after '.'");
            }

            int decimalPlaces = 0;
            while (isDigit(peek()) && decimalPlaces < 6) {
                lexeme.append(advance());
                decimalPlaces++;
            }

            if (decimalPlaces > 6) {
                return errorToken(lexeme.toString(), startLine, startCol, "Malformed float: more than 6 decimals");
            }

            // if another '.' appears its malformed
            if (peek() == '.') {
                // consume the rest of the digits
                do {
                    lexeme.append(advance());
                } while (isDigit(peek()) || peek() == '.');
                return errorToken(lexeme.toString(), startLine, startCol, "Malformed float: multiple decimal points");
            }

            // check for exponent part
            if (peek() == 'e' || peek() == 'E') {
                lexeme.append(advance());
                if (peek() == '+' || peek() == '-') {
                    lexeme.append(advance());
                }
                if (!isDigit(peek())) {
                    return errorToken(lexeme.toString(), startLine, startCol, "Malformed float: invalid exponent");
                }
                while (isDigit(peek())) {
                    lexeme.append(advance());
                }
            }

            return new Token(TokenType.FLOAT, lexeme.toString(), startLine, startCol);
        }

        return new Token(TokenType.INTEGER, lexeme.toString(), startLine, startCol);
    }

    private Token scanString(int startLine, int startCol) {
        StringBuilder lexeme = new StringBuilder("\"");

        while (peek() != '"' && !endOfSource() && peek() != '\n') {
            char c = peek();
            // if \ encountered, enter escape sequence branch
            if (c == '\\') {
                advance();
                lexeme.append('\\');
                char escaped = peek();
                if (escaped == '"' || escaped == '\\' || escaped == 'n' ||
                    escaped == 't' || escaped == 'r') {
                    lexeme.append(advance());
                } else {
                    return errorToken(lexeme.toString(), startLine, startCol, "Invalid escape sequence in string literal");
                }
            } else {
                lexeme.append(advance());
            }
        }
        // unclosed string literal (across lines or at end of file)
        if (endOfSource() || peek() == '\n') {
            return errorToken(lexeme.toString(), startLine, startCol, "Unterminated string literal");
        }

        advance();
        lexeme.append('"');

        return new Token(TokenType.STRING, lexeme.toString(), startLine, startCol);
    }

    private Token scanCharacter(int startLine, int startCol) {
        StringBuilder lexeme = new StringBuilder("'");

        // unclosed char literal
        if (endOfSource() || peek() == '\n') {
            return errorToken(lexeme.toString(), startLine, startCol, "Unterminated character literal");
        }

        char c = peek();
        // enter \ branch and check for valid escape sequence
        if (c == '\\') {
            advance();
            lexeme.append('\\');
            char escaped = peek();
            if (escaped == '\'' || escaped == '\\' || escaped == 'n' ||
                escaped == 't' || escaped == 'r') {
                lexeme.append(advance());
            } else {
                return errorToken(lexeme.toString(), startLine, startCol, "Invalid escape in character literal");
            }
        } else if (c != '\'' ) {
            lexeme.append(advance());
        } else {
            // empty char literal '' is invalid
            return errorToken(lexeme.toString(), startLine, startCol, "Empty character literal");
        }
        if (peek() != '\'' ) {
            return errorToken(lexeme.toString(), startLine, startCol, "Unterminated character literal");
        }

        advance();
        lexeme.append('\'');

        return new Token(TokenType.CHARACTER, lexeme.toString(), startLine, startCol);
    }

    private Token scanWhitespace(char firstChar, int startLine, int startCol) {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append(firstChar);
        while (Character.isWhitespace(peek())) {
            lexeme.append(advance());
        }
        return new Token(TokenType.WHITESPACE, lexeme.toString(), startLine, startCol);
    }

    private Token scanSingleCharOperator(char c, int startLine, int startCol) {
        // fallback for when signed number is actually an operator
        switch (c) {
            case '+': return new Token(TokenType.ADD, "+", startLine, startCol);
            case '-': return new Token(TokenType.SUB, "-", startLine, startCol);
            default: return new Token(TokenType.ERROR, String.valueOf(c), startLine, startCol);
        }
    }

    public void printSymbolTable() {
        System.out.println("\nSymbol Table:");
        for (SymbolTable.Symbol symbol : symbolTable.entries()) {
            System.out.printf("Identifier: %s, First Occurrence: Line %d, Column %d, Frequency: %d%n",
                    symbol.name, symbol.firstLine, symbol.firstColumn, symbol.frequency);
        }
    }

    // Total tokens, count per token type, lines processed, comments removed
    public void printStatistics() {
        System.out.println("\nStatistics: ");

        System.out.println("Total tokens: " + tokens.size());

        System.out.println("\nToken counts:");
        for (Map.Entry<TokenType, Integer> entry : tokenCounts.entrySet()) {
            System.out.printf("%s: %d%n", entry.getKey(), entry.getValue());
        }

        System.out.println("Lines processed: " + line);
        int commentCount = tokenCounts.getOrDefault(TokenType.COMMENT, 0);
        System.out.println("Comments removed: " + commentCount);

        printSymbolTable();

    }

    public static void RunManualScanner(String sourceFile) {

        // Require a .bili source file via command-line; exit if not provided
        if (sourceFile == null || sourceFile.isEmpty()) {
            System.err.println("Error: No input file provided. Please specify a .bili source file.");
            return;
        }
        String path = sourceFile.trim();
        if (!path.endsWith(".bili")) {
            System.err.println("Error: input file must have .bili extension");
            return;
        }

        String source;
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(path);
            source = java.nio.file.Files.readString(p);
        } catch (java.io.IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
            return;
        }

        ManualScanner scanner = new ManualScanner(source);

        // preprocess
        String preprocessed = scanner.preprocess(source);
        scanner.input = preprocessed; // update input with preprocessed version

        //System.out.println("\n=== PREPROCESSED INPUT ===");
        //System.out.println("'" + scanner.input + "'");

        // scan tokens
        List<Token> tokens = scanner.scanTokens();
        for (Token token : tokens) {
            if (token.getType() == TokenType.ERROR) {
                ErrorHandler.handleError(token);
                continue;
            }
            System.out.println(token);
        }

        //scanner.printStatistics();
    }
}
