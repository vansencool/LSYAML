package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.node.AdjacentLine;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.text.Scan;
import net.vansencool.lsyaml.parser.text.Slice;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for block-style sequences.
 */
public final class BlockList {

    private final @NotNull ParseSession session;

    public BlockList(@NotNull ParseSession session) {
        this.session = session;
    }

    /**
     * Returns a block list parsed from the cursor at the expected indentation.
     */
    @NotNull
    public ListNode parse(int expectedIndent, @NotNull List<AdjacentLine> initialLeading) {
        Cursor cursor = session.cursor();
        ListNode list = new ListNode();
        list.getMetadata().setLine(cursor.line() + 1);

        List<AdjacentLine> pending = initialLeading.isEmpty() ? new ArrayList<>() : initialLeading;

        while (cursor.hasMore()) {
            char firstChar = cursor.firstChar();

            if (firstChar == 0) {
                pending.add(AdjacentLine.blank());
                cursor.advance();
                continue;
            }

            int indent = cursor.indent();

            if (firstChar == '#') {
                if (indent < expectedIndent) break;
                pending.add(AdjacentLine.comment(session.comment(cursor)));
                cursor.advance();
                continue;
            }

            if (indent < expectedIndent || firstChar != '-') {
                attachTrailing(list, pending);
                break;
            }

            Slice lineContent = cursor.trimmedContent();
            if (lineContent.length() > 1 && lineContent.charAt(1) != ' ' && lineContent.charAt(1) != '\t') {
                attachTrailing(list, pending);
                break;
            }

            ListNode.ListEntry entry = new ListNode.ListEntry(new ScalarNode(null));
            entry.setLeadingLines(pending);
            pending = new ArrayList<>();

            Slice valueSlice = valueAfterDash(lineContent);
            int hash = Scan.inlineHash(valueSlice.array(), valueSlice.start(), valueSlice.end());
            if (hash >= 0) {
                entry.setInlineComment(valueSlice.copy().sub(hash + 1).toString());
                valueSlice = valueSlice.sub(0, hash - 1).trim();
            }

            cursor.advance();
            for (int i = 0, n = fillEntry(entry, valueSlice.toString(), indent); i < n; i++) {
                pending.add(AdjacentLine.blank());
            }

            list.addEntry(entry);
            if (list.size() == 1) expectedIndent = indent;
        }

        attachTrailing(list, pending);
        return list;
    }

    private int fillEntry(@NotNull ListNode.ListEntry entry, @NotNull String valueStr, int indent) {
        Cursor cursor = session.cursor();
        if (valueStr.isEmpty()) {
            List<AdjacentLine> nested = session.skipBlanksAndComments();
            if (!cursor.hasMore()) {
                entry.setValue(new ScalarNode(null));
                return 0;
            }
            int nextIndent = cursor.indent();
            char nextFirst = cursor.firstChar();
            if (nextIndent > indent && nextFirst != 0) {
                YamlNode value = nextFirst == '-'
                        ? parse(nextIndent, nested)
                        : session.map().parse(nextIndent, nested);
                entry.setValue(value);
                int trailing = value.getTrailingEmptyLines();
                value.setTrailingEmptyLines(0);
                return trailing;
            }
            entry.setValue(new ScalarNode(null));
            if (!nested.isEmpty()) {
                cursor.line(cursor.line() - nested.size());
            }
            return 0;
        }

        char first = valueStr.charAt(0);
        if (first == '{' || first == '[') {
            entry.setValue(Flow.value(valueStr));
        } else if (Scan.hasUnquotedColon(valueStr)) {
            entry.setValue(session.reparseInline(indent + 2, valueStr, '?'));
        } else if (first == '-') {
            entry.setValue(session.reparseInline(indent + 2, valueStr, '-'));
        } else {
            entry.setValue(session.values().parse(valueStr, indent));
        }
        return 0;
    }

    private @NotNull Slice valueAfterDash(@NotNull Slice lineContent) {
        int start = 1;
        while (start < lineContent.length() && (lineContent.charAt(start) == ' ' || lineContent.charAt(start) == '\t')) {
            start++;
        }
        int end = lineContent.length();
        while (end > start && (lineContent.charAt(end - 1) == ' ' || lineContent.charAt(end - 1) == '\t')) {
            end--;
        }
        return start >= end ? Slice.empty() : lineContent.sub(start, end);
    }

    private void attachTrailing(@NotNull ListNode list, @NotNull List<AdjacentLine> trailing) {
        if (!trailing.isEmpty()) {
            list.setTrailingLines(trailing);
        }
    }
}
