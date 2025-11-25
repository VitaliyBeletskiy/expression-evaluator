package expression.parser;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class ParserRules {

    public static void validate(List<String> tokens) {
        validateExpressionStart(tokens);
        validateExpressionEnd(tokens);
        validateParenthesesBalance(tokens);
        validateEmptyParentheses(tokens);
        validateTokenAdjacency(tokens);
        validateBinaryOperatorSequence(tokens);
    }

    // Validate that the expression does NOT start with invalid tokens
    private static void validateExpressionStart(List<String> tokens) {
        String firstToken = tokens.get(0);
        if (Set.of(")", "*", "/").contains(firstToken)) {
            throw new IllegalArgumentException("Invalid first character: " + firstToken);
        }
    }

    // Validate that the expression does NOT end with invalid tokens
    private static void validateExpressionEnd(List<String> tokens) {
        String lastToken = tokens.get(tokens.size() - 1);
        if (Set.of("(", "+", "-", "*", "/").contains(lastToken)) {
            throw new IllegalArgumentException("Invalid last character: " + lastToken);
        }
    }

    // The number of opening and closing parentheses must be equal
    private static void validateParenthesesBalance(List<String> tokens) {
        long openParensCount = tokens.stream().filter(x -> x.equals("(")).count();
        long closeParensCount = tokens.stream().filter(x -> x.equals(")")).count();
        if (openParensCount != closeParensCount) {
            throw new IllegalArgumentException("Mismatched number of opening and closing parentheses.");
        }
    }

    private static void validateEmptyParentheses(List<String> tokens) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            if (tokens.get(i).equals("(") && tokens.get(i+1).equals(")")) {
                throw new IllegalArgumentException("Empty parentheses '()' are not allowed.");
            }
        }
    }

    // Validate token adjacency rules to ensure syntactically correct expressions
    private static void validateTokenAdjacency(List<String> tokens) {
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
    }

    // Ensure that two binary operators do not appear consecutively (following unary is allowed)
    private static void validateBinaryOperatorSequence(List<String> tokens) {
        boolean prevIsOperator = false;
        for (String token : tokens) {
            if ((token.equals("*") || token.equals("/")) && prevIsOperator) {
                throw new IllegalArgumentException("Illegal operator combination near: " + token);
            }
            prevIsOperator = TokenPatterns.isOperator(token);
        }
    }

    /**
     * Returns true if the token is allowed immediately after an opening parenthesis.
     * Valid options: another '(', a unary operator, or a number.
     */
    private static boolean canFollowOpeningParenthesis(String token) {
        return TokenPatterns.isNumber(token)
                || TokenPatterns.isUnaryOperator(token)
                || "(".equals(token);
    }
}
