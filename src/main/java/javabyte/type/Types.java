/*
 *    Copyright 2021 Whilein
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package javabyte.type;

import javabyte.util.AsmUtils;
import javabyte.util.StringUtils;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author whilein
 */
@UtilityClass
public class Types {

    private static final Map<String, ExactTypeName> CACHE_INTERNAL = new HashMap<>();
    private static final Map<String, ExactTypeName> CACHE = new HashMap<>();

    private static final char[] PRIMITIVE_DESCRIPTORS = "VZBSCIJFD".toCharArray();

    // @formatter:off
    public static final int
            VOID_TYPE       = 0,
            BOOL_TYPE       = 1,
            BYTE_TYPE       = 2,
            SHORT_TYPE      = 3,
            CHAR_TYPE       = 4,
            INT_TYPE        = 5,
            LONG_TYPE       = 6,
            FLOAT_TYPE      = 7,
            DOUBLE_TYPE     = 8;

    public static final ExactTypeName
            VOID            = Types._putCache(VOID_TYPE, Void.TYPE),
            VOID_OBJ        = Types._putCache(Void.class),
            BOOL            = Types._putCache(BOOL_TYPE, Boolean.TYPE),
            BOOL_OBJ        = Types._putCache(Boolean.class),
            BYTE            = Types._putCache(BYTE_TYPE, Byte.TYPE),
            BYTE_OBJ        = Types._putCache(Byte.class),
            CHAR            = Types._putCache(CHAR_TYPE, Character.TYPE),
            CHAR_OBJ        = Types._putCache(Character.class),
            SHORT           = Types._putCache(SHORT_TYPE, Short.TYPE),
            SHORT_OBJ       = Types._putCache(Short.class),
            INT             = Types._putCache(INT_TYPE, Integer.TYPE),
            INT_OBJ         = Types._putCache(Integer.class),
            LONG            = Types._putCache(LONG_TYPE, Long.TYPE),
            LONG_OBJ        = Types._putCache(Long.class),
            FLOAT           = Types._putCache(FLOAT_TYPE, Float.TYPE),
            FLOAT_OBJ       = Types._putCache(Float.class),
            DOUBLE          = Types._putCache(DOUBLE_TYPE, Double.TYPE),
            DOUBLE_OBJ      = Types._putCache(Double.class),
            STRING          = Types._putCache(String.class),
            STRING_BUILDER  = Types._putCache(StringBuilder.class),
            NUMBER          = Types._putCache(Number.class),
            OBJECT          = Types._putCache(Object.class),
            ITERABLE        = Types._putCache(Iterable.class),
            ITERATOR        = Types._putCache(Iterator.class),
            COLLECTION      = Types._putCache(Collection.class);
    // @formatter:on

    private final ExactTypeName[] WRAPPERS = {
            VOID_OBJ, BOOL_OBJ,
            BYTE_OBJ, SHORT_OBJ,
            CHAR_OBJ, INT_OBJ,
            LONG_OBJ, FLOAT_OBJ,
            DOUBLE_OBJ
    };

    private final ExactTypeName[] PRIMITIVES = {
            VOID, BOOL,
            BYTE, SHORT,
            CHAR, INT,
            LONG, FLOAT,
            DOUBLE
    };

    public final Wildcard WILDCARD_ANY = new WildcardImpl(null, null);

    public boolean isWrapper(final @NonNull TypeName subject) {
        for (val wrapper : WRAPPERS)
            if (wrapper.equals(subject))
                return true;
        return false;
    }

    public @NotNull ExactTypeName getWrapper(final int primitive) {
        return WRAPPERS[primitive];
    }

    public @NotNull ExactTypeName getPrimitive(final @NonNull TypeName wrapper) {
        for (int i = 0, j = WRAPPERS.length; i < j; i++)
            if (WRAPPERS[i].equals(wrapper))
                return PRIMITIVES[i];

        throw new IllegalStateException(wrapper + " isn't wrapper");
    }

    public @NotNull ExactTypeName getWrapper(final @NonNull TypeName primitive) {
        return WRAPPERS[primitive.getPrimitive()];
    }

    public @NotNull ExactTypeName of(final @NonNull Class<?> cls) {
        return _of(cls);
    }

