package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.NodeMetadata;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;

/**
 * YAML parser that preserves all formatting, comments, and metadata.
 */
public class YamlParser {

    private static final Pattern KEY_PATTERN = Pattern.compile("^(['\"]?)(.+?)\\1\\s*:\\s*(.*)$");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^-\\s*(.*)$");
    private static final Pattern ANCHOR_PATTERN = Pattern.compile("&(\\w+)\\s*");
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^\\*(\\w+)\\s*(.*)$");
    private static final Pattern TAG_PATTERN = Pattern.compile("^!(\\S+)\\s*(.*)$");

    private String[] lines;
    private int currentLine;
    private int totalLines;

    public YamlParser() {
    }

    /**
     * Parses a YAML string into a node tree.
     *
     * @param yaml the YAML content
     * @return the root node
     */
    @NotNull
    public YamlNode parse(@NotNull String yaml) {
        return parseWithOptions(yaml, ParseOptions.defaults());
    }

    /**
     * Parses a YAML string with custom options.
     *
     * @param yaml the YAML content
     * @param options parse options
     * @return the root node
     */
    @NotNull
    public YamlNode parseWithOptions(@NotNull String yaml, @NotNull ParseOptions options) {
        ParseResult result = parseDetailed(yaml, options);
        if (!result.isSuccess()) {
            throw new YamlParseException(result.formatIssues());
        }
        YamlNode node = result.getNode();
        if (node == null) {
            return new MapNode();
        }
        return node;
    }

    /**
     * Parses a YAML string and returns detailed results with all issues.
     *
     * @param yaml the YAML content
     * @param options parse options
     * @return the parse result with issues
     */
    @NotNull
    public ParseResult parseDetailed(@NotNull String yaml, @NotNull ParseOptions options) {
        List<ParseIssue> issues = new ArrayList<>();
        
        try {
            YamlNode node = parseInternal(yaml, options, issues);
            if (issues.stream().anyMatch(ParseIssue::isError)) {
                return ParseResult.withIssues(node, issues);
            }
            if (issues.isEmpty()) {
                return ParseResult.success(node);
            }
            return ParseResult.successWithWarnings(node, issues);
        } catch (Exception e) {
            issues.add(ParseIssue.error(e.getMessage() != null ? e.getMessage() : "Unknown error", 
                    currentLine + 1, 1, lines != null ? lines : new String[0]));
            return ParseResult.failure(issues);
        }
    }

    @NotNull
    private YamlNode parseInternal(@NotNull String yaml, @NotNull ParseOptions options, 
                                   @NotNull List<ParseIssue> issues) {
        if (yaml.trim().isEmpty()) {
            return new MapNode();
        }

        this.lines = yaml.split("\n", -1);

        if (options.isStrict()) {
            validateStrict(issues);
        }
        this.currentLine = 0;
        this.totalLines = lines.length;

        List<String> pendingComments = new ArrayList<>();
        int pendingEmptyLines = 0;

        while (currentLine < totalLines && isEmptyOrCommentLine(lines[currentLine])) {
            String line = lines[currentLine];
            char first = firstNonSpaceChar(line);
            if (first == 0) {
                pendingEmptyLines++;
            } else if (first == '#') {
                pendingComments.add(extractComment(line));
            }
            currentLine++;
        }

        if (currentLine >= totalLines) {
            MapNode empty = new MapNode();
            empty.setCommentsBefore(pendingComments);
            empty.setEmptyLinesBefore(pendingEmptyLines);
            return empty;
        }

        char firstChar = firstNonSpaceChar(lines[currentLine]);

        YamlNode result;
        if (firstChar == '-') {
            result = parseList(0, pendingComments, pendingEmptyLines);
        } else if (firstChar == '{') {
            result = parseFlowMap();
            if (!pendingComments.isEmpty()) {
                result.setCommentsBefore(pendingComments);
            }
            if (pendingEmptyLines > 0) {
                result.setEmptyLinesBefore(pendingEmptyLines);
            }
        } else if (firstChar == '[') {
            result = parseFlowList();
            if (!pendingComments.isEmpty()) {
                result.setCommentsBefore(pendingComments);
            }
            if (pendingEmptyLines > 0) {
                result.setEmptyLinesBefore(pendingEmptyLines);
            }
        } else {
            result = parseMap(0, pendingComments, pendingEmptyLines);
        }

        return result;
    }

