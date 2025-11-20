package expression.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParserTests {

    private Parser parser;

    @BeforeEach
    void setup() {
        parser = new Parser();
    }

    private Node parse(String... tokens) {
        return parser.parse(List.of(tokens));
    }

    @Test
    void parsesSimpleBinaryExpression() {
        Node root = parse("2", "+", "3");
        assertEquals("+", root.value);
        assertEquals("2", root.left.value);
        assertEquals("3", root.right.value);
    }

    @Test
    void parsesSimpleAddition() {
        Node root = parse("2", "+", "3");

        assertEquals("+", root.value);
        assertEquals("2", root.left.value);
        assertEquals("3", root.right.value);
    }

    @Test
    void parsesSimpleSubtraction() {
        Node root = parse("10", "-", "4");

        assertEquals("-", root.value);
        assertEquals("10", root.left.value);
        assertEquals("4", root.right.value);
    }

    @Test
    void parsesSimpleMultiplication() {
        Node root = parse("6", "*", "7");

        assertEquals("*", root.value);
        assertEquals("6", root.left.value);
        assertEquals("7", root.right.value);
    }

    @Test
    void parsesSimpleDivision() {
        Node root = parse("8", "/", "2");

        assertEquals("/", root.value);
        assertEquals("8", root.left.value);
        assertEquals("2", root.right.value);
    }

    @Test
    void multiplicationHasHigherPrecedenceThanAddition() {
        Node root = parse("2", "+", "3", "*", "4");

        // Рут — сложение
        assertEquals("+", root.value);

        // Левый операнд: число
        assertEquals("2", root.left.value);

        // Правый операнд — узел "*"
        assertEquals("*", root.right.value);

        // Проверяем дерево справа
        assertEquals("3", root.right.left.value);
        assertEquals("4", root.right.right.value);
    }

    @Test
    void multiplicationOnLeftSideOfAddition() {
        Node root = parse("2", "*", "3", "+", "4");

        // Корень — +
        assertEquals("+", root.value);

        // Левое поддерево — умножение
        assertEquals("*", root.left.value);
        assertEquals("2", root.left.left.value);
        assertEquals("3", root.left.right.value);

        assertEquals("4", root.right.value);
    }

    @Test
    void mixedOperatorsChain() {
        Node root = parse("2", "+", "3", "*", "4", "-", "5");

        assertEquals("-", root.value);        // последняя + первая низкоприоритетная
        assertEquals("+", root.left.value);   // левое поддерево — сложение 2 + (3*4)
        assertEquals("5", root.right.value);

        // внутри (3*4)
        Node mul = root.left.right;
        assertEquals("*", mul.value);
        assertEquals("3", mul.left.value);
        assertEquals("4", mul.right.value);
    }

    @Test
    void parenthesesOverridePrecedenceLeft() {
        Node root = parse("(", "2", "+", "3", ")", "*", "4");

        assertEquals("*", root.value);

        // внутри скобок — +
        assertEquals("+", root.left.value);
        assertEquals("2", root.left.left.value);
        assertEquals("3", root.left.right.value);

        assertEquals("4", root.right.value);
    }

    @Test
    void parenthesesOverridePrecedenceRight() {
        Node root = parse("2", "*", "(", "3", "+", "4", ")");

        assertEquals("*", root.value);
        assertEquals("2", root.left.value);

        assertEquals("+", root.right.value);
        assertEquals("3", root.right.left.value);
        assertEquals("4", root.right.right.value);
    }

    @Test
    void nestedParentheses() {
        Node root = parse("(", "(", "1", "+", "2", ")", "*", "3", ")", "-", "4");

        assertEquals("-", root.value);
        assertEquals("4", root.right.value);

        Node mul = root.left;
        assertEquals("*", mul.value);

        Node innerPlus = mul.left;
        assertEquals("+", innerPlus.value);
        assertEquals("1", innerPlus.left.value);
        assertEquals("2", innerPlus.right.value);

        assertEquals("3", mul.right.value);
    }

    @Test
    void unaryMinusBeforeNumber() {
        Node root = parse("-", "3");

        assertEquals("-", root.value);
        assertNull(root.left);
        assertEquals("3", root.right.value);
    }

    @Test
    void unaryPlusBeforeNumber() {
        Node root = parse("+", "3");

        assertEquals("+", root.value);
        assertNull(root.left);
        assertEquals("3", root.right.value);
    }

    @Test
    void chainOfUnaryOperatorsCollapsesToSingleOperator() {
        Node root = parse("-", "-", "-", "5");

        // После нормализации остался один унарный минус
        assertEquals("-", root.value);
        assertNull(root.left);
        assertEquals("5", root.right.value);
    }

    @Test
    void unaryBeforeGroup() {
        Node root = parse("-", "(", "3", "+", "2", ")");

        assertEquals("-", root.value);
        assertNull(root.left);

        Node inner = root.right;
        assertEquals("+", inner.value);
        assertEquals("3", inner.left.value);
        assertEquals("2", inner.right.value);
    }

    @Test
    void unaryAndBinaryMixed() {
        Node root = parse("2", "*", "-", "3");

        assertEquals("*", root.value);
        assertEquals("2", root.left.value);

        Node unary = root.right;
        assertEquals("-", unary.value);
        assertEquals("3", unary.right.value);
    }

    @Test
    void unaryInsideComplexExpressionCollapses() {
        Node root = parse("10", "-", "-", "(", "2", "*", "3", ")");

        // Корень — бинарный минус
        assertEquals("+", root.value);
        assertEquals("10", root.left.value);

        // Унарная цепочка схлопнулась, значит справа сразу *
        Node mul = root.right;
        assertEquals("*", mul.value);
        assertEquals("2", mul.left.value);
        assertEquals("3", mul.right.value);
    }

    @Test
    void unaryChainWithoutOperandShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("-", "-", "+"));
    }

    @Test
    void unaryBeforeBinaryWithoutNumberShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("-", "*", "3"));
    }

    @Test
    void emptyParenthesesShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("(", ")"));
    }

    @Test
    void missingClosingParenthesisShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("(", "2", "+", "3"));
    }

    @Test
    void missingOpeningParenthesisShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("2", "+", "3", ")"));
    }

    @Test
    void twoBinaryOperatorsInRowShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("2", "+", "*", "3"));
    }

    @Test
    void operandMissingBetweenOperatorsShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("2", "-", "*", "3"));
    }

    @Test
    void expressionCannotStartWithBinaryOperator() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("*", "3", "+", "2"));
    }

    @Test
    void expressionCannotEndWithOperator() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("2", "+"));
    }

    @Test
    void standaloneOperatorShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("+"));
    }

    @Test
    void standaloneParenthesisShouldFail() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("("));
    }

    @Test
    void unaryBeforeBinaryInsideExpression() {
        Node root = parse("2", "+", "-", "3");
        assertEquals("-", root.value);
        assertEquals("2", root.left.value);
        assertEquals("3", root.right.value);
    }

    // FIXME: something is wrong here. Result is correct but the tree looks weird.
    // Check again after introducing unaryNode and so on.
    @Test
    void unaryBeforeGroupInsideBinaryExpression() {
        Node root = parse("2", "*", "-", "(", "3", "+", "4", ")");
        assertEquals("*", root.value);
        assertEquals("2", root.left.value);

        Node mulRight = root.right;
        assertEquals("+", mulRight.value);
        assertEquals("3", mulRight.left.value);
        assertEquals("4", mulRight.right.value);
    }

    @Test
    void unaryChainInsideBinaryExpression() {
        Node root = parse("10", "-", "-", "-", "3");
        assertEquals("-", root.value);
        assertEquals("10", root.left.value);
        assertEquals("3", root.right.value); // цепочка схлопнута
    }

    @Test
    void unaryInsideNestedGroup() {
        Node root = parse("(", "2", "+", "-", "(", "3", "*", "4", ")", ")");

        // 2 + -(...)  → 2 - (...)
        assertEquals("-", root.value);

        assertEquals("2", root.left.value);

        Node inner = root.right;
        assertEquals("*", inner.value);
        assertEquals("3", inner.left.value);
        assertEquals("4", inner.right.value);
    }

    @Test
    void unaryAtStartWithMixedOperators() {
        Node root = parse("-", "2", "*", "3");

        // (* (- 2) 3)
        assertEquals("*", root.value);

        Node left = root.left;
        assertEquals("-", left.value);
        assertNull(left.left);
        assertEquals("2", left.right.value);

        assertEquals("3", root.right.value);
    }
}
