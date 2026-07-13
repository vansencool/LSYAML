package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.node.IntHashMap;
import net.vansencool.lsyaml.parser.diagnostic.DuplicateAnchorDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.DuplicateKeyDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.EmptyKeyDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.IndentationDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.InvalidSyntaxDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.LegacyBooleanDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.LegacyOctalDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.TabIndentDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.UnclosedQuoteDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.UndefinedAliasDiagnostic;
import net.vansencool.lsyaml.parser.source.LineIndex;
import net.vansencool.lsyaml.parser.source.Source;
import net.vansencool.lsyaml.parser.text.Scan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validator that checks a document against the YAML 1.2 core schema and reports rich diagnostics.
 */
public final class RichYamlValidator implements YamlValidator {

    private @NotNull LineIndex lines;
    private @Nullable String sourceFile;
    private char @NotNull [] chars = new char[0];
    private @Nullable String fullSource;
    private @NotNull List<Diagnostic> out = List.of();
    private final @NotNull Map<String, int[]> seenAnchors = new HashMap<>();
    private int @NotNull [] keyLevelIndent = new int[16];
    private IntHashMap @NotNull [] keyLevelMap = new IntHashMap[16];
    private int keyDepth;

    private RichYamlValidator() {
    }

    /**
     * Returns a new validator instance.
     */
    public static @NotNull RichYamlValidator newInstance() {
        return new RichYamlValidator();
    }

    @Override
    public void validate(@NotNull Source source, @NotNull LineIndex lines, @Nullable String sourceFile, @NotNull List<Diagnostic> diagnostics) {
        this.lines = lines;
        this.sourceFile = sourceFile;
        this.out = diagnostics;
        this.chars = source.chars();
        this.fullSource = null;
        this.seenAnchors.clear();
        this.keyDepth = 0;

        int[] indentStack = new int[256];
        int stackDepth = 0;
        int flowDepth = 0;
        int complexKeyIndent = -1;
        int totalLines = lines.count();

        for (int lineNum = 0; lineNum < totalLines; lineNum++) {
            char firstChar = lines.firstChar(lineNum);
            if (firstChar == 0 || firstChar == '#') {
                continue;
            }

            int lineStart = lines.start(lineNum);
            int lineEnd = trimEnd(lineStart, lines.end(lineNum));

            int tab = tabInIndent(lineStart, lineEnd);
            if (tab >= 0) {
                int tabEnd = tab;
                while (tabEnd < lineEnd && chars[tabEnd] == '\t') {
                    tabEnd++;
                }
                out.add(TabIndentDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), tab - lineStart, tabEnd - lineStart));
            }

            int indent = lines.indent(lineNum);
            int contentStart = lines.contentStart(lineNum);

            int prevFlowDepth = flowDepth;
            flowDepth += flowDelta(contentStart, lineEnd);
            if (prevFlowDepth > 0 || flowDepth > 0 || firstChar == '}' || firstChar == ']') {
                continue;
            }

            if (firstChar == '-') {
                pruneKeyLevels(indent);
                stackDepth = pushOrPop(indentStack, stackDepth, indent);
                if (lineEnd - contentStart > 2 && findUnquotedColon(contentStart, lineEnd) > contentStart + 2) {
                    indentStack[++stackDepth] = indent + 2;
                }
                int itemStart = skipSpaces(contentStart + 1, lineEnd);
                if (itemStart < lineEnd) {
                    checkAnchor(lineNum, itemStart, lineEnd);
                    checkAlias(lineNum, itemStart, lineEnd);
                    checkScalar(lineNum, itemStart, lineEnd);
                }
                continue;
            }

            if (firstChar == '?') {
                complexKeyIndent = indent;
                pruneKeyLevels(indent);
                continue;
            }
            if (firstChar == ':' && complexKeyIndent == indent) {
                complexKeyIndent = -1;
                continue;
            }

