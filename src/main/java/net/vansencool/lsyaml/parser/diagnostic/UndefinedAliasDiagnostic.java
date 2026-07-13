package net.vansencool.lsyaml.parser.diagnostic;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.diagnostic.Severity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Diagnostic for an alias that references an anchor never defined before it.
 */
public final class UndefinedAliasDiagnostic {

    private UndefinedAliasDiagnostic() {
    }

    /**
     * @param sourceFile file name shown in the location header, may be null
     * @param fullSource the whole document, used to render the fix
     * @param alias      the alias name
     * @param line       one based source line
     * @param text       content of the offending line
     * @param column     zero based column of the asterisk
     * @return diagnostic describing the undefined alias
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, @NotNull String alias, int line, @NotNull String text, int column) {
        return Diagnostic.builder()
                .severity(Severity.ERROR)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(column, column + alias.length() + 1)
                .title("alias '" + alias + "' refers to an anchor that is not defined")
                .label("no anchor named '" + alias + "' appears before this point")
                .note("an alias must reference an anchor defined earlier in the document")
                .help("define the anchor with &" + alias + " before using *" + alias + ", or fix the name")
                .build();
    }
}
