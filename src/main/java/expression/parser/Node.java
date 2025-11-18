package expression.parser;

public final class Node {
    public final String value;
    public final Node left;
    public final Node right;

    public Node(String value, Node left, Node right) {
        this.value = value;
        this.left = left;
        this.right = right;
    }
}
