package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.parser.source.Source;
import org.jetbrains.annotations.NotNull;

/**
 * Collector that joins a flow value spanning multiple lines into one content string.
 */
public final class MultiLineFlow {

    private MultiLineFlow() {
    }

    /**
     * Returns the joined flow content starting from an initial line and any continuations.
     */
    public static @NotNull Result collect(@NotNull Cursor cursor, @NotNull String initial, char open, char close) {
        StringBuilder content = new StringBuilder(initial);
        boolean[] quotes = new boolean[2];
        int depth = depthOf(initial, initial.length(), open, close, quotes);
        boolean multiLine = false;
        int flowIndent = 2;

        while (depth > 0 && cursor.hasMore()) {
            multiLine = true;
            Source src = cursor.source();
            int lineStart = cursor.start();
            int lineEnd = cursor.end();
            int nextIndent = cursor.indent();
            if (flowIndent == 2 && nextIndent > 0) flowIndent = nextIndent;

            int from = lineStart + nextIndent;
            int trimEnd = lineEnd;
            while (trimEnd > from && src.charAt(trimEnd - 1) == ' ') trimEnd--;
            content.append(' ').append(src.slice(from, trimEnd));
            cursor.advance();

            depth += depthOf(src.slice(from, trimEnd), trimEnd - from, open, close, quotes);
        }

        return new Result(content.toString(), multiLine, flowIndent);
    }

    private static int depthOf(@NotNull CharSequence text, int end, char open, char close, boolean[] quotes) {
        int delta = 0;
        for (int i = 0; i < end; i++) {
            char c = text.charAt(i);
            if (c == '\'' && !quotes[1]) {
                quotes[0] = !quotes[0];
            } else if (c == '"' && !quotes[0]) {
                quotes[1] = !quotes[1];
            } else if (!quotes[0] && !quotes[1]) {
                if (c == open) delta++;
                else if (c == close) delta--;
            }
        }
        return delta;
    }

    /**
     * The joined content, whether continuation lines were consumed, and the continuation indent.
     */
    public record Result(@NotNull String content, boolean multiLine, int indent) {
    }
}
