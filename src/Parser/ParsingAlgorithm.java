package Parser;

import java.util.*;

/**
 * Task 2.3: LL(1) Parsing Algorithm Implementation
 * Implements the stack-based LL(1) parser with step-by-step display
 */
public class ParsingAlgorithm {

    private Map<String, Map<String, List<String>>> parsingTable;
    private Map<String, Set<String>> followSets;
    private String startSymbol;
    private List<ParseStep> steps;
    private boolean debug;

    /**
     * Constructor for ParsingAlgorithm
     * @param tableConstructor The LL(1) parsing table constructor
     * @param followSets FOLLOW sets for error handling
     */
    public ParsingAlgorithm(LL1ParsingTableConstructor tableConstructor, Map<String, Set<String>> followSets) {
        this.parsingTable = tableConstructor.getParsingTable();
        this.followSets = followSets;
        this.startSymbol = tableConstructor.getStartSymbol();
        this.steps = new ArrayList<>();
        this.debug = true; // Set to false to disable step-by-step printing
    }

    /**
     * Parse a single input string
     * @param input List of tokens to parse
     * @param lineNumber Line number in input file (for error reporting)
     * @return ParseResult containing steps, success status, and any errors
     */
    public ParseResult parse(List<String> input, int lineNumber) {
        steps.clear();
        List<ParseError> errors = new ArrayList<>();
        int stepNumber = 0;
        boolean success = false;

        // Make a working copy of input and add $ terminator
        List<String> inputWithDollar = new ArrayList<>(input);
        inputWithDollar.add("$");

        // Step 1: Initialize stack with $ and start symbol
        stack parserStack = new stack();
        parserStack.initialize(startSymbol);

        // Initialize input pointer
        int ip = 0;

        // Record initial state
        addStep(++stepNumber, parserStack.copy(),
                getRemainingInput(inputWithDollar, ip),
                "Initialize stack with $ and " + startSymbol);

        // Parsing loop - add check for empty stack
        while (!parserStack.isEmpty()) {
            // Get current top of stack and current input symbol
            String X = parserStack.top();
            String a = inputWithDollar.get(ip);

            // Case 1: X = a = $ → Success
            if (X.equals("$") && a.equals("$")) {
                addStep(++stepNumber, parserStack.copy(),
                        getRemainingInput(inputWithDollar, ip),
                        " ACCEPT: Input successfully parsed!");
                success = true;
                break;
            }

            // Case 2: X = a (terminal match)
            if (X.equals(a)) {
                parserStack.pop();
                ip++;

                // Check if stack becomes empty after pop (should not happen except at end)
                if (parserStack.isEmpty() && !a.equals("$")) {
                    ParseError error = createError("Stack became empty prematurely", lineNumber, ip,
                            ParseError.ErrorType.PREMATURE_END, "$", a);
                    errors.add(error);
                    addStep(++stepNumber, parserStack.copy(),
                            getRemainingInput(inputWithDollar, ip),
                            "ERROR: Stack empty - " + error.getMessage());
                    break;
                }

                addStep(++stepNumber, parserStack.copy(),
                        getRemainingInput(inputWithDollar, ip),
                        "Match: '" + a + "'");
                continue;
            }

            // Case 3: X is non-terminal
            if (parsingTable.containsKey(X)) {
                Map<String, List<String>> row = parsingTable.get(X);
                List<String> production = row.get(a);

                // If no production in table → ERROR
                if (production == null) {
                    String errorMsg = String.format("No production for %s with lookahead '%s'", X, a);
                    ParseError error = createError(errorMsg, lineNumber, ip + 1,
                            ParseError.ErrorType.EMPTY_TABLE_ENTRY,
                            "Expected one of: " + row.keySet(), a);
                    errors.add(error);

                    addStep(++stepNumber, parserStack.copy(),
                            getRemainingInput(inputWithDollar, ip),
                            "ERROR: " + errorMsg);

                    // Attempt panic mode recovery
                    if (!panicModeRecovery(parserStack, inputWithDollar, ip)) {
                        addStep(++stepNumber, parserStack.copy(),
                                getRemainingInput(inputWithDollar, ip),
                                "ERROR: Cannot recover - terminating parse");
                        break; // Can't recover
                    }

                    // After recovery, continue with next iteration
                    continue;
                }

                // Pop X from stack
                parserStack.pop();

                // Action description
                String action;

                // If production is ε (empty), don't push anything
                if (production.isEmpty() || (production.size() == 1 && production.get(0).equals("@"))) {
                    action = String.format("Expand %s -> ε", X);
                } else {
                    // Push production in reverse order
                    parserStack.pushAll(production);
                    action = String.format("Expand %s  -> %s", X, String.join("", production));
                }

                addStep(++stepNumber, parserStack.copy(),
                        getRemainingInput(inputWithDollar, ip),
                        action);
                continue;
            }

            // Case 4: X is terminal but X != a  -> ERROR
            String errorMsg = String.format("Expected '%s' but found '%s'", X, a);
            ParseError error = createError(errorMsg, lineNumber, ip + 1,
                    ParseError.ErrorType.UNEXPECTED_SYMBOL, X, a);
            errors.add(error);

            addStep(++stepNumber, parserStack.copy(),
                    getRemainingInput(inputWithDollar, ip),
                    "ERROR: " + errorMsg);

            // Try to recover by skipping input until matching token
            if (!skipUntilMatch(parserStack, inputWithDollar, ip)) {
                addStep(++stepNumber, parserStack.copy(),
                        getRemainingInput(inputWithDollar, ip),
                        "ERROR: Cannot recover - terminating parse");
                break;
            }
        }

        // Check for premature end or other errors
        if (!success) {
            if (ip < inputWithDollar.size() - 1) {
                ParseError error = createError("Input ended prematurely", lineNumber, ip + 1,
                        ParseError.ErrorType.PREMATURE_END,
                        parserStack.isEmpty() ? "$" : parserStack.top(),
                        ip < inputWithDollar.size() ? inputWithDollar.get(ip) : "EOF");
                errors.add(error);
                addStep(++stepNumber, parserStack.copy(),
                        getRemainingInput(inputWithDollar, ip),
                        "ERROR: " + error.getMessage());
            } else if (!parserStack.isEmpty() && !parserStack.onlyDollar()) {
                ParseError error = createError("Stack not empty at end of input", lineNumber, ip + 1,
                        ParseError.ErrorType.PREMATURE_END, "$",
                        parserStack.top());
                errors.add(error);
                addStep(++stepNumber, parserStack.copy(),
                        getRemainingInput(inputWithDollar, ip),
                        "ERROR: " + error.getMessage());
            }
        }

        return new ParseResult(success, steps, errors, String.join(" ", input), lineNumber);
    }

