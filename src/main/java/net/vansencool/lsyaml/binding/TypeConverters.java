package net.vansencool.lsyaml.binding;

import net.vansencool.lsyaml.builder.ListBuilder;
import net.vansencool.lsyaml.builder.MapBuilder;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal utility class for converting between Java types and YAML nodes.
 */
@SuppressWarnings("unused")
final class TypeConverters {

    private static final @NotNull Map<Class<?>, ConfigAdapter<?>> adapters = new ConcurrentHashMap<>();

    private TypeConverters() {
    }

    /**
     * Registers a custom adapter for a type.
     *
     * @param type    the type class
     * @param adapter the adapter
     * @param <T>     the type
     */
    static <T> void registerAdapter(@NotNull Class<T> type, @NotNull ConfigAdapter<T> adapter) {
        adapters.put(type, adapter);
    }

    /**
     * Returns the adapter for a type, or null if none registered.
     *
     * @param type the type class
     * @param <T>  the type
     * @return the adapter, or null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    static <T> ConfigAdapter<T> getAdapter(@NotNull Class<T> type) {
        return (ConfigAdapter<T>) adapters.get(type);
    }

    /**
     * Checks if a type has a registered adapter.
     *
     * @param type the type class
     * @return true if an adapter exists
     */
    static boolean hasAdapter(@NotNull Class<?> type) {
        return adapters.containsKey(type);
    }

