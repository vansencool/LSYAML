package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.parser.diagnostic.EmptyKeyDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.IndentationDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.InvalidSyntaxDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.TabIndentDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.UnclosedQuoteDiagnostic;
import net.vansencool.lsyaml.parser.diagnostic.UndefinedAliasDiagnostic;
import net.vansencool.lsyaml.parser.source.LineIndex;
import net.vansencool.lsyaml.parser.source.Source;
import net.vansencool.lsyaml.parser.text.Scan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validator that reports where a document is not valid YAML 1.2, without rich diagnostics.
 */
public final class FastYamlValidator implements YamlValidator {

    private LineIndex lines;
    private @Nullable String sourceFile;
    private char @NotNull [] chars = new char[0];
    private @Nullable String fullSource;
    private @NotNull List<Diagnostic> out = List.of();
    private final @NotNull Set<String> seenAnchors = new HashSet<>();

    private FastYamlValidator() {
    }

    /**
     * Returns a new validator instance.
     */
    public static @NotNull FastYamlValidator newInstance() {
        return new FastYamlValidator();
    }

    @Override
    public void validate(@NotNull Source source, @NotNull LineIndex lines, @Nullable String sourceFile, @NotNull List<Diagnostic> diagnostics) {
        this.lines = lines;
        this.sourceFile = sourceFile;
        this.out = diagnostics;
        this.chars = source.chars();
        this.fullSource = null;
        this.seenAnchors.clear();

        int prevIndent = 0;
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

            if (indent > prevIndent + 8) {
                out.add(IndentationDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), indent, prevIndent));
            }
            prevIndent = indent;

            if (firstChar == '-') {
                int itemStart = skipSpaces(contentStart + 1, lineEnd);
                if (itemStart < lineEnd) {
                    trackAnchor(itemStart, lineEnd);
                    checkAlias(lineNum, itemStart, lineEnd);
                }
                continue;
            }

            if (firstChar == '?') {
                complexKeyIndent = indent;
                continue;
            }
            if (firstChar == ':' && complexKeyIndent == indent) {
                complexKeyIndent = -1;
                continue;
            }

            int colon = findUnquotedColon(contentStart, lineEnd);
            if (colon > contentStart) {
                validateEntry(lineNum, contentStart, lineEnd, colon, indent);
            } else if (firstChar == ':') {
                out.add(EmptyKeyDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), indent));
            } else if (isStandaloneToken(firstChar, contentStart, lineEnd) && !isBlockScalarContent(lineNum, indent) && !isFlowContent(lineNum)) {
                out.add(InvalidSyntaxDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), indent));
            }
        }
    }

    private void validateEntry(int lineNum, int contentStart, int lineEnd, int colon, int indent) {
        int keyStart = skipSpaces(contentStart, colon);
        int keyEnd = trimEnd(keyStart, colon);

        char keyQuote = keyStart < keyEnd ? quoteChar(chars[keyStart]) : 0;
        if (keyQuote != 0 && !(keyEnd - keyStart >= 2 && chars[keyEnd - 1] == keyQuote)) {
            out.add(UnclosedQuoteDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), keyStart - contentStart + indent, keyQuote, true));
        }

        if (keyStart == keyEnd) {
            out.add(EmptyKeyDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), indent + colon - contentStart));
        }

        int valStart = skipSpaces(colon + 1, lineEnd);
        if (valStart < lineEnd) {
            char valFirst = chars[valStart];
            int valColumn = indent + valStart - contentStart;
            trackAnchor(valStart, lineEnd);
            checkAlias(lineNum, valStart, lineEnd);
            if ((valFirst == '\'' || valFirst == '"') && indexOf(valStart + 1, lineEnd, valFirst) < 0) {
                out.add(UnclosedQuoteDiagnostic.build(sourceFile, fullSource(), lineNum + 1, lineText(lineNum), valColumn, valFirst, false));
            }
        }
    }

    private void trackAnchor(int from, int to) {
        if (from >= to || chars[from] != '&') {
            return;
        }
        int end = from + 1;
        while (end < to && Scan.isWord(chars[end])) {
            end++;
        }
        if (end > from + 1) {
            seenAnchors.add(new String(chars, from + 1, end - from - 1));
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
        if (!seenAnchors.contains(alias)) {
            out.add(UndefinedAliasDiagnostic.build(sourceFile, fullSource(), alias, lineNum + 1, lineText(lineNum), from - lines.start(lineNum)));
        }
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
