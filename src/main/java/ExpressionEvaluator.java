import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ExpressionEvaluator {

    // ===========================
    // Internal tree node for expression AST
    // ===========================
    private static final class Node {
        final String value;
        final Node left;
        final Node right;

        Node(String value, Node left, Node right) {
            this.value = value;
            this.left = left;
            this.right = right;
        }
    }

    private ExpressionEvaluator() {
    } // private constructor

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
        String normalizedString = normalizeString(expression);
        List<String> tokens = tokenizeString(normalizedString);
        List<String> normalizedTokens = normalizeUnaryChains(tokens);
        Node root = parse(normalizedTokens);
        return calculateTree(root);

//        testTokenizeStringSuite();
//        testParse();
//        testStripOuterParentheses();
//        testBuildTree();
    }

    private static double calculateTree(Node root) {
        // leaf: just a value
        if (root.left == null && root.right == null) {
            return Double.parseDouble(root.value);
        }

        // unary operator: only right child is used
        if (root.left == null) {
            String value = root.value;
            if (!value.equals("+") && !value.equals("-")) {
                throw new IllegalArgumentException("Illegal node - expected unary operator: " + printTree(root));
            }
            return root.value.equals("-")
                    ? -calculateTree(root.right)
                    : calculateTree(root.right);
        }

        // just in case
        if (root.right == null) {
            throw new IllegalArgumentException("Illegal node - right == null: " + printTree(root));
        }

        // full node - value must be an operator
        if (!isOperator(root.value)) {
            throw new IllegalStateException("Illegal node - expected operator: " + printTree(root));
        }
        return switch (root.value) {
            case "+" -> calculateTree(root.left) + calculateTree(root.right);
            case "-" -> calculateTree(root.left) - calculateTree(root.right);
            case "*" -> calculateTree(root.left) * calculateTree(root.right);
            case "/" -> calculateTree(root.left) / calculateTree(root.right);
            default -> throw new IllegalArgumentException("Unexpected operator in node: " + printTree(root));
        };
    }

    private static Node parse(final List<String> tokens) throws IllegalArgumentException {
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

    private static Node buildTree(final List<String> tokens) {
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

        Node left = buildTree(new ArrayList<>(stripped.subList(0, mainOpIndex)));
        Node right = buildTree(new ArrayList<>(stripped.subList(mainOpIndex + 1, stripped.size())));
        return new Node(stripped.get(mainOpIndex), left, right);
    }

    private static Node buildSimpleNode(List<String> tokens) {
        // Here could be only three options: [NUMBER OP NUMBER], [OP NUMBER], or [NUMBER].
        int size = tokens.size();
        return switch (size) {
            case 3 -> {
                String left = tokens.get(0);
                String op = tokens.get(1);
                String right = tokens.get(2);

                if (!isNumber(left) || !isOperator(op) || !isNumber(right)) {
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

                if (!isOperator(op) || !isNumber(num)) {
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
    private static List<String> normalizeUnaryChains(final List<String> tokens) {
        List<String> output = new ArrayList<String>();

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

    /**
     * Splits the input expression string into tokens (numbers, operators, parentheses) and removes whitespaces.
     * <p>
     * Example errors handled by this tokenizer:
     * - "3 + $4"   → '$' is an unknown character
     * - "3.1a"     → a number contains a letter
     * - "3.14.15"  → multiple decimal points in a single number
     * - "2.3.4.5"  → same as above
     * - "3..5"     → consecutive dots form an invalid number
     */
    private static List<String> tokenizeString(final String input) throws IllegalArgumentException {
        // Define the set of allowed characters
        Set<Character> allowed = new HashSet<>();
        for (char c : "0123456789.+-*/ ()".toCharArray()) {
            allowed.add(c);
        }

        // Validate that the input contains only allowed characters
        for (char c : input.toCharArray()) {
            if (!allowed.contains(c)) {
                throw new IllegalArgumentException("Input string contains invalid characters: " + c);
            }
        }

        ArrayList<String> output = new ArrayList<>();
        Set<Character> operatorsAndParens = Set.of('+', '-', '*', '/', '(', ')');
        boolean gotDelimiterAlready = false;
        StringBuilder currentNumber = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                currentNumber.append(c);
            } else if (c == '.') {
                if (gotDelimiterAlready) {
                    throw new IllegalArgumentException("Decimal delimiter at the invalid position.");
                }
                gotDelimiterAlready = true;
                currentNumber.append(c);
            } else if (operatorsAndParens.contains(c)) {
                gotDelimiterAlready = false;

                addNumberAndFlushStringBuilder(currentNumber, output);
                output.add(String.valueOf(c));
            } else {
                // if Whitespace: finalize current number (if any)
                gotDelimiterAlready = false;

                addNumberAndFlushStringBuilder(currentNumber, output);
            }
        }
        addNumberAndFlushStringBuilder(currentNumber, output);

        return output;
    }

    private static String normalizeString(final String input) {
        String trimmed = input.strip(); // trim on steroids

        StringBuilder output = new StringBuilder();

        for (char symbol : trimmed.toCharArray()) {
            if (Character.isWhitespace(symbol)) {
                // Convert all whitespace (spaces, tabs, newlines, etc.) into a single space
                output.append(' ');
            } else if (symbol == '–' || symbol == '—' || symbol == '−') {
                // Replace any long dash variants (en dash, em dash, minus sign) with a standard hyphen
                output.append('-');
            } else if (symbol == ',') {
                // Replace comma with a dot to support both decimal separators
                output.append('.');
            } else {
                output.append(symbol);
            }
        }
        return output.toString();
    }

    // region Helper methods
    // Helper method: Removes outermost parentheses if they fully wrap the expression. Example: "((3+2))" → "3+2"
    private static List<String> stripOuterParentheses(List<String> tokens) {
        if (tokens.size() < 2) return tokens;

        if (isExactlyOneOperandInParentheses(tokens)) {
            List<String> inner = new ArrayList<>(tokens.subList(1, tokens.size() - 1));
            return stripOuterParentheses(inner);
        } else {
            return tokens;
        }
    }

    // Helper method
    private static String reduceUnaryChain(final String unaryChain) {
        if (!unaryChain.matches("[+-]+")) {
            throw new IllegalArgumentException("Unary chain must contain only '+' or '-' characters.");
        }
        long minusCount = unaryChain.chars().filter(c -> c == '-').count();
        char result = (minusCount % 2 == 0) ? '+' : '-';
        return String.valueOf(result);
    }

    // Helper method
    private static void addNumberAndFlushStringBuilder(StringBuilder strBuilder, ArrayList<String> arrayWithData) {
        if (!strBuilder.isEmpty()) {
            String strNumber = strBuilder.toString();
            if (strNumber.equals(".")) {
                throw new IllegalArgumentException("Lone dots are not allowed.");
            } else if (strNumber.startsWith(".")) {
                strNumber = "0" + strNumber;
            } else if (strNumber.endsWith(".")) {
                strNumber += "0";
            }
            arrayWithData.add(strNumber);
            strBuilder.setLength(0);
        }
    }
    // endregion

    // region Token predicates
    // Token predicate: checks if the token is an number
    private static boolean isNumber(String value) {
        return value.matches("\\d+(\\.\\d+)?");
    }

    // Token predicate: checks if the token is an operator
    private static boolean isOperator(String value) {
        return value.matches("[*/+\\-]");
    }

    // Token predicate: checks if the given token represents the *end* of an operand.
    private static boolean isOperandEnd(String token) {
        return isNumber(token) || token.equals(")");
    }

    // Token predicate: checks if the given tokens are actually one operand wrapped with parentheses.
    private static boolean isExactlyOneOperandInParentheses(List<String> tokens) {
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
    // endregion

    // region Tests from chatGPT
    // A richer self-checking test suite for tokenizeString(...).
    // It prints normalized input, produced tokens, and expected results for comparison.
    private static void testTokenizeStringSuite() {
        class Case {
            final String input;
            final String expected;

            Case(String i, String e) {
                input = i;
                expected = e;
            }
        }

        Case[] cases = new Case[]{
                // --- Simple values ---
                new Case("", "[]  (empty string → OK, no tokens)"), new Case("   ", "[]  (whitespace only → OK)"), new Case("42", "[42]"), new Case("3.1415", "[3.1415]"), new Case("007", "[007]"),

                // --- Basic arithmetic ---
                new Case("1+2", "[1, +, 2]"), new Case("3 + 4 * 2", "[3, +, 4, *, 2]"), new Case("(3+4)*2", "[(, 3, +, 4, ), *, 2]"), new Case("((1+2)*(3-4))/5", "[(, (, 1, +, 2, ), *, (, 3, -, 4, ), ), /, 5]"),

                // --- Unary operators ---
                new Case("-5", "[-, 5]"), new Case("+5", "[+, 5]"), new Case("--5", "[-, -, 5]"), new Case("+-5", "[+, -, 5]"), new Case("5*-5", "[5, *, -, 5]"), new Case("3 + (-2)", "[3, +, (, -, 2, )]"),

                // --- Decimal commas / normalization ---
                new Case("3,14", "[3.14]"), new Case("3,14,15", "ERROR (multiple decimal separators)"),

                // --- Invalid numeric forms ---
                new Case("3.14.15", "ERROR (two dots in one number)"), new Case("3..5", "ERROR (double dot)"), new Case(".", "ERROR (lone dot)"),

                // --- Operators sequence issues ---
                new Case("2*/3", "[2, *, /, 3] → parse error later"), new Case("+*4", "[+, *, 4] → parse error later"), new Case("3 4", "[3, 4] → parse error later (two numbers)"),

                // --- Bracket balance ---
                new Case("(3+2", "[(, 3, +, 2] → missing ')' → parse error later"), new Case("3+2)", "[3, +, 2, )] → extra ')' → parse error later"),

                // --- Unicode normalization ---
                new Case("−3 + 4", "[-, 3, +, 4] (U+2212 normalized)"), new Case("3 — 4", "[3, -, 4] (em dash normalized)"), new Case("3 × 4", "ERROR (× not allowed)"),

                // --- Unary chains ---
                new Case("++5", "[+, +, 5]"), new Case("--5", "[-, -, 5]"), new Case("-+-5", "[-, +, -, 5]"),

                // --- Messy spacing / newlines ---
                new Case("3\t+\n4", "[3, +, 4] (whitespace collapsed)"), new Case(" 3 + ( 4 - 2 ) * 5 ", "[3, +, (, 4, -, 2, ), *, 5]"),

                // --- Completely invalid symbol ---
                new Case("3 + $5", "ERROR ($ not allowed)")};

        System.out.println("=== tokenizeString() Test Suite ===");

        int idx = 1;
        for (Case cs : cases) {
            System.out.println("\n#" + (idx++) + "  raw: \"" + cs.input + "\"");
            String normalized = normalizeString(cs.input);
            System.out.println("    normalized: " + normalized);
            System.out.println("    expected:   " + cs.expected);

            try {
                List<String> tokens = tokenizeString(normalized);
                System.out.println("    tokens:     " + tokens);
            } catch (IllegalArgumentException e) {
                System.out.println("    ERROR:      " + e.getMessage());
            } catch (Exception e) {
                System.out.println("    ERROR:      " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
    }

    private static void testParse() {
        System.out.println("=== Testing parse(List<String>) ===");

        // ---------------------------- //
        // 1. ERROR CASES (must fail)
        // ---------------------------- //
        List<List<String>> errorCases = List.of(
                // Empty or null
                List.of(),

                // Mismatched parentheses
                List.of("("), List.of("(", "3", "+", "2"), List.of("3", "+", "2", ")"),

                // Invalid first token
                List.of(")", "3", "+", "2"), List.of("*", "3", "+", "2"), List.of("/", "3", "+", "2"),

                // Invalid last token
                List.of("3", "+"), List.of("3", "-"), List.of("3", "*"), List.of("3", "/"), List.of("(", "3", "+", "2", "("),

                // Two numbers in a row
                List.of("3", "4"), List.of("3.14", "15"), List.of("3", "(", "4", ")"),

                // Two binary operators in a row where second is */
                List.of("3", "*", "/", "2"), List.of("3", "/", "*", "2"), List.of("3", "*", "*", "2"), List.of("3", "/", "/", "2"),

                // Bad adjacency with parentheses
                List.of("(", ")"),             // () impossible
                List.of("(", "*"),             // (*3 invalid
                List.of("(", "/", "3", ")"),

                List.of("3", ")", "4"),        // ) before number
                List.of("(", "2", ")", "5"), List.of("(", "3", ")", "10"),

                List.of("3", "+", ")"),        // operator before ")"
                List.of("(", "3", "*", ")"), List.of("(", "/", ")"),

                // Mixed misplaced parentheses
                List.of("3", ")", "+", "4"), List.of("(", "3", "(", "+", "4"), List.of("(", "3", ")", "(", "4", ")"),  // )(
                List.of("3", ")", "(", "4"));

        int errorPassed = 0;
        int errorFailed = 0;

        for (List<String> test : errorCases) {
            try {
                parse(test);
                System.out.println("❌ ERROR: expected exception but got success: " + test);
                errorFailed++;
            } catch (Exception e) {
                System.out.println("✔️ Caught expected exception for: " + test);
                errorPassed++;
            }
        }

        // ---------------------------- //
        // 2. SUCCESS CASES (must succeed)
        // ---------------------------- //
        List<List<String>> okCases = List.of(List.of("3"), List.of("42"), List.of("3", "+", "4"), List.of("3", "-", "2"), List.of("3", "*", "4"), List.of("3", "/", "4"),

                List.of("(", "3", "+", "4", ")"), List.of("(", "3", ")", "*", "(", "2", ")"),

                List.of("3", "+", "(", "4", "-", "1", ")"), List.of("(", "3", "+", "2", ")", "*", "5"),

                // Вложенные скобки
                List.of("(", "(", "3", ")", ")"),

                // Унарные уже сведены нормалайзером
                List.of("-", "5"),   // нормально
                List.of("+", "5"),   // тоже нормально, унарный плюс разрешён
                List.of("3", "+", "-", "5"), // после свёртки будет "3","+","-","5"
                List.of("(", "-", "3", ")"));

        int okPassed = 0;
        int okFailed = 0;

        for (List<String> test : okCases) {
            try {
                Node result = parse(test);
                System.out.println("✔️ OK: " + test + " => " + result);
                okPassed++;
            } catch (Exception e) {
                System.out.println("❌ ERROR: unexpected exception for: " + test + "\n" + e);
                okFailed++;
            }
        }

        // ---------------------------- //
        // SUMMARY
        // ---------------------------- //
        System.out.println("\n=== SUMMARY ===");
        System.out.println("Error tests passed:   " + errorPassed);
        System.out.println("Error tests failed:   " + errorFailed);
        System.out.println("OK tests passed:      " + okPassed);
        System.out.println("OK tests failed:      " + okFailed);
    }

    static void testStripOuterParentheses() {
        System.out.println("=== Testing stripOuterParentheses() ===");

        record Case(List<String> input, List<String> expected) {
        }

        List<Case> tests = List.of(
                // 1. Simple outer parentheses
                new Case(
                        List.of("(", "3", "+", "4", ")"),
                        List.of("3", "+", "4")
                ),

                // 2. Double outer parentheses -> both must be removed
                new Case(
                        List.of("(", "(", "3", "+", "4", ")", ")"),
                        List.of("3", "+", "4")
                ),

                // 3. No outer parentheses
                new Case(
                        List.of("3", "+", "4"),
                        List.of("3", "+", "4")
                ),

                // 4. Parentheses only on a subexpression -> cannot remove
                new Case(
                        List.of("(", "3", "+", "4", ")", "*", "2"),
                        List.of("(", "3", "+", "4", ")", "*", "2")
                ),

                // 5. Independent parentheses inside -> cannot remove
                new Case(
                        List.of("(", "3", ")", "+", "(", "4", ")"),
                        List.of("(", "3", ")", "+", "(", "4", ")")
                ),

                // 6. Nested but not fully wrapping -> cannot remove
                new Case(
                        List.of("(", "(", "3", ")", ")", "+", "4"),
                        List.of("(", "(", "3", ")", ")", "+", "4")
                ),

                // 7. Outer parentheses around a single inner pair
                new Case(
                        List.of("(", "(", "3", ")", ")"),
                        List.of("3")  // final result must be a leaf
                ),

                // 8. Only parentheses — "()" -> becomes empty list
                new Case(
                        List.of("(", ")"),
                        List.of()
                ),

                // 9. Length 1 never changes
                new Case(
                        List.of("42"),
                        List.of("42")
                ),

                // 10. Length 2 but not parentheses pair
                new Case(
                        List.of("3", ")"),
                        List.of("3", ")")
                )
        );

        int ok = 0;
        int fail = 0;

        for (Case test : tests) {
            List<String> result = stripOuterParentheses(test.input);
            if (result.equals(test.expected)) {
                System.out.println("✔  OK: " + test.input + " → " + result);
                ok++;
            } else {
                System.out.println("❌ FAIL: " + test.input);
                System.out.println("   expected: " + test.expected);
                System.out.println("   got:      " + result);
                fail++;
            }
        }

        System.out.println("\nSummary: OK=" + ok + ", FAIL=" + fail);
    }

    static void testBuildTree() {
        System.out.println("=== Testing buildTree() ===");

        record Case(List<String> tokens, String expectedTree, boolean shouldFail) {
        }

        List<Case> tests = List.of(
                // ----------- SIMPLE CASES -----------
                new Case(List.of("3"), "(3)", false),
                new Case(List.of("-", "5"), "(- (5))", false),
                new Case(List.of("3", "+", "4"), "(+ (3) (4))", false),
                new Case(List.of("3", "*", "4"), "(* (3) (4))", false),

                // ----------- PARENTHESES STRIP -----------
                new Case(List.of("(", "3", ")"), "(3)", false),
                new Case(List.of("(", "(", "3", ")", ")"), "(3)", false),
                new Case(List.of("(", "3", "+", "4", ")"), "(+ (3) (4))", false),

                // ----------- MIXED OPERATORS -----------
                new Case(List.of("3", "+", "4", "*", "5"), "(+ (3) (* (4) (5)))", false),
                new Case(List.of("3", "*", "4", "+", "5"), "(+ (* (3) (4)) (5))", false),

                // ----------- UNARY CASES -----------
                new Case(List.of("-", "3", "+", "5"), "(+ (- (3)) (5))", false),
                new Case(List.of("3", "-", "-", "5"), "( - (3) (- (5)) )", false), // after unary normalization

                // ----------- COMPLEX EXPRESSIONS -----------
                new Case(List.of("3", "-", "-", "5"), "(- (3) (- (5)))", false),
                new Case(List.of("-", "(", "3", "+", "2", ")"), "(- (+ (3) (2)))", false),
                new Case(List.of("3", "*", "(", "2", "+", "5", ")"), "(* (3) (+ (2) (5)))", false),
                new Case(List.of("(", "3", "+", "2", ")", "*", "4"), "(* (+ (3) (2)) (4))", false),
                new Case(List.of("3", "/", "2", "/", "2"), "(/ (/ (3) (2)) (2))", false), // left associativity

                // ----------- INVALID CASES -----------
                new Case(List.of("3", ")", "4"), null, true),
                new Case(List.of("3", "+"), null, true),
                new Case(List.of("(", ")"), null, true),
                new Case(List.of("(", "3"), null, true)
        );

        int ok = 0;
        int fail = 0;

        for (Case c : tests) {
            try {
                Node result = buildTree(c.tokens);
                String tree = printTree(result);

                if (c.shouldFail) {
                    System.out.println("❌ FAIL (expected exception): " + c.tokens);
                    fail++;
                } else if (tree.equals(c.expectedTree)) {
                    System.out.println("✔ OK: " + c.tokens + " → " + tree);
                    ok++;
                } else {
                    System.out.println("❌ FAIL: " + c.tokens);
                    System.out.println("   expected: " + c.expectedTree);
                    System.out.println("   got:      " + tree);
                    fail++;
                }

            } catch (Exception e) {
                if (c.shouldFail) {
                    System.out.println("✔ OK (caught expected exception): " + c.tokens);
                    ok++;
                } else {
                    System.out.println("❌ FAIL (unexpected exception): " + c.tokens);
                    e.printStackTrace();
                    fail++;
                }
            }
        }

        System.out.println("\nSummary: OK=" + ok + " FAIL=" + fail);
    }

    private static String printTree(Node n) {
        if (n == null) return "";

        // leaf: just a value
        if (n.left == null && n.right == null) {
            return "(" + n.value + ")";
        }

        // unary operator: only right child is used
        if (n.left == null) {
            return "(" + n.value + " " + printTree(n.right) + ")";
        }

        // (на всякий случай) если когда-нибудь будет только левый ребёнок
        if (n.right == null) {
            return "(" + n.value + " " + printTree(n.left) + ")";
        }

        // binary operator
        return "(" + n.value + " " + printTree(n.left) + " " + printTree(n.right) + ")";
    }

    // endregion
}