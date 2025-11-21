package expression.ast;

public sealed interface Node permits Node.NumberNode, Node.UnaryNode, Node.BinaryNode {

    record NumberNode(double value) implements Node {
    }

    record UnaryNode(String operator, Node operand) implements Node {
    }

    record BinaryNode(String operator, Node left, Node right) implements Node {
    }
}
