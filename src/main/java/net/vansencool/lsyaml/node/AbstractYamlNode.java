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

    protected @NotNull NodeMetadata metadata;
    protected @NotNull List<String> commentsBefore;
    protected @NotNull List<String> trailingComments;
    protected @Nullable String inlineComment;
    protected int emptyLinesBefore;
    protected int trailingEmptyLines;

    protected AbstractYamlNode() {
        this.metadata = new NodeMetadata();
        this.commentsBefore = new ArrayList<>();
        this.trailingComments = new ArrayList<>();
        this.inlineComment = null;
        this.emptyLinesBefore = 0;
        this.trailingEmptyLines = 0;
    }

    protected AbstractYamlNode(@NotNull NodeMetadata metadata) {
        this.metadata = metadata;
        this.commentsBefore = new ArrayList<>();
        this.trailingComments = new ArrayList<>();
        this.inlineComment = null;
        this.emptyLinesBefore = 0;
        this.trailingEmptyLines = 0;
    }

    @Override
    @NotNull
    public NodeMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(@NotNull NodeMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    @NotNull
    public List<String> getCommentsBefore() {
        return commentsBefore;
    }

    @Override
    @Nullable
    public String getInlineComment() {
        return inlineComment;
    }

    @Override
    public void setCommentsBefore(@NotNull List<String> comments) {
        this.commentsBefore = new ArrayList<>(comments);
    }

    @Override
    public void setInlineComment(@Nullable String comment) {
        this.inlineComment = comment;
    }

    @Override
    public void addCommentBefore(@NotNull String comment) {
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

    @NotNull
    public List<String> getTrailingComments() {
        return trailingComments;
    }

    public void setTrailingComments(@NotNull List<String> comments) {
        this.trailingComments = new ArrayList<>(comments);
    }

    public int getTrailingEmptyLines() {
        return trailingEmptyLines;
    }

    public void setTrailingEmptyLines(int count) {
        this.trailingEmptyLines = Math.max(0, count);
    }

    @Override
    @NotNull
    public String toYaml() {
        return toYaml(2, 0);
    }

    protected @NotNull String buildCommentPrefix(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        String indentStr = " ".repeat(indent * currentLevel);

        sb.append("\n".repeat(Math.max(0, emptyLinesBefore)));

        for (String comment : commentsBefore) {
            sb.append(indentStr).append("#").append(comment).append("\n");
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
        if (trailingComments.isEmpty()) {
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
        target.commentsBefore = new ArrayList<>(this.commentsBefore);
        target.trailingComments = new ArrayList<>(this.trailingComments);
        target.inlineComment = this.inlineComment;
        target.emptyLinesBefore = this.emptyLinesBefore;
        target.trailingEmptyLines = this.trailingEmptyLines;
    }
}
