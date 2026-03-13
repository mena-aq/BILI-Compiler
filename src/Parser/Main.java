package Parser;

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

        // rleft factor
        System.out.println("\n--- Removing Left Factoring ---");
        CFG factoredCFG = LeftFactor.leftFactor(noLeftRecursionCFG);
        factoredCFG.print();



    }
}
