package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.parser.text.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Construction of scalar nodes from a materialized value string.
 */
public final class Scalars {

    private Scalars() {
    }

    /**
     * Returns a scalar node for a value string with quote handling and type detection.
     */
    public static @NotNull ScalarNode of(@NotNull String value) {
        int len = value.length();
        if (len >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(len - 1);
            if (first == '\'' && last == '\'') {
                String content = value.substring(1, len - 1);
                if (content.indexOf('\'') >= 0) content = content.replace("''", "'");
                ScalarNode scalar = new ScalarNode(content, ScalarStyle.SINGLE_QUOTED);
                scalar.setRawValue(value);
                return scalar;
            }
            if (first == '"' && last == '"') {
                String inner = value.substring(1, len - 1);
                String content = Strings.unescape(inner);
                ScalarNode scalar = new ScalarNode(content, ScalarStyle.DOUBLE_QUOTED);
                scalar.setRawValue(value);
                return scalar;
            }
        }
        ScalarNode scalar = new ScalarNode(typed(value), ScalarStyle.PLAIN);
        scalar.setRawValue(value);
        return scalar;
    }

    /**
     * Returns the typed value of an unquoted scalar string.
     */
    public static @Nullable Object typed(@NotNull String value) {
        int len = value.length();
        if (len == 0) return null;

        char first = value.charAt(0);
        if (len == 1 && first == '~') return null;

        if (len <= 5) {
            if (len == 4 && (first == 'n' || first == 'N') && "null".equalsIgnoreCase(value)) return null;
            if (len == 4 && (first == 't' || first == 'T') && "true".equalsIgnoreCase(value)) return true;
            if (len == 5 && (first == 'f' || first == 'F') && "false".equalsIgnoreCase(value)) return false;
            if (len == 3 && (first == 'y' || first == 'Y') && "yes".equalsIgnoreCase(value)) return true;
            if (len == 2 && (first == 'n' || first == 'N') && "no".equalsIgnoreCase(value)) return false;
            if (len == 2 && (first == 'o' || first == 'O') && "on".equalsIgnoreCase(value)) return true;
            if (len == 3 && (first == 'o' || first == 'O') && "off".equalsIgnoreCase(value)) return false;
        }

        if (first != '+' && first != '-' && first != '.' && (first < '0' || first > '9')) return value;

        if (first == '0' && len > 1) {
            char second = value.charAt(1);
            if (second == 'x' || second == 'X') return radix(value, 16, Scalars::isHex);
            if (second == 'o' || second == 'O') return radix(value, 8, Scalars::isOctal);
        }

        boolean fractional = false;
        int start = (first == '+' || first == '-') ? 1 : 0;
        for (int i = start; i < len; i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') continue;
            if (c == '.' || c == 'e' || c == 'E') { fractional = true; continue; }
            if ((c == '+' || c == '-') && i > start) continue;
            return value;
        }

        try {
            if (fractional) return Double.parseDouble(value);
            long l = Long.parseLong(value);
            return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (int) l : l;
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static Object radix(@NotNull String value, int radix, @NotNull DigitCheck check) {
        if (!check.allDigits(value, 2)) return value;
        try {
            return Long.parseLong(value.substring(2), radix);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static boolean isHex(@NotNull String value, int from) {
        if (from >= value.length()) return false;
        for (int i = from; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) continue;
            return false;
        }
        return true;
    }

    private static boolean isOctal(@NotNull String value, int from) {
        if (from >= value.length()) return false;
        for (int i = from; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '7') continue;
            return false;
        }
        return true;
    }

    private interface DigitCheck {
        boolean allDigits(@NotNull String value, int from);
    }
}
