package BottomUpParser.src;

import java.io.*;
import java.util.*;

/**
 * Input Reader for SLR Parser
 * Reads input strings from a text file where each line contains one string to parse.
 * Tokens are separated by spaces.
 */
public class InputReader {

    /**
     * Reads input file and returns a list of token lists, each representing one input string.
     *
     * @param filePath Path to the input text file
     * @return List of token lists, where each inner list is one line of input
     * @throws IOException If file cannot be read
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
                    continue;
                }

                // Split line into tokens (handles multiple spaces)
                String[] tokens = line.trim().split("\\s+");
                List<String> tokenList = Arrays.asList(tokens);

                // Optional: Validate that tokens are not empty
                if (tokenList.isEmpty()) {
                    continue;
                }

                // Add to inputs
                inputs.add(tokenList);
                //System.out.println("Line " + lineNumber + ": " + line);
            }
        }

        // Check if any valid inputs were found
        if (inputs.isEmpty()) {
            System.out.println("Warning: No valid input strings found in file.");
        } else {
            System.out.println("Successfully loaded " + inputs.size() + " input string(s) from: " + filePath + "\n");
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

