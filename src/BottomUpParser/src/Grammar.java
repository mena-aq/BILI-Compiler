package BottomUpParser.src;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Grammar {

    // Map from non-terminal to list of productions (each production is a list of symbols).
    private final Map<String, List<List<String>>> productions = new LinkedHashMap<>();

    // The start symbol of the original grammar
    private String startSymbol;

    private Map<String, Set<String>> firstSets;
    private Map<String, Set<String>> followSets;

    /**
     * Parses a CFG from a text file.
     * Format: NonTerminal -> symbol1 symbol2 ... | symbol3 symbol4 ...
     * Use @ for epsilon
     */
    public void parseFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("->");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid production at line " + lineNumber + ": " + line);
                }

                String lhs = parts[0].trim();
                if (!isValidNonTerminal(lhs)) {
                    throw new IllegalArgumentException("Invalid non-terminal at line " + lineNumber + ": " + lhs);
                }

                // Set the start symbol as the first non-terminal encountered
                if (startSymbol == null) {
                    startSymbol = lhs;
                }

                String rhs = parts[1].trim();
                String[] alternatives = rhs.split("\\|");
                for (String alt : alternatives) {
                    alt = alt.trim();
                    List<String> symbols = new ArrayList<>();
                    if (alt.equals("@")) {
                        symbols.add("@");
                    } else {
                        for (String token : alt.split("\\s+")) {
                            if (!token.isEmpty()) symbols.add(token);
                        }
                    }
                    addProduction(lhs, symbols);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read grammar file: " + filePath, e);
        }
    }

    /**
     * Adds a production rule: lhs -> rhs
     */
    public void addProduction(String lhs, List<String> rhs) {
        productions.computeIfAbsent(lhs, k -> new ArrayList<>()).add(new ArrayList<>(rhs));
    }

    /**
     * Gets all productions for a given non-terminal
     */
    public List<List<String>> getProductions(String nonTerminal) {
        return productions.getOrDefault(nonTerminal, Collections.emptyList());
    }

    /**
     * Gets all productions
     */
    public Map<String, List<List<String>>> getAllProductions() {
        return Collections.unmodifiableMap(productions);
    }

    /**
     * Gets the start symbol of the grammar
     */
    public String getStartSymbol() {
        return startSymbol;
    }

    /**
     * Augments the grammar for SLR parsing by adding a new start symbol
     * and a production: S' -> S (where S is the original start symbol)
     *
     * @return the new augmented start symbol (typically S')
     */
    public String augmentGrammar() {
        if (startSymbol == null) {
            throw new IllegalStateException("Grammar has not been parsed or is empty");
        }

        String newStartSymbol = startSymbol + "'";
        List<String> newProduction = new ArrayList<>();
        newProduction.add(startSymbol);

        // Add the new production at the beginning
        Map<String, List<List<String>>> newProductions = new LinkedHashMap<>();
        newProductions.put(newStartSymbol, List.of(newProduction));
        newProductions.putAll(productions);

        productions.clear();
        productions.putAll(newProductions);

        startSymbol = newStartSymbol;

        return newStartSymbol;
    }

    /**
     * Prints the grammar in a readable format
     */
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

    /**
     * Prints the FIRST and FOLLOW sets for all non-terminals
     */
    public void printFirstFollow() {
        computeFirstSets();
        computeFollowSets();
        System.out.println("\nFirst sets:");
        for (String nt : productions.keySet()) {
            System.out.println("First(" + nt + ") = " + getFirstSet(nt));
        }
        System.out.println("\nFollow sets:");
        for (String nt : productions.keySet()) {
            System.out.println("Follow(" + nt + ") = " + getFollowSet(nt));
        }
    }

    /**
     * Validates if a symbol is a valid non-terminal
     */
    private boolean isValidNonTerminal(String symbol) {
        return symbol.length() > 0
                && Character.isUpperCase(symbol.charAt(0))
                && symbol.matches("[A-Za-z'][A-Za-z0-9']*");
    }

    /**
     * Computes FIRST sets for all non-terminals in the grammar.
     * FIRST(X) is the set of terminals that can appear as the first symbol
     * of a derivation starting with X.
     */
    public void computeFirstSets() {
        firstSets = new HashMap<>();

        // Initialize all FIRST sets as empty
        for (String nonTerminal : productions.keySet()) {
            firstSets.put(nonTerminal, new HashSet<>());
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, List<List<String>>> entry : productions.entrySet()) {
                String nonTerminal = entry.getKey();
                Set<String> first = firstSets.get(nonTerminal);

                for (List<String> production : entry.getValue()) {
                    if (production.isEmpty() || (production.size() == 1 && production.get(0).equals("@"))) {
                        // Epsilon production
                        if (first.add("@")) changed = true;
                    } else {
                        for (String symbol : production) {
                            if (isNonTerminal(symbol)) {
                                Set<String> symbolFirst = firstSets.get(symbol);
                                for (String f : symbolFirst) {
                                    if (!f.equals("@") && first.add(f)) {
                                        changed = true;
                                    }
                                }
                                if (!symbolFirst.contains("@")) {
                                    break;
                                }
                            } else {
                                // Terminal
                                if (first.add(symbol)) {
                                    changed = true;
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Computes FOLLOW sets for all non-terminals in the grammar.
     * FOLLOW(A) is the set of terminals that can appear immediately after A
     * in any sentential form.
     */
    public void computeFollowSets() {
        if (firstSets == null) {
            computeFirstSets();
        }

        followSets = new HashMap<>();

        // Initialize all FOLLOW sets as empty
        for (String nonTerminal : productions.keySet()) {
            followSets.put(nonTerminal, new HashSet<>());
        }

        // Add $ to FOLLOW(S') where S' is the start symbol
        followSets.get(startSymbol).add("$");

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, List<List<String>>> entry : productions.entrySet()) {
                for (List<String> production : entry.getValue()) {
                    for (int i = 0; i < production.size(); i++) {
                        String symbol = production.get(i);
                        if (isNonTerminal(symbol)) {
                            Set<String> follow = followSets.get(symbol);
                            // Get FIRST of what follows this symbol
                            boolean allCanBeEmpty = true;
                            for (int j = i + 1; j < production.size(); j++) {
                                String nextSymbol = production.get(j);
                                if (isNonTerminal(nextSymbol)) {
                                    Set<String> nextFirst = firstSets.get(nextSymbol);
                                    for (String f : nextFirst) {
                                        if (!f.equals("@") && follow.add(f)) {
                                            changed = true;
                                        }
                                    }
                                    if (!nextFirst.contains("@")) {
                                        allCanBeEmpty = false;
                                        break;
                                    }
                                } else if (!nextSymbol.equals("@")) {
                                    // Terminal
                                    if (follow.add(nextSymbol)) {
                                        changed = true;
                                    }
                                    allCanBeEmpty = false;
                                    break;
                                }
                            }
                            // If all symbols after can be empty, add FOLLOW of LHS to FOLLOW of symbol
                            if (allCanBeEmpty) {
                                String lhs = entry.getKey();
                                Set<String> lhsFollow = followSets.get(lhs);
                                for (String f : lhsFollow) {
                                    if (follow.add(f)) {
                                        changed = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the FOLLOW set for a non-terminal
     */
    public Set<String> getFollowSet(String nonTerminal) {
        if (followSets == null) {
            computeFollowSets();
        }
        return followSets.getOrDefault(nonTerminal, Collections.emptySet());
    }

    /**
     * Gets the FIRST set for a non-terminal
     */
    public Set<String> getFirstSet(String nonTerminal) {
        if (firstSets == null) {
            computeFirstSets();
        }
        return firstSets.getOrDefault(nonTerminal, Collections.emptySet());
    }

    /**
     * Helper method to check if a symbol is a non-terminal
     */
    public boolean isNonTerminal(String symbol) {
        return productions.containsKey(symbol);
    }


}
