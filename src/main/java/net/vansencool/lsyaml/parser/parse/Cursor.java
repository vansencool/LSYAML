package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.parser.source.LineIndex;
import net.vansencool.lsyaml.parser.source.Source;
import net.vansencool.lsyaml.parser.text.Scan;
import net.vansencool.lsyaml.parser.text.Slice;
import org.jetbrains.annotations.NotNull;

/**
 * Mutable position over the lines of a source with offset-based line inspection.
 */
public final class Cursor {

    private final @NotNull Source source;
    private final @NotNull LineIndex lines;
    private int line;
    private int overrideLine = -1;
    private @NotNull Slice overrideContent = Slice.empty();
    private int overrideIndent;

    public Cursor(@NotNull Source source, @NotNull LineIndex lines) {
        this.source = source;
        this.lines = lines;
        this.line = 0;
    }

    /**
     * Installs a synthetic content and indent for one line so an inline value reparses as a block.
     */
    public void override(int line, @NotNull Slice content, int indent) {
        this.overrideLine = line;
        this.overrideContent = content;
        this.overrideIndent = indent;
    }

    /**
     * Clears any synthetic line override.
     */
    public void clearOverride() {
        this.overrideLine = -1;
    }

    private boolean overridden() {
        return line == overrideLine;
    }

    /**
     * Returns the backing source.
     */
    public @NotNull Source source() {
        return source;
    }

    /**
     * Returns whether the cursor is positioned on a line.
     */
    public boolean hasMore() {
        return line < lines.count();
    }

    /**
     * Returns the current zero-based line number.
     */
    public int line() {
        return line;
    }

    /**
     * Sets the current line number.
     */
    public void line(int value) {
        this.line = value;
    }

    /**
     * Advances to the next line.
     */
    public void advance() {
        line++;
    }

    /**
     * Returns the indentation of the current line.
     */
    public int indent() {
        return overridden() ? overrideIndent : lines.indent(line);
    }

    /**
     * Returns the first non-whitespace character of the current line, or zero when blank.
     */
    public char firstChar() {
        return overridden() ? overrideContent.charAt(0) : lines.firstChar(line);
    }

    /**
     * Returns the raw content start offset of the current line.
     */
    public int start() {
        return lines.start(line);
    }

    /**
     * Returns the raw content end offset of the current line.
     */
    public int end() {
        return lines.end(line);
    }

    /**
     * Returns the offset of the first non-whitespace character of the current line.
     */
    public int contentStart() {
        return lines.contentStart(line);
    }

    /**
     * Returns a zero-copy view of the current line from its first non-whitespace character to its end.
     */
    public @NotNull Slice trimmedContent() {
        if (overridden()) {
            return overrideContent.copy();
        }
        int s = lines.contentStart(line);
        int e = Scan.trimEnd(source, s, lines.end(line));
        return source.slice(s, e);
    }
}
