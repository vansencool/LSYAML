package net.vansencool.lsyaml.parser.text;

import org.jetbrains.annotations.NotNull;

/**
 * Shared string operations for materialized scalar and key leaves.
 */
public final class Strings {

    private Strings() {
    }

    /**
     * Returns a double-quoted inner string with escape sequences resolved.
     */
    public static @NotNull String unescape(@NotNull String str) {
        if (str.indexOf('\\') < 0) {
            return str;
        }
        return str.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
