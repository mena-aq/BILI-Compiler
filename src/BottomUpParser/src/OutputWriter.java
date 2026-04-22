package BottomUpParser.src;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Utility class to handle writing output to files.
 * Creates and manages the output directory structure for SLR parser results.
 */
public class OutputWriter {
    private final Path outputDir;
    private PrintWriter augmentedGrammarWriter;
    private PrintWriter slrItemsWriter;
    private PrintWriter slrTableWriter;
    private PrintWriter slrTraceWriter;
    private PrintWriter parseTreeWriter;

    /**
     * Constructor initializes the output directory
     * @param baseDir the base directory where output folder will be created
     */
    public OutputWriter(String baseDir) {
        try {
            this.outputDir = Paths.get(baseDir, "output");
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory: " + e.getMessage(), e);
        }
    }

    /**
     * Opens all output file writers
     */
    public void openWriters() {
        try {
            augmentedGrammarWriter = new PrintWriter(
                    new FileWriter(outputDir.resolve("augmented_grammar.txt").toFile())
            );
            slrItemsWriter = new PrintWriter(
                    new FileWriter(outputDir.resolve("slr_items.txt").toFile())
            );
            slrTableWriter = new PrintWriter(
                    new FileWriter(outputDir.resolve("slr_parsing_table.txt").toFile())
            );
            slrTraceWriter = new PrintWriter(
                    new FileWriter(outputDir.resolve("slr_trace.txt").toFile())
            );
            parseTreeWriter = new PrintWriter(
                    new FileWriter(outputDir.resolve("parse_trees.txt").toFile())
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to open output writers: " + e.getMessage(), e);
        }
    }

    /**
     * Closes all output file writers
     */
    public void closeWriters() {
        if (augmentedGrammarWriter != null) augmentedGrammarWriter.close();
        if (slrItemsWriter != null) slrItemsWriter.close();
        if (slrTableWriter != null) slrTableWriter.close();
        if (slrTraceWriter != null) slrTraceWriter.close();
        if (parseTreeWriter != null) parseTreeWriter.close();
    }

    /**
     * Writes augmented grammar and FIRST/FOLLOW sets to file
     */
    public void writeAugmentedGrammar(Grammar grammar) {
        augmentedGrammarWriter.println("=== AUGMENTED GRAMMAR ===\n");

        // Write grammar
        for (Map.Entry<String, List<List<String>>> entry : grammar.getAllProductions().entrySet()) {
            String lhs = entry.getKey();
            List<List<String>> rhsList = entry.getValue();
            augmentedGrammarWriter.print(lhs + " -> ");
            for (int i = 0; i < rhsList.size(); i++) {
                List<String> rhs = rhsList.get(i);
                augmentedGrammarWriter.print(String.join(" ", rhs));
                if (i < rhsList.size() - 1) {
                    augmentedGrammarWriter.print(" | ");
                }
            }
            augmentedGrammarWriter.println();
        }

        // Write FIRST sets
        augmentedGrammarWriter.println("\n=== FIRST SETS ===\n");
        for (String nt : grammar.getAllProductions().keySet()) {
            augmentedGrammarWriter.println("First(" + nt + ") = " + grammar.getFirstSet(nt));
        }

        // Write FOLLOW sets
        augmentedGrammarWriter.println("\n=== FOLLOW SETS ===\n");
        for (String nt : grammar.getAllProductions().keySet()) {
            augmentedGrammarWriter.println("Follow(" + nt + ") = " + grammar.getFollowSet(nt));
        }

        augmentedGrammarWriter.flush();
    }

    /**
     * Writes all SLR items (canonical collection) to file
     */
    public void writeSLRItems(List<Set<Items.LRItem>> canonicalCollection) {
        slrItemsWriter.println("=== CANONICAL COLLECTION (DFA STATES) ===\n");

        for (int i = 0; i < canonicalCollection.size(); i++) {
            slrItemsWriter.println("I" + i + ":");

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

            // Write actual items first, then closure items
            for (Items.LRItem item : actualItems) {
                slrItemsWriter.println("  " + item);
            }
            for (Items.LRItem item : closureItems) {
                slrItemsWriter.println("  " + item);
            }
            slrItemsWriter.println();
        }

        slrItemsWriter.flush();
    }

    /**
     * Writes the SLR parsing table to file using formatted table output
     */
    public void writeSLRTable(ParsingTable parsingTable, List<Set<Items.LRItem>> canonicalCollection, Grammar grammar) {
        // Collect all terminals and non-terminals
        Set<String> terminals = new TreeSet<>();
        Set<String> nonTerminals = new TreeSet<>();

        for (Map<String, ParsingTable.Action> row : parsingTable.getActionTable().values()) {
            terminals.addAll(row.keySet());
        }

        for (Map<String, Integer> row : parsingTable.getGotoTable().values()) {
            nonTerminals.addAll(row.keySet());
        }

        // Remove $ from non-terminals if present
        nonTerminals.remove("$");

        // Create ordered list of all symbols (terminals first, then non-terminals)
        List<String> allSymbols = new ArrayList<>(terminals);
        allSymbols.addAll(nonTerminals);

        // Calculate column widths
        int stateWidth = Math.max("State".length(), String.valueOf(canonicalCollection.size()).length()) + 2;
        Map<String, Integer> columnWidths = new HashMap<>();

        for (String symbol : allSymbols) {
            int maxWidth = symbol.length();
            for (int state = 0; state < canonicalCollection.size(); state++) {
                String cellStr;
                if (terminals.contains(symbol)) {
                    ParsingTable.Action action = parsingTable.getAction(state, symbol);
                    if (action.type == ParsingTable.Action.Type.ERROR) {
                        cellStr = "-";
                    } else {
                        cellStr = action.toString();
                    }
                } else {
                    Integer gotoState = parsingTable.getGoto(state, symbol);
                    cellStr = gotoState != null ? String.valueOf(gotoState) : "-";
                }
                maxWidth = Math.max(maxWidth, cellStr.length());
            }
            columnWidths.put(symbol, maxWidth + 2); // Add padding
        }

        slrTableWriter.println("=== SLR(1) PARSING TABLE ===\n");

        // Print top border
        slrTableWriter.print("┌");
        slrTableWriter.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            slrTableWriter.print("┬");
            slrTableWriter.print("─".repeat(columnWidths.get(symbol)));
        }
        slrTableWriter.println("┐");

        // Print header
        slrTableWriter.print("│");
        slrTableWriter.printf(" %-" + (stateWidth - 1) + "s│", "State");
        for (String symbol : allSymbols) {
            slrTableWriter.printf(" %-" + (columnWidths.get(symbol) - 1) + "s│", symbol);
        }
        slrTableWriter.println();

        // Print separator after header
        slrTableWriter.print("├");
        slrTableWriter.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            slrTableWriter.print("┼");
            slrTableWriter.print("─".repeat(columnWidths.get(symbol)));
        }
        slrTableWriter.println("┤");

