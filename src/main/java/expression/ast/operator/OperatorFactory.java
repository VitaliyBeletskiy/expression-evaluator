package expression.ast.operator;

import java.util.Map;

public final class OperatorFactory {

    private static final Map<String, Operator> UNARY = Map.of(
            "+", new UnaryPlus(),
            "-", new UnaryMinus()
    );

    private static final Map<String, Operator> BINARY = Map.of(
            "+", new Add(),
            "-", new Sub(),
            "*", new Mul(),
            "/", new Div()
    );

    private OperatorFactory() {
        // static factory class
    }

    public static Operator fromSymbol(String symbol, int arity) {
        return switch(arity) {
            case 1 -> UNARY.get(symbol);
            case 2 -> BINARY.get(symbol);
            default -> throw new IllegalArgumentException("Invalid operator arity: " + arity);
        };
    }

    public static boolean isUnary(String symbol) {
        return UNARY.containsKey(symbol);
    }

    public static boolean isBinary(String symbol) {
        return BINARY.containsKey(symbol);
    }
}
