package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.parser.text.Slice;
import net.vansencool.lsyaml.parser.text.Strings;
import org.jetbrains.annotations.NotNull;

/**
 * Construction of scalar nodes from a materialized value string.
 */
public final class Scalars {

    private Scalars() {
    }

    /**
     * Returns a scalar node for a value string with quote handling and type detection.
     */
    public static @NotNull ScalarNode of(@NotNull Slice value) {
        char[] chars = value.array();
        int start = value.start();
        int end = value.end();
        int len = end - start;
        if (len >= 2) {
            char first = chars[start];
            char last = chars[end - 1];
            if (first == '\'' && last == '\'') {
                String content = new String(chars, start + 1, len - 2);
                if (content.indexOf('\'') >= 0) content = content.replace("''", "'");
                return new ScalarNode(content, ScalarStyle.SINGLE_QUOTED);
            }
            if (first == '"' && last == '"') {
                String content = Strings.unescape(new String(chars, start + 1, len - 2));
                return new ScalarNode(content, ScalarStyle.DOUBLE_QUOTED);
            }
        }
        return new ScalarNode(new String(chars, start, len), ScalarStyle.PLAIN);
    }
}
