package BottomUpParser.src;

import java.io.IOException;
import java.io.File;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        if (args.length < 1) {
            System.err.println("Usage: java BottomUpParser.src.Main <grammar-file-path> [input-file-path]");
            System.exit(1);
        }

        // Get base directory from grammar file path
        // Grammar files are in src/BottomUpParser/input/, so we need the grandparent directory
        File grammarFilePath = new File(args[0]);
        String baseDir = grammarFilePath.getParent();
        if (baseDir != null) {
            File parentDir = new File(baseDir).getParentFile();
            if (parentDir != null) {
                baseDir = parentDir.getAbsolutePath();
            }
        }
        if (baseDir == null) {
            baseDir = ".";
        }

        // Initialize output writer
        OutputWriter outputWriter = new OutputWriter(baseDir);
        outputWriter.openWriters();

        try {
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

            // Write augmented grammar to file
            outputWriter.writeAugmentedGrammar(grammar);

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

            // Write SLR items to file
            outputWriter.writeSLRItems(canonicalCollection);

            // Build and print SLR parsing table
            ParsingTable parsingTable = new ParsingTable(grammar, canonicalCollection);
            System.out.println("\nSLR(1) Parsing Table:");
            boolean isSlrParseable = parsingTable.printParsingTable();

            // Write parsing table to file
            outputWriter.writeSLRTable(parsingTable, canonicalCollection, grammar);

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

                            String inputString = String.join(" ", tokens);

                            // Write parsing trace to file
                            outputWriter.writeSLRTrace(inputString, parser.getParsingTrace(), success, parser.getParseTree());

                            // Write parse tree to separate file
                            if (success) {
                                outputWriter.writeParseTree(inputString, success, parser.getParseTree());
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
        } finally {
            // Always close writers
            outputWriter.closeWriters();
        }
    }
}