    /**
     * Panic mode recovery: pop stack until synchronizing symbol found
     * @param stack The parser stack
     * @param input Input with $ terminator
     * @param ip Current input pointer
     * @return true if recovery possible, false otherwise
     */
    private boolean panicModeRecovery(stack stack, List<String> input, int ip) {
        if (stack.isEmpty()) {
            System.out.println("  Panic recovery: Stack is empty, cannot recover");
            return false;
        }

        String X = stack.top();
        Set<String> follow = followSets.get(X);

        if (follow == null || follow.isEmpty()) {
            System.out.println("   Panic recovery: No FOLLOW set for " + X);
            return false;
        }

        System.out.println("   Panic mode recovery: Looking for synchronizing symbol in " + follow);
        int poppedCount = 0;

        // Pop stack until we find a symbol whose FOLLOW set contains current input
        while (!stack.isEmpty() && !stack.top().equals("$")) {
            String top = stack.top();
            Set<String> topFollow = followSets.get(top);

            if (topFollow != null && topFollow.contains(input.get(ip))) {
                System.out.println("   Found synchronizing symbol '" + top + "', popped " + poppedCount + " symbols");
                return true; // Found synchronizing symbol
            }
            String popped = stack.pop();
            poppedCount++;
            System.out.println("   Popped: " + popped);
        }

        if (stack.isEmpty()) {
            System.out.println("   Panic recovery: Stack became empty, re-initializing with start symbol");
            stack.initialize(startSymbol);
            return true;
        }

        return !stack.isEmpty();
    }

    /**
     * Skip input tokens until a match is found
     * @param stack The parser stack
     * @param input Input with $ terminator
     * @param ip Current input pointer (passed by value, so we return new position)
     * @return true if recovery possible, false otherwise
     */
    private boolean skipUntilMatch(stack stack, List<String> input, int ip) {
        if (stack.isEmpty()) {
            return false;
        }

        String expected = stack.top();
        int skipped = 0;

        System.out.println("   Skipping input until '" + expected + "' is found");

        while (ip < input.size() && !input.get(ip).equals(expected)) {
            System.out.println("   Skipping: " + input.get(ip));
            ip++;
            skipped++;
        }

        if (ip < input.size() && input.get(ip).equals(expected)) {
            System.out.println("  Found '" + expected + "', skipped " + skipped + " tokens");
            stack.pop();
            return true;
        }

        System.out.println("   Could not find '" + expected + "', recovery failed");
        return false;
    }

    /**
     * Create a ParseError object
     */
    private ParseError createError(String message, int line, int column,
                                   ParseError.ErrorType type, String expected, String found) {
        ParseError error = new ParseError();
        error.setMessage(message);
        error.setLine(line);
        error.setColumn(column); // +1 is now handled in the caller
        error.setType(type);
        error.setExpected(expected);
        error.setFound(found);
        return error;
    }

