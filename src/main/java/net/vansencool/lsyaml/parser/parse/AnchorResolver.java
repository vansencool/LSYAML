package net.vansencool.lsyaml.parser.parse;

import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Two-phase linker that binds merge keys to the maps named by their alias anchors.
 */
public final class AnchorResolver {

    private AnchorResolver() {
    }

    /**
     * Binds each merge entry to the map named by its alias, using anchors collected during parsing.
     */
    public static void resolve(@NotNull List<YamlNode> anchored, @NotNull List<MapNode.MapEntry> mergeEntries) {
        Map<String, YamlNode> anchors = new HashMap<>();
        for (int i = 0, n = anchored.size(); i < n; i++) {
            YamlNode node = anchored.get(i);
            String anchor = node.getMetadata().getAnchor();
            if (anchor != null && !anchor.isEmpty()) {
                anchors.put(anchor, node);
            }
        }
        for (int i = 0, n = mergeEntries.size(); i < n; i++) {
            MapNode.MapEntry mergeEntry = mergeEntries.get(i);
            YamlNode target = anchors.get(mergeEntry.getValue().getMetadata().getAlias());
            if (target instanceof MapNode resolvedMap) {
                mergeEntry.setResolvedMergeMap(resolvedMap);
            }
        }
    }
}
