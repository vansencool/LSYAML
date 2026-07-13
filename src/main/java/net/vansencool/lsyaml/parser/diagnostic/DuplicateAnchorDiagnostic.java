package net.vansencool.lsyaml.parser.diagnostic;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.diagnostic.Severity;
import net.vansencool.lsyaml.diagnostic.fix.Applicability;
import net.vansencool.lsyaml.diagnostic.fix.Edit;
import net.vansencool.lsyaml.diagnostic.fix.Fix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Diagnostic for an anchor name that repeats one already defined in the document.
 */
public final class DuplicateAnchorDiagnostic {

    private DuplicateAnchorDiagnostic() {
    }

    /**
     * @param sourceFile  file name shown in the location header, may be null
     * @param fullSource  the whole document, used to render the fix
     * @param anchor      the repeated anchor name
     * @param line        one based line of the repeat
     * @param text        content of the repeating line
     * @param column      zero based column of the ampersand
     * @param firstLine   one based line of the first definition
     * @param firstText   content of the first definition line
     * @param firstColumn zero based column of the first ampersand
     * @param taken       anchor names already used in the document
     * @return diagnostic describing the duplicate anchor
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, @NotNull String anchor, int line, @NotNull String text, int column, int firstLine, @NotNull String firstText, int firstColumn, @NotNull Set<String> taken) {
        int width = anchor.length() + 1;
        String rename = freeName(anchor, taken);
        return Diagnostic.builder()
                .severity(Severity.WARNING)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(column, column + width)
                .title("anchor '" + anchor + "' is defined more than once")
                .label("redefined here")
                .context(firstLine, firstText, firstColumn, firstColumn + width, "first defined here")
                .note("an alias to '" + anchor + "' binds to the most recent definition under the YAML 1.2 core schema")
                .help("rename one of the anchors so each alias target is unambiguous")
                .fix(new Fix(
                        "rename this anchor",
                        List.of(Edit.replace(line, column, column + width, "&" + rename, "new anchor name")),
                        Applicability.MAYBE_INCORRECT
                ))
                .build();
    }

    private static @NotNull String freeName(@NotNull String anchor, @NotNull Set<String> taken) {
        for (int n = 2; ; n++) {
            String candidate = anchor + "_" + n;
            if (!taken.contains(candidate)) {
                return candidate;
            }
        }
    }
}
