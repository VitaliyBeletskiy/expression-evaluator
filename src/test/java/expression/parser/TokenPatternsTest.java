package expression.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenPatternsTest {

    // -------- isNumber --------

    @Test
    void validNumbers() {
        assertTrue(TokenPatterns.isNumber("0"));
        assertTrue(TokenPatterns.isNumber("42"));
        assertTrue(TokenPatterns.isNumber("3.14"));
        assertTrue(TokenPatterns.isNumber("10.0"));
    }

    @Test
    void invalidNumbers() {
        assertFalse(TokenPatterns.isNumber(""));
        assertFalse(TokenPatterns.isNumber(".5"));
        assertFalse(TokenPatterns.isNumber("5."));
        assertFalse(TokenPatterns.isNumber("3..5"));
        assertFalse(TokenPatterns.isNumber("3a"));
        assertFalse(TokenPatterns.isNumber("a3"));
    }

    // -------- isOperator --------

    @Test
    void validOperators() {
        assertTrue(TokenPatterns.isOperator("+"));
        assertTrue(TokenPatterns.isOperator("-"));
        assertTrue(TokenPatterns.isOperator("*"));
        assertTrue(TokenPatterns.isOperator("/"));
    }

    @Test
    void invalidOperators() {
        assertFalse(TokenPatterns.isOperator(""));
        assertFalse(TokenPatterns.isOperator("^"));
        assertFalse(TokenPatterns.isOperator("+-"));
        assertFalse(TokenPatterns.isOperator("x"));
        assertFalse(TokenPatterns.isOperator("++"));
    }

    // -------- isUnaryChain --------

    @Test
    void validUnaryChains() {
        assertTrue(TokenPatterns.isUnaryChain("-"));
        assertTrue(TokenPatterns.isUnaryChain("+"));
        assertTrue(TokenPatterns.isUnaryChain("---"));
        assertTrue(TokenPatterns.isUnaryChain("+-+--"));
    }

    @Test
    void invalidUnaryChains() {
        assertFalse(TokenPatterns.isUnaryChain(""));
        assertFalse(TokenPatterns.isUnaryChain("*"));
        assertFalse(TokenPatterns.isUnaryChain("+--*"));
        assertFalse(TokenPatterns.isUnaryChain("++-a"));
    }

    // -------- isParenthesis --------

    @Test
    void validParentheses() {
        assertTrue(TokenPatterns.isParenthesis("("));
        assertTrue(TokenPatterns.isParenthesis(")"));
    }

    @Test
    void invalidParentheses() {
        assertFalse(TokenPatterns.isParenthesis(""));
        assertFalse(TokenPatterns.isParenthesis("()"));
        assertFalse(TokenPatterns.isParenthesis(")("));
        assertFalse(TokenPatterns.isParenthesis("((("));
    }

    // -------- classification helpers --------

    @Test
    void unaryOperators() {
        assertTrue(TokenPatterns.isUnaryOperator("+"));
        assertTrue(TokenPatterns.isUnaryOperator("-"));

        assertFalse(TokenPatterns.isUnaryOperator("*"));
        assertFalse(TokenPatterns.isUnaryOperator("/"));
    }

    @Test
    void multiplicativeOperators() {
        assertTrue(TokenPatterns.isMultiplicativeOperator("*"));
        assertTrue(TokenPatterns.isMultiplicativeOperator("/"));

        assertFalse(TokenPatterns.isMultiplicativeOperator("+"));
        assertFalse(TokenPatterns.isMultiplicativeOperator("-"));
    }
}
