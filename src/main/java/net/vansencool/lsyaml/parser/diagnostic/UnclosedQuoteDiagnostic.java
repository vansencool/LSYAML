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
 * Diagnostic for a quoted scalar that opens but never closes on its line.
 */
public final class UnclosedQuoteDiagnostic {

    private UnclosedQuoteDiagnostic() {
    }

    /**
     * @param sourceFile file name shown in the location header, may be null
     * @param fullSource the whole document, used to render the fix
     * @param line       one based source line
     * @param text       content of the offending line
     * @param openColumn zero based column of the opening quote
     * @param quote      the quote character
     * @param inKey      whether the quote is part of a key
     * @return diagnostic describing the unterminated quote
     */
    public static @NotNull Diagnostic build(@Nullable String sourceFile, @NotNull String fullSource, int line, @NotNull String text, int openColumn, char quote, boolean inKey) {
        String kind = quote == '\'' ? "single" : "double";
        String where = inKey ? "key" : "string";
        return Diagnostic.builder()
                .severity(Severity.ERROR)
                .sourceFile(sourceFile)
                .fullSource(fullSource)
                .at(line, text)
                .highlight(openColumn, text.length())
                .title("unterminated " + kind + "-quoted " + where)
                .label("opened here, never closed")
                .fix(new Fix(
                        "add the closing quote",
                        List.of(Edit.insert(line, text.length(), String.valueOf(quote), "close the string")),
                        Applicability.MACHINE_APPLICABLE
                ))
                .build();
    }
}
