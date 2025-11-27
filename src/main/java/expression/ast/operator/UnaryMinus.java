package expression.ast.operator;

public final class UnaryMinus implements Operator {
    @Override
    public String symbol() {
        return "-";
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public double apply(double a) {
        return -a;
    }
}
