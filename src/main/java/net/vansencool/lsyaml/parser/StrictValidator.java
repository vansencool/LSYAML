package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.parser.source.LineIndex;
import net.vansencool.lsyaml.parser.source.Source;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates YAML for strict parsing rules .
 */
public final class StrictValidator {

    private final @NotNull Source source;
    private final @NotNull LineIndex lines;

    /**
     * Creates a validator over the given source and line index.
     */
    public StrictValidator(@NotNull Source source, @NotNull LineIndex lines) {
        this.source = source;
        this.lines = lines;
    }

    /**
     * Validates the source and adds any issues to the given list.
     */
    public void validate(@NotNull List<ParseIssue> issues) {
        String[] text = materializeLines();
        Set<String> seenKeys = new HashSet<>();
        int[] indentStack = new int[100];
        int stackDepth = 0;
        indentStack[0] = 0;
        int flowDepth = 0;
        int totalLines = lines.count();

        for (int lineNum = 0; lineNum < totalLines; lineNum++) {
            String line = text[lineNum];
            char firstChar = lines.firstChar(lineNum);

            if (firstChar == 0 || firstChar == '#') {
                continue;
            }

            if (line.indexOf('\t') >= 0) {
                issues.add(ParseIssue.error("Tab character used for indentation (use spaces only)", lineNum + 1, line.indexOf('\t') + 1, text));
            }

            int indent = lines.indent(lineNum);
            String trimmed = line.substring(indent);

            int prevFlowDepth = flowDepth;
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                if (c == '{' || c == '[') {
                    flowDepth++;
                } else if (c == '}' || c == ']') {
                    flowDepth--;
                }
            }

            if (prevFlowDepth > 0 || flowDepth > 0 || firstChar == '}' || firstChar == ']') {
                continue;
            }

            if (firstChar == '-') {
                int contentIndent = indent + 2;
                if (trimmed.length() > 1 && trimmed.charAt(1) == ' ' && findUnquotedColon(trimmed) > 2) {
                    contentIndent = indent + 2;
                }
                if (indent > indentStack[stackDepth]) {
                    indentStack[++stackDepth] = indent;
                } else {
                    while (stackDepth > 0 && indent < indentStack[stackDepth]) {
                        stackDepth--;
                    }
                }
                if (trimmed.length() > 2 && findUnquotedColon(trimmed) > 2) {
                    indentStack[++stackDepth] = contentIndent;
                }
                continue;
            }

            int colonIdx = findUnquotedColon(trimmed);
            if (colonIdx > 0) {
                String keyPart = trimmed.substring(0, colonIdx);
                String valuePart = colonIdx + 1 < trimmed.length() ? trimmed.substring(colonIdx + 1) : "";

                int keyStart = 0;
                while (keyStart < keyPart.length() && keyPart.charAt(keyStart) == ' ') {
                    keyStart++;
                }
                int keyEnd = keyPart.length();
                while (keyEnd > keyStart && keyPart.charAt(keyEnd - 1) == ' ') {
                    keyEnd--;
                }
                String key = keyPart.substring(keyStart, keyEnd);

                char quoteChar = 0;
                if (!key.isEmpty() && (key.charAt(0) == '\'' || key.charAt(0) == '"')) {
                    quoteChar = key.charAt(0);
                    if (key.length() >= 2 && key.charAt(key.length() - 1) == quoteChar) {
                        key = key.substring(1, key.length() - 1);
                    }
                }

                if (indent > indentStack[stackDepth]) {
                    indentStack[++stackDepth] = indent;
                } else {
                    while (stackDepth > 0 && indent < indentStack[stackDepth]) {
                        stackDepth--;
                    }
                    if (indent != indentStack[stackDepth]) {
                        issues.add(ParseIssue.error("Indentation mismatch, does not align with any previous level", lineNum + 1, indent + 1, text));
                    }
                }

                if (quoteChar != 0 && !trimmed.contains(quoteChar + ":")) {
                    issues.add(ParseIssue.error("Unclosed quote in key: " + quoteChar + key, lineNum + 1, indent + 1, text));
                }

                if (indent == 0) {
                    if (seenKeys.contains(key)) {
                        issues.add(ParseIssue.warning("Duplicate key at root level: '" + key + "'", lineNum + 1, indent + 1, text));
                    }
                    seenKeys.add(key);
                }

                int valStart = 0;
                while (valStart < valuePart.length() && valuePart.charAt(valStart) == ' ') {
                    valStart++;
                }
                if (valStart < valuePart.length()) {
                    char valFirst = valuePart.charAt(valStart);
                    if ((valFirst == '\'' || valFirst == '"') && valuePart.substring(valStart + 1).indexOf(valFirst) < 0) {
                        issues.add(ParseIssue.error("Unclosed " + (valFirst == '\'' ? "single" : "double") + " quote in value", lineNum + 1, indent + colonIdx + 2, text));
                    }
                }
            } else if (firstChar != '|' && firstChar != '>' && firstChar != '[' && firstChar != '{' && firstChar != '*' && firstChar != '&' && firstChar != '?' && firstChar != ':' && !trimmed.startsWith("---") && !trimmed.startsWith("...")) {
                if (!isBlockScalarContent(text, lineNum, indent) && !isFlowContent(text, lineNum)) {
                    issues.add(ParseIssue.error("Invalid YAML syntax, expected key:value or list item", lineNum + 1, indent + 1, text));
                }
            }
        }
    }

    private boolean isBlockScalarContent(@NotNull String[] text, int lineNum, int indent) {
        for (int prev = lineNum - 1; prev >= 0; prev--) {
            char prevFirst = lines.firstChar(prev);
            if (prevFirst == 0 || prevFirst == '#') {
                continue;
            }
            int prevIndent = lines.indent(prev);
            if (prevIndent < indent) {
                String prevTrimmed = text[prev].substring(prevIndent);
                return prevTrimmed.endsWith("|") || prevTrimmed.endsWith(">") || prevTrimmed.endsWith("|+") || prevTrimmed.endsWith(">-") || prevTrimmed.endsWith("|-") || prevTrimmed.endsWith(">+");
            }
            return false;
        }
        return false;
    }

    private boolean isFlowContent(@NotNull String[] text, int lineNum) {
        for (int prev = lineNum - 1; prev >= 0; prev--) {
            char prevFirst = lines.firstChar(prev);
            if (prevFirst == 0 || prevFirst == '#') {
                continue;
            }
            String prevTrimmed = text[prev].substring(lines.indent(prev));
            return prevTrimmed.endsWith("{") || prevTrimmed.endsWith("[") || prevTrimmed.endsWith(",");
        }
        return false;
    }

    private @NotNull String[] materializeLines() {
        int count = lines.count();
        String[] text = new String[count];
        for (int i = 0; i < count; i++) {
            text[i] = source.slice(lines.start(i), lines.end(i));
        }
        return text;
    }

    private int findUnquotedColon(@NotNull String line) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
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
}
