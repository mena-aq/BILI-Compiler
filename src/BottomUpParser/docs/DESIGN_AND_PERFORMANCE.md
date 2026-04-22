# BottomUpParser: Design, Implementation & Performance Analysis

## 1. Data Structures for Items, Item Sets, and Parsing Tables

### 1.1 LR Items (`Items.LRItem`)
**Purpose:** Represents a production rule with a dot position indicating how much of the rule has been processed.

```java
public static class LRItem {
    public final String lhs;           // Left-hand side (non-terminal)
    public final List<String> rhs;     // Right-hand side (sequence of symbols)
    public final int dotPosition;      // Position of the dot in the RHS
    public final Set<String> lookaheads; // For LR(1), empty for SLR(1)
}
```

**Design Rationale:**
- **Immutable fields:** Once created, items never change, enabling efficient set-based operations and caching
- **Lookaheads as a Set:** Allows flexible lookahead handling—empty set for SLR(1), populated for LR(1)
- **List for RHS:** Preserves order and position information, essential for CLOSURE and GOTO operations

**Example:**
```
For production: E -> E + T
Possible items (SLR): E -> • E + T
                       E -> E • + T
                       E -> E + • T
                       E -> E + T •

For LR(1), the same items with lookaheads:
E -> • E + T, {+, $}
E -> E • + T, {+, $}
E -> E + • T, {+, $}
E -> E + T •, {+, $}
```

### 1.2 Item Sets (Canonical Collection)
**Data Structure:** `List<Set<Items.LRItem>>`

Each set represents a state in the DFA (Deterministic Finite Automaton) that the parser generates.

**Key Properties:**
- **Order-Independent:** Using HashSet allows fast lookup operations (O(1) average)
- **Canonical:** Each state uniquely represents a configuration of partially-parsed input
- **Indexed:** Position in the list = state number (used in shift/reduce tables)

**Example State (I0 for simple expression grammar):**
```
I0 = { E' -> • E, $
       E -> • E + T
       E -> • T
       T -> • T * F
       T -> • F
       F -> • id
       F -> • ( E ) }
```

### 1.3 Parsing Tables (`ParsingTable`)
**Dual-Table Design:**

| Component | Data Structure | Purpose |
|-----------|---|---|
| **Action Table** | `Map<Integer, Map<String, Action>>` | Defines parser behavior on terminal symbols |
| **Goto Table** | `Map<Integer, Map<String, Integer>>` | Defines state transitions on non-terminals |

**Action Types:**
```java
public enum Type { SHIFT, REDUCE, ACCEPT, ERROR }

public static class Action {
    Type type;              // One of the 4 types above
    int targetState;        // For SHIFT: which state to push
    String production;      // For REDUCE: which rule to apply
    Set<String> lookaheads; // For LR(1): conditional lookaheads
}
```

**Table Organization:**
```
         | id | + | * | ( | ) | $ || E | T | F
---------|----|----|----|----|----|----|---|----|----
State 0  |s5  | -  | -  |s4  | -  | -  ||1  | 2 | 3
State 1  | -  |s6  | -  | -  | -  |acc||   |   |
State 2  | -  |r2  |s7  | -  |r2  |r2 ||   |   |
...
```

**Memory Layout:**
- **Action entries:** ~O(n_states × n_terminals)
- **Goto entries:** ~O(n_states × n_non_terminals)
- **Total size:** Typically 5-10% of input size for moderate grammars

---

## 2. Algorithm Implementation Details: CLOSURE and GOTO

### 2.1 CLOSURE Algorithm

**Purpose:** For a set of items I, find all items reachable through epsilon (empty) productions.

