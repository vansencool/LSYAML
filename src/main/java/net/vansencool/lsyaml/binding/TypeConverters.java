package net.vansencool.lsyaml.binding;

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
 * <p>
 * Provides two levels of conversion:
 * <ul>
 *   <li>{@link #convertFromNode(YamlNode, Class)} / {@link #convertToNode(Object, Class)} --
 *       core type-based conversion for adapters, primitives, and branch types.</li>
 *   <li>{@link #fromNode(YamlNode, Field)} / {@link #toNode(Object, Field)} --
 *       field-aware conversion that additionally handles generic collections
 *       ({@code List<T>}, {@code Set<T>}, {@code Map<String, T>}).</li>
 * </ul>
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
     * Handles adapters, primitives, collections ({@code List}, {@code Set}, {@code Map}),
     * and branch types. Collection element types are inferred from the field's generic signature.
     *
     * @param node  the YAML node
     * @param field the target field
     * @return the converted value, or null if the node is null or conversion fails
     */
    @Nullable
    static Object fromNode(@Nullable YamlNode node, @NotNull Field field) {
        return fromNode(node, field, null);
    }

    /**
     * Converts a YAML node to a Java value based on the field type, with optional YAML source
     * lines for producing rich error context when type conversion fails.
     *
     * @param node  the YAML node
     * @param field the target field
     * @param lines the raw YAML source lines (nullable, used for error context)
     * @return the converted value, or null if the node is null or conversion fails
     */
    @Nullable
    static Object fromNode(@Nullable YamlNode node, @NotNull Field field,
                           @Nullable String[] lines) {
        if (node == null) return null;

        Class<?> type = field.getType();

        if (List.class.isAssignableFrom(type)) return fromListNode(node, field);
        if (Set.class.isAssignableFrom(type)) {
            List<?> list = fromListNode(node, field);
            return list != null ? new LinkedHashSet<>(list) : null;
        }
        if (Map.class.isAssignableFrom(type)) return fromMapNode(node, field);

        return convertFromNode(node, type, getKeyForField(field), lines);
    }

    /**
     * Core type-based conversion from a YAML node to a Java value.
     * Handles adapters, primitive/wrapper types (with graceful type-mismatch handling),
     * and branch types. Does not handle collections -- use {@link #fromNode(YamlNode, Field)} for those.
     *
     * @param node the YAML node
     * @param type the target type
     * @return the converted value, or null if the node is null or conversion fails
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    static Object convertFromNode(@Nullable YamlNode node, @NotNull Class<?> type) {
        return convertFromNode(node, type, null, null);
    }

    /**
     * Core type-based conversion from a YAML node to a Java value, with optional context
     * for producing rich error messages.
     *
     * @param node    the YAML node
     * @param type    the target type
     * @param keyName the YAML key name (nullable, for error messages)
     * @param lines   the full YAML source lines (nullable, for error context)
     * @return the converted value, or null if the node is null or conversion fails
     */
    @SuppressWarnings("rawtypes")
    @Nullable
    static Object convertFromNode(@Nullable YamlNode node, @NotNull Class<?> type,
                                  @Nullable String keyName, @Nullable String[] lines) {
        if (node == null) return null;

        ConfigAdapter adapter = adapters.get(type);
        if (adapter != null) return adapter.fromNode(node);

        if (isPrimitiveOrWrapper(type)) return convertScalar(node, type, keyName, lines);
        if (isBranchType(type)) return fromBranchNode(node, type, lines);

        return null;
    }

    /**
     * Safely converts a YAML node to a primitive/wrapper value.
     * Returns null (preserving the field's default) when the node is not a scalar
     * or the value cannot be parsed to the target type.
     *
     * @param node    the YAML node (expected to be a {@link ScalarNode})
     * @param type    the target primitive/wrapper type
     * @param keyName the YAML key name (nullable, for error context)
     * @param lines   the full YAML source lines (nullable, for error context)
     * @return the converted value, or null on type mismatch or parse failure
     */
    @Nullable
    private static Object convertScalar(@NotNull YamlNode node, @NotNull Class<?> type,
                                        @Nullable String keyName, @Nullable String[] lines) {
        if (!(node instanceof ScalarNode scalar)) {
            warnConversion(node, keyName, type.getSimpleName(),
                    "Expected a scalar value but got " + node.getType(), lines);
            return null;
        }

        if (type == String.class) return scalar.getStringValue();

        if (type == int.class || type == Integer.class) {
            Integer val = scalar.getInt();
            if (val == null && (!scalar.isNull() || type.isPrimitive())) {
                warnConversion(node, keyName, "int",
                        scalar.isNull() ? "Value is empty - expected an int" : "Cannot convert '" + scalar.getString() + "' to int", lines);
            }
            return val;
        }

        if (type == long.class || type == Long.class) {
            Long val = scalar.getLong();
            if (val == null && (!scalar.isNull() || type.isPrimitive())) {
                warnConversion(node, keyName, "long",
                        scalar.isNull() ? "Value is empty - expected a long" : "Cannot convert '" + scalar.getString() + "' to long", lines);
            }
            return val;
        }

        if (type == double.class || type == Double.class) {
            Double val = scalar.getDouble();
            if (val == null && (!scalar.isNull() || type.isPrimitive())) {
                warnConversion(node, keyName, "double",
                        scalar.isNull() ? "Value is empty - expected a double" : "Cannot convert '" + scalar.getString() + "' to double", lines);
            }
            return val;
        }

        if (type == float.class || type == Float.class) {
            Double val = scalar.getDouble();
            if (val == null && (!scalar.isNull() || type.isPrimitive())) {
                warnConversion(node, keyName, "float",
                        scalar.isNull() ? "Value is empty - expected a float" : "Cannot convert '" + scalar.getString() + "' to float", lines);
            }
            return val != null ? val.floatValue() : null;
        }

        if (type == boolean.class || type == Boolean.class) {
            Boolean val = scalar.getBoolean();
            if (val == null && (!scalar.isNull() || type.isPrimitive())) {
                warnConversion(node, keyName, "boolean",
                        scalar.isNull() ? "Value is empty - expected a boolean" : "Cannot convert '" + scalar.getString() + "' to boolean", lines);
            }
            return val;
        }

        if (type == short.class || type == Short.class) {
            Integer val = scalar.getInt();
            if (val == null && (!scalar.isNull() || type.isPrimitive())) {
                warnConversion(node, keyName, "short",
                        scalar.isNull() ? "Value is empty - expected a short" : "Cannot convert '" + scalar.getString() + "' to short", lines);
            }
            return val != null ? val.shortValue() : null;
        }

        if (type == byte.class || type == Byte.class) {
            Integer val = scalar.getInt();
            if (val == null && (!scalar.isNull() || type.isPrimitive())) {
                warnConversion(node, keyName, "byte",
                        scalar.isNull() ? "Value is empty - expected a byte" : "Cannot convert '" + scalar.getString() + "' to byte", lines);
            }
            return val != null ? val.byteValue() : null;
        }

        if (type == char.class || type == Character.class) {
            String s = scalar.getStringValue();
            return (s != null && !s.isEmpty()) ? s.charAt(0) : null;
        }

        return null;
    }

    /**
     * Produces a rich formatted warning for a type conversion failure, using the same
     * box style as {@link net.vansencool.lsyaml.parser.ParseIssue} when line context
     * is available.
     */
    private static void warnConversion(@NotNull YamlNode node, @Nullable String keyName,
                                       @NotNull String expectedType, @NotNull String message,
                                       @Nullable String[] lines) {
        int nodeLine = node.getMetadata().getLine();

        if (lines == null || nodeLine < 1 || nodeLine > lines.length) {
            String prefix = keyName != null ? "Key '" + keyName + "': " : "";
            LSYAMLLogger.warn(prefix + message);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        sb.append("+-").append("-".repeat(70)).append("-+\n");

        String header = "! WARNING at line " + nodeLine;
        if (keyName != null) {
            header += ", key '" + keyName + "'";
        }
        int pad = Math.max(0, 70 - header.length());
        sb.append("| ").append(header).append(" ".repeat(pad)).append(" |\n");
        sb.append("+-").append("-".repeat(70)).append("-+\n");

        int msgPad = Math.max(0, 70 - message.length());
        sb.append("| ").append(message).append(" ".repeat(msgPad)).append(" |\n");

        String hint = "Expected type: " + expectedType + ". Default value will be used.";
        int hintPad = Math.max(0, 70 - hint.length());
        sb.append("| ").append(hint).append(" ".repeat(hintPad)).append(" |\n");
        sb.append("+-").append("-".repeat(70)).append("-+\n");

        sb.append("|\n");

        int start = Math.max(0, nodeLine - 3);
        int end = Math.min(lines.length, nodeLine + 2);
        for (int i = start; i < end; i++) {
            if (i == nodeLine - 1) {
                sb.append("| > ").append(String.format("%4d", i + 1)).append(" | ")
                        .append(lines[i]).append('\n');
                int col = node.getMetadata().getColumn();
                if (col > 0) {
                    sb.append("|        ").append(" ".repeat(col - 1)).append("^");
                    if (col < lines[i].length()) {
                        sb.append("~".repeat(Math.min(5, lines[i].length() - col)));
                    }
                    sb.append('\n');
                }
            } else {
                sb.append("|   ").append(String.format("%4d", i + 1)).append(" | ")
                        .append(lines[i]).append('\n');
            }
        }

        sb.append("|\n");
        sb.append("+").append("-".repeat(72)).append("+");

        LSYAMLLogger.warn(sb.toString());
    }

    @Nullable
    private static List<?> fromListNode(@Nullable YamlNode node, @NotNull Field field) {
        if (!(node instanceof ListNode listNode)) {
            if (node != null) {
                LSYAMLLogger.warn("Expected a list for field '" + field.getName()
                        + "' but got " + node.getType());
            }
            return null;
        }

        Class<?> elementType = extractElementType(field.getGenericType());

        List<Object> result = new ArrayList<>();
        for (YamlNode item : listNode) {
            Object value = convertFromNode(item, elementType);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    @Nullable
    private static Map<String, ?> fromMapNode(@Nullable YamlNode node, @NotNull Field field) {
        if (!(node instanceof MapNode mapNode)) {
            if (node != null) {
                LSYAMLLogger.warn("Expected a map for field '" + field.getName()
                        + "' but got " + node.getType());
            }
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
            Object value = convertFromNode(child, valueType);
            result.put(key, value);
        }
        return result;
    }

    @Nullable
    private static Object fromBranchNode(@Nullable YamlNode node, @NotNull Class<?> type,
                                         @Nullable String[] lines) {
        if (!(node instanceof MapNode mapNode)) {
            if (node != null) {
                LSYAMLLogger.warn("Expected a map for branch type " + type.getSimpleName()
                        + " but got " + node.getType());
            }
            return null;
        }

        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            for (Field field : type.getDeclaredFields()) {
                if (shouldIgnoreField(field)) {
                    continue;
                }

                field.setAccessible(true);
                YamlNode childNode = resolveNode(field, mapNode);

                if (childNode != null) {
                    Object value = fromNode(childNode, field, lines);
                    if (value != null) {
                        field.set(instance, value);
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            LSYAMLLogger.warn("Failed to create instance of " + type.getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a Java value to a YAML node based on the field type.
     * Handles adapters, primitives, collections, and branch types.
     *
     * @param value the Java value
     * @param field the source field (for type info)
     * @return the YAML node
     */
    @NotNull
    static YamlNode toNode(@Nullable Object value, @NotNull Field field) {
        if (value == null) return new ScalarNode(null);

        if (value instanceof List<?> list) return listToNode(list, field);
        if (value instanceof Set<?> set) return listToNode(new ArrayList<>(set), field);
        if (value instanceof Map<?, ?> map) return mapToNode(map);

        return convertToNode(value, value.getClass());
    }

    /**
     * Core type-based conversion from a Java value to a YAML node.
     * Handles adapters, primitive/wrapper types, and branch types.
     *
     * @param value the Java value
     * @param type  the value's type
     * @return the YAML node
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @NotNull
    static YamlNode convertToNode(@Nullable Object value, @NotNull Class<?> type) {
        if (value == null) return new ScalarNode(null);

        ConfigAdapter adapter = adapters.get(type);
        if (adapter != null) return adapter.toNode(value);

        if (isPrimitiveOrWrapper(type)) return new ScalarNode(value);
        if (isBranchType(type)) return branchToNode(value);

        return new ScalarNode(value.toString());
    }

    @NotNull
    private static ListNode listToNode(@NotNull List<?> list, @NotNull Field field) {
        ListBuilder builder = ListBuilder.create();
        Class<?> elementType = extractElementType(field.getGenericType());

        for (Object item : list) {
            if (item == null) {
                builder.add(new ScalarNode(null));
            } else {
                builder.add(convertToNode(item, elementType));
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
                builder.put(key, convertToNode(value, value.getClass()));
            }
        }

        return builder.build();
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
                LSYAMLLogger.warn("Cannot access field " + field.getName() + " on " + type.getSimpleName());
            }
        }

        return builder.build();
    }

    /**
     * Resolves the YAML node for a field from a map, using key fallback logic.
     * <ol>
     *   <li>{@link ExplicitKey} -- uses exact key, no fallback</li>
     *   <li>{@link Key} -- uses lowercased key, no fallback</li>
     *   <li>{@link PreferKeysWith} (field or class level) -- tries preferred separator first,
     *       then underscore, then plain lowercase as a last resort</li>
     *   <li>Default -- plain lowercase field name</li>
     * </ol>
     *
     * @param field the field to resolve
     * @param map   the map node to look up in
     * @return the resolved node, or null if no matching key is found
     */
    @Nullable
    static YamlNode resolveNode(@NotNull Field field, @NotNull MapNode map) {
        ExplicitKey explicitKeyAnn = field.getAnnotation(ExplicitKey.class);
        if (explicitKeyAnn != null) {
            return map.get(explicitKeyAnn.value());
        }

        Key keyAnn = field.getAnnotation(Key.class);
        if (keyAnn != null) {
            return map.get(keyAnn.value().toLowerCase());
        }

        String separator = getPreferredSeparator(field);
        if (separator != null) {
            String preferredKey = camelToSeparated(field.getName(), separator);
            YamlNode node = map.get(preferredKey);
            if (node != null) return node;

            if (!separator.equals("_")) {
                String underscoreKey = camelToSeparated(field.getName(), "_");
                node = map.get(underscoreKey);
                if (node != null) return node;
            }

            return map.get(field.getName().toLowerCase());
        }

        return map.get(field.getName().toLowerCase());
    }

    /**
     * Resolves which key in the map matches a field, using the same fallback logic
     * as {@link #resolveNode(Field, MapNode)}.
     *
     * @param field the field to resolve
     * @param map   the map node to look up in
     * @return the actual key string found in the map, or null if no match
     */
    @Nullable
    static String resolveKey(@NotNull Field field, @NotNull MapNode map) {
        ExplicitKey explicitKeyAnn = field.getAnnotation(ExplicitKey.class);
        if (explicitKeyAnn != null) {
            return map.get(explicitKeyAnn.value()) != null ? explicitKeyAnn.value() : null;
        }

        Key keyAnn = field.getAnnotation(Key.class);
        if (keyAnn != null) {
            String key = keyAnn.value().toLowerCase();
            return map.get(key) != null ? key : null;
        }

        String separator = getPreferredSeparator(field);
        if (separator != null) {
            String preferredKey = camelToSeparated(field.getName(), separator);
            if (map.get(preferredKey) != null) return preferredKey;

            if (!separator.equals("_")) {
                String underscoreKey = camelToSeparated(field.getName(), "_");
                if (map.get(underscoreKey) != null) return underscoreKey;
            }

            String plain = field.getName().toLowerCase();
            return map.get(plain) != null ? plain : null;
        }

        String plain = field.getName().toLowerCase();
        return map.get(plain) != null ? plain : null;
    }

    /**
     * Gets the config key name for a field (used when writing YAML).
     * <ol>
     *   <li>{@link ExplicitKey} -- exact value as-is</li>
     *   <li>{@link Key} -- lowercased value</li>
     *   <li>{@link PreferKeysWith} -- camelCase split with the preferred separator</li>
     *   <li>Default -- plain lowercase field name</li>
     * </ol>
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

        String separator = getPreferredSeparator(field);
        if (separator != null) {
            return camelToSeparated(field.getName(), separator);
        }

        return field.getName().toLowerCase();
    }

    /**
     * Returns the preferred key separator for a field, checking the field annotation first,
     * then the declaring class, and then walking up through enclosing classes.
     *
     * @param field the field
     * @return the separator string, or null if no {@link PreferKeysWith} is present
     */
    @Nullable
    private static String getPreferredSeparator(@NotNull Field field) {
        PreferKeysWith fieldAnn = field.getAnnotation(PreferKeysWith.class);
        if (fieldAnn != null) return fieldAnn.value();

        Class<?> cls = field.getDeclaringClass();
        while (cls != null) {
            PreferKeysWith classAnn = cls.getAnnotation(PreferKeysWith.class);
            if (classAnn != null) return classAnn.value();
            cls = cls.getEnclosingClass();
        }

        return null;
    }

    /**
     * Converts a camelCase or UPPER_CASE name to a separated lowercase form.
     * For example, with separator "-": {@code maxPlayers} becomes {@code max-players},
     * {@code HTTPServer} becomes {@code http-server}, {@code MAX_CONNECTIONS} becomes
     * {@code max-connections}.
     *
     * @param name      the field name
     * @param separator the separator string to insert between words
     * @return the separated, lowercased key
     */
    @NotNull
    static String camelToSeparated(@NotNull String name, @NotNull String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);

            if (c == '_') {
                sb.append(separator);
                continue;
            }

            if (Character.isUpperCase(c) && i > 0) {
                char prev = name.charAt(i - 1);
                if (prev != '_' && !Character.isUpperCase(prev)) {
                    sb.append(separator);
                } else if (Character.isUpperCase(prev)
                        && i + 1 < name.length()
                        && name.charAt(i + 1) != '_'
                        && !Character.isUpperCase(name.charAt(i + 1))) {
                    sb.append(separator);
                }
            }

            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
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
     * Extracts the first type argument from a generic type.
     * Returns {@link Object} if the type is not parameterized or has no type arguments.
     *
     * @param genericType the generic type
     * @return the element class
     */
    @NotNull
    private static Class<?> extractElementType(@NotNull Type genericType) {
        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                return clazz;
            }
        }
        return Object.class;
    }

    /**
     * Clears all registered adapters.
     */
    static void clearAdapters() {
        adapters.clear();
    }
}
