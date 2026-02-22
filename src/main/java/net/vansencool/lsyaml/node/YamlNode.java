package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.metadata.NodeMetadata;
import net.vansencool.lsyaml.node.type.NodeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base interface for all YAML nodes.
 * Every element in a YAML document implements this interface.
 */
@SuppressWarnings("unused")
public interface YamlNode {

    /**
     * Returns the type of this node.
     *
     * @return the node type
     */
    @NotNull
    NodeType getType();

    /**
     * Returns the metadata associated with this node.
     * Metadata includes position, formatting, and comment information.
     *
     * @return the node metadata
     */
    @NotNull
    NodeMetadata getMetadata();

    /**
     * Sets the metadata for this node.
     *
     * @param metadata the metadata to set
     */
    void setMetadata(@NotNull NodeMetadata metadata);

    /**
     * Returns comments that appear before this node.
     *
     * @return list of comment strings (without # prefix)
     */
    @NotNull
    List<String> getCommentsBefore();

    /**
     * Sets comments that appear before this node.
     *
     * @param comments the comments to set
     */
    void setCommentsBefore(@NotNull List<String> comments);

    /**
     * Returns the inline comment for this node (comment on the same line).
     *
     * @return the inline comment, or null if none
     */
    @Nullable
    String getInlineComment();

    /**
     * Sets the inline comment for this node.
     *
     * @param comment the inline comment, or null to remove
     */
    void setInlineComment(@Nullable String comment);

    /**
     * Adds a comment before this node.
     *
     * @param comment the comment to add (without # prefix)
     */
    void addCommentBefore(@NotNull String comment);

    /**
     * Returns the number of empty lines before this node.
     *
     * @return number of empty lines
     */
    int getEmptyLinesBefore();

    /**
     * Sets the number of empty lines before this node.
     *
     * @param count number of empty lines
     */
    void setEmptyLinesBefore(int count);

    /**
     * Returns the number of empty lines that trail after this node's last child.
     * Used internally during parsing to propagate spacing between sibling entries.
     *
     * @return number of trailing empty lines
     */
    int getTrailingEmptyLines();

    /**
     * Sets the number of trailing empty lines after this node's last child.
     *
     * @param count number of trailing empty lines
     */
    void setTrailingEmptyLines(int count);

    /**
     * Creates a deep copy of this node.
     *
     * @return a deep copy
     */
    @NotNull
    YamlNode copy();

    /**
     * Converts this node to a YAML string representation.
     *
     * @return YAML string
     */
    @NotNull
    String toYaml();

    /**
     * Converts this node to a YAML string with the specified indentation level.
     *
     * @param indent       the indentation level (number of spaces per level)
     * @param currentLevel the current nesting level
     * @return YAML string
     */
    @NotNull
    String toYaml(int indent, int currentLevel);

    /**
     * Returns true if this node is a map.
     *
     * @return true if this is a MapNode
     */
    default boolean isMap() {
        return getType() == NodeType.MAP;
    }

    /**
     * Returns true if this node is a list.
     *
     * @return true if this is a ListNode
     */
    default boolean isList() {
        return getType() == NodeType.LIST;
    }

    /**
     * Returns true if this node is a scalar value.
     *
     * @return true if this is a ScalarNode
     */
    default boolean isScalar() {
        return getType() == NodeType.SCALAR;
    }

    /**
     * Casts this node to a MapNode.
     *
     * @return this node as a MapNode
     * @throws IllegalStateException if this is not a MapNode
     */
    @NotNull
    default MapNode asMap() {
        if (this instanceof MapNode map) {
            return map;
        }
        throw new IllegalStateException("Node is not a map, it is: " + getType());
    }

    /**
     * Casts this node to a ListNode.
     *
     * @return this node as a ListNode
     * @throws IllegalStateException if this is not a ListNode
     */
    @NotNull
    default ListNode asList() {
        if (this instanceof ListNode list) {
            return list;
        }
        throw new IllegalStateException("Node is not a list, it is: " + getType());
    }

    /**
     * Casts this node to a ScalarNode.
     *
     * @return this node as a ScalarNode
     * @throws IllegalStateException if this is not a ScalarNode
     */
    @NotNull
    default ScalarNode asScalar() {
        if (this instanceof ScalarNode scalar) {
            return scalar;
        }
        throw new IllegalStateException("Node is not a scalar, it is: " + getType());
    }