**For SLR(1):**
```java
public static Set<LRItem> closure(Set<LRItem> items, Grammar grammar) {
    Set<LRItem> closure = new HashSet<>(items);
    Queue<LRItem> toProcess = new LinkedList<>(items);

    while (!toProcess.isEmpty()) {
        LRItem item = toProcess.poll();
        
        // If dot is before a non-terminal B in "... • B ..."
        if (item.dotPosition < item.rhs.size()) {
            String symbol = item.rhs.get(item.dotPosition);
            
            // If B is a non-terminal, add all its productions with dot at start
            if (isNonTerminal(symbol, grammar)) {
                for (List<String> production : grammar.getProductions(symbol)) {
                    LRItem newItem = new LRItem(symbol, production, 0, Collections.emptySet());
                    if (!closure.contains(newItem)) {
                        closure.add(newItem);
                        toProcess.add(newItem);
                    }
                }
            }
        }
    }
    return closure;
}
```

**Time Complexity:** O(P × M) where:
- P = number of productions in grammar
- M = size of the current item set
- In practice: O(n) amortized, since each item added only once

**Key Insight:** The queue ensures items are only processed once, preventing exponential blowup.

### 2.2 CLOSURE for LR(1) with Lookaheads

**Additional Complexity:** Must compute FIRST sets dynamically.

```java
public static Set<LRItem> closureLR1(Set<LRItem> items, Grammar grammar) {
    Set<LRItem> closure = new HashSet<>(items);
    Queue<LRItem> toProcess = new LinkedList<>(items);

    while (!toProcess.isEmpty()) {
        LRItem item = toProcess.poll();
        
        if (item.dotPosition < item.rhs.size()) {
            String symbol = item.rhs.get(item.dotPosition);
            
            if (grammar.isNonTerminal(symbol)) {
                // Key difference: Calculate lookaheads for closure items
                List<String> beta = item.rhs.subList(item.dotPosition + 1, item.rhs.size());
                
                for (String lookahead : item.lookaheads) {
                    // Build β + a (rest of production + original lookahead)
                    List<String> betaPlusA = new ArrayList<>(beta);
                    betaPlusA.add(lookahead);
                    
                    // FIRST(β + a) = new lookaheads for closure items
                    Set<String> firstSet = firstOfSequence(betaPlusA, grammar);
                    
                    for (List<String> production : grammar.getProductions(symbol)) {
                        for (String terminal : firstSet) {
                            if (!terminal.equals("@")) {
                                LRItem newItem = new LRItem(symbol, production, 0,
                                    new HashSet<>(Collections.singleton(terminal)));
                                if (!closure.contains(newItem)) {
                                    closure.add(newItem);
                                    toProcess.add(newItem);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return closure;
}
```

**Time Complexity:** O(P × M × FIRST_calls)
- Additional cost: FIRST set computation on each symbol (typically cached)

### 2.3 GOTO Algorithm

**Purpose:** Given a set of items I and a symbol X, compute the next state after consuming X.

```java
public static Set<LRItem> goTo(Set<LRItem> items, String symbol, Grammar grammar) {
    Set<LRItem> shiftedItems = new HashSet<>();

    // Step 1: Find all items ready to consume 'symbol'
    for (LRItem item : items) {
        if (item.dotPosition < item.rhs.size() && 
            item.rhs.get(item.dotPosition).equals(symbol)) {
            
            // Step 2: Move dot past the symbol
            LRItem shifted = new LRItem(item.lhs, item.rhs, item.dotPosition + 1, item.lookaheads);
            shiftedItems.add(shifted);
        }
    }

    // Step 3: Compute closure of shifted items
    return closure(shiftedItems, grammar);
}
```

**For LR(1):**
```java
public static Set<LRItem> goToLR1(Set<LRItem> items, String symbol, Grammar grammar) {
    // ... same as above but uses closureLR1() instead of closure()
    return closureLR1(shiftedItems, grammar);
}
```

**Time Complexity:** O(M + closure_cost) where M = size of input set

**Correctness Property:** GOTO preserves lookahead information without modification (lookaheads travel unchanged through the dot).

### 2.4 Canonical Collection Construction

**Algorithm:** Iteratively apply GOTO to all reachable states until saturation.