    @NotNull
    private MapNode parseMap(int expectedIndent) {
        return parseMap(expectedIndent, new ArrayList<>(), 0);
    }

    @NotNull
    private MapNode parseMap(int expectedIndent, @NotNull List<String> initialComments, int initialEmptyLines) {
        MapNode map = new MapNode();
        map.getMetadata().setLine(currentLine + 1);
        map.getMetadata().setIndentation(expectedIndent);

        List<String> pendingComments = new ArrayList<>(initialComments);
        int pendingEmptyLines = initialEmptyLines;

        while (currentLine < totalLines) {
            String line = lines[currentLine];
            char firstChar = firstNonSpaceChar(line);

            if (firstChar == 0) {
                pendingEmptyLines++;
                currentLine++;
                continue;
            }

            if (firstChar == '#') {
                pendingComments.add(extractComment(line));
                currentLine++;
                continue;
            }

            int indent = getIndentation(line);
            if (indent < expectedIndent) {
                currentLine -= (pendingComments.size() + pendingEmptyLines);
                break;
            }

            if (firstChar == '-') {
                currentLine -= (pendingComments.size() + pendingEmptyLines);
                break;
            }

            ParsedKey parsed = parseKeyLine(line);
            if (parsed == null) {
                break;
            }

            MapNode.MapEntry entry = new MapNode.MapEntry(parsed.key, new ScalarNode(null), parsed.keyStyle);
            entry.setCommentsBefore(pendingComments);
            entry.setEmptyLinesBefore(pendingEmptyLines);
            pendingComments = new ArrayList<>();
            pendingEmptyLines = 0;

            if (parsed.inlineComment != null) {
                entry.setInlineComment(parsed.inlineComment);
            }

            currentLine++;

            if (parsed.value.isEmpty()) {
                if (currentLine < totalLines) {
                    List<String> nestedComments = new ArrayList<>();
                    int nestedEmptyLines = 0;

                    while (currentLine < totalLines) {
                        String nextLine = lines[currentLine];
                        char nextFirst = firstNonSpaceChar(nextLine);

                        if (nextFirst == 0) {
                            nestedEmptyLines++;
                            currentLine++;
                            continue;
                        }
                        if (nextFirst == '#') {
                            nestedComments.add(extractComment(nextLine));
                            currentLine++;
                            continue;
                        }
                        break;
                    }

                    if (currentLine < totalLines) {
                        String nextLine = lines[currentLine];
                        int nextIndent = getIndentation(nextLine);
                        char nextFirst = firstNonSpaceChar(nextLine);

                        if (nextIndent > indent && nextFirst != 0) {
                            if (nextFirst == '-') {
                                entry.setValue(parseList(nextIndent, nestedComments, nestedEmptyLines));
                            } else {
                                entry.setValue(parseMap(nextIndent, nestedComments, nestedEmptyLines));
                            }
                        } else {
                            entry.setValue(new ScalarNode(null));
                            if (!nestedComments.isEmpty() || nestedEmptyLines > 0) {
                                currentLine -= (nestedComments.size() + nestedEmptyLines);
                            }
                        }
                    } else {
                        entry.setValue(new ScalarNode(null));
                    }
                } else {
                    entry.setValue(new ScalarNode(null));
                }
            } else {
                entry.setValue(parseValue(parsed.value, indent));
            }

            map.putEntry(entry);

            if (map.size() == 1) {
                expectedIndent = indent;
            }
        }

        return map;
    }

    @NotNull
    private ListNode parseList(int expectedIndent) {
        return parseList(expectedIndent, new ArrayList<>(), 0);
    }

