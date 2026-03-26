package Parser.src;

import java.io.*;
import java.util.*;

/**
 * Task 2.1: Input String Processing
 * Reads input strings from a text file where each line contains one string to parse.
 * Tokens are separated by spaces and should only contain terminals from the grammar.
 */
public class InputReader {

    /**
     * Reads input file and returns a list of token lists, each representing one input string.
     *
     * @param filePath Path to the input text file
     * @return List of token lists, where each inner list is one line of input
     * @throws IOException              If file cannot be read
     * @throws IllegalArgumentException If file contains invalid tokens (optional validation)
     */
    public static List<List<String>> readInputFile(String filePath) throws IOException {
        List<List<String>> inputs = new ArrayList<>();
        File file = new File(filePath);

        // Check if file exists
        if (!file.exists()) {
            throw new FileNotFoundException("Input file not found: " + filePath);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    System.out.println("Warning: Line " + lineNumber + " is empty. Skipping...");
                    continue;
                }

                // Split line into tokens (handles multiple spaces)
                String[] tokens = line.trim().split("\\s+");
                List<String> tokenList = Arrays.asList(tokens);

                // Optional: Validate that tokens are not empty
                if (tokenList.isEmpty()) {
                    System.out.println("Warning: Line " + lineNumber + " contains no valid tokens. Skipping...");
                    continue;
                }

                // Add to inputs
                inputs.add(tokenList);

                // Debug output (can be removed later)
                System.out.println("Line " + lineNumber + ": " + line);
                System.out.println("  Tokens: " + tokenList);
            }
        }

        // Check if any valid inputs were found
        if (inputs.isEmpty()) {
            System.out.println("Warning: No valid input strings found in file.");
        } else {
            System.out.println("\nSuccessfully loaded " + inputs.size() + " input string(s) from: " + filePath);
        }

        return inputs;
    }

    /**
     * Overloaded method that also validates tokens against a set of valid terminals.
     *
     * @param filePath       Path to the input text file
     * @param validTerminals Set of valid terminal symbols from the grammar
     * @return List of validated token lists
     * @throws IOException If file cannot be read
     */
    public static List<List<String>> readInputFile(String filePath, Set<String> validTerminals) throws IOException {
        List<List<String>> inputs = new ArrayList<>();
        File file = new File(filePath);

        if (!file.exists()) {
            throw new FileNotFoundException("Input file not found: " + filePath);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tokens = line.trim().split("\\s+");
                List<String> tokenList = new ArrayList<>();
                boolean validLine = true;

                // Validate each token
                for (String token : tokens) {
                    if (validTerminals.contains(token)) {
                        tokenList.add(token);
                    } else {
                        System.err.println("Error: Line " + lineNumber + " contains invalid token '" +
                                token + "'. Expected one of: " + validTerminals);
                        validLine = false;
                        break;
                    }
                }

                if (validLine && !tokenList.isEmpty()) {
                    inputs.add(tokenList);
                }
            }
        }

        return inputs;
    }

    /**
     * Utility method to print all loaded input strings in a formatted way.
     *
     * @param inputs List of token lists to print
     */
    public static void printInputs(List<List<String>> inputs) {
        System.out.println("\n=== Loaded Input Strings ===");
        if (inputs.isEmpty()) {
            System.out.println("No input strings to display.");
            return;
        }

        for (int i = 0; i < inputs.size(); i++) {
            List<String> input = inputs.get(i);
            System.out.printf("%2d: %s\n", i + 1, String.join(" ", input));
        }
        System.out.println("============================\n");
    }
}