```java
public static List<Set<LRItem>> buildSLRCanonicalCollection(Grammar grammar) {
    List<Set<LRItem>> canonicalCollection = new ArrayList<>();
    Set<Set<LRItem>> seen = new HashSet<>();

    // Initialize with I0 = CLOSURE({S' -> • S})
    Set<LRItem> initialState = closure(initialItems, grammar);
    canonicalCollection.add(initialState);
    seen.add(initialState);

    // Expand: for each state, compute GOTO for all symbols
    int stateIndex = 0;
    while (stateIndex < canonicalCollection.size()) {
        Set<LRItem> currentState = canonicalCollection.get(stateIndex);

        for (String symbol : getAllSymbols(grammar)) {
            Set<LRItem> gotoState = goTo(currentState, symbol, grammar);

            if (!gotoState.isEmpty() && !seen.contains(gotoState)) {
                canonicalCollection.add(gotoState);
                seen.add(gotoState);
            }
        }
        stateIndex++;
    }

    return canonicalCollection;
}
```

**Termination:** Guaranteed because:
1. Each new state is unique (HashSet prevents duplicates)
2. Number of possible states ≤ 2^P (finite)
3. In practice: O(n) states for typical grammars

---

## 3. Design Decisions and Trade-offs

### 3.1 SLR(1) vs LR(1): Space vs Precision

| Aspect | SLR(1) | LR(1) |
|--------|--------|-------|
| **States Generated** | Fewer (uses FOLLOW sets) | More (individual lookaheads) |
| **Memory Usage** | ~30-40% less | Baseline |
| **Parse Capability** | Lower (many false conflicts) | Higher (fewer false conflicts) |
| **Construction Time** | Faster | Slower |
| **Determinism** | Sometimes fails | Always deterministic |

**Trade-off Decision:** Implemented both variants to allow comparison:
```java
public ParsingTable(Grammar grammar, List<Set<Items.LRItem>> canonicalCollection) {
    this(grammar, canonicalCollection, false);  // SLR(1)
}

public ParsingTable(Grammar grammar, List<Set<Items.LRItem>> canonicalCollection, boolean useLookaheads) {
    this.useLookaheads = useLookaheads;  // true for LR(1), false for SLR(1)
}
```

### 3.2 Lookahead Handling: Per-Item vs Per-Grammar

**Design Choice:** Lookaheads stored **per-item** in LR(1)

**Why:** 
- ✅ Enables precise conflict detection
- ✅ Supports merge-on-equal-core heuristics
- ✅ Clear semantic meaning: "reduce only on these terminals"
- ❌ Larger item representation (extra Set field)

**Alternative (Not Implemented):** Lookaheads per-state
```java
// Could store as: Map<Set<Item>, Set<String>> lookaheadMap
// Problem: Loses precision, increases merge complexity
```

### 3.3 Conflict Resolution Strategy

**Implemented in `ParsingTable.resolveConflicts()`:**

```java
// For if-then-else ambiguity (dangling-else problem):
// Prefer SHIFT over REDUCE on 'else' to bind else to closest if
if (hasShift && hasReduce && symbol.equals("else")) {
    actionTable.get(state).put(symbol, shiftAction);
    System.out.println("Resolved via shift preference on 'else'");
}
```

**Rationale:**
- Shift preference = right-associativity = standard language semantics
- Reduce preference would be left-associativity
- Explicitly documented in output for auditability

### 3.4 Item Set Comparison for Canonicalization

**For LR(1), states must be compared by core + lookaheads:**

```java
private static boolean setsEqual(Set<LRItem> set1, Set<LRItem> set2) {
    if (set1.size() != set2.size()) return false;
    return set1.containsAll(set2);  // Uses LRItem.equals() which includes lookaheads
}
```

**Alternative (Considered):** Merge states with identical cores but different lookaheads
```java
// Not implemented because:
// 1) More complex to implement correctly
// 2) Minimal state reduction in practice (~5-10%)
// 3) Lookahead sets grow over time (need sophisticated merge)
```

---

## 4. Lookahead Handling in LR(1) Items

