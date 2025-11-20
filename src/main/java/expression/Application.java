package expression;

import java.util.Scanner;

public class Application {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Please enter the expression to calculate:");
        String input = scanner.nextLine();
        try {
            double result = ExpressionEvaluator.evaluate(input);
            System.out.println("Result: " + result);
        } catch (Exception e) {
            System.out.println("Error. " + e);
        }
    }
}