    /**
     * Get remaining input as a string for display
     */
    private String getRemainingInput(List<String> input, int ip) {
        if (ip >= input.size()) {
            return "";
        }
        return String.join(" ", input.subList(ip, input.size()));
    }

    /**
     * Add a parsing step
     */
    private void addStep(int stepNumber, stack currentStack, String remainingInput, String action) {
        ParseStep step = new ParseStep(stepNumber, currentStack, remainingInput, action);
        steps.add(step);

        if (debug) {
            System.out.println(step.getFormattedStep());
        }
    }

    /**
     * Parse multiple inputs
     * @param inputs List of input token lists
     * @return List of parse results
     */
    public List<ParseResult> parseAll(List<List<String>> inputs) {
        List<ParseResult> results = new ArrayList<>();
        int lineNumber = 1;

        for (List<String> input : inputs) {
            // Skip comments and empty lines
            if (input.isEmpty() || input.get(0).startsWith("#")) {
                lineNumber++;
                continue;
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("Parsing: " + String.join(" ", input));
            System.out.println("=".repeat(60));

            ParseResult result = parse(input, lineNumber);
            results.add(result);
            result.printResult();

            lineNumber++;
        }

        return results;
    }

    /**
     * Inner class to represent a parsing step
     */
    public static class ParseStep {
        private int stepNumber;
        private String stackContents;
        private String remainingInput;
        private String action;

        public ParseStep(int stepNumber, stack stack, String remainingInput, String action) {
            this.stepNumber = stepNumber;
            this.stackContents = stack.toDisplayString();
            this.remainingInput = remainingInput;
            this.action = action;
        }

        public String getFormattedStep() {
            return String.format("%-5d | %-30s | %-20s | %s",
                    stepNumber, stackContents, remainingInput, action);
        }

        // Getters
        public int getStepNumber() { return stepNumber; }
        public String getStackContents() { return stackContents; }
        public String getRemainingInput() { return remainingInput; }
        public String getAction() { return action; }
    }

    /**
     * Inner class to represent a parse result
     */
    public static class ParseResult {
        private boolean success;
        private List<ParseStep> steps;
        private List<ParseError> errors;
        private String inputString;
        private int lineNumber;

        public ParseResult(boolean success, List<ParseStep> steps, List<ParseError> errors,
                           String inputString, int lineNumber) {
            this.success = success;
            this.steps = steps;
            this.errors = errors;
            this.inputString = inputString;
            this.lineNumber = lineNumber;
        }

        public void printResult() {
            System.out.println("\n" + "-".repeat(80));
            System.out.println("PARSE RESULT for line " + lineNumber + ": " + inputString);
            System.out.println("-".repeat(80));

            if (success) {
                System.out.println("ACCEPT");
            } else {
                System.out.println("REJECT");
            }

            System.out.println("\nParsing Steps:");
            System.out.println("Step  | Stack Contents                   | Remaining Input      | Action");
            System.out.println("-".repeat(80));

            for (ParseStep step : steps) {
                System.out.println(step.getFormattedStep());
            }

            if (!errors.isEmpty()) {
                System.out.println("\nErrors:");
                for (ParseError error : errors) {
                    System.out.println("  " + error.getMessage());
                    if (error.getExpected() != null && error.getFound() != null) {
                        System.out.println("    Expected: " + error.getExpected());
                        System.out.println("    Found: " + error.getFound());
                    }
                }
            }

            System.out.println("-".repeat(80));
        }

        // Getters
        public boolean isSuccess() { return success; }
        public List<ParseStep> getSteps() { return steps; }
        public List<ParseError> getErrors() { return errors; }
        public String getInputString() { return inputString; }
        public int getLineNumber() { return lineNumber; }
    }

    /**
     * Inner class to represent a parse error
     */
    public static class ParseError {
        public enum ErrorType {
            MISSING_SYMBOL,
            UNEXPECTED_SYMBOL,
            EMPTY_TABLE_ENTRY,
            PREMATURE_END
        }

        private String message;
        private int line;
        private int column;
        private ErrorType type;
        private String expected;
        private String found;

        // Setters
        public void setMessage(String message) { this.message = message; }
        public void setLine(int line) { this.line = line; }
        public void setColumn(int column) { this.column = column; }
        public void setType(ErrorType type) { this.type = type; }
        public void setExpected(String expected) { this.expected = expected; }
        public void setFound(String found) { this.found = found; }

        // Getters
        public String getMessage() { return message; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public ErrorType getType() { return type; }
        public String getExpected() { return expected; }
        public String getFound() { return found; }
    }
}