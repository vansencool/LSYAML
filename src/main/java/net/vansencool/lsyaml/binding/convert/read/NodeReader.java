package net.vansencool.lsyaml.binding.convert.read;

import net.vansencool.lsyaml.binding.ConfigAdapter;
import net.vansencool.lsyaml.binding.adapter.AdapterRegistry;
import net.vansencool.lsyaml.binding.key.FieldKeys;
import net.vansencool.lsyaml.binding.type.TypeKinds;
import net.vansencool.lsyaml.logger.LSYAMLLogger;
import net.vansencool.lsyaml.node.ListNode;
import net.vansencool.lsyaml.node.MapNode;
import net.vansencool.lsyaml.node.ScalarNode;
import net.vansencool.lsyaml.node.YamlNode;
import net.vansencool.lsyaml.diagnostic.Diagnostic;
import net.vansencool.lsyaml.diagnostic.DiagnosticBuilder;
import net.vansencool.lsyaml.diagnostic.Severity;
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

/**
 * Conversion from YAML nodes to Java values.
 */
public final class NodeReader {

    private NodeReader() {
    }

    /**
     * Returns the Java value for a field from a YAML node.
     */
    public static @Nullable Object fromNode(@Nullable YamlNode node, @NotNull Field field) {
        return fromNode(node, field, null);
    }

    /**
     * Returns the Java value for a field from a YAML node with source lines for error context.
     */
    public static @Nullable Object fromNode(@Nullable YamlNode node, @NotNull Field field, @Nullable String[] lines) {
        if (node == null) return null;

        Class<?> type = field.getType();

        if (List.class.isAssignableFrom(type)) return fromListNode(node, field, lines);
        if (Set.class.isAssignableFrom(type)) {
            List<?> list = fromListNode(node, field, lines);
            return list != null ? new LinkedHashSet<>(list) : null;
        }
        if (Map.class.isAssignableFrom(type)) return fromMapNode(node, field);

        return convertFromNode(node, type, FieldKeys.keyForField(field), lines);
    }

    /**
     * Returns the Java value for a scalar or branch node of a given type.
     */
    private static @Nullable Object convertFromNode(@Nullable YamlNode node, @NotNull Class<?> type) {
        return convertFromNode(node, type, null, null);
    }

    /**
     * Returns the Java value for a scalar or branch node of a given type with error context.
     */
    @SuppressWarnings("rawtypes")
    private static @Nullable Object convertFromNode(@Nullable YamlNode node, @NotNull Class<?> type, @Nullable String keyName, @Nullable String[] lines) {
        if (node == null) return null;

        ConfigAdapter adapter = AdapterRegistry.adapter(type);
        if (adapter != null) return adapter.fromNode(node);

        if (TypeKinds.isPrimitiveOrWrapper(type)) return convertScalar(node, type, keyName, lines);
        if (TypeKinds.isBranchType(type)) return fromBranchNode(node, type, lines);

        return null;
    }

    /**
     * Returns the primitive or wrapper value for a scalar node, or null on type mismatch.
     */
    private static @Nullable Object convertScalar(@NotNull YamlNode node, @NotNull Class<?> type, @Nullable String keyName, @Nullable String[] lines) {
        if (!(node instanceof ScalarNode scalar)) {
            warnConversion(node, keyName, type.getSimpleName(), "Expected a scalar value but got " + node.getType(), lines);
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
     * Logs a warning describing a type conversion failure.
     */
    private static void warnConversion(@NotNull YamlNode node, @Nullable String keyName, @NotNull String expectedType, @NotNull String message, @Nullable String[] lines) {
        int nodeLine = node.getMetadata().getLine();
        if (lines == null || nodeLine < 1 || nodeLine > lines.length) {
            String prefix = keyName != null ? "key '" + keyName + "': " : "";
            LSYAMLLogger.warn(prefix + message);
            return;
        }

        String sourceLine = lines[nodeLine - 1];
        int col = Math.max(0, node.getMetadata().getColumn() - 1);
        DiagnosticBuilder builder = Diagnostic.builder()
                .severity(Severity.WARNING)
                .title(keyName != null ? "'" + keyName + "': " + message : message)
                .at(nodeLine, sourceLine)
                .highlight(col, sourceLine.length())
                .note("expected " + expectedType + ", the default value will be used");
        for (int i = Math.max(0, nodeLine - 3); i < nodeLine - 1; i++) {
            builder.context(i + 1, lines[i], 0, 0, null);
        }
        LSYAMLLogger.warn(builder.build().format());
    }

    /**
     * Returns the list value for a field from a list node.
     */
    private static @Nullable List<?> fromListNode(@Nullable YamlNode node, @NotNull Field field, @Nullable String[] lines) {
        if (!(node instanceof ListNode listNode)) {
            if (node != null) {
                LSYAMLLogger.warn("expected a list for field '" + field.getName() + "' but got " + node.getType());
            }
            return null;
        }

        Class<?> elementType = TypeKinds.elementType(field.getGenericType());

        List<Object> result = new ArrayList<>();
        int index = 0;
        for (YamlNode item : listNode) {
            Object value = convertFromNode(item, elementType, field.getName() + "[" + index + "]", lines);
            if (value != null) {
                result.add(value);
            }
            index++;
        }
        return result;
    }

    /**
     * Returns the map value for a field from a map node.
     */
    private static @Nullable Map<String, ?> fromMapNode(@Nullable YamlNode node, @NotNull Field field) {
        if (!(node instanceof MapNode mapNode)) {
            if (node != null) {
                LSYAMLLogger.warn("Expected a map for field '" + field.getName() + "' but got " + node.getType());
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

    /**
     * Returns a populated branch instance for a map node of a given type.
     */
    private static @Nullable Object fromBranchNode(@Nullable YamlNode node, @NotNull Class<?> type, @Nullable String[] lines) {
        if (!(node instanceof MapNode mapNode)) {
            if (node != null) {
                LSYAMLLogger.warn("Expected a map for branch type " + type.getSimpleName() + " but got " + node.getType());
            }
            return null;
        }

        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            for (Field field : type.getDeclaredFields()) {
                if (FieldKeys.shouldIgnore(field)) {
                    continue;
                }

                field.setAccessible(true);
                YamlNode childNode = FieldKeys.resolveNode(field, mapNode);

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
}
