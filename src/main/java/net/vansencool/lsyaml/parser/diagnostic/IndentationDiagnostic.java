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
 * Diagnostic for a line indented to a level that matches no enclosing block.
 */
public final class IndentationDiagnostic {

    private IndentationDiagnostic() {
    }

    /**
     * @param sourceFile file name shown in the location header, may be null
     * @param fullSource the whole document, used to render the fix
     * @param line       one based source line
     * @param text       content of the offending line
     * @param found      indentation the line has
     * @param expected   indentation the enclosing block expects
     * @return diagnostic describing the misaligned indentation
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, int line, @NotNull String text, int found, int expected) {
        return Diagnostic.builder()
                .severity(Severity.ERROR)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(0, Math.max(1, found))
                .title("unexpected indentation")
                .label("expected " + expected + " spaces, found " + found)
                .help("align this entry with the block it belongs to")
                .fix(new Fix(
                        "indent to " + expected + " spaces",
                        List.of(Edit.replace(line, 0, found, " ".repeat(expected), "align")),
                        Applicability.MACHINE_APPLICABLE
                ))
                .build();
    }
}