    @NotNull
    private ListNode parseList(int expectedIndent, @NotNull List<String> initialComments, int initialEmptyLines) {
        ListNode list = new ListNode();
        list.getMetadata().setLine(currentLine + 1);
        list.getMetadata().setIndentation(expectedIndent);

        List<String> pendingComments = new ArrayList<>(initialComments);
        int pendingEmptyLines = initialEmptyLines;

        while (currentLine < totalLines) {
            String line = lines[currentLine];
            char firstChar = firstNonSpaceChar(line);

            if (firstChar == 0) {
                pendingEmptyLines++;
                currentLine++;
                continue;
            }

            if (firstChar == '#') {
                pendingComments.add(extractComment(line));
                currentLine++;
                continue;
            }

            int indent = getIndentation(line);
            if (indent < expectedIndent) {
                currentLine -= (pendingComments.size() + pendingEmptyLines);
                break;
            }

            if (firstChar != '-') {
                currentLine -= (pendingComments.size() + pendingEmptyLines);
                break;
            }

            String trimmed = line.substring(indent);
            int dashIdx = 0;
            if (trimmed.length() <= 1 || (trimmed.charAt(1) != ' ' && trimmed.charAt(1) != '\t')) {
                if (trimmed.length() > 1 && trimmed.charAt(1) != ' ') {
                    currentLine -= (pendingComments.size() + pendingEmptyLines);
                    break;
                }
            }

            ListNode.ListEntry entry = new ListNode.ListEntry(new ScalarNode(null));
            entry.setCommentsBefore(pendingComments);
            entry.setEmptyLinesBefore(pendingEmptyLines);
            pendingComments = new ArrayList<>();
            pendingEmptyLines = 0;

            String valueStr = trimmed.length() > 1 ? trimmed.substring(2).trim() : "";
            String inlineComment = extractInlineComment(valueStr);
            if (inlineComment != null) {
                entry.setInlineComment(inlineComment);
                valueStr = removeInlineComment(valueStr);
            }

            currentLine++;

            if (valueStr.isEmpty()) {
                if (currentLine < totalLines) {
                    List<String> nestedComments = new ArrayList<>();
                    int nestedEmptyLines = 0;

                    while (currentLine < totalLines) {
                        String nextLine = lines[currentLine];
                        char nextFirst = firstNonSpaceChar(nextLine);

                        if (nextFirst == 0) {
                            nestedEmptyLines++;
                            currentLine++;
                            continue;
                        }
                        if (nextFirst == '#') {
                            nestedComments.add(extractComment(nextLine));
                            currentLine++;
                            continue;
                        }
                        break;
                    }

                    if (currentLine < totalLines) {
                        String nextLine = lines[currentLine];
                        int nextIndent = getIndentation(nextLine);
                        char nextFirst = firstNonSpaceChar(nextLine);

                        if (nextIndent > indent && nextFirst != 0) {
                            if (nextFirst == '-') {
                                entry.setValue(parseList(nextIndent, nestedComments, nestedEmptyLines));
                            } else {
                                entry.setValue(parseMap(nextIndent, nestedComments, nestedEmptyLines));
                            }
                        } else {
                            entry.setValue(new ScalarNode(null));
                            if (!nestedComments.isEmpty() || nestedEmptyLines > 0) {
                                currentLine -= (nestedComments.size() + nestedEmptyLines);
                            }
                        }
                    } else {
                        entry.setValue(new ScalarNode(null));
                    }
                } else {
                    entry.setValue(new ScalarNode(null));
                }
            } else if (valueStr.contains(":") && !valueStr.startsWith("'") && !valueStr.startsWith("\"")) {
                currentLine--;
                int mapIndent = indent + 2;
                String originalLine = lines[currentLine];
                lines[currentLine] = " ".repeat(mapIndent) + valueStr;
                entry.setValue(parseMap(mapIndent));
                lines[currentLine - 1] = originalLine;
            } else if (valueStr.startsWith("-")) {
                currentLine--;
                int listIndent = indent + 2;
                String originalLine = lines[currentLine];
                lines[currentLine] = " ".repeat(listIndent) + valueStr;
                entry.setValue(parseList(listIndent));
                lines[currentLine - 1] = originalLine;
            } else {
                entry.setValue(parseValue(valueStr, indent));
            }

            list.addEntry(entry);

            if (list.size() == 1) {
                expectedIndent = indent;
            }
        }

        return list;
    }

