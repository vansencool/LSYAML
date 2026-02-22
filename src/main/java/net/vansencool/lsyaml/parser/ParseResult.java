package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of parsing YAML with detailed issue reporting.
 * Provides access to the parsed node and any issues encountered.
 */
public final class ParseResult {

    private final YamlNode node;
    private final List<ParseIssue> issues;
    private final boolean success;

    private ParseResult(@Nullable YamlNode node, @NotNull List<ParseIssue> issues, boolean success) {
        this.node = node;
        this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
        this.success = success;
    }

    /**
     * Creates a successful result.
     *
     * @param node the parsed node
     * @return the result
     */
    @NotNull
    public static ParseResult success(@NotNull YamlNode node) {
        return new ParseResult(node, List.of(), true);
    }

    /**
     * Creates a successful result with warnings.
     *
     * @param node the parsed node
     * @param warnings the warnings encountered
     * @return the result
     */
    @NotNull
    public static ParseResult successWithWarnings(@NotNull YamlNode node, @NotNull List<ParseIssue> warnings) {
        return new ParseResult(node, warnings, true);
    }

    /**
     * Creates a failed result.
     *
     * @param issues the issues that caused failure
     * @return the result
     */
    @NotNull
    public static ParseResult failure(@NotNull List<ParseIssue> issues) {
        return new ParseResult(null, issues, false);
    }

    /**
     * Creates a result with node and issues.
     *
     * @param node the parsed node (may be partial)
     * @param issues all issues encountered
     * @return the result
     */
    @NotNull
    public static ParseResult withIssues(@Nullable YamlNode node, @NotNull List<ParseIssue> issues) {
        boolean hasErrors = issues.stream().anyMatch(ParseIssue::isError);
        return new ParseResult(node, issues, !hasErrors);
    }

    /**
     * @return true if parsing succeeded without errors
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return true if there are any issues (errors or warnings)
     */
    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    /**
     * @return true if there are any errors
     */
    public boolean hasErrors() {
        return issues.stream().anyMatch(ParseIssue::isError);
    }

    /**
     * @return true if there are any warnings
     */
    public boolean hasWarnings() {
        return issues.stream().anyMatch(ParseIssue::isWarning);
    }

    /**
     * @return the parsed node, or null if parsing failed
     */
    @Nullable
    public YamlNode getNode() {
        return node;
    }

    /**
     * Gets the parsed node, throwing if parsing failed.
     *
     * @return the parsed node
     * @throws YamlParseException if parsing failed
     */
    @NotNull
    public YamlNode getNodeOrThrow() {
        if (node == null || !success) {
            throw new YamlParseException(formatIssues());
        }
        return node;
    }

    /**
     * @return all issues (errors and warnings)
     */
    @NotNull
    public List<ParseIssue> getIssues() {
        return issues;
    }

    /**
     * @return only error issues
     */
    @NotNull
    public List<ParseIssue> getErrors() {
        return issues.stream().filter(ParseIssue::isError).toList();
    }

    /**
     * @return only warning issues
     */
    @NotNull
    public List<ParseIssue> getWarnings() {
        return issues.stream().filter(ParseIssue::isWarning).toList();
    }

    /**
     * @return the number of issues
     */
    public int getIssueCount() {
        return issues.size();
    }

    /**
     * @return the number of errors
     */
    public int getErrorCount() {
        return (int) issues.stream().filter(ParseIssue::isError).count();
    }

    /**
     * @return the number of warnings
     */
    public int getWarningCount() {
        return (int) issues.stream().filter(ParseIssue::isWarning).count();
    }

    /**
     * Formats all issues as a detailed string.
     *
     * @return formatted issues
     */
    @NotNull
    public String formatIssues() {
        if (issues.isEmpty()) {
            return "No issues";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getErrorCount()).append(" error(s), ")
          .append(getWarningCount()).append(" warning(s)\n");

        for (ParseIssue issue : issues) {
            sb.append(issue.format());
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        if (success && issues.isEmpty()) {
            return "ParseResult[success]";
        } else if (success) {
            return String.format("ParseResult[success with %d warning(s)]", getWarningCount());
        } else {
            return String.format("ParseResult[failed with %d error(s), %d warning(s)]",
                    getErrorCount(), getWarningCount());
        }
    }
}
