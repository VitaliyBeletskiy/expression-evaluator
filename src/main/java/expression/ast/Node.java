package expression.ast;

import expression.ast.operator.Operator;

public sealed interface Node permits Node.NumberNode, Node.UnaryNode, Node.BinaryNode {

    record NumberNode(double value) implements Node {
    }

    record UnaryNode(Operator operator, Node operand) implements Node {
    }

    record BinaryNode(Operator operator, Node left, Node right) implements Node {
    }
}
