package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.parser.source.Source;
import net.vansencool.lsyaml.parser.text.Slice;
import org.jetbrains.annotations.NotNull;

/**
 * Reader for literal and folded block scalars.
 */
public final class BlockScalar {

    private BlockScalar() {
    }

    /**
     * Returns the block scalar read from the cursor below an indicator line at the given indentation.
     */
    public static @NotNull ScalarNode read(@NotNull Cursor cursor, @NotNull Slice indicator, int indent) {
        ScalarStyle style = indicator.charAt(0) == '|' ? ScalarStyle.LITERAL : ScalarStyle.FOLDED;
        int firstLine = cursor.line();
        int contentIndent = -1;
        int lastLine = firstLine - 1;

        while (cursor.hasMore()) {
            if (cursor.firstChar() == 0) {
                lastLine = cursor.line();
                cursor.advance();
                continue;
            }

            int lineIndent = cursor.indent();
            if (contentIndent == -1) {
                if (lineIndent > indent) {
                    contentIndent = lineIndent;
                } else {
                    break;
                }
            }
            if (lineIndent < contentIndent) {
                break;
            }
            lastLine = cursor.line();
            cursor.advance();
        }

        return new ScalarNode(join(cursor, firstLine, lastLine, contentIndent), style);
    }

    private static @NotNull String join(@NotNull Cursor cursor, int firstLine, int lastLine, int contentIndent) {
        if (contentIndent == -1 || lastLine < firstLine) {
            return "";
        }
        int resume = cursor.line();
        int size = lastLine - firstLine;
        for (int i = firstLine; i <= lastLine; i++) {
            cursor.line(i);
            int end = cursor.end();
            size += end - Math.min(cursor.start() + contentIndent, end);
        }

        char[] chars = cursor.source().chars();
        char[] buffer = new char[size];
        int at = 0;
        for (int i = firstLine; i <= lastLine; i++) {
            if (i > firstLine) {
                buffer[at++] = '\n';
            }
            cursor.line(i);
            int end = cursor.end();
            int from = Math.min(cursor.start() + contentIndent, end);
            int length = end - from;
            System.arraycopy(chars, from, buffer, at, length);
            at += length;
        }
        cursor.line(resume);
        return new String(buffer);
    }
}
