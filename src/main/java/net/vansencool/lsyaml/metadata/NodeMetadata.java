package net.vansencool.lsyaml.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata associated with a YAML node.
 * Contains position information, formatting details, and style preferences.
 */
@SuppressWarnings("unused")
public class NodeMetadata {

    private int line;
    private int column;
    private int indentation;
    private @NotNull String indentString;
    private @Nullable String anchor;
    private @Nullable String alias;

    /**
     * Creates a new NodeMetadata instance with default values.
     * Line and column are set to -1 (unknown), indentation is 0, and no anchor or alias.
     */
    public NodeMetadata() {
        this.line = -1;
        this.column = -1;
        this.indentation = 0;
        this.indentString = "";
        this.anchor = null;
        this.alias = null;
    }

    /**
     * Creates a new NodeMetadata instance with the specified line, column, and indentation.
     *
     * @param line        the line number (1-based)
     * @param column      the column number (1-based)
     * @param indentation the indentation level in spaces
     */
    public NodeMetadata(int line, int column, int indentation) {
        this.line = line;
        this.column = column;
        this.indentation = indentation;
        this.indentString = " ".repeat(indentation);
        this.anchor = null;
        this.alias = null;
    }

    /**
     * Returns the line number where this node starts (1-based).
     *
     * @return the line number, or -1 if not set
     */
    public int getLine() {
        return line;
    }

    /**
     * Sets the line number.
     *
     * @param line the line number (1-based)
     */
    public void setLine(int line) {
        this.line = line;
    }

    /**
     * Returns the column number where this node starts (1-based).
     *
     * @return the column number, or -1 if not set
     */
    public int getColumn() {
        return column;
    }

    /**
     * Sets the column number.
     *
     * @param column the column number (1-based)
     */
    public void setColumn(int column) {
        this.column = column;
    }

    /**
     * Returns the indentation level in spaces.
     *
     * @return the indentation level
     */
    public int getIndentation() {
        return indentation;
    }

    /**
     * Sets the indentation level.
     *
     * @param indentation the indentation level in spaces
     */
    public void setIndentation(int indentation) {
        this.indentation = indentation;
        this.indentString = " ".repeat(indentation);
    }

    /**
     * Returns the exact indentation string (may contain tabs).
     *
     * @return the indentation string
     */
    @NotNull
    public String getIndentString() {
        return indentString;
    }

    /**
     * Sets the exact indentation string.
     *
     * @param indentString the indentation string
     */
    public void setIndentString(@NotNull String indentString) {
        this.indentString = indentString;
        this.indentation = indentString.length();
    }

    /**
     * Returns the anchor name if this node defines one.
     *
     * @return the anchor name, or null
     */
    @Nullable
    public String getAnchor() {
        return anchor;
    }

    /**
     * Sets the anchor name.
     *
     * @param anchor the anchor name
     */
    public void setAnchor(@Nullable String anchor) {
        this.anchor = anchor;
    }

    /**
     * Returns the alias name if this node is an alias reference.
     *
     * @return the alias name, or null
     */
    @Nullable
    public String getAlias() {
        return alias;
    }

    /**
     * Checks if this node has an anchor.
     *
     * @return true if an anchor is defined
     */
    public boolean hasAnchor() {
        return anchor != null && !anchor.isEmpty();
    }

    /**
     * Checks if this node is an alias reference.
     *
     * @return true if this is an alias
     */
    public boolean isAlias() {
        return alias != null && !alias.isEmpty();
    }

    /**
     * Sets the alias name.
     *
     * @param alias the alias name
     */
    public void setAlias(@Nullable String alias) {
        this.alias = alias;
    }

    /**
     * Creates a copy of this metadata.
     *
     * @return a copy
     */
    @NotNull
    public NodeMetadata copy() {
        NodeMetadata copy = new NodeMetadata(line, column, indentation);
        copy.indentString = this.indentString;
        copy.anchor = this.anchor;
        copy.alias = this.alias;
        return copy;
    }
}
