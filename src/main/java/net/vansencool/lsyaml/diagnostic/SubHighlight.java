package net.vansencool.lsyaml.diagnostic;

import org.jetbrains.annotations.Nullable;

public record SubHighlight(int columnStart, int columnEnd, @Nullable String label) {
}
