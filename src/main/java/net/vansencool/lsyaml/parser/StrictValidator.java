package net.vansencool.lsyaml.parser;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates YAML for strict parsing rules.
 * <p>
 * Performs validation checks such as:
 * <ul>
 *   <li>No tab characters for indentation</li>
 *   <li>Consistent indentation levels</li>
 *   <li>No duplicate keys</li>
 *   <li>Proper key/value syntax</li>
 * </ul>
 */
public class StrictValidator {

    @NotNull
    private final ParseContext ctx;

    /**
     * Creates a validator for the given parse context.
     *
     * @param ctx the parse context containing the YAML to validate
     */
    public StrictValidator(@NotNull ParseContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Validates the YAML and adds any issues to the provided list.
     *
     * @param issues the list to add validation issues to
     */
    public void validate(@NotNull List<ParseIssue> issues) {
        Set<String> seenKeys = new HashSet<>(); // tracks root-level keys for duplicate detection
        int[] indentStack = new int[100]; // stack of indentation levels
        int stackDepth = 0;
        indentStack[0] = 0;
        int flowDepth = 0; // tracks nesting depth inside flow collections

        String[] lines = ctx.lines();
        int[] lineIndents = ctx.lineIndents();
        char[] lineFirstChars = ctx.lineFirstChars();
        int totalLines = ctx.totalLines();

        for (int lineNum = 0; lineNum < totalLines; lineNum++) {
            String line = lines[lineNum];
            char firstChar = lineFirstChars[lineNum];

            // skip empty lines and comments
            if (firstChar == 0 || firstChar == '#') {
                continue;
            }

            // check for tab characters - YAML requires spaces
            if (line.indexOf('\t') >= 0) {
                int tabCol = line.indexOf('\t') + 1;
                issues.add(ParseIssue.error(
                        "Tab character used for indentation (use spaces only)",
                        lineNum + 1, tabCol, lines));
            }

            int indent = lineIndents[lineNum];
            String trimmed = line.substring(indent);

            // track flow depth to skip validation inside flow collections
            int prevFlowDepth = flowDepth;
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (c == '{' || c == '[') flowDepth++;
                else if (c == '}' || c == ']') flowDepth--;
            }

            // skip lines inside flow collections - they have different syntax rules
            if (prevFlowDepth > 0 || flowDepth > 0 || firstChar == '}' || firstChar == ']') {
                continue;
            }

            // handle list items
            if (firstChar == '-') {
                int contentIndent = indent + 2;

                if (trimmed.length() > 1 && trimmed.charAt(1) == ' ') {
                    int colonIdx = ParserUtils.findUnquotedColon(trimmed);
                    if (colonIdx > 2) {
                        contentIndent = indent + 2;
                    }
                }

                // adjust indent stack based on list item position
                if (indent > indentStack[stackDepth]) {
                    stackDepth++;
                    indentStack[stackDepth] = indent;
                } else {
                    // pop stack until we find matching indent level
                    while (stackDepth > 0 && indent < indentStack[stackDepth]) {
                        stackDepth--;
                    }
                }

                // if list item has inline map, push content indent
                if (trimmed.length() > 2 && ParserUtils.findUnquotedColon(trimmed) > 2) {
                    stackDepth++;
                    indentStack[stackDepth] = contentIndent;
                }
                continue;
            }

            // handle key:value pairs
            int colonIdx = ParserUtils.findUnquotedColon(trimmed);
            if (colonIdx > 0) {
                String keyPart = trimmed.substring(0, colonIdx);
                String valuePart = colonIdx + 1 < trimmed.length() ? trimmed.substring(colonIdx + 1) : "";

                // extract the actual key, trimming whitespace
                int keyStart = 0;
                while (keyStart < keyPart.length() && keyPart.charAt(keyStart) == ' ') keyStart++;
                int keyEnd = keyPart.length();
                while (keyEnd > keyStart && keyPart.charAt(keyEnd - 1) == ' ') keyEnd--;
                String key = keyPart.substring(keyStart, keyEnd);

                // handle quoted keys
                char quoteChar = 0;
                if (!key.isEmpty() && (key.charAt(0) == '\'' || key.charAt(0) == '"')) {
                    quoteChar = key.charAt(0);
                    if (key.length() >= 2 && key.charAt(key.length() - 1) == quoteChar) {
                        key = key.substring(1, key.length() - 1);
                    }
                }

                // validate indentation alignment
                if (indent > indentStack[stackDepth]) {
                    stackDepth++;
                    indentStack[stackDepth] = indent;
                } else {
                    // pop stack to find matching level
                    while (stackDepth > 0 && indent < indentStack[stackDepth]) {
                        stackDepth--;
                    }
                    // indent must match a previous level exactly
                    if (indent != indentStack[stackDepth]) {
                        issues.add(ParseIssue.error(
                                "Indentation mismatch - does not align with any previous level",
                                lineNum + 1, indent + 1, lines));
                    }
                }

                // validate quoted key syntax
                if (quoteChar != 0) {
                    String searchFor = quoteChar + ":";
                    if (!trimmed.contains(searchFor)) {
                        issues.add(ParseIssue.error(
                                "Unclosed quote in key: " + quoteChar + key,
                                lineNum + 1, indent + 1, lines));
                    }
                }

                // check for duplicate root-level keys
                if (indent == 0) {
                    if (seenKeys.contains(key)) {
                        issues.add(ParseIssue.warning(
                                "Duplicate key at root level: '" + key + "'",
                                lineNum + 1, indent + 1, lines));
                    }
                    seenKeys.add(key);
                }

                // validate quoted value syntax
                int valStart = 0;
                while (valStart < valuePart.length() && valuePart.charAt(valStart) == ' ') valStart++;
                if (valStart < valuePart.length()) {
                    char valFirst = valuePart.charAt(valStart);
                    // check for unclosed quotes in values
                    if (valFirst == '\'' || valFirst == '"') {
                        String rest = valuePart.substring(valStart + 1);
                        if (rest.indexOf(valFirst) < 0) {
                            issues.add(ParseIssue.error(
                                    "Unclosed " + (valFirst == '\'' ? "single" : "double") + " quote in value",
                                    lineNum + 1, indent + colonIdx + 2, lines));
                        }
                    }
                }
            } else if (firstChar != '|' && firstChar != '>' &&
                    firstChar != '[' && firstChar != '{' &&
                    firstChar != '*' && firstChar != '&' &&
                    firstChar != '?' && firstChar != ':' &&
                    !(trimmed.startsWith("---")) && !(trimmed.startsWith("..."))) {
                // line doesn't look like valid YAML - check if it's block scalar content
                boolean isBlockScalarContent = false;
                for (int prev = lineNum - 1; prev >= 0; prev--) {
                    char prevFirst = lineFirstChars[prev];
                    if (prevFirst == 0 || prevFirst == '#') continue;

                    int prevIndent = lineIndents[prev];
                    if (prevIndent < indent) {
                        String prevTrimmed = lines[prev].substring(prevIndent);
                        if (prevTrimmed.endsWith("|") || prevTrimmed.endsWith(">") ||
                                prevTrimmed.endsWith("|+") || prevTrimmed.endsWith(">-") ||
                                prevTrimmed.endsWith("|-") || prevTrimmed.endsWith(">+")) {
                            isBlockScalarContent = true;
                        }
                        break;
                    }
                }

                // check if it's continuation of a multi-line flow collection
                boolean isFlowContent = false;
                if (!isBlockScalarContent) {
                    for (int prev = lineNum - 1; prev >= 0; prev--) {
                        char prevFirst = lineFirstChars[prev];
                        if (prevFirst == 0 || prevFirst == '#') continue;
                        String prevTrimmed = lines[prev].substring(lineIndents[prev]);
                        if (prevTrimmed.endsWith("{") || prevTrimmed.endsWith("[") ||
                                prevTrimmed.endsWith(",")) {
                            isFlowContent = true;
                            break;
                        }
                        break;
                    }
                }

                if (!isBlockScalarContent && !isFlowContent) {
                    issues.add(ParseIssue.error(
                            "Invalid YAML syntax - expected key:value or list item",
                            lineNum + 1, indent + 1, lines));
                }
            }
        }
    }
}
