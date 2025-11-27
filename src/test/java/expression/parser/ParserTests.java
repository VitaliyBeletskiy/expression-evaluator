package expression.parser;

import expression.ast.Node;
import expression.ast.operator.Add;
import expression.ast.operator.Div;
import expression.ast.operator.Mul;
import expression.ast.operator.Operator;
import expression.ast.operator.Sub;
import expression.ast.operator.UnaryMinus;
import expression.ast.operator.UnaryPlus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTests {

    private final Parser parser = new Parser();
    private final Add addOp = new Add();
    private final Sub subOp = new Sub();
    private final Mul mulOp = new Mul();
    private final Div divOp = new Div();
    private final UnaryMinus unaryMinusOp = new UnaryMinus();
    private final UnaryPlus unaryPlusOp = new UnaryPlus();

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

    private static Node.BinaryNode assertBinary(Node n, Operator op) {
        assertInstanceOf(Node.BinaryNode.class, n, "Expected BinaryNode, got " + n);
        Node.BinaryNode bn = (Node.BinaryNode) n;
        assertEquals(op.getClass(), bn.operator().getClass());
        return bn;
    }

    private static Node.UnaryNode assertUnary(Node n, Operator op) {
        assertInstanceOf(Node.UnaryNode.class, n, "Expected UnaryNode, got " + n);
        Node.UnaryNode un = (Node.UnaryNode) n;
        assertEquals(op.getClass(), un.operator().getClass());
        return un;
    }

    // ---------- Tests ----------

    @Test
    void multiplicationOnLeftSideOfAddition() {
        Node root = parse("2", "*", "3", "+", "4");

        var plus = assertBinary(root, addOp);

        var mul = assertBinary(plus.left(), mulOp);
        assertNum(mul.left(), 2);
        assertNum(mul.right(), 3);

        assertNum(plus.right(), 4);
    }

    @Test
    void mixedOperatorsChain() {
        Node root = parse("2", "+", "3", "*", "4", "-", "5");

        var sub = assertBinary(root, subOp);
        var add = assertBinary(sub.left(), addOp);
        assertNum(sub.right(), 5);

        var mul = assertBinary(add.right(), mulOp);
        assertNum(mul.left(), 3);
        assertNum(mul.right(), 4);
    }

    @Test
    void parenthesesOverridePrecedenceLeft() {
        Node root = parse("(", "2", "+", "3", ")", "*", "4");

        var mul = assertBinary(root, mulOp);

        var add = assertBinary(mul.left(), addOp);
        assertNum(add.left(), 2);
        assertNum(add.right(), 3);

        assertNum(mul.right(), 4);
    }

    @Test
    void parenthesesOverridePrecedenceRight() {
        Node root = parse("2", "*", "(", "3", "+", "4", ")");

        var mul = assertBinary(root, mulOp);
        assertNum(mul.left(), 2);

        var add = assertBinary(mul.right(), addOp);
        assertNum(add.left(), 3);
        assertNum(add.right(), 4);
    }

    @Test
    void nestedParentheses() {
        Node root = parse("(", "(", "1", "+", "2", ")", "*", "3", ")", "-", "4");

        var minus = assertBinary(root, subOp);
        assertNum(minus.right(), 4);

        var mul = assertBinary(minus.left(), mulOp);

        var plus = assertBinary(mul.left(), addOp);
        assertNum(plus.left(), 1);
        assertNum(plus.right(), 2);

        assertNum(mul.right(), 3);
    }

    @Test
    void unaryMinusBeforeNumber() {
        Node root = parse("-", "3");

        var unary = assertUnary(root, unaryMinusOp);
        assertNum(unary.operand(), 3);
    }

    @Test
    void unaryPlusBeforeNumber() {
        Node root = parse("+", "3");

        var unary = assertUnary(root, unaryPlusOp);
        assertNum(unary.operand(), 3);
    }

    @Test
    void chainOfUnaryOperatorsCollapsesToSingleOperator() {
        Node root = parse("-", "-", "-", "5");

        var unary = assertUnary(root, unaryMinusOp);
        assertNum(unary.operand(), 5);
    }

    @Test
    void unaryBeforeGroup() {
        Node root = parse("-", "(", "3", "+", "2", ")");

        var unary = assertUnary(root, unaryMinusOp);

        var add = assertBinary(unary.operand(), addOp);
        assertNum(add.left(), 3);
        assertNum(add.right(), 2);
    }

    @Test
    void unaryAndBinaryMixed() {
        Node root = parse("2", "*", "-", "3");

        var mul = assertBinary(root, mulOp);
        assertNum(mul.left(), 2);

        var unary = assertUnary(mul.right(), unaryMinusOp);
        assertNum(unary.operand(), 3);
    }

    @Test
    void unaryChainInsideBinaryExpression() {
        Node root = parse("10", "-", "-", "-", "3");

        var minus = assertBinary(root, subOp);
        assertNum(minus.left(), 10);
        assertNum(minus.right(), 3);
    }

    @Test
    void unaryInsideNestedGroup() {
        Node root = parse("(", "2", "+", "-", "(", "3", "*", "4", ")", ")");

        var minus = assertBinary(root, subOp);
        assertNum(minus.left(), 2);

        var mul = assertBinary(minus.right(), mulOp);
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
