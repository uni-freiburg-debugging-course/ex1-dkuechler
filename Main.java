// Daniel Küchler, Matrikelnummer: 3915548

import java.util.ArrayList;
import java.util.List;

enum TokenType {
    UNARYKEYWORD,
    NUMBER,
    BINARYOPERATOR,
    PARENTHESES,
}

class SmtToken {
    private final TokenType mType;
    private final String mValue;

    public SmtToken(TokenType type, String value) {
        mType = type;
        mValue = value;
    }

    public TokenType getType() {
        return mType;
    }

    public String getValue() {
        return mValue;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", mType, mValue);
    }
}

// Creates SmtTokens from given Lisp style String
class SmtLispLexer {
    private final String mInput;
    private final List<SmtToken> mTokens = new ArrayList<>();
    private int mLookAhead = 0;

    public SmtLispLexer(String args) {
        mInput = args;
        lex();
    }

    public List<SmtToken> getTokens() {
        return mTokens;
    }

    private void lex() {
        // Helper index to avoid incrementing i during loop
        mLookAhead = 0;
        for (int i = 0, n = mInput.length(); i < n; i++) {
            if (i < mLookAhead) {
                continue;
            }
            char c = mInput.charAt(i);
            if (c == ' ' || c == '\t') {
                continue;
            } else if (c == '(' || c == ')') {
                createParenthesesToken(c);
            } else if (c == '+' || c == '*') {
                createBinaryOperatorToken(c);
            } else if (c == '-') {
                if (i == 0 || mInput.charAt(i - 1) == '(') {
                    createBinaryOperatorToken(c);
                } else {
                    mLookAhead = i + 1;
                    createNumberToken(c);
                }
            } else if (Character.isDigit(c)) {
                mLookAhead = i + 1;
                createNumberToken(c);
            } else if (Character.isLetter(c)) {
                mLookAhead = i + 1;
                createUnaryOperatorToken(c);
            }
        }
    }

    void createBinaryOperatorToken(char c) {
        mTokens.add(new SmtToken(TokenType.BINARYOPERATOR, "" + c));
    }

    void createParenthesesToken(char c) {
        mTokens.add(new SmtToken(TokenType.PARENTHESES, "" + c));
    }

    // Digit detected -> keep adding next character while it's still a digit => number
    void createNumberToken(Character c) {
        StringBuilder number = new StringBuilder("" + c);
        int n = mInput.length();
        while (mLookAhead < n && Character.isDigit(mInput.charAt(mLookAhead))) {
            number.append(mInput.charAt(mLookAhead));
            mLookAhead++;
        }
        mTokens.add(new SmtToken(TokenType.NUMBER, number.toString()));
    }

    // Letter detected -> keep adding next character while it's still a letter => keyword
    void createUnaryOperatorToken(Character c) {
        StringBuilder keyword = new StringBuilder("" + c);
        int n = mInput.length();
        while (mLookAhead < n && Character.isLetter(mInput.charAt(mLookAhead))) {
            keyword.append(mInput.charAt(mLookAhead));
            mLookAhead++;
        }
        mTokens.add(new SmtToken(TokenType.UNARYKEYWORD, keyword.toString()));

    }
}

/*
 * Used to build abstract syntax tree, Interpreter pattern
 */
interface Expression {
    long interpret();
}

class SimplifyExpression implements Expression {
    private final Expression mExpression;

    SimplifyExpression(Expression Expression) {
        mExpression = Expression;
    }

    @Override
    public long interpret() {
        return mExpression.interpret();
    }
}

class AddExpression implements Expression {
    private final Expression mExpressionLeft;
    private final Expression mExpressionRight;

    AddExpression(Expression ExpressionLeft, Expression ExpressionRight) {
        mExpressionLeft = ExpressionLeft;
        mExpressionRight = ExpressionRight;
    }

    @Override
    public long interpret() {
        return mExpressionLeft.interpret() + mExpressionRight.interpret();
    }
}

class SubstractExpression implements Expression {
    private final Expression mExpressionLeft;
    private final Expression mExpressionRight;

