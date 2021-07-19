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

package javabyte.name;

import javabyte.util.AsmUtils;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author whilein
 */
@UtilityClass
public class Names {

    private static final Map<String, ExactName> CACHE_INTERNAL = new HashMap<>();
    private static final Map<String, ExactName> CACHE = new HashMap<>();

    private static final char[] PRIMITIVE_DESCRIPTORS = "VZBSCIJFD".toCharArray();

    // @formatter:off
    public static final int
            VOID_TYPE   = 0,
            BOOL_TYPE   = 1,
            BYTE_TYPE   = 2,
            SHORT_TYPE  = 3,
            CHAR_TYPE   = 4,
            INT_TYPE    = 5,
            LONG_TYPE   = 6,
            FLOAT_TYPE  = 7,
            DOUBLE_TYPE = 8;

    public static final ExactName
            VOID        = Names._putCache(VOID_TYPE, Void.TYPE),
            VOID_OBJ    = Names._putCache(Void.class),
            BOOL        = Names._putCache(BOOL_TYPE, Boolean.TYPE),
            BOOL_OBJ    = Names._putCache(Boolean.class),
            BYTE        = Names._putCache(BYTE_TYPE, Byte.TYPE),
            BYTE_OBJ    = Names._putCache(Byte.class),
            CHAR        = Names._putCache(CHAR_TYPE, Character.TYPE),
            CHAR_OBJ    = Names._putCache(Character.class),
            SHORT       = Names._putCache(SHORT_TYPE, Short.TYPE),
            SHORT_OBJ   = Names._putCache(Short.class),
            INT         = Names._putCache(INT_TYPE, Integer.TYPE),
            INT_OBJ     = Names._putCache(Integer.class),
            LONG        = Names._putCache(LONG_TYPE, Long.TYPE),
            LONG_OBJ    = Names._putCache(Long.class),
            FLOAT       = Names._putCache(FLOAT_TYPE, Float.TYPE),
            FLOAT_OBJ   = Names._putCache(Float.class),
            DOUBLE      = Names._putCache(DOUBLE_TYPE, Double.TYPE),
            DOUBLE_OBJ  = Names._putCache(Double.class),
            STRING      = Names._putCache(String.class),
            NUMBER      = Names._putCache(Number.class),
            OBJECT      = Names._putCache(Object.class);
    // @formatter:on

    private final ExactName[] WRAPPERS = {
            VOID_OBJ, BOOL_OBJ,
            BYTE_OBJ, CHAR_OBJ,
            SHORT_OBJ, INT_OBJ,
            LONG_OBJ, FLOAT_OBJ,
            DOUBLE_OBJ
    };

    private final ExactName[] PRIMITIVES = {
            VOID, BOOL,
            BYTE, CHAR,
            SHORT, INT,
            LONG, FLOAT,
            DOUBLE
    };

    public final WildcardName WILDCARD_ANY
            = new WildcardNameImpl(null, null);

    public @NotNull ExactName getWrapper(final int primitive) {
        return WRAPPERS[primitive];
    }

    public @NotNull ExactName getPrimitive(final @NonNull Name wrapper) {
        for (int i = 0, j = WRAPPERS.length; i < j; i++)
            if (WRAPPERS[i].equals(wrapper))
                return PRIMITIVES[i];

        throw new IllegalStateException(wrapper + " isn't wrapper");
    }

    public @NotNull ExactName getWrapper(final @NonNull Name primitive) {
        return WRAPPERS[primitive.getPrimitive()];
    }

    public @NotNull ExactName of(final @NonNull Class<?> cls) {
        return _of(cls);
    }

    private ExactName _of(final Class<?> cls) {
        int dimensions = 0;
        Class<?> component = cls;

        while (component.isArray()) {
            dimensions++;
            component = component.getComponentType();
        }

        return _getCacheOrInit(component.getName(), dimensions,false);
    }

    public @NotNull Name of(final @NonNull Type type) {
        return _fromType(type);
    }

