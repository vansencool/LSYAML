package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.metadata.NodeMetadata;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.type.NodeType;
import net.vansencool.lsyaml.node.type.ScalarType;
import net.vansencool.lsyaml.node.type.ScalarTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a scalar value in YAML (string, number, boolean, null).
 */
@SuppressWarnings({"unused", "DataFlowIssue"})
public class ScalarNode extends AbstractYamlNode {

    private @Nullable Object value;
    private @NotNull ScalarStyle style;
    private @Nullable String tag;
    private @Nullable ScalarType type;

    /**
     * Creates a new scalar node with null value and plain style.
     */
    public ScalarNode() {
        super();
        this.value = null;
        this.style = ScalarStyle.PLAIN;
        this.tag = null;
    }

    /**
     * Creates a new scalar node with the given value and plain style.
     *
     * @param value the scalar value (string, number, boolean, or null)
     */
    public ScalarNode(@Nullable Object value) {
        super();
        this.value = value;
        this.style = ScalarStyle.PLAIN;
        this.tag = null;
    }

    /**
     * Returns a scalar node for a user-supplied string, quoting it when a bare form would reparse as another type or break syntax.
     *
     * @param value the string value
     * @return the scalar node
     */
    public static @NotNull ScalarNode ofString(@NotNull String value) {
        return new ScalarNode(value, needsQuoting(value) ? ScalarStyle.DOUBLE_QUOTED : ScalarStyle.PLAIN);
    }

    private static boolean needsQuoting(@NotNull String str) {
        if (str.isEmpty()) return true;
        if (str.contains(": ") || str.contains(" #") || str.contains("\n")) return true;
        if (str.startsWith("&") || str.startsWith("*") || str.startsWith("!")) return true;
        if (str.startsWith("-") || str.startsWith("[") || str.startsWith("{")) return true;
        if (str.startsWith("'") || str.startsWith("\"")) return true;
        return ScalarTypes.of(str) != ScalarType.STRING;
    }

    /**
     * Creates a new scalar node with the given value and style.
     *
     * @param value the scalar value (string, number, boolean, or null)
     * @param style the quoting style to use when emitting this scalar
     */
    public ScalarNode(@Nullable Object value, @NotNull ScalarStyle style) {
        super();
        this.value = value;
        this.style = style;
        this.tag = null;
    }

    /**
     * Creates a new scalar node with the given value, style, and metadata.
     *
     * @param value    the scalar value
     * @param style    the quoting style to use when emitting this scalar
     * @param metadata the metadata for this node
     */
    public ScalarNode(@Nullable Object value, @NotNull ScalarStyle style, @NotNull NodeMetadata metadata) {
        super(metadata);
        this.value = value;
        this.style = style;
        this.tag = null;
    }

    @Override
    public @NotNull NodeType getType() {
        return NodeType.SCALAR;
    }

    /**
     * Returns the value of this scalar.
     *
     * @return the value, or null
     */
    public @Nullable Object getValue() {
        return value;
    }

    /**
     * Sets the value of this scalar.
     *
     * @param value the value to set
     */
    public void setValue(@Nullable Object value) {
        this.value = value;
        this.type = null;
    }

    /**
     * Returns the resolved scalar kind under the YAML 1.2 core schema, classified on first access.
     *
     * @return the scalar type
     */
    public @NotNull ScalarType type() {
        ScalarType cached = type;
        if (cached == null) {
            cached = ScalarTypes.of(value);
            type = cached;
        }
        return cached;
    }

    /**
     * Returns the value as a string.
     * For folded block scalars (>), newlines are replaced with spaces.
     *
     * @return string representation, or null if value is null
     */
    public @Nullable String getStringValue() {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        if (style == ScalarStyle.FOLDED) {
            return foldString(str);
        }
        return str;
    }

    private @NotNull String foldString(@NotNull String text) {
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n", -1);
        boolean previousWasBlank = false;

        for (String line : lines) {
            if (line.isEmpty()) {
                result.append("\n");
                previousWasBlank = true;
            } else {
                if (!result.isEmpty() && !previousWasBlank) {
                    result.append(" ");
                }
                result.append(line);
                previousWasBlank = false;
            }
        }

        return result.toString().trim();
    }