    SubstractExpression(Expression ExpressionLeft, Expression ExpressionRight) {
        mExpressionLeft = ExpressionLeft;
        mExpressionRight = ExpressionRight;
    }

    @Override
    public long interpret() {
        return mExpressionLeft.interpret() - mExpressionRight.interpret();
    }
}

class MultiplicationExpression implements Expression {
    private final Expression mExpressionLeft;
    private final Expression mExpressionRight;

    MultiplicationExpression(Expression ExpressionLeft, Expression ExpressionRight) {
        mExpressionLeft = ExpressionLeft;
        mExpressionRight = ExpressionRight;
    }

    @Override
    public long interpret() {
        return mExpressionLeft.interpret() * mExpressionRight.interpret();
    }
}

class NumberExpression implements Expression {
    private final String mValue;

    NumberExpression(String Value) {
        mValue = Value;
    }

    @Override
    public long interpret() {
        return Long.parseLong(mValue);
    }
}

// Iterates over (ordered) list of SmtTokens and
// creates Abstract Syntax Tree using Expression
class SmtLispParser {
    private final List<SmtToken> mTokens;
    private int mTokenIndex = 0;
    private final Expression mRoot;

    public SmtLispParser(List<SmtToken> tokens) {
        mTokens = tokens;
        mRoot = createExpressionFromNextToken();
    }

    public Expression getRoot() {
        return mRoot;
    }

    private Expression createExpressionFromNextToken() {
        do {
            mTokenIndex++;
        } while (mTokens.get(mTokenIndex).getType() == TokenType.PARENTHESES);

        TokenType tokenType = mTokens.get(mTokenIndex).getType();
        String tokenValue = mTokens.get(mTokenIndex).getValue();
        if (tokenType == TokenType.BINARYOPERATOR) {
            switch (tokenValue) {
                case ("+"):
                    return (new AddExpression(createExpressionFromNextToken(), createExpressionFromNextToken()));
                case ("-"):
                    return (new SubstractExpression(createExpressionFromNextToken(), createExpressionFromNextToken()));
                case ("*"):
                    return (new MultiplicationExpression(createExpressionFromNextToken(), createExpressionFromNextToken()));
                default:
                    throw new IllegalStateException("Unexpected value: " + tokenValue);
            }
        } else if (tokenType == TokenType.UNARYKEYWORD) {
            switch (tokenValue) {
                case ("simplify"):
                    return (new SimplifyExpression(createExpressionFromNextToken()));
                default:
                    throw new IllegalStateException("Unexpected value: " + tokenValue);
            }
        } else if (tokenType == TokenType.NUMBER) {
            return (new NumberExpression(tokenValue));
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s", mRoot);
    }
}

// goes through given AST recursively, calculates (numerical) result
class SmtLispEvaluator {
    private final long mResult;

    public SmtLispEvaluator(Expression root) {
        mResult = root.interpret();
    }

    public long getResult() {
        return mResult;
    }
}

// combines classes to algorithm
final class SmtSolver {
    public static void solve(String input) {
        SmtLispLexer lexer = new SmtLispLexer(input);
        if (lexer.getTokens().isEmpty()) {
            return;
        }
        SmtLispParser parser = new SmtLispParser(lexer.getTokens());
        SmtLispEvaluator evaluator = new SmtLispEvaluator(parser.getRoot());
        long result = evaluator.getResult();
        printResult(result);
    }

    private static void printResult(long result) {
        if (result < 0) {
            // output negative Integers like this to match z3 output
            System.out.println("(- " + result * -1 + ")");
        } else {
            System.out.println(result);
        }

    }
}

public class Main {
    // expects file as input, if no file given, fuzzes 10 formula to fuzz.txt and evaluates them
    public static void main(String[] args) {
        String filename = extractOrCreateFile(args);
        List<String> lines = SmtFileHandler.readSmtFileByLine(filename);
        for (String line : lines) {
            SmtSolver.solve(line);
        }
    }

    private static String extractOrCreateFile(String[] args) {
        if (args.length == 0) {
            SmtFileHandler.createFuzzFile(10, "fuzz.txt");
            return "fuzz.txt";
        } else {
            return args[0];
        }
    }
}
