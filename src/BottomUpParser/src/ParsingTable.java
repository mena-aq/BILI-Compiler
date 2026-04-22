package BottomUpParser.src;

import java.util.*;

/**
 * Flexible SLR(1)/LR(1) parsing table constructor.
 * Can be extended to support LR(1) by providing lookahead sets in items.
 */
public class ParsingTable {

    /**
     * Represents a parsing action: shift, reduce, or accept
     */
    public static class Action {
        public enum Type { SHIFT, REDUCE, ACCEPT, ERROR }

        public final Type type;
        public final int targetState; // For shift actions
        public final String production; // For reduce actions (e.g., "Expr -> Expr + Term")
        public final Set<String> lookaheads; // For LR(1) reduce actions (empty for SLR(1))

        public Action(Type type, int targetState, String production) {
            this(type, targetState, production, Collections.emptySet());
        }

        /**
         * Constructor for LR(1) actions with lookaheads
         */
        public Action(Type type, int targetState, String production, Set<String> lookaheads) {
            this.type = type;
            this.targetState = targetState;
            this.production = production;
            this.lookaheads = lookaheads == null ? Collections.emptySet() : new HashSet<>(lookaheads);
        }

        public static Action shift(int state) {
            return new Action(Type.SHIFT, state, null);
        }

        public static Action reduce(String prod) {
            return new Action(Type.REDUCE, -1, prod);
        }

        /**
         * Create a reduce action with specific lookaheads (for LR(1))
         */
        public static Action reduce(String prod, Set<String> lookaheads) {
            return new Action(Type.REDUCE, -1, prod, lookaheads);
        }

        public static Action accept() {
            return new Action(Type.ACCEPT, -1, null);
        }

        public static Action error() {
            return new Action(Type.ERROR, -1, null);
        }

