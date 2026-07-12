package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.diagnostic.Severity;
import net.vansencool.lsyaml.exceptions.YamlParseException;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Result of parsing YAML with detailed diagnostics.
 */
@SuppressWarnings("unused")
public final class ParseResult {

    private final YamlNode node;
    private final List<Diagnostic> diagnostics;
    private final boolean success;

    private ParseResult(@Nullable YamlNode node, @NotNull List<Diagnostic> diagnostics, boolean success) {
        this.node = node;
        this.diagnostics = List.copyOf(diagnostics);
        this.success = success;
    }

    /**
     * Creates a successful result.
     */
    public static @NotNull ParseResult success(@NotNull YamlNode node) {
        return new ParseResult(node, List.of(), true);
    }

    /**
     * Creates a successful result with warnings.
     */
    public static @NotNull ParseResult successWithWarnings(@NotNull YamlNode node, @NotNull List<Diagnostic> warnings) {
        return new ParseResult(node, warnings, true);
    }

    /**
     * Creates a failed result.
     */
    public static @NotNull ParseResult failure(@NotNull List<Diagnostic> diagnostics) {
        return new ParseResult(null, diagnostics, false);
    }

    /**
     * Creates a result with a node and diagnostics, failing when any is an error.
     */
    public static @NotNull ParseResult withIssues(@Nullable YamlNode node, @NotNull List<Diagnostic> diagnostics) {
        boolean hasErrors = diagnostics.stream().anyMatch(ParseResult::isError);
        return new ParseResult(node, diagnostics, !hasErrors);
    }

    private static boolean isError(@NotNull Diagnostic d) {
        return d.severity() == Severity.ERROR;
    }

    private static boolean isWarning(@NotNull Diagnostic d) {
        return d.severity() == Severity.WARNING;
    }

    /**
     * Returns whether parsing succeeded without errors.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns whether there are any diagnostics.
     */
    public boolean hasIssues() {
        return !diagnostics.isEmpty();
    }

    /**
     * Returns whether there are any errors.
     */
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(ParseResult::isError);
    }

    /**
     * Returns whether there are any warnings.
     */
    public boolean hasWarnings() {
        return diagnostics.stream().anyMatch(ParseResult::isWarning);
    }

    /**
     * Returns the parsed node, or null when parsing failed.
     */
    public @Nullable YamlNode getNode() {
        return node;
    }

    /**
     * Returns the parsed node, throwing when parsing failed.
     */
    public @NotNull YamlNode getNodeOrThrow() {
        if (node == null || !success) {
            throw new YamlParseException(formatIssues());
        }
        return node;
    }

    /**
     * Returns all diagnostics.
     */
    public @NotNull List<Diagnostic> getIssues() {
        return diagnostics;
    }

    /**
     * Returns only error diagnostics.
     */
    public @NotNull List<Diagnostic> getErrors() {
        return diagnostics.stream().filter(ParseResult::isError).toList();
    }

    /**
     * Returns only warning diagnostics.
     */
    public @NotNull List<Diagnostic> getWarnings() {
        return diagnostics.stream().filter(ParseResult::isWarning).toList();
    }

    /**
     * Returns the number of diagnostics.
     */
    public int getIssueCount() {
        return diagnostics.size();
    }

    /**
     * Returns the number of errors.
     */
    public int getErrorCount() {
        return (int) diagnostics.stream().filter(ParseResult::isError).count();
    }

    /**
     * Returns the number of warnings.
     */
    public int getWarningCount() {
        return (int) diagnostics.stream().filter(ParseResult::isWarning).count();
    }

    /**
     * Returns a rendering of all diagnostics.
     */
    public @NotNull String formatIssues() {
        if (diagnostics.isEmpty()) {
            return "No issues";
        }
        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : diagnostics) {
            sb.append(d.format());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        if (success && diagnostics.isEmpty()) {
            return "ParseResult[success]";
        } else if (success) {
            return String.format("ParseResult[success with %d warning(s)]", getWarningCount());
        }
        return String.format("ParseResult[failed with %d error(s), %d warning(s)]", getErrorCount(), getWarningCount());
    }
}