    private ExactTypeName _of(final Class<?> cls) {
        int dimensions = 0;
        Class<?> component = cls;

        while (component.isArray()) {
            dimensions++;
            component = component.getComponentType();
        }

        return _getCacheOrInit(component.getName(), dimensions, false, cls);
    }

    public @NotNull TypeName of(final @NonNull Type type) {
        return _fromType(type);
    }

    public @NotNull TypeName @NotNull [] of(final @NotNull Type @NonNull ... types) {
        return _fromArray(types);
    }

    public @NotNull ExactTypeName of(final @NonNull String name) {
        return _fromName(name, 0, null);
    }

    public @NotNull ExactTypeName ofInternal(final @NonNull String internalName) {
        return _getCacheOrInit(internalName, 0, true, null);
    }

    public @NotNull ExactTypeName @NotNull [] of(final @NotNull String @NonNull [] names) {
        return _fromArray(names, false);
    }

    public @NotNull ExactTypeName @NotNull [] of(final @NotNull Class<?> @NonNull [] types) {
        return _fromArray(types);
    }


    public @NotNull ExactTypeName @NotNull [] ofInternal(final @NonNull String @NotNull [] internalNames) {
        return _fromArray(internalNames, true);
    }

    public @NotNull TypeParameter variable(
            final @NonNull String label,
            final @NotNull TypeName @NonNull [] bounds
    ) {
        return new TypeParameterImpl(label, bounds);
    }

    public @NotNull ParameterizedTypeName parameterized(
            final @NonNull ExactTypeName name,
            final @NotNull Parameter @NonNull ... parameters
    ) {
        if (parameters.length < 1) {
            throw new IllegalStateException("You should specify at least one parameter");
        }

        return new ParameterizedTypeNameImpl(name, parameters);
    }

    public @NotNull Wildcard wildcardUpper(final @NotNull TypeName @NonNull ... names) {
        if (names.length < 1) {
            throw new IllegalStateException("You should specify at least one name");
        }

        return new WildcardImpl(names, null);
    }

    public @NotNull Wildcard wildcardLower(final @NotNull TypeName @NonNull ... names) {
        if (names.length < 1) {
            throw new IllegalStateException("You should specify at least one name");
        }

        return new WildcardImpl(null, names);
    }

    public boolean hasParameterizedTypes(final @NonNull Iterable<@NotNull TypeName> names) {
        for (val name : names)
            if (name.hasParameterizedTypes())
                return true;

        return false;
    }

    public boolean hasParameterizedTypes(final @NotNull TypeName @NonNull ... names) {
        for (val name : names)
            if (name.hasParameterizedTypes())
                return true;

        return false;
    }

    private ExactTypeName _putCache(final Class<?> type) {
        return _putCache(-1, type);
    }

    private ExactTypeName _putCache(final int primitive, final Class<?> type) {
        val name = _fromName(type.getName(), primitive, 0, "\\.", type);

        CACHE.put(type.getName(), name);
        CACHE_INTERNAL.put(type.getName().replace('.', '/'), name);

        return name;
    }

    private ExactTypeName _getCacheOrInit(final String type, final int dimensions, final boolean internal,
                                          final Class<?> originalClass) {
        if (dimensions == 0) {
            val cache = internal ? CACHE_INTERNAL.get(type) : CACHE.get(type);
            if (cache != null) return cache;
        }

        return _fromName(type, -1, dimensions, internal ? "/" : "\\.", originalClass);
    }

    private ExactTypeName _fromName(final String name, final int primitive, final int dimensions, final String separator,
                                    final Class<?> originalClass) {
        return new ExactTypeNameImpl(primitive, dimensions, name.split(separator), originalClass);
    }

    private ExactTypeName _fromName(final String name, final int dimensions, final Class<?> originalClass) {
        return _fromName(name, -1, dimensions, "\\.", originalClass);
    }

    private Parameter _getParam(final Type type) {
        if (type instanceof WildcardType) {
            val wildcard = (WildcardType) type;
            val upperTypes = wildcard.getUpperBounds();
            val lowerTypes = wildcard.getLowerBounds();

            val upper = upperTypes.length != 0 ? _fromArray(upperTypes) : null;
            val lower = lowerTypes.length != 0 ? _fromArray(lowerTypes) : null;

            return new WildcardImpl(upper, lower);
        } else if (type instanceof TypeVariable<?>) {
            val variable = (TypeVariable<?>) type;

            val boundTypes = variable.getBounds();
            val bounds = _fromArray(boundTypes);

            return new TypeParameterImpl(variable.getName(), bounds);
        }

        return _fromType(type);
    }

