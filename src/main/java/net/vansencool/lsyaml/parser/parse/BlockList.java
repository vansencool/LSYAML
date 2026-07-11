package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
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
    public ListNode parse(int expectedIndent, @NotNull List<String> initialComments, int initialEmptyLines) {
        Cursor cursor = session.cursor();
        ListNode list = new ListNode();
        list.getMetadata().setLine(cursor.line() + 1);
        list.getMetadata().setIndentation(expectedIndent);

        List<String> pendingComments = new ArrayList<>(initialComments);
        int pendingEmptyLines = initialEmptyLines;

        while (cursor.hasMore()) {
            char firstChar = cursor.firstChar();

            if (firstChar == 0) {
                pendingEmptyLines++;
                cursor.advance();
                continue;
            }

            int indent = cursor.indent();

            if (firstChar == '#') {
                if (indent < expectedIndent) break;
                pendingComments.add(session.comment(cursor));
                cursor.advance();
                continue;
            }

            if (indent < expectedIndent || firstChar != '-') {
                attachTrailing(list, pendingComments, pendingEmptyLines);
                break;
            }

            String lineContent = cursor.trimmedContent();
            if (lineContent.length() > 1 && lineContent.charAt(1) != ' ' && lineContent.charAt(1) != '\t') {
                attachTrailing(list, pendingComments, pendingEmptyLines);
                break;
            }

            ListNode.ListEntry entry = new ListNode.ListEntry(new ScalarNode(null));
            entry.setCommentsBefore(pendingComments);
            entry.setEmptyLinesBefore(pendingEmptyLines);
            pendingComments = new ArrayList<>();
            pendingEmptyLines = 0;

            String valueStr = valueAfterDash(lineContent);
            int hash = session.inlineHash(valueStr);
            if (hash >= 0) {
                entry.setInlineComment(valueStr.substring(hash + 1));
                valueStr = valueStr.substring(0, hash - 1).trim();
            }

            cursor.advance();
            pendingEmptyLines += fillEntry(entry, valueStr, indent);

            list.addEntry(entry);
            if (list.size() == 1) expectedIndent = indent;
        }

        attachTrailing(list, pendingComments, pendingEmptyLines);
        return list;
    }

    private int fillEntry(@NotNull ListNode.ListEntry entry, @NotNull String valueStr, int indent) {
        Cursor cursor = session.cursor();
        if (valueStr.isEmpty()) {
            List<String> nestedComments = new ArrayList<>();
            int nestedEmptyLines = session.skipBlanksAndComments(nestedComments);
            if (!cursor.hasMore()) {
                entry.setValue(new ScalarNode(null));
                return 0;
            }
            int nextIndent = cursor.indent();
            char nextFirst = cursor.firstChar();
            if (nextIndent > indent && nextFirst != 0) {
                YamlNode value = nextFirst == '-'
                        ? parse(nextIndent, nestedComments, nestedEmptyLines)
                        : session.map().parse(nextIndent, nestedComments, nestedEmptyLines);
                entry.setValue(value);
                int trailing = value.getTrailingEmptyLines();
                value.setTrailingEmptyLines(0);
                return trailing;
            }
            entry.setValue(new ScalarNode(null));
            if (!nestedComments.isEmpty() || nestedEmptyLines > 0) {
                cursor.line(cursor.line() - (nestedComments.size() + nestedEmptyLines));
            }
            return 0;
        }

        char first = valueStr.charAt(0);
        if (first == '{' || first == '[') {
            entry.setValue(Flow.value(valueStr));
        } else if (containsUnquotedColon(valueStr)) {
            entry.setValue(session.reparseInline(indent + 2, valueStr, '?'));
        } else if (first == '-') {
            entry.setValue(session.reparseInline(indent + 2, valueStr, '-'));
        } else {
            entry.setValue(session.values().parse(valueStr, indent));
        }
        return 0;
    }

    private @NotNull String valueAfterDash(@NotNull String lineContent) {
        int start = 1;
        while (start < lineContent.length() && (lineContent.charAt(start) == ' ' || lineContent.charAt(start) == '\t')) {
            start++;
        }
        int end = lineContent.length();
        while (end > start && (lineContent.charAt(end - 1) == ' ' || lineContent.charAt(end - 1) == '\t')) {
            end--;
        }
        return start >= end ? "" : lineContent.substring(start, end);
    }

    private boolean containsUnquotedColon(@NotNull String value) {
        if (value.isEmpty()) return false;
        char first = value.charAt(0);
        if (first == '\'' || first == '"') return false;
        boolean single = false;
        boolean dbl = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'' && !dbl) single = !single;
            else if (c == '"' && !single) dbl = !dbl;
            else if (c == ':' && !single && !dbl) return true;
        }
        return false;
    }

    private void attachTrailing(@NotNull ListNode list, @NotNull List<String> comments, int emptyLines) {
        if (!comments.isEmpty() || emptyLines > 0) {
            list.setTrailingComments(comments);
            list.setTrailingEmptyLines(emptyLines);
        }
    }
}
