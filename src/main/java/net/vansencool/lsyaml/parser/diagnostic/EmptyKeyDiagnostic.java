package net.vansencool.lsyaml.parser.diagnostic;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.diagnostic.Severity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Diagnostic for a mapping entry whose key is missing before the colon.
 */
public final class EmptyKeyDiagnostic {

    private EmptyKeyDiagnostic() {
    }

    /**
     * @param sourceFile  file name shown in the location header, may be null
     * @param fullSource  the whole document, used to render the fix
     * @param line        one based source line
     * @param text        content of the offending line
     * @param colonColumn zero based column of the colon
     * @return diagnostic describing the empty key
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, int line, @NotNull String text, int colonColumn) {
        return Diagnostic.builder()
                .severity(Severity.ERROR)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(colonColumn, colonColumn + 1)
                .title("missing key before ':'")
                .label("the colon has no key in front of it")
                .help("a mapping entry needs a key, like `name: value`")
                .build();
    }
}