    /**
     * Gets a child node by key. Only works for map nodes.
     *
     * @param key the key to look up
     * @return the child node, or throws IllegalStateException if not a map, or null if key not found
     */
    @Nullable
    default YamlNode get(@NotNull String key) {
        if (this instanceof MapNode map) {
            return map.get(key);
        }
        throw new IllegalStateException("Node is not a map, it is: " + getType());
    }

    /**
     * Gets a child node by index. Only works for list nodes.
     *
     * @param index the index to look up
     * @return the child node, throws IllegalStateException if not a list, or null if index out of bounds
     */
    @Nullable
    default YamlNode get(int index) {
        if (this instanceof ListNode list) {
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
        }
        throw new IllegalStateException("Node is not a list, it is: " + getType());
    }

    /**
     * Gets the string value of this node or a nested path.
     * For scalars, returns the value. For maps/lists, returns null.
     *
     * @return the string value, or null
     */
    @Nullable
    default String getString() {
        if (this instanceof ScalarNode scalar) {
            Object val = scalar.getValue();
            return val != null ? val.toString() : null;
        }
        return null;
    }

    /**
     * Gets a string value at the given path (dot-separated keys or array indices).
     * Example: "database.credentials.username" or "servers.0.host"
     *
     * @param path the dot-separated path
     * @return the string value, or null if not found
     */
    @Nullable
    default String getString(@NotNull String path) {
        YamlNode node = getPath(path);
        return node != null ? node.getString() : null;
    }

    /**
     * Gets a string value at the given path, returning a default if missing.
     *
     * @param path         the dot-separated path
     * @param defaultValue the value to return if missing
     * @return the string value
     */
    @NotNull
    default String getString(@NotNull String path, @NotNull String defaultValue) {
        String val = getString(path);
        return val != null ? val : defaultValue;
    }

    /**
     * Gets a string value for a literal key (not path-based).
     * Use this when your key contains dots that should not be treated as path separators.
     *
     * @param key the literal key
     * @return the string value, or null if not found
     */
    @Nullable
    default String getStringLiteral(@NotNull String key) {
        YamlNode child = get(key);
        return child != null ? child.getString() : null;
    }

    /**
     * Gets a string value for a literal key, returning a default if missing.
     *
     * @param key          the literal key
     * @param defaultValue the value to return if missing
     * @return the string value
     */
    @NotNull
    default String getStringLiteral(@NotNull String key, @NotNull String defaultValue) {
        String val = getStringLiteral(key);
        return val != null ? val : defaultValue;
    }

