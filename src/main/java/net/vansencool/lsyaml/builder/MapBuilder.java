package net.vansencool.lsyaml.builder;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating MapNode instances with fluent API.
 */
public class MapBuilder {

    private final @NotNull List<EntryBuilder> entries;
    private @NotNull CollectionStyle style;
    private @NotNull List<String> commentsBefore;
    private @Nullable String inlineComment;
    private int emptyLinesBefore;
    private @Nullable String anchor;

    public MapBuilder() {
        this.entries = new ArrayList<>();
        this.style = CollectionStyle.BLOCK;
        this.commentsBefore = new ArrayList<>();
        this.inlineComment = null;
        this.emptyLinesBefore = 0;
        this.anchor = null;
    }

    /**
     * Creates a new MapBuilder instance.
     *
     * @return a new builder
     */
    @NotNull
    public static MapBuilder create() {
        return new MapBuilder();
    }

    /**
     * Sets the collection style.
     *
     * @param style the style
     * @return this builder
     */
    @NotNull
    public MapBuilder style(@NotNull CollectionStyle style) {
        this.style = style;
        return this;
    }

    /**
     * Sets the style to flow (inline).
     *
     * @return this builder
     */
    @NotNull
    public MapBuilder flow() {
        this.style = CollectionStyle.FLOW;
        return this;
    }

    /**
     * Sets the style to block.
     *
     * @return this builder
     */
    @NotNull
    public MapBuilder block() {
        this.style = CollectionStyle.BLOCK;
        return this;
    }