        // Print rows
        for (int state = 0; state < canonicalCollection.size(); state++) {
            slrTableWriter.print("│");
            slrTableWriter.printf(" %-" + (stateWidth - 1) + "s│", state);

            for (String symbol : allSymbols) {
                String cellStr;
                if (terminals.contains(symbol)) {
                    ParsingTable.Action action = parsingTable.getAction(state, symbol);
                    if (action.type == ParsingTable.Action.Type.ERROR) {
                        cellStr = "-";
                    } else {
                        cellStr = action.toString();
                    }
                } else {
                    Integer gotoState = parsingTable.getGoto(state, symbol);
                    cellStr = gotoState != null ? String.valueOf(gotoState) : "-";
                }
                slrTableWriter.printf(" %-" + (columnWidths.get(symbol) - 1) + "s│", cellStr);
            }
            slrTableWriter.println();
        }

        // Print bottom border
        slrTableWriter.print("└");
        slrTableWriter.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            slrTableWriter.print("┴");
            slrTableWriter.print("─".repeat(columnWidths.get(symbol)));
        }
        slrTableWriter.println("┘");

        // Print summary
        slrTableWriter.println("\nParser Type: SLR(1)");
        slrTableWriter.println("Is SLR(1) Parseable: " + (parsingTable.isSlrParseable() ? "YES" : "NO"));
        slrTableWriter.println("Conflicts Found: " + parsingTable.getConflicts().size());

        if (!parsingTable.getConflicts().isEmpty()) {
            slrTableWriter.println("\nConflicts:");
            for (ParsingTable.Conflict conflict : parsingTable.getConflicts()) {
                slrTableWriter.println("  " + conflict);
            }
        }
        slrTableWriter.println();

        slrTableWriter.flush();
    }

    /**
     * Writes parsing trace for a single input (without parse tree)
     */
    public void writeSLRTrace(String inputString, List<SLRParser.ParsingStep> parsingTrace, boolean success, Tree parseTree) {
        slrTraceWriter.println("=== INPUT: " + inputString + " ===");
        slrTraceWriter.println("Result: " + (success ? "ACCEPTED" : "REJECTED"));
        slrTraceWriter.println();

        slrTraceWriter.println(String.format("%-8s %-30s %-20s %s", "Step", "Stack", "Input", "Action"));
        slrTraceWriter.println("-".repeat(80));

        for (SLRParser.ParsingStep step : parsingTrace) {
            slrTraceWriter.println(step);
        }

        slrTraceWriter.println("\n" + "=".repeat(80) + "\n");
        slrTraceWriter.flush();
    }

    /**
     * Writes parse tree for a successful parse with its input
     */
    public void writeParseTree(String inputString, boolean success, Tree parseTree) {
        if (success && parseTree != null) {
            parseTreeWriter.println("=== INPUT: " + inputString + " ===");
            parseTreeWriter.println();
            parseTreeWriter.println(parseTree.toDot());
            parseTreeWriter.println();
            parseTreeWriter.println("=".repeat(80));
            parseTreeWriter.println();
            parseTreeWriter.flush();
        }
    }

    /**
     * Helper method to check if a symbol is a non-terminal
     */
    private boolean isNonTerminal(String symbol, Grammar grammar) {
        return grammar.getProductions(symbol) != null &&
               !grammar.getProductions(symbol).isEmpty();
    }
}