        @Override
        public String toString() {
            return switch (type) {
                case SHIFT -> "s" + targetState;
                case REDUCE -> {
                    String result = "r(" + production + ")";
                    if (!lookaheads.isEmpty()) {
                        result += "[" + String.join("|", lookaheads) + "]";
                    }
                    yield result;
                }
                case ACCEPT -> "accept";
                case ERROR -> "error";
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Action)) return false;
            Action action = (Action) o;
            return targetState == action.targetState &&
                    type == action.type &&
                    Objects.equals(production, action.production);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, targetState, production);
        }
    }

    /**
     * Represents a conflict in the parsing table
     */
    public static class Conflict {
        public enum ConflictType { SHIFT_REDUCE, REDUCE_REDUCE }

        public final int state;
        public final String symbol;
        public final ConflictType type;
        public final String details;

        public Conflict(int state, String symbol, ConflictType type, String details) {
            this.state = state;
            this.symbol = symbol;
            this.type = type;
            this.details = details;
        }

        @Override
        public String toString() {
            return type + " conflict in state " + state + " on symbol '" + symbol + "': " + details;
        }
    }

    private final Map<Integer, Map<String, Action>> actionTable;
    private final Map<Integer, Map<String, Integer>> gotoTable;
    private final Map<Integer, Map<String, List<Action>>> conflictTable; // Store conflicting actions
    private final List<Conflict> conflicts;
    private boolean isSlrParseable;
    private final Grammar grammar;
    private final List<Set<Items.LRItem>> canonicalCollection;
    private final Map<Set<Items.LRItem>, Integer> stateMap;
    private final boolean useLookaheads; // Flag for LR(1) vs SLR(1)

    /**
     * Constructor for SLR(1) parsing table
     */
    public ParsingTable(Grammar grammar, List<Set<Items.LRItem>> canonicalCollection) {
        this(grammar, canonicalCollection, false);
    }

    /**
     * Constructor with flexibility for SLR(1) and LR(1) tables
     * @param grammar the augmented grammar
     * @param canonicalCollection the DFA states (canonical collection)
     * @param useLookaheads true for LR(1), false for SLR(1)
     */
    public ParsingTable(Grammar grammar, List<Set<Items.LRItem>> canonicalCollection, boolean useLookaheads) {
        this.grammar = grammar;
        this.canonicalCollection = canonicalCollection;
        this.actionTable = new HashMap<>();
        this.gotoTable = new HashMap<>();
        this.conflictTable = new HashMap<>();
        this.conflicts = new ArrayList<>();
        this.isSlrParseable = true;
        this.stateMap = new HashMap<>();
        this.useLookaheads = useLookaheads;

        // Map each canonical collection state to its index
        for (int i = 0; i < canonicalCollection.size(); i++) {
            stateMap.put(canonicalCollection.get(i), i);
        }

        // Build the parsing table
        buildTable();
    }

    /**
     * Builds the parsing table from the canonical collection.
     * Supports both SLR(1) and LR(1) via the useLookaheads flag.
     */
    private void buildTable() {
        if (useLookaheads) {
            buildLR1Table();
        } else {
            buildSLR1Table();
        }
    }

    /**
     * Specialized SLR(1) parsing table construction.
     * Uses FOLLOW sets for reduce actions.
     */
    private void buildSLR1Table() {
        // Ensure FOLLOW sets are computed
        grammar.computeFollowSets();

        for (int stateIndex = 0; stateIndex < canonicalCollection.size(); stateIndex++) {
            Set<Items.LRItem> state = canonicalCollection.get(stateIndex);
            Map<String, Action> actionRow = new HashMap<>();
            Map<String, Integer> gotoRow = new HashMap<>();

            // Process all items in the state
            for (Items.LRItem item : state) {
                if (item.dotPosition < item.rhs.size()) {
                    // Shift actions: A -> α • X β
                    processSLRShiftActions(item, state, stateIndex, actionRow, gotoRow);
                } else {
                    // Reduce actions or accept: A -> α •
                    processSLRReduceActions(item, stateIndex, actionRow);
                }
            }

            actionTable.put(stateIndex, actionRow);
            gotoTable.put(stateIndex, gotoRow);
        }
    }

    /**
     * Specialized LR(1) parsing table construction.
     * Uses item lookaheads for reduce actions.
     */
    private void buildLR1Table() {
        for (int stateIndex = 0; stateIndex < canonicalCollection.size(); stateIndex++) {
            Set<Items.LRItem> state = canonicalCollection.get(stateIndex);
            Map<String, Action> actionRow = new HashMap<>();
            Map<String, Integer> gotoRow = new HashMap<>();

            // Process all items in the state
            for (Items.LRItem item : state) {
                if (item.dotPosition < item.rhs.size()) {
                    // Shift actions: A -> α • X β
                    processLR1ShiftActions(item, state, stateIndex, actionRow, gotoRow); // Changed to LR1 version
                } else {
                    // Reduce actions or accept: A -> α •
                    processLR1ReduceActions(item, stateIndex, actionRow);
                }
            }

            actionTable.put(stateIndex, actionRow);
            gotoTable.put(stateIndex, gotoRow);
        }
    }

    /**
     * Process shift actions for SLR(1) table
     */
    private void processSLRShiftActions(Items.LRItem item, Set<Items.LRItem> state,
                                       int stateIndex, Map<String, Action> actionRow,
                                       Map<String, Integer> gotoRow) {
        String symbol = item.rhs.get(item.dotPosition);
        Set<Items.LRItem> gotoState = Items.goTo(state, symbol, grammar);

        if (!gotoState.isEmpty() && stateMap.containsKey(gotoState)) {
            int targetState = stateMap.get(gotoState);

            if (isNonTerminal(symbol)) {
                // GOTO action
                gotoRow.put(symbol, targetState);
            } else {
                // SHIFT action (terminal)
                Action shiftAction = Action.shift(targetState);
                Action existing = actionRow.get(symbol);

                if (existing != null && existing.type != Action.Type.SHIFT) {
                    // Conflict: shift/reduce
                    String details = "shift " + targetState + " vs " + existing;
                    conflicts.add(new Conflict(stateIndex, symbol, Conflict.ConflictType.SHIFT_REDUCE, details));
                    isSlrParseable = false;
                }
                actionRow.put(symbol, shiftAction);
            }
        }
    }

    /**
     * Process reduce actions for SLR(1) table using FOLLOW sets
     */
    private void processSLRReduceActions(Items.LRItem item, int stateIndex,
                                         Map<String, Action> actionRow) {
        if (isAcceptItem(item)) {
            // Accept action for S' -> S •
            Action acceptAction = Action.accept();
            actionRow.put("$", acceptAction);
        } else {
            // Reduce action: A -> α •
            // For all terminals in FOLLOW(A), add reduce action
            Set<String> followSet = grammar.getFollowSet(item.lhs);
            String production = item.lhs + " -> " + String.join(" ", item.rhs);

            for (String terminal : followSet) {
                addReduceAction(terminal, production, item, stateIndex, actionRow);
            }
        }
    }

    /**
     * Process reduce actions for LR(1) table using item lookaheads
     */
    private void processLR1ReduceActions(Items.LRItem item, int stateIndex,
                                        Map<String, Action> actionRow) {
        if (isAcceptItem(item)) {
            // Accept action for S' -> S •
            Action acceptAction = Action.accept();
            actionRow.put("$", acceptAction);
        } else {
            // Reduce action: A -> α •
            // For all terminals in item.lookaheads, add reduce action
            String production = item.lhs + " -> " + String.join(" ", item.rhs);

            // Use lookaheads if available, otherwise fall back to FOLLOW
            Set<String> reduceSymbols = !item.lookaheads.isEmpty() ?
                                       item.lookaheads :
                                       grammar.getFollowSet(item.lhs);

            for (String terminal : reduceSymbols) {
                addReduceAction(terminal, production, item, stateIndex, actionRow);
            }
        }
    }

    // Add this method to your ParsingTable class:

    /**
     * Process shift actions for LR(1) table using LR(1) GOTO
     */
    private void processLR1ShiftActions(Items.LRItem item, Set<Items.LRItem> state,
                                        int stateIndex, Map<String, Action> actionRow,
                                        Map<String, Integer> gotoRow) {
        String symbol = item.rhs.get(item.dotPosition);

        // Use LR(1) GOTO for shift actions
        Set<Items.LRItem> gotoState = Items.goToLR1(state, symbol, grammar);

        if (!gotoState.isEmpty() && stateMap.containsKey(gotoState)) {
            int targetState = stateMap.get(gotoState);

            if (isNonTerminal(symbol)) {
                // GOTO action
                gotoRow.put(symbol, targetState);
            } else {
                // SHIFT action (terminal)
                Action shiftAction = Action.shift(targetState);
                Action existing = actionRow.get(symbol);

                if (existing != null && existing.type != Action.Type.SHIFT) {
                    // Conflict: shift/reduce
                    String details = "shift " + targetState + " vs " + existing;
                    conflicts.add(new Conflict(stateIndex, symbol, Conflict.ConflictType.SHIFT_REDUCE, details));
                    isSlrParseable = false;
                }
                actionRow.put(symbol, shiftAction);
            }
        }
    }

    /**
     * Helper method to add a reduce action and handle conflicts
     */
    private void addReduceAction(String terminal, String production, Items.LRItem item,
                                int stateIndex, Map<String, Action> actionRow) {
        Action reduceAction = Action.reduce(production, item.lookaheads);
        Action existing = actionRow.get(terminal);

        if (existing != null) {
            if (existing.type == Action.Type.SHIFT) {
                // Shift/Reduce conflict
                String details = "shift vs reduce " + production;
                conflicts.add(new Conflict(stateIndex, terminal, Conflict.ConflictType.SHIFT_REDUCE, details));
                isSlrParseable = false;
                // Store both actions in conflict table
                storeConflictingActions(stateIndex, terminal, existing, reduceAction);
            } else if (existing.type == Action.Type.REDUCE && !existing.production.equals(production)) {
                // Reduce/Reduce conflict
                String details = "reduce " + existing.production + " vs reduce " + production;
                conflicts.add(new Conflict(stateIndex, terminal, Conflict.ConflictType.REDUCE_REDUCE, details));
                isSlrParseable = false;
                // Store both actions in conflict table
                storeConflictingActions(stateIndex, terminal, existing, reduceAction);
            }
        } else {
            actionRow.put(terminal, reduceAction);
        }
    }

    /**
     * Helper method to store conflicting actions
     */
    private void storeConflictingActions(int stateIndex, String terminal, Action action1, Action action2) {
        if (!conflictTable.containsKey(stateIndex)) {
            conflictTable.put(stateIndex, new HashMap<>());
        }

        List<Action> actions = conflictTable.get(stateIndex).computeIfAbsent(terminal, k -> new ArrayList<>());

        // Add first action if not already present
        if (actions.isEmpty() || !actions.get(0).equals(action1)) {
            actions.add(action1);
        }

        // Add second action if not already present
        if (!actions.contains(action2)) {
            actions.add(action2);
        }
    }

    /**
     * Checks if an item is an accept item (S' -> S •)
     */
    private boolean isAcceptItem(Items.LRItem item) {
        return item.lhs.equals(grammar.getStartSymbol()) &&
               item.rhs.size() == 1 &&
               item.rhs.get(0).equals(grammar.getStartSymbol().substring(0, grammar.getStartSymbol().length() - 1));
    }

    /**
     * Helper method to check if a symbol is a non-terminal
     */
    private boolean isNonTerminal(String symbol) {
        return grammar.getProductions(symbol) != null &&
               !grammar.getProductions(symbol).isEmpty();
    }

    /**
     * Gets the action for a given state and symbol
     */
    public Action getAction(int state, String symbol) {
        return actionTable.getOrDefault(state, new HashMap<>()).getOrDefault(symbol, Action.error());
    }

    /**
     * Gets the goto for a given state and non-terminal
     */
    public Integer getGoto(int state, String nonTerminal) {
        return gotoTable.getOrDefault(state, new HashMap<>()).get(nonTerminal);
    }

    /**
     * Checks if the grammar is SLR parseable (no conflicts)
     */
    public boolean isSlrParseable() {
        return isSlrParseable;
    }

    /**
     * Gets all conflicts found in the table
     */
    public List<Conflict> getConflicts() {
        return new ArrayList<>(conflicts);
    }

    /**
     * Prints the parsing table in a detailed formatted way
     * @return true if the grammar is SLR(1) parseable, false otherwise
     */
    public boolean printParsingTable() {
        // Collect all terminals and non-terminals
        Set<String> terminals = new TreeSet<>();
        Set<String> nonTerminals = new TreeSet<>();

        for (Map<String, Action> row : actionTable.values()) {
            terminals.addAll(row.keySet());
        }

        for (Map<String, Integer> row : gotoTable.values()) {
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
                    // Check for conflicts
                    if (conflictTable.containsKey(state) && conflictTable.get(state).containsKey(symbol)) {
                        List<Action> conflictingActions = conflictTable.get(state).get(symbol);
                        List<String> actionStrs = new ArrayList<>();
                        for (Action action : conflictingActions) {
                            if (action.type != Action.Type.ERROR) {
                                actionStrs.add(action.toString());
                            }
                        }
                        cellStr = String.join(" / ", actionStrs);
                    } else {
                        Action action = getAction(state, symbol);
                        if (action.type == Action.Type.ERROR) {
                            cellStr = "-";
                        } else {
                            cellStr = action.toString();
                        }
                    }
                } else {
                    Integer gotoState = getGoto(state, symbol);
                    cellStr = gotoState != null ? String.valueOf(gotoState) : "-";
                }
                maxWidth = Math.max(maxWidth, cellStr.length());
            }
            columnWidths.put(symbol, maxWidth + 2); // Add padding
        }

        // Print top border
        System.out.print("┌");
        System.out.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            System.out.print("┬");
            System.out.print("─".repeat(columnWidths.get(symbol)));
        }
        System.out.println("┐");

        // Print header
        System.out.print("│");
        System.out.printf(" %-" + (stateWidth - 1) + "s│", "State");
        for (String symbol : allSymbols) {
            System.out.printf(" %-" + (columnWidths.get(symbol) - 1) + "s│", symbol);
        }
        System.out.println();

        // Print separator after header
        System.out.print("├");
        System.out.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            System.out.print("┼");
            System.out.print("─".repeat(columnWidths.get(symbol)));
        }
        System.out.println("┤");

        // Print rows
        for (int state = 0; state < canonicalCollection.size(); state++) {
            System.out.print("│");
            System.out.printf(" %-" + (stateWidth - 1) + "s│", state);

            for (String symbol : allSymbols) {
                String cellStr;
                if (terminals.contains(symbol)) {
                    // Check if there's a conflict for this state/symbol
                    if (conflictTable.containsKey(state) && conflictTable.get(state).containsKey(symbol)) {
                        List<Action> conflictingActions = conflictTable.get(state).get(symbol);
                        List<String> actionStrs = new ArrayList<>();
                        for (Action action : conflictingActions) {
                            if (action.type != Action.Type.ERROR) {
                                actionStrs.add(action.toString());
                            }
                        }
                        cellStr = String.join(" / ", actionStrs);
                    } else {
                        Action action = getAction(state, symbol);
                        if (action.type == Action.Type.ERROR) {
                            cellStr = "-";
                        } else {
                            cellStr = action.toString();
                        }
                    }
                } else {
                    Integer gotoState = getGoto(state, symbol);
                    cellStr = gotoState != null ? String.valueOf(gotoState) : "-";
                }
                System.out.printf(" %-" + (columnWidths.get(symbol) - 1) + "s│", cellStr);
            }
            System.out.println();
        }

        // Print bottom border
        System.out.print("└");
        System.out.print("─".repeat(stateWidth));
        for (String symbol : allSymbols) {
            System.out.print("┴");
            System.out.print("─".repeat(columnWidths.get(symbol)));
        }
        System.out.println("┘");

        // Print summary
        String tableType = useLookaheads ? "LR(1)" : "SLR(1)";
        System.out.println("Parser Type: " + tableType);
        System.out.println("Is " + tableType + " Parseable: " + (isSlrParseable ? "YES" : "NO"));
        System.out.println("Conflicts Found: " + conflicts.size());

        if (!conflicts.isEmpty()) {
            System.out.println("\nConflicts ");
            for (Conflict conflict : conflicts) {
                System.out.println(conflict);
            }
        }
        System.out.println();

        return isSlrParseable;
    }

    /**
     * Prints the parsing table (calls printParsingTable)
     */
    public void print() {
        printParsingTable();
    }

    /**
     * Gets the action table (for file output and analysis)
     */
    public Map<Integer, Map<String, Action>> getActionTable() {
        return new HashMap<>(actionTable);
    }

    /**
     * Gets the goto table (for file output and analysis)
     */
    public Map<Integer, Map<String, Integer>> getGotoTable() {
        return new HashMap<>(gotoTable);
    }


    /*
    for output files
    */


}
