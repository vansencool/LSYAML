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
     * Scans for and strips inline comments before parsing the value.
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

        ScalarNode scalar = parseScalarRaw(value);

        if (inlineComment != null) {
            scalar.setInlineComment(inlineComment);
        }

        return scalar;
    }

    /**
     * Parses a scalar value that has already had inline comments stripped.
     * Use this when the caller has already handled comment extraction to avoid
     * redundant scanning.
     *
     * @param value the scalar value string with no inline comment
     * @return the parsed ScalarNode
     */
    @NotNull
    public ScalarNode parseScalarRaw(@NotNull String value) {
        int len = value.length();
        if (len >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(len - 1);
            if (first == '\'' && last == '\'') {
                String content = value.substring(1, len - 1);
                if (content.indexOf('\'') >= 0) {
                    content = content.replace("''", "'");
                }
                ScalarNode scalar = new ScalarNode(content, ScalarStyle.SINGLE_QUOTED);
                scalar.setRawValue(value);
                return scalar;
            }
            if (first == '"' && last == '"') {
                String inner = value.substring(1, len - 1);
                String content = inner.indexOf('\\') >= 0 ? ParserUtils.unescapeDoubleQuoted(inner) : inner;
                ScalarNode scalar = new ScalarNode(content, ScalarStyle.DOUBLE_QUOTED);
                scalar.setRawValue(value);
                return scalar;
            }
        }
        Object parsed = parseUnquotedValue(value);
        ScalarNode scalar = new ScalarNode(parsed, ScalarStyle.PLAIN);
        scalar.setRawValue(value);
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
        int len = value.length();
        if (len == 0) return null;

        char first = value.charAt(0);

        if (len == 1 && first == '~') return null;

        if (len <= 5) {
            if (len == 4 && (first == 'n' || first == 'N') && "null".equalsIgnoreCase(value)) return null;
            if (len == 4 && (first == 't' || first == 'T') && "true".equalsIgnoreCase(value)) return true;
            if (len == 5 && (first == 'f' || first == 'F') && "false".equalsIgnoreCase(value)) return false;
            if (len == 3 && (first == 'y' || first == 'Y') && "yes".equalsIgnoreCase(value)) return true;
            if (len == 2 && (first == 'n' || first == 'N') && "no".equalsIgnoreCase(value)) return false;
            if (len == 2 && (first == 'o' || first == 'O') && "on".equalsIgnoreCase(value)) return true;
            if (len == 3 && (first == 'o' || first == 'O') && "off".equalsIgnoreCase(value)) return false;
        }

        if (first != '+' && first != '-' && first != '.' && (first < '0' || first > '9')) {
            return value;
        }

        if (first == '0' && len > 1) {
            char second = value.charAt(1);
            if (second == 'x' || second == 'X') {
                if (isValidHex(value, 2)) {
                    try {
                        return Long.parseLong(value.substring(2), 16);
                    } catch (NumberFormatException e) {
                        return value;
                    }
                }
                return value;
            }
            if (second == 'o' || second == 'O') {
                if (isValidOctal(value, 2)) {
                    try {
                        return Long.parseLong(value.substring(2), 8);
                    } catch (NumberFormatException e) {
                        return value;
                    }
                }
                return value;
            }
        }

        boolean hasDecimalOrExp = false;
        boolean valid = true;
        int start = (first == '+' || first == '-') ? 1 : 0;
        for (int i = start; i < len; i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') continue;
            if (c == '.') { hasDecimalOrExp = true; continue; }
            if (c == 'e' || c == 'E') { hasDecimalOrExp = true; continue; }
            if ((c == '+' || c == '-') && i > start) continue;
            valid = false;
            break;
        }

        if (!valid) {
            return value;
        }

        try {
            if (hasDecimalOrExp) {
                return Double.parseDouble(value);
            }
            long l = Long.parseLong(value);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            }
            return l;
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private static boolean isValidHex(@NotNull String value, int from) {
        if (from >= value.length()) return false;
        for (int i = from; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) continue;
            return false;
        }
        return true;
    }

    private static boolean isValidOctal(@NotNull String value, int from) {
        if (from >= value.length()) return false;
        for (int i = from; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '7') continue;
            return false;
        }
        return true;
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
