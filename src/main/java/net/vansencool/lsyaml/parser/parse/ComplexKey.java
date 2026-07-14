package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.node.AdjacentLine;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.text.Scan;
import net.vansencool.lsyaml.parser.text.Slice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Parser for explicit complex key entries introduced by a question mark.
 */
public final class ComplexKey {

    private final @NotNull ParseSession session;

    public ComplexKey(@NotNull ParseSession session) {
        this.session = session;
    }

    /**
     * Returns the complex key entry at the cursor, or null when the line is not one.
     */
    @Nullable
    public MapNode.MapEntry parse(@NotNull List<AdjacentLine> pendingLeading, int indent) {
        Cursor cursor = session.cursor();
        Slice trimmed = cursor.trimmedContent();
        if (trimmed.isEmpty() || trimmed.charAt(0) != '?') return null;

        cursor.advance();
        Slice keyContent = trimmed.length() > 1 ? trimmed.sub(1).trim() : Slice.empty();
        YamlNode complexKey = parseKey(keyContent, indent);
        String keyString = keyString(complexKey);

        while (cursor.hasMore() && (cursor.firstChar() == 0 || cursor.firstChar() == '#')) {
            cursor.advance();
        }

        YamlNode value = parseValue(indent);

        MapNode.MapEntry entry = new MapNode.MapEntry(keyString, value);
        entry.setComplexKey(complexKey);
        entry.setLeadingLines(pendingLeading);
        return entry;
    }

    private @NotNull YamlNode parseKey(@NotNull Slice keyContent, int indent) {
        if (keyContent.isEmpty()) {
            return descend(indent, true);
        }
        char first = keyContent.charAt(0);
        if (first == '{') return Flow.map(keyContent);
        if (first == '[') return Flow.list(keyContent);
        if (Scan.hasUnquotedColon(keyContent)) {
            YamlNode inline = inlineMap(keyContent, indent + 2);
            return inline != null ? inline : session.reparseInline(indent + 2, keyContent, '?');
        }
        return Scalars.of(keyContent);
    }

    private @Nullable MapNode inlineMap(@NotNull Slice content, int mapIndent) {
        Cursor cursor = session.cursor();
        if (cursor.hasMore() && cursor.firstChar() != 0 && cursor.indent() >= mapIndent) {
            return null;
        }

        KeyLine key = KeyLine.parse(content);
        if (key == null || key.inlineComment() != null || key.value().isEmpty()) {
            return null;
        }
        char vf = key.value().charAt(0);
        if (vf == '{' || vf == '[' || vf == '|' || vf == '>' || vf == '&' || vf == '*' || vf == '!') {
            return null;
        }
        if (Anchors.anchorOnly(key.value()) != null) {
            return null;
        }

        int line = cursor.line();
        MapNode map = new MapNode();
        map.getMetadata().setLine(line);

        YamlNode value = session.values().parse(key.value(), mapIndent);
        if (value.getMetadata().getLine() < 0) {
            value.getMetadata().setLine(line);
            value.getMetadata().setColumn(mapIndent + key.key().length() + 3);
        }
        MapNode.MapEntry entry = new MapNode.MapEntry(key.key(), value, key.keyStyle());
        map.putEntry(entry);
        return map;
    }

    private @NotNull YamlNode parseValue(int indent) {
        Cursor cursor = session.cursor();
        if (!cursor.hasMore()) return new ScalarNode(null);

        Slice valueTrimmed = cursor.trimmedContent();
        int valueIndent = cursor.indent();
        if (!valueTrimmed.startsWith(':')) return new ScalarNode(null);

        cursor.advance();
        Slice valueContent = valueTrimmed.length() > 1 ? valueTrimmed.sub(1).trim() : Slice.empty();
        if (!valueContent.isEmpty()) {
            return session.values().parse(valueContent, valueIndent);
        }
        return descend(indent, false);
    }

    private @NotNull YamlNode descend(int indent, boolean keyContext) {
        Cursor cursor = session.cursor();
        List<AdjacentLine> nested = session.skipBlanksAndComments();
        if (!cursor.hasMore()) return new ScalarNode(null);

        int nextIndent = cursor.indent();
        char nextFirst = cursor.firstChar();
        boolean deeper = keyContext ? (nextIndent > indent && nextFirst != ':') : nextIndent > indent;
        if (!deeper) return new ScalarNode(null);

        return nextFirst == '-'
                ? session.list().parse(nextIndent, nested)
                : session.map().parse(nextIndent, nested);
    }

    private @NotNull String keyString(@NotNull YamlNode node) {
        if (node instanceof ScalarNode scalar) {
            Object val = scalar.getValue();
            return val != null ? val.toString() : "";
        }
        if (node instanceof MapNode mapNode) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (MapNode.MapEntry e : mapNode.entries()) {
                if (!first) sb.append(", ");
                sb.append(e.getKey()).append(": ").append(keyString(e.getValue()));
                first = false;
            }
            return sb.append("}").toString();
        }
        if (node instanceof ListNode listNode) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (YamlNode item : listNode) {
                if (!first) sb.append(", ");
                sb.append(keyString(item));
                first = false;
            }
            return sb.append("]").toString();
        }
        return "";
    }

}
