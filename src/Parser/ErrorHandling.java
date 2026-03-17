/// ///////////// I DONT THINK THIS FILE IS USED ANYMORE, BUT I DONT WANT TO DELETE IT JUST IN CASE /////////////

package Parser;

import java.util.*;

/**
 * Task 2.4: Error Handling & Recovery for LL(1) Parser
 * Implements error detection, panic mode recovery, and detailed error reporting
 */
public class ErrorHandling {

    // Error types as per requirements
    public enum ErrorType {
        MISSING_SYMBOL("Missing Symbol"),
        UNEXPECTED_SYMBOL("Unexpected Symbol"),
        EMPTY_TABLE_ENTRY("No Production in Parsing Table"),
        PREMATURE_END("Premature End of Input");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Error class to store detailed error information
    public static class ParseError {
        private ErrorType type;
        private String message;
        private int line;
        private int column;
        private String expected;
        private String found;
        private String context;
        private boolean recovered;

        public ParseError(ErrorType type, String message, int line, int column) {
            this.type = type;
            this.message = message;
            this.line = line;
            this.column = column;
            this.recovered = false;
        }

        // Getters and setters
        public ErrorType getType() { return type; }
        public String getMessage() { return message; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public String getExpected() { return expected; }
        public String getFound() { return found; }
        public String getContext() { return context; }
        public boolean isRecovered() { return recovered; }

        public void setExpected(String expected) { this.expected = expected; }
        public void setFound(String found) { this.found = found; }
        public void setContext(String context) { this.context = context; }
        public void setRecovered(boolean recovered) { this.recovered = recovered; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("[Line %d, Column %d] ", line, column));
            sb.append(type.getDescription()).append(": ").append(message);

            if (expected != null && found != null) {
                sb.append(String.format("\n  Expected: %s", expected));
                sb.append(String.format("\n  Found: %s", found));
            }

            if (context != null) {
                sb.append(String.format("\n  Context: %s", context));
            }

            if (recovered) {
                sb.append("\n  → Recovered from error");
            }

            return sb.toString();
        }

        public String toShortString() {
            return String.format("Line %d, Col %d: %s", line, column, message);
        }
    }

    // Panic Mode Recovery implementation
    public static class PanicModeRecovery {
        private Map<String, Set<String>> followSets;
        private List<ParseError> errors;
        private boolean recoveryPerformed;

        public PanicModeRecovery(Map<String, Set<String>> followSets) {
            this.followSets = followSets;
            this.errors = new ArrayList<>();
            this.recoveryPerformed = false;
        }

        /**
         * Attempt to recover from error using panic mode
         * @param stack The parser stack
         * @param input The input list with $ terminator
         * @param ip Current input pointer (mutable wrapper)
         * @param currentNonTerminal The non-terminal being processed when error occurred
         * @param lineNumber Current line number
         * @param column Current column
         * @return true if recovery successful, false otherwise
         */
        public boolean recover(Stack stack, List<String> input, MutableInt ip,
                               String currentNonTerminal, int lineNumber, int column) {
            recoveryPerformed = false;

            // Get FOLLOW set of current non-terminal for synchronizing
            Set<String> syncSet = followSets.getOrDefault(currentNonTerminal, new HashSet<>());
            syncSet.add("$"); // Always add $ as synchronizing symbol

            // Record error context before recovery
            ParseError error = new ParseError(
                    ErrorType.EMPTY_TABLE_ENTRY,
                    String.format("No production for %s with lookahead '%s'",
                            currentNonTerminal, input.get(ip.getValue())),
                    lineNumber,
                    column
            );
            error.setExpected("One of: " + syncSet);
            error.setFound(input.get(ip.getValue()));
            error.setContext(String.format("Stack: %s, Input: %s",
                    stack.toDisplayString(),
                    getRemainingInput(input, ip.getValue())));
            errors.add(error);

            System.out.println("\n PANIC MODE RECOVERY TRIGGERED");
            System.out.println("   " + error.toShortString());
            System.out.println("   Synchronizing set: " + syncSet);

            // Step 1: Pop stack until we find a symbol whose FOLLOW set contains current input
            System.out.print("   Popping stack: ");
            while (!stack.isEmpty() && !stack.top().equals("$")) {
                String top = stack.top();
                Set<String> topFollow = followSets.getOrDefault(top, new HashSet<>());

                if (topFollow.contains(input.get(ip.getValue()))) {
                    System.out.println("Found synchronizing symbol '" + top + "'");
                    break;
                }

                String popped = stack.pop();
                System.out.print(popped + " ");
            }
            System.out.println();

            // Step 2: Skip input symbols until we find a synchronizing symbol
            System.out.print("   Skipping input: ");
            while (ip.getValue() < input.size()) {
                String currentInput = input.get(ip.getValue());
                if (syncSet.contains(currentInput) || currentInput.equals("$")) {
                    System.out.println("Found synchronizing symbol '" + currentInput + "'");
                    break;
                }
                System.out.print(currentInput + " ");
                ip.increment();
            }
            System.out.println();

            // If we found a synchronizing symbol, recovery was successful
            if (ip.getValue() < input.size() &&
                    (syncSet.contains(input.get(ip.getValue())) || input.get(ip.getValue()).equals("$"))) {
                recoveryPerformed = true;
                error.setRecovered(true);
                System.out.println("   Recovery successful, resuming parsing...\n");
                return true;
            }

            System.out.println("   Recovery failed, cannot resume parsing\n");
            return false;
        }

