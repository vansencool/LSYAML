package net.vansencool.lsyaml.parser.util;

import net.vansencool.lsyaml.metadata.ScalarStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a parsed key-value pair from a YAML line.
 *
 * @param key           the key string
 * @param keyStyle      the scalar style of the key
 * @param value         the value string (may be empty)
 * @param inlineComment any inline comment on the line
 */
public record ParsedKey(@NotNull String key, @NotNull ScalarStyle keyStyle, @NotNull String value,
                        @Nullable String inlineComment) {
}
