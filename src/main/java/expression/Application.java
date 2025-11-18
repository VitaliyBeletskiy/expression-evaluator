package expression;

import java.util.*;

public class Application {

    public static void main(String[] args) {
        // Uncomment to run manual mode:
//        runManual();

        // Run automated tests:
        runFullFlowTests();
    }

    private static void runManual() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Please enter the expression to calculate:");
        String input = scanner.nextLine();
        try {
            double result = ExpressionEvaluator.evaluate(input);
            System.out.println("Result: " + result);
        } catch (Exception e) {
            System.out.println("Error. " + e);
        }

    }

    // ================= FULL FLOW TESTING =================

    static class Case {
        final String input;
        final Double expected;      // null -> should fail
        final boolean shouldFail;

        Case(String input, Double expected, boolean shouldFail) {
            this.input = input;
            this.expected = expected;
            this.shouldFail = shouldFail;
        }
    }

    private static void runFullFlowTests() {
        System.out.println("=== FULL FLOW TESTS ( normalize → tokenize → unary → parse → calc ) ===");

        List<Case> tests = List.of(
                // -------- Basic numbers -------
                new Case("3", 3.0, false),
                new Case("  42  ", 42.0, false),
                new Case("-5", -5.0, false),
                new Case("+5", 5.0, false),

                // -------- Simple arithmetic -------
                new Case("3+4", 7.0, false),
                new Case("10-3", 7.0, false),
                new Case("2*5", 10.0, false),
                new Case("8/4", 2.0, false),

                // -------- Mixed precedence -------
                new Case("3+4*5", 3 + 4 * 5.0, false),
                new Case("(3+4)*5", (3 + 4) * 5.0, false),
                new Case("3*(2+5)", 3 * (2 + 5.0), false),
                new Case("3*2+5", 3 * 2 + 5.0, false),
                new Case("3+(2*5)", 3 + (2 * 5.0), false),

                // -------- Unary operators -------
                new Case("--5", 5.0, false),
                new Case("+-5", -5.0, false),
                new Case("3--5", 3 - -5.0, false),
                new Case("3-(-5)", 3 - (-5.0), false),
                new Case("-(-5)", 5.0, false),
                new Case("-(3+2)", -(3 + 2.0), false),

                // -------- Complex combinations -------
                new Case("3-(2-(1))", 3 - (2 - 1.0), false),
                new Case("(3+2)/(1+1)", (3 + 2.0) / (1 + 1.0), false),
                new Case("3/2/2", 3.0 / 2.0 / 2.0, false), // left associative

                // -------- Invalid expressions -------
                new Case("3+", null, true),
                new Case("+", null, true),
                new Case("()", null, true),
                new Case("(3", null, true),
                new Case("3 4", null, true),
                new Case("3**4", null, true),
                new Case("3*/4", null, true),
                new Case("3+)4", null, true),
                new Case("3 +$ 5", null, true),
                new Case("3..5", null, true),
                new Case("3.1.2", null, true),

                // 1. Унарники на пределе здравого смысла
                new Case("-----5", -5.0, false),
                new Case("+++++5", 5.0, false),
                new Case("+-+-+-5", -5.0, false),
                new Case("-+-+--+-5", -5.0, false),
                new Case("3---5", 3 + -5.0, false),
                new Case("3++++5", 3 + 5.0, false),
                new Case("3+-+--+5", 3 + -5.0, false),
                new Case("3-+-+-5", 3 - (5.0), false),
                new Case("-(---5)", -(-(- -5.0)), false),
                new Case("-(-(-(-5)))", -(-(-(-5.0))), false),

                // 2. Глубоко вложенные скобки
                new Case("((((((3))))))", 3.0, false),
                new Case("(1+(2*(3+(4*(5)))))", 1 + (2 * (3 + (4 * 5.0))), false),
                new Case("((3+(4))*(2+(1)))", (3 + 4.0) * (2 + 1.0), false),
                new Case("(((((3+2))))*(((4))))", (3 + 2.0) * (4.0), false),

                // 3. Унарники перед скобками
                new Case("-(3+2)", -(3 + 2.0), false),
                new Case("+-(3+2)", -(3 + 2.0), false),
                new Case("--(3+2)", (3 + 2.0), false),
                new Case("-(--(3+2))", -(+(3 + 2.0)), false),

                // 4. Комбинации унарных и бинарных операторов, которые обычно ломают парсер
                new Case("3*-5", 3 * -5.0, false),
                new Case("3/-5", 3 / -5.0, false),
                new Case("3*-(2+1)", 3 * -(2 + 1.0), false),
                new Case("3--(2*5)", 3 - -(2 * 5.0), false),
                new Case("3++(+5)", 3 + 5.0, false),

                // 5. Безумные комбинации вложений и приоритетов
                new Case("3*2+5*4", 3 * 2 + 5 * 4.0, false),
                new Case("3*(2+5)*4", 3 * (2 + 5.0) * 4, false),
                new Case("(3*2)+(5*4)", (3 * 2.0) + (5 * 4.0), false),
                new Case("3*(2+(5*4))", 3 * (2 + (5 * 4.0)), false),
                new Case("(3*(2+5))/((4-2)*(1+1))", (3 * (2 + 5.0)) / ((4 - 2.0) * (1 + 1.0)), false)
        );

        int ok = 0;
        int fail = 0;

        for (Case c : tests) {
            try {
                double result = ExpressionEvaluator.evaluate(c.input);

                if (c.shouldFail) {
                    System.out.println("❌ FAIL (expected exception): " + c.input);
                    fail++;
                } else if (Math.abs(result - c.expected) < 1e-9) {
                    System.out.println("✔ OK: " + c.input + " = " + result);
                    ok++;
                } else {
                    System.out.println("❌ FAIL: " + c.input);
                    System.out.println("   expected: " + c.expected);
                    System.out.println("   got:      " + result);
                    fail++;
                }

            } catch (Exception e) {
                if (c.shouldFail) {
                    System.out.println("✔ OK (exception as expected): " + c.input);
                    ok++;
                } else {
                    System.out.println("❌ FAIL (unexpected exception): " + c.input);
                    e.printStackTrace();
                    fail++;
                }
            }
        }

        System.out.println("\nSummary: OK=" + ok + " FAIL=" + fail);
    }
}