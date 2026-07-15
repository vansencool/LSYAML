package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.exceptions.YamlParseException;
import net.vansencool.lsyaml.logger.LSYAMLLogger;
import net.vansencool.lsyaml.node.AdjacentLine;
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
    public MapNode parse(int expectedIndent, @NotNull List<AdjacentLine> initialLeading) {
        Cursor cursor = session.cursor();
        MapNode map = new MapNode();
        map.getMetadata().setLine(cursor.line() + 1);

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

            if (indent < expectedIndent || firstChar == '-') {
                attachTrailing(map, pending);
                break;
            }

            if (firstChar == '?') {
                MapNode.MapEntry entry = session.complexKey().parse(pending, indent);
                if (entry != null) {
                    put(map, entry);
                    pending = new ArrayList<>();
                    if (map.size() == 1) expectedIndent = indent;
                    continue;
                }
            }

            if (firstChar == ':') break;

            KeyLine key = KeyLine.parse(cursor.trimmedContent());
            if (key == null) break;

            int entryLine = cursor.line() + 1;
            MapNode.MapEntry entry = new MapNode.MapEntry(key.key(), new ScalarNode(null), key.keyStyle());
            entry.setLeadingLines(pending);
            pending = new ArrayList<>();
            if (key.inlineComment() != null) entry.setInlineComment(key.inlineComment());

            cursor.advance();

            if (key.value().isEmpty()) {
                addBlanks(pending, fillEmptyValue(entry, indent));
            } else {
                String anchor = Anchors.anchorOnly(key.value());
                if (anchor != null) {
                    addBlanks(pending, fillAnchoredValue(entry, indent, anchor));
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

        attachTrailing(map, pending);
        return map;
    }

    private void addBlanks(@NotNull List<AdjacentLine> pending, int count) {
        for (int i = 0; i < count; i++) {
            pending.add(AdjacentLine.blank());
        }
    }

    private int fillEmptyValue(@NotNull MapNode.MapEntry entry, int indent) {
        Cursor cursor = session.cursor();
        List<AdjacentLine> nested = session.skipBlanksAndComments();

        if (!cursor.hasMore()) {
            entry.setValue(new ScalarNode(null));
            return 0;
        }

        int nextIndent = cursor.indent();
        char nextFirst = cursor.firstChar();
        if (nextIndent > indent && nextFirst != 0) {
            YamlNode value = nextFirst == '-'
                    ? session.list().parse(nextIndent, nested)
                    : parse(nextIndent, nested);
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

    private int fillAnchoredValue(@NotNull MapNode.MapEntry entry, int indent, @NotNull String anchor) {
        Cursor cursor = session.cursor();
        List<AdjacentLine> nested = session.skipBlanksAndComments();

        if (cursor.hasMore()) {
            int nextIndent = cursor.indent();
            char nextFirst = cursor.firstChar();
            if (nextIndent > indent && nextFirst != 0) {
                YamlNode value = nextFirst == '-'
                        ? session.list().parse(nextIndent, nested)
                        : parse(nextIndent, nested);
                value.getMetadata().setAnchor(anchor);
                session.markAnchor(value);
                entry.setValue(value);
                int trailing = value.getTrailingEmptyLines();
                value.setTrailingEmptyLines(0);
                return trailing;
            }
        }

        ScalarNode nullNode = new ScalarNode(null);
        nullNode.getMetadata().setAnchor(anchor);
        session.markAnchor(nullNode);
        entry.setValue(nullNode);
        if (!nested.isEmpty()) {
            cursor.line(cursor.line() - nested.size());
        }
        return 0;
    }

    private void attachTrailing(@NotNull MapNode map, @NotNull List<AdjacentLine> trailing) {
        if (!trailing.isEmpty()) {
            map.setTrailingLines(trailing);
        }
    }

    private void put(@NotNull MapNode map, @NotNull MapNode.MapEntry entry) {
        if (entry.getKey().equals("<<") && entry.getValue().getMetadata().isAlias()) {
            session.markMergeEntry(entry);
        }
        if (!map.containsKey(entry.getKey())) {
            map.appendEntry(entry);
            return;
        }
        switch (session.options().getDuplicateKeyBehavior()) {
            case WARN_AND_OVERRIDE -> {
                LSYAMLLogger.warn("Duplicate key '" + entry.getKey() + "', overriding previous value");
                map.putEntry(entry);
            }
            case WARN_AND_KEEP -> LSYAMLLogger.warn("Duplicate key '" + entry.getKey() + "', keeping first value");
            case SILENT -> map.putEntry(entry);
            case SILENT_AND_KEEP -> {
            }
        }
    }
}
