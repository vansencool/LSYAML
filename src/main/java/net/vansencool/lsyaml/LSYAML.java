package net.vansencool.lsyaml;

import net.vansencool.lsyaml.builder.ListBuilder;
import net.vansencool.lsyaml.builder.MapBuilder;
import net.vansencool.lsyaml.builder.ScalarBuilder;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.ParseOptions;
import net.vansencool.lsyaml.parser.ParseResult;
import net.vansencool.lsyaml.exceptions.YamlParseException;
import net.vansencool.lsyaml.parser.YamlParser;
import net.vansencool.lsyaml.writer.YamlWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * LSYAML - A fast, format-preserving YAML parser and generator.
 * <p>
 * This is the main entry point for the LSYAML library. It provides static methods
 * for parsing, writing, and building YAML documents. The library is designed to be easy to use while also offering advanced features for handling complex YAML structures and preserving formatting.
 * </p>
 *
 * <h2>Parsing YAML</h2>
 * <pre>{@code
 * YamlNode node = LSYAML.parse(yamlString);
 * MapNode map = LSYAML.parseMap(yamlString);
 * ListNode list = LSYAML.parseList(yamlString);
 * }</pre>
 *
 * <h2>Building YAML</h2>
 * <pre>{@code
 * MapNode config = LSYAML.map()
 *     .put("name", "LSYAML")
 *     .put("version", "1.0.0")
 *     .entry("database")
 *         .comment(" Database configuration")
 *         .value(LSYAML.map()
 *             .put("host", "localhost")
 *             .put("port", 3306))
 *     .build();
 * }</pre>
 *
 * <h2>Writing YAML</h2>
 * <pre>{@code
 * String yaml = LSYAML.write(node);
 * LSYAML.writeToFile(node, Path.of("config.yaml"));
 * }</pre>
 */
@SuppressWarnings("unused")
public final class LSYAML {

    private static final ThreadLocal<YamlParser> PARSER = ThreadLocal.withInitial(YamlParser::new);
    private static final YamlWriter DEFAULT_WRITER = new YamlWriter();

    private LSYAML() {
    }

    /**
     * Parses a YAML string into a node tree.
     *
     * @param yaml the YAML content
     * @return the root node (MapNode or ListNode)
     */
    @NotNull
    public static YamlNode parse(@NotNull String yaml) {
        return PARSER.get().parse(yaml);
    }

    /**
     * Parses a YAML string with custom options.
     *
     * @param yaml the YAML content
     * @param options parse options (use ParseOptions.lenient() to disable strict mode)
     * @return the root node
     */
    @NotNull
    public static YamlNode parse(@NotNull String yaml, @NotNull ParseOptions options) {
        return PARSER.get().parseWithOptions(yaml, options);
    }

    /**
     * Parses a YAML string and returns detailed results with all issues.
     * Use this method when you want to inspect parse warnings/errors without exceptions.
     *
     * @param yaml the YAML content
     * @return the parse result with node and any issues
     */
    @NotNull
    public static ParseResult parseDetailed(@NotNull String yaml) {
        return PARSER.get().parseDetailed(yaml, ParseOptions.defaults());
    }

    /**
     * Parses a YAML string and returns detailed results with custom options.
     *
     * @param yaml the YAML content
     * @param options parse options
     * @return the parse result with node and any issues
     */
    @NotNull
    public static ParseResult parseDetailed(@NotNull String yaml, @NotNull ParseOptions options) {
        return PARSER.get().parseDetailed(yaml, options);
    }

    /**
     * Parses a YAML string expecting a map as root.
     *
     * @param yaml the YAML content
     * @return the root MapNode
     */
    @NotNull
    public static MapNode parseMap(@NotNull String yaml) {
        YamlNode node = parse(yaml);
        if (node instanceof MapNode) {
            return (MapNode) node;
        }
        throw new YamlParseException("Expected a map but got " + node.getType());
    }

