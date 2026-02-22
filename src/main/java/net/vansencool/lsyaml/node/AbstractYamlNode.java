package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.metadata.NodeMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for YAML nodes providing common functionality.
 */
public abstract class AbstractYamlNode implements YamlNode {

    protected @NotNull NodeMetadata metadata;
    protected @NotNull List<String> commentsBefore;
    protected @Nullable String inlineComment;
    protected int emptyLinesBefore;

    protected AbstractYamlNode() {
        this.metadata = new NodeMetadata();
        this.commentsBefore = new ArrayList<>();
        this.inlineComment = null;
        this.emptyLinesBefore = 0;
    }

    protected AbstractYamlNode(@NotNull NodeMetadata metadata) {
        this.metadata = metadata;
        this.commentsBefore = new ArrayList<>();
        this.inlineComment = null;
        this.emptyLinesBefore = 0;
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

    @Override
    @NotNull
    public String toYaml() {
        return toYaml(2, 0);
    }

    protected @NotNull String buildCommentPrefix(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        String indentStr = " ".repeat(indent * currentLevel);

        for (int i = 0; i < emptyLinesBefore; i++) {
            sb.append("\n");
        }

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

    protected void copyCommentsTo(@NotNull AbstractYamlNode target) {
        target.commentsBefore = new ArrayList<>(this.commentsBefore);
        target.inlineComment = this.inlineComment;
        target.emptyLinesBefore = this.emptyLinesBefore;
    }
}
