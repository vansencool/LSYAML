package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;

/**
 * Dispatch from a trimmed value string to the matching node type.
 */
public final class Values {

    private final @NotNull ParseSession session;

    public Values(@NotNull ParseSession session) {
        this.session = session;
    }

    /**
     * Returns the node for a value string at the given indentation.
     */
    @NotNull
    public YamlNode parse(@NotNull String value, int indent) {
        String v = value.trim();
        if (v.isEmpty()) return new ScalarNode(null);

        char first = v.charAt(0);
        if (first == '*') {
            String alias = Anchors.alias(v);
            if (alias != null) {
                ScalarNode node = new ScalarNode(null);
                node.getMetadata().setAlias(alias);
                return node;
            }
        }

        if (first == '{' || first == '[') {
            return session.flow(v, first);
        }
        if (first == '|' || first == '>') {
            return session.blockScalar(v, indent);
        }

        String anchor = null;
        if (first == '&') {
            anchor = Anchors.leading(v);
            if (anchor != null) {
                session.markAnchors();
                v = Anchors.withoutLeading(v);
            }
        }

        String tag = null;
        if (!v.isEmpty() && v.charAt(0) == '!') {
            int tagEnd = 1;
            while (tagEnd < v.length() && v.charAt(tagEnd) != ' ' && v.charAt(tagEnd) != '\t') tagEnd++;
            tag = v.substring(0, tagEnd);
            while (tagEnd < v.length() && (v.charAt(tagEnd) == ' ' || v.charAt(tagEnd) == '\t')) tagEnd++;
            v = v.substring(tagEnd);
        }

        ScalarNode scalar = Scalars.of(v);
        if (anchor != null) scalar.getMetadata().setAnchor(anchor);
        if (tag != null) scalar.setTag(tag);
        return scalar;
    }
}