    /**
     * Gets an integer value of this scalar node.
     *
     * @return the integer value, or null if not a scalar or not parseable
     */
    @Nullable
    default Integer getInt() {
        String val = getString();
        if (val == null) return null;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets an integer value at the given path.
     *
     * @param path the dot-separated path
     * @return the integer value, or null if not found or not parseable
     */
    @Nullable
    default Integer getInt(@NotNull String path) {
        YamlNode node = getPath(path);
        return node != null ? node.getInt() : null;
    }

    /**
     * Gets an integer value at the given path, returning a default if missing.
     *
     * @param path         the dot-separated path
     * @param defaultValue the value to return if missing or not parseable
     * @return the integer value
     */
    default int getInt(@NotNull String path, int defaultValue) {
        Integer val = getInt(path);
        return val != null ? val : defaultValue;
    }

    /**
     * Gets an integer value for a literal key (not path-based).
     *
     * @param key the literal key
     * @return the integer value, or null if not found or not parseable
     */
    @Nullable
    default Integer getIntLiteral(@NotNull String key) {
        YamlNode child = get(key);
        return child != null ? child.getInt() : null;
    }

    /**
     * Gets an integer value for a literal key, returning a default if missing.
     *
     * @param key          the literal key
     * @param defaultValue the value to return if missing or not parseable
     * @return the integer value
     */
    default int getIntLiteral(@NotNull String key, int defaultValue) {
        Integer val = getIntLiteral(key);
        return val != null ? val : defaultValue;
    }

    /**
     * Gets a long value of this scalar node.
     *
     * @return the long value, or null if not a scalar or not parseable
     */
    @Nullable
    default Long getLong() {
        String val = getString();
        if (val == null) return null;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets a long value at the given path.
     *
     * @param path the dot-separated path
     * @return the long value, or null if not found or not parseable
     */
    @Nullable
    default Long getLong(@NotNull String path) {
        YamlNode node = getPath(path);
        return node != null ? node.getLong() : null;
    }

    /**
     * Gets a long value at the given path, returning a default if missing.
     *
     * @param path         the dot-separated path
     * @param defaultValue the value to return if missing or not parseable
     * @return the long value
     */
    default long getLong(@NotNull String path, long defaultValue) {
        Long val = getLong(path);
        return val != null ? val : defaultValue;
    }

    /**
     * Gets a long value for a literal key (not path-based).
     *
     * @param key the literal key
     * @return the long value, or null if not found or not parseable
     */
    @Nullable
    default Long getLongLiteral(@NotNull String key) {
        YamlNode child = get(key);
        return child != null ? child.getLong() : null;
    }

    /**
     * Gets a long value for a literal key, returning a default if missing.
     *
     * @param key          the literal key
     * @param defaultValue the value to return if missing or not parseable
     * @return the long value
     */
    default long getLongLiteral(@NotNull String key, long defaultValue) {
        Long val = getLongLiteral(key);
        return val != null ? val : defaultValue;
    }

    /**
     * Gets a double value of this scalar node.
     *
     * @return the double value, or null if not a scalar or not parseable
     */
    @Nullable
    default Double getDouble() {
        String val = getString();
        if (val == null) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets a double value at the given path.
     *
     * @param path the dot-separated path
     * @return the double value, or null if not found or not parseable
     */
    @Nullable
    default Double getDouble(@NotNull String path) {
        YamlNode node = getPath(path);
        return node != null ? node.getDouble() : null;
    }

    /**
     * Gets a double value at the given path, returning a default if missing.
     *
     * @param path         the dot-separated path
     * @param defaultValue the value to return if missing or not parseable
     * @return the double value
     */
    default double getDouble(@NotNull String path, double defaultValue) {
        Double val = getDouble(path);
        return val != null ? val : defaultValue;
    }

    /**
     * Gets a double value for a literal key (not path-based).
     *
     * @param key the literal key
     * @return the double value, or null if not found or not parseable
     */
    @Nullable
    default Double getDoubleLiteral(@NotNull String key) {
        YamlNode child = get(key);
        return child != null ? child.getDouble() : null;
    }

    /**
     * Gets a double value for a literal key, returning a default if missing.
     *
     * @param key          the literal key
     * @param defaultValue the value to return if missing or not parseable
     * @return the double value
     */
    default double getDoubleLiteral(@NotNull String key, double defaultValue) {
        Double val = getDoubleLiteral(key);
        return val != null ? val : defaultValue;
    }

    /**
     * Gets a boolean value of this scalar node.
     *
     * @return the boolean value, or null if not a scalar or not a boolean
     */
    @Nullable
    default Boolean getBoolean() {
        String val = getString();
        if (val == null) return null;
        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("on")) {
            return true;
        }
        if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("no") || val.equalsIgnoreCase("off")) {
            return false;
        }
        return null;
    }

    /**
     * Gets a boolean value at the given path.
     *
     * @param path the dot-separated path
     * @return the boolean value, or null if not found or not a boolean
     */
    @Nullable
    default Boolean getBoolean(@NotNull String path) {
        YamlNode node = getPath(path);
        return node != null ? node.getBoolean() : null;
    }

    /**
     * Gets a boolean value at the given path, returning a default if missing.
     *
     * @param path         the dot-separated path
     * @param defaultValue the value to return if missing or not a boolean
     * @return the boolean value
     */
    default boolean getBoolean(@NotNull String path, boolean defaultValue) {
        Boolean val = getBoolean(path);
        return val != null ? val : defaultValue;
    }

    /**
     * Gets a boolean value for a literal key (not path-based).
     *
     * @param key the literal key
     * @return the boolean value, or null if not found or not a boolean
     */
    @Nullable
    default Boolean getBooleanLiteral(@NotNull String key) {
        YamlNode child = get(key);
        return child != null ? child.getBoolean() : null;
    }

    /**
     * Gets a boolean value for a literal key, returning a default if missing.
     *
     * @param key          the literal key
     * @param defaultValue the value to return if missing or not a boolean
     * @return the boolean value
     */
    default boolean getBooleanLiteral(@NotNull String key, boolean defaultValue) {
        Boolean val = getBooleanLiteral(key);
        return val != null ? val : defaultValue;
    }

    /**
     * Navigates to a node at the given path (dot-separated).
     * Supports both map keys and list indices.
     * Example: "database.pool.max" or "servers.0.host"
     *
     * @param path the dot-separated path
     * @return the node at the path, or null if not found
     */
    @Nullable
    default YamlNode getPath(@NotNull String path) {
        if (path.isEmpty()) return this;

        String[] parts = path.split("\\.");
        YamlNode current = this;

        for (String part : parts) {
            if (current == null) return null;

            if (current.isMap()) {
                current = current.get(part);
            } else if (current.isList()) {
                try {
                    int idx = Integer.parseInt(part);
                    current = current.get(idx);
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Returns the number of children (entries for maps, items for lists).
     * Returns 0 for scalars.
     *
     * @return the size
     */
    default int size() {
        if (this instanceof MapNode map) {
            return map.size();
        }
        if (this instanceof ListNode list) {
            return list.size();
        }
        return 0;
    }

    /**
     * Checks if this node contains the given key (for maps) or has items (for lists).
     *
     * @param key the key to check
     * @return true if the key exists in the map
     */
    default boolean containsKey(@NotNull String key) {
        if (this instanceof MapNode map) {
            return map.containsKey(key);
        }
        return false;
    }

    /**
     * Checks if a value exists at the given dot-separated path.
     *
     * @param path the dot-separated path
     * @return true if a value exists at the path
     */
    default boolean containsPath(@NotNull String path) {
        return getPath(path) != null;
    }

    /**
     * Gets a nested map at the given path.
     *
     * @param path the dot-separated path
     * @return the map node, or null if not found or not a map
     */
    @Nullable
    default MapNode getMap(@NotNull String path) {
        YamlNode node = getPath(path);
        return node instanceof MapNode map ? map : null;
    }

    /**
     * Gets a nested map at the given path, returning a default if missing.
     *
     * @param path         the dot-separated path
     * @param defaultValue the value to return if missing or not a map
     * @return the map node
     */
    @NotNull
    default MapNode getMap(@NotNull String path, @NotNull MapNode defaultValue) {
        MapNode result = getMap(path);
        return result != null ? result : defaultValue;
    }

    /**
     * Gets a nested map for a literal key (not path-based).
     *
     * @param key the literal key
     * @return the map node, or null if not found or not a map
     */
    @Nullable
    default MapNode getMapLiteral(@NotNull String key) {
        YamlNode child = get(key);
        return child instanceof MapNode map ? map : null;
    }

    /**
     * Gets a nested map for a literal key, returning a default if missing.
     *
     * @param key          the literal key
     * @param defaultValue the value to return if missing or not a map
     * @return the map node
     */
    @NotNull
    default MapNode getMapLiteral(@NotNull String key, @NotNull MapNode defaultValue) {
        MapNode result = getMapLiteral(key);
        return result != null ? result : defaultValue;
    }

    /**
     * Gets a nested list at the given path.
     *
     * @param path the dot-separated path
     * @return the list node, or null if not found or not a list
     */
    @Nullable
    default ListNode getList(@NotNull String path) {
        YamlNode node = getPath(path);
        return node instanceof ListNode list ? list : null;
    }

    /**
     * Gets a nested list at the given path, returning a default if missing.
     *
     * @param path         the dot-separated path
     * @param defaultValue the value to return if missing or not a list
     * @return the list node
     */
    @NotNull
    default ListNode getList(@NotNull String path, @NotNull ListNode defaultValue) {
        ListNode result = getList(path);
        return result != null ? result : defaultValue;
    }

    /**
     * Gets a nested list for a literal key (not path-based).
     *
     * @param key the literal key
     * @return the list node, or null if not found or not a list
     */
    @Nullable
    default ListNode getListLiteral(@NotNull String key) {
        YamlNode child = get(key);
        return child instanceof ListNode list ? list : null;
    }

    /**
     * Gets a nested list for a literal key, returning a default if missing.
     *
     * @param key          the literal key
     * @param defaultValue the value to return if missing or not a list
     * @return the list node
     */
    @NotNull
    default ListNode getListLiteral(@NotNull String key, @NotNull ListNode defaultValue) {
        ListNode result = getListLiteral(key);
        return result != null ? result : defaultValue;
    }

    /**
     * Sets a string value at the given path, creating intermediate maps as needed.
     * Only works if this node is a MapNode.
     *
     * @param path  the dot-separated path
     * @param value the value to set
     */
    default void setString(@NotNull String path, @NotNull String value) {
        setAtPath(path, new ScalarNode(value));
    }

    /**
     * Sets an integer value at the given path, creating intermediate maps as needed.
     *
     * @param path  the dot-separated path
     * @param value the value to set
     */
    default void setInt(@NotNull String path, int value) {
        setAtPath(path, new ScalarNode(value));
    }

    /**
     * Sets a long value at the given path, creating intermediate maps as needed.
     *
     * @param path  the dot-separated path
     * @param value the value to set
     */
    default void setLong(@NotNull String path, long value) {
        setAtPath(path, new ScalarNode(value));
    }

    /**
     * Sets a double value at the given path, creating intermediate maps as needed.
     *
     * @param path  the dot-separated path
     * @param value the value to set
     */
    default void setDouble(@NotNull String path, double value) {
        setAtPath(path, new ScalarNode(value));
    }

    /**
     * Sets a boolean value at the given path, creating intermediate maps as needed.
     *
     * @param path  the dot-separated path
     * @param value the value to set
     */
    default void setBoolean(@NotNull String path, boolean value) {
        setAtPath(path, new ScalarNode(value));
    }

    /**
     * Sets a string value for a literal key (not path-based).
     *
     * @param key   the literal key
     * @param value the value to set
     */
    default void setStringLiteral(@NotNull String key, @NotNull String value) {
        if (this instanceof MapNode map) {
            map.put(key, value);
        }
    }

    /**
     * Sets an integer value for a literal key (not path-based).
     *
     * @param key   the literal key
     * @param value the value to set
     */
    default void setIntLiteral(@NotNull String key, int value) {
        if (this instanceof MapNode map) {
            map.put(key, value);
        }
    }

    /**
     * Sets a long value for a literal key (not path-based).
     *
     * @param key   the literal key
     * @param value the value to set
     */
    default void setLongLiteral(@NotNull String key, long value) {
        if (this instanceof MapNode map) {
            map.put(key, value);
        }
    }

    /**
     * Sets a double value for a literal key (not path-based).
     *
     * @param key   the literal key
     * @param value the value to set
     */
    default void setDoubleLiteral(@NotNull String key, double value) {
        if (this instanceof MapNode map) {
            map.put(key, value);
        }
    }

    /**
     * Sets a boolean value for a literal key (not path-based).
     *
     * @param key   the literal key
     * @param value the value to set
     */
    default void setBooleanLiteral(@NotNull String key, boolean value) {
        if (this instanceof MapNode map) {
            map.put(key, value);
        }
    }

    /**
     * Sets a node value at the given path, creating intermediate maps as needed.
     * Only works if this node is a MapNode.
     *
     * @param path  the dot-separated path
     * @param value the value to set
     */
    default void setAtPath(@NotNull String path, @NotNull YamlNode value) {
        if (!(this instanceof MapNode root)) return;

        String[] parts = path.split("\\.", -1);
        MapNode current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            YamlNode next = current.get(parts[i]);
            if (next instanceof MapNode nextMap) {
                current = nextMap;
            } else {
                MapNode child = new MapNode();
                current.put(parts[i], child);
                current = child;
            }
        }

        current.put(parts[parts.length - 1], value);
    }

    /**
     * Removes the value at the given path.
     * Only works if this node is a MapNode.
     *
     * @param path the dot-separated path
     * @return the removed value, or null if not found
     */
    @Nullable
    default YamlNode removeAtPath(@NotNull String path) {
        String[] parts = path.split("\\.", -1);
        if (parts.length == 1) {
            if (this instanceof MapNode map) {
                return map.remove(path);
            }
            return null;
        }

        String parentPath = path.substring(0, path.lastIndexOf('.'));
        YamlNode parent = getPath(parentPath);
        if (parent instanceof MapNode parentMap) {
            return parentMap.remove(parts[parts.length - 1]);
        }
        return null;
    }
}
