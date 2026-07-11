package net.vansencool.lsyaml.parser.source;

import org.jetbrains.annotations.NotNull;

/**
 * Immutable view over the YAML input that exposes characters and slices by index.
 */
public final class Source {

    private final @NotNull String text;
    private final int length;

    public Source(@NotNull String text) {
        this.text = text;
        this.length = text.length();
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
        return text.charAt(index);
    }

    /**
     * Materializes the slice between two indices as a String.
     */
    public @NotNull String slice(int start, int end) {
        return text.substring(start, end);
    }

    /**
     * Returns the backing text.
     */
    public @NotNull String text() {
        return text;
    }
}
