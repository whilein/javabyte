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

import javabyte.bytecode.Bytecode;
import javabyte.bytecode.ExecutableInstructionSet;
import javabyte.bytecode.InstructionSet;
import javabyte.make.MakeClass;
import javabyte.make.MakeConstructor;
import javabyte.make.MakeElement;
import javabyte.make.MakeExecutable;
import javabyte.make.MakeField;
import javabyte.make.MakeHashCodeAndEquals;
import javabyte.make.MakeInnerClass;
import javabyte.make.MakeMethod;
import javabyte.make.MakeToString;
import javabyte.opcode.CompareOpcode;
import javabyte.opcode.FieldOpcode;
import javabyte.opcode.JumpOpcode;
import javabyte.opcode.MathOpcode;
import javabyte.opcode.MethodOpcode;
import javabyte.signature.MethodSignature;
import javabyte.signature.Signatures;
import javabyte.type.ExactTypeName;
import javabyte.type.TypeName;
import javabyte.type.TypeParameter;
import javabyte.type.Types;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author whilein
 */
@UtilityClass
public class Javabyte {

    private MakeClass _make(
            final Version version,
            final ExactTypeName name
    ) {
        return new MakeClassImpl(
                version, name,
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                Types.OBJECT
        );
    }

    public @NotNull MakeClass make(
            final @NonNull Version version,
            final @NonNull String name
    ) {
        return _make(version, Types.of(name));
    }

    public @NotNull MakeClass make(
            final @NonNull String name
    ) {
        return _make(Version.V1_8, Types.of(name));
    }

    public @NotNull MakeClass make(
            final @NonNull Version version,
            final @NonNull ExactTypeName name
    ) {
        return _make(version, name);
    }

    public @NotNull MakeClass make(
            final @NonNull ExactTypeName name
    ) {
        return _make(Version.V1_8, name);
    }

