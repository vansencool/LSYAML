package net.vansencool.lsyaml.parser.source;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Precomputed line boundaries, indentation, and first non-whitespace character over a source.
 */
public final class LineIndex {

    private final @NotNull Source source;
    private int[] start;
    private int[] end;
    private int[] indent;
    private char[] firstChar;
    private int count;

    public LineIndex(@NotNull Source source) {
        this.source = source;
        int len = source.length();
        int cap = (len >> 4) + 2;
        this.start = new int[cap];
        this.end = new int[cap];
        this.indent = new int[cap];
        this.firstChar = new char[cap];

        int lineStart = 0;
        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                record(lineStart, i);
                lineStart = i + 1;
            } else if (c == '\r') {
                record(lineStart, i);
                if (i + 1 < len && source.charAt(i + 1) == '\n') i++;
                lineStart = i + 1;
            }
        }
        record(lineStart, len);
    }

    private void record(int lineStart, int lineEnd) {
        if (count == start.length) {
            grow();
        }
        int idx = count++;
        start[idx] = lineStart;
        end[idx] = lineEnd;

        int ind = 0;
        char first = 0;
        for (int j = lineStart; j < lineEnd; j++) {
            char c = source.charAt(j);
            if (c == ' ') {
                ind++;
            } else if (c == '\t') {
                ind += 2;
            } else {
                first = c;
                break;
            }
        }
        indent[idx] = ind;
        firstChar[idx] = first;
    }

    private void grow() {
        int cap = start.length << 1;
        start = Arrays.copyOf(start, cap);
        end = Arrays.copyOf(end, cap);
        indent = Arrays.copyOf(indent, cap);
        firstChar = Arrays.copyOf(firstChar, cap);
    }

    /**
     * Returns the number of lines.
     */
    public int count() {
        return count;
    }

    /**
     * Returns the start offset of a line.
     */
    public int start(int line) {
        return start[line];
    }

    /**
     * Returns the end offset of a line.
     */
    public int end(int line) {
        return end[line];
    }

    /**
     * Returns the indentation width of a line with tabs counted as two.
     */
    public int indent(int line) {
        return indent[line];
    }

    /**
     * Returns the first non-whitespace character of a line, or zero when blank.
     */
    public char firstChar(int line) {
        return firstChar[line];
    }

    /**
     * Returns the offset of the first non-whitespace character of a line.
     */
    public int contentStart(int line) {
        int s = start[line];
        int e = end[line];
        while (s < e) {
            char c = source.charAt(s);
            if (c != ' ' && c != '\t') break;
            s++;
        }
        return s;
    }
}