        public List<ParseError> getErrors() { return errors; }
        public boolean isRecoveryPerformed() { return recoveryPerformed; }
    }

    // Error Production Method for common mistakes
    public static class ErrorProductionRecovery {
        private Map<String, Map<String, List<String>>> errorProductions;
        private List<ParseError> errors;

        public ErrorProductionRecovery() {
            this.errorProductions = new HashMap<>();
            this.errors = new ArrayList<>();
            initializeErrorProductions();
        }

        /**
         * Initialize common error productions for typical mistakes
         */
        private void initializeErrorProductions() {
            // Example: Missing operand (id + * id) - insert id
            Map<String, List<String>> exprProductions = new HashMap<>();
            List<String> insertId = new ArrayList<>();
            insertId.add("id");
            insertId.add("@"); // Special marker for error recovery
            exprProductions.put("+", insertId);

            errorProductions.put("Expr", exprProductions);

            // Add more error productions as needed
        }

        /**
         * Check if there's an error production for this situation
         */
        public List<String> getErrorProduction(String nonTerminal, String lookahead) {
            if (errorProductions.containsKey(nonTerminal)) {
                return errorProductions.get(nonTerminal).get(lookahead);
            }
            return null;
        }

        /**
         * Handle error by inserting/deleting symbols
         */
        public boolean handleError(String nonTerminal, String lookahead,
                                   List<String> input, MutableInt ip,
                                   int lineNumber, int column) {

            List<String> errorProd = getErrorProduction(nonTerminal, lookahead);

            if (errorProd != null) {
                ParseError error = new ParseError(
                        ErrorType.MISSING_SYMBOL,
                        String.format("Missing operand near '%s'", lookahead),
                        lineNumber,
                        column
                );
                error.setExpected("id");
                error.setFound(lookahead);
                error.setContext("Inserting missing 'id'");
                error.setRecovered(true);
                errors.add(error);

                System.out.println("\n ERROR PRODUCTION RECOVERY");
                System.out.println("   " + error.toShortString());
                System.out.println("   Inserting missing 'id'");

                // Insert missing id into input
                input.add(ip.getValue(), "id");
                return true;
            }

            return false;
        }

        public List<ParseError> getErrors() { return errors; }
    }

    // Utility class for mutable integer (to pass by reference)
    public static class MutableInt {
        private int value;

        public MutableInt(int value) {
            this.value = value;
        }

        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
        public void increment() { value++; }
        public void decrement() { value--; }
    }

    // Helper method to get remaining input for display
    private static String getRemainingInput(List<String> input, int ip) {
        if (ip >= input.size()) {
            return "";
        }
        return String.join(" ", input.subList(ip, input.size()));
    }

