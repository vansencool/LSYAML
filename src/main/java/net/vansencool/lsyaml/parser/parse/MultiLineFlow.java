package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.parser.source.Source;
import net.vansencool.lsyaml.parser.text.Slice;
import org.jetbrains.annotations.NotNull;

/**
 * Collector that joins a flow value spanning multiple lines into one content window.
 */
public final class MultiLineFlow {

    private MultiLineFlow() {
    }

    /**
     * Returns the joined flow content starting from an initial line and any continuations.
     */
    public static @NotNull Result collect(@NotNull Cursor cursor, @NotNull Slice initial, char open, char close) {
        boolean[] quotes = new boolean[2];
        int depth = depthOf(initial.array(), initial.start(), initial.end(), open, close, quotes);
        if (depth <= 0 || !cursor.hasMore()) {
            return new Result(initial, false, 2);
        }

        StringBuilder content = new StringBuilder(initial);
        int flowIndent = 2;
        while (depth > 0 && cursor.hasMore()) {
            Source src = cursor.source();
            int lineEnd = cursor.end();
            int nextIndent = cursor.indent();
            if (flowIndent == 2 && nextIndent > 0) flowIndent = nextIndent;

            char[] chars = src.chars();
            int from = cursor.start() + nextIndent;
            int trimEnd = lineEnd;
            while (trimEnd > from && chars[trimEnd - 1] == ' ') trimEnd--;
            content.append(' ').append(chars, from, trimEnd - from);
            cursor.advance();

            depth += depthOf(chars, from, trimEnd, open, close, quotes);
        }

        char[] joined = content.toString().toCharArray();
        return new Result(Slice.of(joined, 0, joined.length), true, flowIndent);
    }

    private static int depthOf(char[] chars, int from, int to, char open, char close, boolean[] quotes) {
        int delta = 0;
        for (int i = from; i < to; i++) {
            char c = chars[i];
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
    public record Result(@NotNull Slice content, boolean multiLine, int indent) {
    }
}
