package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.NodeMetadata;
import net.vansencool.lsyaml.node.modifier.ListEntryModifier;
import net.vansencool.lsyaml.node.type.NodeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a YAML list.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ListNode extends AbstractYamlNode implements Iterable<YamlNode> {

    private final @NotNull List<ListEntry> entries;
    private @NotNull CollectionStyle style;
    private boolean multiLineFlow;
    private int flowIndent;

    public ListNode() {
        super();
        this.entries = new ArrayList<>();
        this.style = CollectionStyle.BLOCK;
        this.multiLineFlow = false;
        this.flowIndent = 2;
    }

    public ListNode(@NotNull CollectionStyle style) {
        super();
        this.entries = new ArrayList<>();
        this.style = style;
        this.multiLineFlow = false;
        this.flowIndent = 2;
    }

    public ListNode(@NotNull NodeMetadata metadata) {
        super(metadata);
        this.entries = new ArrayList<>();
        this.style = CollectionStyle.BLOCK;
        this.multiLineFlow = false;
        this.flowIndent = 2;
    }

    @Override
    public @NotNull NodeType getType() {
        return NodeType.LIST;
    }

    /**
     * Returns the collection style.
     *
     * @return the style
     */
    public @NotNull CollectionStyle getStyle() {
        return style;
    }

    /**
     * Sets the collection style.
     *
     * @param style the style to set
     */
    public void setStyle(@NotNull CollectionStyle style) {
        this.style = style;
    }

    /**
     * Returns whether this flow-style list should be formatted across multiple lines.
     *
     * @return true if multi-line flow
     */
    public boolean isMultiLineFlow() {
        return multiLineFlow;
    }

    /**
     * Sets whether this flow-style list should be formatted across multiple lines.
     *
     * @param multiLineFlow true to use multi-line formatting
     */
    public void setMultiLineFlow(boolean multiLineFlow) {
        this.multiLineFlow = multiLineFlow;
    }

    /**
     * Returns the indentation for multi-line flow content.
     *
     * @return the flow indent
     */
    public int getFlowIndent() {
        return flowIndent;
    }

    /**
     * Sets the indentation for multi-line flow content.
     *
     * @param flowIndent the indent to use
     */
    public void setFlowIndent(int flowIndent) {
        this.flowIndent = flowIndent;
    }

    /**
     * Returns the number of items.
     *
     * @return item count
     */
    public int size() {
        return entries.size();
    }

    /**
     * Checks if this list is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Gets the value at the given index.
     *
     * @param index the index
     * @return the value node
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull YamlNode get(int index) {
        return entries.get(index).getValue();
    }

    /**
     * Gets the entry at the given index.
     *
     * @param index the index
     * @return the entry
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListEntry getEntry(int index) {
        return entries.get(index);
    }

    /**
     * Gets all entries.
     *
     * @return list of entries
     */
    public @NotNull List<ListEntry> entries() {
        return entries;
    }

    /**
     * Gets a string value at the given index.
     *
     * @param index the index
     * @return the string value, or null
     */
    public @Nullable String getString(int index) {
        YamlNode node = get(index);
        if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getStringValue();
        }
        return null;
    }

    /**
     * Gets a map at the given index.
     *
     * @param index the index
     * @return the map node, or null
     */
    public @Nullable MapNode getMap(int index) {
        YamlNode node = get(index);
        if (node instanceof MapNode) {
            return (MapNode) node;
        }
        return null;
    }

    /**
     * Gets a nested list at the given index.
     *
     * @param index the index
     * @return the list node, or null
     */
    public @Nullable ListNode getList(int index) {
        YamlNode node = get(index);
        if (node instanceof ListNode) {
            return (ListNode) node;
        }
        return null;
    }

    /**
     * Adds a value to the end.
     *
     * @param value the value to add
     * @return this list for chaining
     */
    public @NotNull ListNode add(@NotNull YamlNode value) {
        entries.add(new ListEntry(value));
        return this;
    }

    /**
     * Adds a string value.
     *
     * @param value the string value
     * @return this list for chaining
     */
    public @NotNull ListNode add(@NotNull String value) {
        return add(new ScalarNode(value));
    }

    /**
     * Adds an integer value.
     *
     * @param value the integer value
     * @return this list for chaining
     */
    public @NotNull ListNode add(int value) {
        return add(new ScalarNode(value));
    }

    /**
     * Adds a boolean value.
     *
     * @param value the boolean value
     * @return this list for chaining
     */
    public @NotNull ListNode add(boolean value) {
        return add(new ScalarNode(value));
    }

    /**
     * Adds an entry with full metadata.
     *
     * @param entry the entry to add
     * @return this list for chaining
     */
    public @NotNull ListNode addEntry(@NotNull ListEntry entry) {
        entries.add(entry);
        return this;
    }

    /**
     * Gets a fluent modifier for an entry at the given index.
     *
     * @param index the index to modify
     * @return a list entry modifier for fluent configuration
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListEntryModifier modify(int index) {
        return new ListEntryModifier(this, index);
    }

    /**
     * Sets the value at the given index to a string.
     *
     * @param index the index
     * @param value the string value
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode setString(int index, @NotNull String value) {
        return set(index, new ScalarNode(value));
    }

    /**
     * Sets the value at the given index to an integer.
     *
     * @param index the index
     * @param value the integer value
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode setInt(int index, int value) {
        return set(index, new ScalarNode(value));
    }

    /**
     * Sets the value at the given index to a long.
     *
     * @param index the index
     * @param value the long value
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode setLong(int index, long value) {
        return set(index, new ScalarNode(value));
    }

    /**
     * Sets the value at the given index to a double.
     *
     * @param index the index
     * @param value the double value
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode setDouble(int index, double value) {
        return set(index, new ScalarNode(value));
    }

    /**
     * Sets the value at the given index to a boolean.
     *
     * @param index the index
     * @param value the boolean value
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode setBoolean(int index, boolean value) {
        return set(index, new ScalarNode(value));
    }

    /**
     * Replaces the comments before the entry at the given index.
     *
     * @param index    the index
     * @param comments the comment texts (without #)
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode setComments(int index, @NotNull String... comments) {
        ListEntry entry = entries.get(index);
        List<AdjacentLine> lines = new ArrayList<>();
        for (String comment : comments) {
            lines.add(AdjacentLine.comment(comment));
        }
        for (AdjacentLine line : entry.getLeadingLines()) {
            if (line.isBlank()) {
                lines.add(line);
            }
        }
        entry.setLeadingLines(lines);
        return this;
    }

    /**
     * Adds a comment before the entry at the given index.
     *
     * @param index   the index
     * @param comment the comment text (without #)
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode addComment(int index, @NotNull String comment) {
        entries.get(index).addLeadingLine(AdjacentLine.comment(comment));
        return this;
    }

    /**
     * Clears all comments before the entry at the given index.
     *
     * @param index the index
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode clearComments(int index) {
        ListEntry entry = entries.get(index);
        List<AdjacentLine> lines = new ArrayList<>();
        for (AdjacentLine line : entry.getLeadingLines()) {
            if (line.isBlank()) {
                lines.add(line);
            }
        }
        entry.setLeadingLines(lines);
        return this;
    }

    /**
     * Sets the inline comment for the entry at the given index.
     * Pass {@code null} to remove an existing inline comment.
     *
     * @param index   the index
     * @param comment the comment text (without #), or null to remove
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode setInlineComment(int index, @Nullable String comment) {
        entries.get(index).setInlineComment(comment);
        return this;
    }

    /**
     * Sets the number of blank lines before the entry at the given index.
     *
     * @param index the index
     * @param count the number of empty lines (clamped to zero if negative)
     * @return this list for chaining
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public @NotNull ListNode setEmptyLinesBefore(int index, int count) {
        ListEntry entry = entries.get(index);
        List<AdjacentLine> lines = new ArrayList<>();
        for (AdjacentLine line : entry.getLeadingLines()) {
            if (line.isComment()) {
                lines.add(line);
            }
        }
        for (int i = 0; i < Math.max(0, count); i++) {
            lines.add(AdjacentLine.blank());
        }
        entry.setLeadingLines(lines);
        return this;
    }

    /**
     * Adds a value with a comment before it.
     *
     * @param value         the value to add
     * @param commentBefore the comment text (without #)
     * @return this list for chaining
     */
    public @NotNull ListNode addWithComment(@NotNull YamlNode value, @NotNull String commentBefore) {
        ListEntry entry = new ListEntry(value);
        entry.addLeadingLine(AdjacentLine.comment(commentBefore));
        return addEntry(entry);
    }

    /**
     * Adds a string value with a comment before it.
     *
     * @param value         the string value
     * @param commentBefore the comment text (without #)
     * @return this list for chaining
     */
    public @NotNull ListNode addWithComment(@NotNull String value, @NotNull String commentBefore) {
        return addWithComment(new ScalarNode(value), commentBefore);
    }

    /**
     * Adds a trailing comment at the end of this list.
     *
     * @param comment the comment text (without #)
     * @return this list for chaining
     */
    public @NotNull ListNode addTrailingComment(@NotNull String comment) {
        addTrailingLine(AdjacentLine.comment(comment));
        return this;
    }

    /**
     * Sets all trailing comments, replacing existing ones.
     *
     * @param comments the comment texts (without #)
     * @return this list for chaining
     */
    public @NotNull ListNode setTrailingComments(@NotNull String... comments) {
        setTrailingComments(Arrays.asList(comments));
        return this;
    }

    /**
     * Clears all trailing comments.
     *
     * @return this list for chaining
     */
    public @NotNull ListNode clearTrailingComments() {
        setTrailingLines(List.of());
        return this;
    }

    /**
     * Inserts a value at the given index.
     *
     * @param index the index
     * @param value the value to insert
     * @return this list for chaining
     */
    public @NotNull ListNode insert(int index, @NotNull YamlNode value) {
        entries.add(index, new ListEntry(value));
        return this;
    }

    /**
     * Sets the value at the given index.
     *
     * @param index the index
     * @param value the value to set
     * @return this list for chaining
     */
    public @NotNull ListNode set(int index, @NotNull YamlNode value) {
        entries.get(index).setValue(value);
        return this;
    }

    /**
     * Removes the value at the given index.
     *
     * @param index the index
     * @return the removed value
     */
    public @NotNull YamlNode remove(int index) {
        return entries.remove(index).getValue();
    }

    /**
     * Clears all items.
     */
    public void clear() {
        entries.clear();
    }

    @Override
    public @NotNull Iterator<YamlNode> iterator() {
        return new Iterator<>() {
            private int at = 0;

            @Override
            public boolean hasNext() {
                return at < entries.size();
            }

            @Override
            public @NotNull YamlNode next() {
                return entries.get(at++).getValue();
            }
        };
    }

    @Override
    public @NotNull YamlNode copy() {
        ListNode copy = new ListNode(metadata.copy());
        copy.style = this.style;
        copy.multiLineFlow = this.multiLineFlow;
        copy.flowIndent = this.flowIndent;
        for (ListEntry entry : entries) {
            copy.addEntry(entry.copy());
        }
        copyCommentsTo(copy);
        return copy;
    }

    @Override
    public @NotNull String toYaml(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildCommentPrefix(indent, currentLevel));

        if (metadata.hasAnchor()) {
            sb.append("&").append(metadata.getAnchor()).append(" ");
        }

        if (style == CollectionStyle.FLOW) {
            sb.append(toFlowYaml(indent, currentLevel));
            sb.append(buildInlineComment());
            sb.append(buildTrailingComments(indent, currentLevel));
        } else {
            sb.append(toBlockYaml(indent, currentLevel));
        }

        return sb.toString();
    }

    @NotNull String toYamlWithoutAnchor(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildCommentPrefix(indent, currentLevel));

        if (style == CollectionStyle.FLOW) {
            sb.append(toFlowYaml(indent, currentLevel));
            sb.append(buildInlineComment());
        } else {
            sb.append(toBlockYaml(indent, currentLevel));
        }

        return sb.toString();
    }

    private @NotNull String toFlowYaml(int indent, int currentLevel) {
        if (multiLineFlow) {
            return toMultiLineFlowYaml(indent, currentLevel);
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (ListEntry entry : entries) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.getValue().toYaml(2, 0));
        }
        sb.append("]");
        return sb.toString();
    }

    private @NotNull String toMultiLineFlowYaml(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder("[\n");
        int parentLevel = Math.max(0, currentLevel - 1);
        String entryIndent = " ".repeat(indent * parentLevel + indent);
        String closingIndent = " ".repeat(indent * parentLevel);
        boolean first = true;
        for (ListEntry entry : entries) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append(entryIndent).append(entry.getValue().toYaml(indent, currentLevel));
        }
        sb.append("\n").append(closingIndent).append("]");
        return sb.toString();
    }

    private @NotNull String toBlockYaml(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        String indentStr = " ".repeat(indent * currentLevel);

        boolean first = true;
        for (ListEntry entry : entries) {
            if (!first || currentLevel > 0) {
                sb.append("\n");
            }
            first = false;

            for (AdjacentLine line : entry.getLeadingLines()) {
                if (line.isComment()) {
                    sb.append(indentStr).append("#").append(line.content()).append("\n");
                } else {
                    sb.append("\n");
                }
            }

            sb.append(indentStr).append("-");

            YamlNode value = entry.getValue();
            if (value instanceof MapNode || value instanceof ListNode) {
                if (value.getMetadata().hasAnchor()) {
                    sb.append(" &").append(value.getMetadata().getAnchor());
                }
                if (entry.getInlineComment() != null) {
                    sb.append(" #").append(entry.getInlineComment());
                }
                String nested = (value instanceof MapNode)
                        ? ((MapNode) value).toYamlWithoutAnchor(indent, currentLevel + 1)
                        : ((ListNode) value).toYamlWithoutAnchor(indent, currentLevel + 1);
                if (nested.startsWith("\n")) {
                    sb.append(nested);
                } else {
                    sb.append(" ").append(nested);
                }
            } else {
                sb.append(" ").append(value.toYaml(indent, currentLevel + 1));
                if (entry.getInlineComment() != null) {
                    sb.append(" #").append(entry.getInlineComment());
                }
            }
        }

        sb.append(buildTrailingComments(indent, currentLevel));

        return sb.toString();
    }

    @Override
    public String toString() {
        return "ListNode{size=" + entries.size() + ", style=" + style + "}";
    }

    /**
     * Represents a single entry in a list with full metadata.
     */
    public static class ListEntry {

        private @NotNull YamlNode value;
        private @Nullable List<AdjacentLine> leadingLines;
        private @Nullable String inlineComment;

        public ListEntry(@NotNull YamlNode value) {
            this.value = value;
            this.inlineComment = null;
        }

        public @NotNull YamlNode getValue() {
            return value;
        }

        public void setValue(@NotNull YamlNode value) {
            this.value = value;
        }

        /**
         * Returns the list of comments before this entry.
         */
        public @NotNull List<String> getCommentsBefore() {
            return AbstractYamlNode.commentsOf(leadingLines);
        }

        /**
         * Sets the comments before this entry, replacing existing ones.
         */
        public void setCommentsBefore(@NotNull List<String> comments) {
            this.leadingLines = AbstractYamlNode.mergeComments(leadingLines, comments);
        }

        /**
         * Adds a comment before this entry.
         */
        public void addCommentBefore(@NotNull String comment) {
            addLeadingLine(AdjacentLine.comment(comment));
        }

        /**
         * Returns the number of blank lines before this entry.
         */
        public int getEmptyLinesBefore() {
            return AbstractYamlNode.blanksOf(leadingLines);
        }

        /**
         * Sets the number of blank lines before this entry.
         */
        public void setEmptyLinesBefore(int count) {
            this.leadingLines = AbstractYamlNode.mergeBlanks(leadingLines, Math.max(0, count));
        }

        /**
         * Returns the blank and comment lines before this entry, in source order.
         */
        public @NotNull List<AdjacentLine> getLeadingLines() {
            return leadingLines == null ? List.of() : leadingLines;
        }

        /**
         * Replaces the ordered blank and comment lines before this entry.
         */
        public void setLeadingLines(@NotNull List<AdjacentLine> lines) {
            this.leadingLines = lines.isEmpty() ? null : new ArrayList<>(lines);
        }

        /**
         * Appends one blank or comment line before this entry.
         */
        public void addLeadingLine(@NotNull AdjacentLine line) {
            if (this.leadingLines == null) {
                this.leadingLines = new ArrayList<>();
            }
            this.leadingLines.add(line);
        }

        public @Nullable String getInlineComment() {
            return inlineComment;
        }

        public void setInlineComment(@Nullable String inlineComment) {
            this.inlineComment = inlineComment;
        }

        public @NotNull ListEntry copy() {
            ListEntry copy = new ListEntry(value.copy());
            copy.leadingLines = this.leadingLines == null ? null : new ArrayList<>(this.leadingLines);
            copy.inlineComment = this.inlineComment;
            return copy;
        }
    }
}
