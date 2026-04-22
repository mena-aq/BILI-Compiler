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

            // ==================== PART 1: SLR(1) ====================
            System.out.println("\n" + "=".repeat(80));
            System.out.println("PART 1: SLR(1) PARSER IMPLEMENTATION");
            System.out.println("=".repeat(80));

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
            ParsingTable slrParsingTable = new ParsingTable(grammar, canonicalCollection);
            System.out.println("\nSLR(1) Parsing Table:");
            boolean isSlrParseable = slrParsingTable.printParsingTable();

            // Write parsing table to file
            outputWriter.writeSLRTable(slrParsingTable, canonicalCollection, grammar);

            // If grammar is SLR parseable, allow testing with input file
            if (isSlrParseable && args.length > 1) {
                String inputFile = args[1];

                try {
                    // Read input file
                    List<List<String>> inputStrings = InputReader.readInputFile(inputFile);

                    if (!inputStrings.isEmpty()) {
                        SLRParser slrParser = new SLRParser(slrParsingTable, grammar);

                        // Parse each input string with SLR(1)
                        for (int i = 0; i < inputStrings.size(); i++) {
                            List<String> tokens = inputStrings.get(i);
                            System.out.println("\nInput " + (i + 1) + ": " + String.join(" ", tokens));
                            System.out.println("SLR(1) Parsing:");
                            boolean success = slrParser.parse(tokens);
                            slrParser.printParsingTrace();
                            System.out.println("SLR(1) Parsing result: " + (success ? "ACCEPTED" : "REJECTED") + "\n");

                            String inputString = String.join(" ", tokens);

                            // Write parsing trace to file
                            outputWriter.writeSLRTrace(inputString, slrParser.getParsingTrace(), success, slrParser.getParseTree());

                            // Write parse tree to separate file
                            if (success) {
                                outputWriter.writeParseTree(inputString, success, slrParser.getParseTree());
                                System.out.println("Parse Tree (graphviz dot):\n");
                                System.out.println(slrParser.getParseTree().toDot());
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading input file: " + e.getMessage());
                }
            }

            // ==================== PART 2: LR(1) ====================
            System.out.println("\n" + "=".repeat(80));
            System.out.println("PART 2: LR(1) PARSER IMPLEMENTATION");
            System.out.println("=".repeat(80));

            // Build LR(1) canonical collection
            System.out.println("\n--- LR(1) Canonical Collection (with lookaheads) ---");
            List<Set<Items.LRItem>> lr1CanonicalCollection = Items.buildLR1CanonicalCollection(grammar);

            // Print LR(1) item sets
            Items.printLR1ItemSets(lr1CanonicalCollection);

            // Write LR(1) items to file
            outputWriter.writeLR1Items(lr1CanonicalCollection);

            // Build LR(1) parsing table (useLookaheads = true)
            System.out.println("\n--- LR(1) Parsing Table ---");
            ParsingTable lr1ParsingTable = new ParsingTable(grammar, lr1CanonicalCollection, true);
            boolean isLr1Parseable = lr1ParsingTable.printParsingTable();

            // Write LR(1) table to file
            outputWriter.writeLR1Table(lr1ParsingTable, lr1CanonicalCollection, grammar);

            // Compare number of states
            System.out.println("\n--- Comparison ---");
            System.out.println("SLR(1) states: " + canonicalCollection.size());
            System.out.println("LR(1) states: " + lr1CanonicalCollection.size());
            System.out.println("LR(1) has " + (lr1CanonicalCollection.size() - canonicalCollection.size()) + " more states than SLR(1)");

            // Parse the same input strings with LR(1) parser
            if (isLr1Parseable && args.length > 1) {
                String inputFile = args[1];

                try {
                    List<List<String>> inputStrings = InputReader.readInputFile(inputFile);

                    if (!inputStrings.isEmpty()) {
                        SLRParser lr1Parser = new SLRParser(lr1ParsingTable, grammar);

                        System.out.println("\n--- LR(1) Parsing Results ---");

                        // Parse each input string with LR(1)
                        for (int i = 0; i < inputStrings.size(); i++) {
                            List<String> tokens = inputStrings.get(i);
                            System.out.println("\nInput " + (i + 1) + ": " + String.join(" ", tokens));
                            System.out.println("LR(1) Parsing:");
                            boolean success = lr1Parser.parse(tokens);
                            lr1Parser.printParsingTrace();
                            System.out.println("LR(1) Parsing result: " + (success ? "ACCEPTED" : "REJECTED") + "\n");

                            String inputString = String.join(" ", tokens);

                            // Write LR(1) trace to file
                            outputWriter.writeLR1Trace(inputString, lr1Parser.getParsingTrace(), success, lr1Parser.getParseTree());

                            // Write parse tree to separate file
                            if (success) {
                                outputWriter.writeParseTree(inputString + "_lr1", success, lr1Parser.getParseTree());
                                System.out.println("LR(1) Parse Tree (graphviz dot):\n");
                                System.out.println(lr1Parser.getParseTree().toDot());
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading input file for LR(1): " + e.getMessage());
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