### 4.1 Lookahead Representation

**Stored as:** `Set<String>` in each `LRItem`

```java
LRItem item = new LRItem(
    "E",                           // lhs
    Arrays.asList("E", "+", "T"),  // rhs
    3,                             // dotPosition (E + T •)
    new HashSet<>(Arrays.asList("+", "$"))  // lookaheads: {+, $}
);
```

**Semantics:** Item means:
- "I am at the end of production E -> E + T"
- "I will only reduce IF the next input is + or $"
- "If next is *, this is a parse error"

### 4.2 Lookahead Propagation Through CLOSURE

**For each item [A → α • B β, a]:**

The lookahead for closure items [B → • γ, ?] is **FIRST(βa)**

```java
// From closureLR1:
List<String> beta = item.rhs.subList(item.dotPosition + 1, item.rhs.size());

for (String lookahead : item.lookaheads) {
    List<String> betaPlusA = new ArrayList<>(beta);
    betaPlusA.add(lookahead);
    
    // Calculate FIRST(βa)
    Set<String> firstSet = firstOfSequence(betaPlusA, grammar);
    
    // Each B production gets lookaheads from FIRST(βa)
    for (List<String> production : grammar.getProductions(symbol)) {
        for (String terminal : firstSet) {
            if (!terminal.equals("@")) {
                LRItem newItem = new LRItem(symbol, production, 0,
                    new HashSet<>(Collections.singleton(terminal)));
                closure.add(newItem);
            }
        }
    }
}
```

**Example:**
```
Given item:  E -> • E + T , {$}
Symbol B = E, β = [+, T], a = $

FIRST(+T$) = { + }  (+ is terminal, so it's always first)

Therefore, closure item:
E -> • E + T , {+}
E -> • T , {+}
T -> • T * F , {+}
...etc
```

### 4.3 Lookahead Preservation Through GOTO

**Lookaheads are **NOT modified** during GOTO:**

```java
public static Set<LRItem> goToLR1(Set<LRItem> items, String symbol, Grammar grammar) {
    Set<LRItem> shiftedItems = new HashSet<>();

    for (LRItem item : items) {
        if (item.rhs.get(item.dotPosition).equals(symbol)) {
            // Create shifted item with SAME lookaheads
            LRItem shifted = new LRItem(item.lhs, item.rhs, item.dotPosition + 1, 
                                       new HashSet<>(item.lookaheads));  // ← Preserved!
            shiftedItems.add(shifted);
        }
    }
    
    return closureLR1(shiftedItems, grammar);
}
```

**Correctness Argument:** Lookaheads represent "what can follow this item" in the input stream. Consuming a symbol (GOTO) doesn't change the input stream's future—only our position in the parsing process. Therefore, lookaheads must propagate unchanged.

### 4.4 Lookahead Usage in Reduce Actions

**In `ParsingTable.processLR1ReduceActions()`:**

```java
private void processLR1ReduceActions(Items.LRItem item, int stateIndex, Map<String, Action> actionRow) {
    if (isAcceptItem(item)) {
        actionRow.put("$", Action.accept());
    } else {
        String production = item.lhs + " -> " + String.join(" ", item.rhs);
        
        // Key difference from SLR: Use item.lookaheads instead of FOLLOW set
        Set<String> reduceSymbols = !item.lookaheads.isEmpty() ?
                                   item.lookaheads :
                                   grammar.getFollowSet(item.lhs);  // Fallback
        
        for (String terminal : reduceSymbols) {
            // Add reduce action ONLY for these specific terminals
            actionRow.put(terminal, Action.reduce(production, item.lookaheads));
        }
    }
}
```

**Benefit Over SLR(1):**
```
SLR(1) reduces on: FOLLOW(E) = {+, ), $}
LR(1) reduces on: exact lookahead = {+, $}

Example: In state E -> E + T •
- On input "+": both reduce (✓ correct)
- On input ")": 
  - SLR(1) would reduce (✗ false conflict if ) not in FOLLOW)
  - LR(1) would NOT reduce (✓ correct, ) not in item's lookaheads)
```

