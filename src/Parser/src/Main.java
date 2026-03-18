package Parser.src;

import Parser.src.Grammar.CFG;
import Parser.src.Grammar.CFGParser;
import Parser.src.Grammar.LeftFactor;
import Parser.src.Grammar.LeftRecursionRemover;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        // parse cfg from file
        if (args.length < 2) {
            System.err.println("Usage: java Main <cfg_file_path> <input_file_path>");
            System.exit(1);
        }
        String cfgFilePath = args[0];
        String inputFilePath = args[1];

        CFGParser cfgParser = new CFGParser();
        CFG cfg = cfgParser.parseCFG(cfgFilePath);
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
        Map<String, Set<String>> firstSets = FirstFollow.constructFirstSets(factoredCFG);
        FirstFollow.printFirstSets(firstSets);

        System.out.println("\n--- Follow Sets ---");
        // Get the start symbol (first non-terminal in the grammar)
        String startSymbol = factoredCFG.getAllProductions().keySet().iterator().next();
        Map<String, Set<String>> followSets = FirstFollow.constructFollowSets(factoredCFG, firstSets, startSymbol);
        FirstFollow.printFollowSets(followSets);

        // Task 1.6: LL(1) Parsing Table Construction
        System.out.println("\n--- LL(1) Parsing Table Construction ---");

        // Construct LL(1) parsing table
        LL1ParsingTableConstructor tableConstructor =
                new LL1ParsingTableConstructor(factoredCFG, firstSets, followSets, startSymbol);
        tableConstructor.constructParsingTable();

        // Print the parsing table in detailed format (with borders)
        boolean isLL1Grammar = tableConstructor.printDetailedTable();

        // if not ll1, dont proceed to parsing
        if (!isLL1Grammar) {
            System.out.println("\nThe grammar is not LL(1). Parsing cannot proceed.");
            return;
        }

        // ========== PART 2: Stack-Based Parser Implementation ==========
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PART 2: Stack-Based Parser Implementation");
        System.out.println("=".repeat(60));

        // Task 2.1: Test InputReader
        //System.out.println("\n--- Task 2.1: Testing InputReader ---");
        System.out.println("Parsing file: " + inputFilePath);

        try {
            // Get terminals from the grammar for validation
            Set<String> terminals = tableConstructor.collectTerminals();
            //System.out.println("Valid terminals from grammar: " + terminals);

            // Read and validate input strings (just one call)
            //System.out.println("\nReading input file with validation:");
            List<List<String>> inputs = InputReader.readInputFile(inputFilePath, terminals);
            InputReader.printInputs(inputs);

            /*
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
            */

            // ========== Parsing Algorithm Implementation ==========
            System.out.println("\n--- LL(1) Parsing Stack ---");

            // Create parsing algorithm instance
            Parser parser = new Parser(tableConstructor, followSets);

            // Parse all input strings
            System.out.println("\nParsing all input strings...\n");
            List<Parser.ParseResult> results = parser.parseAll(inputs);

            // Print summary
            System.out.println("\n" + "=".repeat(60));
            System.out.println("PARSING SUMMARY");
            System.out.println("=".repeat(60));

            int successCount = 0;
            for (Parser.ParseResult result : results) {
                if (result.isSuccess()) {
                    successCount++;
                }
            }

            System.out.printf("Total inputs: %d\n", results.size());
            System.out.printf("Successful: %d\n", successCount);
            System.out.printf("Failed: %d\n", results.size() - successCount);


            /*
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
             */

        } catch (IOException e) {
            System.err.println("\nError: " + e.getMessage());
            System.err.println("\nPlease create an input file at: src/Parser/input_valid.txt");
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


}