package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.parser.diagnostic.DuplicateAnchorDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.DuplicateKeyDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.EmptyKeyDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.IndentationDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.InvalidSyntaxDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.LegacyBooleanDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.LegacyOctalDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.TabIndentDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.UnclosedQuoteDiagnostic;
import net.vansencool.lsyaml.parser.source.LineIndex;
import net.vansencool.lsyaml.parser.source.Source;
import net.vansencool.lsyaml.parser.text.Scan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks a document against the YAML 1.2 core schema.
 */
public final class StrictValidator {

    private final @NotNull Source source;
    private final @NotNull LineIndex lines;
    private final @Nullable String sourceFile;

    private String @NotNull [] text = new String[0];
    private @NotNull String fullSource = "";
    private @NotNull List<Diagnostic> out = List.of();
    private final @NotNull Map<String, int[]> seenAnchors = new HashMap<>();

    /**
     * Creates a validator over the given source and line index.
     */
    public StrictValidator(@NotNull Source source, @NotNull LineIndex lines) {
        this(source, lines, null);
    }

    /**
     * Creates a validator that labels diagnostics with a source file name.
     */
    public StrictValidator(@NotNull Source source, @NotNull LineIndex lines, @Nullable String sourceFile) {
        this.source = source;
        this.lines = lines;
        this.sourceFile = sourceFile;
    }

    /**
     * Adds one diagnostic per issue found to the given list.
     */
    public void validate(@NotNull List<Diagnostic> diagnostics) {
        this.out = diagnostics;
        this.text = materializeLines();
        this.fullSource = String.join("\n", text);
        this.seenAnchors.clear();

        Map<Integer, Map<String, Integer>> seenKeys = new HashMap<>();
        int[] indentStack = new int[256];
        int stackDepth = 0;
        int flowDepth = 0;
        int complexKeyIndent = -1;
        int totalLines = lines.count();

        for (int lineNum = 0; lineNum < totalLines; lineNum++) {
            String line = text[lineNum];
            char firstChar = lines.firstChar(lineNum);
            if (firstChar == 0 || firstChar == '#') {
                continue;
            }

            int tab = tabInIndent(line);
            if (tab >= 0) {
                int end = tab;
                while (end < line.length() && line.charAt(end) == '\t') {
                    end++;
                }
                out.add(TabIndentDiagnostic.build(sourceFile, fullSource, lineNum + 1, line, tab, end));
            }

            int indent = lines.indent(lineNum);
            String trimmed = line.substring(indent);

            int prevFlowDepth = flowDepth;
            flowDepth += flowDelta(trimmed);
            if (prevFlowDepth > 0 || flowDepth > 0 || firstChar == '}' || firstChar == ']') {
                continue;
            }

            if (firstChar == '-') {
                seenKeys.keySet().removeIf(level -> level > indent);
                stackDepth = pushOrPop(indentStack, stackDepth, indent);
                if (trimmed.length() > 2 && findUnquotedColon(trimmed) > 2) {
                    indentStack[++stackDepth] = indent + 2;
                }
                String itemValue = trimmed.length() > 1 ? trimmed.substring(1).trim() : "";
                if (!itemValue.isEmpty()) {
                    int itemColumn = line.lastIndexOf(itemValue);
                    checkAnchor(lineNum, line, itemColumn, itemValue);
                    checkScalar(lineNum, line, itemColumn, itemValue);
                }
                continue;
            }

            if (firstChar == '?') {
                complexKeyIndent = indent;
                seenKeys.keySet().removeIf(level -> level > indent);
                continue;
            }
            if (firstChar == ':' && complexKeyIndent == indent) {
                complexKeyIndent = -1;
                continue;
            }

            int colonIdx = findUnquotedColon(trimmed);
            if (colonIdx > 0) {
                seenKeys.keySet().removeIf(level -> level > indent);
                validateEntry(lineNum, line, indent, trimmed, colonIdx, indentStack, stackDepth, seenKeys);
                stackDepth = pushOrPop(indentStack, stackDepth, indent);
            } else if (firstChar == ':') {
                out.add(EmptyKeyDiagnostic.build(sourceFile, fullSource, lineNum + 1, line, indent));
            } else if (isStandaloneToken(firstChar, trimmed) && !isBlockScalarContent(lineNum, indent) && !isFlowContent(lineNum)) {
                out.add(InvalidSyntaxDiagnostic.build(sourceFile, fullSource, lineNum + 1, line, indent));
            }
        }
    }

