package net.vansencool.lsyaml.node;

import org.jetbrains.annotations.NotNull;

/**
 * A blank line or comment line adjacent to a node, in source order.
 */
public record AdjacentLine(@NotNull Kind kind, @NotNull String content) {

    /**
     * Whether an adjacent line is a blank line or a comment.
     */
    public enum Kind {
        BLANK,
        COMMENT
    }

    /**
     * Returns a blank line.
     */
    public static @NotNull AdjacentLine blank() {
        return new AdjacentLine(Kind.BLANK, "");
    }

    /**
     * Returns a comment line holding the text after the hash.
     */
    public static @NotNull AdjacentLine comment(@NotNull String text) {
        return new AdjacentLine(Kind.COMMENT, text);
    }

    /**
     * Returns whether this line is a comment.
     */
    public boolean isComment() {
        return kind == Kind.COMMENT;
    }

    /**
     * Returns whether this line is blank.
     */
    public boolean isBlank() {
        return kind == Kind.BLANK;
    }
}