    public @NotNull Name @NotNull [] of(final @NotNull Type @NonNull ... types) {
        return _fromArray(types);
    }

    public @NotNull ExactName of(final @NonNull String name) {
        return _fromName(name, 0);
    }

    public @NotNull ExactName ofInternal(final @NonNull String internalName) {
        return _getCacheOrInit(internalName, 0, true);
    }

    public @NotNull ExactName @NotNull [] of(final @NotNull String @NonNull [] names) {
        return _fromArray(names, false);
    }

    public @NotNull ExactName @NotNull [] of(final @NotNull Class<?> @NonNull [] types) {
        return _fromArray(types);
    }


    public @NotNull ExactName @NotNull [] ofInternal(final @NonNull String @NotNull [] internalNames) {
        return _fromArray(internalNames, true);
    }

    public @NotNull VariableName variable(
            final @NonNull String label,
            final @NotNull Name @NonNull [] bounds
    ) {
        return new VariableNameImpl(label, bounds);
    }

    public @NotNull ParameterizedName parameterized(
            final @NonNull ExactName name,
            final @NotNull Name @NonNull ... parameters
    ) {
        if (parameters.length < 1) {
            throw new IllegalStateException("You should specify at least one parameter");
        }

        return new ParameterizedNameImpl(name, parameters);
    }

    public @NotNull WildcardName wildcardUpper(final @NotNull Name @NonNull ... names) {
        if (names.length < 1) {
            throw new IllegalStateException("You should specify at least one name");
        }

        return new WildcardNameImpl(names, null);
    }

    public @NotNull WildcardName wildcardLower(final @NotNull Name @NonNull ... names) {
        if (names.length < 1) {
            throw new IllegalStateException("You should specify at least one name");
        }

        return new WildcardNameImpl(null, names);
    }

    public boolean hasParameterizedTypes(final @NonNull Iterable<@NotNull Name> names) {
        for (val name : names)
            if (name.hasParameterizedTypes())
                return true;

        return false;
    }

    public boolean hasParameterizedTypes(final @NotNull Name @NonNull ... names) {
        for (val name : names)
            if (name.hasParameterizedTypes())
                return true;

        return false;
    }

    private ExactName _putCache(final Class<?> type) {
        return _putCache(-1, type);
    }

    private ExactName _putCache(final int primitive, final Class<?> type) {
        val name = _fromName(type.getName(), primitive, 0, "\\.");

        CACHE.put(type.getName(), name);
        CACHE_INTERNAL.put(type.getName().replace('.', '/'), name);

        return name;
    }

    private ExactName _getCacheOrInit(final String type, final int dimensions, final boolean internal) {
        if (dimensions == 0) {
            val cache = internal ? CACHE_INTERNAL.get(type) : CACHE.get(type);
            if (cache != null) return cache;
        }

        return _fromName(type, -1, dimensions, internal ? "/" : "\\.");
    }

    private ExactName _fromName(final String name, final int primitive, final int dimensions, final String separator) {
        return new ExactNameImpl(primitive, dimensions, name.split(separator));
    }

    private ExactName _fromName(final String name, final int dimensions) {
        return _fromName(name, -1, dimensions, "\\.");
    }

    private Name _fromType(final Type type) {
        if (type instanceof Class<?>) {
            return _of((Class<?>) type);
        } else if (type instanceof ParameterizedType) {
            val parameterized = (ParameterizedType) type;

            val rawType = parameterized.getRawType();
            val parameterTypes = parameterized.getActualTypeArguments();

            val raw = _fromType(rawType);
            val parameters = _fromArray(parameterTypes);

            return new ParameterizedNameImpl((ExactName) raw, parameters);
        } else if (type instanceof WildcardType) {
            val wildcard = (WildcardType) type;
            val upperTypes = wildcard.getUpperBounds();
            val lowerTypes = wildcard.getLowerBounds();

            val upper = upperTypes.length != 0 ? _fromArray(upperTypes) : null;
            val lower = lowerTypes.length != 0 ? _fromArray(lowerTypes) : null;

            return new WildcardNameImpl(upper, lower);
        } else if (type instanceof TypeVariable<?>) {
            val variable = (TypeVariable<?>) type;

            val boundTypes = variable.getBounds();
            val bounds = _fromArray(boundTypes);

            return new VariableNameImpl(variable.getName(), bounds);
        } else {
            throw new IllegalArgumentException(type.getClass().getName() + " is not supported");
        }
    }

