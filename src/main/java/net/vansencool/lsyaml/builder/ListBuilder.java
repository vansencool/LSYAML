package net.vansencool.lsyaml.builder;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating ListNode instances with fluent API.
 */
@SuppressWarnings("unused")
public class ListBuilder {

    private final @NotNull List<ItemBuilder> items;
    private @NotNull CollectionStyle style;
    private final @NotNull List<String> commentsBefore;
    private @Nullable String inlineComment;
    private int emptyLinesBefore;
    private @Nullable String anchor;
    private @Nullable ListNode baseNode;

    public ListBuilder() {
        this.items = new ArrayList<>();
        this.style = CollectionStyle.BLOCK;
        this.commentsBefore = new ArrayList<>();
        this.inlineComment = null;
        this.emptyLinesBefore = 0;
        this.anchor = null;
        this.baseNode = null;
    }

    /**
     * Creates a new ListBuilder instance.
     *
     * @return a new builder
     */
    @NotNull
    public static ListBuilder create() {
        return new ListBuilder();
    }

    /**
     * Creates a ListBuilder that wraps an existing ListNode.
     * Changes made through the builder will modify the existing node.
     *
     * @param node the existing ListNode to wrap
     * @return a builder wrapping the node
     */
    @NotNull
    public static ListBuilder from(@NotNull ListNode node) {
        ListBuilder builder = new ListBuilder();
        builder.baseNode = node;
        builder.style = node.getStyle();
        builder.commentsBefore.addAll(node.getCommentsBefore());
        builder.inlineComment = node.getInlineComment();
        builder.emptyLinesBefore = node.getEmptyLinesBefore();
        if (node.getMetadata().hasAnchor()) {
            builder.anchor = node.getMetadata().getAnchor();
        }
        return builder;
    }

    /**
     * Sets the collection style.
     *
     * @param style the style
     * @return this builder
     */
    @NotNull
    public ListBuilder style(@NotNull CollectionStyle style) {
        this.style = style;
        return this;
    }

    /**
     * Sets the style to flow (inline).
     *
     * @return this builder
     */
    @NotNull
    public ListBuilder flow() {
        this.style = CollectionStyle.FLOW;
        return this;
    }

    /**
     * Sets the style to block.
     *
     * @return this builder
     */
    @NotNull
    public ListBuilder block() {
        this.style = CollectionStyle.BLOCK;
        return this;
    }

    /**
     * Adds a comment before the list.
     *
     * @param comment the comment text
     * @return this builder
     */
    @NotNull
    public ListBuilder comment(@NotNull String comment) {
        this.commentsBefore.add(comment);
        return this;
    }

    /**
     * Sets an inline comment.
     *
     * @param comment the comment text
     * @return this builder
     */
    @NotNull
    public ListBuilder inlineComment(@NotNull String comment) {
        this.inlineComment = comment;
        return this;
    }

    /**
     * Adds empty lines before the list.
     *
     * @param count number of empty lines
     * @return this builder
     */
    @NotNull
    public ListBuilder emptyLines(int count) {
        this.emptyLinesBefore = count;
        return this;
    }

    /**
     * Sets an anchor for this list.
     *
     * @param anchor the anchor name
     * @return this builder
     */
    @NotNull
    public ListBuilder anchor(@NotNull String anchor) {
        this.anchor = anchor;
        return this;
    }

    /**
     * Adds a string value.
     *
     * @param value the value
     * @return this builder
     */
    @NotNull
    public ListBuilder add(@NotNull String value) {
        items.add(new ItemBuilder(new ScalarNode(value)));
        return this;
    }

    /**
     * Adds an integer value.
     *
     * @param value the value
     * @return this builder
     */
    @NotNull
    public ListBuilder add(int value) {
        items.add(new ItemBuilder(new ScalarNode(value)));
        return this;
    }

    /**
     * Adds a long value.
     *
     * @param value the value
     * @return this builder
     */
    @NotNull
    public ListBuilder add(long value) {
        items.add(new ItemBuilder(new ScalarNode(value)));
        return this;
    }

    /**
     * Adds a double value.
     *
     * @param value the value
     * @return this builder
     */
    @NotNull
    public ListBuilder add(double value) {
        items.add(new ItemBuilder(new ScalarNode(value)));
        return this;
    }

    /**
     * Adds a boolean value.
     *
     * @param value the value
     * @return this builder
     */
    @NotNull
    public ListBuilder add(boolean value) {
        items.add(new ItemBuilder(new ScalarNode(value)));
        return this;
    }

    /**
     * Adds a node value.
     *
     * @param value the value node
     * @return this builder
     */
    @NotNull
    public ListBuilder add(@NotNull YamlNode value) {
        items.add(new ItemBuilder(value));
        return this;
    }

    /**
     * Adds a nested map.
     *
     * @param builder the map builder
     * @return this builder
     */
    @NotNull
    public ListBuilder add(@NotNull MapBuilder builder) {
        items.add(new ItemBuilder(builder.build()));
        return this;
    }

