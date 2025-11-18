package expression.tokenizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tokenizer converts a normalized expression string into a list of tokens
 * (numbers, operators, parentheses). It performs input cleanup and basic
 * syntactic validation, preparing the expression for the Parser.
 */
public class Tokenizer {

    /**
     * Performs full tokenization of the input expression.
     * <p>
     * This method applies all preprocessing steps:
     * <ul>
     *   <li>normalizes the input string (removes whitespace variations, standardizes characters)</li>
     *   <li>splits the normalized text into tokens (numbers, operators, parentheses)</li>
     * </ul>
     *
     * The returned list of tokens is guaranteed to contain:
     * <ul>
     *   <li>only valid characters</li>
     *   <li>numbers in normalized form (using '.' for decimals)</li>
     *   <li>operators and parentheses as single-character tokens</li>
     * </ul>
     *
     * @param input raw expression from the user
     * @return list of normalized tokens ready for parsing
     * @throws IllegalArgumentException if illegal characters or malformed numbers are encountered
     */
    public List<String> tokenize(String input) {
        String normalized = normalizeString(input);
        return tokenizeString(normalized);
    }

    /**
     * Splits the input expression into tokens (numbers, operators, parentheses)
     * and removes all whitespace.
     * <p>
     * This tokenizer detects and rejects malformed numeric or symbolic sequences.
     * Examples of invalid inputs:
     * <ul>
     *   <li>"3 + $4"   — unknown character '$'</li>
     *   <li>"3.1a"     — a number contains a letter</li>
     *   <li>"3.14.15"  — multiple decimal points in a single number</li>
     *   <li>"2.3.4.5"  — same as above</li>
     *   <li>"3..5"     — consecutive dots produce an invalid number</li>
     * </ul>
     */
    private List<String> tokenizeString(final String input) throws IllegalArgumentException {
        // Define the set of allowed characters
        Set<Character> allowed = new HashSet<>();
        for (char c : "0123456789.+-*/ ()".toCharArray()) {
            allowed.add(c);
        }

        // Validate that the input contains only allowed characters
        for (char c : input.toCharArray()) {
            if (!allowed.contains(c)) {
                throw new IllegalArgumentException("Input string contains invalid characters: " + c);
            }
        }

        ArrayList<String> output = new ArrayList<>();
        Set<Character> operatorsAndParens = Set.of('+', '-', '*', '/', '(', ')');
        boolean gotDelimiterAlready = false;
        StringBuilder currentNumber = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                currentNumber.append(c);
            } else if (c == '.') {
                if (gotDelimiterAlready) {
                    throw new IllegalArgumentException("Decimal delimiter at the invalid position.");
                }
                gotDelimiterAlready = true;
                currentNumber.append(c);
            } else if (operatorsAndParens.contains(c)) {
                gotDelimiterAlready = false;

                addNumberAndFlushStringBuilder(currentNumber, output);
                output.add(String.valueOf(c));
            } else {
                // if Whitespace: finalize current number (if any)
                gotDelimiterAlready = false;

                addNumberAndFlushStringBuilder(currentNumber, output);
            }
        }
        addNumberAndFlushStringBuilder(currentNumber, output);

        return output;
    }

    /**
     * Normalizes the raw input expression before tokenization.
     * <p>
     * This method performs character-level cleanup to ensure that the expression
     * uses a consistent and tokenizer-friendly format.
     * Normalization steps:
     * <ul>
     *   <li>Strips leading and trailing whitespace from the entire input.</li>
     *   <li>Replaces any kind of whitespace (spaces, tabs, newlines) with a single space.</li>
     *   <li>Converts various dash characters (en dash, em dash, Unicode minus) to a standard ASCII minus '-'.</li>
     *   <li>Converts commas to dots to support both decimal separators.</li>
     *   <li>Leaves all other characters unchanged.</li>
     * </ul>
     *
     * The returned string is guaranteed to:
     * <ul>
     *   <li>contain only single spaces between tokens,</li>
     *   <li>use '.' as the decimal separator,</li>
     *   <li>use '-' for both unary and binary minus,</li>
     *   <li>preserve all semantically meaningful characters.</li>
     * </ul>
     *
     * @param input the raw expression provided by the user
     * @return a normalized expression string suitable for tokenization
     */
    private String normalizeString(final String input) {
        String trimmed = input.strip(); // trim on steroids

        StringBuilder output = new StringBuilder();

        for (char symbol : trimmed.toCharArray()) {
            if (Character.isWhitespace(symbol)) {
                // Convert all whitespace (spaces, tabs, newlines, etc.) into a single space
                output.append(' ');
            } else if (symbol == '–' || symbol == '—' || symbol == '−') {
                // Replace any long dash variants (en dash, em dash, minus sign) with a standard hyphen
                output.append('-');
            } else if (symbol == ',') {
                // Replace comma with a dot to support both decimal separators
                output.append('.');
            } else {
                output.append(symbol);
            }
        }
        return output.toString();
    }

    // Helper method
    private void addNumberAndFlushStringBuilder(StringBuilder strBuilder, ArrayList<String> arrayWithData) {
        if (!strBuilder.isEmpty()) {
            String strNumber = strBuilder.toString();
            if (strNumber.equals(".")) {
                throw new IllegalArgumentException("Lone dots are not allowed.");
            } else if (strNumber.startsWith(".")) {
                strNumber = "0" + strNumber;
            } else if (strNumber.endsWith(".")) {
                strNumber += "0";
            }
            arrayWithData.add(strNumber);
            strBuilder.setLength(0);
        }
    }
}
