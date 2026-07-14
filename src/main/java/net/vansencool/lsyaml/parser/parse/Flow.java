package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.text.Scan;
import net.vansencool.lsyaml.parser.text.Slice;
import net.vansencool.lsyaml.parser.text.Strings;
import org.jetbrains.annotations.NotNull;

/**
 * Parsing of flow-style maps and lists over a character view.
 */
public final class Flow {

    private Flow() {
    }

    /**
     * Returns a flow map parsed from a brace-delimited string.
     */
    public static @NotNull MapNode map(@NotNull String str) {
        return map(Slice.of(str.toCharArray(), 0, str.length()));
    }

    /**
     * Returns a flow list parsed from a bracket-delimited string.
     */
    public static @NotNull ListNode list(@NotNull String str) {
        return list(Slice.of(str.toCharArray(), 0, str.length()));
    }

    /**
     * Returns the node for a single flow value, recursing into nested flow collections.
     */
    public static @NotNull YamlNode value(@NotNull String value) {
        return value(Slice.of(value.toCharArray(), 0, value.length()));
    }

    private static @NotNull MapNode map(@NotNull Slice str) {
        MapNode map = new MapNode(CollectionStyle.FLOW);
        mapContent(map, strip(str, '{', '}'));
        return map;
    }

    private static @NotNull ListNode list(@NotNull Slice str) {
        ListNode list = new ListNode(CollectionStyle.FLOW);
        Slice content = strip(str, '[', ']');
        int start = content.start();
        int end = content.end();
        char[] chars = content.array();
        int depth = 0;
        boolean single = false;
        boolean dbl = false;
        int itemStart = start;
        for (int i = start; i < end; i++) {
            char c = chars[i];
            if (c == '\'' && !dbl) {
                single = !single;
            } else if (c == '"' && !single) {
                dbl = !dbl;
            } else if (!single && !dbl) {
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    addItem(list, Slice.of(chars, itemStart, i).trim());
                    itemStart = i + 1;
                }
            }
        }
        addItem(list, Slice.of(chars, itemStart, end).trim());
        return list;
    }

    private static void addItem(@NotNull ListNode list, @NotNull Slice item) {
        if (!item.isEmpty()) {
            list.addEntry(new ListNode.ListEntry(value(item)));
        }
    }

    private static @NotNull YamlNode value(@NotNull Slice value) {
        Slice v = value.trim();
        if (v.startsWith('{')) return map(v);
        if (v.startsWith('[')) return list(v);
        return Scalars.of(v.toString());
    }

    private static void mapContent(@NotNull MapNode map, @NotNull Slice content) {
        if (content.isEmpty()) return;
        int start = content.start();
        int end = content.end();
        char[] chars = content.array();
        int depth = 0;
        boolean single = false;
        boolean dbl = false;
        int pairStart = start;
        for (int i = start; i < end; i++) {
            char c = chars[i];
            if (c == '\'' && !dbl) {
                single = !single;
            } else if (c == '"' && !single) {
                dbl = !dbl;
            } else if (!single && !dbl) {
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    putPair(map, Slice.of(chars, pairStart, i).trim());
                    pairStart = i + 1;
                }
            }
        }
        putPair(map, Slice.of(chars, pairStart, end).trim());
    }

    private static void putPair(@NotNull MapNode map, @NotNull Slice pair) {
        if (pair.isEmpty()) return;
        int colonIdx = Scan.unquotedColon(pair.array(), pair.start(), pair.end());
        if (colonIdx <= 0) return;
        char[] chars = pair.array();
        int base = pair.start();
        Slice valuePart = Slice.of(chars, base + colonIdx + 1, pair.end()).trim();
        Slice keyPart = pair.sub(0, colonIdx).trim();
        map.putEntry(new MapNode.MapEntry(unquoteKey(keyPart), value(valuePart), keyStyle(keyPart)));
    }

    private static @NotNull Slice strip(@NotNull Slice str, char open, char close) {
        Slice s = str.trim();
        int start = s.start();
        int end = s.end();
        char[] chars = s.array();
        if (start < end && chars[start] == open) start++;
        if (start < end && chars[end - 1] == close) end--;
        return Slice.of(chars, start, end).trim();
    }


    private static @NotNull String unquoteKey(@NotNull Slice key) {
        if (key.length() >= 2 && key.startsWith('\'') && key.endsWith('\'')) {
            String inner = key.sub(1, key.length() - 1).toString();
            return inner.indexOf('\'') >= 0 ? inner.replace("''", "'") : inner;
        }
        if (key.length() >= 2 && key.startsWith('"') && key.endsWith('"')) {
            return Strings.unescape(key.sub(1, key.length() - 1).toString());
        }
        return key.toString();
    }

    private static @NotNull ScalarStyle keyStyle(@NotNull Slice key) {
        if (key.startsWith('\'') && key.endsWith('\'')) return ScalarStyle.SINGLE_QUOTED;
        if (key.startsWith('"') && key.endsWith('"')) return ScalarStyle.DOUBLE_QUOTED;
        return ScalarStyle.PLAIN;
    }
}