    /**
     * Adds a nested list.
     *
     * @param builder the list builder
     * @return this builder
     */
    @NotNull
    public ListBuilder add(@NotNull ListBuilder builder) {
        items.add(new ItemBuilder(builder.build()));
        return this;
    }

    /**
     * Adds a string value with specific style.
     *
     * @param value the value
     * @param style the scalar style
     * @return this builder
     */
    @NotNull
    public ListBuilder add(@NotNull String value, @NotNull ScalarStyle style) {
        items.add(new ItemBuilder(new ScalarNode(value, style)));
        return this;
    }

    /**
     * Creates an item builder for advanced item configuration.
     *
     * @return the item builder
     */
    @NotNull
    public ItemBuilder item() {
        ItemBuilder ib = new ItemBuilder();
        items.add(ib);
        return ib;
    }

    /**
     * Builds the ListNode.
     * If this builder was created from an existing node, returns the modified node.
     *
     * @return the constructed ListNode
     */
    @NotNull
    public ListNode build() {
        ListNode list;
        if (baseNode != null) {
            list = baseNode;
            list.setStyle(style);
        } else {
            list = new ListNode(style);
        }
        list.setCommentsBefore(commentsBefore);
        list.setInlineComment(inlineComment);
        list.setEmptyLinesBefore(emptyLinesBefore);
        if (anchor != null) {
            list.getMetadata().setAnchor(anchor);
        }

        for (ItemBuilder ib : items) {
            list.addEntry(ib.buildEntry());
        }

        return list;
    }

    /**
     * Applies changes to the base node without returning a new node.
     * Only works if this builder was created with from().
     *
     * @return the modified ListNode
     * @throws IllegalStateException if not created from an existing node
     */
    @NotNull
    public ListNode apply() {
        if (baseNode == null) {
            throw new IllegalStateException("apply() can only be called on builders created with from()");
        }
        return build();
    }

    /**
     * Builder for individual list items.
     */
    public class ItemBuilder {

        private @Nullable YamlNode value;
        private final @NotNull List<String> commentsBefore;
        private @Nullable String inlineComment;
        private int emptyLinesBefore;

        private ItemBuilder() {
            this.value = null;
            this.commentsBefore = new ArrayList<>();
            this.inlineComment = null;
            this.emptyLinesBefore = 0;
        }

        private ItemBuilder(@NotNull YamlNode value) {
            this.value = value;
            this.commentsBefore = new ArrayList<>();
            this.inlineComment = null;
            this.emptyLinesBefore = 0;
        }

        /**
         * Sets the value as a string.
         *
         * @param value the value
         * @return the parent ListBuilder
         */
        @NotNull
        public ListBuilder value(@NotNull String value) {
            this.value = new ScalarNode(value);
            return ListBuilder.this;
        }

        /**
         * Sets the value as an integer.
         *
         * @param value the value
         * @return the parent ListBuilder
         */
        @NotNull
        public ListBuilder value(int value) {
            this.value = new ScalarNode(value);
            return ListBuilder.this;
        }

        /**
         * Sets the value as a boolean.
         *
         * @param value the value
         * @return the parent ListBuilder
         */
        @NotNull
        public ListBuilder value(boolean value) {
            this.value = new ScalarNode(value);
            return ListBuilder.this;
        }

        /**
         * Sets the value as a node.
         *
         * @param value the value node
         * @return the parent ListBuilder
         */
        @NotNull
        public ListBuilder value(@NotNull YamlNode value) {
            this.value = value;
            return ListBuilder.this;
        }

        /**
         * Sets the value as a built map.
         *
         * @param builder the map builder
         * @return the parent ListBuilder
         */
        @NotNull
        public ListBuilder value(@NotNull MapBuilder builder) {
            this.value = builder.build();
            return ListBuilder.this;
        }

        /**
         * Sets the value as a built list.
         *
         * @param builder the list builder
         * @return the parent ListBuilder
         */
        @NotNull
        public ListBuilder value(@NotNull ListBuilder builder) {
            this.value = builder.build();
            return ListBuilder.this;
        }

        /**
         * Adds a comment before this item.
         *
         * @param comment the comment
         * @return this item builder
         */
        @NotNull
        public ItemBuilder comment(@NotNull String comment) {
            this.commentsBefore.add(comment);
            return this;
        }

        /**
         * Sets an inline comment.
         *
         * @param comment the comment
         * @return this item builder
         */
        @NotNull
        public ItemBuilder inlineComment(@NotNull String comment) {
            this.inlineComment = comment;
            return this;
        }

        /**
         * Adds empty lines before this item.
         *
         * @param count the count
         * @return this item builder
         */
        @NotNull
        public ItemBuilder emptyLines(int count) {
            this.emptyLinesBefore = count;
            return this;
        }

        @NotNull
        ListNode.ListEntry buildEntry() {
            if (value == null) {
                throw new IllegalStateException("Item value not set");
            }
            ListNode.ListEntry entry = new ListNode.ListEntry(value);
            entry.setCommentsBefore(commentsBefore);
            entry.setInlineComment(inlineComment);
            entry.setEmptyLinesBefore(emptyLinesBefore);
            return entry;
        }
    }
}
