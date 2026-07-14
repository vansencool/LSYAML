package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.parser.text.Scan;
import net.vansencool.lsyaml.parser.text.Slice;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detection of anchor, alias, and anchor-only forms within a value.
 */
public final class Anchors {

    private Anchors() {
    }

    /**
     * Returns the alias name of a pure alias value, or null when it is not one.
     */
    public static @Nullable String alias(@NotNull Slice value) {
        char[] chars = value.array();
        int start = value.start();
        int end = value.end();
        if (end - start < 2 || chars[start] != '*') return null;
        int i = start + 1;
        while (i < end && Scan.isWord(chars[i])) i++;
        if (i == start + 1) return null;
        for (int j = i; j < end; j++) {
            char c = chars[j];
            if (c != ' ' && c != '\t') return null;
        }
        return new String(chars, start + 1, i - start - 1);
    }

    /**
     * Returns the anchor name when the value is an anchor with no trailing content, or null.
     */
    public static @Nullable String anchorOnly(@NotNull Slice value) {
        char[] chars = value.array();
        int start = value.start();
        int end = value.end();
        if (end - start < 2 || chars[start] != '&') return null;
        int i = start + 1;
        while (i < end && Scan.isWord(chars[i])) i++;
        if (i == start + 1) return null;
        for (int j = i; j < end; j++) {
            char c = chars[j];
            if (c != ' ' && c != '\t') return null;
        }
        return new String(chars, start + 1, i - start - 1);
    }

    /**
     * Returns the anchor name prefixing a value, or null when none is present.
     */
    public static @Nullable String leading(@NotNull Slice value) {
        char[] chars = value.array();
        int start = value.start();
        int end = value.end();
        if (start >= end || chars[start] != '&') return null;
        int i = start + 1;
        while (i < end && Scan.isWord(chars[i])) i++;
        return i > start + 1 ? new String(chars, start + 1, i - start - 1) : null;
    }

    /**
     * Narrows the value past a leading anchor and following whitespace and returns it.
     */
    public static @NotNull Slice withoutLeading(@NotNull Slice value) {
        char[] chars = value.array();
        int start = value.start();
        int end = value.end();
        int i = start + 1;
        while (i < end && Scan.isWord(chars[i])) i++;
        while (i < end && chars[i] == ' ') i++;
        return value.sub(i - start);
    }
}
