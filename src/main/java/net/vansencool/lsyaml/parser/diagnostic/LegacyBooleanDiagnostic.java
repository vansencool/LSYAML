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
 * Diagnostic for a yes, no, on, or off scalar that is not a boolean under the YAML 1.2 core schema.
 */
public final class LegacyBooleanDiagnostic {

    private LegacyBooleanDiagnostic() {
    }

    /**
     * @param sourceFile file name shown in the location header, may be null
     * @param fullSource the whole document, used to render the fix
     * @param line       one based source line
     * @param text       content of the offending line
     * @param column     zero based column of the value
     * @param value      the scalar text
     * @return diagnostic describing the 1.1 boolean
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, int line, @NotNull String text, int column, @NotNull String value) {
        boolean truthy = value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on");
        return Diagnostic.builder()
                .severity(Severity.WARNING)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(column, column + value.length())
                .title("'" + value + "' is not a boolean in YAML 1.2")
                .label("read as the string \"" + value + "\"")
                .note("the YAML 1.2 core schema treats only true and false as booleans")
                .help("write " + (truthy ? "true" : "false") + " for a boolean, or quote the value to keep it a string")
                .fix(new Fix(
                        "use " + (truthy ? "true" : "false"),
                        List.of(Edit.replace(line, column, column + value.length(), truthy ? "true" : "false", "boolean")),
                        Applicability.MAYBE_INCORRECT
                ))
                .fix(new Fix(
                        "quote to keep it a string",
                        List.of(Edit.replace(line, column, column + value.length(), "\"" + value + "\"", "string")),
                        Applicability.MAYBE_INCORRECT
                ))
                .build();
    }
}
