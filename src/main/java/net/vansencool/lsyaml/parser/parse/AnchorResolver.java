package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Two-phase linker that binds merge keys to the maps named by their alias anchors.
 */
public final class AnchorResolver {

    private AnchorResolver() {
    }

    /**
     * Links merge keys within a node tree to their anchor targets.
     */
    public static void resolve(@NotNull YamlNode root) {
        Map<String, YamlNode> anchors = new HashMap<>();
        collect(root, anchors);
        link(root, anchors);
    }

    private static void collect(@NotNull YamlNode node, @NotNull Map<String, YamlNode> anchors) {
        String anchor = node.getMetadata().getAnchor();
        if (anchor != null && !anchor.isEmpty()) {
            anchors.put(anchor, node);
        }
        if (node instanceof MapNode mapNode) {
            for (MapNode.MapEntry entry : mapNode.entries()) {
                collect(entry.getValue(), anchors);
            }
        } else if (node instanceof ListNode listNode) {
            for (YamlNode item : listNode) {
                collect(item, anchors);
            }
        }
    }

    private static void link(@NotNull YamlNode node, @NotNull Map<String, YamlNode> anchors) {
        if (node instanceof MapNode mapNode) {
            MapNode.MapEntry mergeEntry = mapNode.getEntry("<<");
            if (mergeEntry != null && mergeEntry.getValue().getMetadata().isAlias()) {
                String aliasName = mergeEntry.getValue().getMetadata().getAlias();
                YamlNode target = anchors.get(aliasName);
                if (target instanceof MapNode resolvedMap) {
                    mergeEntry.setResolvedMergeMap(resolvedMap);
                }
            }
            for (MapNode.MapEntry entry : mapNode.entries()) {
                link(entry.getValue(), anchors);
            }
        } else if (node instanceof ListNode listNode) {
            for (YamlNode item : listNode) {
                link(item, anchors);
            }
        }
    }
}
