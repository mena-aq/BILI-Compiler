package Parser;

import java.util.*;

public class LeftRecursionRemover {
    /**
     * Removes left recursion (both direct and indirect) from a CFG.
     *
     * Algorithm:
     *   1. Assign an index 1..m to each non-terminal.
     *   2. Outer loop i = 1..m (for each Ai):
     *        Inner loop j = 1..i-1 (for each Aj with lower index than Ai):
     *          For every production Ai → Aj β:
     *            Replace it with Ai → α1 β | α2 β | ... | αk β
     *            where Aj → α1 | α2 | ... | αk are the CURRENT rules for Aj.
     *            This expands indirect left recursion into direct left recursion.
     *        After the j-loop, eliminate any direct left recursion in Ai:
     *          Split Ai's productions into:
     *            - "recursive" ones of the form Ai → Ai γ
     *            - "base" ones of the form Ai → β  (β does not start with Ai)
     *          If recursive ones exist:
     *            Ai  → β Ai'  for each base β
     *            Ai' → γ Ai' | @   for each recursive γ
     */

    public static CFG removeLeftRecursion(CFG cfg) {
        Map<String, List<List<String>>> grammar = deepCopy(cfg.getAllProductions());

        // Assign a stable index order to non-terminals
        List<String> order = new ArrayList<>(grammar.keySet());

        for (int i = 0; i < order.size(); i++) {
            String Ai = order.get(i);

            // inner j loop: substitute lower-indexed NTs to expand indirect left recursion
            for (int j = 0; j < i; j++) {
                String Aj = order.get(j);

                List<List<String>> currentAiProds = grammar.get(Ai);
                List<List<String>> newAiProds = new ArrayList<>();

                for (List<String> prod : currentAiProds) {
                    // Check if this production starts with Aj
                    if (!prod.isEmpty() && prod.get(0).equals(Aj)) {
                        // Ai → Aj β  →  expand to  Ai → αk β  for each Aj → αk
                        List<String> beta = prod.subList(1, prod.size());
                        List<List<String>> ajProds = grammar.get(Aj);
                        for (List<String> alpha : ajProds) {
                            List<String> expanded = new ArrayList<>(alpha);
                            // Don't append @ symbols literally
                            if (!(expanded.size() == 1 && expanded.get(0).equals("@")
                                    || expanded.size() == 1 && expanded.get(0).equals("@"))) {
                                expanded.addAll(beta);
                            } else {
                                expanded = new ArrayList<>(beta);
                                if (expanded.isEmpty()) {
                                    expanded.add("@");
                                }
                            }
                            newAiProds.add(expanded);
                        }
                    } else {
                        newAiProds.add(new ArrayList<>(prod));
                    }
                }
                grammar.put(Ai, newAiProds);
            }

            // eliminate direct left recursion for Ai
            eliminateDirectLeftRecursion(Ai, grammar);
        }

        // Build result CFG
        CFG result = new CFG();
        for (Map.Entry<String, List<List<String>>> entry : grammar.entrySet()) {
            for (List<String> rhs : entry.getValue()) {
                result.addProduction(entry.getKey(), rhs);
            }
        }
        return result;
    }

    /**
     * Eliminates direct left recursion for a single non-terminal A.
     * If A → A γ1 | A γ2 | β1 | β2 exists, replace with:
     *   A  → β1 A' | β2 A'
     *   A' → γ1 A' | γ2 A' | @
     */
    private static void eliminateDirectLeftRecursion(
            String A, Map<String, List<List<String>>> grammar) {

        List<List<String>> prods = grammar.get(A);
        List<List<String>> recursive = new ArrayList<>(); // A → A γ
        List<List<String>> base      = new ArrayList<>(); // A → β

        for (List<String> prod : prods) {
            if (!prod.isEmpty() && prod.get(0).equals(A)) {
                // strip the leading A
                recursive.add(new ArrayList<>(prod.subList(1, prod.size())));
            } else {
                base.add(new ArrayList<>(prod));
            }
        }

        if (recursive.isEmpty()) return; // no direct left recursion

        String aPrime = freshName(A, grammar.keySet());

        // A  → β A'  for each base β
        List<List<String>> newAProds = new ArrayList<>();
        for (List<String> beta : base) {
            List<String> newProd = new ArrayList<>(beta);
            // if beta is @, the production is just A'
            if (newProd.size() == 1 && (newProd.get(0).equals("@") || newProd.get(0).equals("@"))) {
                newProd = new ArrayList<>();
            }
            newProd.add(aPrime);
            newAProds.add(newProd);
        }
        grammar.put(A, newAProds);

        // A' → γ A' | @  for each recursive γ
        List<List<String>> newAPrimeProds = new ArrayList<>();
        for (List<String> gamma : recursive) {
            List<String> newProd = new ArrayList<>(gamma);
            newProd.add(aPrime);
            newAPrimeProds.add(newProd);
        }
        // @ production
        newAPrimeProds.add(new ArrayList<>(Collections.singletonList("@")));
        grammar.put(aPrime, newAPrimeProds);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String freshName(String base, Set<String> existing) {
        String candidate = base + "'"; //
        while (existing.contains(candidate)) {
            candidate += "'";
        }
        return candidate;
    }

    private static Map<String, List<List<String>>> deepCopy(
            Map<String, List<List<String>>> src) {
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
