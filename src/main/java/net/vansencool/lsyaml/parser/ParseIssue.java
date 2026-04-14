package net.vansencool.lsyaml.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a parsing issue with detailed location and context information.
 * Used by strict mode to provide comprehensive error reporting.
 */
@SuppressWarnings("unused")
public final class ParseIssue {

    private final Severity severity;
    private final String message;
    private final int line;
    private final int column;
    private final String lineContent;
    private final List<String> contextBefore;
    private final List<String> contextAfter;

    private ParseIssue(@NotNull Severity severity, @NotNull String message, int line, int column,
                       @Nullable String lineContent, @NotNull List<String> contextBefore,
                       @NotNull List<String> contextAfter) {
        this.severity = severity;
        this.message = message;
        this.line = line;
        this.column = column;
        this.lineContent = lineContent;
        this.contextBefore = contextBefore;
        this.contextAfter = contextAfter;
    }

    /**
     * Creates an error issue.
     *
     * @param message the error message
     * @param line    the line number (1-based)
     * @param column  the column number (1-based)
     * @param lines   all lines of the document
     * @return the issue
     */
    @NotNull
    public static ParseIssue error(@NotNull String message, int line, int column, @NotNull String[] lines) {
        return create(Severity.ERROR, message, line, column, lines);
    }

    /**
     * Creates a warning issue.
     *
     * @param message the warning message
     * @param line    the line number (1-based)
     * @param column  the column number (1-based)
     * @param lines   all lines of the document
     * @return the issue
     */
    @NotNull
    public static ParseIssue warning(@NotNull String message, int line, int column, @NotNull String[] lines) {
        return create(Severity.WARNING, message, line, column, lines);
    }

    @NotNull
    private static ParseIssue create(@NotNull Severity severity, @NotNull String message,
                                     int line, int column, @NotNull String[] lines) {
        String lineContent = null;
        List<String> before = List.of();
        List<String> after = List.of();

        if (line >= 1 && line <= lines.length) {
            lineContent = lines[line - 1];

            int startBefore = Math.max(0, line - 3);
            int endBefore = line - 1;
            if (startBefore < endBefore) {
                before = List.of(Arrays.copyOfRange(lines, startBefore, endBefore));
            }

            int endAfter = Math.min(lines.length, line + 2);
            if (line < endAfter) {
                after = List.of(Arrays.copyOfRange(lines, line, endAfter));
            }
        }

        return new ParseIssue(severity, message, line, column, lineContent, before, after);
    }

    /**
     * @return the severity level
     */
    @NotNull
    public Severity getSeverity() {
        return severity;
    }

    /**
     * @return the error/warning message
     */
    @NotNull
    public String getMessage() {
        return message;
    }

    /**
     * @return the line number (1-based)
     */
    public int getLine() {
        return line;
    }

    /**
     * @return the column number (1-based)
     */
    public int getColumn() {
        return column;
    }

    /**
     * @return the content of the problematic line, or null if unavailable
     */
    @Nullable
    public String getLineContent() {
        return lineContent;
    }

    /**
     * @return lines before the issue (for context)
     */
    @NotNull
    public List<String> getContextBefore() {
        return contextBefore;
    }

    /**
     * @return lines after the issue (for context)
     */
    @NotNull
    public List<String> getContextAfter() {
        return contextAfter;
    }

    /**
     * @return true if this is an error
     */
    public boolean isError() {
        return severity == Severity.ERROR;
    }

    /**
     * @return true if this is a warning
     */
    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    /**
     * Formats this issue as a detailed, readable string with context.
     *
     * @return formatted issue string
     */
    @NotNull
    public String format() {
        StringBuilder sb = new StringBuilder();

        String icon = severity == Severity.ERROR ? "X" : "!";
        String label = severity == Severity.ERROR ? "ERROR" : "WARNING";

        sb.append("\n");
        sb.append("+-").append("-".repeat(70)).append("-+\n");
        sb.append("| ").append(icon).append(" ").append(label).append(" at line ").append(line);
        if (column > 0) {
            sb.append(", column ").append(column);
        }
        sb.append(" ".repeat(Math.max(0, 70 - label.length() - String.valueOf(line).length() -
                String.valueOf(column).length() - 22))).append(" |\n");
        sb.append("+-").append("-".repeat(70)).append("-+\n");
        sb.append("| ").append(message);
        sb.append(" ".repeat(Math.max(0, 70 - message.length()))).append(" |\n");
        sb.append("+-").append("-".repeat(70)).append("-+\n");

        if (!contextBefore.isEmpty() || lineContent != null || !contextAfter.isEmpty()) {
            sb.append("|\n");

            int startLine = line - contextBefore.size();
            for (int i = 0; i < contextBefore.size(); i++) {
                int lineNum = startLine + i;
                sb.append("|   ").append(String.format("%4d", lineNum)).append(" | ")
                        .append(contextBefore.get(i)).append("\n");
            }

            if (lineContent != null) {
                sb.append("| > ").append(String.format("%4d", line)).append(" | ")
                        .append(lineContent).append("\n");

                if (column > 0) {
                    sb.append("|        ").append(" ".repeat(column - 1)).append("^");
                    if (column < lineContent.length()) {
                        sb.append("~".repeat(Math.min(5, lineContent.length() - column)));
                    }
                    sb.append("\n");
                }
            }

            for (int i = 0; i < contextAfter.size(); i++) {
                int lineNum = line + 1 + i;
                sb.append("|   ").append(String.format("%4d", lineNum)).append(" | ")
                        .append(contextAfter.get(i)).append("\n");
            }

            sb.append("|\n");
        }

        sb.append("+").append("-".repeat(72)).append("+\n");

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] Line %d, Col %d: %s", severity, line, column, message);
    }

    /**
     * Severity level of the issue.
     */
    public enum Severity {
        /** Indicates a critical problem that prevents parsing from succeeding. */
        ERROR,
        /** Indicates a non-critical issue that may not prevent parsing but should be noted. */
        WARNING
    }
}
