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
 * Diagnostic for a key that repeats one already defined in the same mapping.
 */
public final class DuplicateKeyDiagnostic {

    private DuplicateKeyDiagnostic() {
    }

    /**
     * @param sourceFile    file name shown in the location header, may be null
     * @param fullSource    the whole document, used to render the fix
     * @param key           the repeated key
     * @param line          one based line of the repeat
     * @param text          content of the repeating line
     * @param keyColumn     zero based column where the key begins
     * @param firstLine     one based line of the first definition
     * @param firstText     content of the first definition line
     * @param firstColumn   zero based column of the first key
     * @return diagnostic describing the duplicate key
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, @NotNull String key, int line, @NotNull String text, int keyColumn, int firstLine, @NotNull String firstText, int firstColumn) {
        return Diagnostic.builder()
                .severity(Severity.ERROR)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(keyColumn, keyColumn + key.length())
                .title("duplicate key '" + key + "'")
                .label("redefined here")
                .context(firstLine, firstText, firstColumn, firstColumn + key.length(), "first defined here")
                .help("remove or rename one of the entries")
                .fix(new Fix(
                        "remove the duplicate entry",
                        List.of(Edit.delete(line, 0, text.length(), "delete this line")),
                        Applicability.MAYBE_INCORRECT
                ))
                .build();
    }
}
