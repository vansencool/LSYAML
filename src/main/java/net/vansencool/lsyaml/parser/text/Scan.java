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

    /**
     * Returns the start-relative index of the first unquoted colon in a span, or NONE.
     */
    public static int unquotedColon(char[] chars, int start, int end) {
        boolean single = false;
        boolean dbl = false;
        for (int i = start; i < end; i++) {
            char c = chars[i];
            if (c == '\'' && !dbl) single = !single;
            else if (c == '"' && !single) dbl = !dbl;
            else if (c == ':' && !single && !dbl) return i - start;
        }
        return NONE;
    }

    /**
     * Returns the start-relative index of the first unquoted inline comment hash in a span, or NONE.
     */
    public static int inlineHash(char[] chars, int start, int end) {
        boolean single = false;
        boolean dbl = false;
        for (int i = start; i < end; i++) {
            char c = chars[i];
            if (c == '\'' && !dbl) single = !single;
            else if (c == '"' && !single) dbl = !dbl;
            else if (c == '#' && !single && !dbl && i > start && chars[i - 1] == ' ') return i - start;
        }
        return NONE;
    }

    /**
     * Returns whether a string holds a colon outside single or double quotes, ignoring a quoted first character.
     */
    public static boolean hasUnquotedColon(Slice value) {
        int len = value.length();
        if (len == 0) return false;
        char first = value.charAt(0);
        if (first == '\'' || first == '"') return false;
        boolean single = false;
        boolean dbl = false;
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c == '\'' && !dbl) single = !single;
            else if (c == '"' && !single) dbl = !dbl;
            else if (c == ':' && !single && !dbl) return true;
        }
        return false;
    }
}
