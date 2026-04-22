# BottomUpParser Project

## Team Members
* Menahil Ahmad (23i-0546)
* Imama Sarwar (23i-3048)

## Programming Language
- Java

## Compilation Instructions
From the project root directory, run:

```sh
javac src/BottomUpParser/src/*.java -d .
```

## Execution Instructions
Run the main class with the grammar file and (optionally) an input file:

```sh
java BottomUpParser.src.Main <grammar-file-path> [input-file-path] [--benchmark]
```

### Example (SLR(1) Parser)
```sh
java BottomUpParser.src.Main src/BottomUpParser/input/grammar1.txt src/BottomUpParser/input/input_valid.txt
```

### Example (LR(1) Parser)
```sh
java BottomUpParser.src.Main src/BottomUpParser/input/grammar4.txt src/BottomUpParser/input/input_valid.txt
```

## Input File Format Specification
- Each line is a whitespace-separated sequence of tokens (e.g., `id + id * id`).
- Blank lines and lines starting with `#` are ignored.
- Example:
  ```
  id + id * id
  ( id + id ) * id
  ```

## Grammar File Format
- Each production is on a separate line: `NonTerminal -> RHS1 RHS2 ...`
- The first production is the start symbol.
- Example:
  ```
  Expr -> Expr + Term
  Expr -> Term
  Term -> Term * Factor
  Term -> Factor
  Factor -> id
  Factor -> ( Expr )
  ```

## Sample Commands
- **SLR(1) parser:**
  ```sh
  java BottomUpParser.src.Main src/BottomUpParser/input/grammar1.txt src/BottomUpParser/input/input_valid.txt
  ```
- **LR(1) parser:**
  ```sh
  java BottomUpParser.src.Main src/BottomUpParser/input/grammar4.txt src/BottomUpParser/input/input_valid.txt
  ```
The parsers use the same run command; the grammar file determines which parser is used based on its contents.
If the grammar is SLR(1), the SLR(1) parser and LR(1) parser will be invoked; if it is LR(1), the LR(1) parser will be invoked.

## Known Limitations
- SLR(1) parser does not apply disambiguation rules for shift/reduce conflicts (e.g., if-then-else ambiguity).
- LR(1) parser applies the standard shift > reduce rule for if-then-else conflicts and reports them as resolved.
- Only supports grammars and input files in the specified formats.

