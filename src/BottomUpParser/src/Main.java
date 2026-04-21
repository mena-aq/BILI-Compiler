package BottomUpParser.src;

import java.io.IOException;
import java.util.*;

public class Main {

    public static void main(String[] args) {


        if (args.length < 1) {
            System.err.println("Usage: java BottomUpParser.src.Main <grammar-file-path> [input-file-path]");
            System.exit(1);
        }
        // Create a new grammar instance
        Grammar grammar = new Grammar();

        // Parse the grammar from the file provided as a command-line argument
        String grammarFile = args[0];
        grammar.parseFromFile(grammarFile);

        // Print parsed grammar
        System.out.println("Parsed Grammar");
        grammar.print();

        // Augment
        System.out.println("\nAugmented Grammar");
        grammar.augmentGrammar();
        grammar.print();
        grammar.printFirstFollow();

        // Build canonical collection (DFA states)
        List<Set<Items.LRItem>> canonicalCollection = Items.buildSLRCanonicalCollection(grammar);

        System.out.println("\nCanonical Collection (DFA States):");
        for (int i = 0; i < canonicalCollection.size(); i++) {
            System.out.println("\nI" + i + ":");

            // Separate items into actual items (dot > 0) and closure items (dot == 0)
            List<Items.LRItem> actualItems = new ArrayList<>();
            List<Items.LRItem> closureItems = new ArrayList<>();

            for (Items.LRItem item : canonicalCollection.get(i)) {
                if (item.dotPosition == 0) {
                    closureItems.add(item);
                } else {
                    actualItems.add(item);
                }
            }

            // Print actual items first, then closure items
            for (Items.LRItem item : actualItems) {
                System.out.println("  " + item);
            }
            for (Items.LRItem item : closureItems) {
                System.out.println("  " + item);
            }
        }

        // Build and print SLR parsing table
        ParsingTable parsingTable = new ParsingTable(grammar, canonicalCollection);
        System.out.println("\nSLR(1) Parsing Table:");
        boolean isSlrParseable = parsingTable.printParsingTable();

        // If grammar is SLR parseable, allow testing with input file
        if (isSlrParseable && args.length > 1) {
            String inputFile = args[1];

            try {
                // Read input file
                List<List<String>> inputStrings = InputReader.readInputFile(inputFile);

                if (!inputStrings.isEmpty()) {
                    SLRParser parser = new SLRParser(parsingTable, grammar);

                    // Parse each input string
                    for (int i = 0; i < inputStrings.size(); i++) {
                        List<String> tokens = inputStrings.get(i);
                        System.out.println("Input " + (i + 1) + ": " + String.join(" ", tokens));
                        boolean success = parser.parse(tokens);
                        parser.printParsingTrace();
                        System.out.println("Parsing result: " + (success ? "ACCEPTED" : "REJECTED") + "\n");

                        // prase tree
                        if (success) {
                            System.out.println("Parse Tree (graphviz dot):\n");
                            System.out.println(parser.getParseTree().toDot());
                        }
                    }

                }
            } catch (IOException e) {
                System.err.println("Error reading input file: " + e.getMessage());
            }
        }

        // Exit with status code based on parseability
        System.exit(isSlrParseable ? 0 : 1);
    }
}
