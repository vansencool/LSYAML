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
 * Diagnostic for a tab character used in a line's indentation.
 */
public final class TabIndentDiagnostic {

    private TabIndentDiagnostic() {
    }

    /**
     * @param sourceFile file name shown in the location header, may be null
     * @param fullSource the whole document, used to render the fix
     * @param line       one based source line
     * @param text       content of the offending line
     * @param tabStart   zero based column of the first tab
     * @param tabEnd     zero based column past the last consecutive tab
     * @return diagnostic describing the tab indentation
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, int line, @NotNull String text, int tabStart, int tabEnd) {
        return Diagnostic.builder()
                .severity(Severity.ERROR)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(tabStart, tabEnd)
                .title("tabs are not allowed for indentation")
                .label("indented with a tab")
                .note("YAML forbids tab characters in indentation")
                .fix(new Fix(
                        "replace the tab with spaces",
                        List.of(Edit.replace(line, tabStart, tabEnd, "  ".repeat(tabEnd - tabStart), "use spaces")),
                        Applicability.MACHINE_APPLICABLE
                ))
                .build();
    }
}
