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
     * Splits the YAML into lines and pre-computes indentation and first character
     * for each line. This upfront computation speeds up subsequent parsing operations.
     * </p>
     *
     * @param yaml the YAML content to parse
     */
    public ParseContext(@NotNull String yaml) {
        this.lines = splitLines(yaml);
        this.totalLines = lines.length;
        this.lineIndents = new int[totalLines];
        this.lineFirstChars = new char[totalLines];
        for (int i = 0; i < totalLines; i++) {
            lineIndents[i] = computeIndentation(lines[i]);
            lineFirstChars[i] = computeFirstNonSpaceChar(lines[i]);
        }
        this.currentLine = 0;
    }

    /**
     * Splits a YAML string into individual lines.
     * Handles both Unix (\n) and Windows (\r\n) line endings.
     *
     * @param yaml the YAML content
     * @return array of lines
     */
    @NotNull
    private String[] splitLines(@NotNull String yaml) {
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

        String[] result = new String[lineCount];
        int lineIdx = 0;
        int start = 0;
        for (int i = 0; i < len; i++) {
            char c = yaml.charAt(i);
            if (c == '\n') {
                result[lineIdx++] = yaml.substring(start, i);
                start = i + 1;
            } else if (c == '\r') {
                result[lineIdx++] = yaml.substring(start, i);
                if (i + 1 < len && yaml.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
        }
        result[lineIdx] = yaml.substring(start);
        return result;
    }

    /**
     * Computes the indentation level of a line.
     * Spaces count as 1, tabs count as 2.
     *
     * @param line the line to measure
     * @return the indentation count
     */
    private int computeIndentation(@NotNull String line) {
        int len = line.length();
        int count = 0;
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 2;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Finds the first non-whitespace character in a line.
     *
     * @param line the line to examine
     * @return the first non-space/tab character, or 0 if line is empty/whitespace-only
     */
    private char computeFirstNonSpaceChar(@NotNull String line) {
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t') return c;
        }
        return 0;
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
