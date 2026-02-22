package net.vansencool.lsyaml.parser;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the content of a flow-style collection that may span multiple lines.
 *
 * @param content the collected content string
 * @param multiLine whether the content spans multiple lines
 * @param indent the indentation level of the flow content
 */
public record FlowContent(@NotNull String content, boolean multiLine, int indent) {
}
