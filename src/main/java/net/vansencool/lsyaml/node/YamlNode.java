package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.LSYAML;
import net.vansencool.lsyaml.metadata.NodeMetadata;
import net.vansencool.lsyaml.node.type.NodeType;
import net.vansencool.lsyaml.writer.YamlWriter;
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
     * Converts this node to a YAML string using the node's own built-in serialization.
     * <p>
     * This is a convenience method intended for quick serialization and debugging. It always
     * uses a fixed 2-space indent and always preserves comments, empty lines, and quote styles.
     * None of these behaviours are configurable here.
     * </p>
     * <p>
     * If you need to control indentation size or toggle comment/empty-line/quote-style
     * preservation, use {@link LSYAML#write(YamlNode)} or obtain a
     * configurable {@link YamlWriter} via
     * {@link LSYAML#writer()} instead.
     * </p>
     *
     * @return YAML string with 2-space indentation and all preservation options enabled
     */
    @NotNull
    String toYaml();

    /**
     * Converts this node to a YAML string with the specified indentation level.
     * <p>
     * This is the low-level method underlying {@link #toYaml()}. It is called recursively
     * as the node tree is traversed; {@code currentLevel} tracks nesting depth so that
     * each level can be indented correctly.
     * </p>
     *
     * @param indent       the number of spaces per indentation level
     * @param currentLevel the current nesting depth (0 for the root node)
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

}
