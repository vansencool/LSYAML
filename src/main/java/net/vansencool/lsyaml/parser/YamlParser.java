package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.exceptions.YamlParseException;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.util.FlowContent;
import net.vansencool.lsyaml.parser.util.ParsedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YAML parser that preserves all formatting, comments, and metadata.
 * Uses helper classes for modular parsing.
 */
public class YamlParser {

    private ParseContext ctx;
    private ScalarParser scalarParser;
    private FlowParser flowParser;

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
     * @param yaml    the YAML content
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
     * @param yaml    the YAML content
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
            int line = ctx != null ? ctx.currentLine() + 1 : 1;
            String[] lines = ctx != null ? ctx.lines() : new String[0];
            issues.add(ParseIssue.error(e.getMessage() != null ? e.getMessage() : "Unknown error",
                    line, 1, lines));
            return ParseResult.failure(issues);
        }
    }

    @NotNull
    private YamlNode parseInternal(@NotNull String yaml, @NotNull ParseOptions options,
                                   @NotNull List<ParseIssue> issues) {
        int yamlLen = yaml.length();
        if (yamlLen == 0) {
            return new MapNode();
        }

        boolean allWhitespace = true;
        for (int i = 0; i < yamlLen; i++) {
            char c = yaml.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                allWhitespace = false;
                break;
            }
        }
        if (allWhitespace) {
            return new MapNode();
        }

        this.ctx = new ParseContext(yaml);
        this.scalarParser = new ScalarParser();
        this.flowParser = new FlowParser(scalarParser);

        if (options.isStrict()) {
            StrictValidator validator = new StrictValidator(ctx);
            validator.validate(issues);
        }

        List<String> pendingComments = new ArrayList<>();
        int pendingEmptyLines = 0;

        while (ctx.hasMoreLines() && ParserUtils.isEmptyOrComment(ctx.currentFirstChar())) {
            char first = ctx.currentFirstChar();
            if (first == 0) {
                pendingEmptyLines++;
            } else {
                pendingComments.add(ParserUtils.extractComment(ctx.currentLineContent()));
            }
            ctx.advanceLine();
        }

        if (!ctx.hasMoreLines()) {
            MapNode empty = new MapNode();
            empty.setCommentsBefore(pendingComments);
            empty.setEmptyLinesBefore(pendingEmptyLines);
            return empty;
        }

        // determine document type from first content character
        char firstChar = ctx.currentFirstChar();

        YamlNode result;
        if (firstChar == '-') {
            // root is a list
            result = parseList(0, pendingComments, pendingEmptyLines);
        } else if (firstChar == '{') {
            // root is a flow map
            result = flowParser.parseFlowMap(ctx);
            if (!pendingComments.isEmpty()) {
                result.setCommentsBefore(pendingComments);
            }
            if (pendingEmptyLines > 0) {
                result.setEmptyLinesBefore(pendingEmptyLines);
            }
        } else if (firstChar == '[') {
            // root is a flow list
            result = flowParser.parseFlowList(ctx);
            if (!pendingComments.isEmpty()) {
                result.setCommentsBefore(pendingComments);
            }
            if (pendingEmptyLines > 0) {
                result.setEmptyLinesBefore(pendingEmptyLines);
            }
        } else {
            // root is a map (most common case)
            result = parseMap(0, pendingComments, pendingEmptyLines);
        }

        Map<String, YamlNode> anchors = new HashMap<>();
        collectAnchors(result, anchors);
        // resolve merge keys (<<) to their target maps
        resolveMergeKeys(result, anchors);

        return result;
    }

    /**
     * Recursively collects all anchors from the node tree.
     */
    private void collectAnchors(@NotNull YamlNode node, @NotNull Map<String, YamlNode> anchors) {
        String anchor = node.getMetadata().getAnchor();
        if (anchor != null && !anchor.isEmpty()) {
            anchors.put(anchor, node);
        }
        if (node instanceof MapNode mapNode) {
            for (MapNode.MapEntry entry : mapNode.entries()) {
                collectAnchors(entry.getValue(), anchors);
            }
        } else if (node instanceof ListNode listNode) {
            for (YamlNode item : listNode) {
                collectAnchors(item, anchors);
            }
        }
    }

    /**
     * Resolves merge keys (<<) to point to their anchor targets.
     */
    private void resolveMergeKeys(@NotNull YamlNode node, @NotNull Map<String, YamlNode> anchors) {
        if (node instanceof MapNode mapNode) {
            // look for merge key entry
            MapNode.MapEntry mergeEntry = mapNode.getEntry("<<");
            if (mergeEntry != null && mergeEntry.getValue().getMetadata().isAlias()) {
                // resolve the alias to its target
                String aliasName = mergeEntry.getValue().getMetadata().getAlias();
                YamlNode target = anchors.get(aliasName);
                if (target instanceof MapNode resolvedMap) {
                    mergeEntry.setResolvedMergeMap(resolvedMap);
                }
            }
            // recurse into children
            for (MapNode.MapEntry entry : mapNode.entries()) {
                resolveMergeKeys(entry.getValue(), anchors);
            }
        } else if (node instanceof ListNode listNode) {
            for (YamlNode item : listNode) {
                resolveMergeKeys(item, anchors);
            }
        }
    }

    @NotNull
    private MapNode parseMap(int expectedIndent) {
        return parseMap(expectedIndent, new ArrayList<>(), 0);
    }

    /**
     * Parses a block-style map starting at the expected indentation.
     */
    @NotNull
    private MapNode parseMap(int expectedIndent, @NotNull List<String> initialComments, int initialEmptyLines) {
        MapNode map = new MapNode();
        map.getMetadata().setLine(ctx.currentLine() + 1);
        map.getMetadata().setIndentation(expectedIndent);

        // track comments and empty lines to attach to next entry
        List<String> pendingComments = new ArrayList<>(initialComments);
        int pendingEmptyLines = initialEmptyLines;

        while (ctx.hasMoreLines()) {
            char firstChar = ctx.currentFirstChar();

            // collect empty lines
            if (firstChar == 0) {
                pendingEmptyLines++;
                ctx.advanceLine();
                continue;
            }

            int indent = ctx.currentIndent();

            // collect comments at or above expected indent
            if (firstChar == '#') {
                if (indent < expectedIndent) {
                    break; // comment belongs to parent scope
                }
                pendingComments.add(ParserUtils.extractComment(ctx.currentLineContent()));
                ctx.advanceLine();
                continue;
            }

            // dedent means end of this map
            if (indent < expectedIndent) {
                if (!pendingComments.isEmpty() || pendingEmptyLines > 0) {
                    map.setTrailingComments(pendingComments);
                    map.setTrailingEmptyLines(pendingEmptyLines);
                }
                break;
            }

            // list item starts new context
            if (firstChar == '-') {
                if (!pendingComments.isEmpty() || pendingEmptyLines > 0) {
                    map.setTrailingComments(pendingComments);
                    map.setTrailingEmptyLines(pendingEmptyLines);
                }
                break;
            }

            // handle complex key (? key indicator)
            if (firstChar == '?') {
                MapNode.MapEntry entry = parseComplexKeyEntry(pendingComments, pendingEmptyLines);
                if (entry != null) {
                    map.putEntry(entry);
                    pendingComments = new ArrayList<>();
                    pendingEmptyLines = 0;
                    if (map.size() == 1) {
                        expectedIndent = indent;
                    }
                    continue;
                }
            }

            // bare colon without key - invalid
            if (firstChar == ':') {
                break;
            }

            // parse key: value line
            ParsedKey parsed = ParserUtils.parseKeyLine(ctx.currentLineContent());
            if (parsed == null) {
                break; // not a valid key line
            }

            // create entry with parsed key
            MapNode.MapEntry entry = new MapNode.MapEntry(parsed.key(), new ScalarNode(null), parsed.keyStyle());
            entry.setCommentsBefore(pendingComments);
            entry.setEmptyLinesBefore(pendingEmptyLines);
            pendingComments = new ArrayList<>();
            pendingEmptyLines = 0;

            if (parsed.inlineComment() != null) {
                entry.setInlineComment(parsed.inlineComment());
            }

            ctx.advanceLine();

            // handle empty value - look for nested content
            if (parsed.value().isEmpty()) {
                if (ctx.hasMoreLines()) {
                    // collect any comments/empty lines before nested content
                    List<String> nestedComments = new ArrayList<>();
                    int nestedEmptyLines = 0;

                    while (ctx.hasMoreLines()) {
                        char nextFirst = ctx.currentFirstChar();

                        if (nextFirst == 0) {
                            nestedEmptyLines++;
                            ctx.advanceLine();
                            continue;
                        }
                        if (nextFirst == '#') {
                            nestedComments.add(ParserUtils.extractComment(ctx.currentLineContent()));
                            ctx.advanceLine();
                            continue;
                        }
                        break;
                    }

                    if (ctx.hasMoreLines()) {
                        int nextIndent = ctx.currentIndent();
                        char nextFirst = ctx.currentFirstChar();

                        if (nextIndent > indent && nextFirst != 0) {
                            if (nextFirst == '-') {
                                entry.setValue(parseList(nextIndent, nestedComments, nestedEmptyLines));
                            } else {
                                entry.setValue(parseMap(nextIndent, nestedComments, nestedEmptyLines));
                            }
                            pendingEmptyLines += entry.getValue().getTrailingEmptyLines();
                            entry.getValue().setTrailingEmptyLines(0);
                        } else {
                            entry.setValue(new ScalarNode(null));
                            if (!nestedComments.isEmpty() || nestedEmptyLines > 0) {
                                ctx.setCurrentLine(ctx.currentLine() - (nestedComments.size() + nestedEmptyLines));
                            }
                        }
                    } else {
                        entry.setValue(new ScalarNode(null));
                    }
                } else {
                    entry.setValue(new ScalarNode(null));
                }
            } else {
                // check if value is anchor-only (value follows on subsequent lines)
                String anchor = ParserUtils.extractAnchorOnly(parsed.value());
                if (anchor != null) {
                    // anchor with nested content
                    if (ctx.hasMoreLines()) {
                        List<String> nestedComments = new ArrayList<>();
                        int nestedEmptyLines = 0;

                        while (ctx.hasMoreLines()) {
                            char nextFirst = ctx.currentFirstChar();

                            if (nextFirst == 0) {
                                nestedEmptyLines++;
                                ctx.advanceLine();
                                continue;
                            }
                            if (nextFirst == '#') {
                                nestedComments.add(ParserUtils.extractComment(ctx.currentLineContent()));
                                ctx.advanceLine();
                                continue;
                            }
                            break;
                        }

                        if (ctx.hasMoreLines()) {
                            int nextIndent = ctx.currentIndent();
                            char nextFirst = ctx.currentFirstChar();

                            if (nextIndent > indent && nextFirst != 0) {
                                YamlNode nestedValue;
                                if (nextFirst == '-') {
                                    nestedValue = parseList(nextIndent, nestedComments, nestedEmptyLines);
                                } else {
                                    nestedValue = parseMap(nextIndent, nestedComments, nestedEmptyLines);
                                }
                                nestedValue.getMetadata().setAnchor(anchor);
                                entry.setValue(nestedValue);
                                pendingEmptyLines += nestedValue.getTrailingEmptyLines();
                                nestedValue.setTrailingEmptyLines(0);
                            } else {
                                ScalarNode nullNode = new ScalarNode(null);
                                nullNode.getMetadata().setAnchor(anchor);
                                entry.setValue(nullNode);
                                if (!nestedComments.isEmpty() || nestedEmptyLines > 0) {
                                    ctx.setCurrentLine(ctx.currentLine() - (nestedComments.size() + nestedEmptyLines));
                                }
                            }
                        } else {
                            ScalarNode nullNode = new ScalarNode(null);
                            nullNode.getMetadata().setAnchor(anchor);
                            entry.setValue(nullNode);
                        }
                    } else {
                        ScalarNode nullNode = new ScalarNode(null);
                        nullNode.getMetadata().setAnchor(anchor);
                        entry.setValue(nullNode);
                    }
                } else {
                    entry.setValue(parseValue(parsed.value(), indent));
                }
            }

            map.putEntry(entry);

            if (map.size() == 1) {
                expectedIndent = indent;
            }
        }

        if (!pendingComments.isEmpty() || pendingEmptyLines > 0) {
            map.setTrailingComments(pendingComments);
            map.setTrailingEmptyLines(pendingEmptyLines);
        }

        return map;
    }

    /**
     * Parses a complex key entry (? key : value syntax).
     */
    @Nullable
    private MapNode.MapEntry parseComplexKeyEntry(@NotNull List<String> pendingComments, int pendingEmptyLines) {
        String line = ctx.currentLineContent();
        int indent = ctx.currentIndent();
        String trimmed = line.substring(indent);

        if (trimmed.charAt(0) != '?') {
            return null;
        }

        ctx.advanceLine();

        // parse the complex key itself
        YamlNode complexKey;
        String keyContent = trimmed.length() > 1 ? trimmed.substring(1).trim() : "";

        if (!keyContent.isEmpty()) {
            // inline complex key content
            if (keyContent.charAt(0) == '{') {
                complexKey = flowParser.parseFlowMapFromStringNested(keyContent);
            } else if (keyContent.charAt(0) == '[') {
                complexKey = flowParser.parseFlowListFromStringNested(keyContent);
            } else if (ParserUtils.containsUnquotedColon(keyContent)) {
                // key is itself a map - rewrite line and parse
                ctx.setCurrentLine(ctx.currentLine() - 1);
                int mapIndent = indent + 2;
                String originalLine = ctx.lines()[ctx.currentLine()];
                ctx.lines()[ctx.currentLine()] = ParserUtils.spaces(mapIndent) + keyContent;
                ctx.lineIndents()[ctx.currentLine()] = mapIndent;
                ctx.lineFirstChars()[ctx.currentLine()] = keyContent.charAt(0);
                complexKey = parseMap(mapIndent);
                ctx.lines()[ctx.currentLine() > 0 ? ctx.currentLine() - 1 : 0] = originalLine;
            } else {
                complexKey = scalarParser.parseScalar(keyContent);
            }
        } else {
            List<String> nestedComments = new ArrayList<>();
            int nestedEmptyLines = 0;

            while (ctx.hasMoreLines()) {
                char nextFirst = ctx.currentFirstChar();
                if (nextFirst == 0) {
                    nestedEmptyLines++;
                    ctx.advanceLine();
                    continue;
                }
                if (nextFirst == '#') {
                    nestedComments.add(ParserUtils.extractComment(ctx.currentLineContent()));
                    ctx.advanceLine();
                    continue;
                }
                break;
            }

            if (ctx.hasMoreLines()) {
                int nextIndent = ctx.currentIndent();
                char nextFirst = ctx.currentFirstChar();

                if (nextIndent > indent && nextFirst != ':') {
                    if (nextFirst == '-') {
                        complexKey = parseList(nextIndent, nestedComments, nestedEmptyLines);
                    } else {
                        complexKey = parseMap(nextIndent, nestedComments, nestedEmptyLines);
                    }
                } else {
                    complexKey = new ScalarNode(null);
                }
            } else {
                complexKey = new ScalarNode(null);
            }
        }

        // generate string representation for the key
        String keyString = generateKeyStringFromNode(complexKey);

        // skip any comments/empty lines before the value indicator
        while (ctx.hasMoreLines()) {
            char firstChar = ctx.currentFirstChar();
            if (firstChar == 0 || firstChar == '#') {
                ctx.advanceLine();
                continue;
            }
            break;
        }

        // parse the value after : indicator
        YamlNode value = new ScalarNode(null);

        if (ctx.hasMoreLines()) {
            String valueLine = ctx.currentLineContent();
            int valueIndent = ctx.currentIndent();
            String valueTrimmed = valueLine.substring(valueIndent);

            if (valueTrimmed.startsWith(":")) {
                ctx.advanceLine();

                // check for inline value after :
                String valueContent = valueTrimmed.length() > 1 ? valueTrimmed.substring(1).trim() : "";

                if (!valueContent.isEmpty()) {
                    value = parseValue(valueContent, valueIndent);
                } else {
                    // look for nested value on following lines
                    List<String> nestedComments = new ArrayList<>();
                    int nestedEmptyLines = 0;

                    while (ctx.hasMoreLines()) {
                        char nextFirst = ctx.currentFirstChar();
                        if (nextFirst == 0) {
                            nestedEmptyLines++;
                            ctx.advanceLine();
                            continue;
                        }
                        if (nextFirst == '#') {
                            nestedComments.add(ParserUtils.extractComment(ctx.currentLineContent()));
                            ctx.advanceLine();
                            continue;
                        }
                        break;
                    }

                    if (ctx.hasMoreLines()) {
                        int nextIndent = ctx.currentIndent();
                        char nextFirst = ctx.currentFirstChar();

                        if (nextIndent > indent) {
                            if (nextFirst == '-') {
                                value = parseList(nextIndent, nestedComments, nestedEmptyLines);
                            } else {
                                value = parseMap(nextIndent, nestedComments, nestedEmptyLines);
                            }
                        }
                    }
                }
            }
        }

        MapNode.MapEntry entry = new MapNode.MapEntry(keyString, value);
        entry.setComplexKey(complexKey);
        entry.setCommentsBefore(pendingComments);
        entry.setEmptyLinesBefore(pendingEmptyLines);
        return entry;
    }

    /**
     * Generates a string representation of a complex key node.
     * Used for map lookups when the key is a map or list.
     */
    @NotNull
    private String generateKeyStringFromNode(@NotNull YamlNode node) {
        if (node instanceof ScalarNode scalar) {
            Object val = scalar.getValue();
            return val != null ? val.toString() : "";
        } else if (node instanceof MapNode mapNode) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (MapNode.MapEntry e : mapNode.entries()) {
                if (!first) sb.append(", ");
                sb.append(e.getKey()).append(": ").append(generateKeyStringFromNode(e.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } else if (node instanceof ListNode listNode) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (YamlNode item : listNode) {
                if (!first) sb.append(", ");
                sb.append(generateKeyStringFromNode(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        return "";
    }

    @NotNull
    private ListNode parseList(int expectedIndent) {
        return parseList(expectedIndent, new ArrayList<>(), 0);
    }

    /**
     * Parses a block-style list starting at the expected indentation.
     */
    @NotNull
    private ListNode parseList(int expectedIndent, @NotNull List<String> initialComments, int initialEmptyLines) {
        ListNode list = new ListNode();
        list.getMetadata().setLine(ctx.currentLine() + 1);
        list.getMetadata().setIndentation(expectedIndent);

        // track comments and empty lines to attach to next entry
        List<String> pendingComments = new ArrayList<>(initialComments);
        int pendingEmptyLines = initialEmptyLines;

        while (ctx.hasMoreLines()) {
            String line = ctx.currentLineContent();
            char firstChar = ctx.currentFirstChar();

            // collect empty lines
            if (firstChar == 0) {
                pendingEmptyLines++;
                ctx.advanceLine();
                continue;
            }

            int indent = ctx.currentIndent();

            // collect comments at or above expected indent
            if (firstChar == '#') {
                if (indent < expectedIndent) {
                    break; // comment belongs to parent scope
                }
                pendingComments.add(ParserUtils.extractComment(line));
                ctx.advanceLine();
                continue;
            }

            // dedent means end of this list
            if (indent < expectedIndent) {
                if (!pendingComments.isEmpty() || pendingEmptyLines > 0) {
                    list.setTrailingComments(pendingComments);
                    list.setTrailingEmptyLines(pendingEmptyLines);
                }
                break;
            }

            // non-dash means end of list
            if (firstChar != '-') {
                if (!pendingComments.isEmpty() || pendingEmptyLines > 0) {
                    list.setTrailingComments(pendingComments);
                    list.setTrailingEmptyLines(pendingEmptyLines);
                }
                break;
            }

            // validate list item syntax (- followed by space)
            String trimmed = line.substring(indent);
            if (trimmed.length() <= 1 || (trimmed.charAt(1) != ' ' && trimmed.charAt(1) != '\t')) {
                if (trimmed.length() > 1 && trimmed.charAt(1) != ' ') {
                    if (!pendingComments.isEmpty() || pendingEmptyLines > 0) {
                        list.setTrailingComments(pendingComments);
                        list.setTrailingEmptyLines(pendingEmptyLines);
                    }
                    break; // not a valid list item
                }
            }

            // create new list entry
            ListNode.ListEntry entry = new ListNode.ListEntry(new ScalarNode(null));
            entry.setCommentsBefore(pendingComments);
            entry.setEmptyLinesBefore(pendingEmptyLines);
            pendingComments = new ArrayList<>();
            pendingEmptyLines = 0;

            // extract value after dash
            String valueStr = trimmed.length() > 1 ? ParserUtils.trimAfterDash(trimmed) : "";
            String inlineComment = ParserUtils.extractInlineComment(valueStr);
            if (inlineComment != null) {
                entry.setInlineComment(inlineComment);
                valueStr = ParserUtils.removeInlineComment(valueStr);
            }

            ctx.advanceLine();

            // handle different value types
            if (valueStr.isEmpty()) {
                // empty value - look for nested content
                if (ctx.hasMoreLines()) {
                    List<String> nestedComments = new ArrayList<>();
                    int nestedEmptyLines = 0;

                    while (ctx.hasMoreLines()) {
                        char nextFirst = ctx.currentFirstChar();

                        if (nextFirst == 0) {
                            nestedEmptyLines++;
                            ctx.advanceLine();
                            continue;
                        }
                        if (nextFirst == '#') {
                            nestedComments.add(ParserUtils.extractComment(ctx.currentLineContent()));
                            ctx.advanceLine();
                            continue;
                        }
                        break;
                    }

                    if (ctx.hasMoreLines()) {
                        int nextIndent = ctx.currentIndent();
                        char nextFirst = ctx.currentFirstChar();

                        if (nextIndent > indent && nextFirst != 0) {
                            if (nextFirst == '-') {
                                entry.setValue(parseList(nextIndent, nestedComments, nestedEmptyLines));
                            } else {
                                entry.setValue(parseMap(nextIndent, nestedComments, nestedEmptyLines));
                            }
                            pendingEmptyLines += entry.getValue().getTrailingEmptyLines();
                            entry.getValue().setTrailingEmptyLines(0);
                        } else {
                            entry.setValue(new ScalarNode(null));
                            if (!nestedComments.isEmpty() || nestedEmptyLines > 0) {
                                ctx.setCurrentLine(ctx.currentLine() - (nestedComments.size() + nestedEmptyLines));
                            }
                        }
                    } else {
                        entry.setValue(new ScalarNode(null));
                    }
                } else {
                    entry.setValue(new ScalarNode(null));
                }
            } else if (valueStr.charAt(0) == '{' || valueStr.charAt(0) == '[') {
                // inline flow collection
                entry.setValue(flowParser.parseFlowValueInline(valueStr));
            } else if (ParserUtils.containsUnquotedColon(valueStr)) {
                // inline map value - rewrite line and parse
                ctx.setCurrentLine(ctx.currentLine() - 1);
                int mapIndent = indent + 2;
                String originalLine = ctx.lines()[ctx.currentLine()];
                ctx.lines()[ctx.currentLine()] = ParserUtils.spaces(mapIndent) + valueStr;
                ctx.lineIndents()[ctx.currentLine()] = mapIndent;
                ctx.lineFirstChars()[ctx.currentLine()] = valueStr.charAt(0);
                entry.setValue(parseMap(mapIndent));
                ctx.lines()[ctx.currentLine() - 1] = originalLine;
            } else if (valueStr.charAt(0) == '-') {
                // nested list starting on same line
                ctx.setCurrentLine(ctx.currentLine() - 1);
                int listIndent = indent + 2;
                String originalLine = ctx.lines()[ctx.currentLine()];
                ctx.lines()[ctx.currentLine()] = ParserUtils.spaces(listIndent) + valueStr;
                ctx.lineIndents()[ctx.currentLine()] = listIndent;
                ctx.lineFirstChars()[ctx.currentLine()] = '-';
                entry.setValue(parseList(listIndent));
                ctx.lines()[ctx.currentLine() - 1] = originalLine;
            } else {
                // scalar value
                entry.setValue(parseValue(valueStr, indent));
            }

            list.addEntry(entry);

            if (list.size() == 1) {
                expectedIndent = indent;
            }
        }

        if (!pendingComments.isEmpty() || pendingEmptyLines > 0) {
            list.setTrailingComments(pendingComments);
            list.setTrailingEmptyLines(pendingEmptyLines);
        }

        return list;
    }

    /**
     * Parses a value string into the appropriate node type.
     * Handles aliases, flow collections, block scalars, anchors, and tags.
     */
    @NotNull
    private YamlNode parseValue(@NotNull String value, int indent) {
        value = ParserUtils.trimWhitespace(value);

        if (value.isEmpty()) {
            return new ScalarNode(null);
        }

        // check for alias (*name)
        if (value.charAt(0) == '*') {
            String alias = ParserUtils.extractAlias(value);
            if (alias != null) {
                ScalarNode node = new ScalarNode(null);
                node.getMetadata().setAlias(alias);
                return node;
            }
        }

        // check for flow map
        if (value.charAt(0) == '{') {
            FlowContent flowContent = flowParser.collectMultiLineFlow(ctx, value, '{', '}');
            MapNode flowMap = flowParser.parseFlowMapFromStringNested(flowContent.content());
            if (flowContent.multiLine()) {
                flowMap.setMultiLineFlow(true);
                flowMap.setFlowIndent(flowContent.indent());
            }
            return flowMap;
        }

        // check for flow list
        if (value.charAt(0) == '[') {
            FlowContent flowContent = flowParser.collectMultiLineFlow(ctx, value, '[', ']');
            ListNode flowList = flowParser.parseFlowListFromStringNested(flowContent.content());
            if (flowContent.multiLine()) {
                flowList.setMultiLineFlow(true);
                flowList.setFlowIndent(flowContent.indent());
            }
            return flowList;
        }

        // check for block scalar (| or >)
        if (value.charAt(0) == '|' || value.charAt(0) == '>') {
            return scalarParser.parseBlockScalar(ctx, value, indent);
        }

        // check for anchor (&name)
        String anchor = null;
        if (value.charAt(0) == '&') {
            int anchorEnd = 1;
            while (anchorEnd < value.length() && ParserUtils.isWordChar(value.charAt(anchorEnd))) {
                anchorEnd++;
            }
            if (anchorEnd > 1) {
                anchor = value.substring(1, anchorEnd);
                // skip anchor and trailing whitespace
                while (anchorEnd < value.length() && value.charAt(anchorEnd) == ' ') {
                    anchorEnd++;
                }
                value = value.substring(anchorEnd);
            }
        }

        // check for tag (!tag)
        String tag = null;
        if (!value.isEmpty() && value.charAt(0) == '!') {
            int tagEnd = 1;
            while (tagEnd < value.length() && value.charAt(tagEnd) != ' ' && value.charAt(tagEnd) != '\t') {
                tagEnd++;
            }
            tag = value.substring(0, tagEnd);
            // skip tag and trailing whitespace
            while (tagEnd < value.length() && (value.charAt(tagEnd) == ' ' || value.charAt(tagEnd) == '\t')) {
                tagEnd++;
            }
            value = value.substring(tagEnd);
        }

        // parse the actual scalar value
        ScalarNode scalar = scalarParser.parseScalar(value);
        if (anchor != null) {
            scalar.getMetadata().setAnchor(anchor);
        }
        if (tag != null) {
            scalar.setTag(tag);
        }

        return scalar;
    }
}
