package net.vansencool.lsyaml.node.modifier;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent API for modifying a map entry with comments and formatting.
 * Obtain via {@link MapNode#modify(String)}.
 */
@SuppressWarnings({"unused", "DataFlowIssue", "UnusedReturnValue"})
public class EntryModifier {

    private final @NotNull MapNode map;
    private final @NotNull String key;
    private @Nullable MapNode.MapEntry entry;

    public EntryModifier(@NotNull MapNode map, @NotNull String key) {
        this.map = map;
        this.key = key;
        this.entry = map.getEntry(key);
    }

    /**
     * Sets the value for this entry.
     *
     * @param value the value node
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier value(@NotNull YamlNode value) {
        ensureEntry();
        entry.setValue(value);
        return this;
    }

    /**
     * Sets a string value for this entry.
     *
     * @param value the string value
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier value(@NotNull String value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets an integer value for this entry.
     *
     * @param value the integer value
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier value(int value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets a long value for this entry.
     *
     * @param value the long value
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier value(long value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets a double value for this entry.
     *
     * @param value the double value
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier value(double value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets a boolean value for this entry.
     *
     * @param value the boolean value
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier value(boolean value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets an empty map as value for this entry.
     *
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier valueMap() {
        return value(new MapNode());
    }

    /**
     * Sets an empty list as value for this entry.
     *
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier valueList() {
        return value(new ListNode());
    }

    /**
     * Adds a comment before this entry.
     *
     * @param comment the comment text (without #)
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier commentBefore(@NotNull String comment) {
        ensureEntry();
        entry.addCommentBefore(comment);
        return this;
    }

    /**
     * Adds multiple comments before this entry.
     *
     * @param comments the comment texts (without #)
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier commentsBefore(@NotNull String... comments) {
        ensureEntry();
        for (String comment : comments) {
            entry.addCommentBefore(comment);
        }
        return this;
    }

    /**
     * Clears all comments before this entry.
     *
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier clearCommentsBefore() {
        ensureEntry();
        entry.setCommentsBefore(new ArrayList<>());
        return this;
    }

    /**
     * Sets an inline comment for this entry (appears after the value on the same line).
     *
     * @param comment the comment text (without #), or null to remove
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier inlineComment(@Nullable String comment) {
        ensureEntry();
        entry.setInlineComment(comment);
        return this;
    }

    /**
     * Removes the inline comment from this entry.
     *
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier clearInlineComment() {
        return inlineComment(null);
    }

    /**
     * Sets the number of empty lines before this entry.
     *
     * @param count the number of empty lines
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier emptyLinesBefore(int count) {
        ensureEntry();
        entry.setEmptyLinesBefore(count);
        return this;
    }

    /**
     * Sets the key style for this entry.
     *
     * @param style the scalar style for the key
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier keyStyle(@NotNull ScalarStyle style) {
        ensureEntry();
        entry.setKeyStyle(style);
        return this;
    }

    /**
     * Renames this entry's key.
     *
     * @param newKey the new key name
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier rename(@NotNull String newKey) {
        if (entry != null && !key.equals(newKey)) {
            map.renameKey(key, newKey);
            entry = map.getEntry(newKey);
        }
        return this;
    }

    /**
     * Removes this entry from the map.
     */
    public void remove() {
        map.remove(key);
        entry = null;
    }

    /**
     * Gets the current value of this entry.
     *
     * @return the value, or null if entry doesn't exist
     */
    @Nullable
    public YamlNode getValue() {
        return entry != null ? entry.getValue() : null;
    }

    /**
     * Gets the current comments before this entry.
     *
     * @return list of comments, or empty list if entry doesn't exist
     */
    @NotNull
    public List<String> getCommentsBefore() {
        return entry != null ? entry.getCommentsBefore() : new ArrayList<>();
    }

    /**
     * Sets all comments before this entry, replacing existing ones.
     *
     * @param comments the comment texts (without #)
     * @return this modifier for chaining
     */
    @NotNull
    public EntryModifier setCommentsBefore(@NotNull String... comments) {
        ensureEntry();
        entry.setCommentsBefore(Arrays.asList(comments));
        return this;
    }

    /**
     * Gets the current inline comment for this entry.
     *
     * @return the inline comment, or null
     */
    @Nullable
    public String getInlineComment() {
        return entry != null ? entry.getInlineComment() : null;
    }

    /**
     * Checks if this entry exists in the map.
     *
     * @return true if the entry exists
     */
    public boolean exists() {
        return entry != null;
    }

    /**
     * Gets the underlying entry.
     *
     * @return the entry, or null if it doesn't exist
     */
    @Nullable
    public MapNode.MapEntry getEntry() {
        return entry;
    }

    /**
     * Returns to the parent map for further operations.
     *
     * @return the parent map
     */
    @NotNull
    public MapNode done() {
        return map;
    }

    private void ensureEntry() {
        if (entry == null) {
            entry = new MapNode.MapEntry(key, new ScalarNode(null));
            map.putEntry(entry);
        }
    }
}
