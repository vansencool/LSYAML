package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.parser.text.Slice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detection of a leading tag prefix within a value.
 */
public final class Tags {

    private Tags() {
    }

    /**
     * Returns the tag prefixing a value, or null when none is present.
     */
    public static @Nullable String leading(@NotNull Slice value) {
        char[] chars = value.array();
        int start = value.start();
        int end = value.end();
        if (start >= end || chars[start] != '!') return null;
        int i = start + 1;
        while (i < end && chars[i] != ' ' && chars[i] != '\t') i++;
        return new String(chars, start, i - start);
    }

    /**
     * Narrows the value past a leading tag and following whitespace and returns it.
     */
    public static @NotNull Slice withoutLeading(@NotNull Slice value) {
        char[] chars = value.array();
        int start = value.start();
        int end = value.end();
        int i = start + 1;
        while (i < end && chars[i] != ' ' && chars[i] != '\t') i++;
        while (i < end && (chars[i] == ' ' || chars[i] == '\t')) i++;
        return value.sub(i - start);
    }
}
