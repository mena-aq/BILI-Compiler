package Parser;

import java.util.*;

public class CFG {

    // Map from non-terminal to list of productions (each production is a list of symbols).
    private final Map<String, List<List<String>>> productions = new LinkedHashMap<>();

    public void addProduction(String lhs, List<String> rhs) {
        productions.computeIfAbsent(lhs, k -> new ArrayList<>()).add(new ArrayList<>(rhs));
    }

    public List<List<String>> getProductions(String nonTerminal) {
        return productions.getOrDefault(nonTerminal, Collections.emptyList());
    }

    public Map<String, List<List<String>>> getAllProductions() {
        return Collections.unmodifiableMap(productions);
    }

    public void print() {
        for (Map.Entry<String, List<List<String>>> entry : productions.entrySet()) {
            String lhs = entry.getKey();
            List<List<String>> rhsList = entry.getValue();
            System.out.print(lhs + " -> ");
            for (int i = 0; i < rhsList.size(); i++) {
                List<String> rhs = rhsList.get(i);
                System.out.print(String.join(" ", rhs));
                if (i < rhsList.size() - 1) {
                    System.out.print(" | ");
                }
            }
            System.out.println();
        }
    }
}
