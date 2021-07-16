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
import javabyte.signature.MethodSignature;
import javabyte.type.Field;
import javabyte.type.Invoke;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

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

    private void _invoke(final Compile compile, final Invoke invoke,
                         final Name owner, final String name,
                         final MethodSignature descriptor) {
        compile.mv.visitMethodInsn(invoke.getOpcode(), owner.getInternalName(), name, descriptor.getDescriptor(),
                invoke.getOpcode() == INVOKEINTERFACE);

        for (int i = 0, j = descriptor.getParameterTypes().length; i < j; i++) {
            compile.popStack();
        }

        if (!descriptor.getReturnType().equals(Names.VOID))
            compile.pushStack(descriptor.getReturnType());
    }

    private void _invoke(final Invoke invoke, final Name owner, final String name, final MethodSignature descriptor) {
        insn(compile -> _invoke(compile, invoke, owner, name, descriptor));
    }

    private void pushLocal(final Compile compile, final int index) {
        val local = compile.locals.get(index);
        val localName = local.name;
        val localType = localName.getType();

        compile.mv.visitVarInsn(localType.getOpcode(ILOAD), local.offset);
        compile.pushStack(localName);
    }

    @Override
    public void pushLocal(final int index) {
        insn(compile -> pushLocal(compile, index));
    }

    @Override
    public void pushThis() {
        insn(compile -> {
            if (compile.executable.isStatic()) {
                throw new IllegalStateException("Cannot push this because method is static");
            }

            pushLocal(compile, 0);
        });
    }

    @Override
    public void invokeOwn(
            final @NonNull Invoke invoke,
            final @NonNull String name,
            final @NonNull MethodSignature descriptor
    ) {
        insn(compile -> _invoke(compile, invoke, compile.executable.getDeclaringClass().getName(),
                name, descriptor));
    }


    @Override
    public void invokeSuper(
            final @NonNull Invoke invoke,
            final @NonNull String name,
            final @NonNull MethodSignature descriptor
    ) {
        insn(compile -> _invoke(compile, invoke, compile.executable.getDeclaringClass().getSuperName(),
                name, descriptor));
    }

    @Override
    public void invoke(
            final @NonNull Invoke invoke,
            final @NonNull Name owner,
            final @NonNull String name,
            final @NonNull MethodSignature signature
    ) {
        _invoke(invoke, owner, name, signature);
    }

    @Override
    public void invoke(
            final @NonNull Invoke invoke,
            final @NonNull Type owner,
            final @NonNull String name,
            final @NonNull MethodSignature signature
    ) {
        _invoke(invoke, Names.of(owner), name, signature);
    }

    @Override
    public void fieldOwn(final @NonNull Field field, final @NonNull String name, final @NonNull Type type) {
        insn(compile -> _field(compile, field.getOpcode(), compile.executable.getDeclaringClass().getName(),
                name, Names.of(type)));
    }

    @Override
    public void fieldOwn(final @NonNull Field field, final @NonNull String name, final @NonNull Name type) {
        insn(compile -> _field(compile, field.getOpcode(), compile.executable.getDeclaringClass().getName(),
                name, type));
    }

    private void _field(final Compile compile, final int opcode, final Name owner, final String name, final Name type) {
        if (opcode == PUTFIELD || opcode == PUTSTATIC) {
            compile.popStack();
        }

        compile.popStack();
        compile.mv.visitFieldInsn(opcode, owner.getInternalName(), name, type.getDescriptor());
        compile.pushStack(type);
    }

    private void _field(final int opcode, final Name owner, final String name, final Name type) {
        insn(compile -> _field(compile, opcode, owner, name, type));
    }

    @Override
    public void field(
            final @NonNull Field field,
            final @NonNull Name owner,
            final @NonNull String name,
            final @NonNull Name type
    ) {
        _field(field.getOpcode(), owner, name, type);
    }

    @Override
    public void field(
            final @NonNull Field field,
            final @NonNull Type owner,
            final @NonNull String name,
            final @NonNull Name type
    ) {
        _field(field.getOpcode(), Names.of(owner), name, type);
    }

    @Override
    public void field(
            final @NonNull Field field,
            final @NonNull Type owner,
            final @NonNull String name,
            final @NonNull Type type
    ) {
        _field(field.getOpcode(), Names.of(owner), name, Names.of(type));
    }

    @Override
    public void field(
            final @NonNull Field field,
            final @NonNull Name owner,
            final @NonNull String name,
            final @NonNull Type type
    ) {
        _field(field.getOpcode(), owner, name, Names.of(type));
    }

    @Override
    public void pushString(final @NotNull String value) {
        insn(compile -> {
            compile.mv.visitLdcInsn(value);
            compile.pushStack(Names.STRING);
        });
    }

    @Override
    public void pushInt(final int value) {
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
    public void pushNull() {
        insn(compile -> {
            compile.mv.visitInsn(ACONST_NULL);
            compile.pushStack(Names.OBJECT);
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

        private void pushStack(final Name name) {
            this.stack.push(name);
            this.stackSize += name.getSize();

            this.maxStackSize = Math.max(maxStackSize, stackSize);
        }

        private Name popStack() {
            val name = stack.pop();
            stackSize -= name.getSize();

            return name;
        }

    }

}
