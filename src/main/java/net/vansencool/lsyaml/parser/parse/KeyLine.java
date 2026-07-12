package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.parser.text.Slice;
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
     * Returns the parsed key line for a trimmed content view, or null when it is not a key line.
     */
    public static @Nullable KeyLine parse(@NotNull Slice trimmed) {
        int colonIdx = unquotedColon(trimmed);
        if (colonIdx <= 0) return null;

        char[] chars = trimmed.array();
        int base = trimmed.start();
        Slice valuePart = Slice.of(chars, base + colonIdx + 1, trimmed.end()).trim();
        Slice keyPart = trimmed.sub(0, colonIdx).trim();

        String inlineComment;
        if (valuePart.startsWith('#')) {
            inlineComment = valuePart.sub(1).toString();
            valuePart = Slice.empty();
        } else {
            int hash = inlineHash(valuePart);
            if (hash >= 0) {
                inlineComment = valuePart.sub(hash + 1).toString();
                valuePart = valuePart.sub(0, hash - 1).trim();
            } else {
                inlineComment = null;
            }
        }

        return new KeyLine(unquoteKey(keyPart), keyStyle(keyPart), valuePart.toString(), inlineComment);
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

    private static int unquotedColon(@NotNull Slice s) {
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

    private static int inlineHash(@NotNull Slice s) {
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

    private static @NotNull String unquoteKey(@NotNull Slice key) {
        if (key.length() >= 2 && key.startsWith('\'') && key.endsWith('\'')) {
            String inner = key.sub(1, key.length() - 1).toString();
            return inner.indexOf('\'') >= 0 ? inner.replace("''", "'") : inner;
        }
        if (key.length() >= 2 && key.startsWith('"') && key.endsWith('"')) {
            return Strings.unescape(key.sub(1, key.length() - 1).toString());
        }
        return key.toString();
    }

    private static @NotNull ScalarStyle keyStyle(@NotNull Slice key) {
        if (key.startsWith('\'') && key.endsWith('\'')) return ScalarStyle.SINGLE_QUOTED;
        if (key.startsWith('"') && key.endsWith('"')) return ScalarStyle.DOUBLE_QUOTED;
        return ScalarStyle.PLAIN;
    }
}
