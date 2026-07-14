package net.vansencool.lsyaml.node.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Classification of a scalar value into its YAML 1.2 core schema kind.
 */
public final class ScalarTypes {

    private ScalarTypes() {
    }

    /**
     * Returns the resolved kind of a stored scalar value.
     */
    public static @NotNull ScalarType of(@Nullable Object value) {
        if (value == null) {
            return ScalarType.NULL;
        }
        if (value instanceof Boolean) {
            return ScalarType.BOOLEAN;
        }
        if (value instanceof Integer) {
            return ScalarType.INT;
        }
        if (value instanceof Long) {
            return ScalarType.LONG;
        }
        if (value instanceof Double || value instanceof Float) {
            return ScalarType.DOUBLE;
        }
        return of(value.toString());
    }

    /**
     * Returns the resolved kind of an unquoted scalar string.
     */
    public static @NotNull ScalarType of(@NotNull String value) {
        int len = value.length();
        if (len == 0) {
            return ScalarType.STRING;
        }

        char first = value.charAt(0);
        if (len == 1 && first == '~') {
            return ScalarType.NULL;
        }

        if (len <= 5) {
            if (len == 4 && (first == 'n' || first == 'N') && "null".equalsIgnoreCase(value)) return ScalarType.NULL;
            if (len == 4 && (first == 't' || first == 'T') && "true".equalsIgnoreCase(value)) return ScalarType.BOOLEAN;
            if (len == 5 && (first == 'f' || first == 'F') && "false".equalsIgnoreCase(value)) return ScalarType.BOOLEAN;
            if (len == 3 && (first == 'y' || first == 'Y') && "yes".equalsIgnoreCase(value)) return ScalarType.BOOLEAN;
            if (len == 2 && (first == 'n' || first == 'N') && "no".equalsIgnoreCase(value)) return ScalarType.BOOLEAN;
            if (len == 2 && (first == 'o' || first == 'O') && "on".equalsIgnoreCase(value)) return ScalarType.BOOLEAN;
            if (len == 3 && (first == 'o' || first == 'O') && "off".equalsIgnoreCase(value)) return ScalarType.BOOLEAN;
        }

        if (first != '+' && first != '-' && first != '.' && (first < '0' || first > '9')) {
            return ScalarType.STRING;
        }

        if (first == '0' && len > 1) {
            char second = value.charAt(1);
            if (second == 'x' || second == 'X') return isRadix(value, 16) ? ScalarType.LONG : ScalarType.STRING;
            if (second == 'o' || second == 'O') return isRadix(value, 8) ? ScalarType.LONG : ScalarType.STRING;
        }

        boolean fractional = false;
        int start = (first == '+' || first == '-') ? 1 : 0;
        int digits = 0;
        int dots = 0;
        int exponents = 0;
        for (int i = start; i < len; i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
            } else if (c == '.') {
                if (++dots > 1 || exponents > 0) return ScalarType.STRING;
                fractional = true;
            } else if (c == 'e' || c == 'E') {
                if (++exponents > 1 || digits == 0) return ScalarType.STRING;
                fractional = true;
            } else if ((c == '+' || c == '-') && value.charAt(i - 1) != 'e' && value.charAt(i - 1) != 'E') {
                return ScalarType.STRING;
            } else if (c != '+' && c != '-') {
                return ScalarType.STRING;
            }
        }
        if (digits == 0) {
            return ScalarType.STRING;
        }

        if (fractional) {
            return ScalarType.DOUBLE;
        }
        try {
            long l = Long.parseLong(value);
            return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? ScalarType.INT : ScalarType.LONG;
        } catch (NumberFormatException e) {
            return ScalarType.STRING;
        }
    }

    private static boolean isRadix(@NotNull String value, int radix) {
        for (int i = 2; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), radix) < 0) {
                return false;
            }
        }
        return value.length() > 2;
    }
}
