package expression;

import java.util.Scanner;

public class Application {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println(getPrompt());
        String input = scanner.nextLine();
        try {
            double result = ExpressionEvaluator.evaluate(input);
            System.out.println("Result: " + result);
        } catch (Exception e) {
            System.out.println("Error. " + e);
        }
    }

    private static String getPrompt() {
        return """
                Please enter a mathematical expression to evaluate.
                You can use:
                  • parentheses: ( )
                  • unary operators: +  -
                  • binary operators: +  -  *  /
                  • decimal numbers with dot or comma
                
                Examples:
                  3 + 4 * 2
                  -(3 + 2)
                  10 / (2 - 1)
                
                  Enter the expression to calculate:
                """;
    }
}
