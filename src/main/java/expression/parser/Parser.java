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
        // region Fail early validation
        // Verify that the token list is not null or empty
        if (tokens == null || tokens.isEmpty()) {
            throw new IllegalArgumentException("Expression is empty or blank.");
        }

        // The number of opening and closing parentheses must be equal
        long openParensCount = tokens.stream().filter(x -> x.equals("(")).count();
        long closeParensCount = tokens.stream().filter(x -> x.equals(")")).count();
        if (openParensCount != closeParensCount) {
            throw new IllegalArgumentException("Mismatched number of opening and closing parentheses.");
        }

        // Validate that the expression does NOT start and end with invalid tokens
        String firstToken = tokens.get(0);
        if (Set.of(")", "*", "/").contains(firstToken)) {
            throw new IllegalArgumentException("Invalid first character: " + firstToken);
        }
        String lastToken = tokens.get(tokens.size() - 1);
        if (Set.of("(", "+", "-", "*", "/").contains(lastToken)) {
            throw new IllegalArgumentException("Invalid last character: " + lastToken);
        }

        // Check that two numeric tokens do not appear consecutively (e.g., "3 4" or "3.14 15")
        boolean prevIsNumber = false;
        for (String token : tokens) {
            if (TokenPatterns.isNumber(token)) {
                if (prevIsNumber) {
                    throw new IllegalArgumentException("Two consecutive numbers detected near: " + token);
                }
                prevIsNumber = true;
            } else {
                prevIsNumber = false;
            }
        }

        // Ensure that two binary operators do not appear consecutively (following unary is allowed)
        boolean prevIsOperator = false;
        for (String token : tokens) {
            if ((token.equals("*") || token.equals("/")) && prevIsOperator) {
                throw new IllegalArgumentException("Illegal operator combination near: " + token);
            }
            prevIsOperator = TokenPatterns.isOperator(token);
        }

        // Validate token adjacency rules to ensure syntactically correct expressions:
        // 1. '(' must not be immediately followed by ')', '*' or '/' (e.g., "()" or "(*3)").
        // 2. '(' must not be directly preceded by a number or ')' - prevents invalid sequences like "3(" or ")(".
        // 3. ')' must not be immediately followed by a number — prevents invalid sequences like ")3" or ")(3)".
        // 4. ')' must not directly follow an operator ('*', '/', '+', '-') — operators cannot end a subexpression.
        Iterator<String> iterator = tokens.iterator();
        String currentToken = iterator.next();
        while (iterator.hasNext()) {
            String nextToken = iterator.next();
            if (currentToken.equals("(") && !canFollowOpeningParenthesis(nextToken)) {
                throw new IllegalArgumentException("Invalid token sequence: ( followed by " + nextToken);
            }
            if ((currentToken.equals(")") || TokenPatterns.isNumber(currentToken)) && nextToken.equals("(")) {
                throw new IllegalArgumentException("Invalid token sequence: " + currentToken + " followed by (.");
            }
            if (currentToken.equals(")") && TokenPatterns.isNumber(nextToken)) {
                throw new IllegalArgumentException("Invalid token sequence: ) followed by " + nextToken);
            }
            if (TokenPatterns.isOperator(currentToken) && nextToken.equals(")")) {
                throw new IllegalArgumentException("Invalid token sequence: " + currentToken + " followed by ).");
            }

            currentToken = nextToken;
        }
        // endregion Fail early validation

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
     *   ["+", "+", "3"]      → ["+", "3"]<br />
     *   ["-", "-", "5"]      → ["+", "5"]<br />
     *   ["3", "+", "-", "2"] → ["3", "-", "2"]<br />
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
     *   "+"      → "+"
     *   "--"     → "+"
     *   "---"    → "-"
     *   "+-+-"   → "+"
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
     *          (3)+2 (4+5)*(4-6) → false.
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

    /**
     * Returns true if the token is allowed immediately after an opening parenthesis.
     * Valid options: another '(', a unary operator, or a number.
     */
    private boolean canFollowOpeningParenthesis(String token) {
        return TokenPatterns.isNumber(token)
                || TokenPatterns.isUnaryOperator(token)
                || "(".equals(token);
    }
}
