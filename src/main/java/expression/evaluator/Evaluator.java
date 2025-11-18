package expression.evaluator;

import expression.parser.Node;

public class Evaluator {

    public double evaluate(Node root) {
        return evaluateTree(root);
    }

    private double evaluateTree(Node root) {
        // leaf: just a value
        if (root.left == null && root.right == null) {
            return Double.parseDouble(root.value);
        }

        // unary operator: only right child is used
        if (root.left == null) {
            return root.value.equals("-")
                    ? -evaluateTree(root.right)
                    : evaluateTree(root.right);
        }

        // just in case (I'm aware that it's redundant)
        if (root.right == null) {
            throw new IllegalArgumentException("Illegal node - right == null: " + printTree(root));
        }

        // full node
        return switch (root.value) {
            case "+" -> evaluateTree(root.left) + evaluateTree(root.right);
            case "-" -> evaluateTree(root.left) - evaluateTree(root.right);
            case "*" -> evaluateTree(root.left) * evaluateTree(root.right);
            case "/" -> evaluateTree(root.left) / evaluateTree(root.right);
            default -> throw new IllegalArgumentException("Unexpected operator in node: " + printTree(root));
        };
    }

    private String printTree(Node n) {
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
}