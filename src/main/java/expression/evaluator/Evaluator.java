package expression.evaluator;

import expression.ast.Node;
import expression.ast.operator.Operator;

public class Evaluator {

    public double evaluate(Node root) {
        return evaluateTree(root);
    }

    private double evaluateTree(Node root) {
        if (root instanceof Node.NumberNode numberNode) {
            return numberNode.value();
        }

        if (root instanceof Node.UnaryNode unaryNode) {
            Operator op = unaryNode.operator();
            double value = evaluateTree(unaryNode.operand());
            return op.apply(value);
        }

        if (root instanceof Node.BinaryNode binaryNode) {
            Operator op = binaryNode.operator();
            double leftOperand = evaluateTree(binaryNode.left());
            double rightOperand = evaluateTree(binaryNode.right());
            return op.apply(leftOperand, rightOperand);
        }

        throw new IllegalStateException("Unsupported AST node type: " + root.getClass());
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