    private void validateEntry(int lineNum, @NotNull String line, int indent, @NotNull String trimmed, int colonIdx, int @NotNull [] indentStack, int stackDepth, @NotNull Map<Integer, Map<String, Integer>> seenKeys) {
        String keyPart = trimmed.substring(0, colonIdx);
        String valuePart = colonIdx + 1 < trimmed.length() ? trimmed.substring(colonIdx + 1) : "";

        int keyStart = leadingSpaces(keyPart);
        int keyEnd = trailingBound(keyPart, keyStart);
        String rawKey = keyPart.substring(keyStart, keyEnd);
        String key = unquote(rawKey);

        char keyQuote = quoteChar(rawKey);
        if (keyQuote != 0 && !endsWithQuote(rawKey, keyQuote)) {
            out.add(UnclosedQuoteDiagnostic.build(sourceFile, fullSource, lineNum + 1, line, indent + keyStart, keyQuote, true));
        }

        if (rawKey.isEmpty()) {
            out.add(EmptyKeyDiagnostic.build(sourceFile, fullSource, lineNum + 1, line, indent + colonIdx));
        }

        int expected = alignedLevel(indentStack, stackDepth, indent);
        if (indent <= indentStack[stackDepth] && indent != expected) {
            out.add(IndentationDiagnostic.build(sourceFile, fullSource, lineNum + 1, line, indent, expected));
        }

        Map<String, Integer> atLevel = seenKeys.computeIfAbsent(indent, k -> new HashMap<>());
        Integer first = atLevel.get(key);
        if (first != null) {
            out.add(DuplicateKeyDiagnostic.build(sourceFile, fullSource, key, lineNum + 1, line, indent, first + 1, text[first], lines.indent(first)));
        } else {
            atLevel.put(key, lineNum);
        }

        int valStart = leadingSpaces(valuePart);
        if (valStart < valuePart.length()) {
            char valFirst = valuePart.charAt(valStart);
            int valColumn = indent + colonIdx + 1 + valStart;
            String value = valuePart.substring(valStart).trim();
            checkAnchor(lineNum, line, valColumn, value);
            if ((valFirst == '\'' || valFirst == '"') && valuePart.substring(valStart + 1).indexOf(valFirst) < 0) {
                out.add(UnclosedQuoteDiagnostic.build(sourceFile, fullSource, lineNum + 1, line, valColumn, valFirst, false));
            } else {
                checkScalar(lineNum, line, valColumn, value);
            }
        }
    }

    private void checkAnchor(int lineNum, @NotNull String line, int column, @NotNull String value) {
        if (value.isEmpty() || value.charAt(0) != '&' || column < 0) {
            return;
        }
        int end = 1;
        while (end < value.length() && Scan.isWord(value.charAt(end))) {
            end++;
        }
        if (end == 1) {
            return;
        }
        String anchor = value.substring(1, end);
        int[] first = seenAnchors.get(anchor);
        if (first != null) {
            out.add(DuplicateAnchorDiagnostic.build(sourceFile, fullSource, anchor, lineNum + 1, line, column, first[0] + 1, text[first[0]], first[1], seenAnchors.keySet()));
        } else {
            seenAnchors.put(anchor, new int[]{lineNum, column});
        }
    }

