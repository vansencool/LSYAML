package net.vansencool.lsyaml.parser;

import org.jetbrains.annotations.NotNull;

/**
 * Holds shared parsing state during YAML parsing.
 * <p>
 * This context manages line-by-line traversal of a YAML document, providing quick access
 * to pre-computed line properties like indentation and first non-whitespace character.
 * Parser components share this context to maintain a consistent position while parsing.
 * </p>
 * <p>
 * The arrays (lines, indents, firstChars) are computed once during construction for
 * performance, avoiding repeated string operations during parsing.
 * </p>
 */
public class ParseContext {

    private final String[] lines;
    private final int[] lineIndents;
    private final char[] lineFirstChars;
    private final int totalLines;
    private int currentLine;

    /**
     * Creates a new parse context from the given YAML string.
     * <p>
     * Performs a single pass over the YAML to split lines, compute indentation,
     * and determine the first non-whitespace character of each line.
     * </p>
     *
     * @param yaml the YAML content to parse
     */
    public ParseContext(@NotNull String yaml) {
        int len = yaml.length();

        int lineCount = 1;
        for (int i = 0; i < len; i++) {
            char c = yaml.charAt(i);
            if (c == '\n') {
                lineCount++;
            } else if (c == '\r') {
                lineCount++;
                if (i + 1 < len && yaml.charAt(i + 1) == '\n') {
                    i++;
                }
            }
        }

        this.lines = new String[lineCount];
        this.lineIndents = new int[lineCount];
        this.lineFirstChars = new char[lineCount];
        this.totalLines = lineCount;

        int lineIdx = 0;
        int start = 0;
        for (int i = 0; i < len; i++) {
            char c = yaml.charAt(i);
            if (c == '\n') {
                storeLine(yaml, start, i, lineIdx++);
                start = i + 1;
            } else if (c == '\r') {
                storeLine(yaml, start, i, lineIdx++);
                if (i + 1 < len && yaml.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
        }
        storeLine(yaml, start, len, lineIdx);

        this.currentLine = 0;
    }

    /**
     * Stores a line and computes its indentation and first non-whitespace character
     * in a single scan, avoiding separate passes over the same data.
     *
     * @param yaml    the full YAML string
     * @param start   the start index of the line (inclusive)
     * @param end     the end index of the line (exclusive)
     * @param lineIdx the line index to store into
     */
    private void storeLine(@NotNull String yaml, int start, int end, int lineIdx) {
        lines[lineIdx] = yaml.substring(start, end);

        int indent = 0;
        char firstChar = 0;
        for (int j = start; j < end; j++) {
            char c = yaml.charAt(j);
            if (c == ' ') {
                indent++;
            } else if (c == '\t') {
                indent += 2;
            } else {
                firstChar = c;
                break;
            }
        }
        lineIndents[lineIdx] = indent;
        lineFirstChars[lineIdx] = firstChar;
    }

    /**
     * Returns the raw lines array for direct manipulation.
     * Use with caution - modifications affect parsing state.
     *
     * @return the mutable lines array
     */
    @NotNull
    public String[] lines() {
        return lines;
    }

    /**
     * Returns the pre-computed indentation values for all lines.
     * Use with caution - modifications affect parsing state.
     *
     * @return the mutable indentation array
     */
    public int[] lineIndents() {
        return lineIndents;
    }

    /**
     * Returns the pre-computed first characters for all lines.
     * Use with caution - modifications affect parsing state.
     *
     * @return the mutable first-char array
     */
    public char[] lineFirstChars() {
        return lineFirstChars;
    }

    /**
     * Returns the total number of lines in the document.
     *
     * @return total line count
     */
    public int totalLines() {
        return totalLines;
    }

    /**
     * Returns the current line index (0-based).
     *
     * @return current parsing position
     */
    public int currentLine() {
        return currentLine;
    }

    /**
     * Sets the current line index for parsing.
     * Used when backtracking or skipping lines.
     *
     * @param line the line index to jump to (0-based)
     */
    public void setCurrentLine(int line) {
        this.currentLine = line;
    }

    /**
     * Advances to the next line.
     * Equivalent to {@code setCurrentLine(currentLine() + 1)}.
     */
    public void advanceLine() {
        currentLine++;
    }

    /**
     * Checks if there are more lines to parse.
     *
     * @return true if currentLine is less than totalLines
     */
    public boolean hasMoreLines() {
        return currentLine < totalLines;
    }

    /**
     * Returns the content of a specific line.
     *
     * @param index the line index (0-based)
     * @return the line content
     */
    @NotNull
    public String line(int index) {
        return lines[index];
    }

    /**
     * Returns the pre-computed indentation of a specific line.
     *
     * @param index the line index (0-based)
     * @return the indentation level
     */
    public int indent(int index) {
        return lineIndents[index];
    }

    /**
     * Returns the content of the current line.
     * Shorthand for {@code line(currentLine())}.
     *
     * @return the current line content
     */
    @NotNull
    public String currentLineContent() {
        return lines[currentLine];
    }

    /**
     * Returns the indentation of the current line.
     * Shorthand for {@code indent(currentLine())}.
     *
     * @return the current line's indentation level
     */
    public int currentIndent() {
        return lineIndents[currentLine];
    }

    /**
     * Returns the first non-whitespace character of the current line.
     * Shorthand for {@code firstChar(currentLine())}.
     *
     * @return the first character, or 0 if line is empty/whitespace
     */
    public char currentFirstChar() {
        return lineFirstChars[currentLine];
    }
}
