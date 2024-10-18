// Daniel Küchler, Matrikelnummer: 3915548

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class SmtLispFuzzer {
    Random mRandom = new Random();

    public String fuzzMany(int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append("(simplify ");
            result.append(generateRandomExpression()).append(")").append("\n");
        }
        return result.toString();
    }

    // pseudo random expression creation (Expression = (Operator, Expression, Expression) | Number)
    private String generateRandomExpression() {
        StringBuilder expression = new StringBuilder("(");
        int rand = mRandom.nextInt();
        // TODO: make more cleanly random
        String operator = rand % 2 == 0 ? "+" : rand % 3 == 0 ? "-" : "*";
        expression.append(operator);
        for (int i = 0; i < 2; i++) {
            rand = mRandom.nextInt();
            // arbitrary choice of 3, 2 created too much expression nesting on average (debug readability)
            if (rand % 3 == 0) {
                expression.append(generateRandomExpression());
            } else {
                expression.append(" ").append(generateRandomNumber());
            }
        }
        return expression + ")";
    }

    private String generateRandomNumber() {
        return (String.valueOf(mRandom.nextInt(10)));
    }

}

class SmtFileHandler {
    public static void createFuzzFile(int formulaCount, String filename) {
        SmtLispFuzzer fuzzer = new SmtLispFuzzer();
        try (PrintWriter out = new PrintWriter(filename)) {
            out.println(fuzzer.fuzzMany(formulaCount));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static List<String> readSmtFileByLine(String filename) {
        List<String> lines = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filename), "UTF-8")) {
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return lines;
    }
}
