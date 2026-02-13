
public class Token {
    private TokenType type;
    private String lexeme;
    private int line;
    private int column;
    private String reason; // for error tokens

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
    }

    // token w reason for error tokens
    public Token(TokenType type, String lexeme, int line, int column, String reason) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
        this.reason = reason;
    }

    // Getters
    public TokenType getType() { return type; }
    public String getLexeme() { return lexeme; }
    public int getLine() { return line; }
    public int getColumn() { return column; }

    @Override
    public String toString() {
        // to print for parse output
        // e.g. <KEYWORD, "start", Line: 1, Col: 1>
        return String.format("<%s, \"%s\", Line: %d, Col: %d>",
                type, lexeme, line, column);
    }
}