### 4.5 Conflict Scenarios and Resolution

**Case 1: Shift/Reduce on Different Lookaheads**
```
State 7: E -> E + • T {+, $}
         T -> • F {+, $}
         
On input "+": No conflict (lookahead + exists, but no shift)
On input "*": No conflict (shift has priority)
```

**Case 2: Reduce/Reduce (Rare in LR(1))**
```
State X: A -> α • {x, y}
         B -> β • {x, z}

On input "x": Conflict! Both can reduce.
Solution: Grammar is ambiguous; requires precedence rules.
```

---

## How to Compare Performance: Table Construction & Memory

### Measurement Points

#### 1. **Table Construction Time**

**Code Instrumentation:**
```java
// In Main.java, wrap construction calls:
long startTime = System.nanoTime();
List<Set<Items.LRItem>> canonicalCollection = Items.buildSLRCanonicalCollection(grammar);
long slrBuildTime = System.nanoTime() - startTime;

startTime = System.nanoTime();
List<Set<Items.LRItem>> lr1Collection = Items.buildLR1CanonicalCollection(grammar);
long lr1BuildTime = System.nanoTime() - startTime;

System.out.println("SLR(1) construction: " + (slrBuildTime / 1_000_000.0) + " ms");
System.out.println("LR(1) construction: " + (lr1BuildTime / 1_000_000.0) + " ms");
```

**Key Factors:**
- **SLR(1) faster:** Uses simple FOLLOW sets, fewer state explorations
- **LR(1) slower:** CLOSURE costs more due to FIRST(βa) calculations, potentially more states

#### 2. **Memory Usage**

**Measurement Strategy:**

```java
// Approximate memory before and after
Runtime runtime = Runtime.getRuntime();
long memBefore = runtime.totalMemory() - runtime.freeMemory();

// Build parser
ParsingTable table = new ParsingTable(grammar, canonicalCollection);

long memAfter = runtime.totalMemory() - runtime.freeMemory();
long tableMemory = memAfter - memBefore;

System.out.println("Table memory: " + (tableMemory / 1024) + " KB");
```

**Components to Account For:**
1. **Item sets storage:** O(states × avg_items_per_state × item_size)
2. **Lookahead sets (LR(1)):** Extra O(avg_lookaheads_per_item) per item
3. **Table entries:** O(states × (terminals + non-terminals))
4. **Conflict tracking:** O(num_conflicts) additional storage

#### 3. **State Count Comparison**

**Already in Main.java (lines 160-164):**
```java
System.out.println("\n--- Comparison ---");
System.out.println("SLR(1) states: " + canonicalCollection.size());
System.out.println("LR(1) states: " + lr1CanonicalCollection.size());
System.out.println("LR(1) has " + (lr1CanonicalCollection.size() - canonicalCollection.size()) + " more states");
```

**Typical Ratios:**
- Simple grammars: LR(1) ≈ 1.2–1.5× states of SLR(1)
- Complex grammars: LR(1) ≈ 2–3× states of SLR(1)

#### 4. **Conflict Detection**

```java
// Both parsers report conflicts:
System.out.println("SLR(1) conflicts: " + slrTable.getConflicts().size());
System.out.println("LR(1) conflicts: " + lr1Table.getConflicts().size());

// LR(1) should have ≤ conflicts of SLR(1)
```

### Benchmark Recommendations

**Test with different grammar complexities:**

| Grammar | Characteristics | Expected Results |
|---------|---|---|
| `grammar1.txt` | Simple: addition | SLR ≈ LR for time, but LR has fewer conflicts |
| `grammar2.txt` | Moderate: mul/add | LR ≈ 1.5× slower but no conflicts |
| `grammar3.txt` | Complex: pointers | LR ≈ 2× slower, 30-40% more memory |
| `grammar4.txt` | Ambiguous: if-then-else | LR(1) essential; SLR fails |


