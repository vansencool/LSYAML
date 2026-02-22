package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.NodeMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a YAML sequence (list).
 * Preserves order and all formatting metadata.
 */
public class ListNode extends AbstractYamlNode implements Iterable<YamlNode> {

    private final @NotNull List<ListEntry> entries;
    private @NotNull CollectionStyle style;

    public ListNode() {
        super();
        this.entries = new ArrayList<>();
        this.style = CollectionStyle.BLOCK;
    }

    public ListNode(@NotNull CollectionStyle style) {
        super();
        this.entries = new ArrayList<>();
        this.style = style;
    }

    public ListNode(@NotNull NodeMetadata metadata) {
        super(metadata);
        this.entries = new ArrayList<>();
        this.style = CollectionStyle.BLOCK;
    }

    @Override
    @NotNull
    public NodeType getType() {
        return NodeType.LIST;
    }

    /**
     * Returns the collection style.
     *
     * @return the style
     */
    @NotNull
    public CollectionStyle getStyle() {
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
    @NotNull
    public YamlNode get(int index) {
        return entries.get(index).getValue();
    }

    /**
     * Gets the entry at the given index.
     *
     * @param index the index
     * @return the entry
     * @throws IndexOutOfBoundsException if index is invalid
     */
    @NotNull
    public ListEntry getEntry(int index) {
        return entries.get(index);
    }

    /**
     * Gets all entries.
     *
     * @return list of entries
     */
    @NotNull
    public List<ListEntry> entries() {
        return entries;
    }

    /**
     * Gets a string value at the given index.
     *
     * @param index the index
     * @return the string value, or null
     */
    @Nullable
    public String getString(int index) {
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
    @Nullable
    public MapNode getMap(int index) {
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
    @Nullable
    public ListNode getList(int index) {
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
    @NotNull
    public ListNode add(@NotNull YamlNode value) {
        entries.add(new ListEntry(value));
        return this;
    }

    /**
     * Adds a string value.
     *
     * @param value the string value
     * @return this list for chaining
     */
    @NotNull
    public ListNode add(@NotNull String value) {
        return add(new ScalarNode(value));
    }

    /**
     * Adds an integer value.
     *
     * @param value the integer value
     * @return this list for chaining
     */
    @NotNull
    public ListNode add(int value) {
        return add(new ScalarNode(value));
    }

    /**
     * Adds a boolean value.
     *
     * @param value the boolean value
     * @return this list for chaining
     */
    @NotNull
    public ListNode add(boolean value) {
        return add(new ScalarNode(value));
    }

    /**
     * Adds an entry with full metadata.
     *
     * @param entry the entry to add
     * @return this list for chaining
     */
    @NotNull
    public ListNode addEntry(@NotNull ListEntry entry) {
        entries.add(entry);
        return this;
    }

    /**
     * Inserts a value at the given index.
     *
     * @param index the index
     * @param value the value to insert
     * @return this list for chaining
     */
    @NotNull
    public ListNode insert(int index, @NotNull YamlNode value) {
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
    @NotNull
    public ListNode set(int index, @NotNull YamlNode value) {
        entries.get(index).setValue(value);
        return this;
    }

    /**
     * Removes the value at the given index.
     *
     * @param index the index
     * @return the removed value
     */
    @NotNull
    public YamlNode remove(int index) {
        return entries.remove(index).getValue();
    }

    /**
     * Clears all items.
     */
    public void clear() {
        entries.clear();
    }

    @Override
    @NotNull
    public Iterator<YamlNode> iterator() {
        return entries.stream().map(ListEntry::getValue).iterator();
    }

    @Override
    @NotNull
    public YamlNode copy() {
        ListNode copy = new ListNode(metadata.copy());
        copy.style = this.style;
        for (ListEntry entry : entries) {
            copy.addEntry(entry.copy());
        }
        copyCommentsTo(copy);
        return copy;
    }

    @Override
    @NotNull
    public String toYaml(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildCommentPrefix(indent, currentLevel));

        if (metadata.hasAnchor()) {
            sb.append("&").append(metadata.getAnchor()).append(" ");
        }

        if (style == CollectionStyle.FLOW) {
            sb.append(toFlowYaml());
            sb.append(buildInlineComment());
        } else {
            sb.append(toBlockYaml(indent, currentLevel));
        }

        return sb.toString();
    }

    private @NotNull String toFlowYaml() {
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

    private @NotNull String toBlockYaml(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        String indentStr = " ".repeat(indent * currentLevel);

        for (ListEntry entry : entries) {
            sb.append("\n");

            for (int i = 0; i < entry.getEmptyLinesBefore(); i++) {
                sb.append("\n");
            }

            for (String comment : entry.getCommentsBefore()) {
                sb.append(indentStr).append("#").append(comment).append("\n");
            }

            sb.append(indentStr).append("-");

            YamlNode value = entry.getValue();
            if (value instanceof MapNode || value instanceof ListNode) {
                if (entry.getInlineComment() != null) {
                    sb.append(" #").append(entry.getInlineComment());
                }
                String nested = value.toYaml(indent, currentLevel + 1);
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
        private @NotNull List<String> commentsBefore;
        private @Nullable String inlineComment;
        private int emptyLinesBefore;

        public ListEntry(@NotNull YamlNode value) {
            this.value = value;
            this.commentsBefore = new ArrayList<>();
            this.inlineComment = null;
            this.emptyLinesBefore = 0;
        }

        @NotNull
        public YamlNode getValue() {
            return value;
        }

        public void setValue(@NotNull YamlNode value) {
            this.value = value;
        }

        @NotNull
        public List<String> getCommentsBefore() {
            return commentsBefore;
        }

        public void setCommentsBefore(@NotNull List<String> comments) {
            this.commentsBefore = new ArrayList<>(comments);
        }

        public void addCommentBefore(@NotNull String comment) {
            this.commentsBefore.add(comment);
        }

        @Nullable
        public String getInlineComment() {
            return inlineComment;
        }

        public void setInlineComment(@Nullable String inlineComment) {
            this.inlineComment = inlineComment;
        }

        public int getEmptyLinesBefore() {
            return emptyLinesBefore;
        }

        public void setEmptyLinesBefore(int count) {
            this.emptyLinesBefore = Math.max(0, count);
        }

        @NotNull
        public ListEntry copy() {
            ListEntry copy = new ListEntry(value.copy());
            copy.commentsBefore = new ArrayList<>(this.commentsBefore);
            copy.inlineComment = this.inlineComment;
            copy.emptyLinesBefore = this.emptyLinesBefore;
            return copy;
        }
    }
}
