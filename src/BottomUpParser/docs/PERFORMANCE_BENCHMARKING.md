# BottomUpParser - Performance Benchmarking Guide

## Quick Start

### How to Compare SLR(1) vs LR(1) Performance

#### **1. Construction Time & Memory Usage**

Run the benchmarking tool:

```bash
cd C:\Users\menah\source\compiler-1
javac src/BottomUpParser/src/*.java
java BottomUpParser.src.Main "src/BottomUpParser/input/grammar2.txt" "src/BottomUpParser/input/input_valid.txt" --benchmark
```

**Output Example:**
```
================================================================================
PERFORMANCE COMPARISON: SLR(1) vs LR(1)
================================================================================

SLR(1) Parser Results:
  Construction Time: 45 ms
  Memory Used: 128 KB
  States: 12
  Total Items: 48
  Conflicts: 0
  Avg Lookaheads/Item: 0

LR(1) Parser Results:
  Construction Time: 67 ms
  Memory Used: 156 KB
  States: 14
  Total Items: 63
  Conflicts: 0
  Avg Lookaheads/Item: 2

--------------------------------------------------------------------------------
RELATIVE METRICS:
--------------------------------------------------------------------------------
Construction Time: LR(1) is 1.49× slower than SLR(1)
Memory Usage: LR(1) is 1.22× more than SLR(1)
State Count: LR(1) has 1.17× states of SLR(1) (difference: 2)
Item Count: LR(1) has 1.31× items of SLR(1) (difference: 15)
Conflicts: SLR(1) has 0, LR(1) has 0 (difference: 0)
Average Lookaheads per Item: LR(1) = 2.00

================================================================================
```

---

#### **2. Understanding the Metrics**

| Metric | What It Means |
|--------|---|
| **Construction Time** | How long it takes to build the parsing table (CLOSURE + GOTO iterations) |
| **Memory Used** | Amount of heap memory allocated for the parser's data structures |
| **State Count** | Number of DFA states generated (more states = more table rows) |
| **Item Count** | Total LR items across all states (includes closure items) |
| **Conflicts** | Number of shift/reduce or reduce/reduce conflicts detected |
| **Avg Lookaheads/Item** | (LR(1) only) Average lookahead set size per item |

---

#### **3. Key Comparisons for Different Grammars**

**Test Grammar 1: Simple Addition** (`grammar1.txt`)
```
E -> E + T | T
T -> id
```
**Expected Results:**
- Both SLR(1) and LR(1) have ~0 conflicts
- State counts very similar (LR(1) ≈ 1.1× SLR(1))
- Construction time nearly identical

**→ Use SLR(1)** for best performance

---

**Test Grammar 2: Addition & Multiplication** (`grammar2.txt`)
```
E -> E + T | T
T -> T * F | F
F -> ( E ) | id
```
**Expected Results:**
- Both parsers work without conflicts
- LR(1) ≈ 1.2–1.5× states of SLR(1)
- LR(1) ≈ 1.3× slower in construction

**→ SLR(1) is sufficient**

---

**Test Grammar 4: If-Then-Else** (`grammar4.txt`)
```
S -> if id then S else S | other
```
**Expected Results:**
- **SLR(1):** 1+ shift/reduce conflicts (ambiguous)
- **LR(1):** 0 conflicts (resolved with lookaheads)
- LR(1) ≈ 2–3× more states than SLR(1)

**→ LR(1) is necessary**

---

### **4. How Table Construction Works**

**SLR(1) Process:**
1. Generate initial state: `CLOSURE({S' → • S})`
2. For each state and each symbol: compute `GOTO(state, symbol)`
3. Add new states until saturation
4. Build action table using **FOLLOW sets** for reduce actions
5. Build goto table for non-terminals

**Time:** O(n_states × n_symbols × closure_cost)

**LR(1) Process:**
1. Generate initial state: `CLOSURE({[S' → • S, $]})`
2. For each state and each symbol: compute LR(1) `GOTO(state, symbol)`
   - Uses `CLOSURE_LR1` which computes FIRST(βa) for each item's lookahead β and a
3. Add new states until saturation
4. Build action table using **item lookaheads** for reduce actions
5. Build goto table

**Time:** O(n_states × n_symbols × (closure_cost + FIRST_computations))

---

### **5. Memory Breakdown**

**SLR(1) Table Structure:**
```
Action Table: states × terminals → (SHIFT/REDUCE/ACCEPT)
Goto Table:   states × non-terminals → state_number
Total Size:   ~O(states × avg_symbols_per_state)
```

**LR(1) Table Structure:**
```
Action Table: states × terminals → (SHIFT/REDUCE_with_lookaheads/ACCEPT)
Goto Table:   (same as SLR)
Lookahead Storage: per-item Set<String>
Total Size:   SLR(1) + overhead for lookahead sets
```

---

### **6. Benchmark Without Parser Runs**

If you only want to measure table construction (skip parsing test input):

```bash
java BottomUpParser.src.Main "src/BottomUpParser/input/grammar2.txt" --benchmark
```

(Omit the input file path; benchmarks will still run)

---

### **7. Interpreting the Comparison Output**

**Example Analysis:**

```
Construction Time: LR(1) is 1.49× slower than SLR(1)
→ LR(1) takes 49% longer to construct

Memory Usage: LR(1) is 1.22× more than SLR(1)
→ LR(1) uses 22% more RAM

State Count: LR(1) has 1.17× states of SLR(1) (difference: 2)
→ LR(1) has 2 extra states (17% increase)

Conflicts: SLR(1) has 3, LR(1) has 1 (difference: 2)
→ LR(1) resolves 2 conflicts (66.7% reduction)
```

**Decision Logic:**
- If SLR(1) has **0 conflicts** → Use SLR(1) for better performance
- If LR(1) **eliminates conflicts** → Use LR(1) for correctness
- If both have conflicts → Grammar is ambiguous, needs review

---

## Implementation Details

### PerformanceBenchmark Class

Located in: `src/BottomUpParser/src/PerformanceBenchmark.java`

**Methods:**
- `benchmarkSLR(Grammar)` → Measures SLR(1) construction
- `benchmarkLR1(Grammar)` → Measures LR(1) construction
- `compareResults(slr, lr1)` → Outputs detailed comparison
- `benchmark(Grammar)` → Runs both and compares

**BenchmarkResult Fields:**
```java
String parserType;        // "SLR(1)" or "LR(1)"
long constructionTimeMs;  // In milliseconds
long memoryUsedKB;        // In kilobytes
int stateCount;           // Number of DFA states
int itemCount;            // Total items across all states
int conflictCount;        // Detected conflicts
int avgLookaheadsPerItem; // (LR(1) only)
```

---

## When to Use Each Parser

| Criterion | SLR(1) | LR(1) |
|-----------|--------|-------|
| **Simple grammars** | ✅ Use | Overkill |
| **Grammar has conflicts** | ❌ Fails | ✅ Use |
| **Performance critical** | ✅ Faster | Slower |
| **Memory constrained** | ✅ Compact | Uses more |
| **Precise lookahead needed** | ❌ Over-conservative | ✅ Exact |
| **Dangling-else problems** | ❌ False conflicts | ✅ Resolves |

---

## References

See `DESIGN_AND_PERFORMANCE.md` for:
- **Section 1:** Data structure details
- **Section 2:** CLOSURE/GOTO algorithm complexity
- **Section 3:** Design trade-offs explained
- **Section 4:** Lookahead handling in LR(1)
- **Section 5:** Measurement strategies (in-depth)


