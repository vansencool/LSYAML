package net.vansencool.lsyaml.writer;

import net.vansencool.lsyaml.metadata.CollectionStyle;
import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;

/**
 * YAML writer with configurable formatting options.
 */
@SuppressWarnings("unused")
public class YamlWriter {

    private int indentSize;
    private boolean preserveComments;
    private boolean preserveEmptyLines;
    private boolean preserveQuoteStyles;

    /**
     * Creates a new YamlWriter with default settings.
     * Default indent size is 2 spaces, and all preservation options are enabled.
     */
    public YamlWriter() {
        this.indentSize = 2;
        this.preserveComments = true;
        this.preserveEmptyLines = true;
        this.preserveQuoteStyles = true;
    }

    /**
     * Sets the indentation size in spaces.
     *
     * @param size the indent size
     * @return this writer
     */
    @NotNull
    public YamlWriter indentSize(int size) {
        this.indentSize = Math.max(1, size);
        return this;
    }

    /**
     * Sets whether to preserve comments in output.
     *
     * @param preserve true to preserve
     * @return this writer
     */
    @NotNull
    public YamlWriter preserveComments(boolean preserve) {
        this.preserveComments = preserve;
        return this;
    }

    /**
     * Sets whether to preserve empty lines in output.
     *
     * @param preserve true to preserve
     * @return this writer
     */
    @NotNull
    public YamlWriter preserveEmptyLines(boolean preserve) {
        this.preserveEmptyLines = preserve;
        return this;
    }

    /**
     * Sets whether to preserve original quote styles.
     *
     * @param preserve true to preserve
     * @return this writer
     */
    @NotNull
    public YamlWriter preserveQuoteStyles(boolean preserve) {
        this.preserveQuoteStyles = preserve;
        return this;
    }

    /**
     * Serializes a YAML node to a string using this writer's current settings.
     * <p>
     * This is the configurable counterpart to {@link YamlNode#toYaml()}. While
     * {@code toYaml()} uses serialization logic baked into each node class with a fixed
     * 2-space indent and all preservation options hard-wired on, this method respects the
     * {@link #indentSize(int)}, {@link #preserveComments(boolean)},
     * {@link #preserveEmptyLines(boolean)}, and {@link #preserveQuoteStyles(boolean)}
     * settings on this instance.
     * </p>
     *
     * @param node the node to serialize
     * @return the YAML string
     */
    @NotNull
    public String write(@NotNull YamlNode node) {
        StringBuilder sb = new StringBuilder();
        writeNode(sb, node, 0, true);
        return sb.toString();
    }

    private void writeNode(@NotNull StringBuilder sb, @NotNull YamlNode node, int level, boolean isRoot) {
        if (node instanceof MapNode) {
            writeMap(sb, (MapNode) node, level, isRoot);
        } else if (node instanceof ListNode) {
            writeList(sb, (ListNode) node, level, isRoot);
        } else if (node instanceof ScalarNode) {
            writeScalar(sb, (ScalarNode) node);
        }
    }

    private void writeMap(@NotNull StringBuilder sb, @NotNull MapNode map, int level, boolean isRoot) {
        String indent = " ".repeat(indentSize * level);

        if (preserveEmptyLines && map.getEmptyLinesBefore() > 0 && !isRoot) {
            sb.append("\n".repeat(Math.max(0, map.getEmptyLinesBefore())));
        }

        if (preserveComments) {
            for (String comment : map.getCommentsBefore()) {
                if (!isRoot || !sb.isEmpty()) {
                    sb.append(indent);
                }
                sb.append("#").append(comment).append("\n");
            }
        }

        if (map.getMetadata().hasAnchor()) {
            sb.append("&").append(map.getMetadata().getAnchor()).append(" ");
        }

        if (map.getStyle() == CollectionStyle.FLOW) {
            writeFlowMap(sb, map);
            if (preserveComments && map.getInlineComment() != null) {
                sb.append(" #").append(map.getInlineComment());
            }
            return;
        }

        boolean first = true;
        for (MapNode.MapEntry entry : map.entries()) {
            if (!first || !isRoot) {
                sb.append("\n");
            }
            first = false;

            if (preserveEmptyLines && entry.getEmptyLinesBefore() > 0) {
                sb.append("\n".repeat(Math.max(0, entry.getEmptyLinesBefore())));
            }

            if (preserveComments) {
                for (String comment : entry.getCommentsBefore()) {
                    sb.append(indent).append("#").append(comment).append("\n");
                }
            }

            sb.append(indent);

            if (entry.hasComplexKey()) {
                sb.append("? ");
                //noinspection DataFlowIssue
                writeNode(sb, entry.getComplexKey(), level + 1, false);
                sb.append("\n").append(indent).append(":");
            } else {
                sb.append(formatKey(entry.getKey(), entry.getKeyStyle())).append(":");
            }

            YamlNode value = entry.getValue();

            if (value instanceof MapNode || value instanceof ListNode) {
                if (preserveComments && entry.getInlineComment() != null) {
                    sb.append(" #").append(entry.getInlineComment());
                }
                writeNode(sb, value, level + 1, false);
            } else {
                sb.append(" ");
                writeNode(sb, value, level + 1, false);
                if (preserveComments && entry.getInlineComment() != null) {
                    sb.append(" #").append(entry.getInlineComment());
                }
            }
        }
    }

    private void writeFlowMap(@NotNull StringBuilder sb, @NotNull MapNode map) {
        sb.append("{");
        boolean first = true;
        for (MapNode.MapEntry entry : map.entries()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(formatKey(entry.getKey(), entry.getKeyStyle())).append(": ");
            writeFlowValue(sb, entry.getValue());
        }
        sb.append("}");
    }

