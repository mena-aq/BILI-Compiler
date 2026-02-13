public class ErrorHandler {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void report(TokenType type, int line, int col, String lexeme, String reason) {
        System.out.printf("%s[ERROR] Type: %s | Line: %d, Col: %d | Lexeme: '%s' | Reason: %s%s%n",
                ANSI_RED, type, line, col, lexeme, reason, ANSI_RESET);
    }

    // handle error with a reason
    public static void handleError(Token token, String reason) {
        report(token.getType(), token.getLine(), token.getColumn(), token.getLexeme(), reason);
    }

    // if no reason provided try to infer reason based on lexeme pattern
    public static void handleError(Token token) {
        String lexeme = token.getLexeme();
        String reason = "Invalid token";

        if (lexeme == null) {
            reason = "Unknown error";
        } else if (lexeme.startsWith("\"")) {
            reason = "Unterminated string literal";
        } else if (lexeme.startsWith("#*")) {
            reason = "Unclosed multi-line comment";
        } else if (lexeme.contains(".") && lexeme.matches(".*\\..*\\..*")) {
            reason = "Malformed numeric literal (multiple decimal points)";
        } else if (lexeme.length() > 31) {
            reason = "Identifier exceeds maximum length (31)";
        } else if (!lexeme.isEmpty() && Character.isLowerCase(lexeme.charAt(0))) {
            reason = "Invalid identifier (must start with Uppercase)";
        }

        report(token.getType(), token.getLine(), token.getColumn(), lexeme, reason);
    }
}