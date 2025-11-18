package expression.parser;

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
            if (token.matches("[\\d.]+")) {
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
            prevIsOperator = token.matches("[*/+\\-]");
        }

        // Validate token adjacency rules to ensure syntactically correct expressions:
        // 1. '(' must not be immediately followed by ')', '*' or '/' (e.g., "()" or "(*3)").
        // 2. '(' must not be directly preceded by a number or ')' - prevents invalid sequences like "3(" or ")(".
        // 3. ')' must not directly follow an operator ('*', '/', '+', '-') — operators cannot end a subexpression.
        // 4. ')' must not be immediately followed by a number — prevents invalid sequences like ")3" or ")(3)".
        Iterator<String> iterator = tokens.iterator();
        String currentToken = iterator.next();
        while (iterator.hasNext()) {
            String nextToken = iterator.next();
            if (currentToken.equals("(") && nextToken.matches("[)*/]")) {
                throw new IllegalArgumentException("Invalid token sequence: ( followed by " + nextToken);
            }
            if (nextToken.equals("(") && (currentToken.equals(")") || isNumber(currentToken))) {
                throw new IllegalArgumentException("Invalid token sequence: " + currentToken + " followed by (.");
            }
            if (currentToken.equals(")") && nextToken.matches("\\d+(\\.\\d+)?")) {
                throw new IllegalArgumentException("Invalid token sequence: ) followed by " + nextToken);
            }
            if (nextToken.equals(")") && currentToken.matches("[*/+\\-]")) {
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

        if (stripped.get(0).matches("[+\\-]") && isExactlyOneOperandInParentheses(stripped.subList(1, stripped.size()))) {
            return new Node(
                    stripped.get(0),
                    null,
                    buildTree(new ArrayList<>(stripped.subList(1, stripped.size())))
            );
        }

        /* Here we are looking for the main operator that divides entire expression:
         *  <left expression> <main operator> <right expression> */
        int mainOpIndex = -1;
        int level = 0;
        for (int i = stripped.size() - 1; i >= 0; i--) {
            String token = stripped.get(i);
            if (token.equals(")")) {
                level++;
            } else if (token.equals("(")) {
                level--;
            }
            if (level == 0 && token.matches("[+\\-]")) {
                // Check if the operator is NOT unary
                if (i > 0 && isOperandEnd(stripped.get(i - 1))) {
                    mainOpIndex = i;
                    break;
                }
            }
            if (level == 0 && mainOpIndex < 0 && token.matches("[*/]")) {
                mainOpIndex = i;
            }
        }
        if (mainOpIndex < 0) {
            throw new IllegalArgumentException("Operator is missing in " + stripped);
        }

        String op = stripped.get(mainOpIndex);
        // full node - value must be an operator
        if (isNotOperator(op)) {
            throw new IllegalStateException("Parser error: expected operator but found: " + op);
        }
        Node left = buildTree(new ArrayList<>(stripped.subList(0, mainOpIndex)));
        Node right = buildTree(new ArrayList<>(stripped.subList(mainOpIndex + 1, stripped.size())));
        return new Node(op, left, right);
    }

    private Node buildSimpleNode(List<String> tokens) {
        // Here could be only three options: [NUMBER OP NUMBER], [OP NUMBER], or [NUMBER].
        int size = tokens.size();
        return switch (size) {
            case 3 -> {
                String left = tokens.get(0);
                String op = tokens.get(1);
                String right = tokens.get(2);

                if (!isNumber(left) || isNotOperator(op) || !isNumber(right)) {
                    throw new IllegalArgumentException("Invalid simple expression structure (expected NUMBER OP NUMBER): " + tokens);
                }
                yield new Node(
                        op,
                        new Node(left, null, null),
                        new Node(right, null, null)
                );
            }
            case 2 -> {
                String op = tokens.get(0);
                String num = tokens.get(1);

                if ((!op.equals("+") && !op.equals("-")) || !isNumber(num)) {
                    throw new IllegalArgumentException("Invalid simple expression structure (expected OP NUMBER): " + tokens);
                }
                yield new Node(
                        op,
                        null,
                        new Node(num, null, null)
                );
            }
            case 1 -> {
                String num = tokens.get(0);
                if (!isNumber(num)) {
                    throw new IllegalArgumentException("Invalid simple expression structure (expected NUMBER): " + tokens);
                }
                yield new Node(num, null, null);
            }
            default -> throw new IllegalArgumentException("Invalid simple expression structure: " + tokens);
        };
    }

    // Replace consecutive unary '+' and '-' operators with a single normalized sign
    private List<String> normalizeUnaryChains(final List<String> tokens) {
        List<String> output = new ArrayList<>();

        Set<String> unary = Set.of("+", "-");
        StringBuilder unaryChain = new StringBuilder();
        for (String token : tokens) {
            if (unary.contains(token)) {
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

    // Helper method: Removes outermost parentheses if they fully wrap the expression. Example: "((3+2))" → "3+2"
    private List<String> stripOuterParentheses(List<String> tokens) {
        if (tokens.size() < 2) return tokens;

        if (isExactlyOneOperandInParentheses(tokens)) {
            List<String> inner = new ArrayList<>(tokens.subList(1, tokens.size() - 1));
            return stripOuterParentheses(inner);
        } else {
            return tokens;
        }
    }

    // Helper method
    private String reduceUnaryChain(final String unaryChain) {
        if (!unaryChain.matches("[+\\-]+")) {
            throw new IllegalArgumentException("Unary chain must contain only '+' or '-' characters.");
        }
        long minusCount = unaryChain.chars().filter(c -> c == '-').count();
        char result = (minusCount % 2 == 0) ? '+' : '-';
        return String.valueOf(result);
    }

    // region Token predicates
    // Token predicate: checks if the given tokens are actually one operand wrapped with parentheses.
    private boolean isExactlyOneOperandInParentheses(List<String> tokens) {
        if (tokens.isEmpty()) return false;
        if (!tokens.get(0).equals("(")) return false;
        if (!tokens.get(tokens.size() - 1).equals(")")) return false;

        int depth = 0;
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (token.equals("(")) depth++;
            if (token.equals(")")) depth--;

            if (depth == 0 && i != tokens.size() - 1) return false;
        }

        return true;
    }

    // Token predicate: checks if the token is a number
    private boolean isNumber(String value) {
        return value.matches("\\d+(\\.\\d+)?");
    }

    // Token predicate: checks if the token is an operator
    private boolean isNotOperator(String value) {
        return !value.matches("[*/+\\-]");
    }

    // Token predicate: checks if the given token represents the *end* of an operand.
    private boolean isOperandEnd(String token) {
        return isNumber(token) || token.equals(")");
    }
    // endregion
}
