package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.metadata.NodeMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for YAML nodes providing common functionality.
 */
@SuppressWarnings("unused")
public abstract class AbstractYamlNode implements YamlNode {

    /**
     * Metadata about this node.
     */
    protected @NotNull NodeMetadata metadata;

    /**
     * Comment lines that appear before this node.
     */
    protected @Nullable List<String> commentsBefore;

    /**
     * Comment lines that appear after this node.
     */
    protected @Nullable List<String> trailingComments;

    /**
     * Comment on the same line as this node.
     */
    protected @Nullable String inlineComment;

    /**
     * Number of empty lines before this node.
     */
    protected int emptyLinesBefore;

    /**
     * Number of empty lines after this node.
     */
    protected int trailingEmptyLines;

    /**
     * Creates a new AbstractYamlNode with default metadata and no comments.
     */
    protected AbstractYamlNode() {
        this.metadata = new NodeMetadata();
        this.commentsBefore = null;
        this.trailingComments = null;
        this.inlineComment = null;
        this.emptyLinesBefore = 0;
        this.trailingEmptyLines = 0;
    }

    /**
     * Creates a new AbstractYamlNode with the specified metadata and no comments.
     *
     * @param metadata the metadata to associate with this node
     */
    protected AbstractYamlNode(@NotNull NodeMetadata metadata) {
        this.metadata = metadata;
        this.commentsBefore = null;
        this.trailingComments = null;
        this.inlineComment = null;
        this.emptyLinesBefore = 0;
        this.trailingEmptyLines = 0;
    }

    @Override
    public @NotNull NodeMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(@NotNull NodeMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public @NotNull List<String> getCommentsBefore() {
        return commentsBefore == null ? List.of() : commentsBefore;
    }

    @Override
    public void setCommentsBefore(@NotNull List<String> comments) {
        this.commentsBefore = comments;
    }

    @Override
    public @Nullable String getInlineComment() {
        return inlineComment;
    }

    @Override
    public void setInlineComment(@Nullable String comment) {
        this.inlineComment = comment;
    }

    @Override
    public void addCommentBefore(@NotNull String comment) {
        if (this.commentsBefore == null) {
            this.commentsBefore = new ArrayList<>();
        }
        this.commentsBefore.add(comment);
    }

    @Override
    public int getEmptyLinesBefore() {
        return emptyLinesBefore;
    }

    @Override
    public void setEmptyLinesBefore(int count) {
        this.emptyLinesBefore = Math.max(0, count);
    }

    public @NotNull List<String> getTrailingComments() {
        return trailingComments == null ? List.of() : trailingComments;
    }

    public void setTrailingComments(@NotNull List<String> comments) {
        this.trailingComments = comments;
    }

    public int getTrailingEmptyLines() {
        return trailingEmptyLines;
    }

    public void setTrailingEmptyLines(int count) {
        this.trailingEmptyLines = Math.max(0, count);
    }

    @Override
    public @NotNull String toYaml() {
        return toYaml(2, 0);
    }

    protected @NotNull String buildCommentPrefix(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        String indentStr = " ".repeat(indent * currentLevel);

        sb.append("\n".repeat(Math.max(0, emptyLinesBefore)));

        if (commentsBefore != null) {
            for (String comment : commentsBefore) {
                sb.append(indentStr).append("#").append(comment).append("\n");
            }
        }

        return sb.toString();
    }

    protected @NotNull String buildInlineComment() {
        if (inlineComment != null) {
            return " #" + inlineComment;
        }
        return "";
    }

    protected @NotNull String buildTrailingComments(int indent, int currentLevel) {
        if (trailingComments == null || trailingComments.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String indentStr = " ".repeat(indent * currentLevel);

        for (String comment : trailingComments) {
            sb.append("\n").append(indentStr).append("#").append(comment);
        }

        return sb.toString();
    }

    protected void copyCommentsTo(@NotNull AbstractYamlNode target) {
        target.commentsBefore = this.commentsBefore == null ? null : new ArrayList<>(this.commentsBefore);
        target.trailingComments = this.trailingComments == null ? null : new ArrayList<>(this.trailingComments);
        target.inlineComment = this.inlineComment;
        target.emptyLinesBefore = this.emptyLinesBefore;
        target.trailingEmptyLines = this.trailingEmptyLines;
    }
}