    /**
     * Adds a comment before the map.
     *
     * @param comment the comment text
     * @return this builder
     */
    @NotNull
    public MapBuilder comment(@NotNull String comment) {
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
    public MapBuilder inlineComment(@NotNull String comment) {
        this.inlineComment = comment;
        return this;
    }

    /**
     * Adds empty lines before the map.
     *
     * @param count number of empty lines
     * @return this builder
     */
    @NotNull
    public MapBuilder emptyLines(int count) {
        this.emptyLinesBefore = count;
        return this;
    }

    /**
     * Sets an anchor for this map.
     *
     * @param anchor the anchor name
     * @return this builder
     */
    @NotNull
    public MapBuilder anchor(@NotNull String anchor) {
        this.anchor = anchor;
        return this;
    }

    /**
     * Adds a string value entry.
     *
     * @param key the key
     * @param value the value
     * @return this builder
     */
    @NotNull
    public MapBuilder put(@NotNull String key, @NotNull String value) {
        entries.add(new EntryBuilder(key, new ScalarNode(value)));
        return this;
    }

    /**
     * Adds an integer value entry.
     *
     * @param key the key
     * @param value the value
     * @return this builder
     */
    @NotNull
    public MapBuilder put(@NotNull String key, int value) {
        entries.add(new EntryBuilder(key, new ScalarNode(value)));
        return this;
    }

    /**
     * Adds a long value entry.
     *
     * @param key the key
     * @param value the value
     * @return this builder
     */
    @NotNull
    public MapBuilder put(@NotNull String key, long value) {
        entries.add(new EntryBuilder(key, new ScalarNode(value)));
        return this;
    }

    /**
     * Adds a double value entry.
     *
     * @param key the key
     * @param value the value
     * @return this builder
     */
    @NotNull
    public MapBuilder put(@NotNull String key, double value) {
        entries.add(new EntryBuilder(key, new ScalarNode(value)));
        return this;
    }

    /**
     * Adds a boolean value entry.
     *
     * @param key the key
     * @param value the value
     * @return this builder
     */
    @NotNull
    public MapBuilder put(@NotNull String key, boolean value) {
        entries.add(new EntryBuilder(key, new ScalarNode(value)));
        return this;
    }

    /**
     * Adds a node value entry.
     *
     * @param key the key
     * @param value the value node
     * @return this builder
     */
    @NotNull
    public MapBuilder put(@NotNull String key, @NotNull YamlNode value) {
        entries.add(new EntryBuilder(key, value));
        return this;
    }

    /**
     * Adds a nested map entry.
     *
     * @param key the key
     * @param builder the map builder
     * @return this builder
     */
    @NotNull
    public MapBuilder put(@NotNull String key, @NotNull MapBuilder builder) {
        entries.add(new EntryBuilder(key, builder.build()));
        return this;
    }

    /**
     * Adds a nested list entry.
     *
     * @param key the key
     * @param builder the list builder
     * @return this builder
     */
    @NotNull
    public MapBuilder put(@NotNull String key, @NotNull ListBuilder builder) {
        entries.add(new EntryBuilder(key, builder.build()));
        return this;
    }

    /**
     * Adds a scalar entry with specific style and optional comment.
     *
     * @param key the key
     * @param value the value
     * @param style the scalar style
     * @return this builder
     */
    @NotNull
    public MapBuilder put(@NotNull String key, @NotNull String value, @NotNull ScalarStyle style) {
        entries.add(new EntryBuilder(key, new ScalarNode(value, style)));
        return this;
    }

    /**
     * Creates an entry builder for advanced entry configuration.
     *
     * @param key the key
     * @return the entry builder
     */
    @NotNull
    public EntryBuilder entry(@NotNull String key) {
        EntryBuilder eb = new EntryBuilder(key);
        entries.add(eb);
        return eb;
    }

    /**
     * Builds the MapNode.
     *
     * @return the constructed MapNode
     */
    @NotNull
    public MapNode build() {
        MapNode map = new MapNode(style);
        map.setCommentsBefore(commentsBefore);
        map.setInlineComment(inlineComment);
        map.setEmptyLinesBefore(emptyLinesBefore);
        if (anchor != null) {
            map.getMetadata().setAnchor(anchor);
        }

        for (EntryBuilder eb : entries) {
            map.putEntry(eb.buildEntry());
        }

        return map;
    }

    /**
     * Builder for individual map entries.
     */
    public class EntryBuilder {

        private @NotNull String key;
        private @Nullable YamlNode value;
        private @NotNull ScalarStyle keyStyle;
        private @NotNull List<String> commentsBefore;
        private @Nullable String inlineComment;
        private int emptyLinesBefore;

        private EntryBuilder(@NotNull String key) {
            this.key = key;
            this.value = null;
            this.keyStyle = ScalarStyle.PLAIN;
            this.commentsBefore = new ArrayList<>();
            this.inlineComment = null;
            this.emptyLinesBefore = 0;
        }

        private EntryBuilder(@NotNull String key, @NotNull YamlNode value) {
            this.key = key;
            this.value = value;
            this.keyStyle = ScalarStyle.PLAIN;
            this.commentsBefore = new ArrayList<>();
            this.inlineComment = null;
            this.emptyLinesBefore = 0;
        }

        /**
         * Sets the value as a string.
         *
         * @param value the value
         * @return the parent MapBuilder
         */
        @NotNull
        public MapBuilder value(@NotNull String value) {
            this.value = new ScalarNode(value);
            return MapBuilder.this;
        }

        /**
         * Sets the value as an integer.
         *
         * @param value the value
         * @return the parent MapBuilder
         */
        @NotNull
        public MapBuilder value(int value) {
            this.value = new ScalarNode(value);
            return MapBuilder.this;
        }

        /**
         * Sets the value as a boolean.
         *
         * @param value the value
         * @return the parent MapBuilder
         */
        @NotNull
        public MapBuilder value(boolean value) {
            this.value = new ScalarNode(value);
            return MapBuilder.this;
        }

        /**
         * Sets the value as a node.
         *
         * @param value the value node
         * @return the parent MapBuilder
         */
        @NotNull
        public MapBuilder value(@NotNull YamlNode value) {
            this.value = value;
            return MapBuilder.this;
        }

        /**
         * Sets the value as a built map.
         *
         * @param builder the map builder
         * @return the parent MapBuilder
         */
        @NotNull
        public MapBuilder value(@NotNull MapBuilder builder) {
            this.value = builder.build();
            return MapBuilder.this;
        }

        /**
         * Sets the value as a built list.
         *
         * @param builder the list builder
         * @return the parent MapBuilder
         */
        @NotNull
        public MapBuilder value(@NotNull ListBuilder builder) {
            this.value = builder.build();
            return MapBuilder.this;
        }

        /**
         * Sets the key style.
         *
         * @param style the style
         * @return this entry builder
         */
        @NotNull
        public EntryBuilder keyStyle(@NotNull ScalarStyle style) {
            this.keyStyle = style;
            return this;
        }

        /**
         * Adds a comment before this entry.
         *
         * @param comment the comment
         * @return this entry builder
         */
        @NotNull
        public EntryBuilder comment(@NotNull String comment) {
            this.commentsBefore.add(comment);
            return this;
        }

        /**
         * Sets an inline comment.
         *
         * @param comment the comment
         * @return this entry builder
         */
        @NotNull
        public EntryBuilder inlineComment(@NotNull String comment) {
            this.inlineComment = comment;
            return this;
        }

        /**
         * Adds empty lines before this entry.
         *
         * @param count the count
         * @return this entry builder
         */
        @NotNull
        public EntryBuilder emptyLines(int count) {
            this.emptyLinesBefore = count;
            return this;
        }

        @NotNull
        MapNode.MapEntry buildEntry() {
            if (value == null) {
                throw new IllegalStateException("Entry value not set for key: " + key);
            }
            MapNode.MapEntry entry = new MapNode.MapEntry(key, value, keyStyle);
            entry.setCommentsBefore(commentsBefore);
            entry.setInlineComment(inlineComment);
            entry.setEmptyLinesBefore(emptyLinesBefore);
            return entry;
        }
    }
}
