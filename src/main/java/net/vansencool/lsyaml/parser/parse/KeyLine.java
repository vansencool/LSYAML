package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.parser.text.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block mapping key line split into its key, key style, value, and inline comment.
 */
public final class KeyLine {

    private final @NotNull String key;
    private final @NotNull ScalarStyle keyStyle;
    private final @NotNull String value;
    private final @Nullable String inlineComment;

    private KeyLine(@NotNull String key, @NotNull ScalarStyle keyStyle, @NotNull String value, @Nullable String inlineComment) {
        this.key = key;
        this.keyStyle = keyStyle;
        this.value = value;
        this.inlineComment = inlineComment;
    }

    /**
     * Returns the parsed key line for a trimmed content string, or null when it is not a key line.
     */
    public static @Nullable KeyLine parse(@NotNull String trimmed) {
        int colonIdx = unquotedColon(trimmed);
        if (colonIdx <= 0) return null;

        String keyPart = trimmed.substring(0, colonIdx).trim();
        String valuePart = trimmed.substring(colonIdx + 1).trim();

        String inlineComment;
        if (valuePart.startsWith("#")) {
            inlineComment = valuePart.substring(1);
            valuePart = "";
        } else {
            int hash = inlineHash(valuePart);
            if (hash >= 0) {
                inlineComment = valuePart.substring(hash + 1);
                valuePart = valuePart.substring(0, hash - 1).trim();
            } else {
                inlineComment = null;
            }
        }

        return new KeyLine(unquoteKey(keyPart), keyStyle(keyPart), valuePart, inlineComment);
    }

    /**
     * Returns the unquoted key text.
     */
    public @NotNull String key() {
        return key;
    }

    /**
     * Returns the key quote style.
     */
    public @NotNull ScalarStyle keyStyle() {
        return keyStyle;
    }

    /**
     * Returns the value text with any inline comment removed.
     */
    public @NotNull String value() {
        return value;
    }

    /**
     * Returns the inline comment text, or null when absent.
     */
    public @Nullable String inlineComment() {
        return inlineComment;
    }

    private static int unquotedColon(@NotNull String s) {
        boolean single = false;
        boolean dbl = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !dbl) single = !single;
            else if (c == '"' && !single) dbl = !dbl;
            else if (c == ':' && !single && !dbl) return i;
        }
        return -1;
    }

    private static int inlineHash(@NotNull String s) {
        boolean single = false;
        boolean dbl = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !dbl) single = !single;
            else if (c == '"' && !single) dbl = !dbl;
            else if (c == '#' && !single && !dbl && i > 0 && s.charAt(i - 1) == ' ') return i;
        }
        return -1;
    }

    private static @NotNull String unquoteKey(@NotNull String key) {
        if (key.startsWith("'") && key.endsWith("'") && key.length() >= 2) {
            return key.substring(1, key.length() - 1).replace("''", "'");
        }
        if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
            return Strings.unescape(key.substring(1, key.length() - 1));
        }
        return key;
    }

    private static @NotNull ScalarStyle keyStyle(@NotNull String key) {
        if (key.startsWith("'") && key.endsWith("'")) return ScalarStyle.SINGLE_QUOTED;
        if (key.startsWith("\"") && key.endsWith("\"")) return ScalarStyle.DOUBLE_QUOTED;
        return ScalarStyle.PLAIN;
    }
}
