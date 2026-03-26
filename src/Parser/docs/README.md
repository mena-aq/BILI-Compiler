# BILI Compiler

The BILI Compiler is a compiler for the BILI language (.bili).
The compiler is currently at Phase 2 : LL(1) Parser Design & Implementation

### Team Members and Roll Numbers
- Menahil Ahmad Qureshi - 23i-0546
- Imama Sarwar - 23i-3048

### Programming Language
Java

### Compilation Instructions
```bash
javac -d out src/Parser/src/Grammar/*.java src/Parser/src/*.java
```

### Execution Instructions
```bash
java -cp out Parser/src/Main <grammar-file> <input-file>
```
#### Examples
- ##### Example 1: Run with grammar2.txt and valid input
```bash
java -cp out Parser/src/Main src/Parser/input/grammar2.txt src/Parser/input/input_valid.txt
```
- ##### Example 2: Run with grammar2.txt and invalid input
``` bash
java -cp out Parser/src/Main src/Parser/input/grammar2.txt src/Parser/input/input_invalid.txt
```
- ##### Example 3: Run with a different grammar file
``` bash
java -cp out Parser/src/Main src/Parser/input/grammar1.txt src/Parser/input/input_valid.txt
```

### Input File Format Specification
* One string per line
* Each token is separated by a single space
* Can add comments using # <br>
e.g.
```
( id + id ) * id  # comment
id * id + id           
```

###  Grammar File Format Specification
One production per line
* Format:`NonTerminal -> production1 | production2 | ...`
* Use -> as arrow symbol
* Use | to separate alternatives
* Terminals: lowercase letters, operators, keywords
* Non-terminals: Multi-character names starting with uppercase (e.g., Expr, Term, Factor)
* Single-character non-terminals (E, T, F, etc.) are NOT allowed
* Epsilon: @
  <br> e.g.
```
Start -> First Second
First -> a | @
Second -> b
```

### Sample Grammar and Input Files Explanation

take for example the grammar
```
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

The sample input file could be:
```
( id + id ) * id  # comment
id * id + id    
id + * id              # Missing operand between + and *
```

In this example:
- The first line `( id + id ) * id` is a valid expression according to the grammar, so the parser should accept it.
- The second line `id * id + id` is also valid, as it can be parsed as `Term -> Term * Factor` followed by `Expr -> Expr + Term`.
- The third line `id + * id` is invalid because there is a missing operand between the `+` and `*`, which violates the grammar rules. The parser should reject this input and provide an appropriate error message indicating the syntax error.


### Known Limitations
- If the grammar is not LL(1), the parser stops after generating the parsing table and does not attempt to parse any input strings. 
- The Graphviz Tree cannot be rendered in console, the .dot file output must be used with external Graphviz software to visualize the parse tree.


<img width="200" height="80" alt="image" src="https://github.com/user-attachments/assets/24f55df3-6d16-4b48-8fcf-7b6f1f411128" />
