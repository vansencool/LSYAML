package net.vansencool.lsyaml.binding.convert.write;

import net.vansencool.lsyaml.binding.Comment;
import net.vansencool.lsyaml.binding.ConfigAdapter;
import net.vansencool.lsyaml.binding.Space;
import net.vansencool.lsyaml.binding.adapter.AdapterRegistry;
import net.vansencool.lsyaml.binding.key.FieldKeys;
import net.vansencool.lsyaml.binding.type.TypeKinds;
import net.vansencool.lsyaml.builder.ListBuilder;
import net.vansencool.lsyaml.builder.MapBuilder;
import net.vansencool.lsyaml.logger.LSYAMLLogger;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Conversion from Java values to YAML nodes.
 */
public final class NodeWriter {

    private NodeWriter() {
    }

    /**
     * Returns the YAML node for a field value.
     */
    public static @NotNull YamlNode toNode(@Nullable Object value, @NotNull Field field) {
        if (value == null) return new ScalarNode(null);

        if (value instanceof List<?> list) return listToNode(list, field);
        if (value instanceof Set<?> set) return listToNode(new ArrayList<>(set), field);
        if (value instanceof Map<?, ?> map) return mapToNode(map);

        return convertToNode(value, value.getClass());
    }

    /**
     * Returns the YAML node for a scalar or branch value of a given type.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static @NotNull YamlNode convertToNode(@Nullable Object value, @NotNull Class<?> type) {
        if (value == null) return new ScalarNode(null);

        ConfigAdapter adapter = AdapterRegistry.adapter(type);
        if (adapter != null) return adapter.toNode(value);

        if (TypeKinds.isPrimitiveOrWrapper(type)) return new ScalarNode(value);
        if (TypeKinds.isBranchType(type)) return branchToNode(value);

        return new ScalarNode(value.toString());
    }

    /**
     * Returns the list node for a list field value.
     */
    private static @NotNull ListNode listToNode(@NotNull List<?> list, @NotNull Field field) {
        ListBuilder builder = ListBuilder.create();
        Class<?> elementType = TypeKinds.elementType(field.getGenericType());

        for (Object item : list) {
            if (item == null) {
                builder.add(new ScalarNode(null));
            } else {
                builder.add(convertToNode(item, elementType));
            }
        }

        return builder.build();
    }

    /**
     * Returns the map node for a map value.
     */
    private static @NotNull MapNode mapToNode(@NotNull Map<?, ?> map) {
        MapBuilder builder = MapBuilder.create();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (value == null) {
                builder.put(key, new ScalarNode(null));
            } else {
                builder.put(key, convertToNode(value, value.getClass()));
            }
        }

        return builder.build();
    }

    /**
     * Returns the map node for a branch value, applying field comments and spacing.
     */
    private static @NotNull MapNode branchToNode(@NotNull Object branch) {
        MapBuilder builder = MapBuilder.create();
        Class<?> type = branch.getClass();

        Space spaceAnn = type.getAnnotation(Space.class);
        if (spaceAnn != null && spaceAnn.before() > 0) {
            builder.emptyLines(spaceAnn.before());
        }

        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            if (FieldKeys.shouldIgnore(field)) {
                continue;
            }

            field.setAccessible(true);
            String key = FieldKeys.keyForField(field);

            try {
                Object value = field.get(branch);
                YamlNode node = toNode(value, field);

                Comment comment = field.getAnnotation(Comment.class);
                if (comment != null) {
                    for (String line : comment.value()) {
                        node.addCommentBefore(line);
                    }
                }

                Space fieldSpace = field.getAnnotation(Space.class);
                if (fieldSpace != null && fieldSpace.before() > 0) {
                    node.setEmptyLinesBefore(fieldSpace.before());
                }

                builder.put(key, node);
            } catch (IllegalAccessException e) {
                LSYAMLLogger.warn("Cannot access field " + field.getName() + " on " + type.getSimpleName());
            }
        }

        return builder.build();
    }
}
