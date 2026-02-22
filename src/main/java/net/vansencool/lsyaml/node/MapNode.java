package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.NodeMetadata;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a YAML mapping (key-value pairs).
 * Preserves insertion order and all formatting metadata.
 */
public class MapNode extends AbstractYamlNode {

    private final @NotNull LinkedHashMap<String, MapEntry> entries;
    private @NotNull CollectionStyle style;

    public MapNode() {
        super();
        this.entries = new LinkedHashMap<>();
        this.style = CollectionStyle.BLOCK;
    }

    public MapNode(@NotNull CollectionStyle style) {
        super();
        this.entries = new LinkedHashMap<>();
        this.style = style;
    }

    public MapNode(@NotNull NodeMetadata metadata) {
        super(metadata);
        this.entries = new LinkedHashMap<>();
        this.style = CollectionStyle.BLOCK;
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
        return entry != null ? entry.getValue() : null;
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
     * Gets a string value for a key.
     *
     * @param key the key
     * @return the string value, or null
     */
    @Nullable
    public String getString(@NotNull String key) {
        YamlNode node = get(key);
        if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getStringValue();
        }
        return null;
    }

    /**
     * Gets an integer value for a key.
     *
     * @param key the key
     * @param defaultValue default if not found
     * @return the integer value
     */
    public int getInt(@NotNull String key, int defaultValue) {
        YamlNode node = get(key);
        if (node instanceof ScalarNode) {
            try {
                return ((ScalarNode) node).getIntValue();
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets a boolean value for a key.
     *
     * @param key the key
     * @param defaultValue default if not found
     * @return the boolean value
     */
    public boolean getBoolean(@NotNull String key, boolean defaultValue) {
        YamlNode node = get(key);
        if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getBooleanValue();
        }
        return defaultValue;
    }

    /**
     * Gets a nested map for a key.
     *
     * @param key the key
     * @return the map node, or null
     */
    @Nullable
    public MapNode getMap(@NotNull String key) {
        YamlNode node = get(key);
        if (node instanceof MapNode) {
            return (MapNode) node;
        }
        return null;
    }

    /**
     * Gets a nested list for a key.
     *
     * @param key the key
     * @return the list node, or null
     */
    @Nullable
    public ListNode getList(@NotNull String key) {
        YamlNode node = get(key);
        if (node instanceof ListNode) {
            return (ListNode) node;
        }
        return null;
    }

    /**
     * Puts a value with the given key.
     * If the key already exists, the entry's comments and metadata are preserved.
     *
     * @param key the key
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
     * @param key the key
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
     * @param key the key
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
     * @param key the key
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
     * @param key the key
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
     * @param key the key
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
            sb.append(toFlowYaml());
            sb.append(buildInlineComment());
        } else {
            sb.append(toBlockYaml(indent, currentLevel));
        }

        return sb.toString();
    }

    private @NotNull String toFlowYaml() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (MapEntry entry : entries.values()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.formatKey()).append(": ");
            if (entry.getValue() instanceof ScalarNode) {
                sb.append(entry.getValue().toYaml(2, 0));
            } else {
                sb.append(entry.getValue().toYaml(2, 0));
            }
        }
        sb.append("}");
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

            for (int i = 0; i < entry.getEmptyLinesBefore(); i++) {
                sb.append("\n");
            }

            for (String comment : entry.getCommentsBefore()) {
                sb.append(indentStr).append("#").append(comment).append("\n");
            }

            sb.append(indentStr).append(entry.formatKey()).append(":");

            YamlNode value = entry.getValue();
            if (value instanceof MapNode || value instanceof ListNode) {
                if (entry.getInlineComment() != null) {
                    sb.append(" #").append(entry.getInlineComment());
                }
                sb.append(value.toYaml(indent, currentLevel + 1));
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
        return "MapNode{entries=" + entries.size() + ", style=" + style + "}";
    }

    /**
     * Represents a single entry in a map with full metadata.
     */
    public static class MapEntry {

        private @NotNull String key;
        private @NotNull YamlNode value;
        private @NotNull ScalarStyle keyStyle;
        private @NotNull List<String> commentsBefore;
        private @Nullable String inlineComment;
        private int emptyLinesBefore;

        public MapEntry(@NotNull String key, @NotNull YamlNode value) {
            this.key = key;
            this.value = value;
            this.keyStyle = ScalarStyle.PLAIN;
            this.commentsBefore = new ArrayList<>();
            this.inlineComment = null;
            this.emptyLinesBefore = 0;
        }

        public MapEntry(@NotNull String key, @NotNull YamlNode value, @NotNull ScalarStyle keyStyle) {
            this.key = key;
            this.value = value;
            this.keyStyle = keyStyle;
            this.commentsBefore = new ArrayList<>();
            this.inlineComment = null;
            this.emptyLinesBefore = 0;
        }

        @NotNull
        public String getKey() {
            return key;
        }

        public void setKey(@NotNull String key) {
            this.key = key;
        }

        @NotNull
        public YamlNode getValue() {
            return value;
        }

        public void setValue(@NotNull YamlNode value) {
            this.value = value;
        }

        @NotNull
        public ScalarStyle getKeyStyle() {
            return keyStyle;
        }

        public void setKeyStyle(@NotNull ScalarStyle keyStyle) {
            this.keyStyle = keyStyle;
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
        public String formatKey() {
            switch (keyStyle) {
                case SINGLE_QUOTED:
                    return "'" + key.replace("'", "''") + "'";
                case DOUBLE_QUOTED:
                    return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                default:
                    if (needsQuoting(key)) {
                        return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
                    }
                    return key;
            }
        }

        private boolean needsQuoting(@NotNull String str) {
            if (str.isEmpty()) return true;
            if (str.contains(": ") || str.contains(" #") || str.contains("\n")) return true;
            if (str.startsWith("&") || str.startsWith("*") || str.startsWith("!")) return true;
            if (str.startsWith("-") || str.startsWith("[") || str.startsWith("{")) return true;
            return false;
        }

        @NotNull
        public MapEntry copy() {
            MapEntry copy = new MapEntry(key, value.copy(), keyStyle);
            copy.commentsBefore = new ArrayList<>(this.commentsBefore);
            copy.inlineComment = this.inlineComment;
            copy.emptyLinesBefore = this.emptyLinesBefore;
            return copy;
        }
    }
}
