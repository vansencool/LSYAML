package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.exceptions.YamlParseException;
import net.vansencool.lsyaml.logger.LSYAMLLogger;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for block-style mappings.
 */
public final class BlockMap {

    private final @NotNull ParseSession session;

    public BlockMap(@NotNull ParseSession session) {
        this.session = session;
    }

    /**
     * Returns a block map parsed from the cursor at the expected indentation.
     */
    @NotNull
    public MapNode parse(int expectedIndent, @NotNull List<String> initialComments, int initialEmptyLines) {
        Cursor cursor = session.cursor();
        MapNode map = new MapNode();
        map.getMetadata().setLine(cursor.line() + 1);
        map.getMetadata().setIndentation(expectedIndent);

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

            if (indent < expectedIndent || firstChar == '-') {
                attachTrailing(map, pendingComments, pendingEmptyLines);
                break;
            }

            if (firstChar == '?') {
                MapNode.MapEntry entry = session.complexKey().parse(pendingComments, pendingEmptyLines, indent);
                if (entry != null) {
                    put(map, entry);
                    pendingComments = new ArrayList<>();
                    pendingEmptyLines = 0;
                    if (map.size() == 1) expectedIndent = indent;
                    continue;
                }
            }

            if (firstChar == ':') break;

            KeyLine key = KeyLine.parse(cursor.trimmedContent());
            if (key == null) break;

            int entryLine = cursor.line() + 1;
            MapNode.MapEntry entry = new MapNode.MapEntry(key.key(), new ScalarNode(null), key.keyStyle());
            entry.setCommentsBefore(pendingComments);
            entry.setEmptyLinesBefore(pendingEmptyLines);
            pendingComments = new ArrayList<>();
            pendingEmptyLines = 0;
            if (key.inlineComment() != null) entry.setInlineComment(key.inlineComment());

            cursor.advance();

            if (key.value().isEmpty()) {
                pendingEmptyLines += fillEmptyValue(entry, indent);
            } else {
                String anchor = Anchors.anchorOnly(key.value());
                if (anchor != null) {
                    session.markAnchors();
                    pendingEmptyLines += fillAnchoredValue(entry, indent, anchor);
                } else {
                    entry.setValue(session.values().parse(key.value(), indent));
                }
            }

            if (entry.getValue().getMetadata().getLine() < 0) {
                entry.getValue().getMetadata().setLine(entryLine);
                entry.getValue().getMetadata().setColumn(indent + key.key().length() + 3);
            }

            put(map, entry);
            if (map.size() == 1) expectedIndent = indent;
        }

        attachTrailing(map, pendingComments, pendingEmptyLines);
        return map;
    }

    private int fillEmptyValue(@NotNull MapNode.MapEntry entry, int indent) {
        Cursor cursor = session.cursor();
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
                    ? session.list().parse(nextIndent, nestedComments, nestedEmptyLines)
                    : parse(nextIndent, nestedComments, nestedEmptyLines);
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

    private int fillAnchoredValue(@NotNull MapNode.MapEntry entry, int indent, @NotNull String anchor) {
        Cursor cursor = session.cursor();
        List<String> nestedComments = new ArrayList<>();
        int nestedEmptyLines = session.skipBlanksAndComments(nestedComments);

        if (cursor.hasMore()) {
            int nextIndent = cursor.indent();
            char nextFirst = cursor.firstChar();
            if (nextIndent > indent && nextFirst != 0) {
                YamlNode value = nextFirst == '-'
                        ? session.list().parse(nextIndent, nestedComments, nestedEmptyLines)
                        : parse(nextIndent, nestedComments, nestedEmptyLines);
                value.getMetadata().setAnchor(anchor);
                entry.setValue(value);
                int trailing = value.getTrailingEmptyLines();
                value.setTrailingEmptyLines(0);
                return trailing;
            }
        }

        ScalarNode nullNode = new ScalarNode(null);
        nullNode.getMetadata().setAnchor(anchor);
        entry.setValue(nullNode);
        if (!nestedComments.isEmpty() || nestedEmptyLines > 0) {
            cursor.line(cursor.line() - (nestedComments.size() + nestedEmptyLines));
        }
        return 0;
    }

    private void attachTrailing(@NotNull MapNode map, @NotNull List<String> comments, int emptyLines) {
        if (!comments.isEmpty() || emptyLines > 0) {
            map.setTrailingComments(comments);
            map.setTrailingEmptyLines(emptyLines);
        }
    }

    private void put(@NotNull MapNode map, @NotNull MapNode.MapEntry entry) {
        if (map.get(entry.getKey()) == null) {
            map.putEntry(entry);
            return;
        }
        switch (session.options().getDuplicateKeyBehavior()) {
            case WARN_AND_OVERRIDE -> {
                LSYAMLLogger.warn("Duplicate key '" + entry.getKey() + "', overriding previous value");
                map.putEntry(entry);
            }
            case WARN_AND_KEEP -> LSYAMLLogger.warn("Duplicate key '" + entry.getKey() + "', keeping first value");
            case SILENT -> map.putEntry(entry);
            case ERROR -> throw new YamlParseException("Duplicate key: '" + entry.getKey() + "'");
        }
    }
}
