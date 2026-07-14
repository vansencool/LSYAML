package net.vansencool.lsyaml.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata associated with a YAML node.
 * Contains position information and anchor or alias references.
 */
@SuppressWarnings("unused")
public class NodeMetadata {

    private int line;
    private int column;
    private @Nullable String anchor;
    private @Nullable String alias;

    /**
     * Creates a new NodeMetadata instance with default values.
     * Line and column are set to -1 (unknown), and no anchor or alias.
     */
    public NodeMetadata() {
        this.line = -1;
        this.column = -1;
        this.anchor = null;
        this.alias = null;
    }

    /**
     * Creates a new NodeMetadata instance with the specified line and column.
     *
     * @param line   the line number (1-based)
     * @param column the column number (1-based)
     */
    public NodeMetadata(int line, int column) {
        this.line = line;
        this.column = column;
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
     * Returns the anchor name if this node defines one.
     *
     * @return the anchor name, or null
     */
    public @Nullable String getAnchor() {
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
    public @Nullable String getAlias() {
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
    public @NotNull NodeMetadata copy() {
        NodeMetadata copy = new NodeMetadata(line, column);
        copy.anchor = this.anchor;
        copy.alias = this.alias;
        return copy;
    }
}