    private void checkScalar(int lineNum, @NotNull String line, int column, @NotNull String value) {
        if (value.isEmpty() || column < 0) {
            return;
        }
        char c0 = value.charAt(0);
        if (c0 == '"' || c0 == '\'' || c0 == '{' || c0 == '[' || c0 == '|' || c0 == '>' || c0 == '&' || c0 == '*' || c0 == '#') {
            return;
        }
        if (value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("on") || value.equalsIgnoreCase("off")) {
            out.add(LegacyBooleanDiagnostic.build(sourceFile, fullSource, lineNum + 1, line, column, value));
        } else if (value.length() > 1 && value.charAt(0) == '0' && isOctalDigits(value)) {
            out.add(LegacyOctalDiagnostic.build(sourceFile, fullSource, lineNum + 1, line, column, value));
        }
    }

    private boolean isOctalDigits(@NotNull String value) {
        for (int i = 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '7') {
                return false;
            }
        }
        return value.length() > 1;
    }

    private int pushOrPop(int @NotNull [] indentStack, int stackDepth, int indent) {
        if (indent > indentStack[stackDepth]) {
            indentStack[++stackDepth] = indent;
        } else {
            while (stackDepth > 0 && indent < indentStack[stackDepth]) {
                stackDepth--;
            }
        }
        return stackDepth;
    }

    private int alignedLevel(int @NotNull [] indentStack, int stackDepth, int indent) {
        int level = 0;
        for (int i = 0; i <= stackDepth; i++) {
            if (indentStack[i] <= indent) {
                level = indentStack[i];
            }
        }
        return level;
    }

    private boolean isBlockScalarContent(int lineNum, int indent) {
        for (int prev = lineNum - 1; prev >= 0; prev--) {
            char prevFirst = lines.firstChar(prev);
            if (prevFirst == 0 || prevFirst == '#') {
                continue;
            }
            int prevIndent = lines.indent(prev);
            if (prevIndent >= indent) {
                continue;
            }
            String prevTrimmed = text[prev].substring(prevIndent);
            return prevTrimmed.endsWith("|") || prevTrimmed.endsWith(">") || prevTrimmed.endsWith("|+") || prevTrimmed.endsWith(">-") || prevTrimmed.endsWith("|-") || prevTrimmed.endsWith(">+");
        }
        return false;
    }

    private boolean isFlowContent(int lineNum) {
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

    private boolean isStandaloneToken(char firstChar, @NotNull String trimmed) {
        return firstChar != '|' && firstChar != '>' && firstChar != '[' && firstChar != '{'
                && firstChar != '*' && firstChar != '&' && firstChar != '?' && firstChar != ':'
                && !trimmed.startsWith("---") && !trimmed.startsWith("...");
    }

    private int flowDelta(@NotNull String trimmed) {
        int d = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{' || c == '[') {
                d++;
            } else if (c == '}' || c == ']') {
                d--;
            }
        }
        return d;
    }

    private int tabInIndent(@NotNull String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') {
                return i;
            }
            if (c != ' ') {
                return -1;
            }
        }
        return -1;
    }

    private @NotNull String unquote(@NotNull String key) {
        char q = quoteChar(key);
        if (q != 0 && endsWithQuote(key, q)) {
            return key.substring(1, key.length() - 1);
        }
        return key;
    }

    private char quoteChar(@NotNull String s) {
        return !s.isEmpty() && (s.charAt(0) == '\'' || s.charAt(0) == '"') ? s.charAt(0) : 0;
    }

    private boolean endsWithQuote(@NotNull String s, char quote) {
        return s.length() >= 2 && s.charAt(s.length() - 1) == quote;
    }

    private boolean isAllDigits(@NotNull String s) {
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return s.length() > 1;
    }

    private int leadingSpaces(@NotNull String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private int trailingBound(@NotNull String s, int from) {
        int end = s.length();
        while (end > from && s.charAt(end - 1) == ' ') {
            end--;
        }
        return end;
    }

    private @NotNull String[] materializeLines() {
        int count = lines.count();
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = source.slice(lines.start(i), lines.end(i)).toString();
        }
        return result;
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
