package Parser;

import java.util.Map;
import java.util.Set;

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
        Map<String, Set<String>> followSets = FirstFollowSetConstructor.constructFollowSets(factoredCFG, firstSets, null);
        FirstFollowSetConstructor.printFollowSets(followSets);


    }
}
