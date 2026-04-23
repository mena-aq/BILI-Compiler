package BottomUpParser.src;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Utility class to handle writing output to files.
 * Creates and manages the output directory structure for SLR and LR(1) parser results.
 */
public class OutputWriter {
    private final Path outputDir;
    private PrintWriter augmentedGrammarWriter;
    private PrintWriter slrItemsWriter;
    private PrintWriter slrTableWriter;
    private PrintWriter slrTraceWriter;
    private PrintWriter parseTreeWriter;

    // LR(1) writers
    private PrintWriter lr1ItemsWriter;
    private PrintWriter lr1TableWriter;
    private PrintWriter lr1TraceWriter;

    // Comparison writer
    private PrintWriter comparisonWriter;

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

            // LR(1) writers
            lr1ItemsWriter = new PrintWriter(
                    new FileWriter(outputDir.resolve("lr1_items.txt").toFile())
            );
            lr1TableWriter = new PrintWriter(
                    new FileWriter(outputDir.resolve("lr1_parsing_table.txt").toFile())
            );
            lr1TraceWriter = new PrintWriter(
                    new FileWriter(outputDir.resolve("lr1_trace.txt").toFile())
            );

            // Comparison writer
            comparisonWriter = new PrintWriter(
                    new FileWriter(outputDir.resolve("comparison.txt").toFile())
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

        // Close LR(1) writers
        if (lr1ItemsWriter != null) lr1ItemsWriter.close();
        if (lr1TableWriter != null) lr1TableWriter.close();
        if (lr1TraceWriter != null) lr1TraceWriter.close();

        // Close comparison writer
        if (comparisonWriter != null) comparisonWriter.close();
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
        slrItemsWriter.println("=== CANONICAL COLLECTION (DFA STATES) - SLR(1) ===\n");

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
     * Writes all LR(1) items (canonical collection with lookaheads) to file
     */
    public void writeLR1Items(List<Set<Items.LRItem>> canonicalCollection) {
        lr1ItemsWriter.println("=== CANONICAL COLLECTION (DFA STATES) - LR(1) ===\n");
        lr1ItemsWriter.println("Note: Items include lookahead information in brackets\n");

        for (int i = 0; i < canonicalCollection.size(); i++) {
            lr1ItemsWriter.println("I" + i + ":");

            // Sort items for consistent output
            List<Items.LRItem> sortedItems = new ArrayList<>(canonicalCollection.get(i));
            sortedItems.sort((a, b) -> {
                int cmp = a.lhs.compareTo(b.lhs);
                if (cmp != 0) return cmp;
                cmp = Integer.compare(a.dotPosition, b.dotPosition);
                if (cmp != 0) return cmp;
                return a.rhs.toString().compareTo(b.rhs.toString());
            });

            for (Items.LRItem item : sortedItems) {
                lr1ItemsWriter.println("  " + item);
            }
            lr1ItemsWriter.println();
        }

        lr1ItemsWriter.flush();
    }

    /**
     * Writes the SLR parsing table to file using formatted table output
     */
    public void writeSLRTable(ParsingTable parsingTable, List<Set<Items.LRItem>> canonicalCollection, Grammar grammar) {
        writeParsingTable(parsingTable, canonicalCollection, grammar, slrTableWriter, "SLR(1)");
    }

    /**
     * Writes the LR(1) parsing table to file using formatted table output
     */
    public void writeLR1Table(ParsingTable parsingTable, List<Set<Items.LRItem>> canonicalCollection, Grammar grammar) {
        writeParsingTable(parsingTable, canonicalCollection, grammar, lr1TableWriter, "LR(1)");
    }

    /**
     * Generic method to write parsing table to file
     */
    private void writeParsingTable(ParsingTable parsingTable, List<Set<Items.LRItem>> canonicalCollection,
                                   Grammar grammar, PrintWriter writer, String parserType) {
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

        writer.println("=== " + parserType + " PARSING TABLE ===\n");

        // Print top border
        writer.print("┌");
        writer.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            writer.print("┬");
            writer.print("─".repeat(columnWidths.get(symbol)));
        }
        writer.println("┐");

        // Print header
        writer.print("│");
        writer.printf(" %-" + (stateWidth - 1) + "s│", "State");
        for (String symbol : allSymbols) {
            writer.printf(" %-" + (columnWidths.get(symbol) - 1) + "s│", symbol);
        }
        writer.println();

        // Print separator after header
        writer.print("├");
        writer.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            writer.print("┼");
            writer.print("─".repeat(columnWidths.get(symbol)));
        }
        writer.println("┤");

