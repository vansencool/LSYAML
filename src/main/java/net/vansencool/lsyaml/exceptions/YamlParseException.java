package net.vansencool.lsyaml.exceptions;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Exception thrown when a document fails validation.
 */
@SuppressWarnings("unused")
public class YamlParseException extends RuntimeException {

    private final @NotNull List<Diagnostic> diagnostics;

    /**
     * Creates an exception reporting every diagnostic found in the document.
     *
     * @param diagnostics the diagnostics, rendered in order as the message
     */
    public YamlParseException(@NotNull List<Diagnostic> diagnostics) {
        super(render(diagnostics));
        this.diagnostics = List.copyOf(diagnostics);
    }

    private static @NotNull String render(@NotNull List<Diagnostic> diagnostics) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic diagnostic : diagnostics) {
            sb.append(diagnostic.format());
        }
        return sb.toString();
    }

    /**
     * Returns every diagnostic that caused this failure.
     *
     * @return the diagnostics
     */
    public @NotNull List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
