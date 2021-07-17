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

package javabyte.bytecode;

import javabyte.make.MakeExecutable;
import javabyte.name.Name;
import javabyte.name.Names;
import javabyte.opcode.FieldOpcode;
import javabyte.opcode.MathOpcode;
import javabyte.opcode.MethodOpcode;
import javabyte.signature.MethodSignature;
import javabyte.signature.Signatures;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author whilein
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class AsmBytecode implements Bytecode {

    public static @NotNull Bytecode create() {
        return new AsmBytecode(new LinkedList<>());
    }

    final List<Consumer<Compile>> instructions;

    private void insn(final Consumer<Compile> instruction) {
        instructions.add(instruction);
    }

    @Override
    public void loadLocal(final int index) {
        insn(compile -> {
            val local = compile.locals.get(index);
            val localName = local.name;
            val localType = localName.getType();

            compile.mv.visitVarInsn(localType.getOpcode(ILOAD), local.offset);
            compile.pushStack(localName);
        });
    }

    @Override
    public @NotNull FieldInsn fieldInsn(final @NonNull FieldOpcode opcode, final @NonNull String name) {
        val field = new FieldInsnImpl(name, opcode);
        insn(field);

        return field;
    }

    @Override
    public @NotNull MethodInsn methodInsn(final @NonNull MethodOpcode opcode, final @NonNull String name) {
        val method = new MethodInsnImpl(name, opcode);
        insn(method);

        return method;
    }

    @Override
    public void loadString(final @NotNull String value) {
        insn(compile -> {
            compile.mv.visitLdcInsn(value);
            compile.pushStack(Names.STRING);
        });
    }

    @Override
    public void loadInt(final int value) {
        insn(compile -> {
            if (value >= 0 && value <= 5) {
                compile.mv.visitInsn(ICONST_0 + value);
            } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                compile.mv.visitIntInsn(BIPUSH, value);
            } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                compile.mv.visitIntInsn(SIPUSH, value);
            } else {
                compile.mv.visitLdcInsn(value);
            }

            compile.pushStack(Names.INT);
        });
    }

    @Override
    public void loadFloat(final float value) {
        insn(compile -> {
            int intValue;

            if (value >= 0 && value <= 2 && (intValue = (int) value) == value) {
                compile.mv.visitInsn(FCONST_0 + intValue);
            } else {
                compile.mv.visitLdcInsn(value);
            }

            compile.pushStack(Names.FLOAT);
        });
    }

    @Override
    public void loadDouble(final double value) {
        insn(compile -> {
            if (value == 0) {
                compile.mv.visitInsn(DCONST_0);
            } else if (value == 1) {
                compile.mv.visitInsn(DCONST_1);
            } else {
                compile.mv.visitLdcInsn(value);
            }

            compile.pushStack(Names.DOUBLE);
        });
    }

    @Override
    public void loadLong(final long value) {
        insn(compile -> {
            if (value == 0) {
                compile.mv.visitInsn(LCONST_0);
            } else if (value == 1) {
                compile.mv.visitInsn(LCONST_1);
            } else {
                compile.mv.visitLdcInsn(value);
            }

            compile.pushStack(Names.LONG);
        });
    }

    @Override
    public void loadNull() {
        insn(compile -> {
            compile.mv.visitInsn(ACONST_NULL);
            compile.pushStack(Names.OBJECT);
        });
    }

    @Override
    public void callMath(final @NonNull MathOpcode opcode) {
        insn(compile -> {
            final Name type;

            switch (opcode) {
                default:
                case IADD: case ISUB: case IMUL: case IDIV: case IREM: case INEG:
                    type = Names.INT;
                    break;
                case LADD: case LSUB: case LMUL: case LDIV: case LREM: case LNEG:
                    type = Names.LONG;
                    break;
                case FADD: case FSUB: case FMUL: case FDIV: case FREM: case FNEG:
                    type = Names.FLOAT;
                    break;
                case DADD: case DSUB: case DMUL: case DDIV: case DREM: case DNEG:
                    type = Names.DOUBLE;
                    break;
            }

            compile.requireStrictStack(type, type);
            compile.popStack();
            compile.popStack();
            compile.mv.visitInsn(opcode.getOpcode());
            compile.pushStack(type);
        });
    }

    @Override
    public void callBox() {
        insn(compile -> {
            val stack = compile.popStack();

            if (!stack.isPrimitive()) {
                throw new IllegalStateException("Stack item should be a primitive!");
            }

            val wrapper = Names.getWrapper(stack);

            compile.mv.visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf",
                    "(" + stack.getDescriptor() + ")" + wrapper.getDescriptor(),
                    false);

            compile.pushStack(wrapper);
        });
    }

    private void _callCast(final Name to) {
        insn(compile -> {
            val mv = compile.mv;
            val stack = compile.popStack();

            if (stack.equals(to)) {
                compile.pushStack(stack);
                return;
            }

            if (stack.isPrimitive() && to.isPrimitive()
                    && stack.getPrimitive() != Names.BOOL_TYPE
                    && to.getPrimitive() != Names.BOOL_TYPE) {
                switch (stack.getPrimitive()) {
                    case Names.BYTE_TYPE:
                    case Names.SHORT_TYPE:
                    case Names.CHAR_TYPE:
                    case Names.INT_TYPE:
                        switch (to.getPrimitive()) {
                            case Names.FLOAT_TYPE:
                                mv.visitInsn(I2F);
                                break;
                            case Names.DOUBLE_TYPE:
                                mv.visitInsn(I2D);
                                break;
                            case Names.LONG_TYPE:
                                mv.visitInsn(I2L);
                                break;
                        }
                    case Names.FLOAT_TYPE:
                        switch (to.getPrimitive()) {
                            case Names.BYTE_TYPE:
                                mv.visitInsn(F2I);
                                mv.visitInsn(I2B);
                                break;
                            case Names.CHAR_TYPE:
                                mv.visitInsn(F2I);
                                mv.visitInsn(I2C);
                                break;
                            case Names.SHORT_TYPE:
                                mv.visitInsn(F2I);
                                mv.visitInsn(I2S);
                                break;
                            case Names.INT_TYPE:
                                mv.visitInsn(F2I);
                                break;
                            case Names.DOUBLE_TYPE:
                                mv.visitInsn(F2D);
                                break;
                            case Names.LONG_TYPE:
                                mv.visitInsn(F2L);
                                break;
                        }
                    case Names.DOUBLE_TYPE:
                        switch (to.getPrimitive()) {
                            case Names.BYTE_TYPE:
                                mv.visitInsn(D2I);
                                mv.visitInsn(I2B);
                                break;
                            case Names.CHAR_TYPE:
                                mv.visitInsn(D2I);
                                mv.visitInsn(I2C);
                                break;
                            case Names.SHORT_TYPE:
                                mv.visitInsn(D2I);
                                mv.visitInsn(I2S);
                                break;
                            case Names.INT_TYPE:
                                mv.visitInsn(D2I);
                                break;
                            case Names.FLOAT_TYPE:
                                mv.visitInsn(D2F);
                                break;
                            case Names.LONG_TYPE:
                                mv.visitInsn(D2L);
                                break;
                        }
                    case Names.LONG_TYPE:
                        switch (to.getPrimitive()) {
                            case Names.BYTE_TYPE:
                                mv.visitInsn(L2I);
                                mv.visitInsn(I2B);
                                break;
                            case Names.CHAR_TYPE:
                                mv.visitInsn(L2I);
                                mv.visitInsn(I2C);
                                break;
                            case Names.SHORT_TYPE:
                                mv.visitInsn(L2I);
                                mv.visitInsn(I2S);
                                break;
                            case Names.INT_TYPE:
                                mv.visitInsn(L2I);
                                break;
                            case Names.FLOAT_TYPE:
                                mv.visitInsn(L2F);
                                break;
                            case Names.DOUBLE_TYPE:
                                mv.visitInsn(L2D);
                                break;
                        }
                }
            } else if (!stack.isPrimitive() && !to.isPrimitive()) {
                compile.mv.visitTypeInsn(CHECKCAST, to.getInternalName());
            } else {
                throw new IllegalStateException("Cannot cast " + stack + " to " + to);
            }

            compile.pushStack(to);
        });
    }

    @Override
    public void callCast(@NotNull final Type to) {
        _callCast(Names.of(to));
    }

    @Override
    public void callCast(@NotNull final Name to) {
        _callCast(to);
    }

    @Override
    public void callUnbox() {
        insn(compile -> {
            val stack = compile.popStack();

            if (stack.isPrimitive()) {
                throw new IllegalStateException("Stack item should not be a primitive!");
            }

            final String methodName;

            val primitive = Names.getPrimitive(stack);

            switch (primitive.getPrimitive()) {
                case Names.BOOL_TYPE:
                    methodName = "booleanValue";
                    break;
                case Names.BYTE_TYPE:
                    methodName = "byteValue";
                    break;
                case Names.CHAR_TYPE:
                    methodName = "charValue";
                    break;
                case Names.SHORT_TYPE:
                    methodName = "shortValue";
                    break;
                case Names.INT_TYPE:
                    methodName = "intValue";
                    break;
                case Names.LONG_TYPE:
                    methodName = "longValue";
                    break;
                case Names.FLOAT_TYPE:
                    methodName = "floatValue";
                    break;
                case Names.DOUBLE_TYPE:
                    methodName = "doubleValue";
                    break;
                default:
                    throw new IllegalStateException("Unsupported type: " + stack);
            }

            compile.mv.visitMethodInsn(INVOKEVIRTUAL, stack.getInternalName(), methodName,
                    "()" + primitive.getDescriptor(),
                    false);

            compile.pushStack(primitive);
        });
    }

    @Override
    public void callReturn() {
        insn(compile -> {
            if (compile.executable.getReturnType().equals(Names.VOID)) {
                compile.mv.visitInsn(RETURN);
            } else {
                val stack = compile.popStack();
                compile.mv.visitInsn(stack.getType().getOpcode(IRETURN));
            }
        });
    }

    @Override
    public void compile(
            final @NonNull MakeExecutable executable,
            final @NonNull MethodVisitor visitor
    ) {
        val locals = new ArrayList<Local>();

        int localSize = 0;

        if (!executable.isStatic()) {
            locals.add(new Local(executable.getDeclaringClass().getName(), localSize));
            localSize++;
        }

        for (val parameter : executable.getParameters()) {
            locals.add(new Local(parameter, localSize));
            localSize += parameter.getSize();
        }

        val compile = new Compile(0, localSize, 0, localSize,
                executable, visitor, new LinkedList<>(), locals);

        for (val instruction : instructions) {
            instruction.accept(compile);
        }

        visitor.visitMaxs(compile.maxStackSize, compile.maxLocalSize);
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class FieldInsnImpl implements FieldInsn, Consumer<Compile> {
        final String name;
        final FieldOpcode opcode;

        Function<Compile, Name> owner;
        Name descriptor;

        @Override
        public void accept(final @NonNull Compile compile) {
            if (owner == null) {
                throw new IllegalStateException("You should to specify owner using FieldInsn#in method!");
            }

            if (descriptor == null) {
                throw new IllegalStateException("You should to specify descriptor using FieldInsn#descriptor method!");
            }

            if (opcode == FieldOpcode.PUT || opcode == FieldOpcode.PUT_STATIC) {
                compile.popStack();
            }

            compile.popStack();
            compile.mv.visitFieldInsn(opcode.getOpcode(), owner.apply(compile).getInternalName(),
                    name, descriptor.getDescriptor());
            compile.pushStack(descriptor);
        }

        @Override
        public @NotNull FieldInsn descriptor(final @NonNull Name descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        @Override
        public @NotNull FieldInsn descriptor(final @NonNull Type type) {
            return descriptor(Names.of(type));
        }

        @Override
        public @NotNull FieldInsn in(final @NonNull Type owner) {
            return in(Names.of(owner));
        }

        @Override
        public @NotNull FieldInsn in(final @NonNull Name owner) {
            this.owner = __ -> owner;

            return this;
        }

        @Override
        public @NotNull FieldInsn inCurrent() {
            this.owner = compile -> compile.executable.getDeclaringClass().getName();

            return this;
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MethodInsnImpl implements MethodInsn, Consumer<Compile> {
        final String name;
        final MethodOpcode opcode;

        Function<Compile, Name> owner;
        MethodSignature descriptor;

        @Override
        public void accept(final @NonNull Compile compile) {
            if (owner == null) {
                throw new IllegalStateException("You should to specify owner using MethodInsn#in method!");
            }

            if (descriptor == null) {
                throw new IllegalStateException("You should to specify descriptor using MethodInsn#descriptor method!");
            }

            compile.requireStack(descriptor.getParameterTypes());

            compile.mv.visitMethodInsn(opcode.getOpcode(), owner.apply(compile).getInternalName(),
                    name, descriptor.getDescriptor(), opcode == MethodOpcode.INTERFACE);

            for (int i = 0, j = descriptor.getParameterTypes().length; i < j; i++) {
                compile.popStack();
            }

            if (!descriptor.getReturnType().equals(Names.VOID))
                compile.pushStack(descriptor.getReturnType());
        }

        @Override
        public @NotNull MethodInsn descriptor(final @NonNull MethodSignature signature) {
            this.descriptor = signature;
            return this;
        }

        @Override
        public @NotNull MethodInsn descriptor(final @NonNull Type returnType, final @NotNull Type @NotNull ... parameters) {
            return descriptor(Signatures.methodSignature(returnType, parameters));
        }

        @Override
        public @NotNull MethodInsn descriptor(final @NonNull Name returnType, final @NotNull Name @NotNull ... parameters) {
            return descriptor(Signatures.methodSignature(returnType, parameters));
        }

        @Override
        public @NotNull MethodInsn in(final @NonNull Type owner) {
            return in(Names.of(owner));
        }

        @Override
        public @NotNull MethodInsn in(final @NonNull Name owner) {
            this.owner = __ -> owner;

            return this;
        }

        @Override
        public @NotNull MethodInsn inCurrent() {
            this.owner = compile -> compile.executable.getDeclaringClass().getName();

            return this;
        }

        @Override
        public @NotNull MethodInsn inSuper() {
            this.owner = compile -> compile.executable.getDeclaringClass().getSuperName();

            return this;
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Local {
        Name name;
        int offset;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Compile {
        int stackSize;
        int localSize;
        int maxStackSize;
        int maxLocalSize;

        final MakeExecutable executable;
        final MethodVisitor mv;

        final LinkedList<Name> stack;
        final List<Local> locals;

        private void requireStack(final Name... params) {
            if (stack.size() < params.length) {
                throw new IllegalStateException("Not enough stack elements, required: "
                        + Arrays.toString(params) + ", but found: " + stack);
            }
        }

        private void requireStrictStack(final Name... params) {
            requireStack(params);

            int counter = 0;

            for (val stackStart : stack) {
                if (!stackStart.equals(params[counter++]))
                    throw new IllegalStateException("Required stack: "
                            + Arrays.toString(params) + ", but found: " + stack);
            }
        }

        private void pushStack(final Name name) {
            this.stack.push(name);
            this.stackSize += name.getSize();

            this.maxStackSize = Math.max(maxStackSize, stackSize);
        }

        private Name popStack() {
            if (stack.isEmpty()) {
                throw new IllegalStateException("No more elements in the stack");
            }

            val name = stack.pop();
            stackSize -= name.getSize();

            return name;
        }

    }

}