    @NotNull
    private YamlNode parseValue(@NotNull String value, int indent) {
        value = value.trim();

        if (value.isEmpty()) {
            return new ScalarNode(null);
        }

        Matcher aliasMatcher = ALIAS_PATTERN.matcher(value);
        if (aliasMatcher.matches()) {
            ScalarNode node = new ScalarNode(null);
            node.getMetadata().setAlias(aliasMatcher.group(1));
            return node;
        }

        if (value.startsWith("{")) {
            return parseFlowMapFromString(value);
        }

        if (value.startsWith("[")) {
            return parseFlowListFromString(value);
        }

        if (value.startsWith("|") || value.startsWith(">")) {
            return parseBlockScalar(value, indent);
        }

        String anchor = null;
        Matcher anchorMatcher = ANCHOR_PATTERN.matcher(value);
        if (anchorMatcher.find() && anchorMatcher.start() == 0) {
            anchor = anchorMatcher.group(1);
            value = value.substring(anchorMatcher.end()).trim();
        }

        String tag = null;
        Matcher tagMatcher = TAG_PATTERN.matcher(value);
        if (tagMatcher.matches()) {
            tag = "!" + tagMatcher.group(1);
            value = tagMatcher.group(2).trim();
        }

        ScalarNode scalar = parseScalar(value);
        if (anchor != null) {
            scalar.getMetadata().setAnchor(anchor);
        }
        if (tag != null) {
            scalar.setTag(tag);
        }

        return scalar;
    }

