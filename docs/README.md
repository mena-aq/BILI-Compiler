# BILI Compiler

The BILI Compiler is a compiler for the BILI language (.bili).
The compiler is currently at Phase 1 : Scanning

### File Extension
```
.bili
```
### Keyword List

* start: Indicates the start of the program (program entry point).
* finish: Indicates the end of the program.
* loop: Indicates the start of a loop block.
* condition: Indicates the condition for a loop or a condition statement.
* declare: Indicates the declaration of a variable.
* output: To output a value to the console.
* input: To take input from the console.
* function: Indicates the declaration of a function.
* return: To return a value from a function.
* break: To break out of a loop.
* continue: To continue to the next iteration of a loop.
* else: Indicates the else block of a condition statement.

### Identifier Rules
Identifiers must start with an uppercase letter (A-Z), and are followed by {0-31} alphanumeric characters or underscores (_).
e.g.
`MyVariable200`, `My_Counter`, `BILI_Variable1`


### Literal Formats
* Integer Literals: Optional sign (+|-) followed by a sequence of digits (0-9) without a decimal point.
  e.g. `123`, `0`, `456789`
* Floating-Point Literals: Optional sign (+|-) followed by a sequence of digits with a decimal point, with {1-6} digits after the decimal point. Optional exponent part (e or E followed by an optional sign and digits) is also allowed.
  e.g. `3.14`, `0.001`, `-2.5`
* String Literals: Enclosed in double quotes (".."), can contain any characters except unescaped double quotes. Escape sequences (e.g., \n, \t, \", \\) are allowed.
  e.g. `"Hello from BILI!"`, `"This is a string with a newline\nand a tab\t."`
* Character Literals: Enclosed in single quotes ('..'), can contain a single character or an escape sequence.
  e.g. `'B'`, `'\n'`, `'\''`
* Boolean Literals: The keywords `true` and `false`.

### Operators & Their Precedence
The following operators are supported in BILI, listed in order of precedence (from highest to lowest):
* Increment and Decrement: `++`, `--`
* Multiplicative Operators: `*`, `/`, `%`
* Additive Operators: `+`, `-`
* Relational Operators: `<`, `>`, `<=`, `>=`
* Equality Operators: `==`, `!=`
* Logical AND: `&&`
* Logical OR: `||`
* Assignment Operators: `=`, `+=`, `-=`, `*=`, `/=`, `%=`

### Comments in BILI
BILI supports single and multiple line comments:
* Single Line Comments: Start with `##` and continue until the end of the line.
```
## This is a single line comment in BILI
```
* Multi-line Comments: Enclosed between `#*` and `*#`, can span multiple lines.
```
#*
This is a multi-line comment in BILI.
BILI language is simple and easy to learn.
*#
```

### Sample BILI Programs

**Hello World in BILI:**
```
start{
    output "Hello, World!";
} finish
```
**BILI Program to Compute Factorial**
```
start{
    declare n;
    declare result;
    
    output "Enter a number: ";
    input n;
    
    result = 1;
    
    loop condition (n > 1) {
        result = result * n;
        n = n - 1;
    }
    
    output "Factorial is: " + result;
} finish
```
**Modular BILI Program for **
```
function Add(A, B) {
    return A + B;
}

function Subtract(A, B) {
    return A - B;
}

start{
    declare Num1;
    declare Num2;
    declare Sum;
    declare Difference;

    output "Enter first number: ";
    input Num1;

    output "Enter second number: ";
    input Num2;

    Sum = Add(Num1, Num2);
    Difference = Subtract(Num1, Num2);

    output "Sum: " + Sum;
    output "Difference: " + Difference;
} finish
```
### Compilation and Execution

To compile and execute a BILI program, follow these steps:
1. Save your BILI code in a file with the `.bili` extension
2. To generate the JFlex Lexer, run the following command in the terminal:
```
jflex src/Scanner.jflex
```
3. Compile the manual scanner & its dependencies:
```
javac -d out src/*.java
```
4. To run the compiled BILI program, use the following command:
```
java -cp out Main <path_to_your_bili_file> <scan-mode>
```
where `<path_to_your_bili_file>` is the path to your `.bili` file and `<scan-mode>' is any of the following:
* manual: tokenize the input using the manual scanner
* flex: tokenize the input using the JFlex generated scanner
* manual-flex: tokenize the input using both scanners


**BILI**, brought to you by:
* Imama Sarwar (23I-3048)
* Menahil Ahmad (23I-0546)
<img width="200" height="80" alt="image" src="https://github.com/user-attachments/assets/24f55df3-6d16-4b48-8fcf-7b6f1f411128" />

