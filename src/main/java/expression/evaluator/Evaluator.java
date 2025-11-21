package expression.evaluator;

import expression.ast.Node;

public class Evaluator {

    public double evaluate(Node root) {
        return evaluateTree(root);
    }

    private double evaluateTree(Node root) {
        if (root instanceof Node.NumberNode numberNode) {
            return numberNode.value();
        }

        if (root instanceof Node.UnaryNode unaryNode) {
            return switch (unaryNode.operator()) {
                case "-" -> -evaluateTree(unaryNode.operand());
                case "+" -> evaluateTree(unaryNode.operand());
                default -> throw new IllegalArgumentException("Unknown unary op: " + unaryNode.operator());
            };
        }

        if (root instanceof Node.BinaryNode binaryNode) {
            return switch (binaryNode.operator()) {
                case "+" -> evaluateTree(binaryNode.left()) + evaluateTree(binaryNode.right());
                case "-" -> evaluateTree(binaryNode.left()) - evaluateTree(binaryNode.right());
                case "*" -> evaluateTree(binaryNode.left()) * evaluateTree(binaryNode.right());
                case "/" -> evaluateTree(binaryNode.left()) / evaluateTree(binaryNode.right());
                default -> throw new IllegalArgumentException("Unknown binary op: " + binaryNode.operator());
            };
        }

        throw new IllegalStateException("Unknown node: " + root);
    }

    // FIXME: Method got broken after introducing sealed interface Node
    // It'not important as it will be completely re-written for pretty print soon.
//    private String printTree(Node n) {
//        if (n == null) return "";
//
//        // leaf: just a value
//        if (n.left == null && n.right == null) {
//            return "(" + n.value + ")";
//        }
//
//        // unary operator: only right child is used
//        if (n.left == null) {
//            return "(" + n.value + " " + printTree(n.right) + ")";
//        }
//
//        // (на всякий случай) если когда-нибудь будет только левый ребёнок
//        if (n.right == null) {
//            return "(" + n.value + " " + printTree(n.left) + ")";
//        }
//
//        // binary operator
//        return "(" + n.value + " " + printTree(n.left) + " " + printTree(n.right) + ")";
//    }
}