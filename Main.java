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

    public SmtLispLexer(String args) {
        this.mInput = args;
        lex();
    }

    public List<SmtToken> getTokens() {
        return mTokens;
    }

    private void lex() {
        String s = mInput;
        // to avoid incrementing i during loop
        int lookAhead = 0;
        for (int i = 0, n = s.length(); i < n; i++) {
            if (i < lookAhead) {
                continue;
            }
            char c = s.charAt(i);
            // we split input by lines during file scan, so \n doesn't indicate new formula
            if (c == ' ' || c == '\t' || c == '\n') {
                continue;
            } else if (c == '+' || c == '-' || c == '*') {
                mTokens.add(new SmtToken(TokenType.BINARYOPERATOR, "" + c));
            } else if (c == '(' || c == ')') {
                mTokens.add(new SmtToken(TokenType.PARENTHESES, "" + c));
            } else if (Character.isDigit(c)) {
                StringBuilder number = new StringBuilder("" + c);
                lookAhead = i + 1;
                while (lookAhead < n && Character.isDigit(s.charAt(lookAhead))) {
                    number.append(s.charAt(lookAhead));
                    lookAhead = lookAhead + 1;
                }
                mTokens.add(new SmtToken(TokenType.NUMBER, number.toString()));
            } else if (Character.isLetter(c)) {
                StringBuilder keyword = new StringBuilder("" + c);
                lookAhead = i + 1;
                while (lookAhead < n && Character.isLetter(s.charAt(lookAhead))) {
                    keyword.append(s.charAt(lookAhead));
                    lookAhead = lookAhead + 1;
                }
                mTokens.add(new SmtToken(TokenType.UNARYKEYWORD, keyword.toString()));
            }
        }
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
        if (mTokens.isEmpty()) {
            return null;
        }
        do {
            mTokenIndex++;
        } while (mTokens.get(mTokenIndex).getType() == TokenType.PARENTHESES);

        AbstractSyntaxNodeBinary node = null;
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
            String tokenValue = node.getToken().getValue();
            switch (tokenValue) {
                case "+":
                    return evaluate(node.getLeft()) + evaluate(node.getRight());
                case "-":
                    return evaluate(node.getLeft()) - evaluate(node.getRight());
                case "*":
                    return evaluate(node.getLeft()) * evaluate(node.getRight());
            }
        } else if (tokenType == TokenType.NUMBER) {
            return (Integer.parseInt(node.getToken().getValue()));
        }
        // TODO: handle error
        return Integer.MIN_VALUE;
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
        // output negative Integers like this to match z3 output
        if (result < 0) {
            System.out.println("(- " + result * -1 + ")");
        } else {
            System.out.println(evaluator.getResult());
        }
    }
}

public class Main {
    // expects file as input, if no file given, fuzzes 10 formula to fuzz.txt and evaluates them
    public static void main(String[] args) {
        String filename;
        if (args.length == 0) {
            SmtFileHandler.createFuzzFile(10, "fuzz.txt");
            filename = "fuzz.txt";
        } else {
            filename = args[0];
        }
        List<String> lines = SmtFileHandler.readSmtFileByLine(filename);
        for (String line : lines) {
            SmtSolver.solve(line);
        }
    }
}
