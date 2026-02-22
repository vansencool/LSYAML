package net.vansencool.lsyaml.parser;

import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when YAML parsing fails.
 */
@SuppressWarnings("unused")
public class YamlParseException extends RuntimeException {

    private final int line;
    private final int column;

    public YamlParseException(@NotNull String message) {
        super(message);
        this.line = -1;
        this.column = -1;
    }

    public YamlParseException(@NotNull String message, int line, int column) {
        super(String.format("Line %d, column %d: %s", line, column, message));
        this.line = line;
        this.column = column;
    }

    public YamlParseException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
        this.line = -1;
        this.column = -1;
    }

    public YamlParseException(@NotNull String message, int line, int column, @NotNull Throwable cause) {
        super(String.format("Line %d, column %d: %s", line, column, message), cause);
        this.line = line;
        this.column = column;
    }

    /**
     * Returns the line number where the error occurred.
     *
     * @return line number (1-based), or -1 if unknown
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column number where the error occurred.
     *
     * @return column number (1-based), or -1 if unknown
     */
    public int getColumn() {
        return column;
    }
}
