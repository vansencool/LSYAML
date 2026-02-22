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
     * @return the child node, or null if not found or not a map
     */
    @Nullable
    default YamlNode get(@NotNull String key) {
        if (this instanceof MapNode map) {
            return map.get(key);
        }
        return null;
    }

    /**
     * Gets a child node by index. Only works for list nodes.
     *
     * @param index the index to look up
     * @return the child node, or null if out of bounds or not a list
     */
    @Nullable
    default YamlNode get(int index) {
        if (this instanceof ListNode list) {
            if (index >= 0 && index < list.size()) {
                return list.get(index);
            }
        }
        return null;
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
}
