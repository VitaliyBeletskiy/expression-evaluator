package expression.ast.operator;

public final class Div implements Operator {
    @Override
    public String symbol() {
        return "/";
    }

    @Override
    public int arity() {
        return 2;
    }

    @Override
    public double apply(double a, double b) {
        return a / b;
    }
}
