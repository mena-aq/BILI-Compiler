package Parser;

import java.util.*;

public class Parser {

    private Map<String, Map<String, List<String>>> parsingTable;
    private Map<String, Set<String>> followSets;
    private String startSymbol;
    private List<ParseStep> steps;
    private boolean debug;
    private int ip;

    //for parse tree
    private List<Tree> trees = new ArrayList<>();

    public Parser(LL1ParsingTableConstructor tableConstructor,
                  Map<String, Set<String>> followSets) {
        this.parsingTable = tableConstructor.getParsingTable();
        this.followSets = followSets;
        this.startSymbol = tableConstructor.getStartSymbol();
        this.steps = new ArrayList<>();
        this.debug = false;
    }

    public ParseResult parse(List<String> input, int lineNumber) {
        steps.clear();
        List<ErrorHandler.ParseError> errors = new ArrayList<>();
        int stepNumber = 0;
        boolean success = false;

        List<String> inputWithDollar = new ArrayList<>(input);
        inputWithDollar.add("$");

        Stack parserStack = new Stack();
        parserStack.initialize(startSymbol);
        ip = 0;

        Tree tree = new Tree();
        tree.init(startSymbol);
        trees.add(tree); // TREE: store this input's tree
        Set<String> nonTerminals = parsingTable.keySet();

        ErrorHandler errorHandler = new ErrorHandler(
                parsingTable, followSets, steps, stepNumber);

        addStep(++stepNumber, parserStack.copy(),
                getRemainingInput(inputWithDollar, ip),
                "Initialize stack with $ and " + startSymbol);

        while (!parserStack.isEmpty()) {
            String X = parserStack.top();
            String a = inputWithDollar.get(ip);

            // Case 1: X = a = $ → Accept
            if (X.equals("$") && a.equals("$")) {
                addStep(++stepNumber, parserStack.copy(),
                        getRemainingInput(inputWithDollar, ip),
                        "ACCEPT: Input successfully parsed!");
                success = true;
                break;
            }

            // Case 2: X = a (terminal match) — consume both
            if (X.equals(a)) {
                Stack snapshot = parserStack.copy();
                String inputBefore = getRemainingInput(inputWithDollar, ip);
                parserStack.pop();
                tree.match();
                ip++;
                addStep(++stepNumber, snapshot, inputBefore, "Match: '" + a + "'");
                continue;
            }

            // Case 3: X is non-terminal — look up table and expand, or recover if empty
            if (parsingTable.containsKey(X)) {
                Map<String, List<String>> row = parsingTable.get(X);
                List<String> production = row.get(a);

                if (production == null) {
                    errors.add(errorHandler.classifyError(
                            X, a, lineNumber, ip, row));
                    addStep(++stepNumber, parserStack.copy(),
                            getRemainingInput(inputWithDollar, ip),
                            "ERROR: " + errors.get(errors.size() - 1).getMessage());

                    int[] ipRef = {ip};
                    int[] stepRef = {stepNumber};
                    if (!errorHandler.recover(parserStack, inputWithDollar,
                            ipRef, stepRef, lineNumber,tree)) {
                        addStep(++stepNumber, parserStack.copy(),
                                getRemainingInput(inputWithDollar, ip),
                                "ERROR: Cannot recover - terminating parse");
                        break;
                    }
                    ip = ipRef[0];
                    stepNumber = stepRef[0];
                    continue;
                }

                // Valid production — expand
                Stack snapshot = parserStack.copy();
                parserStack.pop();
                if (production.isEmpty() ||
                        (production.size() == 1 && production.get(0).equals("@"))) {
                    tree.expand(production, nonTerminals);
                    addStep(++stepNumber, snapshot,
                            getRemainingInput(inputWithDollar, ip),
                            "Expand " + X + " -> @");
                } else {
                    parserStack.pushAll(production);
                    tree.expand(production, nonTerminals);
                    addStep(++stepNumber, snapshot,
                            getRemainingInput(inputWithDollar, ip),
                            "Expand " + X + " -> " + String.join(" ", production));
                }
                continue;
            }

            // Case 4: X is terminal but X != a — report error and recover
            errors.add(errorHandler.classifyError(
                    X, a, lineNumber, ip, null));
            addStep(++stepNumber, parserStack.copy(),
                    getRemainingInput(inputWithDollar, ip),
                    "ERROR: " + errors.get(errors.size() - 1).getMessage());

            if (a.equals("$")) {
                String popped = parserStack.pop();
                tree.popError();
                addStep(++stepNumber, parserStack.copy(),
                        getRemainingInput(inputWithDollar, ip),
                        "Recovery: popped terminal '" + popped + "' (a=$)");
            } else {
                tree.skipToken(a);
                addStep(++stepNumber, parserStack.copy(),
                        getRemainingInput(inputWithDollar, ip),
                        "Recovery: skipping '" + a + "'");
                ip++;
            }
        }

        // Premature end check
        if (!success && !parserStack.isEmpty() && !parserStack.onlyDollar()) {
            errors.add(errorHandler.classifyError(
                    parserStack.top(), "$", lineNumber, ip, null));
            addStep(++stepNumber, parserStack.copy(),
                    getRemainingInput(inputWithDollar, ip),
                    "ERROR: " + errors.get(errors.size() - 1).getMessage());
        }

        return new ParseResult(success, steps, errors,
                String.join(" ", input), lineNumber, tree);
    }

