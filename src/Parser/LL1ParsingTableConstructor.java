package Parser;

import java.util.*;

public class LL1ParsingTableConstructor {

    private CFG cfg;
    private Map<String, Set<String>> firstSets;
    private Map<String, Set<String>> followSets;
    private String startSymbol;
    private Map<String, Map<String, List<String>>> parsingTable;
    private boolean isLL1;

    public LL1ParsingTableConstructor(CFG cfg, Map<String, Set<String>> firstSets,
                                      Map<String, Set<String>> followSets, String startSymbol) {
        this.cfg = cfg;
        this.firstSets = firstSets;
        this.followSets = followSets;
        this.startSymbol = startSymbol;
        this.parsingTable = new LinkedHashMap<>();
        this.isLL1 = true;
    }

    /**
     * Construct the LL(1) parsing table using the algorithm:
     * For each production A -> α:
     * 1. For each terminal a in FIRST(α):
     *      Add A -> α to M[A, a]
     * 2. If ε is in FIRST(α):
     *      For each terminal b in FOLLOW(A):
     *          Add A -> α to M[A, b]
     *      If $ is in FOLLOW(A), add A -> α to M[A, $]
     */
    public void constructParsingTable() {
        Map<String, List<List<String>>> grammar = cfg.getAllProductions();
        Set<String> terminals = collectTerminals();

        // Initialize parsing table with empty entries
        for (String nonTerminal : grammar.keySet()) {
            parsingTable.put(nonTerminal, new LinkedHashMap<>());
            for (String terminal : terminals) {
                parsingTable.get(nonTerminal).put(terminal, null);
            }
            parsingTable.get(nonTerminal).put("$", null);
        }

        // For each production A -> α
        for (Map.Entry<String, List<List<String>>> entry : grammar.entrySet()) {
            String A = entry.getKey(); // Non-terminal

            for (List<String> production : entry.getValue()) {
                // Compute FIRST(α)
                Set<String> firstAlpha = firstOfString(production);

                // Rule 1: For each terminal a in FIRST(α) (excluding ε)
                for (String terminal : firstAlpha) {
                    if (!terminal.equals("@") && !terminal.equals("ε")) {
                        addToTable(A, terminal, production);
                    }
                }

                // Rule 2: If ε is in FIRST(α)
                if (firstAlpha.contains("@") || firstAlpha.contains("ε") || production.isEmpty()) {
                    // For each terminal b in FOLLOW(A)
                    Set<String> followA = followSets.get(A);
                    for (String terminal : followA) {
                        addToTable(A, terminal, production);
                    }
                }
            }
        }
    }

    /**
     * Add a production to the parsing table and check for conflicts
     */
    private void addToTable(String nonTerminal, String terminal, List<String> production) {
        Map<String, List<String>> row = parsingTable.get(nonTerminal);

        if (row.get(terminal) != null) {
            // Conflict detected - grammar is not LL(1)
            isLL1 = false;
            System.out.println("Conflict at M[" + nonTerminal + ", " + terminal + "]:");
            System.out.println("  Existing: " + productionToString(row.get(terminal)));
            System.out.println("  New: " + productionToString(production));
        } else {
            row.put(terminal, new ArrayList<>(production));
        }
    }

    /**
     * Convert production list to readable string
     */
    private String productionToString(List<String> production) {
        if (production == null || production.isEmpty()) {
            return "@";
        }
        return String.join("", production);
    }

    /**
     * Compute FIRST set for a string of symbols (production)
     */
    private Set<String> firstOfString(List<String> symbols) {
        if (symbols == null || symbols.isEmpty() ||
                (symbols.size() == 1 && (symbols.get(0).equals("@") || symbols.get(0).equals("ε")))) {
            Set<String> result = new HashSet<>();
            result.add("@");
            return result;
        }

        Set<String> result = new LinkedHashSet<>();
        boolean allNullable = true;

        for (String symbol : symbols) {
            Set<String> firstSym;

            if (cfg.getAllProductions().containsKey(symbol)) {
                // Non-terminal
                firstSym = firstSets.get(symbol);
            } else {
                // Terminal
                firstSym = new HashSet<>();
                firstSym.add(symbol);
            }

            // Add all except ε
            for (String s : firstSym) {
                if (!s.equals("@") && !s.equals("ε")) {
                    result.add(s);
                }
            }

            // Check if this symbol is nullable
            if (!firstSym.contains("@") && !firstSym.contains("ε")) {
                allNullable = false;
                break;
            }
        }

        if (allNullable) {
            result.add("@");
        }

        return result;
    }

    /**
     * Collect all terminals from the grammar
     */
    public Set<String> collectTerminals() {
        Set<String> terminals = new TreeSet<>();
        Map<String, List<List<String>>> grammar = cfg.getAllProductions();
        Set<String> nonTerminals = grammar.keySet();

        // Scan all productions for terminals
        for (List<List<String>> productions : grammar.values()) {
            for (List<String> production : productions) {
                for (String symbol : production) {
                    // If not a non-terminal and not ε, it's a terminal
                    if (!nonTerminals.contains(symbol) && !symbol.equals("@") && !symbol.equals("ε")) {
                        terminals.add(symbol);
                    }
                }
            }
        }

        return terminals;
    }

