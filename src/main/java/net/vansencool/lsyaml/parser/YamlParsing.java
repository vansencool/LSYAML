package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.diagnostic.Severity;
import net.vansencool.lsyaml.exceptions.YamlParseException;
import net.vansencool.lsyaml.node.AdjacentLine;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.parse.AnchorResolver;
import net.vansencool.lsyaml.parser.parse.Cursor;
import net.vansencool.lsyaml.parser.parse.ParseSession;
import net.vansencool.lsyaml.parser.source.LineIndex;
import net.vansencool.lsyaml.parser.source.Source;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point that parses YAML text into a node tree over source offsets.
 */
public final class YamlParsing {

    private YamlParsing() {
    }

    /**
     * Returns the root node parsed from YAML text with the given options.
     */
    public static @NotNull YamlNode parse(@NotNull String yaml, @NotNull ParseOptions options) {
        if (options.isStrict()) {
            List<Diagnostic> issues = new ArrayList<>();
            YamlNode node = parseInternal(yaml, options, issues);
            for (Diagnostic issue : issues) {
                if (issue.severity() == Severity.ERROR) {
                    throw new YamlParseException(issue.format());
                }
            }
            return node;
        }
        return parseInternal(yaml, options, null);
    }

    /**
     * Returns the root node and collects strict validation diagnostics into the given list.
     */
    public static @NotNull YamlNode parseDetailed(@NotNull String yaml, @NotNull ParseOptions options, @NotNull List<Diagnostic> issues) {
        return parseInternal(yaml, options, issues);
    }

    private static @NotNull YamlNode parseInternal(@NotNull String yaml, @NotNull ParseOptions options, List<Diagnostic> issues) {
        if (yaml.isEmpty() || isBlank(yaml)) {
            return new MapNode();
        }

        Source source = new Source(yaml);
        LineIndex lines = new LineIndex(source);
        YamlValidator validator = options.getValidator();
        if (issues != null && validator != null) {
            validator.validate(source, lines, null, issues);
        }
        Cursor cursor = new Cursor(source, lines);
        ParseSession session = new ParseSession(cursor, options);

        List<AdjacentLine> pending = session.skipBlanksAndComments();

        if (!cursor.hasMore()) {
            MapNode empty = new MapNode();
            empty.setLeadingLines(pending);
            return empty;
        }

        char firstChar = cursor.firstChar();
        YamlNode result;
        if (firstChar == '-') {
            result = session.list().parse(0, pending);
        } else if (firstChar == '{' || firstChar == '[') {
            result = session.flow(cursor.trimmedContent().toString(), firstChar);
            cursor.advance();
            if (!pending.isEmpty()) result.setLeadingLines(pending);
            List<AdjacentLine> trailing = session.skipBlanksAndComments();
            if (!trailing.isEmpty()) result.setTrailingLines(trailing);
        } else {
            result = session.map().parse(0, pending);
        }

        List<YamlNode> anchored = session.anchored();
        List<MapNode.MapEntry> mergeEntries = session.mergeEntries();
        if (anchored != null && mergeEntries != null) {
            AnchorResolver.resolve(anchored, mergeEntries);
        }
        return result;
    }

    private static boolean isBlank(@NotNull String yaml) {
        for (int i = 0; i < yaml.length(); i++) {
            char c = yaml.charAt(i);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') return false;
        }
        return true;
    }
}