    private String getRemainingInput(List<String> input, int ip) {
        if (ip >= input.size()) return "";
        return String.join(" ", input.subList(ip, input.size()));
    }

    private void addStep(int stepNumber, Stack currentStack,
                         String remainingInput, String action) {
        ParseStep step = new ParseStep(stepNumber, currentStack,
                remainingInput, action);
        steps.add(step);
        if (debug) System.out.println(step.getFormattedStep());
    }

    public List<ParseResult> parseAll(List<List<String>> inputs) {
        List<ParseResult> results = new ArrayList<>();
        int lineNumber = 1;
        for (List<String> input : inputs) {
            if (input.isEmpty() || input.get(0).startsWith("#")) {
                lineNumber++;
                continue;
            }
            System.out.println("Parsing: " + String.join(" ", input));
            ParseResult result = parse(input, lineNumber);
            results.add(result);
            result.printResult();
            lineNumber++;
        }
        return results;
    }

    public List<Tree> getTrees() { return trees; }

    // --------------- Parse Step ---------------
    public static class ParseStep {
        private int stepNumber;
        private String stackContents;
        private String remainingInput;
        private String action;

        public ParseStep(int stepNumber, Stack stack,
                         String remainingInput, String action) {
            this.stepNumber = stepNumber;
            this.stackContents = stack.toDisplayString();
            this.remainingInput = remainingInput;
            this.action = action;
        }

        public String getFormattedStep() {
            return String.format("%-5d | %-30s | %-20s | %s",
                    stepNumber, stackContents, remainingInput, action);
        }

        public int getStepNumber() { return stepNumber; }
        public String getStackContents() { return stackContents; }
        public String getRemainingInput() { return remainingInput; }
        public String getAction() { return action; }
    }

    // --------------- Parse Result ---------------
    public static class ParseResult {
        private boolean success;
        private List<ParseStep> steps;
        private List<ErrorHandler.ParseError> errors;
        private String inputString;
        private int lineNumber;
        private Tree tree;

        public ParseResult(boolean success, List<ParseStep> steps,
                           List<ErrorHandler.ParseError> errors,
                           String inputString, int lineNumber,
                           Tree tree) {
            this.success = success;
            this.steps = steps;
            this.errors = errors;
            this.inputString = inputString;
            this.lineNumber = lineNumber;
            this.tree = tree;
        }

        public Tree getTree() { return tree; }

        public void printResult() {
            System.out.println(success ? "Parse Result: ACCEPT" : "Parse Result: REJECT");
            System.out.println("\nParsing Steps:");
            System.out.println("Step  | Stack Contents                   | Remaining Input      | Action");
            System.out.println("-".repeat(80));
            for (ParseStep step : steps) {
                System.out.println(step.getFormattedStep());
            }
            if (!errors.isEmpty()) {
                System.out.println("\nErrors:");
                for (ErrorHandler.ParseError error : errors) {
                    System.out.println("  " + error.getMessage());
                    if (error.getExpected() != null && error.getFound() != null) {
                        System.out.println("    Expected: " + error.getExpected());
                        System.out.println("    Found: " + error.getFound());
                    }
                }
            }

            if (tree != null) {
                System.out.println("\nParse Tree:");
                tree.preOrder(tree.getRoot());
                System.out.println("\nDOT format (paste into Graphviz):");
                tree.printDot();
            }

            System.out.println("-".repeat(80));
        }

        public boolean isSuccess() { return success; }
        public List<ParseStep> getSteps() { return steps; }
        public List<ErrorHandler.ParseError> getErrors() { return errors; }
        public String getInputString() { return inputString; }
        public int getLineNumber() { return lineNumber; }
    }
}