    /**
     * Get the start symbol of the grammar
     * @return The start symbol
     */
    public String getStartSymbol() {
        return startSymbol;
    }

    /**
     * Print the LL(1) parsing table in a formatted way
     */
    public void printParsingTable() {
        System.out.println("\n--- LL(1) Parsing Table ---");

        Map<String, List<List<String>>> grammar = cfg.getAllProductions();
        Set<String> nonTerminals = grammar.keySet();
        Set<String> terminals = collectTerminals();

        // Add $ to terminals for column headers
        List<String> allTerminals = new ArrayList<>(terminals);
        allTerminals.add("$");

        // Print header
        System.out.print("Non-Terminal\t");
        for (String terminal : allTerminals) {
            System.out.print(terminal + "\t");
        }
        System.out.println();
        System.out.println("-".repeat(80));

        // Print each row
        for (String nonTerminal : nonTerminals) {
            System.out.print(nonTerminal + "\t\t");
            Map<String, List<String>> row = parsingTable.get(nonTerminal);

            for (String terminal : allTerminals) {
                List<String> production = row.get(terminal);
                if (production == null) {
                    System.out.print("-\t");
                } else {
                    System.out.print(productionToString(production) + "\t");
                }
            }
            System.out.println();
        }

        // Print LL(1) status
        System.out.println("\n--- LL(1) Grammar Check ---");
        if (isLL1) {
            System.out.println("The grammar is LL(1) - Each table entry has at most one production.");
        } else {
            System.out.println("The grammar is NOT LL(1) - Conflicts detected in parsing table.");
        }
    }

    /**
     * Print the parsing table in a more detailed format showing conflicts
     */
    public void printDetailedTable() {
        Map<String, List<List<String>>> grammar = cfg.getAllProductions();
        Set<String> nonTerminals = grammar.keySet();
        Set<String> terminals = collectTerminals();
        List<String> allTerminals = new ArrayList<>(terminals);
        allTerminals.add("$");

        // Calculate column widths
        int nonTerminalWidth = "Non-Terminal".length();
        for (String nt : nonTerminals) {
            nonTerminalWidth = Math.max(nonTerminalWidth, nt.length());
        }
        nonTerminalWidth += 2; // Add padding

        // Calculate width for each terminal column
        Map<String, Integer> columnWidths = new HashMap<>();
        for (String terminal : allTerminals) {
            int maxWidth = terminal.length();
            for (String nonTerminal : nonTerminals) {
                List<String> production = parsingTable.get(nonTerminal).get(terminal);
                String prodStr = production == null ? "-" : productionToString(production);
                maxWidth = Math.max(maxWidth, prodStr.length());
            }
            columnWidths.put(terminal, maxWidth + 2); // Add padding
        }

        // Print top border
        System.out.print("┌");
        System.out.print("─".repeat(nonTerminalWidth));
        for (String terminal : allTerminals) {
            System.out.print("┬");
            System.out.print("─".repeat(columnWidths.get(terminal)));
        }
        System.out.println("┐");

        // Print header
        System.out.print("│");
        System.out.printf(" %-" + (nonTerminalWidth - 1) + "s│", "Non-Terminal");
        for (String terminal : allTerminals) {
            System.out.printf(" %-" + (columnWidths.get(terminal) - 1) + "s│", terminal);
        }
        System.out.println();

        // Print separator after header
        System.out.print("├");
        System.out.print("─".repeat(nonTerminalWidth));
        for (String terminal : allTerminals) {
            System.out.print("┼");
            System.out.print("─".repeat(columnWidths.get(terminal)));
        }
        System.out.println("┤");

        // Print rows
        for (String nonTerminal : nonTerminals) {
            System.out.print("│");
            System.out.printf(" %-" + (nonTerminalWidth - 1) + "s│", nonTerminal);

            Map<String, List<String>> row = parsingTable.get(nonTerminal);
            for (String terminal : allTerminals) {
                List<String> production = row.get(terminal);
                String prodStr = production == null ? "-" : productionToString(production);
                System.out.printf(" %-" + (columnWidths.get(terminal) - 1) + "s│", prodStr);
            }
            System.out.println();
        }

        // Print bottom border
        System.out.print("└");
        System.out.print("─".repeat(nonTerminalWidth));
        for (String terminal : allTerminals) {
            System.out.print("┴");
            System.out.print("─".repeat(columnWidths.get(terminal)));
        }
        System.out.println("┘");

        // LL(1) status
        System.out.println("\nLL(1) Check: " + (isLL1 ?
                "Grammar is LL(1). Each table entry has at most one production" :
                "Grammar is NOT LL(1)"));
        if (!isLL1) {
            System.out.println("Conflicts found - Grammar has multiple productions for same table entry.");
        }
    }

    public boolean isLL1() {
        return isLL1;
    }

    public Map<String, Map<String, List<String>>> getParsingTable() {
        return parsingTable;
    }
}