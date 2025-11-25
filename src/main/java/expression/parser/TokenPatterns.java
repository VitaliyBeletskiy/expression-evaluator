package expression.parser;

import java.util.regex.Pattern;

public final class TokenPatterns {

    // private constructor
    private TokenPatterns() {}

    private static final Pattern NUMBER = Pattern.compile("\\d+(\\.\\d+)?");
    private static final Pattern OPERATOR = Pattern.compile("[+\\-*/]");
    private static final Pattern PAREN = Pattern.compile("[()]");
    private static final Pattern UNARY_CHAIN = Pattern.compile("[+\\-]+");

    public static boolean isNumber(String s) {
        return NUMBER.matcher(s).matches();
    }

    public static boolean isOperator(String s) {
        return OPERATOR.matcher(s).matches();
    }

    public static boolean isUnaryChain(String s) {
        return UNARY_CHAIN.matcher(s).matches();
    }

    public static boolean isParenthesis(String s) {
        return PAREN.matcher(s).matches();
    }

    public static boolean isUnaryOperator(String s) {
        return "+".equals(s) || "-".equals(s);
    }

    public static boolean isMultiplicativeOperator(String s) {
        return "*".equals(s) || "/".equals(s);
    }
}
