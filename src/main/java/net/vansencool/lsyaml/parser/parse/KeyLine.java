package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.parser.text.Scan;
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
    private final @NotNull Slice value;
    private final @Nullable String inlineComment;

    private KeyLine(@NotNull String key, @NotNull ScalarStyle keyStyle, @NotNull Slice value, @Nullable String inlineComment) {
        this.key = key;
        this.keyStyle = keyStyle;
        this.value = value;
        this.inlineComment = inlineComment;
    }

    /**
     * Returns the parsed key line for a trimmed content view, or null when it is not a key line.
     */
    public static @Nullable KeyLine parse(@NotNull Slice trimmed) {
        int colonIdx = Scan.unquotedColon(trimmed.array(), trimmed.start(), trimmed.end());
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
            int hash = Scan.inlineHash(valuePart.array(), valuePart.start(), valuePart.end());
            if (hash >= 0) {
                inlineComment = valuePart.copy().sub(hash + 1).toString();
                valuePart.sub(0, hash - 1).trim();
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
    public @NotNull Slice value() {
        return value;
    }

    /**
     * Returns the inline comment text, or null when absent.
     */
    public @Nullable String inlineComment() {
        return inlineComment;
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
