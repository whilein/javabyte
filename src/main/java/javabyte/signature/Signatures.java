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

package javabyte.signature;

import javabyte.type.TypeName;
import javabyte.type.TypeParameter;
import javabyte.type.Types;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author whilein
 */
@UtilityClass
public class Signatures {

    private static TypeParameter[] getTypeParameters(final GenericDeclaration declaration) {
        val typeParameters = declaration.getTypeParameters();
        val typeParameterNames = new TypeParameter[typeParameters.length];

        for (int i = 0, j = typeParameterNames.length; i < j; i++) {
            val typeParameter = typeParameters[i];
            typeParameterNames[i] = Types.variable(typeParameter.getName(), Types.of(typeParameter.getBounds()));
        }

        return typeParameterNames;
    }

    private static MethodSignature _methodSignature(
            final TypeName returnType,
            final TypeName[] parameters
    ) {
        return new MethodSignatureImpl(new TypeParameter[0], returnType, parameters);
    }

    public static @NotNull MethodSignature methodSignature(
            final @NotNull TypeParameter @NonNull [] variables,
            final @NonNull TypeName returnType,
            final @NotNull TypeName @NonNull [] parameters
    ) {
        return new MethodSignatureImpl(variables, returnType, parameters);
    }

    public static @NotNull MethodSignature methodSignature(
            final @NonNull TypeName returnType,
            final @NotNull TypeName @NonNull ... parameters
    ) {
        return _methodSignature(returnType, parameters);
    }

    public static @NotNull MethodSignature methodSignature(
            final @NonNull Type returnType,
            final @NotNull Type @NonNull ... parameters
    ) {
        return _methodSignature(Types.of(returnType), Types.of(parameters));
    }

    public static @NotNull ClassSignature classSignature(
            final @NotNull TypeParameter @NonNull [] variables,
            final @NonNull TypeName superName,
            final @NotNull TypeName @Nullable [] interfaces
    ) {
        return new ClassSignatureImpl(variables, superName, interfaces);
    }

    public static @NotNull ClassSignature classSignature(
            final @NonNull TypeName superName,
            final @NotNull TypeName @Nullable [] interfaces
    ) {
        return new ClassSignatureImpl(new TypeParameter[0], superName, interfaces);
    }


    public static @NotNull ClassSignature ofClass(final @NonNull Class<?> cls) {
        val superName = Types.of(cls.getGenericSuperclass());
        val interfaces = Types.of(cls.getGenericInterfaces());

        val typeParameters = getTypeParameters(cls);

        return new ClassSignatureImpl(typeParameters, superName, interfaces);
    }

    public static @NotNull MethodSignature ofMethod(final @NonNull Method method) {
        val returnName = Types.of(method.getGenericReturnType());
        val parameterNames = Types.of(method.getGenericParameterTypes());

        val typeParameters = getTypeParameters(method);

        return new MethodSignatureImpl(typeParameters, returnName, parameterNames);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ClassSignatureImpl implements ClassSignature {

        TypeParameter[] generic;
        TypeName superName;
        TypeName[] interfaces;

        @Override
        public @NotNull String getSignature() {
            val out = new StringBuilder();
            getSignature(out);

            return out.toString();
        }

        @Override
        public void getSignature(final @NonNull StringBuilder out) {
            if (generic.length != 0) {
                out.append('<');
                for (val generic : generic) {
                    generic.getDefinitionSignature(out);
                }
                out.append('>');
            }

            superName.getSignature(out);

            if (interfaces != null) {
                for (val parameterType : interfaces) {
                    parameterType.getSignature(out);
                }
            }
        }

        @Override
        public String toString() {
            val out = new StringBuilder();

            out.append("Dummy");

            if (generic.length != 0) {
                out.append('<');
                for (int i = 0, j = generic.length; i < j; i++) {
                    if (i != 0) out.append(',').append(' ');
                    generic[i].toString(out);
                }
                out.append('>');
            }

            if (superName != null && !Types.OBJECT.equals(superName)) {
                out.append(" extends ");
                superName.toString(out);
            }

            if (interfaces != null && interfaces.length != 0) {
                out.append(" implements ");
                for (int i = 0, j = interfaces.length; i < j; i++) {
                    if (i != 0) out.append(',').append(' ');
                    interfaces[i].toString(out);
                }
            }

            return out.toString();
        }

    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MethodSignatureImpl implements MethodSignature {

        TypeParameter[] generic;
        TypeName returnType;
        TypeName[] parameterTypes;

        @Override
        public @NotNull String getSignature() {
            val out = new StringBuilder();
            getSignature(out);

            return out.toString();
        }

        @Override
        public void getSignature(final @NonNull StringBuilder out) {
            if (generic.length != 0) {
                out.append('<');
                for (val generic : generic) {
                    generic.getDefinitionSignature(out);
                }
                out.append('>');
            }
            out.append('(');
            for (val parameterType : parameterTypes) {
                parameterType.getSignature(out);
            }
            out.append(')');
            returnType.getSignature(out);
        }

        @Override
        public String toString() {
            val out = new StringBuilder();
            if (generic.length != 0) {
                out.append('<');
                for (int i = 0, j = generic.length; i < j; i++) {
                    if (i != 0) out.append(',').append(' ');
                    generic[i].toString(out);
                }
                out.append('>').append(' ');
            }
            returnType.toString(out);
            out.append(" dummy").append('(');
            for (int i = 0, j = parameterTypes.length; i < j; i++) {
                if (i != 0) out.append(',').append(' ');
                parameterTypes[i].toString(out);
            }
            out.append(')').append(';');
            return out.toString();
        }

        @Override
        public @NotNull String getDescriptor() {
            val out = new StringBuilder();
            getDescriptor(out);

            return out.toString();
        }

        @Override
        public void getDescriptor(final @NonNull StringBuilder out) {
            out.append('(');
            for (val parameterType : parameterTypes) {
                parameterType.getDescriptor(out);
            }
            out.append(')');
            returnType.getDescriptor(out);
        }

        @Override
        public boolean hasParameterizedTypes() {
            return returnType.hasParameterizedTypes() || Types.hasParameterizedTypes(parameterTypes);
        }
    }


}