    /**
     * Parses a YAML string expecting a list as root.
     *
     * @param yaml the YAML content
     * @return the root ListNode
     */
    @NotNull
    public static ListNode parseList(@NotNull String yaml) {
        YamlNode node = parse(yaml);
        if (node instanceof ListNode) {
            return (ListNode) node;
        }
        throw new YamlParseException("Expected a list but got " + node.getType());
    }

    /**
     * Parses a YAML file into a node tree.
     *
     * @param path the path to the YAML file
     * @return the root node
     */
    @NotNull
    public static YamlNode parseFile(@NotNull Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return parse(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path, e);
        }
    }

    /**
     * Parses a YAML file expecting a map as root.
     *
     * @param path the path to the YAML file
     * @return the root MapNode
     */
    @NotNull
    public static MapNode parseMapFile(@NotNull Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return parseMap(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path, e);
        }
    }

    /**
     * Parses a YAML file expecting a list as root.
     *
     * @param path the path to the YAML file
     * @return the root ListNode
     */
    @NotNull
    public static ListNode parseListFile(@NotNull Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return parseList(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path, e);
        }
    }

    /**
     * Writes a YAML node to a string.
     *
     * @param node the node to write
     * @return the YAML string
     */
    @NotNull
    public static String write(@NotNull YamlNode node) {
        return DEFAULT_WRITER.write(node);
    }

    /**
     * Writes a YAML node to a file.
     *
     * @param node the node to write
     * @param path the path to write to
     */
    public static void writeToFile(@NotNull YamlNode node, @NotNull Path path) {
        try {
            String yaml = write(node);
            Files.writeString(path, yaml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write file: " + path, e);
        }
    }

    /**
     * Creates a new YAML writer with custom options.
     *
     * @return a new YamlWriter
     */
    @NotNull
    public static YamlWriter writer() {
        return new YamlWriter();
    }

    /**
     * Creates a new YAML parser.
     *
     * @return a new YamlParser
     */
    @NotNull
    public static YamlParser parser() {
        return new YamlParser();
    }

    /**
     * Creates a new MapBuilder for building map nodes.
     *
     * @return a new MapBuilder
     */
    @NotNull
    public static MapBuilder map() {
        return new MapBuilder();
    }

    /**
     * Creates a new ListBuilder for building list nodes.
     *
     * @return a new ListBuilder
     */
    @NotNull
    public static ListBuilder list() {
        return new ListBuilder();
    }

    /**
     * Creates a new ScalarBuilder for building scalar nodes.
     *
     * @return a new ScalarBuilder
     */
    @NotNull
    public static ScalarBuilder scalar() {
        return new ScalarBuilder();
    }

    /**
     * Creates an empty MapNode.
     *
     * @return a new empty MapNode
     */
    @NotNull
    public static MapNode emptyMap() {
        return new MapNode();
    }

    /**
     * Creates an empty ListNode.
     *
     * @return a new empty ListNode
     */
    @NotNull
    public static ListNode emptyList() {
        return new ListNode();
    }

    /**
     * Merges two maps, with values from the second map taking precedence.
     *
     * @param base the base map
     * @param override the override map
     * @return a new merged MapNode
     */
    @NotNull
    public static MapNode merge(@NotNull MapNode base, @NotNull MapNode override) {
        MapNode result = (MapNode) base.copy();

        for (MapNode.MapEntry entry : override.entries()) {
            YamlNode baseValue = result.get(entry.getKey());
            YamlNode overrideValue = entry.getValue();

            if (baseValue instanceof MapNode && overrideValue instanceof MapNode) {
                result.put(entry.getKey(), merge((MapNode) baseValue, (MapNode) overrideValue));
            } else {
                result.putEntry(entry.copy());
            }
        }

        return result;
    }

    /**
     * Creates a deep copy of a node.
     *
     * @param node the node to copy
     * @return a deep copy
     */
    @NotNull
    public static YamlNode copy(@NotNull YamlNode node) {
        return node.copy();
    }
}