        // Print rows
        for (int state = 0; state < canonicalCollection.size(); state++) {
            writer.print("│");
            writer.printf(" %-" + (stateWidth - 1) + "s│", state);

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
                writer.printf(" %-" + (columnWidths.get(symbol) - 1) + "s│", cellStr);
            }
            writer.println();
        }

        // Print bottom border
        writer.print("└");
        writer.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            writer.print("┴");
            writer.print("─".repeat(columnWidths.get(symbol)));
        }
        writer.println("┘");

        // Print summary
        writer.println("\nParser Type: " + parserType);
        writer.println("Is Parseable: " + (parsingTable.isSlrParseable() ? "YES" : "NO"));
        writer.println("Conflicts Found: " + parsingTable.getConflicts().size());

        if (!parsingTable.getConflicts().isEmpty()) {
            writer.println("\nConflicts:");
            for (ParsingTable.Conflict conflict : parsingTable.getConflicts()) {
                writer.println("  " + conflict);
            }
        }
        writer.println();

        writer.flush();
    }

    /**
     * Writes SLR parsing trace for a single input
     */
    public void writeSLRTrace(String inputString, List<SLRParser.ParsingStep> parsingTrace, boolean success, Tree parseTree) {
        writeTrace(inputString, parsingTrace, success, parseTree, slrTraceWriter, "SLR(1)");
    }

    /**
     * Writes LR(1) parsing trace for a single input
     */
    public void writeLR1Trace(String inputString, List<SLRParser.ParsingStep> parsingTrace, boolean success, Tree parseTree) {
        writeTrace(inputString, parsingTrace, success, parseTree, lr1TraceWriter, "LR(1)");
    }

    /**
     * Generic method to write parsing trace to file
     */
    private void writeTrace(String inputString, List<SLRParser.ParsingStep> parsingTrace,
                            boolean success, Tree parseTree, PrintWriter writer, String parserType) {
        writer.println("=== " + parserType + " PARSING TRACE ===");
        writer.println("=== INPUT: " + inputString + " ===");
        writer.println("Result: " + (success ? "ACCEPTED" : "REJECTED"));
        writer.println();

        writer.println(String.format("%-8s %-30s %-20s %s", "Step", "Stack", "Input", "Action"));
        writer.println("-".repeat(80));

        for (SLRParser.ParsingStep step : parsingTrace) {
            writer.println(step);
        }

        writer.println("\n" + "=".repeat(80) + "\n");
        writer.flush();
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
     * Writes a comparison of SLR(1) and LR(1) parsing table action sections
     */
    public void writeComparison(ParsingTable slrTable, ParsingTable lr1Table,
                                List<Set<Items.LRItem>> slrCollection,
                                List<Set<Items.LRItem>> lr1Collection,
                                Grammar grammar) {
        comparisonWriter.println("=== PARSING TABLE COMPARISON: SLR(1) vs LR(1) ===\n");

        // SLR(1) Action Table
        writeActionSection(slrTable, slrCollection, grammar, comparisonWriter, "SLR(1)");

        comparisonWriter.println("\n" + "=".repeat(100) + "\n");

        // LR(1) Action Table
        writeActionSection(lr1Table, lr1Collection, grammar, comparisonWriter, "LR(1)");

        comparisonWriter.flush();
    }

    /**
     * Helper method to write action table section for comparison
     */
    private void writeActionSection(ParsingTable parsingTable, List<Set<Items.LRItem>> canonicalCollection,
                                   Grammar grammar, PrintWriter writer, String parserType) {
        // Collect all terminals
        Set<String> terminals = new TreeSet<>();
        for (Map<String, ParsingTable.Action> row : parsingTable.getActionTable().values()) {
            terminals.addAll(row.keySet());
        }

        // Create ordered list of terminals
        List<String> allSymbols = new ArrayList<>(terminals);

        // Calculate column widths
        int stateWidth = Math.max("State".length(), String.valueOf(canonicalCollection.size()).length()) + 2;
        Map<String, Integer> columnWidths = new HashMap<>();

        for (String symbol : allSymbols) {
            int maxWidth = symbol.length();
            for (int state = 0; state < canonicalCollection.size(); state++) {
                ParsingTable.Action action = parsingTable.getAction(state, symbol);
                String cellStr;
                if (action.type == ParsingTable.Action.Type.ERROR) {
                    cellStr = "-";
                } else {
                    cellStr = action.toString();
                }
                maxWidth = Math.max(maxWidth, cellStr.length());
            }
            columnWidths.put(symbol, maxWidth + 2);
        }

        writer.println(parserType + " ACTION TABLE:");
        writer.println();

        // Print top border
        writer.print("┌");
        writer.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            writer.print("┬");
            writer.print("─".repeat(columnWidths.get(symbol)));
        }
        writer.println("┐");

        // Print header
        writer.print("│");
        writer.printf(" %-" + (stateWidth - 1) + "s│", "State");
        for (String symbol : allSymbols) {
            writer.printf(" %-" + (columnWidths.get(symbol) - 1) + "s│", symbol);
        }
        writer.println();

        // Print separator after header
        writer.print("├");
        writer.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            writer.print("┼");
            writer.print("─".repeat(columnWidths.get(symbol)));
        }
        writer.println("┤");

        // Print rows
        for (int state = 0; state < canonicalCollection.size(); state++) {
            writer.print("│");
            writer.printf(" %-" + (stateWidth - 1) + "s│", state);

            for (String symbol : allSymbols) {
                ParsingTable.Action action = parsingTable.getAction(state, symbol);
                String cellStr;
                if (action.type == ParsingTable.Action.Type.ERROR) {
                    cellStr = "-";
                } else {
                    cellStr = action.toString();
                }
                writer.printf(" %-" + (columnWidths.get(symbol) - 1) + "s│", cellStr);
            }
            writer.println();
        }

        // Print bottom border
        writer.print("└");
        writer.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            writer.print("┴");
            writer.print("─".repeat(columnWidths.get(symbol)));
        }
        writer.println("┘");

        writer.println();
        writer.println("States: " + canonicalCollection.size());
    }
}