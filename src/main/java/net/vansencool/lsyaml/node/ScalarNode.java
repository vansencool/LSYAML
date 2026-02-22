package net.vansencool.lsyaml.node;

import net.vansencool.lsyaml.metadata.NodeMetadata;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.type.NodeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a scalar value in YAML (string, number, boolean, null).
 */
@SuppressWarnings({"unused", "DataFlowIssue"})
public class ScalarNode extends AbstractYamlNode {

    private @Nullable Object value;
    private @NotNull ScalarStyle style;
    private @Nullable String rawValue;
    private @Nullable String tag;

    public ScalarNode() {
        super();
        this.value = null;
        this.style = ScalarStyle.PLAIN;
        this.rawValue = null;
        this.tag = null;
    }

    public ScalarNode(@Nullable Object value) {
        super();
        this.value = value;
        this.style = ScalarStyle.PLAIN;
        this.rawValue = null;
        this.tag = null;
    }

    public ScalarNode(@Nullable Object value, @NotNull ScalarStyle style) {
        super();
        this.value = value;
        this.style = style;
        this.rawValue = null;
        this.tag = null;
    }

    public ScalarNode(@Nullable Object value, @NotNull ScalarStyle style, @NotNull NodeMetadata metadata) {
        super(metadata);
        this.value = value;
        this.style = style;
        this.rawValue = null;
        this.tag = null;
    }

    @Override
    @NotNull
    public NodeType getType() {
        return NodeType.SCALAR;
    }

    /**
     * Returns the value of this scalar.
     *
     * @return the value, or null
     */
    @Nullable
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value of this scalar.
     *
     * @param value the value to set
     */
    public void setValue(@Nullable Object value) {
        this.value = value;
    }

    /**
     * Returns the value as a string.
     * For folded block scalars (>), newlines are replaced with spaces.
     *
     * @return string representation, or null if value is null
     */
    @Nullable
    public String getStringValue() {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        if (style == ScalarStyle.FOLDED) {
            return foldString(str);
        }
        return str;
    }

    @NotNull
    private String foldString(@NotNull String text) {
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
        return Integer.parseInt(value.toString());
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
        return Long.parseLong(value.toString());
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
    @NotNull
    public ScalarStyle getStyle() {
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
     * Returns the raw value string as it appeared in the source.
     *
     * @return the raw value, or null if generated
     */
    @Nullable
    public String getRawValue() {
        return rawValue;
    }

    /**
     * Sets the raw value string.
     *
     * @param rawValue the raw value
     */
    public void setRawValue(@Nullable String rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Returns the YAML tag if specified.
     *
     * @return the tag, or null
     */
    @Nullable
    public String getTag() {
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
    @NotNull
    public YamlNode copy() {
        ScalarNode copy = new ScalarNode(value, style, metadata.copy());
        copy.rawValue = this.rawValue;
        copy.tag = this.tag;
        copyCommentsTo(copy);
        return copy;
    }

    @Override
    @NotNull
    public String toYaml(int indent, int currentLevel) {
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
            default -> {
                if (needsQuoting(strValue)) {
                    yield "\"" + escapeDoubleQuoted(strValue) + "\"";
                }
                yield strValue;
            }
        };
    }

    private @NotNull String escapeDoubleQuoted(@NotNull String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private boolean needsQuoting(@NotNull String str) {
        if (str.isEmpty()) return true;
        if (str.contains(": ") || str.contains(" #") || str.contains("\n")) return true;
        if (str.startsWith("&") || str.startsWith("*") || str.startsWith("!")) return true;
        if (str.startsWith("-") || str.startsWith("[") || str.startsWith("{")) return true;
        if (str.startsWith("'") || str.startsWith("\"")) return true;
        if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) {
            return !(value instanceof Boolean);
        }
        if ("null".equalsIgnoreCase(str) || "~".equals(str)) {
            return value != null;
        }
        try {
            Double.parseDouble(str);
            return !(value instanceof Number);
        } catch (NumberFormatException e) {
            return false;
        }
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
