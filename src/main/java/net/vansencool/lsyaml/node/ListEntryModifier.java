package net.vansencool.lsyaml.node;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent API for modifying a list entry with comments and formatting.
 * Obtain via {@link ListNode#modify(int)}.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ListEntryModifier {

    private final @NotNull ListNode list;
    private final int index;

    ListEntryModifier(@NotNull ListNode list, int index) {
        this.list = list;
        this.index = index;
    }

    /**
     * Sets the value for this entry.
     *
     * @param value the value node
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier value(@NotNull YamlNode value) {
        getEntry().setValue(value);
        return this;
    }

    /**
     * Sets a string value for this entry.
     *
     * @param value the string value
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier value(@NotNull String value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets an integer value for this entry.
     *
     * @param value the integer value
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier value(int value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets a long value for this entry.
     *
     * @param value the long value
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier value(long value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets a double value for this entry.
     *
     * @param value the double value
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier value(double value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets a boolean value for this entry.
     *
     * @param value the boolean value
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier value(boolean value) {
        return value(new ScalarNode(value));
    }

    /**
     * Sets an empty map as value for this entry.
     *
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier valueMap() {
        return value(new MapNode());
    }

    /**
     * Sets an empty list as value for this entry.
     *
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier valueList() {
        return value(new ListNode());
    }

    /**
     * Adds a comment before this entry.
     *
     * @param comment the comment text (without #)
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier commentBefore(@NotNull String comment) {
        getEntry().addCommentBefore(comment);
        return this;
    }

    /**
     * Adds multiple comments before this entry.
     *
     * @param comments the comment texts (without #)
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier commentsBefore(@NotNull String... comments) {
        for (String comment : comments) {
            getEntry().addCommentBefore(comment);
        }
        return this;
    }

    /**
     * Sets all comments before this entry, replacing existing ones.
     *
     * @param comments the comment texts (without #)
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier setCommentsBefore(@NotNull String... comments) {
        getEntry().setCommentsBefore(Arrays.asList(comments));
        return this;
    }

    /**
     * Clears all comments before this entry.
     *
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier clearCommentsBefore() {
        getEntry().setCommentsBefore(new ArrayList<>());
        return this;
    }

    /**
     * Sets an inline comment for this entry (appears after the value on the same line).
     *
     * @param comment the comment text (without #), or null to remove
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier inlineComment(@Nullable String comment) {
        getEntry().setInlineComment(comment);
        return this;
    }

    /**
     * Removes the inline comment from this entry.
     *
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier clearInlineComment() {
        return inlineComment(null);
    }

    /**
     * Sets the number of empty lines before this entry.
     *
     * @param count the number of empty lines
     * @return this modifier for chaining
     */
    @NotNull
    public ListEntryModifier emptyLinesBefore(int count) {
        getEntry().setEmptyLinesBefore(count);
        return this;
    }

    /**
     * Removes this entry from the list.
     */
    public void remove() {
        list.remove(index);
    }

    /**
     * Gets the current value of this entry.
     *
     * @return the value
     */
    @NotNull
    public YamlNode getValue() {
        return getEntry().getValue();
    }

    /**
     * Gets the current comments before this entry.
     *
     * @return list of comments
     */
    @NotNull
    public List<String> getCommentsBefore() {
        return getEntry().getCommentsBefore();
    }

    /**
     * Gets the current inline comment for this entry.
     *
     * @return the inline comment, or null
     */
    @Nullable
    public String getInlineComment() {
        return getEntry().getInlineComment();
    }

    /**
     * Gets the index of this entry.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the underlying entry.
     *
     * @return the entry
     */
    @NotNull
    public ListNode.ListEntry getEntry() {
        return list.getEntry(index);
    }

    /**
     * Returns to the parent list for further operations.
     *
     * @return the parent list
     */
    @NotNull
    public ListNode done() {
        return list;
    }
}