    private Name[] _fromArray(final Type... types) {
        val names = new Name[types.length];

        for (int i = 0, j = names.length; i < j; i++)
            names[i] = _fromType(types[i]);

        return names;
    }

    private ExactName[] _fromArray(final String[] names, final boolean internal) {
        val exactNames = new ExactName[names.length];

        for (int i = 0, j = names.length; i < j; i++)
            exactNames[i] = Names._getCacheOrInit(names[i], 0, internal);

        return exactNames;
    }

    private ExactName[] _fromArray(final Class<?>[] types) {
        val exactNames = new ExactName[types.length];

        for (int i = 0, j = types.length; i < j; i++)
            exactNames[i] = _of(types[i]);

        return exactNames;
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ParameterizedNameImpl implements ParameterizedName {

        ExactName rawName;
        Name[] parameters;

        @Override
        public String toString() {
            val out = new StringBuilder();
            toString(out);

            return out.toString();
        }

        @Override
        public int getDimensions() {
            return rawName.getDimensions();
        }

        @Override
        public boolean isArray() {
            return rawName.isArray();
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
        public @NotNull org.objectweb.asm.Type getType() {
            return rawName.getType();
        }

        @Override
        public @NotNull String getInternalName() {
            return rawName.getInternalName();
        }

        @Override
        public @NotNull String getName() {
            return rawName.getName();
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
            out.append(rawName.getName());
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
        public @NotNull String getSignature() {
            val out = new StringBuilder();
            getSignature(out);

            return out.toString();
        }

        @Override
        public @NotNull String getDescriptor() {
            val out = new StringBuilder();
            getDescriptor(out);

            return out.toString();
        }

        @Override
        public @NotNull ParameterizedName dimensions(final int dimensions) {
            return new ParameterizedNameImpl(rawName.dimensions(dimensions), parameters);
        }
    }


    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class WildcardNameImpl implements WildcardName {

        Name[] upper;
        Name[] lower;

        @Override
        public int hashCode() {
            int hash = 1;
            hash = hash * 31 + Arrays.hashCode(upper);
            hash = hash * 31 + Arrays.hashCode(lower);

            return hash;
        }

        @Override
        public int getDimensions() {
            return 0;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public int getPrimitive() {
            return -1;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public @NotNull org.objectweb.asm.Type getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof WildcardName)) return false;

            val that = (WildcardName) obj;

            return Arrays.equals(getUpper(), that.getUpper())
                    && Arrays.equals(getLower(), that.getLower());
        }

        @Override
        public String toString() {
            val out = new StringBuilder();
            toString(out);

            return out.toString();
        }

        @Override
        public @NotNull String getInternalName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull String getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getName(final @NonNull StringBuilder out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getInternalName(final @NonNull StringBuilder out) {
            throw new UnsupportedOperationException();
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
            return Names.hasParameterizedTypes(upper) || Names.hasParameterizedTypes(lower);
        }

        @Override
        public int getSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull String getSignature() {
            val out = new StringBuilder();
            getSignature(out);

            return out.toString();
        }

        @Override
        public @NotNull String getDescriptor() {
            return "";
        }

    }


    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class VariableNameImpl implements VariableName {

        String label;
        Name[] bounds;

        @Override
        public int getDimensions() {
            return 0;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public int getPrimitive() {
            return -1;
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public @NotNull org.objectweb.asm.Type getType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof VariableName)) return false;

            val that = (VariableName) obj;
            return Arrays.equals(getBounds(), that.getBounds());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bounds);
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public @NotNull String getInternalName() {
            return getName();
        }

        @Override
        public @NotNull String getSignature() {
            return 'T' + label + ';';
        }

        @Override
        public @NotNull String getDescriptor() {
            return "";
        }

        @Override
        public @NotNull String getName() {
            return label;
        }

        @Override
        public void getName(final @NonNull StringBuilder out) {
            out.append(getName());
        }

        @Override
        public void getInternalName(final @NonNull StringBuilder out) {
            out.append(getName());
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
        }

        @Override
        public boolean hasParameterizedTypes() {
            return false;
        }

        @Override
        public int getSize() {
            return 1;
        }

        @Override
        public @NotNull String getDefinitionSignature() {
            return "";
        }

        @Override
        public @NotNull String getDefinition() {
            return label;
        }

        @Override
        public void getDefinitionSignature(final @NonNull StringBuilder out) {
            out.append(label);

            for (val bound : bounds) {
                out.append(':');
                bound.getSignature(out);
            }
        }

        @Override
        public void getDefinition(final @NonNull StringBuilder out) {
            out.append(label);
            if (bounds.length != 0 && !OBJECT.equals(bounds[0])) {
                out.append(" extends ");
                for (int i = 0, j = bounds.length; i < j; i++) {
                    if (i != 0) out.append(' ').append('&').append(' ');
                    bounds[i].toString(out);
                }
            }
        }
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ExactNameImpl implements ExactName {

        int primitive;
        int dimensions;
        String[] array;

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof ExactName)) return false;

            val that = (ExactName) obj;
            return Arrays.equals(split(), that.split());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(array);
        }

        @Override
        public String toString() {
            return getName();
        }

        @Override
        public boolean isArray() {
            return dimensions > 0;
        }

        @Override
        public boolean isPrimitive() {
            return primitive != -1;
        }

        @Override
        public @NotNull org.objectweb.asm.Type getType() {
            return AsmUtils.getType(array);
        }

        @Override
        public @NotNull String getInternalName() {
            return String.join("/", array);
        }

        @Override
        public @NotNull String getSignature() {
            val out = new StringBuilder();
            getSignature(out);

            return out.toString();
        }

        @Override
        public @NotNull String getDescriptor() {
            return getSignature();
        }

        @Override
        public @NotNull String getName() {
            return String.join(".", array);
        }

        @Override
        public void getName(final @NonNull StringBuilder out) {
            for (int i = 0, j = array.length; i < j; i++) {
                if (i != 0) out.append('.');
                out.append(array[i]);
            }
        }

        @Override
        public void getInternalName(final @NonNull StringBuilder out) {
            for (int i = 0, j = array.length; i < j; i++) {
                if (i != 0) out.append('/');
                out.append(array[i]);
            }
        }

        @Override
        public void getSignature(final @NonNull StringBuilder out) {
            if (primitive != -1) {
                val descriptor = PRIMITIVE_DESCRIPTORS[primitive];
                out.append(descriptor);
                return;
            }

            for (int i = 0; i < dimensions; i++) {
                out.append('[');
            }

            out.append('L');
            getInternalName(out);
            out.append(';');
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
        public @NotNull ParameterizedName parameterized(final @NotNull Name @NonNull ... parameters) {
            if (parameters.length < 1) {
                throw new IllegalStateException("You should specify at least one parameter");
            }

            return new ParameterizedNameImpl(this, parameters);
        }

        @Override
        public @NotNull ParameterizedName parameterized(final @NotNull Type @NonNull ... parameters) {
            if (parameters.length < 1) {
                throw new IllegalStateException("You should specify at least one parameter");
            }

            return new ParameterizedNameImpl(this, _fromArray(parameters));
        }

        @Override
        public @NotNull ExactName dimensions(final int dimensions) {
            return new ExactNameImpl(primitive, dimensions, array);
        }
    }

}
