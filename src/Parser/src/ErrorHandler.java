package Parser.src;

import java.util.*;

public class ErrorHandler {

    private Map<String, Map<String, List<String>>> parsingTable;
    private Map<String, Set<String>> followSets;
    private List<Parser.ParseStep> steps;
    private int stepNumber;

    public ErrorHandler(Map<String, Map<String, List<String>>> parsingTable,
                        Map<String, Set<String>> followSets,
                        List<Parser.ParseStep> steps,
                        int stepNumber) {
        this.parsingTable = parsingTable;
        this.followSets = followSets;
        this.steps = steps;
        this.stepNumber = stepNumber;
    }

    public int getStepNumber() { return stepNumber; }

    // Empty Table Entry: NT on stack with no production for current token — scan or pop to recover
    // Missing Symbol: expected terminal not found in input — skip bad token or pop if end of input
    // Unexpected Symbol: terminal on stack but different terminal in input — skip or pop to recover
    // Premature End: input exhausted but stack still has symbols — pop remaining stack
    public ParseError classifyError(String X, String a, int lineNumber,
                                    int ip, Map<String, List<String>> row) {
        String errorMsg;
        ParseError.ErrorType type;
        String expected;

        if (row != null) {
            // Empty Table Entry: NT on stack with no production for current token
            List<String> validTerminals = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : row.entrySet()) {
                if (entry.getValue() != null) validTerminals.add(entry.getKey());
            }
            errorMsg = String.format(
                    "Empty Table Entry: No production in M[%s, '%s']", X, a);
            type = ParseError.ErrorType.EMPTY_TABLE_ENTRY;
            expected = "Expected one of: " + validTerminals;

        } else if (a.equals("$")) {
            // Premature End: input exhausted but stack still has symbols
            errorMsg = String.format(
                    "Premature End: Input ends but stack not empty, expected '%s'", X);
            type = ParseError.ErrorType.PREMATURE_END;
            expected = X;

        } else if (parsingTable.containsKey(a)) {
            // Unexpected Symbol: terminal on stack but non-terminal in input
            errorMsg = String.format(
                    "Unexpected Symbol: '%s' appears where terminal '%s' expected", a, X);
            type = ParseError.ErrorType.UNEXPECTED_SYMBOL;
            expected = X;

        } else {
            // Missing Symbol: expected terminal not found in input
            errorMsg = String.format(
                    "Missing Symbol: Expected terminal '%s' but found '%s'", X, a);
            type = ParseError.ErrorType.MISSING_SYMBOL;
            expected = X;
        }

        return createError(errorMsg, lineNumber, ip + 1, type, expected, a);
    }

    /**
     * Unified recovery loop — scan or pop until resynchronized
     * while M[X, a] is empty:
     *   if a == $ or a ∈ Follow(X): pop X
     *   else: scan (skip a)
     */
    public boolean recover(Stack stack, List<String> input, int[] ip,
                           int[] stepRef, int lineNumber, Tree tree) {

        while (!stack.isEmpty() && !stack.top().equals("$")) {
            String X = stack.top();
            String a = input.get(ip[0]);

            // Resynchronized — valid table entry exists for NT
            if (parsingTable.containsKey(X) && parsingTable.get(X).get(a) != null) {
                return true;
            }

            // Resynchronized — terminal on stack matches input
            if (!parsingTable.containsKey(X) && X.equals(a)) {
                return true;
            }

            Set<String> followX = followSets.get(X);
            boolean inFollow = followX != null && followX.contains(a);

            if (a.equals("$") || inFollow) {
                String popped = stack.pop();
                tree.popError();
                addStep(++stepRef[0], stack.copy(),
                        getRemainingInput(input, ip[0]),
                        "Recovery: popped " + popped);
            } else {
                tree.skipToken(a);
                addStep(++stepRef[0], stack.copy(),
                        getRemainingInput(input, ip[0]),
                        "Recovery: skipping '" + a + "'");
                ip[0]++;
            }
        }
        return false;
    }

    private ParseError createError(String message, int line, int column,
                                   ParseError.ErrorType type,
                                   String expected, String found) {
        ParseError error = new ParseError();
        error.setMessage(message);
        error.setLine(line);
        error.setColumn(column);
        error.setType(type);
        error.setExpected(expected);
        error.setFound(found);
        return error;
    }

    private String getRemainingInput(List<String> input, int ip) {
        if (ip >= input.size()) return "";
        return String.join(" ", input.subList(ip, input.size()));
    }

    private void addStep(int stepNumber, Stack stack,
                         String remainingInput, String action) {
        steps.add(new Parser.ParseStep(stepNumber, stack, remainingInput, action));
    }

    public static class ParseError {
        public enum ErrorType {
            MISSING_SYMBOL, UNEXPECTED_SYMBOL, EMPTY_TABLE_ENTRY, PREMATURE_END
        }

        private String message;
        private int line;
        private int column;
        private ErrorType type;
        private String expected;
        private String found;

        public void setMessage(String message) { this.message = message; }
        public void setLine(int line) { this.line = line; }
        public void setColumn(int column) { this.column = column; }
        public void setType(ErrorType type) { this.type = type; }
        public void setExpected(String expected) { this.expected = expected; }
        public void setFound(String found) { this.found = found; }

        public String getMessage() { return message; }
        public int getLine() { return line; }
        public int getColumn() { return column; }
        public ErrorType getType() { return type; }
        public String getExpected() { return expected; }
        public String getFound() { return found; }
    }
}