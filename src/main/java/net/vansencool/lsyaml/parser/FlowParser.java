package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.util.FlowContent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses flow-style YAML content (inline maps and lists).
 * <p>
 * Flow style uses compact notation with braces for maps and brackets for lists,
 * such as {@code {key: value}} or {@code [item1, item2]}.
 * </p>
 */
public class FlowParser {

    private final ScalarParser scalarParser;

    /**
     * Creates a new flow parser with the given scalar parser.
     *
     * @param scalarParser the scalar parser for parsing individual values
     */
    public FlowParser(@NotNull ScalarParser scalarParser) {
        this.scalarParser = scalarParser;
    }

    /**
     * Parses a flow map from the current line in the parse context.
     * Advances the context past the consumed line.
     *
     * @param ctx the parse context positioned at a flow map line
     * @return the parsed MapNode with FLOW style
     */
    @NotNull
    public MapNode parseFlowMap(@NotNull ParseContext ctx) {
        String line = ctx.currentLineContent().trim();
        MapNode map = parseFlowMapFromString(line);
        ctx.advanceLine();
        return map;
    }

    /**
     * Parses a flow map from a string value.
     * Handles nested braces and quoted values.
     *
     * @param str the flow map string, e.g. {@code {key: value}}
     * @return the parsed MapNode with FLOW style
     */
    @NotNull
    public MapNode parseFlowMapFromString(@NotNull String str) {
        MapNode map = new MapNode(CollectionStyle.FLOW);

        str = str.trim();
        if (str.startsWith("{")) {
            str = str.substring(1);
        }
        if (str.endsWith("}")) {
            str = str.substring(0, str.length() - 1);
        }

        parseFlowMapContent(map, str.trim());
        return map;
    }

    private void parseFlowMapContent(@NotNull MapNode map, @NotNull String content) {
        if (content.isEmpty()) {
            return;
        }

        List<String> pairs = splitFlowContent(content); // split by comma, respecting nesting
        for (String pair : pairs) {
            int colonIdx = ParserUtils.findUnquotedColon(pair); // find key:value separator
            if (colonIdx > 0) {
                String key = pair.substring(0, colonIdx).trim();
                String value = pair.substring(colonIdx + 1).trim();

                key = ParserUtils.unquoteKey(key);
                ScalarStyle keyStyle = ParserUtils.detectKeyStyle(pair.substring(0, colonIdx).trim());

                YamlNode valueNode = parseFlowValue(value);
                MapNode.MapEntry entry = new MapNode.MapEntry(key, valueNode, keyStyle);
                map.putEntry(entry);
            }
        }
    }

    /**
     * Parses a flow list from the current line in the parse context.
     * Advances the context past the consumed line.
     *
     * @param ctx the parse context positioned at a flow list line
     * @return the parsed ListNode with FLOW style
     */
    @NotNull
    public ListNode parseFlowList(@NotNull ParseContext ctx) {
        String line = ctx.currentLineContent().trim();
        ListNode list = parseFlowListFromString(line);
        ctx.advanceLine();
        return list;
    }

    @NotNull
    public ListNode parseFlowListFromString(@NotNull String str) {
        ListNode list = new ListNode(CollectionStyle.FLOW);

        str = str.trim();
        if (str.startsWith("[")) {
            str = str.substring(1);
        }
        if (str.endsWith("]")) {
            str = str.substring(0, str.length() - 1);
        }

        List<String> items = splitFlowContent(str.trim());
        for (String item : items) {
            if (!item.trim().isEmpty()) {
                list.addEntry(new ListNode.ListEntry(parseFlowValue(item.trim())));
            }
        }

        return list;
    }

    @NotNull
    public YamlNode parseFlowValue(@NotNull String value) {
        value = value.trim();

        if (value.startsWith("{")) {
            return parseFlowMapFromStringNested(value);
        }
        if (value.startsWith("[")) {
            return parseFlowListFromStringNested(value);
        }

        return scalarParser.parseScalar(value);
    }

    @NotNull
    public YamlNode parseFlowValueInline(@NotNull String value) {
        value = value.trim();

        if (value.startsWith("{")) {
            return parseFlowMapFromStringNested(value);
        }
        if (value.startsWith("[")) {
            return parseFlowListFromStringNested(value);
        }

        return scalarParser.parseScalar(value);
    }

    @NotNull
    public MapNode parseFlowMapFromStringNested(@NotNull String str) {
        MapNode map = new MapNode(CollectionStyle.FLOW);

        str = str.trim();
        if (str.startsWith("{")) {
            str = str.substring(1);
        }
        if (str.endsWith("}")) {
            str = str.substring(0, str.length() - 1);
        }

        parseFlowMapContent(map, str.trim());
        return map;
    }

    @NotNull
    public ListNode parseFlowListFromStringNested(@NotNull String str) {
        ListNode list = new ListNode(CollectionStyle.FLOW);

        str = str.trim();
        if (str.startsWith("[")) {
            str = str.substring(1);
        }
        if (str.endsWith("]")) {
            str = str.substring(0, str.length() - 1);
        }

        List<String> items = splitFlowContent(str.trim());
        for (String item : items) {
            if (!item.trim().isEmpty()) {
                list.addEntry(new ListNode.ListEntry(parseFlowValue(item.trim())));
            }
        }

        return list;
    }

    @NotNull
    public List<String> splitFlowContent(@NotNull String content) {
        List<String> parts = new ArrayList<>();
        int depth = 0; // tracks nesting depth of braces/brackets
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            // toggle quote state when not inside the other quote type
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(c);
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(c);
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (c == '{' || c == '[') {
                    depth++;
                    current.append(c);
                } else if (c == '}' || c == ']') {
                    depth--;
                    current.append(c);
                } else if (c == ',' && depth == 0) {
                    // only split on comma at top level
                    parts.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    @NotNull
    public FlowContent collectMultiLineFlow(@NotNull ParseContext ctx, @NotNull String initial, char openBrace, char closeBrace) {
        StringBuilder content = new StringBuilder(initial);
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean multiLine = false;
        int flowIndent = 2;

        for (int i = 0; i < initial.length(); i++) {
            char c = initial.charAt(i);
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (!inSingleQuote && !inDoubleQuote) {
                if (c == openBrace) depth++;
                else if (c == closeBrace) depth--;
            }
        }

        while (depth > 0 && ctx.hasMoreLines()) {
            multiLine = true;
            String nextLine = ctx.currentLineContent();
            int nextIndent = ctx.currentIndent();
            if (flowIndent == 2 && nextIndent > 0) {
                flowIndent = nextIndent;
            }
            int trimEnd = nextLine.length();
            while (trimEnd > nextIndent && nextLine.charAt(trimEnd - 1) == ' ') trimEnd--;
            content.append(" ").append(nextLine, nextIndent, trimEnd);
            ctx.advanceLine();

            for (int i = nextIndent; i < trimEnd; i++) {
                char c = nextLine.charAt(i);
                if (c == '\'' && !inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                } else if (c == '"' && !inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                } else if (!inSingleQuote && !inDoubleQuote) {
                    if (c == openBrace) depth++;
                    else if (c == closeBrace) depth--;
                }
            }
        }

        return new FlowContent(content.toString(), multiLine, flowIndent);
    }
}
