import java.io.*;

public class Main {
    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Usage: java Main <source.bili> <manual|flex|manual-flex>");
            System.exit(1);
        }

        String filePath = args[0];
        String mode = args[1].toLowerCase();
        boolean runManual = mode.equals("manual") || mode.equals("manual-flex");
        boolean runFlex = mode.equals("flex") || mode.equals("manual-flex");

        if (!filePath.endsWith(".bili")) {
            System.err.println("Error: Source file must have .bili extension.");
            System.exit(1);
        }
        if (!runManual && !runFlex) {
            System.err.println("Error: Mode must be one of: manual, flex, manual-flex");
            System.exit(1);
        }

        if (runManual) {
            System.out.println("\n--- Running Manual Scanner ---");
            ManualScanner.RunManualScanner(filePath);
        }

        if (runFlex) {
            System.out.println("\n--- Running JFlex Lexer ---");
            try (FileReader reader = new FileReader(filePath)) {
                Lexer lexer = new Lexer(reader);
                Token token;
                while ((token = lexer.yylex()) != null && token.getType() != TokenType.EOF) {
                    if (token.getType() == TokenType.ERROR) {
                        ErrorHandler.handleError(token);
                    } else {
                        System.out.println(token);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}