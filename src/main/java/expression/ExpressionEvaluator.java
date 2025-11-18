package expression;

import expression.evaluator.Evaluator;
import expression.parser.Node;
import expression.parser.Parser;
import expression.tokenizer.Tokenizer;

import java.util.List;

public final class ExpressionEvaluator {

    // private constructor
    private ExpressionEvaluator() {
    }

    /**
     * Evaluates a mathematical expression represented as a string.
     * <p>
     * This method performs a full multi-stage evaluation pipeline:
     *
     * <ol>
     *     <li><b>Normalization</b> — trims the input, replaces long dashes with '-' and
     *         converts commas to decimal dots.</li>
     *
     *     <li><b>Tokenization</b> — converts the input string into a list of lexical tokens:
     *         numbers, operators (+, -, *, /) and parentheses.</li>
     *
     *     <li><b>Unary operator normalization</b> — compresses consecutive '+' and '-'
     *         into a single equivalent unary operator.
     *         (e.g. <code>--5 → +5</code>, <code>+-+5 → -5</code>)</li>
     *
     *     <li><b>Syntax validation</b> — checks parentheses balance,
     *         detects illegal operator placement, invalid adjacency of tokens,
     *         malformed numbers, and other structural issues.</li>
     *
     *     <li><b>Parse tree construction</b> — recursively builds an expression tree
     *         respecting operator precedence and associativity.</li>
     *
     *     <li><b>Tree evaluation</b> — recursively computes the numerical result
     *         by applying unary and binary operators.</li>
     * </ol>
     *
     * <p>
     * If any stage fails (invalid syntax, mismatched parentheses, illegal tokens, etc.),
     * an {@link IllegalArgumentException} is thrown with a descriptive error message.
     *
     * @param expression the raw input mathematical expression (may contain spaces,
     *                   unary operators, nested parentheses, decimal values).
     * @return the numerical result of evaluating the expression.
     * @throws IllegalArgumentException if the input contains invalid syntax,
     *                                  unrecognized characters, malformed numbers, or produces an illegal
     *                                  parse tree structure.
     */
    public static double evaluate(String expression) throws IllegalArgumentException {
        Tokenizer tokenizer = new Tokenizer();
        List<String> tokens = tokenizer.tokenize(expression);

        Parser parser = new Parser();
        Node root = parser.parse(tokens);

        Evaluator evaluator = new Evaluator();
        return evaluator.evaluate(root);
    }
}
