package net.vansencool.lsyaml.parser.diagnostic;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.diagnostic.Severity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Diagnostic for a line that is neither a mapping entry nor a sequence item.
 */
public final class InvalidSyntaxDiagnostic {

    private InvalidSyntaxDiagnostic() {
    }

    /**
     * @param sourceFile file name shown in the location header, may be null
     * @param fullSource the whole document, used to render the fix
     * @param line       one based source line
     * @param text       content of the offending line
     * @param indent     indentation of the line
     * @return diagnostic describing the invalid line
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, int line, @NotNull String text, int indent) {
        return Diagnostic.builder()
                .severity(Severity.ERROR)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(indent, text.length())
                .title("expected a key-value pair or list item")
                .label("not valid mapping or sequence syntax")
                .help("mapping entries look like `key: value` and sequence items start with `- `")
                .build();
    }
}
