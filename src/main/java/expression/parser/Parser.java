package expression.parser;

import expression.ast.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Builds an Abstract Syntax Tree (AST) from a list of tokens.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Ensure that the expression is syntactically valid.</li>
 *   <li>Ensure operators have the correct number of operands. Examples of invalid expressions:
 *       <ul>
 *         <li>"1 +"</li>
 *         <li>"(3+)*2"</li>
 *         <li>"+-3"</li>
 *         <li>"3 + * 2"</li>
 *       </ul>
 *   </li>
 *   <li>Ensure that generated AST nodes are structurally correct:
 *       <ul>
 *         <li>Binary operator nodes always have both left and right children.</li>
 *         <li>Number nodes never have children.</li>
 *       </ul>
 *   </li>
 *   <li>Respect operator precedence (* / before +-).</li>
 *   <li>Correctly handle parentheses and nested expressions.</li>
 *   <li>Guarantee that the Evaluator receives a fully valid AST and does not need to perform structural checks.</li>
 * </ul>
 */
public class Parser {

    public Node parse(final List<String> tokens) {
        List<String> normalizedTokens = normalizeUnaryChains(tokens);
        return parseTokens(normalizedTokens);
    }

    private Node parseTokens(final List<String> tokens) throws IllegalArgumentException {

        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Expression is empty or blank.");
        }

        ParserRules.validate(tokens);

