package me.matiego.counting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LogicalExpressionsParser {
    public final static char[] ALLOWED_VARIABLES = new char[]{'p', 'q', 'r', 's'};

    public static @NotNull String simplify(@NotNull String expression) {
        String before = expression + "$";
        while (!before.equals(expression)) {
            before = expression;
            expression = expression
                    .replace("(0)", "0")
                    .replace("(1)", "1")
                    .replace("~1", "0")
                    .replace("~0", "1");
            for (char c : ALLOWED_VARIABLES) {
                expression = expression.replace("(" + c + ")", String.valueOf(c));
            }
        }
        return expression;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isTautology(@NotNull String expression) throws IllegalArgumentException {
        return checkSubstitutions(expression, getVariablesAsString(expression));
    }

    private static @NotNull String getVariablesAsString(@NotNull String expression) {
        StringBuilder result = new StringBuilder();
        for (char c : ALLOWED_VARIABLES) {
            if (expression.contains(String.valueOf(c))) {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean checkSubstitutions(@NotNull String expression, @NotNull String variables) {
        if (variables.isEmpty()) return calculateLogicalValue(simplify(expression));
        return checkSubstitutions(expression.replace(variables.charAt(0), '0'), variables.substring(1))
                && checkSubstitutions(expression.replace(variables.charAt(0), '1'), variables.substring(1));
    }

    private static boolean calculateLogicalValue(@NotNull String expression) throws IllegalArgumentException {
        String before = expression + "$";
        while (expression.length() > 1 && !before.equals(expression)) {
            before = expression;
            expression = simplify(replaceFirstLogicalConnective(expression));
        }
        if (expression.equals("0")) {
            return false;
        }
        if (expression.equals("1")) {
            return true;
        }
        throw new IllegalArgumentException("expression becomes neither 1 nor 0");
    }

    private static @NotNull String replaceFirstLogicalConnective(@NotNull String expression) throws IllegalArgumentException {
        for (int i = 1; i < expression.length() - 1; i++) {
            char p = expression.charAt(i - 1);
            if (!(p == '0' || p == '1')) continue;
            char q = expression.charAt(i + 1);
            if (!(q == '0' || q == '1')) continue;

            LogicalConnective connective = LogicalConnective.getByCharacter(expression.charAt(i));
            if (connective == null) continue;

            return expression.substring(0, i - 1) + (connective.calculateValue(p == '1', q == '1') ? "1" : "0") + expression.substring(i + 2);
        }
        throw new IllegalArgumentException("expression cannot be simplified to 0 or 1");
    }

    private enum LogicalConnective {
        BICONDITIONAL('⇔', true, false, false, true),
        CONDITIONAL('⇒', true, false, true, true),
        CONJUNCTION('∧', true, false, false, false),
        DISJUNCTION('∨', true, true, true, false),
        XOR('⊕', false, true, true, false);

        LogicalConnective(char c, boolean... values) {
            this.c = c;
            this.values = values;
        }

        private final char c;
        //0 - true, true
        //1 - true, false
        //2 - false, true
        //3 - false, false
        private final boolean[] values;

        public char getCharacter() {
            return c;
        }

        public boolean calculateValue(boolean p, boolean q) {
            return values[(p ? 0 : 2) + (q ? 0 : 1)];
        }

        public static @Nullable LogicalConnective getByCharacter(char c) {
            for (LogicalConnective connective : LogicalConnective.values()) {
                if (connective.getCharacter() == c) {
                    return connective;
                }
            }
            return null;
        }
    }
}
