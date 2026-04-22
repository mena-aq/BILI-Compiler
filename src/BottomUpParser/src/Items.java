package BottomUpParser.src;

import java.util.*;

public class Items {
    /**
     * Represents an LR item (dot position in a production).
     * For SLR: lookaheads are not used. For LR(1): lookaheads are used.
     */
    public static class LRItem {
        public final String lhs;
        public final List<String> rhs;
        public final int dotPosition;
        public final Set<String> lookaheads; // For LR(1), empty for SLR(1)

        public LRItem(String lhs, List<String> rhs, int dotPosition, Set<String> lookaheads) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.dotPosition = dotPosition;
            this.lookaheads = lookaheads == null ? Collections.emptySet() : lookaheads;
        }

        @Override
        public String toString() {
            List<String> rhsWithDot = new ArrayList<>(rhs);
            rhsWithDot.add(dotPosition, ".");
            String prod = lhs + " -> " + String.join(" ", rhsWithDot);
            if (!lookaheads.isEmpty()) {
                prod += ", " + lookaheads;
            }
            return prod;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LRItem)) return false;
            LRItem item = (LRItem) o;
            return dotPosition == item.dotPosition &&
                    lhs.equals(item.lhs) &&
                    rhs.equals(item.rhs) &&
                    lookaheads.equals(item.lookaheads);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lhs, rhs, dotPosition, lookaheads);
        }
    }

    /**
     * Generates all SLR(1) items for the given grammar.
     * Each item is a production with the dot at every possible position.
     * @param grammar the Grammar object
     * @return list of all SLR(1) items
     */
    public static List<LRItem> generateSLRItems(Grammar grammar) {
        List<LRItem> items = new ArrayList<>();
        for (Map.Entry<String, List<List<String>>> entry : grammar.getAllProductions().entrySet()) {
            String lhs = entry.getKey();
            for (List<String> rhs : entry.getValue()) {
                for (int pos = 0; pos <= rhs.size(); pos++) {
                    items.add(new LRItem(lhs, rhs, pos, Collections.emptySet()));
                }
            }
        }
        return items;
    }

    /**
     * Generates an item with the dot shifted one position to the right.
     * Used after a GOTO operation to advance the dot past a symbol.
     * @param item the item to shift
     * @return a new item with the dot shifted right, or null if already at end
     */
    public static LRItem generateShiftedItem(LRItem item) {
        if (item.dotPosition >= item.rhs.size()) {
            return null; // Already at end, cannot shift
        }
        return new LRItem(item.lhs, item.rhs, item.dotPosition + 1, 
                          new HashSet<>(item.lookaheads));
    }

    /**
     * Generates closure items for a specific non-terminal.
     * Returns all productions of the given non-terminal with dot at the beginning.
     * @param nonTerminal the non-terminal symbol
     * @param grammar the Grammar object
     * @return list of closure items for the non-terminal
     */
    public static List<LRItem> generateClosureItems(String nonTerminal, Grammar grammar) {
        List<LRItem> closureItems = new ArrayList<>();
        for (List<String> rhs : grammar.getProductions(nonTerminal)) {
            closureItems.add(new LRItem(nonTerminal, rhs, 0, Collections.emptySet()));
        }
        return closureItems;
    }

    /**
     * Computes the CLOSURE of a set of items.
     * For each item A -> α • B β where B is a non-terminal:
     * - Adds all items B -> • γ for each production B -> γ
     * Continues until no new items can be added.
     * @param items the set of items to compute closure for
     * @param grammar the Grammar object
     * @return the closure of the input items (may include original items)
     */
    public static Set<LRItem> closure(Set<LRItem> items, Grammar grammar) {
        Set<LRItem> closure = new HashSet<>(items);
        Queue<LRItem> toProcess = new LinkedList<>(items);

        while (!toProcess.isEmpty()) {
            LRItem item = toProcess.poll();
            // Check if there's a symbol after the dot
            if (item.dotPosition < item.rhs.size()) {
                String symbol = item.rhs.get(item.dotPosition);
                // If it's a non-terminal, add its productions with dot at the beginning
                if (isNonTerminal(symbol, grammar)) {
                    List<LRItem> closureItems = generateClosureItems(symbol, grammar);
                    for (LRItem closureItem : closureItems) {
                        if (!closure.contains(closureItem)) {
                            closure.add(closureItem);
                            toProcess.add(closureItem);
                        }
                    }
                }
            }
        }
        return closure;
    }

    /**
     * Computes GOTO(I, X) where I is a set of items and X is a grammar symbol.
     * Finds all items A -> α • X β, moves the dot past X to get A -> α X • β,
     * and returns the closure of these shifted items.
     * @param items the set of items
     * @param symbol the grammar symbol to go to
     * @param grammar the Grammar object
     * @return the GOTO set (closure of shifted items)
     */
    public static Set<LRItem> goTo(Set<LRItem> items, String symbol, Grammar grammar) {
        Set<LRItem> shiftedItems = new HashSet<>();

        // Find all items with the dot before the symbol
        for (LRItem item : items) {
            if (item.dotPosition < item.rhs.size() && 
                item.rhs.get(item.dotPosition).equals(symbol)) {
                LRItem shifted = generateShiftedItem(item);
                if (shifted != null) {
                    shiftedItems.add(shifted);
                }
            }
        }

        // Return the closure of the shifted items
        return closure(shiftedItems, grammar);
    }

    /**
     * Builds the canonical collection (DFA states) for the grammar.
     * Starts with CLOSURE({S' -> • S}) and repeatedly applies GOTO until
     * no new states can be added.
     * @param grammar the augmented Grammar object
     * @return list of item sets representing the DFA states
     */
    public static List<Set<LRItem>> buildSLRCanonicalCollection(Grammar grammar) {
        List<Set<LRItem>> canonicalCollection = new ArrayList<>();
        Set<Set<LRItem>> seen = new HashSet<>();

        // Initialize with the first production (S' -> • S)
        String startSymbol = grammar.getStartSymbol();
        List<List<String>> startProductions = grammar.getProductions(startSymbol);
        Set<LRItem> initialItems = new HashSet<>();
        if (!startProductions.isEmpty()) {
            // The first (and should be only) production of S' is S' -> S
            initialItems.add(new LRItem(startSymbol, startProductions.get(0), 0, Collections.emptySet()));
        }

        Set<LRItem> initialState = closure(initialItems, grammar);
        canonicalCollection.add(initialState);
        seen.add(initialState);

        // Process all states
        int stateIndex = 0;
        while (stateIndex < canonicalCollection.size()) {
            Set<LRItem> currentState = canonicalCollection.get(stateIndex);

            // Get all possible symbols from the grammar
            Set<String> symbols = getAllSymbols(grammar);

            // For each symbol, compute GOTO
            for (String symbol : symbols) {
                Set<LRItem> gotoState = goTo(currentState, symbol, grammar);

                // If GOTO is not empty and not already in the collection, add it
                if (!gotoState.isEmpty() && !seen.contains(gotoState)) {
                    canonicalCollection.add(gotoState);
                    seen.add(gotoState);
                }
            }
            stateIndex++;
        }

        return canonicalCollection;
    }

    /**
     * Helper method to check if a symbol is a non-terminal.
     * A symbol is a non-terminal if it exists as a key in the grammar productions.
     * @param symbol the symbol to check
     * @param grammar the Grammar object
     * @return true if the symbol is a non-terminal
     */
    private static boolean isNonTerminal(String symbol, Grammar grammar) {
        return grammar.getProductions(symbol) != null && 
               !grammar.getProductions(symbol).isEmpty();
    }

    /**
     * Helper method to get all unique symbols (terminals and non-terminals) from the grammar.
     * @param grammar the Grammar object
     * @return set of all symbols
     */
    private static Set<String> getAllSymbols(Grammar grammar) {
        Set<String> symbols = new HashSet<>();
        for (Map.Entry<String, List<List<String>>> entry : grammar.getAllProductions().entrySet()) {
            for (List<String> rhs : entry.getValue()) {
                symbols.addAll(rhs);
            }
        }
        symbols.addAll(grammar.getAllProductions().keySet());
        return symbols;
    }

    // Add these methods to your existing Items class:

    /**
     * Computes FIRST set for a string of symbols (used in LR(1) closure)
     * @param symbols the sequence of symbols
     * @param grammar the Grammar object
     * @return set of terminals that can begin the string
     */
    private static Set<String> firstOfSequence(List<String> symbols, Grammar grammar) {
        Set<String> result = new HashSet<>();
        boolean allCanBeEmpty = true;

        for (String symbol : symbols) {
            if (grammar.isNonTerminal(symbol)) {
                Set<String> firstSet = grammar.getFirstSet(symbol);
                // Add all non-epsilon symbols
                for (String term : firstSet) {
                    if (!term.equals("@")) {
                        result.add(term);
                    }
                }
                // If this symbol can't be empty, stop
                if (!firstSet.contains("@")) {
                    allCanBeEmpty = false;
                    break;
                }
            } else {
                // Terminal symbol
                result.add(symbol);
                allCanBeEmpty = false;
                break;
            }
        }

        // If all symbols can be empty, add epsilon
        if (allCanBeEmpty) {
            result.add("@");
        }

        return result;
    }

    /**
     * Computes CLOSURE for LR(1) items (with lookaheads)
     * For each item [A -> α • B β, a]:
     * - For each production B -> γ
     * - For each terminal b in FIRST(βa)
     * - Add [B -> • γ, b]
     * @param items the set of LR(1) items
     * @param grammar the Grammar object
     * @return the LR(1) closure
     */
    public static Set<LRItem> closureLR1(Set<LRItem> items, Grammar grammar) {
        Set<LRItem> closure = new HashSet<>(items);
        Queue<LRItem> toProcess = new LinkedList<>(items);

        while (!toProcess.isEmpty()) {
            LRItem item = toProcess.poll();

            // Check if there's a symbol after the dot
            if (item.dotPosition < item.rhs.size()) {
                String symbol = item.rhs.get(item.dotPosition);

                // If it's a non-terminal, add its productions
                if (grammar.isNonTerminal(symbol)) {
                    // Get β (the rest after B) and a (lookahead)
                    List<String> beta = item.rhs.subList(item.dotPosition + 1, item.rhs.size());

                    // For each lookahead in the item's lookaheads
                    for (String lookahead : item.lookaheads) {
                        // Create sequence βa
                        List<String> betaPlusA = new ArrayList<>(beta);
                        betaPlusA.add(lookahead);

                        // Compute FIRST(βa)
                        Set<String> firstSet = firstOfSequence(betaPlusA, grammar);

                        // Add all productions of B with dot at beginning
                        for (List<String> production : grammar.getProductions(symbol)) {
                            // For each terminal in FIRST(βa)
                            for (String terminal : firstSet) {
                                if (!terminal.equals("@")) {
                                    LRItem newItem = new LRItem(symbol, production, 0,
                                            new HashSet<>(Collections.singleton(terminal)));
                                    if (!closure.contains(newItem)) {
                                        closure.add(newItem);
                                        toProcess.add(newItem);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return closure;
    }

    /**
     * Computes GOTO for LR(1) items
     * @param items the set of LR(1) items
     * @param symbol the grammar symbol
     * @param grammar the Grammar object
     * @return the GOTO set (closure of shifted items)
     */
    public static Set<LRItem> goToLR1(Set<LRItem> items, String symbol, Grammar grammar) {
        Set<LRItem> shiftedItems = new HashSet<>();

        // Find all items with the dot before the symbol
        for (LRItem item : items) {
            if (item.dotPosition < item.rhs.size() &&
                    item.rhs.get(item.dotPosition).equals(symbol)) {
                LRItem shifted = generateShiftedItem(item);
                if (shifted != null) {
                    shiftedItems.add(shifted);
                }
            }
        }

        // Return the LR(1) closure of the shifted items
        return closureLR1(shiftedItems, grammar);
    }

    /**
     * Builds the canonical collection of LR(1) items
     * @param grammar the augmented Grammar object
     * @return list of LR(1) item sets
     */
    public static List<Set<LRItem>> buildLR1CanonicalCollection(Grammar grammar) {
        List<Set<LRItem>> canonicalCollection = new ArrayList<>();
        Map<Set<LRItem>, Integer> stateIndexMap = new HashMap<>();

        // Initialize with [S' -> • S, $]
        String startSymbol = grammar.getStartSymbol();
        List<List<String>> startProductions = grammar.getProductions(startSymbol);
        Set<LRItem> initialItems = new HashSet<>();

        if (!startProductions.isEmpty()) {
            // Create LR(1) item with lookahead $
            Set<String> lookahead = new HashSet<>(Collections.singleton("$"));
            initialItems.add(new LRItem(startSymbol, startProductions.get(0), 0, lookahead));
        }

        Set<LRItem> initialState = closureLR1(initialItems, grammar);
        canonicalCollection.add(initialState);
        stateIndexMap.put(initialState, 0);

        // Process all states
        int stateIndex = 0;
        while (stateIndex < canonicalCollection.size()) {
            Set<LRItem> currentState = canonicalCollection.get(stateIndex);

            // Get all possible symbols
            Set<String> symbols = getAllSymbols(grammar);

            // For each symbol, compute GOTO
            for (String symbol : symbols) {
                Set<LRItem> gotoState = goToLR1(currentState, symbol, grammar);

                // If GOTO is not empty and not already in the collection, add it
                if (!gotoState.isEmpty()) {
                    boolean found = false;
                    for (Set<LRItem> existing : canonicalCollection) {
                        if (setsEqual(existing, gotoState)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        canonicalCollection.add(gotoState);
                        stateIndexMap.put(gotoState, canonicalCollection.size() - 1);
                    }
                }
            }
            stateIndex++;
        }

        return canonicalCollection;
    }

    /**
     * Helper to check if two sets of LR(1) items are equal
     */
    private static boolean setsEqual(Set<LRItem> set1, Set<LRItem> set2) {
        if (set1.size() != set2.size()) return false;
        return set1.containsAll(set2);
    }

    /**
     * Prints LR(1) item sets
     */
    public static void printLR1ItemSets(List<Set<LRItem>> itemSets) {
        System.out.println("\n=== LR(1) Item Sets ===");
        for (int i = 0; i < itemSets.size(); i++) {
            System.out.println("\nI" + i + ":");
            List<LRItem> sortedItems = new ArrayList<>(itemSets.get(i));
            // Sort for consistent output
            sortedItems.sort((a, b) -> {
                int cmp = a.lhs.compareTo(b.lhs);
                if (cmp != 0) return cmp;
                cmp = Integer.compare(a.dotPosition, b.dotPosition);
                if (cmp != 0) return cmp;
                return a.rhs.toString().compareTo(b.rhs.toString());
            });
            for (LRItem item : sortedItems) {
                System.out.println("  " + item);
            }
        }
    }
}
