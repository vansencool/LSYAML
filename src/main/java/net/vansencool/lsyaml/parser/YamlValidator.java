package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.parser.source.LineIndex;
import net.vansencool.lsyaml.parser.source.Source;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Strategy that checks a parsed document against a validity contract.
 */
public interface YamlValidator {

    /**
     * Appends one diagnostic per issue found in the source to the given list.
     */
    void validate(@NotNull Source source, @NotNull LineIndex lines, @Nullable String sourceFile, @NotNull List<Diagnostic> out);
}
