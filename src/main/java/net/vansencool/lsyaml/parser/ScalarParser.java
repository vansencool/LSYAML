package net.vansencool.lsyaml.parser;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import net.vansencool.lsyaml.node.ScalarNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parses scalar values from YAML content.
 * <p>
 * Handles plain scalars, single-quoted strings, double-quoted strings,
 * and block scalars (literal | and folded >).
 * </p>
 */
public class ScalarParser {

    /**
     * Parses a scalar value string into a ScalarNode.
     *
     * @param value the scalar value string
     * @return the parsed ScalarNode
     */
    @NotNull
    public ScalarNode parseScalar(@NotNull String value) {
        String inlineComment = ParserUtils.extractInlineComment(value);
        if (inlineComment != null) {
            value = ParserUtils.removeInlineComment(value);
        }

        ScalarNode scalar;

        // single-quoted strings - double single-quotes escape to single
        if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
            String content = value.substring(1, value.length() - 1).replace("''", "'");
            scalar = new ScalarNode(content, ScalarStyle.SINGLE_QUOTED);
            scalar.setRawValue(value);
        // double-quoted strings - support escape sequences
        } else if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            String content = ParserUtils.unescapeDoubleQuoted(value.substring(1, value.length() - 1));
            scalar = new ScalarNode(content, ScalarStyle.DOUBLE_QUOTED);
            scalar.setRawValue(value);
        } else {
            Object parsed = parseUnquotedValue(value);
            scalar = new ScalarNode(parsed, ScalarStyle.PLAIN);
            scalar.setRawValue(value);
        }

        if (inlineComment != null) {
            scalar.setInlineComment(inlineComment);
        }

        return scalar;
    }

    /**
     * Parses an unquoted value into its appropriate type.
     *
     * @param value the value string
     * @return the parsed value (null, Boolean, Integer, Long, Double, or String)
     */
    @Nullable
    public Object parseUnquotedValue(@NotNull String value) {
        // YAML null values
        if (value.isEmpty() || "null".equalsIgnoreCase(value) || "~".equals(value)) {
            return null;
        }

        // YAML boolean true values
        if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)) {
            return true;
        }

        // YAML boolean false values
        if ("false".equalsIgnoreCase(value) || "no".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value)) {
            return false;
        }

        // try parsing as number
        try {
            // floating point
            if (value.contains(".") || value.toLowerCase().contains("e")) {
                return Double.parseDouble(value);
            }
            // hexadecimal
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return Long.parseLong(value.substring(2), 16);
            }
            // octal
            if (value.startsWith("0o") || value.startsWith("0O")) {
                return Long.parseLong(value.substring(2), 8);
            }
            // integer - use int if fits, otherwise long
            long l = Long.parseLong(value);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            }
            return l;
        } catch (NumberFormatException e) {
            // not a number, return as string
            return value;
        }
    }

    /**
     * Parses a block scalar (literal | or folded >).
     *
     * @param ctx       the parse context
     * @param indicator the indicator line (| or >)
     * @param indent    the current indentation level
     * @return the parsed ScalarNode
     */
    @NotNull
    public ScalarNode parseBlockScalar(@NotNull ParseContext ctx, @NotNull String indicator, int indent) {
        boolean literal = indicator.charAt(0) == '|';
        ScalarStyle style = literal ? ScalarStyle.LITERAL : ScalarStyle.FOLDED;

        StringBuilder content = new StringBuilder();
        int contentIndent = -1;

        while (ctx.hasMoreLines()) {
            String line = ctx.currentLineContent();
            char firstChar = ctx.currentFirstChar();

            if (firstChar == 0) {
                content.append("\n");
                ctx.advanceLine();
                continue;
            }

            int lineIndent = ctx.currentIndent();

            if (contentIndent == -1) {
                if (lineIndent > indent) {
                    contentIndent = lineIndent;
                } else {
                    break;
                }
            }

            if (lineIndent < contentIndent) {
                break;
            }

            if (!content.isEmpty()) {
                content.append("\n");
            }
            content.append(line.substring(Math.min(contentIndent, line.length())));
            ctx.advanceLine();
        }

        return new ScalarNode(content.toString(), style);
    }
}
