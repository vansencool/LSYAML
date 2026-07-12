package net.vansencool.lsyaml.parser.text;

import org.jetbrains.annotations.NotNull;

/**
 * A mutable zero-copy window over a range of a backing character array.
 */
public final class Slice implements CharSequence {

    private char @NotNull [] chars;
    private int start;
    private int end;

    private Slice(char @NotNull [] chars, int start, int end) {
        this.chars = chars;
        this.start = start;
        this.end = end;
    }

    /**
     * Returns a window over a range of a character array.
     */
    public static @NotNull Slice of(char @NotNull [] chars, int start, int end) {
        return new Slice(chars, start, Math.max(start, end));
    }

    /**
     * Returns an empty window.
     */
    public static @NotNull Slice empty() {
        return new Slice(new char[0], 0, 0);
    }

    /**
     * Returns an independent window over the same range as this one.
     */
    public @NotNull Slice copy() {
        return new Slice(chars, start, end);
    }

    @Override
    public int length() {
        return end - start;
    }

    @Override
    public char charAt(int index) {
        return chars[start + index];
    }

    @Override
    public @NotNull CharSequence subSequence(int begin, int stop) {
        return copy().sub(begin, stop);
    }

    /**
     * Returns whether the window has no characters.
     */
    public boolean isEmpty() {
        return start >= end;
    }

    /**
     * Returns the backing character array.
     */
    public char @NotNull [] array() {
        return chars;
    }

    /**
     * Returns the absolute start offset in the backing array.
     */
    public int start() {
        return start;
    }

    /**
     * Returns the absolute end offset in the backing array.
     */
    public int end() {
        return end;
    }

    /**
     * Narrows this window to a sub range measured from its start and returns it.
     */
    public @NotNull Slice sub(int begin, int stop) {
        int s = start + begin;
        this.end = start + stop;
        this.start = s;
        return this;
    }

    /**
     * Narrows this window to begin at an offset measured from its start and returns it.
     */
    public @NotNull Slice sub(int begin) {
        this.start += begin;
        return this;
    }

    /**
     * Narrows this window past leading and trailing ASCII whitespace and returns it.
     */
    public @NotNull Slice trim() {
        while (start < end && chars[start] <= ' ') {
            start++;
        }
        while (end > start && chars[end - 1] <= ' ') {
            end--;
        }
        return this;
    }

    /**
     * Returns whether the window begins with a character.
     */
    public boolean startsWith(char c) {
        return start < end && chars[start] == c;
    }

    /**
     * Returns whether the window ends with a character.
     */
    public boolean endsWith(char c) {
        return start < end && chars[end - 1] == c;
    }

    /**
     * Returns whether the window begins with a string.
     */
    public boolean startsWith(@NotNull String prefix) {
        int n = prefix.length();
        if (n > end - start) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (chars[start + i] != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index of a character measured from this window's start, or minus one when absent.
     */
    public int indexOf(char c) {
        for (int i = start; i < end; i++) {
            if (chars[i] == c) {
                return i - start;
            }
        }
        return -1;
    }

    /**
     * Returns whether the window equals a string character for character.
     */
    public boolean contentEquals(@NotNull String other) {
        if (other.length() != end - start) {
            return false;
        }
        for (int i = 0; i < other.length(); i++) {
            if (chars[start + i] != other.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull String toString() {
        return new String(chars, start, end - start);
    }
}
