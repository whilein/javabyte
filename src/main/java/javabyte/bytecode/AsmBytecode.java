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
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

    private void inst(final Consumer<Compile> instruction) {
        instructions.add(instruction);
    }

    private void _invoke(final int opcode, final Name owner, final String name, final MethodSignature descriptor) {
        inst(compile -> {
            compile.mv.visitMethodInsn(opcode, owner.getInternalName(), name, descriptor.getDescriptor(),
                    opcode == INVOKEINTERFACE);

            for (int i = 0, j = descriptor.getParameterTypes().length; i < j; i++) {
                compile.popStack();
            }

            if (!descriptor.getReturnType().equals(Names.VOID))
                compile.pushStack(descriptor.getReturnType());
        });
    }

    private void pushLocal(final Compile compile, final int index) {
        val param = compile.locals.get(index);
        val paramType = param.getType();

        compile.mv.visitVarInsn(paramType.getOpcode(ILOAD), index);
        compile.pushStack(param);
    }

    @Override
    public void pushLocal(final int index) {
        inst(compile -> pushLocal(compile, index));
    }

    @Override
    public void pushThis() {
        inst(compile -> {
            if (compile.executable.isStatic()) {
                throw new IllegalStateException("Cannot push this because method is static");
            }

            pushLocal(compile, 0);
        });
    }

    @Override
    public void invokeVirtual(
            final @NonNull Name owner,
            final @NonNull String name,
            final @NonNull MethodSignature signature
    ) {
        _invoke(INVOKEVIRTUAL, owner, name, signature);
    }

    @Override
    public void invokeVirtual(
            final @NonNull Type owner,
            final @NonNull String name,
            final @NonNull MethodSignature signature
    ) {
        _invoke(INVOKEVIRTUAL, Names.of(owner), name, signature);
    }

    @Override
    public void invokeVirtual(
            final @NonNull String owner,
            final @NonNull String name,
            final @NonNull MethodSignature signature
    ) {
        _invoke(INVOKEVIRTUAL, Names.exact(owner), name, signature);
    }

    @Override
    public void invokeSpecial(
            final @NonNull Name owner,
            final @NonNull String name,
            final @NonNull MethodSignature signature
    ) {
        _invoke(INVOKESPECIAL, owner, name, signature);
    }

    @Override
    public void invokeSpecial(
            final @NonNull Type owner,
            final @NonNull String name,
            final @NonNull MethodSignature signature
    ) {
        _invoke(INVOKESPECIAL, Names.of(owner), name, signature);
    }

    @Override
    public void invokeSpecial(
            final @NonNull String owner,
            final @NonNull String name,
            final @NonNull MethodSignature signature
    ) {
        _invoke(INVOKESPECIAL, Names.exact(owner), name, signature);
    }

    @Override
    public void pushString(final @NotNull String value) {
        inst(compile -> {
            compile.mv.visitLdcInsn(value);
            compile.pushStack(Names.STRING);
        });
    }

    @Override
    public void pushInt(final int value) {
        inst(compile -> {
            if (value >= 0 && value <= 5) {
                compile.mv.visitInsn(ICONST_0 + value);
            } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE){
                compile.mv.visitIntInsn(BIPUSH, value);
            } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE){
                compile.mv.visitIntInsn(SIPUSH, value);
            } else {
                compile.mv.visitLdcInsn(value);
            }

            compile.pushStack(Names.INT);
        });
    }

    @Override
    public void pushNull() {
        inst(compile -> {
            compile.mv.visitInsn(ACONST_NULL);
            compile.pushStack(Names.OBJECT);
        });
    }

    @Override
    public void callBox() {
        inst(compile -> {
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
        inst(compile -> {
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
        inst(compile -> {
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
        val locals = new HashMap<Integer, Name>();

        int offset = 0;
        int localSize = 0;

        if (!executable.isStatic()) {
            locals.put(offset++, executable.getDeclaringClass().getName());
            localSize++;
        }

        for (val parameter : executable.getParameters()) {
            locals.put(offset++, parameter);
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
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Compile {
        int stackSize;
        int localSize;
        int maxStackSize;
        int maxLocalSize;

        final MakeExecutable executable;
        final MethodVisitor mv;

        final LinkedList<Name> stack;
        final Map<Integer, Name> locals;

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
