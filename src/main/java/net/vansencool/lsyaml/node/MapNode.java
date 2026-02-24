package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.NodeMetadata;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.modifier.EntryModifier;
import net.vansencool.lsyaml.node.type.NodeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a YAML mapping (key-value pairs).
 * Preserves insertion order and all formatting metadata.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class MapNode extends AbstractYamlNode {

    private final @NotNull LinkedHashMap<String, MapEntry> entries;
    private @NotNull CollectionStyle style;
    private boolean multiLineFlow;
    private int flowIndent;

    /**
     * Creates a new empty MapNode with block style.
     */
    public MapNode() {
        super();
        this.entries = new LinkedHashMap<>();
        this.style = CollectionStyle.BLOCK;
        this.multiLineFlow = false;
        this.flowIndent = 2;
    }

    /**
     * Creates a new empty MapNode with the given collection style.
     *
     * @param style the collection style to use
     */
    public MapNode(@NotNull CollectionStyle style) {
        super();
        this.entries = new LinkedHashMap<>();
        this.style = style;
        this.multiLineFlow = false;
        this.flowIndent = 2;
    }

    /**
     * Creates a new empty MapNode with the given metadata.
     *
     * @param metadata the node metadata to use
     */
    public MapNode(@NotNull NodeMetadata metadata) {
        super(metadata);
        this.entries = new LinkedHashMap<>();
        this.style = CollectionStyle.BLOCK;
        this.multiLineFlow = false;
        this.flowIndent = 2;
    }

    @Override
    @NotNull
    public NodeType getType() {
        return NodeType.MAP;
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
     * Returns whether this flow-style map should be formatted across multiple lines.
     *
     * @return true if multi-line flow
     */
    public boolean isMultiLineFlow() {
        return multiLineFlow;
    }

    /**
     * Sets whether this flow-style map should be formatted across multiple lines.
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
     * Returns all keys in order.
     *
     * @return set of keys
     */
    @NotNull
    public Set<String> keys() {
        return entries.keySet();
    }

    /**
     * Returns all entries in order.
     *
     * @return collection of entries
     */
    @NotNull
    public Collection<MapEntry> entries() {
        return entries.values();
    }

    /**
     * Returns the number of entries.
     *
     * @return entry count
     */
    public int size() {
        return entries.size();
    }

    /**
     * Checks if this map is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Checks if this map contains the given key.
     *
     * @param key the key to check
     * @return true if the key exists
     */
    public boolean containsKey(@NotNull String key) {
        return entries.containsKey(key);
    }

    /**
     * Gets the value for a key.
     *
     * @param key the key
     * @return the value node, or null if not found
     */
    @Nullable
    public YamlNode get(@NotNull String key) {
        MapEntry entry = entries.get(key);
        if (entry != null) {
            return entry.getValue();
        }
        MapEntry mergeEntry = entries.get("<<");
        if (mergeEntry != null && mergeEntry.getResolvedMergeMap() != null) {
            return mergeEntry.getResolvedMergeMap().get(key);
        }
        return null;
    }

    /**
     * Gets the entry for a key.
     *
     * @param key the key
     * @return the entry, or null if not found
     */
    @Nullable
    public MapEntry getEntry(@NotNull String key) {
        return entries.get(key);
    }

    /**
     * Puts a value with the given key.
     * If the key already exists, the entry's comments and metadata are preserved.
     *
     * @param key   the key
     * @param value the value node
     * @return this map for chaining
     */
    @NotNull
    public MapNode put(@NotNull String key, @NotNull YamlNode value) {
        MapEntry existing = entries.get(key);
        if (existing != null) {
            existing.setValue(value);
        } else {
            entries.put(key, new MapEntry(key, value));
        }
        return this;
    }

    /**
     * Puts a string value.
     *
     * @param key   the key
     * @param value the string value
     * @return this map for chaining
     */
    @NotNull
    public MapNode put(@NotNull String key, @NotNull String value) {
        return put(key, new ScalarNode(value));
    }

    /**
     * Puts an integer value.
     *
     * @param key   the key
     * @param value the integer value
     * @return this map for chaining
     */
    @NotNull
    public MapNode put(@NotNull String key, int value) {
        return put(key, new ScalarNode(value));
    }

    /**
     * Puts a long value.
     *
     * @param key   the key
     * @param value the long value
     * @return this map for chaining
     */
    @NotNull
    public MapNode put(@NotNull String key, long value) {
        return put(key, new ScalarNode(value));
    }

    /**
     * Puts a double value.
     *
     * @param key   the key
     * @param value the double value
     * @return this map for chaining
     */
    @NotNull
    public MapNode put(@NotNull String key, double value) {
        return put(key, new ScalarNode(value));
    }

    /**
     * Puts a boolean value.
     *
     * @param key   the key
     * @param value the boolean value
     * @return this map for chaining
     */
    @NotNull
    public MapNode put(@NotNull String key, boolean value) {
        return put(key, new ScalarNode(value));
    }

    /**
     * Puts an entry with full metadata.
     *
     * @param entry the entry to put
     * @return this map for chaining
     */
    @NotNull
    public MapNode putEntry(@NotNull MapEntry entry) {
        entries.put(entry.getKey(), entry);
        return this;
    }

    /**
     * Gets a fluent modifier for an entry. Creates the entry if it doesn't exist.
     *
     * @param key the key to modify
     * @return an entry modifier for fluent configuration
     */
    @NotNull
    public EntryModifier modify(@NotNull String key) {
        return new EntryModifier(this, key);
    }

    /**
     * Inserts an entry before the specified existing key.
     *
     * @param key       the key to insert
     * @param value     the value
     * @param beforeKey the key to insert before
     * @return this map for chaining
     */
    @NotNull
    public MapNode insertBefore(@NotNull String key, @NotNull YamlNode value, @NotNull String beforeKey) {
        return insertBefore(new MapEntry(key, value), beforeKey);
    }

    /**
     * Inserts an entry before the specified existing key.
     *
     * @param entry     the entry to insert
     * @param beforeKey the key to insert before
     * @return this map for chaining
     */
    @NotNull
    public MapNode insertBefore(@NotNull MapEntry entry, @NotNull String beforeKey) {
        if (!entries.containsKey(beforeKey)) {
            return putEntry(entry);
        }
        LinkedHashMap<String, MapEntry> newEntries = new LinkedHashMap<>();
        for (Map.Entry<String, MapEntry> e : entries.entrySet()) {
            if (e.getKey().equals(beforeKey)) {
                newEntries.put(entry.getKey(), entry);
            }
            newEntries.put(e.getKey(), e.getValue());
        }
        entries.clear();
        entries.putAll(newEntries);
        return this;
    }

    /**
     * Inserts an entry after the specified existing key.
     *
     * @param key      the key to insert
     * @param value    the value
     * @param afterKey the key to insert after
     * @return this map for chaining
     */
    @NotNull
    public MapNode insertAfter(@NotNull String key, @NotNull YamlNode value, @NotNull String afterKey) {
        return insertAfter(new MapEntry(key, value), afterKey);
    }

    /**
     * Inserts an entry after the specified existing key.
     *
     * @param entry    the entry to insert
     * @param afterKey the key to insert after
     * @return this map for chaining
     */
    @NotNull
    public MapNode insertAfter(@NotNull MapEntry entry, @NotNull String afterKey) {
        if (!entries.containsKey(afterKey)) {
            return putEntry(entry);
        }
        LinkedHashMap<String, MapEntry> newEntries = new LinkedHashMap<>();
        for (Map.Entry<String, MapEntry> e : entries.entrySet()) {
            newEntries.put(e.getKey(), e.getValue());
            if (e.getKey().equals(afterKey)) {
                newEntries.put(entry.getKey(), entry);
            }
        }
        entries.clear();
        entries.putAll(newEntries);
        return this;
    }

    /**
     * Renames a key while preserving the entry's position and metadata.
     *
     * @param oldKey the current key name
     * @param newKey the new key name
     * @return this map for chaining
     */
    @NotNull
    public MapNode renameKey(@NotNull String oldKey, @NotNull String newKey) {
        if (!entries.containsKey(oldKey) || oldKey.equals(newKey)) {
            return this;
        }
        LinkedHashMap<String, MapEntry> newEntries = new LinkedHashMap<>();
        for (Map.Entry<String, MapEntry> e : entries.entrySet()) {
            if (e.getKey().equals(oldKey)) {
                MapEntry entry = e.getValue();
                entry.setKey(newKey);
                newEntries.put(newKey, entry);
            } else {
                newEntries.put(e.getKey(), e.getValue());
            }
        }
        entries.clear();
        entries.putAll(newEntries);
        return this;
    }

    /**
     * Replaces the comments before the entry with the given key.
     * Has no effect if the key does not exist.
     *
     * @param key      the key
     * @param comments the comment texts (without #)
     * @return this map for chaining
     */
    @NotNull
    public MapNode setComments(@NotNull String key, @NotNull String... comments) {
        MapEntry entry = entries.get(key);
        if (entry != null) {
            entry.setCommentsBefore(Arrays.asList(comments));
        }
        return this;
    }

    /**
     * Adds a comment before the entry with the given key.
     * Has no effect if the key does not exist.
     *
     * @param key     the key
     * @param comment the comment text (without #)
     * @return this map for chaining
     */
    @NotNull
    public MapNode addComment(@NotNull String key, @NotNull String comment) {
        MapEntry entry = entries.get(key);
        if (entry != null) {
            entry.addCommentBefore(comment);
        }
        return this;
    }

    /**
     * Clears all comments before the entry with the given key.
     * Has no effect if the key does not exist.
     *
     * @param key the key
     * @return this map for chaining
     */
    @NotNull
    public MapNode clearComments(@NotNull String key) {
        MapEntry entry = entries.get(key);
        if (entry != null) {
            entry.setCommentsBefore(new ArrayList<>());
        }
        return this;
    }

    /**
     * Sets the inline comment for the entry with the given key.
     * Pass {@code null} to remove an existing inline comment.
     * Has no effect if the key does not exist.
     *
     * @param key     the key
     * @param comment the comment text (without #), or null to remove
     * @return this map for chaining
     */
    @NotNull
    public MapNode setInlineComment(@NotNull String key, @Nullable String comment) {
        MapEntry entry = entries.get(key);
        if (entry != null) {
            entry.setInlineComment(comment);
        }
        return this;
    }

    /**
     * Sets the number of blank lines before the entry with the given key.
     * Has no effect if the key does not exist.
     *
     * @param key   the key
     * @param count the number of empty lines (clamped to zero if negative)
     * @return this map for chaining
     */
    @NotNull
    public MapNode setEmptyLinesBefore(@NotNull String key, int count) {
        MapEntry entry = entries.get(key);
        if (entry != null) {
            entry.setEmptyLinesBefore(count);
        }
        return this;
    }

    /**
     * Adds a trailing comment at the end of this map.
     *
     * @param comment the comment text (without #)
     * @return this map for chaining
     */
    @NotNull
    public MapNode addTrailingComment(@NotNull String comment) {
        trailingComments.add(comment);
        return this;
    }

    /**
     * Sets all trailing comments, replacing existing ones.
     *
     * @param comments the comment texts (without #)
     * @return this map for chaining
     */
    @NotNull
    public MapNode setTrailingComments(@NotNull String... comments) {
        trailingComments.clear();
        trailingComments.addAll(Arrays.asList(comments));
        return this;
    }

    /**
     * Clears all trailing comments.
     *
     * @return this map for chaining
     */
    @NotNull
    public MapNode clearTrailingComments() {
        trailingComments.clear();
        return this;
    }

    /**
     * Removes an entry by key.
     *
     * @param key the key to remove
     * @return the removed value, or null
     */
    @Nullable
    public YamlNode remove(@NotNull String key) {
        MapEntry entry = entries.remove(key);
        return entry != null ? entry.getValue() : null;
    }

    /**
     * Clears all entries.
     */
    public void clear() {
        entries.clear();
    }

    @Override
    @NotNull
    public YamlNode copy() {
        MapNode copy = new MapNode(metadata.copy());
        copy.style = this.style;
        copy.multiLineFlow = this.multiLineFlow;
        copy.flowIndent = this.flowIndent;
        for (MapEntry entry : entries.values()) {
            copy.putEntry(entry.copy());
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
            sb.append(toFlowYaml(indent, currentLevel));
            sb.append(buildInlineComment());
        } else {
            sb.append(toBlockYaml(indent, currentLevel));
        }

        return sb.toString();
    }

    @NotNull
    String toYamlWithoutAnchor(int indent, int currentLevel) {
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
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (MapEntry entry : entries.values()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.formatKey()).append(": ");
            sb.append(entry.getValue().toYaml(2, 0));
        }
        sb.append("}");
        return sb.toString();
    }

    private @NotNull String toMultiLineFlowYaml(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder("{\n");
        int parentLevel = Math.max(0, currentLevel - 1);
        String entryIndent = " ".repeat(indent * parentLevel + indent);
        String closingIndent = " ".repeat(indent * parentLevel);
        boolean first = true;
        for (MapEntry entry : entries.values()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append(entryIndent).append(entry.formatKey()).append(": ");
            sb.append(entry.getValue().toYaml(indent, currentLevel));
        }
        sb.append("\n").append(closingIndent).append("}");
        return sb.toString();
    }

    private @NotNull String toBlockYaml(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        String indentStr = " ".repeat(indent * currentLevel);

        boolean first = true;
        for (MapEntry entry : entries.values()) {
            if (!first || currentLevel > 0) {
                sb.append("\n");
            }
            first = false;

            sb.append("\n".repeat(Math.max(0, entry.getEmptyLinesBefore())));

            for (String comment : entry.getCommentsBefore()) {
                sb.append(indentStr).append("#").append(comment).append("\n");
            }

            sb.append(indentStr).append(entry.formatKey()).append(":");

            YamlNode value = entry.getValue();
            if (value instanceof MapNode mapValue && mapValue.getStyle() == CollectionStyle.FLOW) {
                sb.append(" ");
                if (entry.getInlineComment() != null) {
                    sb.append("#").append(entry.getInlineComment()).append("\n").append(indentStr);
                }
                sb.append(value.toYaml(indent, currentLevel + 1));
            } else if (value instanceof ListNode listValue && listValue.getStyle() == CollectionStyle.FLOW) {
                sb.append(" ");
                if (entry.getInlineComment() != null) {
                    sb.append("#").append(entry.getInlineComment()).append("\n").append(indentStr);
                }
                sb.append(value.toYaml(indent, currentLevel + 1));
            } else if (value instanceof MapNode || value instanceof ListNode) {
                if (value.getMetadata().hasAnchor()) {
                    sb.append(" &").append(value.getMetadata().getAnchor());
                }
                if (entry.getInlineComment() != null) {
                    sb.append(" #").append(entry.getInlineComment());
                }
                sb.append(((value instanceof MapNode)
                        ? ((MapNode) value).toYamlWithoutAnchor(indent, currentLevel + 1)
                        : ((ListNode) value).toYamlWithoutAnchor(indent, currentLevel + 1)));
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
        return "MapNode{entries=" + entries.size() + ", style=" + style + "}";
    }

    /**
     * Represents a single entry in a map with full metadata.
     */
    public static class MapEntry {

        private @NotNull String key;
        private @Nullable YamlNode complexKey;
        private @NotNull YamlNode value;
        private @NotNull ScalarStyle keyStyle;
        private @NotNull List<String> commentsBefore;
        private @Nullable String inlineComment;
        private int emptyLinesBefore;
        private @Nullable MapNode resolvedMergeMap;

        /**
         * Creates a new MapEntry with the given key and value, using plain style for the key.
         *
         * @param key   the key
         * @param value the value node
         */
        public MapEntry(@NotNull String key, @NotNull YamlNode value) {
            this.key = key;
            this.value = value;
            this.keyStyle = ScalarStyle.PLAIN;
            this.commentsBefore = new ArrayList<>();
            this.inlineComment = null;
            this.emptyLinesBefore = 0;
        }

        /**
         * Creates a new MapEntry with the given key, value, and key style.
         *
         * @param key      the key
         * @param value    the value node
         * @param keyStyle the scalar style to use for the key when emitting
         */
        public MapEntry(@NotNull String key, @NotNull YamlNode value, @NotNull ScalarStyle keyStyle) {
            this.key = key;
            this.value = value;
            this.keyStyle = keyStyle;
            this.commentsBefore = new ArrayList<>();
            this.inlineComment = null;
            this.emptyLinesBefore = 0;
        }

        /**
         * Returns the key of this entry.
         *
         * @return the key
         */
        @NotNull
        public String getKey() {
            return key;
        }

        /**
         * Sets the key of this entry. It is recommended to use {@link MapNode#renameKey}.
         *
         * @param key the new key
         */
        public void setKey(@NotNull String key) {
            this.key = key;
        }

        /**
         * Returns the complex key of this entry, or null if this entry uses a simple string key.
         * Complex keys are maps, lists, or other non-scalar keys that use the `?` YAML syntax.
         *
         * @return the complex key node, or null if using a simple string key
         */
        @Nullable
        public YamlNode getComplexKey() {
            return complexKey;
        }

        /**
         * Sets a complex key for this entry. Complex keys are maps, lists, or other non-scalar
         * keys that require the `?` YAML syntax. When a complex key is set, the simple string
         * key is used as a lookup key but the complex key is emitted.
         *
         * @param complexKey the complex key node, or null to use only the simple string key
         */
        public void setComplexKey(@Nullable YamlNode complexKey) {
            this.complexKey = complexKey;
        }

        /**
         * Returns true if this entry has a complex key (non-scalar key requiring `?` syntax).
         *
         * @return true if the entry has a complex key
         */
        public boolean hasComplexKey() {
            return complexKey != null;
        }

        /**
         * Returns the value node of this entry.
         *
         * @return the value node
         */
        @NotNull
        public YamlNode getValue() {
            return value;
        }

        /**
         * Sets the value node of this entry.
         *
         * @param value the new value node
         */
        public void setValue(@NotNull YamlNode value) {
            this.value = value;
        }

        /**
         * Returns the scalar style used for the key when emitting.
         *
         * @return the key scalar style
         */
        @NotNull
        public ScalarStyle getKeyStyle() {
            return keyStyle;
        }

        /**
         * Sets the scalar style to use for the key when emitting.
         *
         * @param keyStyle the key scalar style
         */
        public void setKeyStyle(@NotNull ScalarStyle keyStyle) {
            this.keyStyle = keyStyle;
        }

        /**
         * Returns the list of comments before this entry.
         *
         * @return list of comment texts (without #)
         */
        @NotNull
        public List<String> getCommentsBefore() {
            return commentsBefore;
        }

        /**
         * Sets the comments before this entry, replacing existing ones.
         *
         * @param comments the comment texts (without #)
         */
        public void setCommentsBefore(@NotNull List<String> comments) {
            this.commentsBefore = new ArrayList<>(comments);
        }

        /**
         * Adds a comment before this entry.
         *
         * @param comment the comment text (without #)
         */
        public void addCommentBefore(@NotNull String comment) {
            this.commentsBefore.add(comment);
        }

        /**
         * Returns the inline comment for this entry, or null if none.
         *
         * @return the inline comment text (without #), or null
         */
        @Nullable
        public String getInlineComment() {
            return inlineComment;
        }

        /**
         * Sets the inline comment for this entry. Pass null to remove an existing inline comment.
         *
         * @param inlineComment the comment text (without #), or null to remove
         */
        public void setInlineComment(@Nullable String inlineComment) {
            this.inlineComment = inlineComment;
        }

        /**
         * Returns the number of blank lines before this entry.
         *
         * @return the number of empty lines
         */
        public int getEmptyLinesBefore() {
            return emptyLinesBefore;
        }

        /**
         * Sets the number of blank lines before this entry. Negative values are treated as zero.
         *
         * @param count the number of empty lines
         */
        public void setEmptyLinesBefore(int count) {
            this.emptyLinesBefore = Math.max(0, count);
        }

        /**
         * Returns the resolved merge map for a {@code <<} alias entry, or null if not a merge entry.
         *
         * @return the resolved merge map, or null
         */
        @Nullable
        public MapNode getResolvedMergeMap() {
            return resolvedMergeMap;
        }

        /**
         * Sets the resolved merge map for a {@code <<} alias entry.
         *
         * @param resolvedMergeMap the resolved map node
         */
        public void setResolvedMergeMap(@Nullable MapNode resolvedMergeMap) {
            this.resolvedMergeMap = resolvedMergeMap;
        }

        /**
         * Formats the key according to the specified key style and content.
         *
         * @return the formatted key string
         */
        @NotNull
        public String formatKey() {
            return switch (keyStyle) {
                case SINGLE_QUOTED -> "'" + key.replace("'", "''") + "'";
                case DOUBLE_QUOTED -> "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                default -> {
                    if (needsQuoting(key)) {
                        yield "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                    }
                    yield key;
                }
            };
        }

        private boolean needsQuoting(@NotNull String str) {
            if (str.isEmpty()) return true;
            if (str.contains(": ") || str.contains(" #") || str.contains("\n")) return true;
            if (str.startsWith("&") || str.startsWith("*") || str.startsWith("!")) return true;
            return str.startsWith("-") || str.startsWith("[") || str.startsWith("{");
        }

        /**
         * Creates a deep copy of this MapEntry, including the value node and all metadata.
         *
         * @return a copy of this entry
         */
        @NotNull
        public MapEntry copy() {
            MapEntry copy = new MapEntry(key, value.copy(), keyStyle);
            copy.commentsBefore = new ArrayList<>(this.commentsBefore);
            copy.inlineComment = this.inlineComment;
            copy.emptyLinesBefore = this.emptyLinesBefore;
            copy.resolvedMergeMap = this.resolvedMergeMap;
            copy.complexKey = this.complexKey != null ? this.complexKey.copy() : null;
            return copy;
        }
    }
}