    /**
     * Returns the value as an integer.
     *
     * @return integer value
     * @throws NumberFormatException if value cannot be converted
     */
    public int getIntValue() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return parseInt(value.toString());
    }

    /**
     * Returns the value as a long.
     *
     * @return long value
     * @throws NumberFormatException if value cannot be converted
     */
    public long getLongValue() {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return parseLong(value.toString());
    }

    /**
     * Returns the value as a double.
     *
     * @return double value
     * @throws NumberFormatException if value cannot be converted
     */
    public double getDoubleValue() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    /**
     * Returns the value as a boolean.
     *
     * @return boolean value
     */
    public boolean getBooleanValue() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = value.toString().toLowerCase();
        return "true".equals(str) || "yes".equals(str) || "on".equals(str);
    }

    /**
     * Returns the value as a plain string.
     * Returns null if the value is null.
     *
     * @return the string representation of the value, or null
     */
    public @Nullable String getString() {
        return value != null ? value.toString() : null;
    }

    /**
     * Parses the value as an integer.
     *
     * @return the integer value, or null if the value is null or not parseable
     */
    public @Nullable Integer getInt() {
        String val = getString();
        if (val == null) return null;
        try {
            return parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses the value as a long.
     *
     * @return the long value, or null if the value is null or not parseable
     */
    public @Nullable Long getLong() {
        String val = getString();
        if (val == null) return null;
        try {
            return parseLong(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses the value as a double.
     *
     * @return the double value, or null if the value is null or not parseable
     */
    public @Nullable Double getDouble() {
        String val = getString();
        if (val == null) return null;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses the value as a boolean.
     * Recognises true/yes/on and false/no/off (case-insensitive).
     *
     * @return the boolean value, or null if the value is null or not a boolean
     */
    public @Nullable Boolean getBoolean() {
        String val = getString();
        if (val == null) return null;
        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("on")) {
            return true;
        }
        if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("no") || val.equalsIgnoreCase("off")) {
            return false;
        }
        return null;
    }

    /**
     * Checks if this scalar is null.
     *
     * @return true if value is null or "null"/"~"
     */
    public boolean isNull() {
        if (value == null) return true;
        String str = value.toString();
        return "null".equalsIgnoreCase(str) || "~".equals(str);
    }

    /**
     * Returns the quoting style.
     *
     * @return the scalar style
     */
    public @NotNull ScalarStyle getStyle() {
        return style;
    }

    /**
     * Sets the quoting style.
     *
     * @param style the style to set
     */
    public void setStyle(@NotNull ScalarStyle style) {
        this.style = style;
    }

    /**
     * Returns the YAML tag if specified.
     *
     * @return the tag, or null
     */
    public @Nullable String getTag() {
        return tag;
    }

    /**
     * Sets the YAML tag.
     *
     * @param tag the tag
     */
    public void setTag(@Nullable String tag) {
        this.tag = tag;
    }

    @Override
    public @NotNull YamlNode copy() {
        ScalarNode copy = new ScalarNode(value, style, metadata.copy());
        copy.tag = this.tag;
        copyCommentsTo(copy);
        return copy;
    }

    @Override
    public @NotNull String toYaml(int indent, int currentLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildCommentPrefix(indent, currentLevel));

        if (tag != null) {
            sb.append(tag).append(" ");
        }

        if (metadata.hasAnchor()) {
            sb.append("&").append(metadata.getAnchor()).append(" ");
        }

        if (metadata.isAlias()) {
            sb.append("*").append(metadata.getAlias());
        } else {
            sb.append(formatValue());
        }

        sb.append(buildInlineComment());
        return sb.toString();
    }

    private @NotNull String formatValue() {
        if (value == null) {
            return "null";
        }

        String strValue = value.toString();

        return switch (style) {
            case SINGLE_QUOTED -> "'" + strValue.replace("'", "''") + "'";
            case DOUBLE_QUOTED -> "\"" + escapeDoubleQuoted(strValue) + "\"";
            case LITERAL -> formatLiteralBlock(strValue);
            case FOLDED -> formatFoldedBlock(strValue);
            default -> strValue;
        };
    }

    private @NotNull String escapeDoubleQuoted(@NotNull String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static int parseInt(@NotNull String str) {
        if (str.length() > 2 && str.charAt(0) == '0') {
            char second = str.charAt(1);
            if (second == 'x' || second == 'X') return Integer.parseInt(str.substring(2), 16);
            if (second == 'o' || second == 'O') return Integer.parseInt(str.substring(2), 8);
        }
        return Integer.parseInt(str);
    }

    private static long parseLong(@NotNull String str) {
        if (str.length() > 2 && str.charAt(0) == '0') {
            char second = str.charAt(1);
            if (second == 'x' || second == 'X') return Long.parseLong(str.substring(2), 16);
            if (second == 'o' || second == 'O') return Long.parseLong(str.substring(2), 8);
        }
        return Long.parseLong(str);
    }

    private @NotNull String formatLiteralBlock(@NotNull String str) {
        StringBuilder sb = new StringBuilder("|\n");
        for (String line : str.split("\n", -1)) {
            sb.append("  ").append(line).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    private @NotNull String formatFoldedBlock(@NotNull String str) {
        StringBuilder sb = new StringBuilder(">\n");
        for (String line : str.split("\n", -1)) {
            sb.append("  ").append(line).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    @Override
    public String toString() {
        return "ScalarNode{value=" + value + ", style=" + style + "}";
    }
}