    private void writeList(@NotNull StringBuilder sb, @NotNull ListNode list, int level, boolean isRoot) {
        String indent = " ".repeat(indentSize * level);

        if (preserveEmptyLines && list.getEmptyLinesBefore() > 0 && !isRoot) {
            sb.append("\n".repeat(Math.max(0, list.getEmptyLinesBefore())));
        }

        if (preserveComments) {
            for (String comment : list.getCommentsBefore()) {
                sb.append(indent).append("#").append(comment).append("\n");
            }
        }

        if (list.getMetadata().hasAnchor()) {
            sb.append("&").append(list.getMetadata().getAnchor()).append(" ");
        }

        if (list.getStyle() == CollectionStyle.FLOW) {
            writeFlowList(sb, list);
            if (preserveComments && list.getInlineComment() != null) {
                sb.append(" #").append(list.getInlineComment());
            }
            return;
        }

        for (ListNode.ListEntry entry : list.entries()) {
            sb.append("\n");

            if (preserveEmptyLines && entry.getEmptyLinesBefore() > 0) {
                sb.append("\n".repeat(Math.max(0, entry.getEmptyLinesBefore())));
            }

            if (preserveComments) {
                for (String comment : entry.getCommentsBefore()) {
                    sb.append(indent).append("#").append(comment).append("\n");
                }
            }

            sb.append(indent).append("-");

            YamlNode value = entry.getValue();

            if (value instanceof MapNode || value instanceof ListNode) {
                if (preserveComments && entry.getInlineComment() != null) {
                    sb.append(" #").append(entry.getInlineComment());
                }
                writeNode(sb, value, level + 1, false);
            } else {
                sb.append(" ");
                writeNode(sb, value, level + 1, false);
                if (preserveComments && entry.getInlineComment() != null) {
                    sb.append(" #").append(entry.getInlineComment());
                }
            }
        }
    }

    private void writeFlowList(@NotNull StringBuilder sb, @NotNull ListNode list) {
        sb.append("[");
        boolean first = true;
        for (ListNode.ListEntry entry : list.entries()) {
            if (!first) sb.append(", ");
            first = false;
            writeFlowValue(sb, entry.getValue());
        }
        sb.append("]");
    }

    private void writeScalar(@NotNull StringBuilder sb, @NotNull ScalarNode scalar) {
        if (scalar.getMetadata().isAlias()) {
            sb.append("*").append(scalar.getMetadata().getAlias());
            return;
        }

        if (scalar.getMetadata().hasAnchor()) {
            sb.append("&").append(scalar.getMetadata().getAnchor()).append(" ");
        }

        if (scalar.getTag() != null) {
            sb.append(scalar.getTag()).append(" ");
        }

        sb.append(formatScalarValue(scalar));

        if (preserveComments && scalar.getInlineComment() != null) {
            sb.append(" #").append(scalar.getInlineComment());
        }
    }

    private void writeFlowValue(@NotNull StringBuilder sb, @NotNull YamlNode node) {
        if (node instanceof MapNode) {
            writeFlowMap(sb, (MapNode) node);
        } else if (node instanceof ListNode) {
            writeFlowList(sb, (ListNode) node);
        } else if (node instanceof ScalarNode) {
            sb.append(formatScalarValue((ScalarNode) node));
        }
    }

    @NotNull
    private String formatKey(@NotNull String key, @NotNull ScalarStyle style) {
        if (!preserveQuoteStyles) {
            if (needsQuoting(key)) {
                return "\"" + escapeDoubleQuoted(key) + "\"";
            }
            return key;
        }

        return switch (style) {
            case SINGLE_QUOTED -> "'" + key.replace("'", "''") + "'";
            case DOUBLE_QUOTED -> "\"" + escapeDoubleQuoted(key) + "\"";
            default -> {
                if (needsQuoting(key)) {
                    yield "\"" + escapeDoubleQuoted(key) + "\"";
                }
                yield key;
            }
        };
    }

    @NotNull
    private String formatScalarValue(@NotNull ScalarNode scalar) {
        Object value = scalar.getValue();

        if (value == null) {
            return "null";
        }

        String strValue = value.toString();
        ScalarStyle style = preserveQuoteStyles ? scalar.getStyle() : ScalarStyle.PLAIN;

        return switch (style) {
            case SINGLE_QUOTED -> "'" + strValue.replace("'", "''") + "'";
            case DOUBLE_QUOTED -> "\"" + escapeDoubleQuoted(strValue) + "\"";
            case LITERAL -> formatLiteralBlock(strValue);
            case FOLDED -> formatFoldedBlock(strValue);
            default -> {
                if (needsQuoting(strValue) && !(value instanceof Number) && !(value instanceof Boolean)) {
                    yield "\"" + escapeDoubleQuoted(strValue) + "\"";
                }
                yield strValue;
            }
        };
    }

    @NotNull
    private String escapeDoubleQuoted(@NotNull String str) {
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
        if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str)) return true;
        if ("null".equalsIgnoreCase(str) || "~".equals(str)) return true;
        if ("yes".equalsIgnoreCase(str) || "no".equalsIgnoreCase(str)) return true;
        if ("on".equalsIgnoreCase(str) || "off".equalsIgnoreCase(str)) return true;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @NotNull
    private String formatLiteralBlock(@NotNull String str) {
        StringBuilder sb = new StringBuilder("|\n");
        for (String line : str.split("\n", -1)) {
            sb.append("  ").append(line).append("\n");
        }
        return sb.toString().stripTrailing();
    }

    @NotNull
    private String formatFoldedBlock(@NotNull String str) {
        StringBuilder sb = new StringBuilder(">\n");
        for (String line : str.split("\n", -1)) {
            sb.append("  ").append(line).append("\n");
        }
        return sb.toString().stripTrailing();
    }
}
