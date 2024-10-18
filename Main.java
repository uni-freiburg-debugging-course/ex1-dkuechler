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
        this.mType = type;
        this.mValue = value;
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
        this.mInput = args;
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
            } else if (c == '-' ) {
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

// Node used for AST of Parser. Hardcoded to have 2 children (for now)
class AbstractSyntaxNodeBinary {
    private final SmtToken mToken;
    private final AbstractSyntaxNodeBinary mLeft;
    private final AbstractSyntaxNodeBinary mRight;

    public AbstractSyntaxNodeBinary(SmtToken token, AbstractSyntaxNodeBinary mLeft, AbstractSyntaxNodeBinary mRight) {
        this.mToken = token;
        this.mLeft = mLeft;
        this.mRight = mRight;
    }

    public SmtToken getToken() {
        return mToken;
    }

    public AbstractSyntaxNodeBinary getLeft() {
        return mLeft;
    }

    public AbstractSyntaxNodeBinary getRight() {
        return mRight;
    }

    @Override
    public String toString() {
        if (mToken.getType().equals(TokenType.NUMBER)) {
            return String.format("%s", mToken);
        } else if (mToken.getType().equals(TokenType.UNARYKEYWORD)) {
            return String.format("%s (%s)", mToken, mLeft.toString());
        } else {
            return String.format("%s (%s, %s)", mToken, mLeft.toString(), mRight.toString());
        }
    }
}

class SmtLispParser {
    private final List<SmtToken> mTokens;
    private int mTokenIndex = 0;
    private final AbstractSyntaxNodeBinary mRoot;

    public SmtLispParser(List<SmtToken> tokens) {
        this.mTokens = tokens;
        this.mRoot = buildTree();
    }

    public AbstractSyntaxNodeBinary getRoot() {
        return mRoot;
    }

    private AbstractSyntaxNodeBinary buildTree() {
        do {
            mTokenIndex++;
        } while (mTokens.get(mTokenIndex).getType() == TokenType.PARENTHESES);

        AbstractSyntaxNodeBinary node;
        if (mTokens.get(mTokenIndex).getType() == TokenType.BINARYOPERATOR) {
            node = new AbstractSyntaxNodeBinary(mTokens.get(mTokenIndex), buildTree(), buildTree());
        } else if (mTokens.get(mTokenIndex).getType() == TokenType.UNARYKEYWORD) {
            node = new AbstractSyntaxNodeBinary(mTokens.get(mTokenIndex), buildTree(), null);
        } else {
            node = new AbstractSyntaxNodeBinary(mTokens.get(mTokenIndex), null, null);
        }
        return node;
    }

    @Override
    public String toString() {
        return String.format("%s", mRoot);
    }
}

// goes through given AST recursively, calculates (numerical) result
class SmtLispEvaluator {
    private long mResult = 0;

    public SmtLispEvaluator(AbstractSyntaxNodeBinary root) {
        mResult = evaluate(root);
    }

    public long getResult() {
        return mResult;
    }

    private long evaluate(AbstractSyntaxNodeBinary node) {
        TokenType tokenType = node.getToken().getType();
        if (tokenType == TokenType.UNARYKEYWORD) {
            return evaluate(node.getLeft());
        } else if (tokenType == TokenType.BINARYOPERATOR) {
            return evaluateBinaryOperator(node);
        } else if (tokenType == TokenType.NUMBER) {
            return (Integer.parseInt(node.getToken().getValue()));
        } else {
            throw new UnsupportedOperationException("Unsupported token type: " + tokenType);
        }
    }

    long evaluateBinaryOperator(AbstractSyntaxNodeBinary node) {
        String tokenValue = node.getToken().getValue();
        switch (tokenValue) {
            case "+":
                return evaluate(node.getLeft()) + evaluate(node.getRight());
            case "-":
                return evaluate(node.getLeft()) - evaluate(node.getRight());
            case "*":
                return evaluate(node.getLeft()) * evaluate(node.getRight());
            default:
                throw new UnsupportedOperationException("Unexpected binary operator: " + tokenValue);
        }
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