            int colon = findUnquotedColon(contentStart, lineEnd);
            if (colon > contentStart) {
                pruneKeyLevels(indent);
                validateEntry(lineNum, contentStart, lineEnd, colon, indent, indentStack, stackDepth);
                stackDepth = pushOrPop(indentStack, stackDepth, indent);
            } else if (firstChar == ':') {
                out.add(EmptyKeyDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), indent));
            } else if (isStandaloneToken(firstChar, contentStart, lineEnd) && !isBlockScalarContent(lineNum, indent) && !isFlowContent(lineNum)) {
                out.add(InvalidSyntaxDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), indent));
            }
        }
    }

    private void validateEntry(int lineNum, int contentStart, int lineEnd, int colon, int indent, int @NotNull [] indentStack, int stackDepth) {
        int keyStart = skipSpaces(contentStart, colon);
        int keyEnd = trimEnd(keyStart, colon);

        char keyQuote = keyStart < keyEnd ? quoteChar(chars[keyStart]) : 0;
        if (keyQuote != 0 && !(keyEnd - keyStart >= 2 && chars[keyEnd - 1] == keyQuote)) {
            out.add(UnclosedQuoteDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), keyStart - contentStart + indent, keyQuote, true));
        }

        if (keyStart == keyEnd) {
            out.add(EmptyKeyDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), indent + colon - contentStart));
        }

        int expected = alignedLevel(indentStack, stackDepth, indent);
        if (indent <= indentStack[stackDepth] && indent != expected) {
            out.add(IndentationDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), indent, expected));
        }

        String key = unquotedKey(keyStart, keyEnd);
        IntHashMap atLevel = keyScope(indent);
        int first = atLevel.get(key);
        if (first != IntHashMap.ABSENT) {
            out.add(DuplicateKeyDiagnostic.build(sourceFile, fullSource(), key, lineNum + 1, lineText(lineNum), indent, first + 1, lineText(first), lines.indent(first)));
        } else {
            atLevel.put(key, lineNum);
        }

        int valStart = skipSpaces(colon + 1, lineEnd);
        if (valStart < lineEnd) {
            char valFirst = chars[valStart];
            int valColumn = indent + valStart - contentStart;
            checkAnchor(lineNum, valStart, lineEnd);
            checkAlias(lineNum, valStart, lineEnd);
            if ((valFirst == '\'' || valFirst == '"') && indexOf(valStart + 1, lineEnd, valFirst) < 0) {
                out.add(UnclosedQuoteDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), valColumn, valFirst, false));
            } else {
                checkScalar(lineNum, valStart, lineEnd);
            }
        }
    }

    private void checkAnchor(int lineNum, int from, int to) {
        if (from >= to || chars[from] != '&') {
            return;
        }
        int end = from + 1;
        while (end < to && Scan.isWord(chars[end])) {
            end++;
        }
        if (end == from + 1) {
            return;
        }
        String anchor = new String(chars, from + 1, end - from - 1);
        int column = from - lines.start(lineNum);
        int[] existing = seenAnchors.get(anchor);
        if (existing != null) {
            out.add(DuplicateAnchorDiagnostic.build(sourceFile, fullSource(), anchor, lineNum + 1, lineText(lineNum), column, existing[0] + 1, lineText(existing[0]), existing[1], seenAnchors.keySet()));
        } else {
            seenAnchors.put(anchor, new int[]{lineNum, column});
        }
    }

    private void checkAlias(int lineNum, int from, int to) {
        if (from >= to || chars[from] != '*') {
            return;
        }
        int end = from + 1;
        while (end < to && Scan.isWord(chars[end])) {
            end++;
        }
        if (end == from + 1) {
            return;
        }
        String alias = new String(chars, from + 1, end - from - 1);
        if (!seenAnchors.containsKey(alias)) {
            int column = from - lines.start(lineNum);
            out.add(UndefinedAliasDiagnostic.build(sourceFile, fullSource(), alias, lineNum + 1, lineText(lineNum), column));
        }
    }

    private void checkScalar(int lineNum, int from, int to) {
        int end = trimEnd(from, to);
        if (from >= end) {
            return;
        }
        char c0 = chars[from];
        if (c0 == '"' || c0 == '\'' || c0 == '{' || c0 == '[' || c0 == '|' || c0 == '>' || c0 == '&' || c0 == '*' || c0 == '#') {
            return;
        }
        int len = end - from;
        int column = from - lines.start(lineNum);
        if (isLegacyBoolean(from, end)) {
            out.add(LegacyBooleanDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), column, new String(chars, from, len)));
        } else if (len > 1 && c0 == '0' && isOctalDigits(from, end)) {
            out.add(LegacyOctalDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), column, new String(chars, from, len)));
        }
    }

    private boolean isLegacyBoolean(int from, int to) {
        int len = to - from;
        if (len == 2) {
            char a = lower(chars[from]);
            char b = lower(chars[from + 1]);
            return (a == 'n' && b == 'o') || (a == 'o' && b == 'n');
        }
        if (len == 3) {
            char a = lower(chars[from]);
            char b = lower(chars[from + 1]);
            char c = lower(chars[from + 2]);
            return (a == 'y' && b == 'e' && c == 's') || (a == 'o' && b == 'f' && c == 'f');
        }
        return false;
    }

    private boolean isOctalDigits(int from, int to) {
        for (int i = from + 1; i < to; i++) {
            char c = chars[i];
            if (c < '0' || c > '7') {
                return false;
            }
        }
        return to - from > 1;
    }

    private void pruneKeyLevels(int indent) {
        while (keyDepth > 0 && keyLevelIndent[keyDepth - 1] > indent) {
            keyDepth--;
        }
    }

    private @NotNull IntHashMap keyScope(int indent) {
        if (keyDepth > 0 && keyLevelIndent[keyDepth - 1] == indent) {
            return keyLevelMap[keyDepth - 1];
        }
        if (keyDepth == keyLevelMap.length) {
            keyLevelIndent = Arrays.copyOf(keyLevelIndent, keyDepth << 1);
            keyLevelMap = Arrays.copyOf(keyLevelMap, keyDepth << 1);
        }
        IntHashMap map = keyLevelMap[keyDepth];
        if (map == null) {
            map = new IntHashMap();
            keyLevelMap[keyDepth] = map;
        } else {
            map.clear();
        }
        keyLevelIndent[keyDepth] = indent;
        keyDepth++;
        return map;
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
            int end = trimEnd(lines.contentStart(prev), lines.end(prev));
            char last = end > lines.contentStart(prev) ? chars[end - 1] : 0;
            return last == '|' || last == '>' || last == '+' || last == '-';
        }
        return false;
    }

    private boolean isFlowContent(int lineNum) {
        for (int prev = lineNum - 1; prev >= 0; prev--) {
            char prevFirst = lines.firstChar(prev);
            if (prevFirst == 0 || prevFirst == '#') {
                continue;
            }
            int end = trimEnd(lines.contentStart(prev), lines.end(prev));
            char last = end > lines.contentStart(prev) ? chars[end - 1] : 0;
            return last == '{' || last == '[' || last == ',';
        }
        return false;
    }

    private boolean isStandaloneToken(char firstChar, int from, int to) {
        if (firstChar == '|' || firstChar == '>' || firstChar == '[' || firstChar == '{'
                || firstChar == '*' || firstChar == '&' || firstChar == '?' || firstChar == ':') {
            return false;
        }
        return !startsWith(from, to, '-', '-', '-') && !startsWith(from, to, '.', '.', '.');
    }

    private boolean startsWith(int from, int to, char a, char b, char c) {
        return to - from >= 3 && chars[from] == a && chars[from + 1] == b && chars[from + 2] == c;
    }

    private int flowDelta(int from, int to) {
        int d = 0;
        for (int i = from; i < to; i++) {
            char c = chars[i];
            if (c == '{' || c == '[') {
                d++;
            } else if (c == '}' || c == ']') {
                d--;
            }
        }
        return d;
    }

    private int tabInIndent(int from, int to) {
        for (int i = from; i < to; i++) {
            char c = chars[i];
            if (c == '\t') {
                return i;
            }
            if (c != ' ') {
                return -1;
            }
        }
        return -1;
    }

    private @NotNull String unquotedKey(int from, int to) {
        if (to - from >= 2) {
            char q = chars[from];
            if ((q == '\'' || q == '"') && chars[to - 1] == q) {
                return new String(chars, from + 1, to - from - 2);
            }
        }
        return new String(chars, from, to - from);
    }

    private char quoteChar(char c) {
        return c == '\'' || c == '"' ? c : 0;
    }

    private int skipSpaces(int from, int to) {
        int i = from;
        while (i < to && chars[i] == ' ') {
            i++;
        }
        return i;
    }

    private int trimEnd(int from, int to) {
        int end = to;
        while (end > from && chars[end - 1] == ' ') {
            end--;
        }
        return end;
    }

    private int indexOf(int from, int to, char target) {
        for (int i = from; i < to; i++) {
            if (chars[i] == target) {
                return i;
            }
        }
        return -1;
    }

    private char lower(char c) {
        return c >= 'A' && c <= 'Z' ? (char) (c + 32) : c;
    }

    private @NotNull String lineText(int lineNum) {
        int start = lines.start(lineNum);
        return new String(chars, start, lines.end(lineNum) - start);
    }

    private @NotNull String fullSource() {
        if (fullSource == null) {
            int count = lines.count();
            StringBuilder sb = new StringBuilder(chars.length);
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                int start = lines.start(i);
                sb.append(chars, start, lines.end(i) - start);
            }
            fullSource = sb.toString();
        }
        return fullSource;
    }

    private int findUnquotedColon(int from, int to) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = from; i < to; i++) {
            char c = chars[i];
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
