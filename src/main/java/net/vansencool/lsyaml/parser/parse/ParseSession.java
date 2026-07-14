package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.node.AdjacentLine;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.ParseOptions;
import net.vansencool.lsyaml.parser.text.Scan;
import net.vansencool.lsyaml.parser.text.Slice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared parsing state holding the cursor, options, and the block, list, complex key, and value parsers.
 */
public final class ParseSession {

    private final @NotNull Cursor cursor;
    private final @NotNull ParseOptions options;
    private final @NotNull BlockMap map;
    private final @NotNull BlockList list;
    private final @NotNull ComplexKey complexKey;
    private final @NotNull Values values;
    private @Nullable List<YamlNode> anchored;
    private @Nullable List<MapNode.MapEntry> mergeEntries;

    public ParseSession(@NotNull Cursor cursor, @NotNull ParseOptions options) {
        this.cursor = cursor;
        this.options = options;
        this.map = new BlockMap(this);
        this.list = new BlockList(this);
        this.complexKey = new ComplexKey(this);
        this.values = new Values(this);
    }

    /**
     * Returns the shared cursor.
     */
    public @NotNull Cursor cursor() {
        return cursor;
    }

    /**
     * Returns the parse options.
     */
    public @NotNull ParseOptions options() {
        return options;
    }

    /**
     * Returns the block map parser.
     */
    public @NotNull BlockMap map() {
        return map;
    }

    /**
     * Returns the block list parser.
     */
    public @NotNull BlockList list() {
        return list;
    }

    /**
     * Returns the complex key parser.
     */
    public @NotNull ComplexKey complexKey() {
        return complexKey;
    }

    /**
     * Returns the value parser.
     */
    public @NotNull Values values() {
        return values;
    }

    /**
     * Records a node that carries an anchor.
     */
    public void markAnchor(@NotNull YamlNode node) {
        if (anchored == null) {
            anchored = new ArrayList<>();
        }
        anchored.add(node);
    }

    /**
     * Returns the nodes carrying anchors, or null when there are none.
     */
    public @Nullable List<YamlNode> anchored() {
        return anchored;
    }

    /**
     * Records a merge key entry whose value is an alias.
     */
    public void markMergeEntry(@NotNull MapNode.MapEntry entry) {
        if (mergeEntries == null) {
            mergeEntries = new ArrayList<>();
        }
        mergeEntries.add(entry);
    }

    /**
     * Returns the merge key entries, or null when there are none.
     */
    public @Nullable List<MapNode.MapEntry> mergeEntries() {
        return mergeEntries;
    }

    /**
     * Returns the standalone comment text of the cursor line, excluding the hash.
     */
    public @NotNull String comment(@NotNull Cursor c) {
        int hash = Scan.standaloneHash(c.source(), c.contentStart(), c.end());
        return c.source().slice(hash, c.end()).toString();
    }

    /**
     * Advances over blank and comment lines, appending each to the given list in source order.
     */
    public void skipBlanksAndComments(@NotNull List<AdjacentLine> leading) {
        while (cursor.hasMore()) {
            char first = cursor.firstChar();
            if (first == 0) {
                leading.add(AdjacentLine.blank());
                cursor.advance();
            } else if (first == '#') {
                leading.add(AdjacentLine.comment(comment(cursor)));
                cursor.advance();
            } else {
                break;
            }
        }
    }

    /**
     * Returns the ordered blank and comment lines the cursor skips, or an empty list when there are none.
     */
    public @NotNull List<AdjacentLine> skipBlanksAndComments() {
        char first = cursor.firstChar();
        if (!cursor.hasMore() || (first != 0 && first != '#')) {
            return List.of();
        }
        List<AdjacentLine> leading = new ArrayList<>();
        skipBlanksAndComments(leading);
        return leading;
    }

    /**
     * Returns a flow node parsed from an inline value, collecting continuation lines.
     */
    public @NotNull YamlNode flow(@NotNull Slice value, char open) {
        char close = open == '{' ? '}' : ']';
        MultiLineFlow.Result collected = MultiLineFlow.collect(cursor, value, open, close);
        YamlNode node = open == '{' ? Flow.map(collected.content()) : Flow.list(collected.content());
        if (collected.multiLine()) {
            if (node instanceof MapNode m) {
                m.setMultiLineFlow(true);
                m.setFlowIndent(collected.indent());
            } else if (node instanceof ListNode l) {
                l.setMultiLineFlow(true);
                l.setFlowIndent(collected.indent());
            }
        }
        return node;
    }

    /**
     * Returns a node parsed by treating an inline value as a re-indented block at the prior line.
     */
    public @NotNull YamlNode reparseInline(int indent, @NotNull Slice content, char marker) {
        cursor.line(cursor.line() - 1);
        cursor.override(cursor.line(), content, indent);
        YamlNode node = marker == '-' ? list.parse(indent, new ArrayList<>()) : map.parse(indent, new ArrayList<>());
        cursor.clearOverride();
        return node;
    }
}
