package BottomUpParser.src;

import java.util.*;

/**
 * SLR(1) Parser - implements the SLR parsing algorithm
 * Uses the parsing table (ACTION and GOTO) to parse input tokens
 */
public class SLRParser {

    private ParsingTable parsingTable;
    private Grammar grammar;
    private List<String> inputTokens;
    private Stack stack;
    private int inputPointer;
    private List<ParsingStep> parsingTrace;

    //for tree
    private Tree parseTree;
    private boolean buildTree;

    /**
     * Represents a single step in the parsing process
     */
    public static class ParsingStep {
        public int stepNumber;
        public String stack;
        public String input;
        public String action;

        public ParsingStep(int stepNumber, String stack, String input, String action) {
            this.stepNumber = stepNumber;
            this.stack = stack;
            this.input = input;
            this.action = action;
        }

        @Override
        public String toString() {
            return String.format("%-8d %-30s %-20s %s", stepNumber, stack, input, action);
        }
    }

    /**
     * Constructor
     * @param parsingTable the SLR(1) parsing table
     * @param grammar the augmented grammar
     */
    public SLRParser(ParsingTable parsingTable, Grammar grammar) {
        this(parsingTable, grammar, true);
    }

    public SLRParser(ParsingTable parsingTable, Grammar grammar, boolean buildTree) {
        this.parsingTable = parsingTable;
        this.grammar = grammar;
        this.stack = new Stack();
        this.parsingTrace = new ArrayList<>();
        this.buildTree = buildTree;
        if (buildTree) {
            this.parseTree = new Tree();
        }
    }

    /**
     * Parse a list of input tokens
     * @param tokens list of input tokens (without $)
     * @return true if parsing succeeds, false otherwise
     */
    public boolean parse(List<String> tokens) {
        // Append $ to input
        this.inputTokens = new ArrayList<>(tokens);
        this.inputTokens.add("$");
        this.inputPointer = 0;
        this.parsingTrace.clear();

        // Initialize stack with $0 (bottom marker and state 0)
        this.stack = new Stack();
        this.stack.push("0");  // Just push state 0, $ is implicit as bottom marker

        if (buildTree) {
            parseTree.clear();
            // Get original start symbol (remove the ' from augmented grammar)
            String startSymbol = grammar.getStartSymbol();
            if (startSymbol.endsWith("'")) {
                startSymbol = startSymbol.substring(0, startSymbol.length() - 1);
            }
            parseTree.init(startSymbol);
        }

        int stepCounter = 0;

        // Parse
        while (true) {
            stepCounter++;
            int currentState = Integer.parseInt(stack.top());
            String currentSymbol = inputTokens.get(inputPointer);

            // Look up action
            ParsingTable.Action action = parsingTable.getAction(currentState, currentSymbol);

            if (action.type == ParsingTable.Action.Type.SHIFT) {
                // Shift action: push symbol, then push next state
                int nextState = action.targetState;
                stack.push(currentSymbol);
                stack.push(String.valueOf(nextState));
                if (buildTree && !currentSymbol.equals("$")) {
                    parseTree.shift(currentSymbol);
                }
                inputPointer++;
                recordStep(stepCounter, "shift", nextState);

            } else if (action.type == ParsingTable.Action.Type.REDUCE) {
                // Reduce action: pop 2*|rhs| symbols, get new state, push lhs and new state
                String production = action.production;
                String[] parts = production.split(" -> ");
                String lhs = parts[0].trim();
                String rhs = parts[1].trim();
                List<String> rhsSymbols = rhs.equals("@") ? Collections.emptyList() : Arrays.asList(rhs.split(" "));

                // Pop 2 * |rhs| symbols (each symbol has a state below it)
                int popCount = 2 * rhsSymbols.size();
                for (int i = 0; i < popCount; i++) {
                    stack.pop();
                }

                // Get new state (top of stack after popping)
                int newState = Integer.parseInt(stack.top());

                // Get goto state from parsing table
                Integer gotoState = parsingTable.getGoto(newState, lhs);
                if (gotoState == null) {
                    recordStep(stepCounter, "ERROR: no goto for " + lhs + " in state " + newState);
                    return false;
                }

                // Push LHS and new state
                stack.push(lhs);
                if (buildTree) {
                    parseTree.reduce(lhs, rhsSymbols, production);
                }
                stack.push(String.valueOf(gotoState));
                recordStep(stepCounter, "reduce", production, gotoState);

            } else if (action.type == ParsingTable.Action.Type.ACCEPT) {
                recordStep(stepCounter, "accept");
                if (buildTree) {
                    parseTree.finalizeTree();
                }
                return true;

            } else {
                // Error
                recordStep(stepCounter, "ERROR: no action for (" + currentState + ", " + currentSymbol + ")");
                return false;
            }
        }
    }

    /**
     * Record a parsing step for shift actions
     */
    private void recordStep(int stepNumber, String actionType, int state) {
        String stackStr = stackToString();
        String inputStr = inputToString();
        String actionDesc = actionType + " " + state;
        parsingTrace.add(new ParsingStep(stepNumber, stackStr, inputStr, actionDesc));
    }

    /**
     * Record a parsing step for reduce actions
     */
    private void recordStep(int stepNumber, String actionType, String production, int gotoState) {
        String stackStr = stackToString();
        String inputStr = inputToString();
        String actionDesc = actionType + " " + production ;
        parsingTrace.add(new ParsingStep(stepNumber, stackStr, inputStr, actionDesc));
    }

    /**
     * Record a parsing step (accept or error)
     */
    private void recordStep(int stepNumber, String actionDesc) {
        String stackStr = stackToString();
        String inputStr = inputToString();
        parsingTrace.add(new ParsingStep(stepNumber, stackStr, inputStr, actionDesc));
    }

    /**
     * Convert stack to string format showing as $ followed by alternating state/symbol
     * Format: $0 [symbol state]*
     */
    private String stackToString() {
        List<String> contents = stack.getContents();
        if (contents.isEmpty()) {
            return "$";
        }

        // Build string with $ at beginning
        StringBuilder sb = new StringBuilder("$");
        for (int i = 0; i < contents.size(); i++) {
            sb.append(contents.get(i));
        }
        return sb.toString();
    }

    /**
     * Convert remaining input to string format
     */
    private String inputToString() {
        if (inputPointer >= inputTokens.size()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = inputPointer; i < inputTokens.size(); i++) {
            if (i > inputPointer) {
                sb.append(" ");
            }
            sb.append(inputTokens.get(i));
        }
        return sb.toString();
    }

    /**
     * Print the parsing trace in a formatted table
     */
    public void printParsingTrace() {
        System.out.println("SLR Parsing Trace: ");
        System.out.println(String.format("%-8s %-30s %-20s %s", "Step", "Stack", "Input", "Action"));
        System.out.println("-".repeat(80));

        for (ParsingStep step : parsingTrace) {
            System.out.println(step);
        }

        System.out.println();
    }

    /**
     * Get parsing trace
     */
    public List<ParsingStep> getParsingTrace() {
        return new ArrayList<>(parsingTrace);
    }


    public void printParseTree() {
        if (buildTree && parseTree != null) {
            parseTree.printFull();
        }
    }

    public Tree getParseTree() { return parseTree; }

    /*
    for output files
     */

}