package expression.tokenizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenizerTests {

    private Tokenizer tokenizer;

    @BeforeEach
    void setup() {
        tokenizer = new Tokenizer();
    }

    @Test
    void simpleTokens() {
        List<String> tokens = tokenizer.tokenize("2 + 3");
        assertEquals(List.of("2", "+", "3"), tokens);
    }

    @Test
    void simpleMultiplication() {
        List<String> tokens = tokenizer.tokenize("4*5");
        assertEquals(List.of("4", "*", "5"), tokens);
    }

    @Test
    void simpleDivision() {
        List<String> tokens = tokenizer.tokenize("10 /2");
        assertEquals(List.of("10", "/", "2"), tokens);
    }

    @Test
    void decimalNumbers() {
        List<String> tokens = tokenizer.tokenize("3.14 + 2.5");
        assertEquals(List.of("3.14", "+", "2.5"), tokens);
    }

    @Test
    void decimalWithoutLeadingZero() {
        List<String> tokens = tokenizer.tokenize(".5 + .25");
        assertEquals(List.of("0.5", "+", "0.25"), tokens);
    }

    @Test
    void commaAsDecimalSeparator() {
        List<String> tokens = tokenizer.tokenize("3,5 + 2,5");
        assertEquals(List.of("3.5", "+", "2.5"), tokens);
    }

    @Test
    void decimalsWithoutSpaces() {
        List<String> tokens = tokenizer.tokenize("10.5*2.5");
        assertEquals(List.of("10.5", "*", "2.5"), tokens);
    }

    @Test
    void invalidMultipleDecimalPoints() {
        assertThrows(IllegalArgumentException.class, () -> tokenizer.tokenize("3.14.15"));
    }

    @Test
    void invalidTwoDots() {
        assertThrows(IllegalArgumentException.class, () -> tokenizer.tokenize("3..5"));
    }

    @Test
    void invalidCommaSequence() {
        assertThrows(IllegalArgumentException.class, () -> tokenizer.tokenize("3,14,15"));
    }

    @Test
    void invalidDollarSign() {
        assertThrows(IllegalArgumentException.class, () -> tokenizer.tokenize("3 + $ 4"));
    }

    @Test
    void invalidLetter() {
        assertThrows(IllegalArgumentException.class, () -> tokenizer.tokenize("3a + 4"));
    }

    @Test
    void invalidUnicodeSymbol() {
        assertThrows(IllegalArgumentException.class, () -> tokenizer.tokenize("2 ∞ 3"));
    }

    @Test
    void invalidUnderscore() {
        assertThrows(IllegalArgumentException.class, () -> tokenizer.tokenize("3_14"));
    }

    @Test
    void unarySingleMinusTokenizes() {
        assertEquals(List.of("-", "3"), tokenizer.tokenize("-3"));
    }

    @Test
    void unarySinglePlusTokenizes() {
        assertEquals(List.of("+", "3"), tokenizer.tokenize("+3"));
    }

    @Test
    void unaryDoubleMinusTokenizes() {
        assertEquals(List.of("-", "-", "3"), tokenizer.tokenize("--3"));
    }

    @Test
    void unaryMixedChainTokenizes() {
        assertEquals(List.of("+", "-", "-", "+", "-", "3"), tokenizer.tokenize("+--+-3"));
    }

    @Test
    void unaryBeforeParentheses() {
        assertEquals(
                List.of("-", "(", "2", "+", "3", ")"),
                tokenizer.tokenize("-(2+3)")
        );
    }

    @Test
    void unaryChainBeforeGroup() {
        assertEquals(
                List.of("-", "-", "(", "3", "*", "2", ")"),
                tokenizer.tokenize("--(3*2)")
        );
    }

    @Test
    void unicodeMinusVariantsAreNormalized() {
        // – (EN DASH), — (EM DASH), − (UNICODE MINUS)
        List<String> tokens = tokenizer.tokenize("3 – 2 — 1 − 5");
        assertEquals(List.of("3", "-", "2", "-", "1", "-", "5"), tokens);
    }

    @Test
    void commaConvertedToDot() {
        List<String> tokens = tokenizer.tokenize("3,5 + 1,25");
        assertEquals(List.of("3.5", "+", "1.25"), tokens);
    }

    @Test
    void multipleSpacesCollapse() {
        List<String> tokens = tokenizer.tokenize("3     +     4");
        assertEquals(List.of("3", "+", "4"), tokens);
    }

    @Test
    void mixedWhitespaceNormalized() {
        List<String> tokens = tokenizer.tokenize("3\t +\n4");
        assertEquals(List.of("3", "+", "4"), tokens);
    }

    @Test
    void nonBreakingSpaceHandled() {
        List<String> tokens = tokenizer.tokenize("3\u00A0+\u00A04");
        assertEquals(List.of("3", "+", "4"), tokens);
    }

    @Test
    void leadingAndTrailingWhitespaceIgnored() {
        List<String> tokens = tokenizer.tokenize("   3 + 4   ");
        assertEquals(List.of("3", "+", "4"), tokens);
    }

    @Test
    void unaryMinusToken() {
        assertEquals(List.of("-", "3"), tokenizer.tokenize("-3"));
    }

    @Test
    void binaryMinusToken() {
        assertEquals(List.of("3", "-", "3"), tokenizer.tokenize("3-3"));
    }

    @Test
    void binaryThenUnary() {
        assertEquals(List.of("2", "*", "-", "3"), tokenizer.tokenize("2*-3"));
    }

    @Test
    void unaryBeforeGroup() {
        assertEquals(List.of("-", "(", "3", "+", "2", ")"), tokenizer.tokenize("-(3+2)"));
    }

    @Test
    void binaryThenUnaryAdjacent() {
        assertEquals(List.of("3", "-", "-", "3"), tokenizer.tokenize("3--3"));
    }

    @Test
    void leadingZerosAllowed() {
        assertEquals(List.of("000123"), tokenizer.tokenize("000123"));
    }

    @Test
    void decimalWithoutFractionPart() {
        assertEquals(List.of("5.0"), tokenizer.tokenize("5."));
    }

    @Test
    void negativeZero() {
        assertEquals(List.of("-", "0"), tokenizer.tokenize("-0"));
    }

    @Test
    void negativeZeroDecimal() {
        assertEquals(List.of("-", "0.0"), tokenizer.tokenize("-0.0"));
    }

    @Test
    void largeNumbers() {
        List<String> tokens = tokenizer.tokenize("12345678901234567890 + 2");
        assertEquals(List.of("12345678901234567890", "+", "2"), tokens);
    }

    @Test
    void decimalManyZeros() {
        assertEquals(List.of("0.00000001"), tokenizer.tokenize("0.00000001"));
    }

    @Test
    void longExpressionTokenization() {
        List<String> tokens = tokenizer.tokenize(
                "1 + 2 + 3 + 4 + 5 + 6 + 7 + 8 + 9 + 10"
        );

        assertEquals(
                List.of("1", "+", "2", "+", "3", "+", "4", "+", "5", "+", "6", "+", "7", "+", "8", "+", "9", "+", "10"),
                tokens
        );
    }

    @Test
    void longMultiplicationChain() {
        List<String> tokens = tokenizer.tokenize(
                "2 * 2 * 2 * 2 * 2 * 2 * 2 * 2 * 2 * 2"
        );

        assertEquals(
                List.of("2", "*", "2", "*", "2", "*", "2", "*", "2", "*", "2", "*", "2", "*", "2", "*", "2", "*", "2"),
                tokens
        );
    }

    @Test
    void longUnaryChainTokenization() {
        List<String> tokens = tokenizer.tokenize(
                "--------------------3"
        );

        // 20 минусов + число
        List<String> expected = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) expected.add("-");
        expected.add("3");

        assertEquals(expected, tokens);
    }

    @Test
    void tokenizationOfVeryLongNumber() {
        String number = "1234567890".repeat(10); // 100 символов
        List<String> tokens = tokenizer.tokenize(number);
        assertEquals(List.of(number), tokens);
    }

    @Test
    void longExpressionGenerated() {
        // динамически генерируем: 1+1+1+...+1 (100 раз)
        StringBuilder expr = new StringBuilder("1");
        for (int i = 0; i < 100; i++) expr.append("+1");

        List<String> tokens = tokenizer.tokenize(expr.toString());

        // генерируем ожидаемые токены
        List<String> expected = new java.util.ArrayList<>();
        expected.add("1");
        for (int i = 0; i < 100; i++) {
            expected.add("+");
            expected.add("1");
        }

        assertEquals(expected, tokens);
    }

    @Test
    void tokenizerDoesNotValidateMissingOperand() {
        // Tokenizer должен просто токенизировать, не проверяя структуру
        assertDoesNotThrow(() -> tokenizer.tokenize("--"));
    }

    @Test
    void tokenizerDoesNotValidateOperatorSequence() {
        assertDoesNotThrow(() -> tokenizer.tokenize("--+*3"));
    }

    @Test
    void tokenizerDoesNotValidateDoubleUnaryBeforeBinary() {
        assertDoesNotThrow(() -> tokenizer.tokenize("--*3"));
    }

    @Test
    void tokenizerAllowsBinaryThenUnaryChain() {
        List<String> tokens = tokenizer.tokenize("3--3");
        assertEquals(List.of("3", "-", "-", "3"), tokens);
    }
}