    @NotNull
    private ScalarNode parseScalar(@NotNull String value) {
        String inlineComment = extractInlineComment(value);
        if (inlineComment != null) {
            value = removeInlineComment(value);
        }

        ScalarNode scalar;

        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
            String content = value.substring(1, value.length() - 1).replace("''", "'");
            scalar = new ScalarNode(content, ScalarStyle.SINGLE_QUOTED);
            scalar.setRawValue(value);
        } else if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            String content = unescapeDoubleQuoted(value.substring(1, value.length() - 1));
            scalar = new ScalarNode(content, ScalarStyle.DOUBLE_QUOTED);
            scalar.setRawValue(value);
        } else {
            Object parsed = parseUnquotedValue(value);
            scalar = new ScalarNode(parsed, ScalarStyle.PLAIN);
            scalar.setRawValue(value);
        }

        if (inlineComment != null) {
            scalar.setInlineComment(inlineComment);
        }

        return scalar;
    }

    @Nullable
    private Object parseUnquotedValue(@NotNull String value) {
        if (value.isEmpty() || "null".equalsIgnoreCase(value) || "~".equals(value)) {
            return null;
        }

        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
            return true;
        }

        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
            return false;
        }

        try {
            if (value.contains(".") || value.toLowerCase().contains("e")) {
                return Double.parseDouble(value);
            }
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseLong(value.substring(2), 16);
            }
            if (value.startsWith("0o") || value.startsWith("0O")) {
                return Long.parseLong(value.substring(2), 8);
            }
            long l = Long.parseLong(value);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            }
            return l;
        } catch (NumberFormatException e) {
            return value;
        }
    }

    @NotNull
    private ScalarNode parseBlockScalar(@NotNull String indicator, int indent) {
        boolean literal = indicator.startsWith("|");
        ScalarStyle style = literal ? ScalarStyle.LITERAL : ScalarStyle.FOLDED;

        StringBuilder content = new StringBuilder();
        int contentIndent = -1;

        while (currentLine < totalLines) {
            String line = lines[currentLine];
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                content.append("\n");
                currentLine++;
                continue;
            }

            int lineIndent = getIndentation(line);

            if (contentIndent == -1) {
                if (lineIndent > indent) {
                    contentIndent = lineIndent;
                } else {
                    break;
                }
            }

            if (lineIndent < contentIndent && !trimmed.isEmpty()) {
                break;
            }

            if (content.length() > 0) {
                content.append("\n");
            }
            content.append(line.substring(Math.min(contentIndent, line.length())));
            currentLine++;
        }

        ScalarNode scalar = new ScalarNode(content.toString(), style);
        return scalar;
    }

    @NotNull
    private MapNode parseFlowMap() {
        String line = lines[currentLine].trim();
        return parseFlowMapFromString(line);
    }

    @NotNull
    private MapNode parseFlowMapFromString(@NotNull String str) {
        MapNode map = new MapNode(CollectionStyle.FLOW);

        str = str.trim();
        if (str.startsWith("{")) {
            str = str.substring(1);
        }
        if (str.endsWith("}")) {
            str = str.substring(0, str.length() - 1);
        }

        parseFlowMapContent(map, str.trim());
        currentLine++;
        return map;
    }

    private void parseFlowMapContent(@NotNull MapNode map, @NotNull String content) {
        if (content.isEmpty()) {
            return;
        }

        List<String> pairs = splitFlowContent(content);
        for (String pair : pairs) {
            int colonIdx = findUnquotedColon(pair);
            if (colonIdx > 0) {
                String key = pair.substring(0, colonIdx).trim();
                String value = pair.substring(colonIdx + 1).trim();

                key = unquoteKey(key);
                ScalarStyle keyStyle = detectKeyStyle(pair.substring(0, colonIdx).trim());

                YamlNode valueNode = parseFlowValue(value);
                MapNode.MapEntry entry = new MapNode.MapEntry(key, valueNode, keyStyle);
                map.putEntry(entry);
            }
        }
    }

    @NotNull
    private ListNode parseFlowList() {
        String line = lines[currentLine].trim();
        return parseFlowListFromString(line);
    }

    @NotNull
    private ListNode parseFlowListFromString(@NotNull String str) {
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

        currentLine++;
        return list;
    }

    @NotNull
    private YamlNode parseFlowValue(@NotNull String value) {
        value = value.trim();

        if (value.startsWith("{")) {
            return parseFlowMapFromStringNested(value);
        }
        if (value.startsWith("[")) {
            return parseFlowListFromStringNested(value);
        }

        return parseScalar(value);
    }

    @NotNull
    private MapNode parseFlowMapFromStringNested(@NotNull String str) {
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
    private ListNode parseFlowListFromStringNested(@NotNull String str) {
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
    private List<String> splitFlowContent(@NotNull String content) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

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
                    parts.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    private int findUnquotedColon(@NotNull String str) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == ':' && !inSingleQuote && !inDoubleQuote) {
                return i;
            }
        }

        return -1;
    }

    @Nullable
    private ParsedKey parseKeyLine(@NotNull String line) {
        String trimmed = line.trim();

        int colonIdx = findUnquotedColon(trimmed);
        if (colonIdx <= 0) {
            return null;
        }

        String keyPart = trimmed.substring(0, colonIdx).trim();
        String valuePart = trimmed.substring(colonIdx + 1).trim();

        String key = unquoteKey(keyPart);
        ScalarStyle keyStyle = detectKeyStyle(keyPart);

        String inlineComment = extractInlineComment(valuePart);
        if (inlineComment != null) {
            valuePart = removeInlineComment(valuePart);
        }

        return new ParsedKey(key, keyStyle, valuePart, inlineComment);
    }

    @NotNull
    private String unquoteKey(@NotNull String key) {
        if (key.startsWith("'") && key.endsWith("'") && key.length() >= 2) {
            return key.substring(1, key.length() - 1).replace("''", "'");
        }
        if (key.startsWith("\"") && key.endsWith("\"") && key.length() >= 2) {
            return unescapeDoubleQuoted(key.substring(1, key.length() - 1));
        }
        return key;
    }

    @NotNull
    private ScalarStyle detectKeyStyle(@NotNull String key) {
        if (key.startsWith("'") && key.endsWith("'")) {
            return ScalarStyle.SINGLE_QUOTED;
        }
        if (key.startsWith("\"") && key.endsWith("\"")) {
            return ScalarStyle.DOUBLE_QUOTED;
        }
        return ScalarStyle.PLAIN;
    }

    @NotNull
    private String unescapeDoubleQuoted(@NotNull String str) {
        return str.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private int getIndentation(@NotNull String line) {
        int len = line.length();
        int count = 0;
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 2;
            } else {
                break;
            }
        }
        return count;
    }

    private char firstNonSpaceChar(@NotNull String line) {
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t') return c;
        }
        return 0;
    }

    private int firstNonSpaceIndex(@NotNull String line) {
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c != ' ' && c != '\t') return i;
        }
        return -1;
    }

    @NotNull
    private String trimmedSubstring(@NotNull String line) {
        int start = firstNonSpaceIndex(line);
        if (start < 0) return "";
        int end = line.length();
        while (end > start && line.charAt(end - 1) == ' ') end--;
        return line.substring(start, end);
    }

    private boolean isEmptyOrCommentLine(@NotNull String line) {
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t') continue;
            return c == '#';
        }
        return true;
    }

    @NotNull
    private String extractComment(@NotNull String line) {
        int len = line.length();
        for (int i = 0; i < len; i++) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t') continue;
            if (c == '#') {
                return line.substring(i + 1);
            }
            break;
        }
        return "";
    }

    @Nullable
    private String extractInlineComment(@NotNull String value) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '#' && !inSingleQuote && !inDoubleQuote && i > 0 && value.charAt(i - 1) == ' ') {
                return value.substring(i + 1);
            }
        }

        return null;
    }

    @NotNull
    private String removeInlineComment(@NotNull String value) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '#' && !inSingleQuote && !inDoubleQuote && i > 0 && value.charAt(i - 1) == ' ') {
                return value.substring(0, i - 1).trim();
            }
        }

        return value;
    }

    private void validateStrict(@NotNull List<ParseIssue> issues) {
        Set<String> seenKeys = new HashSet<>();
        int[] indentStack = new int[100];
        int stackDepth = 0;
        indentStack[0] = 0;

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];
            
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            if (line.contains("\t")) {
                int tabCol = line.indexOf('\t') + 1;
                issues.add(ParseIssue.error(
                        "Tab character used for indentation (use spaces only)",
                        lineNum + 1, tabCol, lines));
            }

            int indent = getIndentation(line);
            String trimmed = line.trim();

            if (trimmed.startsWith("-")) {
                continue;
            }

            Matcher keyMatcher = KEY_PATTERN.matcher(trimmed);
            if (keyMatcher.matches()) {
                String quoteChar = keyMatcher.group(1);
                String key = keyMatcher.group(2);
                String value = keyMatcher.group(3);

                if (!quoteChar.isEmpty()) {
                    if ((quoteChar.equals("'") && !trimmed.contains("':")) ||
                        (quoteChar.equals("\"") && !trimmed.contains("\":"))) {
                        issues.add(ParseIssue.error(
                                "Unclosed quote in key: " + quoteChar + key,
                                lineNum + 1, indent + 1, lines));
                    }
                }

                if (indent == 0) {
                    String fullKey = key;
                    if (seenKeys.contains(fullKey)) {
                        issues.add(ParseIssue.warning(
                                "Duplicate key at root level: '" + key + "'",
                                lineNum + 1, indent + 1, lines));
                    }
                    seenKeys.add(fullKey);
                }

                if (value.startsWith("'") || value.startsWith("\"")) {
                    char quote = value.charAt(0);
                    String rest = value.substring(1);
                    if (!rest.contains(String.valueOf(quote))) {
                        issues.add(ParseIssue.error(
                                "Unclosed " + (quote == '\'' ? "single" : "double") + " quote in value",
                                lineNum + 1, indent + key.length() + 3, lines));
                    }
                }
            } else if (trimmed.contains(":") && !trimmed.startsWith("[") && !trimmed.startsWith("{")) {
            } else if (!trimmed.startsWith("|") && !trimmed.startsWith(">") &&
                       !trimmed.startsWith("[") && !trimmed.startsWith("{") &&
                       !trimmed.startsWith("*") && !trimmed.startsWith("&") &&
                       !trimmed.startsWith("---") && !trimmed.startsWith("...")) {
                boolean isListContinuation = false;
                for (int prev = lineNum - 1; prev >= 0; prev--) {
                    String prevLine = lines[prev].trim();
                    if (prevLine.isEmpty() || prevLine.startsWith("#")) continue;
                    if (prevLine.endsWith("|") || prevLine.endsWith(">") || 
                        prevLine.endsWith("|+") || prevLine.endsWith(">-") ||
                        prevLine.endsWith("|-") || prevLine.endsWith(">+")) {
                        isListContinuation = true;
                    }
                    break;
                }
                
                if (!isListContinuation) {
                    issues.add(ParseIssue.error(
                            "Invalid YAML syntax - expected key:value or list item",
                            lineNum + 1, indent + 1, lines));
                }
            }
        }
    }

    private static class ParsedKey {
        final @NotNull String key;
        final @NotNull ScalarStyle keyStyle;
        final @NotNull String value;
        final @Nullable String inlineComment;

        ParsedKey(@NotNull String key, @NotNull ScalarStyle keyStyle, @NotNull String value, @Nullable String inlineComment) {
            this.key = key;
            this.keyStyle = keyStyle;
            this.value = value;
            this.inlineComment = inlineComment;
        }
    }
}