        return buildTree(tokens);
    }

    private Node buildTree(final List<String> tokens) {
        List<String> stripped = stripOuterParentheses(tokens);
        if (stripped.size() <= 3) return buildSimpleNode(stripped);

        if (TokenPatterns.isUnaryOperator(stripped.get(0)) && isWrappedBySingleOuterParens(stripped.subList(1, stripped.size()))) {
            return new Node.UnaryNode(
                    stripped.get(0),
                    buildTree(new ArrayList<>(stripped.subList(1, stripped.size())))
            );
        }

        /* Here we are looking for the main operator that divides entire expression:
         *  <left expression> <main operator> <right expression> */
        int mainOpIndex = -1;
        int operatorLevel = 0;
        for (int i = stripped.size() - 1; i >= 0; i--) {
            String token = stripped.get(i);
            if (token.equals(")")) {
                operatorLevel++;
            } else if (token.equals("(")) {
                operatorLevel--;
            }
            if (operatorLevel == 0 && TokenPatterns.isUnaryOperator(token) && i > 0 && isOperandEnd(stripped.get(i - 1))) {
                mainOpIndex = i;
                break;
            }
            if (operatorLevel == 0 && mainOpIndex < 0 && TokenPatterns.isMultiplicativeOperator(token)) {
                mainOpIndex = i;
            }
        }
        if (mainOpIndex < 0) {
            throw new IllegalArgumentException("Operator is missing in " + stripped);
        }

        String op = stripped.get(mainOpIndex);
        // full node - value must be an operator
        if (!TokenPatterns.isOperator(op)) {
            throw new IllegalStateException("Parser error: expected operator but found: " + op);
        }
        Node left = buildTree(new ArrayList<>(stripped.subList(0, mainOpIndex)));
        Node right = buildTree(new ArrayList<>(stripped.subList(mainOpIndex + 1, stripped.size())));
        return new Node.BinaryNode(op, left, right);
    }

    private Node buildSimpleNode(List<String> tokens) {
        // Here could be only three options: [NUMBER OP NUMBER], [OP NUMBER], or [NUMBER].
        int size = tokens.size();
        return switch (size) {
            case 3 -> {
                double leftOperand = Double.parseDouble(tokens.get(0));
                String op = tokens.get(1);
                double rightOperand = Double.parseDouble(tokens.get(2));

                if (!TokenPatterns.isOperator(op)) {
                    throw new IllegalArgumentException("Invalid simple expression structure (expected NUMBER OP NUMBER): " + tokens);
                }
                yield new Node.BinaryNode(
                        op,
                        new Node.NumberNode(leftOperand),
                        new Node.NumberNode(rightOperand)
                );
            }
            case 2 -> {
                String op = tokens.get(0);
                double num = Double.parseDouble(tokens.get(1));

                if (!op.equals("+") && !op.equals("-")) {
                    throw new IllegalArgumentException("Invalid simple expression structure (expected OP NUMBER): " + tokens);
                }
                yield new Node.UnaryNode(
                        op,
                        new Node.NumberNode(num)
                );
            }
            case 1 -> {
                double num = Double.parseDouble(tokens.get(0));
                yield new Node.NumberNode(num);
            }
            default -> throw new IllegalArgumentException("Invalid simple expression structure: " + tokens);
        };
    }

    /**
     * Normalizes sequences of consecutive unary '+' and '-' operators by collapsing
     * them into a single effective sign. Only applies when unary operators appear
     * back-to-back; other tokens are left unchanged.
     * <p>
     * Examples:<br />
     * ["+", "+", "3"]      → ["+", "3"]<br />
     * ["-", "-", "5"]      → ["+", "5"]<br />
     * ["3", "+", "-", "2"] → ["3", "-", "2"]<br />
     * <p>
     * Does not modify isolated '+' or '-' when they are not part of a chain.
     */
    private List<String> normalizeUnaryChains(final List<String> tokens) {
        List<String> output = new ArrayList<>();

        StringBuilder unaryChain = new StringBuilder();
        for (String token : tokens) {
            if (TokenPatterns.isUnaryOperator(token)) {
                unaryChain.append(token);
            } else {
                if (!unaryChain.isEmpty()) {
                    output.add(reduceUnaryChain(unaryChain.toString()));
                    unaryChain.setLength(0);
                }
                output.add(token);
            }
        }
        if (!unaryChain.isEmpty()) {
            output.add(reduceUnaryChain(unaryChain.toString()));
        }
        return output;
    }

    // Removes outermost parentheses if they fully wrap the expression. Example: "((3+2))" → "3+2"
    private List<String> stripOuterParentheses(List<String> tokens) {
        if (tokens.size() < 2) return tokens;

        if (isWrappedBySingleOuterParens(tokens)) {
            List<String> inner = new ArrayList<>(tokens.subList(1, tokens.size() - 1));
            return stripOuterParentheses(inner);
        } else {
            return tokens;
        }
    }

    /**
     * Reduces a sequence of unary '+' and '-' operators into a single effective
     * unary operator. A chain with an even number of '-' becomes '+', while an
     * odd number becomes '-'. Assumes input contains only '+' or '-' characters.
     * <p>
     * Examples:
     * "+"      → "+"
     * "--"     → "+"
     * "---"    → "-"
     * "+-+-"   → "+"
     *
     * @param unaryChain sequence of '+' and '-' characters
     * @return a single '+' or '-'
     * @throws IllegalArgumentException if the input contains non-unary characters
     */
    private String reduceUnaryChain(final String unaryChain) {
        if (!TokenPatterns.isUnaryChain(unaryChain)) {
            throw new IllegalArgumentException("Unary chain must contain only '+' or '-' characters.");
        }
        long minusCount = unaryChain.chars().filter(c -> c == '-').count();
        char result = (minusCount % 2 == 0) ? '+' : '-';
        return String.valueOf(result);
    }

    /**
     * Returns true if the entire token sequence is enclosed in a single
     * top-level pair of parentheses, with no extra tokens outside the outermost
     * brackets.
     * Example: (3+2) (5) (3+5+6+7) ((3+2)*4) (3) → true,
     * (3)+2 (4+5)*(4-6) → false.
     */
    private boolean isWrappedBySingleOuterParens(List<String> tokens) {
        if (tokens.isEmpty()) return false;
        if (!tokens.get(0).equals("(")) return false;
        if (!tokens.get(tokens.size() - 1).equals(")")) return false;

        int depth = 0;
        for (int i = 0; i < tokens.size() - 1; i++) {
            String token = tokens.get(i);

            if (token.equals("(")) depth++;
            else if (token.equals(")")) depth--;

            if (depth == 0) return false;
        }

        return true;
    }

    /**
     * Returns true if the token can legally terminate an operand in an expression.
     * Valid endings are numbers or closing parentheses.
     */
    private boolean isOperandEnd(String token) {
        return TokenPatterns.isNumber(token) || token.equals(")");
    }


}
