package net.vansencool.lsyaml.parser.source;

import org.jetbrains.annotations.NotNull;

/**
 * Precomputed line boundaries, indentation, and first non-whitespace character over a source.
 */
public final class LineIndex {

    private final @NotNull Source source;
    private final int[] start;
    private final int[] end;
    private final int[] indent;
    private final char[] firstChar;
    private final int count;

    public LineIndex(@NotNull Source source) {
        this.source = source;
        int len = source.length();

        int lines = 1;
        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                lines++;
            } else if (c == '\r') {
                lines++;
                if (i + 1 < len && source.charAt(i + 1) == '\n') i++;
            }
        }

        this.start = new int[lines];
        this.end = new int[lines];
        this.indent = new int[lines];
        this.firstChar = new char[lines];
        this.count = lines;

        int idx = 0;
        int lineStart = 0;
        for (int i = 0; i < len; i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                record(idx++, lineStart, i);
                lineStart = i + 1;
            } else if (c == '\r') {
                record(idx++, lineStart, i);
                if (i + 1 < len && source.charAt(i + 1) == '\n') i++;
                lineStart = i + 1;
            }
        }
        record(idx, lineStart, len);
    }

    private void record(int idx, int lineStart, int lineEnd) {
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

