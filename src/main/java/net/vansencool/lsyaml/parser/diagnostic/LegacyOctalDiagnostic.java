package net.vansencool.lsyaml.parser.diagnostic;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.diagnostic.Severity;
import net.vansencool.lsyaml.diagnostic.fix.Applicability;
import net.vansencool.lsyaml.diagnostic.fix.Edit;
import net.vansencool.lsyaml.diagnostic.fix.Fix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Diagnostic for a leading-zero integer that is not octal under the YAML 1.2 core schema.
 */
public final class LegacyOctalDiagnostic {

    private LegacyOctalDiagnostic() {
    }

    /**
     * @param sourceFile file name shown in the location header, may be null
     * @param fullSource the whole document, used to render the fix
     * @param line       one based source line
     * @param text       content of the offending line
     * @param column     zero based column of the value
     * @param value      the scalar text
     * @return diagnostic describing the 1.1 octal
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, int line, @NotNull String text, int column, @NotNull String value) {
        String digits = value.substring(1);
        return Diagnostic.builder()
                .severity(Severity.WARNING)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(column, column + value.length())
                .title("'" + value + "' is read as a decimal number in YAML 1.2")
                .label("the leading zero has no meaning here")
                .note("the YAML 1.2 core schema writes octal as 0o" + digits)
                .fix(new Fix(
                        "use the 0o octal form",
                        List.of(Edit.replace(line, column, column + value.length(), "0o" + digits, "octal")),
                        Applicability.MAYBE_INCORRECT
                ))
                .build();
    }
}
