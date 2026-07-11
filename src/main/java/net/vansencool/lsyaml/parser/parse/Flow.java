package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.text.Strings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsing of flow-style maps and lists from a single materialized content string.
 */
public final class Flow {

    private Flow() {
    }

    /**
     * Returns a flow map parsed from a brace-delimited string.
     */
    public static @NotNull MapNode map(@NotNull String str) {
        MapNode map = new MapNode(CollectionStyle.FLOW);
        String inner = strip(str, '{', '}');
        mapContent(map, inner.trim());
        return map;
    }

    /**
     * Returns a flow list parsed from a bracket-delimited string.
     */
    public static @NotNull ListNode list(@NotNull String str) {
        ListNode list = new ListNode(CollectionStyle.FLOW);
        String inner = strip(str, '[', ']');
        for (String item : split(inner.trim())) {
            if (!item.trim().isEmpty()) {
                list.addEntry(new ListNode.ListEntry(value(item.trim())));
            }
        }
        return list;
    }

    /**
     * Returns the node for a single flow value, recursing into nested flow collections.
     */
    public static @NotNull YamlNode value(@NotNull String value) {
        String v = value.trim();
        if (v.startsWith("{")) return map(v);
        if (v.startsWith("[")) return list(v);
        return Scalars.of(v);
    }

    private static void mapContent(@NotNull MapNode map, @NotNull String content) {
        if (content.isEmpty()) return;
        for (String pair : split(content)) {
            int colonIdx = unquotedColon(pair);
            if (colonIdx > 0) {
                String keyPart = pair.substring(0, colonIdx).trim();
                String value = pair.substring(colonIdx + 1).trim();
                String key = unquoteKey(keyPart);
                ScalarStyle keyStyle = keyStyle(keyPart);
                map.putEntry(new MapNode.MapEntry(key, value(value), keyStyle));
            }
        }
    }

    private static @NotNull String strip(@NotNull String str, char open, char close) {
        String s = str.trim();
        if (!s.isEmpty() && s.charAt(0) == open) s = s.substring(1);
        if (!s.isEmpty() && s.charAt(s.length() - 1) == close) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static @NotNull List<String> split(@NotNull String content) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean single = false;
        boolean dbl = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\'' && !dbl) {
                single = !single;
                current.append(c);
            } else if (c == '"' && !single) {
                dbl = !dbl;
                current.append(c);
            } else if (!single && !dbl) {
                if (c == '{' || c == '[') {
                    depth++;
                    current.append(c);
                } else if (c == '}' || c == ']') {
                    depth--;
                    current.append(c);
                } else if (c == ',' && depth == 0) {
                    parts.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) parts.add(current.toString().trim());
        return parts;
    }

    private static int unquotedColon(@NotNull String str) {
        boolean single = false;
        boolean dbl = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\'' && !dbl) single = !single;
            else if (c == '"' && !single) dbl = !dbl;
            else if (c == ':' && !single && !dbl) return i;
        }
        return -1;
    }

    private static @NotNull String unquoteKey(@NotNull String key) {
        if (key.startsWith("'") && key.endsWith("'") && key.length() >= 2) {
            return key.substring(1, key.length() - 1).replace("''", "'");
        }
        if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
            return Strings.unescape(key.substring(1, key.length() - 1));
        }
        return key;
    }

    private static @NotNull ScalarStyle keyStyle(@NotNull String key) {
        if (key.startsWith("'") && key.endsWith("'")) return ScalarStyle.SINGLE_QUOTED;
        if (key.startsWith("\"") && key.endsWith("\"")) return ScalarStyle.DOUBLE_QUOTED;
        return ScalarStyle.PLAIN;
    }
}
