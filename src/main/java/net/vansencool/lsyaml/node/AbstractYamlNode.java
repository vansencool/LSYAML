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
     * Blank and comment lines before this node, in source order.
     */
    protected @Nullable List<AdjacentLine> leadingLines;

    /**
     * Blank and comment lines after this node, in source order.
     */
    protected @Nullable List<AdjacentLine> trailingLines;

    /**
     * Comment on the same line as this node.
     */
    protected @Nullable String inlineComment;

    /**
     * Creates a new AbstractYamlNode with default metadata and no comments.
     */
    protected AbstractYamlNode() {
        this.metadata = new NodeMetadata();
        this.inlineComment = null;
    }

    /**
     * Creates a new AbstractYamlNode with the specified metadata and no comments.
     *
     * @param metadata the metadata to associate with this node
     */
    protected AbstractYamlNode(@NotNull NodeMetadata metadata) {
        this.metadata = metadata;
        this.inlineComment = null;
    }

    @Override
    public @NotNull NodeMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void setMetadata(@NotNull NodeMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Returns the blank and comment lines before this node, in source order.
     */
    public @NotNull List<AdjacentLine> getLeadingLines() {
        return leadingLines == null ? List.of() : leadingLines;
    }

    /**
     * Replaces the ordered blank and comment lines before this node.
     */
    public void setLeadingLines(@NotNull List<AdjacentLine> lines) {
        this.leadingLines = lines.isEmpty() ? null : lines;
    }

    /**
     * Appends one blank or comment line before this node.
     */
    public void addLeadingLine(@NotNull AdjacentLine line) {
        if (this.leadingLines == null) {
            this.leadingLines = new ArrayList<>();
        }
        this.leadingLines.add(line);
    }

    /**
     * Returns the blank and comment lines after this node, in source order.
     */
    public @NotNull List<AdjacentLine> getTrailingLines() {
        return trailingLines == null ? List.of() : trailingLines;
    }

    /**
     * Replaces the ordered blank and comment lines after this node.
     */
    public void setTrailingLines(@NotNull List<AdjacentLine> lines) {
        this.trailingLines = lines.isEmpty() ? null : new ArrayList<>(lines);
    }

    /**
     * Appends one blank or comment line after this node.
     */
    public void addTrailingLine(@NotNull AdjacentLine line) {
        if (this.trailingLines == null) {
            this.trailingLines = new ArrayList<>();
        }
        this.trailingLines.add(line);
    }

    @Override
    public @NotNull List<String> getCommentsBefore() {
        return commentsOf(leadingLines);
    }

    @Override
    public void setCommentsBefore(@NotNull List<String> comments) {
        this.leadingLines = mergeComments(leadingLines, comments);
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
        addLeadingLine(AdjacentLine.comment(comment));
    }

    @Override
    public int getEmptyLinesBefore() {
        return blanksOf(leadingLines);
    }

    @Override
    public void setEmptyLinesBefore(int count) {
        this.leadingLines = mergeBlanks(leadingLines, Math.max(0, count));
    }

    public @NotNull List<String> getTrailingComments() {
        return commentsOf(trailingLines);
    }

    public void setTrailingComments(@NotNull List<String> comments) {
        this.trailingLines = mergeComments(trailingLines, comments);
    }

    public int getTrailingEmptyLines() {
        return blanksOf(trailingLines);
    }

    public void setTrailingEmptyLines(int count) {
        this.trailingLines = mergeBlanks(trailingLines, Math.max(0, count));
    }

    static @NotNull List<String> commentsOf(@Nullable List<AdjacentLine> lines) {
        if (lines == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (AdjacentLine line : lines) {
            if (line.isComment()) {
                result.add(line.content());
            }
        }
        return result;
    }

    static int blanksOf(@Nullable List<AdjacentLine> lines) {
        if (lines == null) {
            return 0;
        }
        int count = 0;
        for (AdjacentLine line : lines) {
            if (line.isBlank()) {
                count++;
            }
        }
        return count;
    }

    static @Nullable List<AdjacentLine> mergeComments(@Nullable List<AdjacentLine> lines, @NotNull List<String> comments) {
        List<AdjacentLine> result = new ArrayList<>();
        for (String comment : comments) {
            result.add(AdjacentLine.comment(comment));
        }
        if (lines != null) {
            for (AdjacentLine line : lines) {
                if (line.isBlank()) {
                    result.add(line);
                }
            }
        }
        return result.isEmpty() ? null : result;
    }

    static @Nullable List<AdjacentLine> mergeBlanks(@Nullable List<AdjacentLine> lines, int count) {
        List<AdjacentLine> result = new ArrayList<>();
        if (lines != null) {
            for (AdjacentLine line : lines) {
                if (line.isComment()) {
                    result.add(line);
                }
            }
        }
        for (int i = 0; i < count; i++) {
            result.add(AdjacentLine.blank());
        }
        return result.isEmpty() ? null : result;
    }

    @Override
    public @NotNull String toYaml() {
        return toYaml(2, 0);
    }

    protected @NotNull String buildCommentPrefix(int indent, int currentLevel) {
        if (leadingLines == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String indentStr = " ".repeat(indent * currentLevel);
        for (AdjacentLine line : leadingLines) {
            if (line.isComment()) {
                sb.append(indentStr).append("#").append(line.content()).append("\n");
            } else {
                sb.append("\n");
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
        if (trailingLines == null || trailingLines.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String indentStr = " ".repeat(indent * currentLevel);
        for (AdjacentLine line : trailingLines) {
            if (line.isComment()) {
                sb.append("\n").append(indentStr).append("#").append(line.content());
            } else {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    protected void copyCommentsTo(@NotNull AbstractYamlNode target) {
        target.leadingLines = this.leadingLines == null ? null : new ArrayList<>(this.leadingLines);
        target.trailingLines = this.trailingLines == null ? null : new ArrayList<>(this.trailingLines);
        target.inlineComment = this.inlineComment;
    }
}
