package net.vansencool.lsyaml;

import net.vansencool.lsyaml.builder.ListBuilder;
import net.vansencool.lsyaml.builder.MapBuilder;
import net.vansencool.lsyaml.builder.ScalarBuilder;
import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.exceptions.YamlParseException;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.parser.ParseOptions;
import net.vansencool.lsyaml.parser.ParseResult;
import net.vansencool.lsyaml.parser.YamlParsing;
import net.vansencool.lsyaml.writer.YamlWriter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * LSYAML - A fast, format-preserving YAML parser and generator.
 * <p>
 * This is the main entry point for the LSYAML library. It provides static methods
 * for parsing, writing, and building YAML documents. The library is designed to be easy to use while also offering advanced features for handling complex YAML structures and preserving formatting.
 * </p>
 *
 * <h2>Parsing YAML</h2>
 * <pre>{@code
 * MapNode map = LSYAML.parse(yamlString);
 * ListNode list = LSYAML.parseList(yamlString);
 * ScalarNode scalar = LSYAML.parseScalar(yamlString);
 * YamlNode any = LSYAML.parseAny(yamlString);
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

    private static final YamlWriter DEFAULT_WRITER = new YamlWriter();

    private LSYAML() {
    }

    /**
     * Parses a YAML string expecting a map as root.
     *
     * @param yaml the YAML content
     * @return the root MapNode
     */
    public static @NotNull MapNode parse(@NotNull String yaml) {
        YamlNode node = YamlParsing.parse(yaml, ParseOptions.defaults());
        if (node instanceof MapNode) {
            return (MapNode) node;
        }
        throw new IllegalStateException("Expected a map but got " + node.getType());
    }

    /**
     * Parses a YAML string with custom options, expecting a map as root.
     *
     * @param yaml    the YAML content
     * @param options parse options
     * @return the root MapNode
     */
    public static @NotNull MapNode parse(@NotNull String yaml, @NotNull ParseOptions options) {
        YamlNode node = YamlParsing.parse(yaml, options);
        if (node instanceof MapNode) {
            return (MapNode) node;
        }
        throw new IllegalStateException("Expected a map but got " + node.getType());
    }

    /**
     * Parses a YAML string into any node type (map, list, or scalar).
     *
     * @param yaml the YAML content
     * @return the root node
     */
    public static @NotNull YamlNode parseAny(@NotNull String yaml) {
        return YamlParsing.parse(yaml, ParseOptions.defaults());
    }

    /**
     * Parses a YAML string with custom options into any node type.
     *
     * @param yaml    the YAML content
     * @param options parse options
     * @return the root node
     */
    public static @NotNull YamlNode parseAny(@NotNull String yaml, @NotNull ParseOptions options) {
        return YamlParsing.parse(yaml, options);
    }

    /**
     * Parses a YAML string and returns detailed results with all issues.
     * Use this method when you want to inspect parse warnings/errors without exceptions.
     *
     * @param yaml the YAML content
     * @return the parse result with node and any issues
     */
    public static @NotNull ParseResult parseDetailed(@NotNull String yaml) {
        return parseDetailed(yaml, ParseOptions.defaults());
    }

    /**
     * Parses a YAML string and returns detailed results with custom options.
     *
     * @param yaml    the YAML content
     * @param options parse options
     * @return the parse result with node and any issues
     */
    public static @NotNull ParseResult parseDetailed(@NotNull String yaml, @NotNull ParseOptions options) {
        List<Diagnostic> issues = new ArrayList<>();
        try {
            YamlNode node = YamlParsing.parseDetailed(yaml, options, issues);
            return issues.isEmpty() ? ParseResult.success(node) : ParseResult.withIssues(node, issues);
        } catch (YamlParseException e) {
            return ParseResult.failure(issues);
        }
    }

    /**
     * Parses a YAML string expecting a list as root.
     *
     * @param yaml the YAML content
     * @return the root ListNode
     */
    public static @NotNull ListNode parseList(@NotNull String yaml) {
        YamlNode node = parseAny(yaml);
        if (node instanceof ListNode) {
            return (ListNode) node;
        }
        throw new IllegalStateException("Expected a list but got " + node.getType());
    }

    /**
     * Parses a YAML string expecting a scalar as root.
     *
     * @param yaml the YAML content
     * @return the root ScalarNode
     */
    public static @NotNull ScalarNode parseScalar(@NotNull String yaml) {
        YamlNode node = parseAny(yaml);
        if (node instanceof ScalarNode) {
            return (ScalarNode) node;
        }
        throw new IllegalStateException("Expected a scalar but got " + node.getType());
    }

    /**
     * Parses a YAML file expecting a map as root.
     * This is the default file parse method.
     *
     * @param path the path to the YAML file
     * @return the root MapNode
     */
    public static @NotNull MapNode parseFile(@NotNull Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return parse(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path, e);
        }
    }

    /**
     * Parses a YAML file into any node type (map, list, or scalar).
     * Use this when the root type is not known.
     *
     * @param path the path to the YAML file
     * @return the root node
     */
    public static @NotNull YamlNode parseAnyFile(@NotNull Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return parseAny(content);
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
    public static @NotNull ListNode parseListFile(@NotNull Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return parseList(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path, e);
        }
    }

    /**
     * Parses a YAML file expecting a scalar as root.
     *
     * @param path the path to the YAML file
     * @return the root ScalarNode
     */
    public static @NotNull ScalarNode parseScalarFile(@NotNull Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return parseScalar(content);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file: " + path, e);
        }
    }

    /**
     * Parses a YAML file expecting a map as root.
     * Convenience method equivalent to {@link #parseFile(Path)}.
     *
     * @param path the path to the YAML file
     * @return the root MapNode
     */
    public static @NotNull MapNode parseMapFile(@NotNull Path path) {
        return parseFile(path);
    }

    /**
     * Serializes a YAML node to a string using the default {@link YamlWriter} settings.
     * <p>
     * The default settings are: 2-space indentation, and preservation of comments, empty
     * lines, and quote styles all enabled. For output with different settings, obtain a
     * configured writer via {@link #writer()} and call {@link YamlWriter#write(YamlNode)}
     * directly:
     * </p>
     * <pre>{@code
     * String yaml = LSYAML.writer()
     *     .indentSize(4)
     *     .preserveComments(false)
     *     .write(node);
     * }</pre>
     * <p>
     * Unlike {@link YamlNode#toYaml()}, which uses serialization
     * logic embedded directly in each node class, this method delegates to a standalone
     * {@link YamlWriter} whose behaviour can be fully configured.
     * </p>
     *
     * @param node the node to serialize
     * @return the YAML string
     */
    public static @NotNull String write(@NotNull YamlNode node) {
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
    public static @NotNull YamlWriter writer() {
        return new YamlWriter();
    }

    /**
     * Creates a new MapBuilder for building map nodes.
     *
     * @return a new MapBuilder
     */
    public static @NotNull MapBuilder map() {
        return new MapBuilder();
    }

    /**
     * Creates a new ListBuilder for building list nodes.
     *
     * @return a new ListBuilder
     */
    public static @NotNull ListBuilder list() {
        return new ListBuilder();
    }

    /**
     * Creates a new ScalarBuilder for building scalar nodes.
     *
     * @return a new ScalarBuilder
     */
    public static @NotNull ScalarBuilder scalar() {
        return new ScalarBuilder();
    }

    /**
     * Creates an empty MapNode.
     *
     * @return a new empty MapNode
     */
    public static @NotNull MapNode emptyMap() {
        return new MapNode();
    }

    /**
     * Creates an empty ListNode.
     *
     * @return a new empty ListNode
     */
    public static @NotNull ListNode emptyList() {
        return new ListNode();
    }

    /**
     * Merges two maps, with values from the second map taking precedence.
     *
     * @param base     the base map
     * @param override the override map
     * @return a new merged MapNode
     */
    public static @NotNull MapNode merge(@NotNull MapNode base, @NotNull MapNode override) {
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
    public static @NotNull YamlNode copy(@NotNull YamlNode node) {
        return node.copy();
    }
}
