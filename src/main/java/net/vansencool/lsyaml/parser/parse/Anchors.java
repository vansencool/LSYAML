package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.parser.text.Scan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Detection of anchor, alias, and anchor-only forms within a value string.
 */
public final class Anchors {

    private Anchors() {
    }

    /**
     * Returns the alias name of a pure alias value, or null when it is not one.
     */
    public static @Nullable String alias(@NotNull String value) {
        if (value.length() < 2 || value.charAt(0) != '*') return null;
        int i = 1;
        while (i < value.length() && Scan.isWord(value.charAt(i))) i++;
        if (i == 1) return null;
        for (int j = i; j < value.length(); j++) {
            char c = value.charAt(j);
            if (c != ' ' && c != '\t') return null;
        }
        return value.substring(1, i);
    }

    /**
     * Returns the anchor name when the value is an anchor with no trailing content, or null.
     */
    public static @Nullable String anchorOnly(@NotNull String value) {
        if (value.length() < 2 || value.charAt(0) != '&') return null;
        int i = 1;
        while (i < value.length() && Scan.isWord(value.charAt(i))) i++;
        if (i == 1) return null;
        for (int j = i; j < value.length(); j++) {
            char c = value.charAt(j);
            if (c != ' ' && c != '\t') return null;
        }
        return value.substring(1, i);
    }

    /**
     * Returns the anchor name prefixing a value, or null when none is present.
     */
    public static @Nullable String leading(@NotNull String value) {
        if (value.isEmpty() || value.charAt(0) != '&') return null;
        int end = 1;
        while (end < value.length() && Scan.isWord(value.charAt(end))) end++;
        return end > 1 ? value.substring(1, end) : null;
    }

    /**
     * Returns the value with a leading anchor and following whitespace removed.
     */
    public static @NotNull String withoutLeading(@NotNull String value) {
        int end = 1;
        while (end < value.length() && Scan.isWord(value.charAt(end))) end++;
        while (end < value.length() && value.charAt(end) == ' ') end++;
        return value.substring(end);
    }
}
