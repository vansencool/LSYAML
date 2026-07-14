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
        StringBuilder content = new StringBuilder();
        int contentIndent = -1;

        while (cursor.hasMore()) {
            char first = cursor.firstChar();
            if (first == 0) {
                content.append('\n');
                cursor.advance();
                continue;
            }

            int lineIndent = cursor.indent();
            if (contentIndent == -1) {
                if (lineIndent > indent) contentIndent = lineIndent;
                else break;
            }
            if (lineIndent < contentIndent) break;

            if (!content.isEmpty()) content.append('\n');
            Source src = cursor.source();
            int start = cursor.start();
            int end = cursor.end();
            int from = Math.min(start + contentIndent, end);
            content.append(src.slice(from, end));
            cursor.advance();
        }

        return new ScalarNode(content.toString(), style);
    }
}
