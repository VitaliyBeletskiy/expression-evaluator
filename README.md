# Java Expression Evaluator — recursive descent AST parser

A small, self-contained arithmetic expression evaluator written entirely in pure Java.  
It supports:

- `+`, `-`, `*`, `/`
- nested parentheses
- unary operators (`+5`, `--3`, `3---5`)
- decimal numbers with dot or comma
- whitespace anywhere in the expression
- full syntax validation
- AST (abstract syntax tree) construction
- recursive evaluation of the AST

The evaluator performs a complete multi-stage pipeline:

1. **Normalization**  
   Trims the input, replaces long dashes with `-`, and converts commas to dots.

2. **Tokenization**  
   Splits the expression into numbers, operators, and parentheses.

3. **Unary chain normalization**  
   Reduces sequences like `--+-5` to a single unary operator (`+` or `-`).

4. **Syntax validation**  
   Detects illegal adjacency (`3 4`, `3*/4`, `)(`), mismatched parentheses, empty subexpressions, and malformed numbers.

5. **Parse tree construction**  
   Builds an AST respecting operator precedence and left associativity.

6. **Evaluation**  
   Recursively computes the final result by executing unary and binary operations.

---

## Examples

```java
ExpressionEvaluator.evaluate("3 + 4 * 5");
// → 23.0
ExpressionEvaluator.evaluate("-(3 + --5) * 2");
// → -16.0
ExpressionEvaluator.evaluate("3*(2+(5*4))");
// → 66.0
```

## Unary operator behavior
```java
--5        → 5
+-+-5      → -5
3---5      → 8
-+-+--+-5  → -5
```

## Error handling

Invalid expressions throw descriptive IllegalArgumentException messages, e.g.:
```java
3+           → Invalid last token
3 4          → Two consecutive numbers
3*/4         → Illegal operator sequence
)(3+2)       → Invalid token sequence
(3+2         → Mismatched parentheses
3..5         → Invalid decimal literal
```
---
A minimal but robust demonstration of:

- lexical analysis
- unary operator reduction
- syntactic validation
- recursive parsing
- AST construction
- expression evaluation

All implemented manually, without external libraries or scripting engines.