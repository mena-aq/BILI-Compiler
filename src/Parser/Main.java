package Parser;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        // parse cfg from file
        if (args.length < 1) {
            System.err.println("Usage: java Main <cfg_file_path>");
            System.exit(1);
        }
        String cfgFilePath = args[0];
        CFGParser parser = new CFGParser();
        CFG cfg = parser.parseCFG(cfgFilePath);
        cfg.print();

        // eliminate left recursion
        System.out.println("\n--- Removing Left Recursion ---");
        CFG noLeftRecursionCFG = LeftRecursionRemover.removeLeftRecursion(cfg);
        noLeftRecursionCFG.print();

        // left factor
        System.out.println("\n--- Left Factoring ---");
        CFG factoredCFG = LeftFactor.leftFactor(noLeftRecursionCFG);
        factoredCFG.print();

        // construct first and follow sets
        System.out.println("\n--- First Sets ---");
        Map<String, Set<String>> firstSets = FirstFollowSetConstructor.constructFirstSets(factoredCFG);
        FirstFollowSetConstructor.printFirstSets(firstSets);

        System.out.println("\n--- Follow Sets ---");
        // Get the start symbol (first non-terminal in the grammar)
        String startSymbol = factoredCFG.getAllProductions().keySet().iterator().next();
        Map<String, Set<String>> followSets = FirstFollowSetConstructor.constructFollowSets(factoredCFG, firstSets, startSymbol);
        FirstFollowSetConstructor.printFollowSets(followSets);

        // Task 1.6: LL(1) Parsing Table Construction
        System.out.println("\n--- LL(1) Parsing Table Construction ---");

        // Construct LL(1) parsing table
        LL1ParsingTableConstructor tableConstructor =
                new LL1ParsingTableConstructor(factoredCFG, firstSets, followSets, startSymbol);
        tableConstructor.constructParsingTable();

        // Print the parsing table in detailed format (with borders)
        tableConstructor.printDetailedTable();

        // ========== PART 2: Stack-Based Parser Implementation ==========
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PART 2: Stack-Based Parser Implementation");
        System.out.println("=".repeat(60));

        // Task 2.1: Test InputReader
        System.out.println("\n--- Task 2.1: Testing InputReader ---");
        String inputFilePath = "src/Parser/input.txt";

        try {
            // Get terminals from the grammar for validation
            Set<String> terminals = tableConstructor.collectTerminals();
            System.out.println("Valid terminals from grammar: " + terminals);

            // Read and validate input strings (just one call)
            System.out.println("\nReading input file with validation:");
            List<List<String>> inputs = InputReader.readInputFile(inputFilePath, terminals);
            InputReader.printInputs(inputs);

            // ========== Task 2.2: Stack Implementation Testing ==========
            System.out.println("\n--- Task 2.2: Stack Implementation Testing ---");

            // Test the stack implementation
            System.out.println("\nTesting stack operations with start symbol: " + startSymbol);
            System.out.println("-".repeat(40));

            // Create and initialize stack
            stack parserStack = new stack();
            parserStack.initialize(startSymbol);
            System.out.println("Stack after initialization: " + parserStack.toDisplayString());
            System.out.println("Top of stack: " + parserStack.top());
            System.out.println("Stack size: " + parserStack.size());

            // Test push operation
            System.out.println("\n1. Testing push operation:");
            parserStack.push("Term");
            System.out.println("After pushing 'Term': " + parserStack.toDisplayString());
            parserStack.push("+");
            System.out.println("After pushing '+': " + parserStack.toDisplayString());
            System.out.println("Top now: " + parserStack.top());

            // Test pushAll operation (for production expansion)
            System.out.println("\n2. Testing pushAll (for production expansion):");
            System.out.println("Current stack: " + parserStack.toDisplayString());
            List<String> production = new ArrayList<>();
            production.add("Factor");
            production.add("*");
            production.add("Term'");
            System.out.println("Pushing production in reverse: " + production);
            parserStack.pushAll(production);
            System.out.println("Stack after pushAll: " + parserStack.toDisplayString());

            // Test pop operation
            System.out.println("\n3. Testing pop operation:");
            String popped = parserStack.pop();
            System.out.println("Popped: " + popped);
            System.out.println("Stack after pop: " + parserStack.toDisplayString());
            popped = parserStack.pop();
            System.out.println("Popped: " + popped);
            System.out.println("Stack after pop: " + parserStack.toDisplayString());

            // Test isEmpty and onlyDollar
            System.out.println("\n4. Testing utility methods:");
            System.out.println("Is stack empty? " + parserStack.isEmpty());
            System.out.println("Does stack only have $? " + parserStack.onlyDollar());

            // Clear stack and test only $ condition
            System.out.println("\n5. Testing clear and only $ condition:");
            parserStack.clear();
            parserStack.push("$");
            System.out.println("Stack after clear and push $: " + parserStack.toDisplayString());
            System.out.println("Does stack only have $? " + parserStack.onlyDollar());

            // Test copy operation
            System.out.println("\n6. Testing copy operation:");
            parserStack.initialize(startSymbol);
            System.out.println("Original stack: " + parserStack.toDisplayString());
            stack copyStack = parserStack.copy();
            System.out.println("Copied stack: " + copyStack.toDisplayString());
            copyStack.push("Test");
            System.out.println("After modifying copy (push 'Test'): " + copyStack.toDisplayString());
            System.out.println("Original stack unchanged: " + parserStack.toDisplayString());

            System.out.println("\nStack implementation test completed successfully!");

            // ========== Task 2.3: Parsing Algorithm Implementation ==========
            System.out.println("\n--- Task 2.3: Parsing Algorithm Implementation ---");

            // Create parsing algorithm instance
            ParsingAlgorithm parsingAlgorithm = new ParsingAlgorithm(tableConstructor, followSets);

            // Parse all input strings
            System.out.println("\nParsing all input strings...");
            List<ParsingAlgorithm.ParseResult> results = parsingAlgorithm.parseAll(inputs);

            // Print summary
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PARSING SUMMARY");
            System.out.println("=".repeat(60));

            int successCount = 0;
            for (ParsingAlgorithm.ParseResult result : results) {
                if (result.isSuccess()) {
                    successCount++;
                }
            }

            System.out.printf("Total inputs: %d\n", results.size());
            System.out.printf("Successful: %d\n", successCount);
            System.out.printf("Failed: %d\n", results.size() - successCount);

            // ========== Task 2.4: Error Handling & Recovery ==========
            System.out.println("\n--- Task 2.4: Error Handling & Recovery ---");

            // Test individual error types
            System.out.println("\n1. Testing Error Type Detection:");
            testErrorTypes(followSets);

            // Test panic mode recovery with sample scenarios
            System.out.println("\n2. Testing Panic Mode Recovery:");
            testPanicModeRecovery(followSets, tableConstructor);

            // Test error production method
            System.out.println("\n3. Testing Error Production Method:");
            testErrorProductionMethod();

        } catch (IOException e) {
            System.err.println("\nError: " + e.getMessage());
            System.err.println("\nPlease create an input file at: src/Parser/input.txt");
            System.err.println("With content like:");
            System.err.println("  id + id * id");
            System.err.println("  ( id + id ) * id");
            System.err.println("  id * id + id");
            System.err.println("\nAnd for error testing, include lines like:");
            System.err.println("  id + * id");
            System.err.println("  ( id + id");
            System.err.println("  id + id *");
            System.err.println("  * id + id");
        }
    }

    /**
     * Test function to demonstrate different error types
     */
    private static void testErrorTypes(Map<String, Set<String>> followSets) {
        System.out.println("\n  Error Type 1: Missing Symbol");
        System.out.println("    Example: 'id + * id' - Missing operand between + and *");
        System.out.println("    Detection: Parser expects 'id' but finds '*'");

        System.out.println("\n  Error Type 2: Unexpected Symbol");
        System.out.println("    Example: '* id + id' - Expression starts with operator");
        System.out.println("    Detection: Parser expects 'id' or '(' but finds '*'");

        System.out.println("\n  Error Type 3: Empty Table Entry");
        System.out.println("    Example: ') id' - Closing parenthesis with no matching opening");
        System.out.println("    Detection: No production in M[Expr, ')' ]");

        System.out.println("\n  Error Type 4: Premature End");
        System.out.println("    Example: 'id +' - Incomplete expression");
        System.out.println("    Detection: Input ends but stack not empty");
    }

    /**
     * Test panic mode recovery with sample scenarios
     */
    private static void testPanicModeRecovery(Map<String, Set<String>> followSets,
                                              LL1ParsingTableConstructor tableConstructor) {
        // Create panic mode recovery instance
        ErrorHandling.PanicModeRecovery panicRecovery = new ErrorHandling.PanicModeRecovery(followSets);

        // Test Scenario 1: Missing operand
        System.out.println("\n  Scenario 1: Missing operand in 'id + * id'");
        stack stack1 = new stack();
        stack1.initialize(tableConstructor.getStartSymbol());
        stack1.push("Term"); // Simulate parser state

        List<String> input1 = new ArrayList<>(Arrays.asList("*", "id", "$"));
        ErrorHandling.MutableInt ip1 = new ErrorHandling.MutableInt(0);

        boolean recovered1 = panicRecovery.recover(stack1, input1, ip1, "Term", 1, 5);
        System.out.println("    Recovery " + (recovered1 ? "successful" : "failed"));

        // Test Scenario 2: Missing closing parenthesis
        System.out.println("\n  Scenario 2: Missing closing parenthesis in '( id + id'");
        stack stack2 = new stack();
        stack2.initialize(tableConstructor.getStartSymbol());

        List<String> input2 = new ArrayList<>(Arrays.asList("+", "id", "$"));
        ErrorHandling.MutableInt ip2 = new ErrorHandling.MutableInt(0);

        boolean recovered2 = panicRecovery.recover(stack2, input2, ip2, "Expr", 2, 10);
        System.out.println("    Recovery " + (recovered2 ? "successful" : "failed"));
    }

    /**
     * Test error production method
     */
    private static void testErrorProductionMethod() {
        ErrorHandling.ErrorProductionRecovery prodRecovery = new ErrorHandling.ErrorProductionRecovery();

        // Test missing operand error
        System.out.println("\n  Testing missing operand insertion:");
        List<String> testInput = new ArrayList<>(Arrays.asList("+", "*", "id", "$"));
        ErrorHandling.MutableInt ip = new ErrorHandling.MutableInt(0);

        boolean handled = prodRecovery.handleError("Expr", "+", testInput, ip, 3, 7);
        System.out.println("    Error handled: " + handled);
        System.out.println("    Input after handling: " + String.join(" ", testInput));

        // Display any errors recorded
        if (!prodRecovery.getErrors().isEmpty()) {
            System.out.println("\n  Recorded errors:");
            for (ErrorHandling.ParseError error : prodRecovery.getErrors()) {
                System.out.println("     " + error.toShortString());
            }
        }
    }
}