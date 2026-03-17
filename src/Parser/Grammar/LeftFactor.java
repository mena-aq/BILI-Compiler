package Parser.Grammar;

import java.util.*;

public class LeftFactor {

    /**
     * Applies left factoring to the given CFG in place (modifies a copy and returns it).
     *
     * Algorithm:
     *   while changes occur:
     *     for each non-terminal A:
     *       find the longest prefix α shared by two or more productions of A
     *       if α exists:
     *         create fresh non-terminal A'
     *         replace A → αβ1 | αβ2 | ... | αβk | γ1 | ... | γm
     *         with    A  → α A' | γ1 | ... | γm
     *                 A' → β1 | β2 | ... | βk
     */
    public static CFG leftFactor(CFG cfg) {
        // Work on a mutable copy of the grammar
        Map<String, List<List<String>>> grammar = deepCopy(cfg.getAllProductions());

        boolean changed = true;
        while (changed) {
            changed = false;

            // Snapshot the current non-terminals (avoid ConcurrentModificationException)
            List<String> nonTerminals = new ArrayList<>(grammar.keySet());

            for (String A : nonTerminals) {
                List<List<String>> prods = grammar.get(A);
                if (prods == null || prods.size() < 2) continue;

                // Find the longest prefix shared by at least two productions
                List<String> longestPrefix = findLongestSharedPrefix(prods);

                if (longestPrefix.isEmpty()) continue; // no shared prefix

                // Partition productions into those starting with longestPrefix and those that don't
                List<List<String>> matching    = new ArrayList<>();
                List<List<String>> nonMatching = new ArrayList<>();

                for (List<String> prod : prods) {
                    if (startsWith(prod, longestPrefix)) {
                        matching.add(prod);
                    } else {
                        nonMatching.add(prod);
                    }
                }

                // matching must have at least 2 (by construction), so we proceed
                // Generate a fresh name for A'
                String aPrime = freshName(A, grammar.keySet());

                // Build the suffixes β_i (everything after the shared prefix)
                List<List<String>> suffixes = new ArrayList<>();
                for (List<String> prod : matching) {
                    List<String> suffix = new ArrayList<>(prod.subList(longestPrefix.size(), prod.size()));
                    if (suffix.isEmpty()) {
                        suffix.add("@"); // use @ for empty suffix
                    }
                    suffixes.add(suffix);
                }

                // Build the new production for A: α A' | γ1 | ... | γm
                List<String> newProd = new ArrayList<>(longestPrefix);
                newProd.add(aPrime);

                List<List<String>> newProdsForA = new ArrayList<>();
                newProdsForA.add(newProd);
                newProdsForA.addAll(nonMatching);

                grammar.put(A, newProdsForA);
                grammar.put(aPrime, suffixes);

                changed = true;
                // Restart the inner loop for A (the outer while will re-process)
                break; // restart while loop; non-terminal list has changed
            }
        }

        // Build and return a new CFG from the transformed grammar
        CFG result = new CFG();
        for (Map.Entry<String, List<List<String>>> entry : grammar.entrySet()) {
            for (List<String> rhs : entry.getValue()) {
                result.addProduction(entry.getKey(), rhs);
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Finds the longest prefix (length ≥ 1) shared by at least two productions.
     * Returns an empty list if no such prefix exists.
     */
    private static List<String> findLongestSharedPrefix(List<List<String>> prods) {
        List<String> best = Collections.emptyList();

        for (int i = 0; i < prods.size(); i++) {
            for (int j = i + 1; j < prods.size(); j++) {
                List<String> prefix = commonPrefix(prods.get(i), prods.get(j));
                if (prefix.size() > best.size()) {
                    best = prefix;
                }
            }
        }
        return best;
    }

    /** Returns the longest common prefix of two symbol lists. */
    private static List<String> commonPrefix(List<String> a, List<String> b) {
        List<String> prefix = new ArrayList<>();
        int len = Math.min(a.size(), b.size());
        for (int k = 0; k < len; k++) {
            if (a.get(k).equals(b.get(k))) {
                prefix.add(a.get(k));
            } else {
                break;
            }
        }
        return prefix;
    }

    /** Returns true iff prod starts with prefix. */
    private static boolean startsWith(List<String> prod, List<String> prefix) {
        if (prod.size() < prefix.size()) return false;
        for (int i = 0; i < prefix.size(); i++) {
            if (!prod.get(i).equals(prefix.get(i))) return false;
        }
        return true;
    }

    /**
     * Generates a fresh non-terminal name based on base.
     * Tries base + "'" first, then base + "''", etc.
     */
    private static String freshName(String base, Set<String> existing) {
        String candidate = base + "'";
        while (existing.contains(candidate)) {
            candidate += "'";
        }
        return candidate;
    }

    /** Deep-copies the production map. */
    private static Map<String, List<List<String>>> deepCopy(Map<String, List<List<String>>> src) {
        Map<String, List<List<String>>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<List<String>>> entry : src.entrySet()) {
            List<List<String>> prodsCopy = new ArrayList<>();
            for (List<String> prod : entry.getValue()) {
                prodsCopy.add(new ArrayList<>(prod));
            }
            copy.put(entry.getKey(), prodsCopy);
        }
        return copy;
    }


}