    @FieldDefaults(level = AccessLevel.PROTECTED)
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class AbstractMakeElement implements MakeElement {
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
        public void setPublic() {
            setAccess(Access.PUBLIC);
            setFinal(false);
        }

        @Override
        public void setPublicFinal() {
            setAccess(Access.PUBLIC);
            setFinal(true);
        }

        @Override
        public void setPrivate() {
            setAccess(Access.PRIVATE);
            setFinal(false);
        }

        @Override
        public void setPrivateFinal() {
            setAccess(Access.PRIVATE);
            setFinal(true);
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

    @FieldDefaults(level = AccessLevel.PROTECTED)
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class AbstractMakeClass extends AbstractMakeElement implements MakeClass {

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
        final ExactTypeName name;

        @Getter
        final List<TypeName> interfaces;

        final List<MakeFieldImpl> fields;

        final List<MakeExecutableImpl> executables;

        @Getter
        final List<MakeInnerClassImpl> innerClasses;

        @Getter
        @NotNull
        TypeName superName;

        MakeConstructorImpl staticConstructor;

        protected final ClassWriter makeClassWriter() {
            val cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

            for (val innerClass : innerClasses) {
                cw.visitInnerClass(innerClass.getName().getInternalName(), name.getInternalName(),
                        innerClass.getInnerName(), innerClass.getModifiers());

                val innerWriter = innerClass.makeClassWriter();
                innerWriter.visitOuterClass(name.getInternalName(), null, null);
            }

            final String[] interfaceNames;

            if (!interfaces.isEmpty()) {
                interfaceNames = interfaces.stream()
                        .map(TypeName::getInternalName)
                        .toArray(String[]::new);
            } else {
                interfaceNames = null;
            }

            final String classSignature;

            if (superName.hasParameterizedTypes() || Types.hasParameterizedTypes(interfaces)) {
                classSignature = Signatures.classSignature(
                        new TypeParameter[0], superName,
                        interfaces.toArray(new TypeName[0])
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

                emptyConstructorCode.methodInsn(MethodOpcode.SPECIAL, "<init>")
                        .inSuper()
                        .descriptor(void.class);

                emptyConstructorCode.callReturn();

                addExecutable(cw, emptyConstructor);
            }

            return cw;
        }

        private void addExecutable(final ClassWriter writer, final MakeExecutableImpl executable) {
            val exceptions = executable.getExceptions().stream()
                    .map(ExactTypeName::getName)
                    .toArray(String[]::new);

            val methodSignature = Signatures.methodSignature(executable.getReturnType(),
                    executable.getParameters().toArray(new TypeName[0]));

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
                    val oldParameters = executable.parameters;
                    val newParameters = overrides.parameterTypes;

                    if (oldParameters.size() != newParameters.length) {
                        throw new IllegalStateException("Cannot override method "
                                + overrides.type.getName() + "#" + method.getName()
                                + " because the method has " + newParameters.length + " parameters"
                                + ", but implementation has " + oldParameters.size());
                    }

                    val bridge = _initMethod(executable.getName());
                    bridge.setReturnType(overrides.returnType);
                    bridge.setParameters(newParameters);

                    bridge.setModifiers(executable.getAccess().getOpcode() | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC);

                    val code = bridge.getBytecode();
                    code.loadLocal(0);

                    for (int i = 0; i < newParameters.length; i++) {
                        code.loadLocal(i + 1);
                        code.callCast(oldParameters.get(i));
                    }

                    code.methodInsn(MethodOpcode.VIRTUAL, executable.getName())
                            .in(name).descriptor(executable.getSignature());

                    code.callReturn();

                    addExecutable(writer, bridge);
                }
            }

            executable.getBytecode().compile(executable, mv);

            mv.visitEnd();
        }

        private MakeConstructorImpl _initConstructor(final boolean isStatic) {
            return new MakeConstructorImpl(Bytecode.bytecode(), this,
                    isStatic ? "<clinit>" : "<init>", Types.VOID,
                    new ArrayList<>(), new ArrayList<>(), isStatic);
        }

        private MakeMethodImpl _initMethod(final String name) {
            return new MakeMethodImpl(Bytecode.bytecode(), this,
                    name, Types.VOID, new ArrayList<>(), new ArrayList<>());
        }

        private <T extends MakeExecutableImpl> T _addExecutable(final T executable) {
            executables.add(executable);
            return executable;
        }

        @Override
        public @NotNull MakeHashCodeAndEquals addHashCodeAndEquals() {
            val hashCodeMethod = _addExecutable(_initMethod("hashCode"));
            hashCodeMethod.setReturnType(int.class);

            val equalsMethod = _addExecutable(_initMethod("equals"));
            equalsMethod.addParameter(Object.class);
            equalsMethod.setReturnType(boolean.class);

            val hashCode = new MakeHashCodeAndEqualsImpl(this, hashCodeMethod, equalsMethod);
            hashCode.initMethods();

            return hashCode;
        }


        @Override
        public @NotNull MakeToString addToString() {
            val method = _addExecutable(_initMethod("toString"));
            method.setReturnType(String.class);

            val toString = new MakeToStringImpl(this, method);
            toString.initMethod();

            return toString;
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
        public @NotNull MakeMethod addMethod(final @NonNull String name) {
            return _addExecutable(_initMethod(name));
        }

        @Override
        public @NotNull MakeInnerClass addInner(final @NonNull String name) {
            val inner = new MakeInnerClassImpl(
                    version, Types.of(this.name.getName() + "$" + name), name,
                    new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>(), new ArrayList<>(),
                    this, Types.OBJECT
            );
            innerClasses.add(inner);
            return inner;
        }

        private MakeField _addField(final String name, final TypeName type) {
            val field = new MakeFieldImpl(this, name, type);
            fields.add(field);

            return field;
        }

        @Override
        public @NotNull MakeField addField(final @NonNull String name, final @NonNull TypeName type) {
            return _addField(name, type);
        }

        @Override
        public @NotNull MakeField addField(final @NonNull String name, final @NonNull Type type) {
            return _addField(name, Types.of(type));
        }

        @Override
        public @Unmodifiable @NotNull List<@NotNull MakeMethod> getMethods() {
            return Collections.unmodifiableList(executables.stream()
                    .filter(method -> method instanceof MakeMethod)
                    .map(method -> (MakeMethod) method)
                    .collect(Collectors.toList()));
        }

        @Override
        public @Unmodifiable @NotNull List<@NotNull MakeConstructor> getConstructors() {
            return Collections.unmodifiableList(executables.stream()
                    .filter(method -> method instanceof MakeConstructor)
                    .map(method -> (MakeConstructor) method)
                    .collect(Collectors.toList()));
        }

        @Override
        public @Unmodifiable @NotNull List<@NotNull MakeField> getFields() {
            return Collections.unmodifiableList(new ArrayList<>(fields));
        }

        @Override
        public void setSuperName(final @NonNull Type type) {
            this.superName = Types.of(type);
        }

        @Override
        public void setSuperName(final @NonNull TypeName name) {
            this.superName = name;
        }

        @Override
        public void addInterface(final @NonNull TypeName name) {
            this.interfaces.add(name);
        }

        @Override
        public void addInterface(final @NonNull Type type) {
            this.interfaces.add(Types.of(type));
        }

        @Override
        public void setInterfaces(final @NotNull TypeName @NotNull ... interfaces) {
            this.interfaces.clear();
            this.interfaces.addAll(Arrays.asList(interfaces));
        }

        @Override
        public void setInterfaces(final @NonNull Collection<? extends @NotNull TypeName> interfaces) {
            this.interfaces.clear();
            this.interfaces.addAll(interfaces);
        }

        @Override
        public void setInterfaceTypes(final @NotNull Type @NotNull ... interfaces) {
            _setInterfaces(Arrays.stream(interfaces).map(Types::of).collect(Collectors.toList()));
        }

        @Override
        public void setInterfaceTypes(final @NonNull Collection<? extends @NotNull Type> interfaces) {
            _setInterfaces(interfaces.stream().map(Types::of).collect(Collectors.toList()));
        }

        @Override
        public void writeClass(final @NonNull OutputStream os) throws IOException {
            os.write(writeAsBytes());
        }

        @Override
        public void writeTo(final @NonNull File directory) throws IOException {
            val thatClass = new File(directory, name.getSimpleName() + ".class");

            try (val output = new FileOutputStream(thatClass)) {
                writeClass(output);
            }

            for (val inner : innerClasses) {
                inner.writeTo(directory);
            }
        }

        @Override
        public void writeTo(final @NonNull Path directory) throws IOException {
            val thatClass = directory.resolve(name.getSimpleName() + ".class");

            try (val output = Files.newOutputStream(thatClass)) {
                writeClass(output);
            }

            for (val inner : innerClasses) {
                inner.writeTo(directory);
            }
        }

        @Override
        public byte @NotNull [] writeAsBytes() {
            return makeClassWriter().toByteArray();
        }

        @Override
        public @NotNull Class<?> load(final @NonNull ClassLoader loader) {
            for (val innerClass : innerClasses) {
                innerClass.load(loader);
            }

            val bytes = writeAsBytes();

            try {
                return (Class<?>) INVOKE_DEFINE_CLASS.invoke(loader, name.getName(),
                        bytes, 0, bytes.length, null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        private void _setInterfaces(final Collection<TypeName> interfaces) {
            this.interfaces.clear();
            this.interfaces.addAll(interfaces);
        }

    }


    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MakeHashCodeAndEqualsImpl implements MakeHashCodeAndEquals {
        final AbstractMakeClass type;

        final MakeMethod hashCode;
        final MakeMethod equals;

        String[] fieldNames;
        boolean includeSuper;

        private List<MakeField> getFields() {
            val fields = new ArrayList<MakeField>();

            if (fieldNames == null) {
                fields.addAll(type.fields);
            } else {
                for (val fieldName : fieldNames) {
                    for (val field : type.fields) {
                        if (field.getName().equals(fieldName)) {
                            fields.add(field);
                        }
                    }
                }
            }

            return fields;
        }

        private void initMethods() {
            initHashCode();
            initEquals();
        }

        private void initEquals() {
            val code = equals.getBytecode();

            val afterEqualTest = Bytecode.position();
            val afterInstanceTest = Bytecode.position();

            code.loadLocal(1);
            code.loadLocal(0);
            code.jump(JumpOpcode.IF_ACMPNE, afterEqualTest);
            code.pushInt(1);
            code.callReturn();

            code.visit(afterEqualTest);

            code.loadLocal(1);
            code.callInstanceOf(type.name);

            code.jump(JumpOpcode.IFNE, afterInstanceTest);

            code.pushInt(0);
            code.callReturn();

            code.visit(afterInstanceTest);

            code.whenCompile(() -> {
                val fields = getFields();

                if (!fields.isEmpty() || includeSuper) {
                    val afterComparisons = Bytecode.position();

                    code.loadLocal(1);
                    code.callCast(type.name);

                    val that = code.storeLocal();

                    if (includeSuper && !type.superName.equals(Types.OBJECT)) {
                        code.loadLocal(0);
                        code.loadLocal(that);
                        code.methodInsn(MethodOpcode.SPECIAL, "equals")
                                .descriptor(equals.getSignature())
                                .inSuper();

                        if (!fields.isEmpty())
                            code.jump(JumpOpcode.IFEQ, afterComparisons);
                    }

                    for (val field : fields) {
                        val fieldType = field.getType();

                        code.loadLocal(0);
                        code.fieldInsn(FieldOpcode.GET, field.getName()).inCurrent().descriptor(fieldType);
                        code.loadLocal(that);
                        code.fieldInsn(FieldOpcode.GET, field.getName()).inCurrent().descriptor(fieldType);

                        if (fieldType.isPrimitive()) {
                            switch (fieldType.getPrimitive()) {
                                case Types.BOOL_TYPE:
                                case Types.BYTE_TYPE:
                                case Types.CHAR_TYPE:
                                case Types.SHORT_TYPE:
                                case Types.INT_TYPE:
                                    code.jump(JumpOpcode.IF_ICMPNE, afterComparisons);
                                    break;
                                case Types.LONG_TYPE:
                                    code.callCompare(CompareOpcode.LCMP);
                                    code.jump(JumpOpcode.IFNE, afterComparisons);
                                    break;
                                case Types.FLOAT_TYPE:
                                    code.callCompare(CompareOpcode.FCMPL);
                                    code.jump(JumpOpcode.IFNE, afterComparisons);
                                    break;
                                case Types.DOUBLE_TYPE:
                                    code.callCompare(CompareOpcode.DCMPL);
                                    code.jump(JumpOpcode.IFNE, afterComparisons);
                                    break;
                            }
                        } else {
                            if (fieldType.isArray()) {
                                val component = fieldType.getComponent();

                                val equalSubject = !component.isPrimitive()
                                        ? Types.of(Object[].class)
                                        : fieldType;

                                val signature = Signatures.methodSignature(
                                        Types.BOOL,
                                        equalSubject,
                                        equalSubject
                                );

                                val methodName = component.isArray() ? "deepEquals" : "equals";

                                code.methodInsn(MethodOpcode.STATIC, methodName)
                                        .descriptor(signature)
                                        .in(Arrays.class);
                            } else {
                                code.methodInsn(MethodOpcode.STATIC, "equals")
                                        .descriptor(Types.BOOL, Types.OBJECT, Types.OBJECT)
                                        .in(Objects.class);
                            }

                            code.jump(JumpOpcode.IFEQ, afterComparisons);
                        }
                    }

                    if (!fields.isEmpty())
                        code.pushInt(1);

                    code.callReturn();

                    code.visit(afterComparisons);
                }

                code.pushInt(fields.isEmpty() ? 1 : 0);
                code.callReturn();
            });
        }

        private void initHashCode() {
            val code = hashCode.getBytecode();

            code.whenCompile(() -> {
                val fields = getFields();

                if (fields.isEmpty() && !includeSuper) {
                    code.pushInt(0);
                } else {
                    val includeSuper = this.includeSuper && !type.superName.equals(Types.OBJECT);

                    for (int i = 0, j = fields.size() + (includeSuper ? 1 : 0); i < j; i++) {
                        code.pushInt(31);
                    }

                    boolean shouldMultiply = false;

                    if (includeSuper) {
                        code.loadLocal(0);
                        code.methodInsn(MethodOpcode.SPECIAL, "hashCode").inSuper().descriptor(int.class);
                        code.callMath(MathOpcode.IADD);
                        shouldMultiply = true;
                    }

                    for (val field : fields) {
                        if (shouldMultiply) {
                            code.callMath(MathOpcode.IMUL);
                        }

                        val fieldType = field.getType();

                        code.loadLocal(0);
                        code.fieldInsn(FieldOpcode.GET, field.getName()).inCurrent().descriptor(fieldType);

                        if (fieldType.isArray()) {
                            val component = fieldType.getComponent();

                            val signature = Signatures.methodSignature(
                                    Types.INT,
                                    !component.isPrimitive()
                                            ? Types.of(Object[].class)
                                            : fieldType
                            );

                            val methodName = component.isArray() ? "deepHashCode" : "hashCode";

                            code.methodInsn(MethodOpcode.STATIC, methodName)
                                    .descriptor(signature)
                                    .in(Arrays.class);
                        } else if (fieldType.isPrimitive()) {
                            switch (fieldType.getPrimitive()) {
                                case Types.BOOL_TYPE:
                                case Types.FLOAT_TYPE:
                                case Types.DOUBLE_TYPE:
                                case Types.LONG_TYPE:
                                    code.methodInsn(MethodOpcode.STATIC, "hashCode")
                                            .descriptor(Signatures.methodSignature(Types.INT, fieldType))
                                            .in(Types.getWrapper(fieldType));
                                    break;
                            }
                        } else {
                            code.methodInsn(MethodOpcode.STATIC, "hashCode")
                                    .descriptor(Signatures.methodSignature(Types.INT, fieldType))
                                    .in(Objects.class);
                        }

                        code.callMath(MathOpcode.IADD);

                        shouldMultiply = true;
                    }
                }
            });

            code.callReturn();
        }

        @Override
        public void includeSuper() {
            this.includeSuper = true;
        }

        @Override
        public void setFields(final @NotNull String @NonNull ... fieldNames) {
            this.fieldNames = fieldNames;
        }
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MakeToStringImpl implements MakeToString {
        final AbstractMakeClass type;
        final MakeMethod handle;

        String label;
        String[] fieldNames;

        boolean includeSuper;

        private void append(final InstructionSet code, final TypeName type) {
            val sb = Types.STRING_BUILDER;

            final TypeName argument;

            if (type.isArray()) {
                argument = Types.STRING;

                val component = type.getComponent();

                val signature = Signatures.methodSignature(
                        argument,
                        !component.isPrimitive()
                                ? Types.of(Object[].class)
                                : type
                );

                val methodName = component.isArray() ? "deepToString" : "toString";

                code.methodInsn(MethodOpcode.STATIC, methodName).in(Arrays.class).descriptor(signature);
            } else if (type.isPrimitive() || type.equals(Types.STRING)) {
                argument = type.equals(Types.BYTE) || type.equals(Types.SHORT)
                        ? Types.INT
                        : type;
            } else {
                argument = Types.OBJECT;
            }

            code.methodInsn(MethodOpcode.VIRTUAL, "append")
                    .descriptor(Signatures.methodSignature(sb, argument))
                    .in(StringBuilder.class);
        }

        private List<MakeField> getFields() {
            val fields = new ArrayList<MakeField>();

            if (fieldNames == null) {
                fields.addAll(type.fields);
            } else {
                for (val fieldName : fieldNames) {
                    for (val field : type.fields) {
                        if (field.getName().equals(fieldName)) {
                            fields.add(field);
                        }
                    }
                }
            }

            return fields;
        }

        private void initMethod() {
            val code = handle.getBytecode();
            code.callInit(StringBuilder.class);

            code.whenCompile(() -> {
                val fields = getFields();

                val label = this.label == null
                        ? this.type.name.getSimpleName()
                        : this.label;

                if (fields.isEmpty() && !includeSuper) {
                    code.pushString(label);
                } else {
                    boolean separator = false;
                    boolean prevString = false;

                    for (val field : fields) {
                        val fieldType = field.getType();

                        val isString = Types.STRING.equals(fieldType);

                        val fieldLabel = new StringBuilder();
                        if (prevString) fieldLabel.append('\'');
                        if (separator) fieldLabel.append(", ");
                        else fieldLabel.append(label).append("[");
                        fieldLabel.append(field.getName());
                        fieldLabel.append('=');
                        if (isString) fieldLabel.append('\'');
                        code.pushString(fieldLabel.toString());

                        append(code, Types.STRING);

                        code.loadLocal(0);
                        code.fieldInsn(FieldOpcode.GET, field.getName()).inCurrent().descriptor(fieldType);

                        append(code, fieldType);

                        prevString = isString;
                        separator = true;
                    }

                    if (!type.superName.equals(Types.OBJECT) && includeSuper) {
                        val fieldLabel = new StringBuilder();
                        if (prevString) fieldLabel.append('\'');
                        if (separator) fieldLabel.append(", ");
                        fieldLabel.append("@super=");
                        code.pushString(fieldLabel.toString());


                        append(code, Types.STRING);

                        code.loadLocal(0);
                        code.methodInsn(MethodOpcode.SPECIAL, "toString").inSuper().descriptor(String.class);

                        append(code, Types.STRING);
                        prevString = false;
                    }

                    code.pushString(prevString ? "']" : "]");
                }

                append(code, Types.STRING);
            });

            code.methodInsn(MethodOpcode.VIRTUAL, "toString")
                    .in(StringBuilder.class)
                    .descriptor(String.class);
            code.callReturn();
        }

        @Override
        public void includeSuper() {
            this.includeSuper = true;
        }

        @Override
        public void setLabel(final @NonNull String label) {
            this.label = label;
        }

        @Override
        public void setFields(final @NotNull String @NonNull ... fieldNames) {
            this.fieldNames = fieldNames;
        }
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class MakeInnerClassImpl extends AbstractMakeClass implements MakeInnerClass {

        String innerName;
        MakeClass declaringClass;

        private MakeInnerClassImpl(
                final Version version,
                final ExactTypeName name,
                final String innerName,
                final List<TypeName> interfaces,
                final List<MakeFieldImpl> fields,
                final List<MakeExecutableImpl> executables,
                final List<MakeInnerClassImpl> innerClasses,
                final MakeClass declaringClass,
                final TypeName superName
        ) {
            super(version, name, interfaces, fields, executables, innerClasses, superName);

            this.innerName = innerName;
            this.declaringClass = declaringClass;
        }

    }

    private static final class MakeClassImpl extends AbstractMakeClass implements MakeClass {

        private MakeClassImpl(
                final Version version,
                final ExactTypeName name,
                final List<TypeName> interfaces,
                final List<MakeFieldImpl> fields,
                final List<MakeExecutableImpl> executables,
                final List<MakeInnerClassImpl> innerClasses,
                final TypeName superName
        ) {
            super(version, name, interfaces, fields, executables, innerClasses, superName);
        }

    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MakeFieldImpl extends AbstractMakeElement implements MakeField {
        MakeClass declaringClass;
        String name;
        TypeName type;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class MakeConstructorImpl extends MakeExecutableImpl implements MakeConstructor {

        boolean isStatic;

        private MakeConstructorImpl(
                final ExecutableInstructionSet bytecode,
                final MakeClass declaringClass,
                final String name,
                final TypeName returnType,
                final List<TypeName> parameters,
                final List<ExactTypeName> exceptions,
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
                final ExecutableInstructionSet bytecode,
                final MakeClass declaringClass,
                final String name,
                final TypeName returnType,
                final List<TypeName> parameters,
                final List<ExactTypeName> exceptions
        ) {
            super(bytecode, declaringClass, name, returnType, parameters, exceptions);
        }

        @Override
        public void setSignature(@NotNull final MethodSignature signature) {
            this.returnType = signature.getReturnType();

            _setParameters(Arrays.asList(signature.getParameterTypes()));
        }

        @Override
        public void setReturnType(final @NonNull TypeName type) {
            this.returnType = type;
        }

        @Override
        public void setReturnType(final @NonNull Type type) {
            this.returnType = Types.of(type);
        }

        private Method getSameMethod(final Class<?> type) {
            val methods = Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> method.getName().equals(name))
                    .collect(Collectors.toList());

            if (methods.isEmpty()) {
                throw new IllegalStateException("Cannot override method "
                        + type.getName() + "#" + name
                        + ": No method found");
            }

            if (methods.size() > 1) {
                throw new IllegalStateException("Cannot override method "
                        + type.getName() + "#" + name
                        + ": The class has multiple methods with name " + name);
            }

            return methods.get(0);
        }

        @Override
        public void copySignatureFrom(final @NonNull Class<?> type) {
            val method = getSameMethod(type);
            setReturnType(method.getReturnType());
            setParameterTypes(method.getParameterTypes());
        }

        @Override
        public void copySignatureFrom(final @NonNull Method method) {
            setReturnType(method.getReturnType());
            setParameterTypes(method.getParameterTypes());
        }

        @Override
        public void setOverrides(
                final @NonNull Class<?> type
        ) {
            val method = getSameMethod(type);
            _setOverrides(type, method.getReturnType(), method.getParameterTypes());
        }

        public void _setOverrides(
                final Class<?> type,
                final TypeName returnType,
                final TypeName... parameterTypes
        ) {
            this.overrides = new Overrides(type, returnType, parameterTypes);
        }

        private void _setOverrides(
                final Class<?> type,
                final Class<?> returnType,
                final Class<?>... parameterTypes
        ) {
            _setOverrides(type, Types.of(returnType), Types.of(parameterTypes));
        }

        @Override
        public void setOverrides(
                final @NonNull Class<?> type,
                final @NonNull Class<?> returnType,
                final @NotNull Class<?> @NotNull ... parameterTypes
        ) {
            _setOverrides(type, returnType, parameterTypes);
        }

        @Override
        public void setOverrides(
                final @NonNull Class<?> type,
                final @NonNull TypeName returnType,
                final @NotNull TypeName @NotNull ... parameterTypes
        ) {
            _setOverrides(type, returnType, parameterTypes);
        }

        private boolean shouldMakeBridge() {
            if (isStatic() || overrides == null) return false;

            return !returnType.equals(overrides.returnType) ||
                    !parameters.equals(Arrays.asList(overrides.parameterTypes));
        }


    }

    @FieldDefaults(level = AccessLevel.PROTECTED)
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class MakeExecutableImpl extends AbstractMakeElement implements MakeExecutable {

        @Getter
        final ExecutableInstructionSet bytecode;

        @Getter
        final MakeClass declaringClass;

        @Getter
        final String name;

        @Getter
        @NotNull
        TypeName returnType;

        @Getter
        final List<TypeName> parameters;

        @Getter
        final List<ExactTypeName> exceptions;

        Overrides overrides;

        @Override
        public int getParameterCount() {
            return parameters.size();
        }

        @Override
        public final @NotNull MethodSignature getSignature() {
            return Signatures.methodSignature(returnType, parameters.toArray(new TypeName[0]));
        }

        @Override
        public final void addException(final @NonNull Class<?> type) {
            exceptions.add(Types.of(type));
        }

        @Override
        public final void addException(final @NonNull ExactTypeName name) {
            exceptions.add(name);
        }

        @Override
        public final void setExceptionTypes(final @NonNull Collection<? extends @NotNull Class<?>> types) {
            _setExceptions(types.stream().map(Types::of).collect(Collectors.toList()));
        }

        @Override
        public final void setExceptionTypes(final @NotNull Class<?> @NotNull ... types) {
            _setExceptions(Arrays.stream(types).map(Types::of).collect(Collectors.toList()));
        }

        @Override
        public final void setExceptions(final @NonNull Collection<? extends @NotNull ExactTypeName> names) {
            _setExceptions(names);
        }

        @Override
        public final void setExceptions(final @NotNull ExactTypeName @NotNull ... names) {
            _setExceptions(Arrays.asList(names));
        }

        @Override
        public final void addParameter(final @NonNull TypeName type) {
            parameters.add(type);
        }

        @Override
        public final void addParameter(final int i, final @NonNull TypeName parameter) {
            parameters.add(i, parameter);
        }

        @Override
        public final void addParameter(final @NonNull Type type) {
            parameters.add(Types.of(type));
        }

        @Override
        public final void addParameter(final int i, final @NonNull Type type) {
            parameters.add(i, Types.of(type));
        }

        @Override
        public final void addParameter(final @NonNull String name) {
            parameters.add(Types.of(name));
        }

        @Override
        public final void addParameter(final int i, final @NonNull String name) {
            parameters.add(i, Types.of(name));
        }

        @Override
        public final void setParameterTypes(final @NonNull Collection<? extends @NotNull Type> types) {
            this._setParameters(types.stream().map(Types::of).collect(Collectors.toList()));
        }

        @Override
        public final void setParameterTypes(final @NotNull Type @NonNull ... types) {
            this._setParameters(Arrays.stream(types).map(Types::of).collect(Collectors.toList()));
        }

        @Override
        public final void setParameters(final @NonNull Collection<? extends @NotNull TypeName> parameters) {
            _setParameters(parameters);
        }

        @Override
        public final void setParameters(final @NotNull TypeName @NotNull ... parameters) {
            _setParameters(Arrays.asList(parameters));
        }

        protected void _setParameters(final Collection<? extends TypeName> parameters) {
            this.parameters.clear();
            this.parameters.addAll(parameters);
        }

        protected void _setExceptions(final Collection<? extends ExactTypeName> exceptions) {
            this.exceptions.clear();
            this.exceptions.addAll(exceptions);
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Overrides {
        Class<?> type;
        TypeName returnType;
        TypeName[] parameterTypes;
    }

}
