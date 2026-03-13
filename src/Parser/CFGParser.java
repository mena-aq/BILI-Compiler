package Parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Parser.CFG;

// Parses a CFG from a text file.
public class CFGParser {

    public CFG parseCFG(String filePath) {
        CFG cfg = new CFG();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("->");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid production at line " + lineNumber + ": " + line);
                }

                String lhs = parts[0].trim();
                if (!isValidNonTerminal(lhs)) {
                    throw new IllegalArgumentException("Invalid non-terminal at line " + lineNumber + ": " + lhs);
                }

                String rhs = parts[1].trim();
                String[] alternatives = rhs.split("\\|");
                for (String alt : alternatives) {
                    alt = alt.trim();
                    List<String> symbols = new ArrayList<>();
                    if (alt.equals("@") || alt.equals("@")) {
                        symbols.add("@");
                    } else {
                        for (String token : alt.split("\\s+")) {
                            if (!token.isEmpty()) symbols.add(token);
                        }
                    }
                    cfg.addProduction(lhs, symbols);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read CFG file: " + filePath, e);
        }

        return cfg;
    }

    private boolean isValidNonTerminal(String symbol) {
        return symbol.length() > 1
                && Character.isUpperCase(symbol.charAt(0))
                && symbol.matches("[A-Za-z][A-Za-z0-9]*");
    }
}
