package net.vansencool.lsyaml.parser.text;

import net.vansencool.lsyaml.parser.source.Source;

/**
 * Pure quote-aware and whitespace scanners over source offsets that allocate nothing.
 */
public final class Scan {

    /**
     * Sentinel returned when a scanned feature is absent.
     */
    public static final int NONE = -1;

    private Scan() {
    }

    /**
     * Returns the index after the last non-whitespace character in a span.
     */
    public static int trimEnd(Source src, int start, int end) {
        int i = end;
        while (i > start) {
            char c = src.charAt(i - 1);
            if (c != ' ' && c != '\t') break;
            i--;
        }
        return i;
    }

    /**
     * Returns the index after the hash of a standalone comment in a span, or NONE.
     */
    public static int standaloneHash(Source src, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = src.charAt(i);
            if (c == ' ' || c == '\t') continue;
            if (c == '#') return i + 1;
            break;
        }
        return NONE;
    }

    /**
     * Returns whether a character is a YAML word character.
     */
    public static boolean isWord(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }
}
