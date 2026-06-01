package net.vansencool.lsyaml.binding.type;

import net.vansencool.lsyaml.binding.adapter.AdapterRegistry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classification of Java types for config binding.
 */
public final class TypeKinds {

    private TypeKinds() {
    }

    /**
     * Returns whether a type is a primitive or wrapper type.
     */
    public static boolean isPrimitiveOrWrapper(@NotNull Class<?> type) {
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
     * Returns whether a type is a branch type, a nested config object bound field by field.
     */
    public static boolean isBranchType(@NotNull Class<?> type) {
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
        return !AdapterRegistry.has(type);
    }

    /**
     * Returns the first type argument of a generic type, or Object when it is not parameterized.
     */
    @NotNull
    public static Class<?> elementType(@NotNull Type genericType) {
        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                return clazz;
            }
        }
        return Object.class;
    }
}
