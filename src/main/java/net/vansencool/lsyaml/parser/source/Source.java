package net.vansencool.lsyaml.parser.source;

import net.vansencool.lsyaml.parser.text.Slice;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable view over the YAML input.
 */
public final class Source {

    private final char @NotNull [] chars;
    private final int length;

    public Source(@NotNull String text) {
        this.chars = text.toCharArray();
        this.length = chars.length;
    }

    /**
     * Returns the underlying text length.
     */
    public int length() {
        return length;
    }

    /**
     * Returns the character at an index.
     */
    public char charAt(int index) {
        return chars[index];
    }

    /**
     * Returns a zero-copy view over the range between two indices.
     */
    public @NotNull Slice slice(int start, int end) {
        return Slice.of(chars, start, end);
    }

    /**
     * Returns the backing character array.
     */
    public char @NotNull [] chars() {
        return chars;
    }
}
