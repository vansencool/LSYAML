package net.vansencool.lsyaml.builder;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ScalarNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating ScalarNode instances with fluent API.
 */
@SuppressWarnings("unused")
public class ScalarBuilder {

    private @Nullable Object value;
    private @NotNull ScalarStyle style;
    private final @NotNull List<String> commentsBefore;
    private @Nullable String inlineComment;
    private int emptyLinesBefore;
    private @Nullable String anchor;
    private @Nullable String tag;

    public ScalarBuilder() {
        this.value = null;
        this.style = ScalarStyle.PLAIN;
        this.commentsBefore = new ArrayList<>();
        this.inlineComment = null;
        this.emptyLinesBefore = 0;
        this.anchor = null;
        this.tag = null;
    }

    /**
     * Creates a new ScalarBuilder instance.
     *
     * @return a new builder
     */
    @NotNull
    public static ScalarBuilder create() {
        return new ScalarBuilder();
    }

    /**
     * Sets the value as a string.
     *
     * @param value the string value
     * @return this builder
     */
    @NotNull
    public ScalarBuilder string(@NotNull String value) {
        this.value = value;
        return this;
    }

    /**
     * Sets the value as an integer.
     *
     * @param value the integer value
     * @return this builder
     */
    @NotNull
    public ScalarBuilder integer(int value) {
        this.value = value;
        return this;
    }

    /**
     * Sets the value as a long.
     *
     * @param value the long value
     * @return this builder
     */
    @NotNull
    public ScalarBuilder longVal(long value) {
        this.value = value;
        return this;
    }

    /**
     * Sets the value as a double.
     *
     * @param value the double value
     * @return this builder
     */
    @NotNull
    public ScalarBuilder doubleVal(double value) {
        this.value = value;
        return this;
    }

    /**
     * Sets the value as a boolean.
     *
     * @param value the boolean value
     * @return this builder
     */
    @NotNull
    public ScalarBuilder bool(boolean value) {
        this.value = value;
        return this;
    }

    /**
     * Sets the value to null.
     *
     * @return this builder
     */
    @NotNull
    public ScalarBuilder nullValue() {
        this.value = null;
        return this;
    }

    /**
     * Sets the scalar style.
     *
     * @param style the style
     * @return this builder
     */
    @NotNull
    public ScalarBuilder style(@NotNull ScalarStyle style) {
        this.style = style;
        return this;
    }

    /**
     * Sets the style to plain (unquoted).
     *
     * @return this builder
     */
    @NotNull
    public ScalarBuilder plain() {
        this.style = ScalarStyle.PLAIN;
        return this;
    }

    /**
     * Sets the style to single-quoted.
     *
     * @return this builder
     */
    @NotNull
    public ScalarBuilder singleQuoted() {
        this.style = ScalarStyle.SINGLE_QUOTED;
        return this;
    }

    /**
     * Sets the style to double-quoted.
     *
     * @return this builder
     */
    @NotNull
    public ScalarBuilder doubleQuoted() {
        this.style = ScalarStyle.DOUBLE_QUOTED;
        return this;
    }

    /**
     * Sets the style to literal block.
     *
     * @return this builder
     */
    @NotNull
    public ScalarBuilder literal() {
        this.style = ScalarStyle.LITERAL;
        return this;
    }

    /**
     * Sets the style to folded block.
     *
     * @return this builder
     */
    @NotNull
    public ScalarBuilder folded() {
        this.style = ScalarStyle.FOLDED;
        return this;
    }

    /**
     * Adds a comment before the scalar.
     *
     * @param comment the comment text
     * @return this builder
     */
    @NotNull
    public ScalarBuilder comment(@NotNull String comment) {
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
    public ScalarBuilder inlineComment(@NotNull String comment) {
        this.inlineComment = comment;
        return this;
    }

    /**
     * Adds empty lines before the scalar.
     *
     * @param count number of empty lines
     * @return this builder
     */
    @NotNull
    public ScalarBuilder emptyLines(int count) {
        this.emptyLinesBefore = count;
        return this;
    }

    /**
     * Sets an anchor for this scalar.
     *
     * @param anchor the anchor name
     * @return this builder
     */
    @NotNull
    public ScalarBuilder anchor(@NotNull String anchor) {
        this.anchor = anchor;
        return this;
    }

    /**
     * Sets a YAML tag for this scalar.
     *
     * @param tag the tag
     * @return this builder
     */
    @NotNull
    public ScalarBuilder tag(@NotNull String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * Builds the ScalarNode.
     *
     * @return the constructed ScalarNode
     */
    @NotNull
    public ScalarNode build() {
        ScalarNode scalar = new ScalarNode(value, style);
        scalar.setCommentsBefore(commentsBefore);
        scalar.setInlineComment(inlineComment);
        scalar.setEmptyLinesBefore(emptyLinesBefore);
        if (anchor != null) {
            scalar.getMetadata().setAnchor(anchor);
        }
        if (tag != null) {
            scalar.setTag(tag);
        }
        return scalar;
    }
}
