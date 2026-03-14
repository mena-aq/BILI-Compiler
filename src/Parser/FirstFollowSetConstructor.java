package Parser;

import java.util.*;

public class FirstFollowSetConstructor {

     //  --------------------------------- FIRST SETS ---------------------------------------

     /*
     * Algorithm:
     *   Initialize First(A) = {} for all non-terminals A.
     *   while changes occur:
     *     for each production A → X1 X2 ... Xn:
     *       k = 1; Continue = true
     *       while Continue and k <= n:
     *         add First(Xk) - {@} to First(A)
     *         if @ not in First(Xk): Continue = false
     *         k++
     *       if Continue == true:        // @ survived through all Xk
     *         add @ to First(A)
     *
     */
    public static Map<String, Set<String>> constructFirstSets(CFG cfg) {
        Map<String, List<List<String>>> grammar = cfg.getAllProductions();
        Set<String> nonTerminals = grammar.keySet();

        // Initialize First(A) = {} for every non-terminal
        Map<String, Set<String>> first = new LinkedHashMap<>();
        for (String nt : nonTerminals) {
            first.put(nt, new LinkedHashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;

            for (Map.Entry<String, List<List<String>>> entry : grammar.entrySet()) {
                String A = entry.getKey();
                Set<String> firstA = first.get(A);

                for (List<String> production : entry.getValue()) {

                    // if has @ production, add @ to First(A)
                    if (production.isEmpty()
                            || (production.size() == 1
                            && isEpsilon(production.get(0)))) {
                        if (firstA.add("@"))
                            changed = true;
                        continue;
                    }

                    int k = 0;
                    boolean cont = true;

                    // A->X1 X2 ... Xn
                    while (cont && k < production.size()) {
                        String Xk = production.get(k);

                        Set<String> firstXk = firstOf(Xk, first, nonTerminals);

                        // add First(Xk) - {@} to First(A)
                        for (String sym : firstXk) {
                            if (!sym.equals("@")) {
                                if (firstA.add(sym)) changed = true;
                            }
                        }

                        // if First(Xk) has @, look at k+1
                        if (!firstXk.contains("@")) {
                            cont = false;
                        }

                        k++;
                    }

                    // if we consumed all symbols and @ survived, add @ to First(A)
                    if (cont) {
                        if (firstA.add("@")) changed = true;
                    }
                }
            }
        }

        return Collections.unmodifiableMap(first);
    }

    /**
     * Returns the FIRST set of a single symbol Xk.
     *   - If Xk is a non-terminal: return the currently computed First(Xk)
     *   - If Xk is "@": return {\@}
     *   - Otherwise (terminal): return {Xk}
     */
    private static Set<String> firstOf(String symbol,
                                       Map<String, Set<String>> first,
                                       Set<String> nonTerminals) {
        if (isEpsilon(symbol)) {
            return Collections.singleton("@");
        }
        if (nonTerminals.contains(symbol)) {
            return first.get(symbol);
        }
        // terminal
        return Collections.singleton(symbol);
    }

    private static boolean isEpsilon(String symbol) {
        return symbol.equals("@");
    }

    public static void printFirstSets(Map<String, Set<String>> firstSets) {
        for (Map.Entry<String, Set<String>> entry : firstSets.entrySet()) {
            System.out.println("First(" + entry.getKey() + ") = { "
                    + String.join(", ", entry.getValue()) + " }");
        }
    }


    //  --------------------------------- FOLLOW SETS ---------------------------------------


     /* Algorithm:
     *   Follow(start) = { $ }
     *   Follow(A)     = {}   for all other non-terminals A
     *
     *   while changes occur:
     *     for each production A → X1 X2 ... Xn:
     *       for each Xi that is a non-terminal:
     *         compute First(Xi+1 Xi+2 ... Xn)
     *         add First(trailer) - {@} to Follow(Xi)
     *         if @ ∈ First(trailer):
     *           add Follow(A) to Follow(Xi)
     *         (if i == n the trailer is empty, so First(trailer) = {@},
     *          which means Follow(A) is always added for the last symbol)
     *
     */
    public static Map<String, Set<String>> constructFollowSets(
            CFG cfg,
            Map<String, Set<String>> firstSets,
            String start) {

        // if start symbol not passed, use the first non-terminal in the grammar as start
        if (start == null || start.isEmpty()) {
            start = cfg.getAllProductions().keySet().iterator().next();
        }

        Map<String, List<List<String>>> grammar = cfg.getAllProductions();
        Set<String> nonTerminals = grammar.keySet();

        // Initialise
        Map<String, Set<String>> follow = new LinkedHashMap<>();
        for (String nt : nonTerminals) {
            follow.put(nt, new LinkedHashSet<>());
        }
        follow.get(start).add("$");

        boolean changed = true;
        while (changed) {
            changed = false;

            // For each production A → X1 X2 ... Xn
            for (Map.Entry<String, List<List<String>>> entry : grammar.entrySet()) {
                String A = entry.getKey();

                for (List<String> production : entry.getValue()) {

                    // Skip pure @ productions — no non-terminals to process
                    if (production.isEmpty()
                            || (production.size() == 1 && isEpsilon(production.get(0)))) {
                        continue;
                    }

                    int n = production.size();

                    for (int i = 0; i < n; i++) {
                        // update follow set of Xi
                        String Xi = production.get(i);

                        // we only update Follow sets for non-terminals
                        if (!nonTerminals.contains(Xi)) continue;

                        Set<String> followXi = follow.get(Xi);

                        // Compute trailer = First(Xi+1 ... Xn)
                        // If i == n-1 (Xi is the last symbol), trailer is empty so First = {@}
                        Set<String> trailerFirst = firstOfString(
                                production.subList(i + 1, n), firstSets, nonTerminals);

                        // Add First(trailer) - {@} to Follow(Xi)
                        for (String sym : trailerFirst) {
                            if (!sym.equals("@")) {
                                if (followXi.add(sym)) changed = true;
                            }
                        }

                        // If @ ∈ First(trailer), add Follow(A) to Follow(Xi)
                        if (trailerFirst.contains("@")) {
                            if (followXi.addAll(follow.get(A))) changed = true;
                        }
                    }
                }
            }
        }

        return Collections.unmodifiableMap(follow);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Computes First(Y1 Y2 ... Ym) for a list of symbols.
     * Stops at Yi where @ is not in First(Yi).
     * Returns {\@} for an empty list (the trailer after the last symbol).
     */
    private static Set<String> firstOfString(
            List<String> symbols,
            Map<String, Set<String>> firstSets,
            Set<String> nonTerminals) {

        Set<String> result = new LinkedHashSet<>();

        if (symbols.isEmpty()) {
            result.add("@");
            return result;
        }

        boolean allNullable = true;

        for (String sym : symbols) {
            Set<String> firstSym = firstOf(sym, firstSets, nonTerminals);

            // Add everything except @
            for (String s : firstSym) {
                if (!s.equals("@")) result.add(s);
            }

            if (!firstSym.contains("@")) {
                // This symbol is not nullable — @ cannot propagate further
                allNullable = false;
                break;
            }
        }

        if (allNullable) result.add("@");
        return result;
    }

    // -----------------------------------------------------------------------
    // Pretty printer
    // -----------------------------------------------------------------------
    public static void printFollowSets(Map<String, Set<String>> followSets) {
        for (Map.Entry<String, Set<String>> entry : followSets.entrySet()) {
            System.out.println("Follow(" + entry.getKey() + ") = { "
                    + String.join(", ", entry.getValue()) + " }");
        }
    }



}