    /**
     * Converts a YAML node to a Java value based on the field type.
     *
     * @param node  the YAML node
     * @param field the target field
     * @return the converted value
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    static Object fromNode(@Nullable YamlNode node, @NotNull Field field) {
        if (node == null) {
            return null;
        }

        Class<?> type = field.getType();

        ConfigAdapter adapter = adapters.get(type);
        if (adapter != null) {
            return adapter.fromNode(node);
        }

        if (type == String.class) {
            return node.getString();
        }

        if (type == int.class || type == Integer.class) {
            return node.getInt();
        }

        if (type == long.class || type == Long.class) {
            return node.getLong();
        }

        if (type == double.class || type == Double.class) {
            return node.getDouble();
        }

        if (type == float.class || type == Float.class) {
            Double d = node.getDouble();
            return d != null ? d.floatValue() : null;
        }

        if (type == boolean.class || type == Boolean.class) {
            return node.getBoolean();
        }

        if (type == short.class || type == Short.class) {
            Integer i = node.getInt();
            return i != null ? i.shortValue() : null;
        }

        if (type == byte.class || type == Byte.class) {
            Integer i = node.getInt();
            return i != null ? i.byteValue() : null;
        }

        if (type == char.class || type == Character.class) {
            String s = node.getString();
            return (s != null && !s.isEmpty()) ? s.charAt(0) : null;
        }

        if (List.class.isAssignableFrom(type)) {
            return fromListNode(node, field);
        }

        if (Set.class.isAssignableFrom(type)) {
            List<?> list = fromListNode(node, field);
            return list != null ? new LinkedHashSet<>(list) : null;
        }

        if (Map.class.isAssignableFrom(type)) {
            return fromMapNode(node, field);
        }

        if (isBranchType(type)) {
            return fromBranchNode(node, type);
        }

        return null;
    }

    @Nullable
    private static List<?> fromListNode(@Nullable YamlNode node, @NotNull Field field) {
        if (!(node instanceof ListNode listNode)) {
            return null;
        }

        Type genericType = field.getGenericType();
        Class<?> elementType = Object.class;

        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                elementType = clazz;
            }
        }

        List<Object> result = new ArrayList<>();
        for (YamlNode item : listNode) {
            Object value = fromNodeByType(item, elementType);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    @Nullable
    private static Map<String, ?> fromMapNode(@Nullable YamlNode node, @NotNull Field field) {
        if (!(node instanceof MapNode mapNode)) {
            return null;
        }

        Type genericType = field.getGenericType();
        Class<?> valueType = Object.class;

        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 1 && typeArgs[1] instanceof Class<?> clazz) {
                valueType = clazz;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : mapNode.keys()) {
            YamlNode child = mapNode.get(key);
            Object value = fromNodeByType(child, valueType);
            result.put(key, value);
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Nullable
    private static Object fromNodeByType(@Nullable YamlNode node, @NotNull Class<?> type) {
        if (node == null) {
            return null;
        }

        ConfigAdapter adapter = adapters.get(type);
        if (adapter != null) {
            return adapter.fromNode(node);
        }

        if (type == String.class) {
            return node.getString();
        }

        if (type == Integer.class || type == int.class) {
            return node.getInt();
        }

        if (type == Long.class || type == long.class) {
            return node.getLong();
        }

        if (type == Double.class || type == double.class) {
            return node.getDouble();
        }

        if (type == Float.class || type == float.class) {
            Double d = node.getDouble();
            return d != null ? d.floatValue() : null;
        }

        if (type == Boolean.class || type == boolean.class) {
            return node.getBoolean();
        }

        if (type == Short.class || type == short.class) {
            Integer i = node.getInt();
            return i != null ? i.shortValue() : null;
        }

        if (type == Byte.class || type == byte.class) {
            Integer i = node.getInt();
            return i != null ? i.byteValue() : null;
        }

        if (type == Character.class || type == char.class) {
            String s = node.getString();
            return (s != null && !s.isEmpty()) ? s.charAt(0) : null;
        }

        if (isBranchType(type)) {
            return fromBranchNode(node, type);
        }

        return null;
    }

    @Nullable
    private static Object fromBranchNode(@Nullable YamlNode node, @NotNull Class<?> type) {
        if (!(node instanceof MapNode mapNode)) {
            return null;
        }

        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            for (Field field : type.getDeclaredFields()) {
                if (shouldIgnoreField(field)) {
                    continue;
                }

                field.setAccessible(true);
                String key = getKeyForField(field);
                YamlNode childNode = mapNode.get(key);

                if (childNode != null) {
                    Object value = fromNode(childNode, field);
                    if (value != null) {
                        field.set(instance, value);
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts a Java value to a YAML node.
     *
     * @param value the Java value
     * @param field the source field (for type info)
     * @return the YAML node
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @NotNull
    static YamlNode toNode(@Nullable Object value, @NotNull Field field) {
        if (value == null) {
            return new ScalarNode(null);
        }

        Class<?> type = value.getClass();

        ConfigAdapter adapter = adapters.get(type);
        if (adapter != null) {
            return adapter.toNode(value);
        }

        if (isPrimitiveOrWrapper(type)) {
            return new ScalarNode(value);
        }

        if (value instanceof List<?> list) {
            return listToNode(list, field);
        }

        if (value instanceof Set<?> set) {
            return listToNode(new ArrayList<>(set), field);
        }

        if (value instanceof Map<?, ?> map) {
            return mapToNode(map);
        }

        if (isBranchType(type)) {
            return branchToNode(value);
        }

        return new ScalarNode(value.toString());
    }

    @NotNull
    private static ListNode listToNode(@NotNull List<?> list, @NotNull Field field) {
        ListBuilder builder = ListBuilder.create();

        Type genericType = field.getGenericType();
        Class<?> elementType = Object.class;

        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                elementType = clazz;
            }
        }

        for (Object item : list) {
            if (item == null) {
                builder.add(new ScalarNode(null));
            } else {
                YamlNode node = toNodeByType(item, elementType);
                builder.add(node);
            }
        }

        return builder.build();
    }

    @NotNull
    private static MapNode mapToNode(@NotNull Map<?, ?> map) {
        MapBuilder builder = MapBuilder.create();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (value == null) {
                builder.put(key, new ScalarNode(null));
            } else {
                YamlNode node = toNodeByType(value, value.getClass());
                builder.put(key, node);
            }
        }

        return builder.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @NotNull
    private static YamlNode toNodeByType(@Nullable Object value, @NotNull Class<?> type) {
        if (value == null) {
            return new ScalarNode(null);
        }

        ConfigAdapter adapter = adapters.get(type);
        if (adapter != null) {
            return adapter.toNode(value);
        }

        if (isPrimitiveOrWrapper(value.getClass())) {
            return new ScalarNode(value);
        }

        if (isBranchType(value.getClass())) {
            return branchToNode(value);
        }

        return new ScalarNode(value.toString());
    }

    @NotNull
    private static MapNode branchToNode(@NotNull Object branch) {
        MapBuilder builder = MapBuilder.create();
        Class<?> type = branch.getClass();

        Space spaceAnn = type.getAnnotation(Space.class);
        if (spaceAnn != null && spaceAnn.before() > 0) {
            builder.emptyLines(spaceAnn.before());
        }

        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) {
            if (shouldIgnoreField(field)) {
                continue;
            }

            field.setAccessible(true);
            String key = getKeyForField(field);

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
                // Skip field
            }
        }

        return builder.build();
    }

    /**
     * Gets the config key name for a field.
     * Keys are converted to lowercase unless @ExplicitKey is used.
     *
     * @param field the field
     * @return the key name
     */
    @NotNull
    static String getKeyForField(@NotNull Field field) {
        ExplicitKey explicitKeyAnn = field.getAnnotation(ExplicitKey.class);
        if (explicitKeyAnn != null) {
            return explicitKeyAnn.value();
        }

        Key keyAnn = field.getAnnotation(Key.class);
        if (keyAnn != null) {
            return keyAnn.value().toLowerCase();
        }

        return field.getName().toLowerCase();
    }

    /**
     * Checks if a field should be ignored.
     *
     * @param field the field
     * @return true if ignored
     */
    static boolean shouldIgnoreField(@NotNull Field field) {
        if (field.isAnnotationPresent(Ignore.class)) {
            return true;
        }
        return field.isSynthetic();
    }

    /**
     * Checks if a type is a primitive or wrapper type.
     *
     * @param type the type
     * @return true if primitive/wrapper
     */
    static boolean isPrimitiveOrWrapper(@NotNull Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Boolean.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Short.class
                || type == Byte.class
                || type == Character.class;
    }

    /**
     * Checks if a type is a branch type (nested config object).
     * A branch type is any non-primitive class that has fields suitable for config binding.
     *
     * @param type the type
     * @return true if branch type
     */
    static boolean isBranchType(@NotNull Class<?> type) {
        if (isPrimitiveOrWrapper(type)) {
            return false;
        }
        if (type.isArray()) {
            return false;
        }
        if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
            return false;
        }
        if (Map.class.isAssignableFrom(type)) {
            return false;
        }
        if (type.isEnum()) {
            return false;
        }
        return !hasAdapter(type);
    }

    /**
     * Clears all registered adapters.
     */
    static void clearAdapters() {
        adapters.clear();
    }
}