    // Comprehensive error reporter
    public static class ErrorReporter {
        private List<ParseError> allErrors;
        private boolean hasErrors;

        public ErrorReporter() {
            this.allErrors = new ArrayList<>();
            this.hasErrors = false;
        }

        public void addError(ParseError error) {
            allErrors.add(error);
            hasErrors = true;
        }

        public void addAllErrors(List<ParseError> errors) {
            allErrors.addAll(errors);
            if (!errors.isEmpty()) {
                hasErrors = true;
            }
        }

        public void printErrorSummary() {
            if (!hasErrors) {
                System.out.println("\n No errors detected.");
                return;
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("ERROR SUMMARY");
            System.out.println("=".repeat(60));

            // Group errors by type
            Map<ErrorType, List<ParseError>> errorsByType = new HashMap<>();
            for (ParseError error : allErrors) {
                errorsByType.computeIfAbsent(error.getType(), k -> new ArrayList<>()).add(error);
            }

            // Print summary statistics
            System.out.printf("Total Errors: %d\n", allErrors.size());
            for (Map.Entry<ErrorType, List<ParseError>> entry : errorsByType.entrySet()) {
                System.out.printf("  %s: %d\n", entry.getKey().getDescription(), entry.getValue().size());
            }

            // Print detailed errors
            System.out.println("\n" + "-".repeat(60));
            System.out.println("DETAILED ERROR REPORT");
            System.out.println("-".repeat(60));

            for (int i = 0; i < allErrors.size(); i++) {
                System.out.printf("\nError #%d:\n%s\n", i + 1, allErrors.get(i).toString());
            }
        }

        public void printErrorStatistics() {
            int recovered = 0;
            int fatal = 0;

            for (ParseError error : allErrors) {
                if (error.isRecovered()) {
                    recovered++;
                } else {
                    fatal++;
                }
            }

            System.out.println("\nError Statistics:");
            System.out.printf("  Total: %d\n", allErrors.size());
            System.out.printf("  Recovered: %d\n", recovered);
            System.out.printf("  Fatal: %d\n", fatal);
        }

        public boolean hasErrors() { return hasErrors; }
        public List<ParseError> getAllErrors() { return allErrors; }
    }

    // Test method to demonstrate error handling
    public static void main(String[] args) {
        System.out.println("=== Testing Error Handling ===\n");

        // Create sample FOLLOW sets for testing
        Map<String, Set<String>> sampleFollow = new HashMap<>();
        sampleFollow.put("Expr", new HashSet<>(Arrays.asList("$", ")")));
        sampleFollow.put("Term", new HashSet<>(Arrays.asList("+", "$", ")")));
        sampleFollow.put("Factor", new HashSet<>(Arrays.asList("*", "+", "$", ")")));

        // Test Panic Mode Recovery
        System.out.println("Test 1: Panic Mode Recovery");
        PanicModeRecovery panicRecovery = new PanicModeRecovery(sampleFollow);

        // Simulate error scenario
        Stack testStack = new Stack();
        testStack.initialize("Expr");
        testStack.push("Term");

        List<String> testInput = new ArrayList<>(Arrays.asList("*", "id", "+", "id", "$"));
        MutableInt ip = new MutableInt(0);

        boolean recovered = panicRecovery.recover(testStack, testInput, ip, "Term", 1, 5);
        System.out.println("Recovery successful: " + recovered);

        // Print errors
        ErrorReporter reporter = new ErrorReporter();
        reporter.addAllErrors(panicRecovery.getErrors());
        reporter.printErrorSummary();

        // Test Error Production Recovery
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Test 2: Error Production Recovery");
        ErrorProductionRecovery prodRecovery = new ErrorProductionRecovery();

        List<String> testInput2 = new ArrayList<>(Arrays.asList("+", "*", "id", "$"));
        MutableInt ip2 = new MutableInt(0);

        boolean handled = prodRecovery.handleError("Expr", "+", testInput2, ip2, 2, 3);
        System.out.println("Error handled: " + handled);
        System.out.println("Input after handling: " + testInput2);
    }
}