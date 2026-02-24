package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.parser.util.ParsedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods shared across parser components.
 * <p>
 * Provides string manipulation, quote handling, and parsing helpers
 * that are used by multiple parser classes.
 * </p>
 */
public final class ParserUtils {

    private static final String[] SPACE_CACHE = new String[64];

    static {
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < 64; i++) {
            SPACE_CACHE[i] = sb.toString();
            sb.append(' ');
        }
    }

    private ParserUtils() {
    }

    /**
     * Returns a string of the specified number of spaces.
     * Uses a cache for common indentation levels.
     *
     * @param count the number of spaces
     * @return a string containing the specified number of spaces
     */
    @NotNull
    public static String spaces(int count) {
        if (count < SPACE_CACHE.length) {
            return SPACE_CACHE[count];
        }
        return " ".repeat(count);
    }

    /**
     * Finds the index of an unquoted colon in a string.
     * Ignores colons inside single or double quotes.
     *
     * @param str the string to search
     * @return the index of the colon, or -1 if not found
     */
    public static int findUnquotedColon(@NotNull String str) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            // track quote state to ignore colons inside quotes
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == ':' && !inSingleQuote && !inDoubleQuote) {
                return i; // found unquoted colon
            }
        }

        return -1; // no unquoted colon found
    }

    /**
     * Checks if a value contains an unquoted colon.
     *
     * @param value the value to check
     * @return true if the value contains an unquoted colon
     */
    public static boolean containsUnquotedColon(@NotNull String value) {
        if (value.isEmpty()) return false;
        char first = value.charAt(0);
        // skip checking quoted strings - they're safe
        if (first == '\'' || first == '"') return false;
        return findUnquotedColon(value) >= 0;
    }

    /**
     * Checks if a character indicates an empty or comment line.
     *
     * @param firstChar the first non-whitespace character
     * @return true if the character is null (empty line) or # (comment)
     */
    public static boolean isEmptyOrComment(char firstChar) {
        return firstChar == 0 || firstChar == '#';
    }

    /**
     * Checks if a character is a valid YAML word character (for anchors/aliases).
     *
     * @param c the character to check
     * @return true if alphanumeric or underscore
     */
    public static boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    @NotNull
    public static String extractComment(@NotNull String line) {
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t') continue;
            if (c == '#') {
                return line.substring(i + 1);
            }
            break;
        }
        return "";
    }

    @Nullable
    public static String extractInlineComment(@NotNull String value) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '#' && !inSingleQuote && !inDoubleQuote && i > 0 && value.charAt(i - 1) == ' ') {
                return value.substring(i + 1);
            }
        }

        return null;
    }

    @NotNull
    public static String removeInlineComment(@NotNull String value) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '#' && !inSingleQuote && !inDoubleQuote && i > 0 && value.charAt(i - 1) == ' ') {
                return value.substring(0, i - 1).trim();
            }
        }

        return value;
    }

    @NotNull
    public static String unquoteKey(@NotNull String key) {
        if (key.startsWith("'") && key.endsWith("'") && key.length() >= 2) {
            return key.substring(1, key.length() - 1).replace("''", "'");
        }
        if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
            return unescapeDoubleQuoted(key.substring(1, key.length() - 1));
        }
        return key;
    }

    @NotNull
    public static ScalarStyle detectKeyStyle(@NotNull String key) {
        if (key.startsWith("'") && key.endsWith("'")) {
            return ScalarStyle.SINGLE_QUOTED;
        }
        if (key.startsWith("\"") && key.endsWith("\"")) {
            return ScalarStyle.DOUBLE_QUOTED;
        }
        return ScalarStyle.PLAIN;
    }

    @NotNull
    public static String unescapeDoubleQuoted(@NotNull String str) {
        return str.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    @Nullable
    public static ParsedKey parseKeyLine(@NotNull String line) {
        String trimmed = line.trim();

        int colonIdx = findUnquotedColon(trimmed);
        if (colonIdx <= 0) {
            return null;
        }

        String keyPart = trimmed.substring(0, colonIdx).trim();
        String valuePart = trimmed.substring(colonIdx + 1).trim();

        String key = unquoteKey(keyPart);
        ScalarStyle keyStyle = detectKeyStyle(keyPart);

        String inlineComment;

        if (valuePart.startsWith("#")) {
            inlineComment = valuePart.substring(1);
            valuePart = "";
        } else {
            inlineComment = extractInlineComment(valuePart);
            if (inlineComment != null) {
                valuePart = removeInlineComment(valuePart);
            }
        }

        return new ParsedKey(key, keyStyle, valuePart, inlineComment);
    }

    @Nullable
    public static String extractAlias(@NotNull String value) {
        if (value.length() < 2 || value.charAt(0) != '*') {
            return null;
        }
        int i = 1;
        while (i < value.length() && isWordChar(value.charAt(i))) {
            i++;
        }
        if (i == 1) {
            return null;
        }
        for (int j = i; j < value.length(); j++) {
            char c = value.charAt(j);
            if (c != ' ' && c != '\t') {
                return null;
            }
        }
        return value.substring(1, i);
    }

    @Nullable
    public static String extractAnchorOnly(@NotNull String value) {
        int len = value.length();
        if (len < 2 || value.charAt(0) != '&') {
            return null;
        }
        int i = 1;
        while (i < len && isWordChar(value.charAt(i))) {
            i++;
        }
        if (i == 1) {
            return null;
        }
        for (int j = i; j < len; j++) {
            char c = value.charAt(j);
            if (c != ' ' && c != '\t') {
                return null;
            }
        }
        return value.substring(1, i);
    }

    @NotNull
    public static String trimAfterDash(@NotNull String trimmed) {
        int start = 2;
        int len = trimmed.length();
        while (start < len && (trimmed.charAt(start) == ' ' || trimmed.charAt(start) == '\t')) {
            start++;
        }
        if (start >= len) {
            return "";
        }
        int end = len;
        while (end > start && (trimmed.charAt(end - 1) == ' ' || trimmed.charAt(end - 1) == '\t')) {
            end--;
        }
        return trimmed.substring(start, end);
    }

    @NotNull
    public static String trimWhitespace(@NotNull String value) {
        int len = value.length();
        int start = 0;
        while (start < len && (value.charAt(start) == ' ' || value.charAt(start) == '\t')) {
            start++;
        }
        int end = len;
        while (end > start && (value.charAt(end - 1) == ' ' || value.charAt(end - 1) == '\t')) {
            end--;
        }
        if (start == 0 && end == len) {
            return value;
        }
        return value.substring(start, end);
    }
}
