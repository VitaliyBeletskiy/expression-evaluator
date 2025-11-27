package expression.ast.operator;

public sealed interface Operator permits UnaryPlus, UnaryMinus, Add, Sub, Mul, Div {

    /**
     * @return the symbol representing the operator, e.g. "+", "-".
     */
    String symbol();

    /**
     * @return operator arity: 1 for unary, 2 for binary.
     */
    int arity();

    /**
     * Applies a unary operator to the operand.
     * Operators that are not unary must override arity()=2
     * and will throw UnsupportedOperationException here.
     */
    default double apply(double a) {
        throw new UnsupportedOperationException(
                "Unary apply() not supported by operator: " + symbol()
        );
    }

    /**
     * Applies a binary operator to two operands.
     * Operators that are unary must override arity()=1
     * and will throw UnsupportedOperationException here.
     */
    default double apply(double a, double b) {
        throw new UnsupportedOperationException(
                "Binary apply() not supported by operator: " + symbol()
        );
    }
}