    private TypeName _fromType(final Type type) {
        if (type instanceof Class<?>) {
            return _of((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            val parameterized = (ParameterizedType) type;

            val rawType = parameterized.getRawType();
            val parameterTypes = parameterized.getActualTypeArguments();

            val raw = _fromType(rawType);
            val parameters = _getParams(parameterTypes);

            return new ParameterizedTypeNameImpl((ExactTypeName) raw, parameters);
        } else {
            throw new IllegalArgumentException(type.getClass().getName() + " is not supported");
        }
    }

    private Parameter[] _getParams(final Type... types) {
        val params = new Parameter[types.length];

        for (int i = 0, j = params.length; i < j; i++)
            params[i] = _getParam(types[i]);

        return params;
    }

    private TypeName[] _fromArray(final Type... types) {
        val names = new TypeName[types.length];

        for (int i = 0, j = names.length; i < j; i++)
            names[i] = _fromType(types[i]);

        return names;
    }

    private ExactTypeName[] _fromArray(final String[] names, final boolean internal) {
        val exactNames = new ExactTypeName[names.length];

        for (int i = 0, j = names.length; i < j; i++)
            exactNames[i] = Types._getCacheOrInit(names[i], 0, internal, null);

        return exactNames;
    }

    private ExactTypeName[] _fromArray(final Class<?>[] types) {
        val exactNames = new ExactTypeName[types.length];

        for (int i = 0, j = types.length; i < j; i++)
            exactNames[i] = _of(types[i]);

        return exactNames;
    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class AbstractParameter implements Parameter {
        @Override
        public final @NotNull String getSignature() {
            return StringUtils.from(this::getSignature);
        }

        @Override
        public final @NotNull String getDescriptor() {
            return StringUtils.from(this::getDescriptor);
        }

        @Override
        public final @NotNull String toString() {
            return StringUtils.from(this::toString);
        }
    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class AbstractTypeName extends AbstractParameter implements TypeName {
        @Override
        public final @NotNull String getInternalName() {
            return StringUtils.from(this::getInternalName);
        }

        @Override
        public final @NotNull String getName() {
            return StringUtils.from(this::getName);
        }
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ParameterizedTypeNameImpl extends AbstractTypeName implements ParameterizedTypeName {

        ExactTypeName rawName;
        Parameter[] parameters;

        @Override
        public int getDimensions() {
            return rawName.getDimensions();
        }

        @Override
        public boolean isArray() {
            return rawName.isArray();
        }

        @Override
        public @NotNull TypeName getComponent() {
            return rawName.getComponent();
        }

        @Override
        public int getPrimitive() {
            return rawName.getPrimitive();
        }

        @Override
        public boolean isPrimitive() {
            return rawName.isPrimitive();
        }

        @Override
        public @NotNull org.objectweb.asm.Type toType() {
            return rawName.toType();
        }

        @Override
        public @NotNull Class<?> toClass() {
            return rawName.toClass();
        }

        @Override
        public @NotNull String getSimpleName() {
            return rawName.getSimpleName();
        }

        @Override
        public void getName(final @NonNull StringBuilder out) {
            rawName.getName(out);
        }

        @Override
        public void getInternalName(final @NonNull StringBuilder out) {
            rawName.getInternalName(out);
        }

        @Override
        public void getSignature(final @NonNull StringBuilder out) {
            for (int i = 0, j = rawName.getDimensions(); i < j; i++)
                out.append('[');
            out.append('L');
            out.append(rawName.getInternalName());
            out.append('<');
            for (val parameter : parameters) {
                parameter.getSignature(out);
            }
            out.append('>');
            out.append(';');
        }

        @Override
        public void getDescriptor(final @NonNull StringBuilder out) {
            for (int i = 0, j = rawName.getDimensions(); i < j; i++)
                out.append('[');
            out.append('L');
            out.append(rawName.getInternalName());
            out.append(';');
        }

        @Override
        public void toString(final @NonNull StringBuilder out) {
            rawName.dimensions(0).getName(out);
            out.append('<');
            for (int i = 0, j = parameters.length; i < j; i++) {
                if (i != 0) out.append(',').append(' ');
                parameters[i].toString(out);
            }
            out.append('>');
            for (int i = 0, j = rawName.getDimensions(); i < j; i++)
                out.append('[').append(']');
        }

        @Override
        public boolean hasParameterizedTypes() {
            return true;
        }

        @Override
        public int getSize() {
            return rawName.getSize();
        }

        @Override
        public @NotNull ParameterizedTypeName dimensions(final int dimensions) {
            return new ParameterizedTypeNameImpl(rawName.dimensions(dimensions), parameters);
        }
    }


    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class WildcardImpl extends AbstractParameter implements Wildcard {

        TypeName[] upper;
        TypeName[] lower;

        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 31 + Arrays.hashCode(upper);
            hash = hash * 31 + Arrays.hashCode(lower);

            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Wildcard)) return false;

            val that = (Wildcard) obj;

            return Arrays.equals(getUpper(), that.getUpper())
                    && Arrays.equals(getLower(), that.getLower());
        }

        @Override
        public void getSignature(final @NonNull StringBuilder out) {
            val hasLower = lower != null;
            val hasUpper = upper != null;

            if (hasUpper && hasLower && OBJECT.equals(lower[0]) && OBJECT.equals(upper[0])) {
                out.append('*');
            } else if (hasLower) {
                out.append('-');

                for (val lower : lower) {
                    lower.getSignature(out);
                }
            } else if (hasUpper) {
                if (upper.length == 1 && OBJECT.equals(upper[0])) {
                    out.append('*');
                } else {
                    out.append('+');

                    for (val upper : upper) {
                        upper.getSignature(out);
                    }
                }
            } else {
                out.append('*');
            }
        }

        @Override
        public void getDescriptor(final @NonNull StringBuilder out) {
        }

        @Override
        public void toString(final @NonNull StringBuilder out) {
            val hasLower = lower != null;
            val hasUpper = upper != null;

            if (hasUpper && hasLower && OBJECT.equals(lower[0]) && OBJECT.equals(upper[0])) {
                out.append('?');
            } else if (hasLower) {
                out.append("? super ");

                for (int i = 0, j = lower.length; i < j; i++) {
                    if (i != 0) out.append(" & ");
                    lower[i].toString(out);
                }
            } else if (hasUpper) {
                if (upper.length == 1 && OBJECT.equals(upper[0])) {
                    out.append('?');
                } else {
                    out.append("? extends ");

                    for (int i = 0, j = upper.length; i < j; i++) {
                        if (i != 0) out.append(" & ");
                        upper[i].toString(out);
                    }
                }
            } else {
                out.append('?');
            }
        }

        @Override
        public boolean hasParameterizedTypes() {
            return Types.hasParameterizedTypes(upper) || Types.hasParameterizedTypes(lower);
        }

    }


    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class TypeParameterImpl extends AbstractParameter implements TypeParameter {

        String label;
        TypeName[] bounds;

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof TypeParameter)) return false;

            val that = (TypeParameter) obj;
            return Arrays.equals(getBounds(), that.getBounds());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bounds);
        }

        @Override
        public void getSignature(final @NonNull StringBuilder out) {
            out.append('T');
            out.append(label);
            out.append(';');
        }

        @Override
        public void getDescriptor(final @NonNull StringBuilder out) {
        }

        @Override
        public void toString(final @NonNull StringBuilder out) {
            out.append(label);
            if (bounds.length != 0 && !OBJECT.equals(bounds[0])) {
                out.append(" extends ");
                for (int i = 0, j = bounds.length; i < j; i++) {
                    if (i != 0) out.append(' ').append('&').append(' ');
                    bounds[i].toString(out);
                }
            }
        }

        @Override
        public boolean hasParameterizedTypes() {
            return false;
        }

        @Override
        public @NotNull String getDefinitionSignature() {
            return StringUtils.from(this::getDefinitionSignature);
        }

        @Override
        public void getDefinitionSignature(final @NonNull StringBuilder out) {
            out.append(label);

            for (val bound : bounds) {
                out.append(':');
                bound.getSignature(out);
            }
        }

    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ExactTypeNameImpl extends AbstractTypeName implements ExactTypeName {

        final int primitive;
        final int dimensions;
        final String[] array;

        Class<?> originalClass;

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof ExactTypeName)) return false;

            val that = (ExactTypeName) obj;
            
            return dimensions == that.getDimensions()
                    && primitive == that.getPrimitive()
                    && Arrays.equals(split(), that.split());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(array);
        }

        @Override
        public boolean isArray() {
            return dimensions > 0;
        }

        @Override
        public @NotNull TypeName getComponent() {
            if (dimensions < 1) {
                throw new IllegalStateException(this + " should be an array");
            }

            return dimensions(dimensions - 1);
        }

        @Override
        public boolean isPrimitive() {
            return primitive != -1;
        }

        @Override
        public @NotNull org.objectweb.asm.Type toType() {
            return AsmUtils.getType(getInternalName());
        }

        @Override
        @SneakyThrows
        public @NotNull Class<?> toClass() {
            if (originalClass == null) {
                originalClass = Class.forName(getName());
                originalClass = _originalWithDims(0, dimensions);
            }

            return originalClass;
        }

        @Override
        public @NotNull String getSimpleName() {
            return array[array.length - 1];
        }

        @Override
        public void getName(final @NonNull StringBuilder out) {
            for (int i = 0, j = array.length; i < j; i++) {
                if (i != 0) out.append('.');
                out.append(array[i]);
            }

            for (int i = 0; i < dimensions; i++)
                out.append('[').append(']');
        }

        @Override
        public void getInternalName(final @NonNull StringBuilder out) {
            if (getDimensions() > 0) {
                out.append('[');

                val component = getComponent();
                component.getSignature(out);
            } else {
                for (int i = 0, j = array.length; i < j; i++) {
                    if (i != 0) out.append('/');
                    out.append(array[i]);
                }
            }
        }

        @Override
        public void getSignature(final @NonNull StringBuilder out) {
            if (primitive != -1) {
                val descriptor = PRIMITIVE_DESCRIPTORS[primitive];
                out.append(descriptor);
                return;
            }

            if (dimensions > 0) {
                out.append('[');

                val component = getComponent();
                component.getSignature(out);
            } else {
                out.append('L');
                getInternalName(out);
                out.append(';');
            }
        }

        @Override
        public void getDescriptor(final @NonNull StringBuilder out) {
            getSignature(out);
        }

        @Override
        public void toString(final @NonNull StringBuilder out) {
            getName(out);
        }

        @Override
        public boolean hasParameterizedTypes() {
            return false;
        }

        @Override
        public int getSize() {
            return primitive == LONG_TYPE || primitive == DOUBLE_TYPE ? 2 : 1;
        }

        @Override
        public @NotNull String @Nullable [] split() {
            return array;
        }

        @Override
        public @NotNull ParameterizedTypeName parameterized(final @NotNull Parameter @NonNull ... parameters) {
            if (parameters.length < 1) {
                throw new IllegalStateException("You should specify at least one parameter");
            }

            return new ParameterizedTypeNameImpl(this, parameters);
        }

        @Override
        public @NotNull ParameterizedTypeName parameterized(final @NotNull Type @NonNull ... parameters) {
            if (parameters.length < 1) {
                throw new IllegalStateException("You should specify at least one parameter");
            }

            return new ParameterizedTypeNameImpl(this, _fromArray(parameters));
        }

        @Override
        public @NotNull ExactTypeName dimensions(final int dimensions) {
            if (dimensions == this.dimensions) return this;

            val originalClass = this.originalClass != null
                    ? _originalWithDims(this.dimensions, dimensions)
                    : null;

            return dimensions == 0
                    ? _getCacheOrInit(String.join(".", array), 0, false, originalClass)
                    : new ExactTypeNameImpl(primitive, dimensions, array, originalClass);
        }

        @SneakyThrows
        private Class<?> _originalWithDims(final int originalDims, final int dims) {
            Class<?> result = originalClass;

            int diff = Math.abs(originalDims - dims);

            if (originalDims < dims) {
                while (diff > 0) {
                    result = Array.newInstance(result, 0).getClass();
                    diff--;
                }
            } else {
                while (diff > 0) {
                    result = result.getComponentType();
                    diff--;
                }
            }

            return result;
        }
    }

}
