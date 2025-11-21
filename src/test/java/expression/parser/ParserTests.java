package expression.parser;

import expression.ast.Node;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTests {

    private final Parser parser = new Parser();

    private Node parse(String... tokens) {
        return parser.parse(new ArrayList<>(List.of(tokens)));
    }

    // ---------- Assertion helpers ----------

    private static Node.NumberNode assertNum(Node n, double expected) {
        assertInstanceOf(Node.NumberNode.class, n, "Expected NumberNode, got " + n);
        Node.NumberNode nn = (Node.NumberNode) n;
        assertEquals(expected, nn.value(), 1e-9);
        return nn;
    }

    private static Node.BinaryNode assertBinary(Node n, String op) {
        assertInstanceOf(Node.BinaryNode.class, n, "Expected BinaryNode, got " + n);
        Node.BinaryNode bn = (Node.BinaryNode) n;
        assertEquals(op, bn.operator());
        return bn;
    }

    private static Node.UnaryNode assertUnary(Node n, String op) {
        assertInstanceOf(Node.UnaryNode.class, n, "Expected UnaryNode, got " + n);
        Node.UnaryNode un = (Node.UnaryNode) n;
        assertEquals(op, un.operator());
        return un;
    }

    // ---------- Tests ----------

    @Test
    void multiplicationOnLeftSideOfAddition() {
        Node root = parse("2", "*", "3", "+", "4");

        var plus = assertBinary(root, "+");

        var mul = assertBinary(plus.left(), "*");
        assertNum(mul.left(), 2);
        assertNum(mul.right(), 3);

        assertNum(plus.right(), 4);
    }

    @Test
    void mixedOperatorsChain() {
        Node root = parse("2", "+", "3", "*", "4", "-", "5");

        var sub = assertBinary(root, "-");
        var add = assertBinary(sub.left(), "+");
        assertNum(sub.right(), 5);

        var mul = assertBinary(add.right(), "*");
        assertNum(mul.left(), 3);
        assertNum(mul.right(), 4);
    }

    @Test
    void parenthesesOverridePrecedenceLeft() {
        Node root = parse("(", "2", "+", "3", ")", "*", "4");

        var mul = assertBinary(root, "*");

        var add = assertBinary(mul.left(), "+");
        assertNum(add.left(), 2);
        assertNum(add.right(), 3);

        assertNum(mul.right(), 4);
    }

    @Test
    void parenthesesOverridePrecedenceRight() {
        Node root = parse("2", "*", "(", "3", "+", "4", ")");

        var mul = assertBinary(root, "*");
        assertNum(mul.left(), 2);

        var add = assertBinary(mul.right(), "+");
        assertNum(add.left(), 3);
        assertNum(add.right(), 4);
    }

    @Test
    void nestedParentheses() {
        Node root = parse("(", "(", "1", "+", "2", ")", "*", "3", ")", "-", "4");

        var minus = assertBinary(root, "-");
        assertNum(minus.right(), 4);

        var mul = assertBinary(minus.left(), "*");

        var plus = assertBinary(mul.left(), "+");
        assertNum(plus.left(), 1);
        assertNum(plus.right(), 2);

        assertNum(mul.right(), 3);
    }

    @Test
    void unaryMinusBeforeNumber() {
        Node root = parse("-", "3");

        var unary = assertUnary(root, "-");
        assertNum(unary.operand(), 3);
    }

    @Test
    void unaryPlusBeforeNumber() {
        Node root = parse("+", "3");

        var unary = assertUnary(root, "+");
        assertNum(unary.operand(), 3);
    }

    @Test
    void chainOfUnaryOperatorsCollapsesToSingleOperator() {
        Node root = parse("-", "-", "-", "5");

        var unary = assertUnary(root, "-");
        assertNum(unary.operand(), 5);
    }

    @Test
    void unaryBeforeGroup() {
        Node root = parse("-", "(", "3", "+", "2", ")");

        var unary = assertUnary(root, "-");

        var add = assertBinary(unary.operand(), "+");
        assertNum(add.left(), 3);
        assertNum(add.right(), 2);
    }

    @Test
    void unaryAndBinaryMixed() {
        Node root = parse("2", "*", "-", "3");

        var mul = assertBinary(root, "*");
        assertNum(mul.left(), 2);

        var unary = assertUnary(mul.right(), "-");
        assertNum(unary.operand(), 3);
    }

    @Test
    void unaryChainInsideBinaryExpression() {
        Node root = parse("10", "-", "-", "-", "3");

        var minus = assertBinary(root, "-");
        assertNum(minus.left(), 10);
        assertNum(minus.right(), 3);
    }

    @Test
    void unaryInsideNestedGroup() {
        Node root = parse("(", "2", "+", "-", "(", "3", "*", "4", ")", ")");

        var minus = assertBinary(root, "-");
        assertNum(minus.left(), 2);

        var mul = assertBinary(minus.right(), "*");
        assertNum(mul.left(), 3);
        assertNum(mul.right(), 4);
    }

    // -------- Error cases --------

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
}
