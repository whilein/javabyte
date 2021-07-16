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

package javabyte;

import javabyte.bytecode.AsmBytecode;
import javabyte.bytecode.Bytecode;
import javabyte.make.*;
import javabyte.name.ExactName;
import javabyte.name.Name;
import javabyte.name.Names;
import javabyte.name.VariableName;
import javabyte.signature.MethodSignature;
import javabyte.signature.Signatures;
import javabyte.type.Access;
import javabyte.type.MethodOpcode;
import javabyte.type.Version;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author whilein
 */
@UtilityClass
public class Javabyte {

    private MakeClass _make(
            final Version version,
            final ExactName name
    ) {
        return new MakeClassImpl(version, name, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), Names.OBJECT);
    }

    public @NotNull MakeClass make(
            final @NonNull Version version,
            final @NonNull String name
    ) {
        return _make(version, Names.of(name));
    }

    public @NotNull MakeClass make(
            final @NonNull String name
    ) {
        return _make(Version.V1_8, Names.of(name));
    }

    public @NotNull MakeClass make(
            final @NonNull Version version,
            final @NonNull ExactName name
    ) {
        return _make(version, name);
    }

    public @NotNull MakeClass make(
            final @NonNull ExactName name
    ) {
        return _make(Version.V1_8, name);
    }

    @FieldDefaults(level = AccessLevel.PROTECTED)
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class MakeElementImpl implements MakeElement {
        @Getter
        @Setter
        int modifiers;

        public void setAccess(final @NonNull Access access) {
            setModifiers((modifiers & ~0b111) | access.getOpcode());
        }

        @Override
        public @NotNull Access getAccess() {
            switch (this.modifiers & 0b111) {
                default:
                case 0: return Access.PACKAGE;
                case 1: return Access.PUBLIC;
                case 2: return Access.PRIVATE;
                case 4: return Access.PROTECTED;
            }
        }

        @Override
        public void setFinal(final boolean flag) {
            if (flag) {
                setModifiers(modifiers |= Opcodes.ACC_FINAL);
            } else {
                setModifiers(modifiers &= ~Opcodes.ACC_FINAL);
            }
        }

        @Override
        public void setStatic(final boolean flag) {
            if (flag) {
                setModifiers(modifiers |= Opcodes.ACC_STATIC);
            } else {
                setModifiers(modifiers &= ~Opcodes.ACC_STATIC);
            }
        }

        @Override
        public boolean isFinal() {
            return (modifiers & Opcodes.ACC_FINAL) != 0;
        }

        @Override
        public boolean isStatic() {
            return (modifiers & Opcodes.ACC_STATIC) != 0;
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MakeClassImpl extends MakeElementImpl implements MakeClass {

        private static final Method INVOKE_DEFINE_CLASS;

        static {
            try {
                INVOKE_DEFINE_CLASS = ClassLoader.class.getDeclaredMethod("defineClass",
                        String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
                INVOKE_DEFINE_CLASS.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Getter
        final Version version;

        @Getter
        final ExactName name;

        @Getter
        final List<Name> interfaces;

        final List<MakeFieldImpl> fields;

        final List<MakeExecutableImpl> executables;

        @Getter
        @NotNull
        Name superName;

        MakeConstructorImpl staticConstructor;

        private ClassWriter makeClassWriter() {
            val cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

            final String[] interfaceNames;

            if (!interfaces.isEmpty()) {
                interfaceNames = interfaces.stream()
                        .map(Name::getInternalName)
                        .toArray(String[]::new);
            } else {
                interfaceNames = null;
            }

            final String classSignature;

            if (superName.hasParameterizedTypes() || Names.hasParameterizedTypes(interfaces)) {
                classSignature = Signatures.classSignature(
                        new VariableName[0], superName,
                        interfaces.toArray(new Name[0])
                ).getSignature();
            } else {
                classSignature = null;
            }

            cw.visit(version.getNumber(), modifiers, name.getInternalName(), classSignature,
                    superName.getInternalName(), interfaceNames);

            for (val field : fields) {
                val fieldType = field.getType();

                val signature = fieldType.hasParameterizedTypes()
                        ? fieldType.getSignature()
                        : null;

                cw.visitField(field.getModifiers(), field.getName(), fieldType.getDescriptor(), signature, null)
                        .visitEnd();
            }

            boolean hasConstructors = false;

            for (val executable : executables) {
                addExecutable(cw, executable);

                if (executable.getName().equals("<init>")) {
                    hasConstructors = true;
                }
            }

            if (!hasConstructors) {
                val emptyConstructor = _initConstructor(false);
                emptyConstructor.setAccess(Access.PUBLIC);

                val emptyConstructorCode = emptyConstructor.getBytecode();
                emptyConstructorCode.loadLocal(0);

                emptyConstructorCode.methodInsn("<init>")
                        .inSuper()
                        .opcode(MethodOpcode.SPECIAL)
                        .descriptor(void.class);

                emptyConstructorCode.callReturn();

                addExecutable(cw, emptyConstructor);
            }

            return cw;
        }

        private void addExecutable(final ClassWriter writer, final MakeExecutableImpl executable) {
            val exceptions = executable.getExceptions().stream()
                    .map(ExactName::getName)
                    .toArray(String[]::new);

            val methodSignature = Signatures.methodSignature(executable.getReturnType(),
                    executable.getParameters().toArray(new Name[0]));

            val signature = methodSignature.hasParameterizedTypes()
                    ? methodSignature.getSignature()
                    : null;

            val mv = writer.visitMethod(executable.getModifiers(), executable.getName(),
                    methodSignature.getDescriptor(), signature, exceptions);

            mv.visitCode();

            if (executable instanceof MakeMethodImpl) {
                val method = (MakeMethodImpl) executable;

                if (method.shouldMakeBridge()) {
                    val overrides = executable.overrides;
                    val bridge = _initMethod(executable.getName(), overrides.returnType);

                    bridge.setParameters(overrides.parameterTypes);
                    bridge.setModifiers(executable.getAccess().getOpcode() | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC);

                    val code = bridge.getBytecode();
                    code.loadLocal(0);
                    code.methodInsn(executable.getName())
                            .inCurrent()
                            .opcode(MethodOpcode.VIRTUAL)
                            .descriptor(executable.getReturnType());
                    code.callReturn();

                    addExecutable(writer, bridge);
                }
            }

            executable.getBytecode().compile(executable, mv);

            mv.visitEnd();
        }

        private MakeConstructorImpl _initConstructor(final boolean isStatic) {
            return new MakeConstructorImpl(AsmBytecode.create(), this,
                    isStatic ? "<clinit>" : "<init>", Names.VOID,
                    new ArrayList<>(), new ArrayList<>(), isStatic);
        }

        private MakeMethodImpl _initMethod(final String name, final Name returns) {
            return new MakeMethodImpl(AsmBytecode.create(), this,
                    name, returns, new ArrayList<>(), new ArrayList<>());
        }

        private <T extends MakeExecutableImpl> T _addExecutable(final T executable) {
            executables.add(executable);
            return executable;
        }

        @Override
        public @NotNull MakeConstructor addConstructor() {
            return _addExecutable(_initConstructor(false));
        }

        @Override
        public @NotNull MakeConstructor getStaticConstructor() {
            if (staticConstructor == null) {
                val clinit = _addExecutable(_initConstructor(true));
                clinit.setStatic(true);

                this.staticConstructor = clinit;
            }

            return staticConstructor;
        }

        @Override
        public @NotNull MakeMethod addMethod(final @NonNull String name, final @NonNull Name returns) {
            return _addExecutable(_initMethod(name, returns));
        }

        @Override
        public @NotNull MakeMethod addMethod(final @NonNull String name, final @NonNull Type returns) {
            return _addExecutable(_initMethod(name, Names.of(returns)));
        }

        @Override
        public @NotNull MakeMethod addVoidMethod(final @NonNull String name) {
            return _addExecutable(_initMethod(name, Names.VOID));
        }

        private MakeField _addField(final String name, final Name type) {
            val field = new MakeFieldImpl(this, name, type);
            fields.add(field);

            return field;
        }

        @Override
        public @NotNull MakeField addField(final @NonNull String name, final @NonNull Name type) {
            return _addField(name, type);
        }

        @Override
        public @NotNull MakeField addField(final @NonNull String name, final @NonNull Type type) {
            return _addField(name, Names.of(type));
        }

        @Override
        public void setSuperName(final @NonNull Type type) {
            this.superName = Names.of(type);
        }

        @Override
        public void setSuperName(final @NonNull Name name) {
            this.superName = name;
        }

        @Override
        public void addInterface(final @NonNull Name name) {
            this.interfaces.add(name);
        }

        @Override
        public void addInterface(final @NonNull Type type) {
            this.interfaces.add(Names.of(type));
        }

        @Override
        public void setInterfaces(final @NotNull Name @NotNull ... interfaces) {
            this.interfaces.clear();
            this.interfaces.addAll(Arrays.asList(interfaces));
        }

        @Override
        public void setInterfaces(final @NonNull Collection<@NotNull Name> interfaces) {
            this.interfaces.clear();
            this.interfaces.addAll(interfaces);
        }

        @Override
        public void setInterfaceTypes(final @NotNull Type @NotNull ... interfaces) {
            _setInterfaces(Arrays.stream(interfaces).map(Names::of).collect(Collectors.toList()));
        }

        @Override
        public void setInterfaceTypes(final @NonNull Collection<@NotNull Type> interfaces) {
            _setInterfaces(interfaces.stream().map(Names::of).collect(Collectors.toList()));
        }

        @Override
        public @NotNull Class<?> load(final @NonNull ClassLoader loader) {
            val bytes = writeAsBytes();

            try {
                return (Class<?>) INVOKE_DEFINE_CLASS.invoke(loader, name.getName(),
                        bytes, 0, bytes.length, null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void writeClass(final @NonNull OutputStream os) throws IOException {
            os.write(writeAsBytes());
        }

        @Override
        public void writeClass(final @NonNull File file) throws IOException {
            try (val output = new FileOutputStream(file)) {
                writeClass(output);
            }
        }

        @Override
        public void writeClass(final @NonNull Path path) throws IOException {
            try (val output = Files.newOutputStream(path)) {
                writeClass(output);
            }
        }

        @Override
        public byte @NotNull [] writeAsBytes() {
            return makeClassWriter().toByteArray();
        }

        private void _setInterfaces(final Collection<Name> interfaces) {
            this.interfaces.clear();
            this.interfaces.addAll(interfaces);
        }


    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MakeFieldImpl extends MakeElementImpl implements MakeField {
        MakeClass declaringClass;
        String name;
        Name type;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class MakeConstructorImpl extends MakeExecutableImpl implements MakeConstructor {

        boolean isStatic;

        private MakeConstructorImpl(
                final Bytecode bytecode,
                final MakeClass declaringClass,
                final String name,
                final Name returnType,
                final List<Name> parameters,
                final List<ExactName> exceptions,
                final boolean isStatic
        ) {
            super(bytecode, declaringClass, name, returnType, parameters, exceptions);

            this.isStatic = isStatic;
        }

        @Override
        public void setModifiers(final int modifiers) {
            if ((modifiers & Opcodes.ACC_FINAL) != 0) {
                throw new UnsupportedOperationException("Cannot change final modifier for constructor");
            }

            if ((modifiers & Opcodes.ACC_STATIC) == 0 == isStatic) {
                throw new UnsupportedOperationException("Cannot change static modifier for constructor");
            }

            super.setModifiers(modifiers);
        }
    }

    private static final class MakeMethodImpl extends MakeExecutableImpl implements MakeMethod {

        private MakeMethodImpl(
                final Bytecode bytecode,
                final MakeClass declaringClass,
                final String name,
                final Name returnType,
                final List<Name> parameters,
                final List<ExactName> exceptions
        ) {
            super(bytecode, declaringClass, name, returnType, parameters, exceptions);
        }

        @Override
        public void setOverrides(
                final @NonNull Class<?> type,
                final @NonNull String name
        ) {
            val methods = Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> method.getName().equals(name))
                    .collect(Collectors.toList());

            if (methods.isEmpty()) {
                throw new IllegalStateException("Cannot override method "
                        + type.getDeclaringClass().getName() + "#" + name
                        + ": No method found");
            }

            if (methods.size() > 1) {
                throw new IllegalStateException("Cannot override method "
                        + type.getDeclaringClass().getName() + "#" + name
                        + ": The class has multiple methods with name " + name);
            }

            val method = methods.get(0);
            _setOverrides(type, name, method.getReturnType(), method.getParameterTypes());
        }

        public void _setOverrides(
                final Class<?> type,
                final String name,
                final Name returnType,
                final Name... parameterTypes
        ) {
            this.overrides = new Overrides(type, name, returnType, parameterTypes);
        }

        private void _setOverrides(
                final Class<?> type,
                final String name,
                final Class<?> returnType,
                final Class<?>... parameterTypes
        ) {
            _setOverrides(type, name, Names.exact(returnType), Names.exact(parameterTypes));
        }

        @Override
        public void setOverrides(
                final @NonNull Class<?> type,
                final @NonNull String name,
                final @NonNull Class<?> returnType,
                final @NotNull Class<?> @NotNull ... parameterTypes
        ) {
            _setOverrides(type, name, returnType, parameterTypes);
        }

        @Override
        public void setOverrides(
                final @NonNull Class<?> type,
                final @NonNull String name,
                final @NonNull Name returnType,
                final @NotNull Name @NotNull ... parameterTypes
        ) {
            _setOverrides(type, name, returnType, parameterTypes);
        }

        private boolean shouldMakeBridge() {
            if (isStatic() || overrides == null) return false;

            return !returnType.equals(overrides.returnType) ||
                    !parameters.equals(Arrays.asList(overrides.parameterTypes));
        }


    }

    @FieldDefaults(level = AccessLevel.PROTECTED)
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class MakeExecutableImpl extends MakeElementImpl implements MakeExecutable {

        @Getter
        final Bytecode bytecode;

        @Getter
        final MakeClass declaringClass;

        @Getter
        final String name;

        @Getter
        final Name returnType;

        @Getter
        final List<Name> parameters;

        @Getter
        final List<ExactName> exceptions;

        Overrides overrides;

        @Override
        public final @NotNull MethodSignature getSignature() {
            return Signatures.methodSignature(returnType, parameters.toArray(new Name[0]));
        }

        @Override
        public final void addException(final @NonNull Class<?> type) {
            exceptions.add(Names.exact(type));
        }

        @Override
        public final void addException(final @NonNull ExactName name) {
            exceptions.add(name);
        }

        @Override
        public final void setExceptionTypes(final @NonNull Collection<@NotNull Class<?>> types) {
            _setExceptions(types.stream().map(Names::exact).collect(Collectors.toList()));
        }

        @Override
        public final void setExceptionTypes(final @NotNull Class<?> @NotNull ... types) {
            _setExceptions(Arrays.stream(types).map(Names::exact).collect(Collectors.toList()));
        }

        @Override
        public final void setExceptions(final @NonNull Collection<@NotNull ExactName> names) {
            _setExceptions(names);
        }

        @Override
        public final void setExceptions(final @NotNull ExactName @NotNull ... names) {
            _setExceptions(Arrays.asList(names));
        }

        @Override
        public final void addParameter(final @NonNull Name type) {
            parameters.add(type);
        }

        @Override
        public final void addParameter(final int i, final @NonNull Name parameter) {
            parameters.add(i, parameter);
        }

        @Override
        public final void addParameter(final @NonNull Type type) {
            parameters.add(Names.of(type));
        }

        @Override
        public final void addParameter(final int i, final @NonNull Type type) {
            parameters.add(i, Names.of(type));
        }

        @Override
        public final void addParameter(final @NonNull String name) {
            parameters.add(Names.of(name));
        }

        @Override
        public final void addParameter(final int i, final @NonNull String name) {
            parameters.add(i, Names.of(name));
        }

        @Override
        public final void setParameterTypes(final @NonNull Collection<@NotNull Type> types) {
            this._setParameters(types.stream().map(Names::of).collect(Collectors.toList()));
        }

        @Override
        public final void setParameterTypes(final @NotNull Type @NonNull ... types) {
            this._setParameters(Arrays.stream(types).map(Names::of).collect(Collectors.toList()));
        }

        @Override
        public final void setParameters(final @NonNull Collection<@NotNull Name> parameters) {
            _setParameters(parameters);
        }

        @Override
        public final void setParameters(final @NotNull Name @NotNull ... parameters) {
            _setParameters(Arrays.asList(parameters));
        }

        private void _setParameters(final Collection<Name> parameters) {
            this.parameters.clear();
            this.parameters.addAll(parameters);
        }

        private void _setExceptions(final Collection<ExactName> exceptions) {
            this.exceptions.clear();
            this.exceptions.addAll(exceptions);
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Overrides {
        Class<?> type;
        String name;
        Name returnType;
        Name[] parameterTypes;
    }

}
