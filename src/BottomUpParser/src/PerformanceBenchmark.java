package BottomUpParser.src;

import java.util.*;

/**
 * Performance benchmarking utility for comparing SLR(1) and LR(1) parser construction.
 * Measures:
 * - Table construction time
 * - Memory usage
 * - State counts
 * - Conflict detection
 */
public class PerformanceBenchmark {

    public static class BenchmarkResult {
        public final String parserType;
        public final long constructionTimeNs;
        public final long constructionTimeMs;
        public final long memoryUsedBytes;
        public final long memoryUsedKB;
        public final int stateCount;
        public final int itemCount;
        public final int conflictCount;
        public final int avgLookaheadsPerItem;

        public BenchmarkResult(String parserType, long constructionTimeNs, long memoryUsedBytes,
                             int stateCount, int itemCount, int conflictCount, int avgLookaheads) {
            this.parserType = parserType;
            this.constructionTimeNs = constructionTimeNs;
            this.constructionTimeMs = constructionTimeNs / 1_000_000;
            this.memoryUsedBytes = memoryUsedBytes;
            this.memoryUsedKB = memoryUsedBytes / 1024;
            this.stateCount = stateCount;
            this.itemCount = itemCount;
            this.conflictCount = conflictCount;
            this.avgLookaheadsPerItem = avgLookaheads;
        }

        @Override
        public String toString() {
            return String.format(
                "%s Parser Results:\n" +
                "  Construction Time: %d ms\n" +
                "  Memory Used: %d KB\n" +
                "  States: %d\n" +
                "  Total Items: %d\n" +
                "  Conflicts: %d\n" +
                "  Avg Lookaheads/Item: %d",
                parserType, constructionTimeMs, memoryUsedKB, stateCount, itemCount, conflictCount, avgLookaheadsPerItem
            );
        }
    }

    /**
     * Benchmark SLR(1) parser construction
     */
    public static BenchmarkResult benchmarkSLR(Grammar grammar) {
        // Measure construction time
        long startTime = System.nanoTime();
        List<Set<Items.LRItem>> canonicalCollection = Items.buildSLRCanonicalCollection(grammar);
        long constructionTime = System.nanoTime() - startTime;

        // Build table and measure
        long tableStartTime = System.nanoTime();
        ParsingTable slrTable = new ParsingTable(grammar, canonicalCollection);
        constructionTime += (System.nanoTime() - tableStartTime);

        // Calculate statistics
        int totalItems = canonicalCollection.stream().mapToInt(Set::size).sum();
        int stateCount = canonicalCollection.size();
        int conflictCount = slrTable.getConflicts().size();

        // Estimate memory usage (rough calculation)
        // Each LRItem: ~80 bytes (String lhs, List rhs, int dotPos, Set lookaheads)
        // Each state (HashSet): ~48 bytes overhead + items
        // Action/Goto table entries: ~56 bytes each
        long itemMemory = totalItems * 80L;
        long stateMemory = stateCount * 48L;
        long actionTableMemory = (long) stateCount * grammar.getAllProductions().keySet().size() * 56L;
        long memoryUsed = itemMemory + stateMemory + actionTableMemory;

        return new BenchmarkResult("SLR(1)", constructionTime, memoryUsed, stateCount, totalItems, conflictCount, 0);
    }

    /**
     * Benchmark LR(1) parser construction
     */
    public static BenchmarkResult benchmarkLR1(Grammar grammar) {
        // Measure construction time
        long startTime = System.nanoTime();
        List<Set<Items.LRItem>> canonicalCollection = Items.buildLR1CanonicalCollection(grammar);
        long constructionTime = System.nanoTime() - startTime;

        // Build table and measure
        long tableStartTime = System.nanoTime();
        ParsingTable lr1Table = new ParsingTable(grammar, canonicalCollection, true);
        constructionTime += (System.nanoTime() - tableStartTime);

        // Calculate statistics
        int totalItems = canonicalCollection.stream().mapToInt(Set::size).sum();
        int stateCount = canonicalCollection.size();
        int conflictCount = lr1Table.getConflicts().size();

        // Calculate average lookaheads per item
        int totalLookaheads = 0;
        for (Set<Items.LRItem> state : canonicalCollection) {
            for (Items.LRItem item : state) {
                totalLookaheads += item.lookaheads.size();
            }
        }
        int avgLookaheads = totalItems > 0 ? totalLookaheads / totalItems : 0;

        // Estimate memory usage (rough calculation)
        // Each LRItem: ~80 bytes + lookahead set (~32 bytes base + 8 bytes per lookahead)
        // Each state (HashSet): ~48 bytes overhead + items
        // Action/Goto table entries: ~56 bytes each
        long avgLookaheadSize = avgLookaheads > 0 ? avgLookaheads * 8 : 0;
        long itemMemory = totalItems * (80 + 32 + avgLookaheadSize);
        long stateMemory = stateCount * 48L;
        long actionTableMemory = (long) stateCount * grammar.getAllProductions().keySet().size() * 56L;
        long memoryUsed = itemMemory + stateMemory + actionTableMemory;

        return new BenchmarkResult("LR(1)", constructionTime, memoryUsed, stateCount, totalItems, conflictCount, avgLookaheads);
    }

    /**
     * Compare results - only memory and construction time
     */
    public static void compareResults(BenchmarkResult slr, BenchmarkResult lr1) {
        System.out.println("\nPerformance Comparison: SLR(1) vs LR(1)");
        System.out.println("SLR(1):\n  Construction Time: " + slr.constructionTimeMs + " ms\n  Memory Usage: " + slr.memoryUsedKB + " KB");
        System.out.println("LR(1):\n   Construciton Time: " + lr1.constructionTimeMs + " ms\n  Memory Usage: " + lr1.memoryUsedKB + " KB\n");
    }

    /**
     * Run benchmarks on a given grammar
     */
    public static void benchmark(Grammar grammar) {
        System.out.println("\nStarting performance benchmarks...\n");

        try {
            BenchmarkResult slrResult = benchmarkSLR(grammar);
            BenchmarkResult lr1Result = benchmarkLR1(grammar);
            compareResults(slrResult, lr1Result);
        } catch (Exception e) {
            System.err.println("Benchmark error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}





