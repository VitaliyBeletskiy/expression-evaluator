package expression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionEvaluatorTests {

    private double eval(String expression) {
        return ExpressionEvaluator.evaluate(expression);
    }

    // to avoid unexpected floating-point precision issues
    private void assertEval(double expected, String expr) {
        assertEquals(expected, eval(expr), 1e-9);
    }

    // region Basic arithmetic tests
    @ParameterizedTest
    @CsvSource({
            "5, 2 + 3",
            "8, 10 - 2",
            "12, 3 * 4",
            "2, 8 / 4"
    })
    void basicArithmeticTests(double expected, String expression) {
        assertEval(expected, expression);
    }
    // endregion

    // region Operator precedence tests
    @ParameterizedTest
    @CsvSource({
            "14.0, 2 + 3 * 4",
            "8.0, 10 - 6 / 3",
            "19.0, 2 + 3 * 5 + 2",
            "5.0, 20 / 4 / 1"
    })
    void operatorPrecedenceTests(double expected, String expression) {
        assertEval(expected, expression);
    }
    // endregion

    // region Unary operator tests
    @ParameterizedTest
    @CsvSource({
            "-3.0, -3",
            "3.0, +3",
            "3.0, --3",
            "-3.0, +--+-3",
            "-5.0, -(2 + 3)",
            "-6.0, -3 * 2",
            "-8.0, 4 * -2"
    })
    void unaryOperatorTests(double expected, String expression) {
        assertEval(expected, expression);
    }
    // endregion

    // region Parentheses tests
    @ParameterizedTest
    @CsvSource({
            "20.0, (2 + 3) * 4",
            "14.0, 2 * (3 + (1 + 3))",
            "7.0, (((((7)))))",
            "15.0, 3 + (4 * 3)",
            "7.0, (10 - 3)",
            "-15.0, -(3 * (2 + 3))"
    })
    void parenthesesTests(double expected, String expression) {
        assertEval(expected, expression);
    }
    // endregion

    // region Invalid syntax tests
    @ParameterizedTest
    @ValueSource(strings = {
            "1 +",
            "3 + * 4",
            "(3 + 2",
            "3 + 2)",
            "3 + $4",
            "3.14.15",
            "3..5",
            "3+)4",
            "3**4",
            "3 4",
            "1 + 3 4",
            "(3",
            "()",
            "+"
    })
    void invalidSyntaxTests(String expr) {
        assertThrows(IllegalArgumentException.class, () -> eval(expr));
    }
    // endregion

    // region деление на ноль (IEEE 754)
    @Test
    void divisionByZeroPositive() {
        assertEval(Double.POSITIVE_INFINITY, "3 / 0");
    }

    @Test
    void divisionByZeroNegative() {
        assertEval(Double.NEGATIVE_INFINITY, "-3 / 0");
    }

    @Test
    void zeroDividedByZeroIsNaN() {
        assertTrue(Double.isNaN(eval("0 / 0")));
    }
    // endregion

    // region Long expression tests
    @ParameterizedTest
    @CsvSource({
            "42.0, 1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 6",
            "10.0, 1 + 2 * 3 + 4 / 2 + 3 - 1 * 2",
            "10.0, 1 + 2 + -3 + 4 + --6",
            "1024.0, 2 * 2 * 2 * 2 * 2 * 2 * 2 * 2 * 2 * 2"
    })
    void longExpressionTests(double expected, String expression) {
        assertEval(expected, expression);
    }

    @Test
    void repeatedPattern() {
        String expr = "1";
        for (int i = 0; i < 20; i++) {
            expr += " + 1";
        }
        assertEval(21.0, expr);
    }
    // endregion

    // region Huge tests from the previous implementation
    @ParameterizedTest
    @CsvSource({
            "23,    3+4*5",
            "35,    (3+4)*5",
            "21,    3*(2+5)",
            "11,    3*2+5",
            "13,    3+(2*5)",

            "5,     --5",
            "-5,    +-5",
            "8,     3--5",
            "8,     3-(-5)",
            "5,     -(-5)",
            "-5,    -(3+2)",

            "2,     3-(2-(1))",
            "2.5,   (3+2)/(1+1)",
            "0.75,  3/2/2",

            "-5,    -----5",
            "5,     +++++5",
            "-5,    +-+-+-5",
            "-5,    -+-+--+-5",
            "-2,    3---5",
            "8,     3++++5",
            "-2,    3+-+--+5",
            "-2,    3-+-+-5",
            "5,     -(---5)",
            "5,     -(-(-(-5)))",

            "3,     ((((((3))))))",
            "47,    (1+(2*(3+(4*(5)))))",
            "21,    ((3+(4))*(2+(1)))",
            "20,    (((((3+2))))*(((4))))",

            "-5,    -(3+2)",
            "-5,    +-(3+2)",
            "5,     --(3+2)",
            "-5,    -(--(3+2))",

            "-15,   3*-5",
            "-0.6,  3/-5",
            "-9,    3*-(2+1)",
            "13,    3--(2*5)",
            "8,     3++(+5)",

            "26,    3*2+5*4",
            "84,    3*(2+5)*4",
            "26,    (3*2)+(5*4)",
            "66,    3*(2+(5*4))",
            "5.25,  (3*(2+5))/((4-2)*(1+1))"
    })
    void positiveTests(double expected, String expression) {
        assertEval(expected, expression);
    }
    // endregion
}
