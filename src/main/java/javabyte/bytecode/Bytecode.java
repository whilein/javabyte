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

import javabyte.bytecode.insn.Instruction;
import javabyte.make.MakeExecutable;
import javabyte.opcode.MethodOpcode;
import javabyte.signature.MethodSignature;
import javabyte.type.TypeName;
import javabyte.type.Types;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author whilein
 */
@UtilityClass
public class Bytecode {

    private static final StackItem _BOOL = new StackItemImpl(1, Types.BOOL, "integer", 1);
    private static final StackItem _BYTE = new StackItemImpl(1, Types.BYTE, "integer", 1);
    private static final StackItem _SHORT = new StackItemImpl(1, Types.SHORT, "integer", 1);
    private static final StackItem _CHAR = new StackItemImpl(1, Types.CHAR, "integer", 1);

    public static final StackItem INT = new StackItemImpl(1, Types.INT, "integer", 1);
    public static final StackItem LONG = new StackItemImpl(2, Types.LONG, "long", 2);

    public static final StackItem FLOAT = new StackItemImpl(3, Types.FLOAT, "float", 1);
    public static final StackItem DOUBLE = new StackItemImpl(4, Types.DOUBLE, "double", 2);

    public static final StackItem REF = new StackItemImpl(5, Types.OBJECT, "objectRef", 1);

    public @NotNull ExecutableInstructionSet bytecode() {
        return new ExecutableInstructionSetImpl(new ArrayList<>());
    }

    public @NotNull LocalIndex index() {
        return new LocalIndexImpl();
    }

    public @NotNull LocalIndex indexOf(final int value) {
        return new LocalIndexImpl(value);
    }

    public @NotNull Position position() {
        return new PositionImpl(new Label());
    }

    public @NotNull Position position(final @NonNull Label label) {
        return new PositionImpl(label);
    }

    public @NotNull StackItem item(final @NonNull TypeName type) {
        if (type.isPrimitive()) {
            switch (type.getPrimitive()) {
                default:
                case Types.BOOL_TYPE:
                    return _BOOL;
                case Types.BYTE_TYPE:
                    return _BYTE;
                case Types.CHAR_TYPE:
                    return _CHAR;
                case Types.SHORT_TYPE:
                    return _SHORT;
                case Types.INT_TYPE:
                    return INT;
                case Types.LONG_TYPE:
                    return LONG;
                case Types.FLOAT_TYPE:
                    return FLOAT;
                case Types.DOUBLE_TYPE:
                    return DOUBLE;
            }
        }

        return new StackItemImpl(5, type, "objectRef[" + type + "]", 1);
    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class StackItemImpl implements StackItem {

        int id;

        TypeName type;
        String name;
        int size;

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof StackItemImpl)) return false;

            val that = (StackItemImpl) obj;
            return id == that.id;
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class StackImpl implements Stack {

        final LinkedList<StackItem> stack;

        @Getter
        int size;

        @Getter
        int maxSize;

        @Override
        public void dup() {
            val item = pop();
            push(item);
            push(item);
        }

        @Override
        public void swap() {
            val first = pop();
            val second = pop();
            push(first);
            push(second);
        }

        @Override
        public void ensureSize(final int i) {
            if (stack.size() < i) {
                throw new IllegalStateException("Required stack: "
                        + Collections.nCopies(i, "any") + ", but: " + stack);
            }
        }

        @Override
        public void ensure(final @NotNull StackItem item, final int count) {
            if (stack.size() >= count) {
                int counter = 0;

                for (val stackStart : stack) {
                    if (++counter >= count) return;

                    if (!stackStart.equals(item))
                        break;
                }
            }

            throw new IllegalStateException("Required stack: "
                    + Collections.nCopies(count, item.getName()) + ", but: " + stack);
        }


        @Override
        public void ensure(final @NotNull StackItem @NonNull ... items) {
            if (stack.size() >= items.length) {
                int counter = 0;

                for (val stackStart : stack) {
                    if (counter >= items.length - 1) return;

                    if (!stackStart.equals(items[counter++]))
                        break;
                }
            }

            throw new IllegalStateException("Required stack: "
                    + Arrays.toString(items) + ", but: " + stack);
        }

        @Override
        public @NotNull StackItem pop() {
            if (stack.isEmpty()) {
                throw new IllegalStateException("No more elements in the stack");
            }

            val name = stack.pop();
            size -= name.getSize();

            return name;
        }

        private void _push(final StackItem item) {
            stack.push(item);
            size += item.getSize();

            maxSize = Math.max(maxSize, size);
        }

        @Override
        public void push(final @NonNull StackItem item) {
            _push(item);
        }

        @Override
        public void push(final @NonNull TypeName type) {
            _push(item(type));
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PositionImpl implements Position {

        Label label;

        @Override
        public void visit(final @NonNull MethodVisitor mv) {
            mv.visitLabel(label);
        }

        @Override
        public void jump(final @NonNull MethodVisitor mv, final int opcode) {
            mv.visitJumpInsn(opcode, label);
        }

        @Override
        public String toString() {
            try {
                return "L" + label.getOffset();
            } catch (IllegalStateException e) {
                return "L@unitialized";
            }
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class LocalIndexImpl implements LocalIndex {

        @Getter
        @Setter
        int value = -1;

        @Override
        public boolean isInitialized() {
            return value != -1;
        }

        @Override
        public String toString() {
            return isInitialized() ? String.valueOf(value) : "@unitialized";
        }
    }

    private static final class ExecutableInstructionSetImpl
            extends AbstractInstructionSet
            implements ExecutableInstructionSet {
        private ExecutableInstructionSetImpl(final List<Instruction> instructions) {
            super(instructions);
        }

        @Override
        public void compile(final @NonNull MakeExecutable executable, final @NonNull MethodVisitor visitor) {
            val locals = new ArrayList<Local>();

            int localSize = 0;

            if (!executable.isStatic()) {
                locals.add(new LocalImpl(executable.getDeclaringClass().getName(), new LocalIndexImpl(0), localSize));
                localSize++;
            }

            for (val parameter : executable.getParameters()) {
                locals.add(new LocalImpl(parameter, new LocalIndexImpl(locals.size()), localSize));
                localSize += parameter.getSize();
            }

            val ctx = new CompileContextImpl(new StackImpl(new LinkedList<>()), localSize, localSize,
                    executable, visitor, locals);

            compile(ctx);

            visitor.visitMaxs(ctx.getStack().getMaxSize(), ctx.maxLocalSize);
        }

    }

    @Getter
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class LocalImpl implements Local {
        TypeName name;
        LocalIndex index;

        int offset;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class CompileContextImpl implements CompileContext {

        @Getter
        final Stack stack;

        int localSize;
        int maxLocalSize;

        @Getter
        final MakeExecutable executable;

        @Getter
        final MethodVisitor methodVisitor;
        final List<Local> locals;

        @Override
        public void visitLong(final long value) {
            if (value == 0) {
                methodVisitor.visitInsn(LCONST_0);
            } else if (value == 1) {
                methodVisitor.visitInsn(LCONST_1);
            } else {
                methodVisitor.visitLdcInsn(value);
            }
        }

        @Override
        public void visitFloat(final float value) {
            if (value == 0) {
                methodVisitor.visitInsn(FCONST_0);
            } else if (value == 1) {
                methodVisitor.visitInsn(FCONST_1);
            } else if (value == 3) {
                methodVisitor.visitInsn(FCONST_1);
            } else {
                methodVisitor.visitLdcInsn(value);
            }
        }

        @Override
        public void visitDouble(final double value) {
            if (value == 0) {
                methodVisitor.visitInsn(DCONST_0);
            } else if (value == 1) {
                methodVisitor.visitInsn(DCONST_1);
            } else {
                methodVisitor.visitLdcInsn(value);
            }
        }

        @Override
        public void visitString(final @NonNull String value) {
            methodVisitor.visitLdcInsn(value);
        }

        @Override
        public void visitNull() {
            methodVisitor.visitInsn(ACONST_NULL);
        }

        @Override
        public void jump(final int opcode, final @NotNull Position position) {
            position.jump(methodVisitor, opcode);
        }

        @Override
        public void visitMethodInsn(
                final @NonNull MethodOpcode opcode,
                final @NonNull TypeName owner,
                final @NonNull String name,
                final @NonNull MethodSignature descriptor
        ) {
            for (int i = 0, j = descriptor.getParameterTypes().length; i < j; i++) {
                stack.pop();
            }

            switch (opcode) {
                case SPECIAL:
                case VIRTUAL:
                    stack.pop();
                    break;
            }

            getMethodVisitor().visitMethodInsn(
                    opcode.getOpcode(), owner.getInternalName(),
                    name, descriptor.getDescriptor(), opcode == MethodOpcode.INTERFACE
            );

            if (!descriptor.getReturnType().equals(Types.VOID)) {
                stack.push(descriptor.getReturnType());
            }
        }

        @Override
        public void visitInt(final int value) {
            if (value >= -1 && value <= 5) {
                methodVisitor.visitInsn(ICONST_0 + value);
            } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                methodVisitor.visitIntInsn(BIPUSH, value);
            } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                methodVisitor.visitIntInsn(SIPUSH, value);
            } else {
                methodVisitor.visitLdcInsn(value);
            }
        }

        @Override
        public @NotNull TypeName callArrayLoad(final @NonNull TypeName array) {
            if (!array.isArray()) {
                throw new IllegalStateException("Type " + array + " is not array!");
            }

            val component = array.dimensions(array.getDimensions() - 1);

            if (array.getDimensions() > 1 || !component.isPrimitive()) {
                methodVisitor.visitInsn(AALOAD);
            } else {
                switch (component.getPrimitive()) {
                    case Types.BYTE_TYPE:
                    case Types.BOOL_TYPE:
                        methodVisitor.visitInsn(BALOAD);
                        break;
                    case Types.CHAR_TYPE:
                        methodVisitor.visitInsn(CALOAD);
                        break;
                    case Types.SHORT_TYPE:
                        methodVisitor.visitInsn(SALOAD);
                        break;
                    case Types.INT_TYPE:
                        methodVisitor.visitInsn(IALOAD);
                        break;
                    case Types.LONG_TYPE:
                        methodVisitor.visitInsn(LALOAD);
                        break;
                    case Types.FLOAT_TYPE:
                        methodVisitor.visitInsn(FALOAD);
                        break;
                    case Types.DOUBLE_TYPE:
                        methodVisitor.visitInsn(DALOAD);
                        break;
                }
            }

            return component;
        }

        @Override
        public @NotNull Local replaceLocal(final @NonNull LocalIndex localIndex, final @NonNull TypeName name) {
            if (!localIndex.isInitialized()) {
                throw new IllegalStateException("Index should be initiailized!");
            }

            val index = localIndex.getValue();

            while (index >= locals.size()) {
                pushLocal(Types.OBJECT);
            }

            val oldLocal = this.locals.get(index);
            val oldName = oldLocal.getName();

            val local = new LocalImpl(name, localIndex, oldLocal.getOffset());

            this.locals.set(index, local);

            if (name.getSize() != oldName.getSize()) {
                val shift = name.getSize() - oldName.getSize();

                for (int i = index + 1; i < locals.size(); i++) {
                    val unshiftedLocal = locals.get(i);

                    locals.set(i, new LocalImpl(
                            unshiftedLocal.getName(), unshiftedLocal.getIndex(),
                            unshiftedLocal.getOffset() + shift
                    ));
                }

                this.localSize += shift;
                this.maxLocalSize = Math.max(maxLocalSize, localSize);
            }

            return local;
        }

        private Local _pushLocal(final LocalIndex index, final TypeName name) {
            index.setValue(locals.size());

            val local = new LocalImpl(name, index, localSize);

            this.locals.add(local);

            this.localSize += name.getSize();
            this.maxLocalSize = Math.max(maxLocalSize, localSize);

            return local;
        }

        @Override
        public @NotNull Local pushLocal(final @NonNull LocalIndex index, final @NonNull TypeName name) {
            return _pushLocal(index, name);
        }

        @Override
        public @NotNull Local pushLocal(final @NonNull TypeName name) {
            return _pushLocal(new LocalIndexImpl(), name);
        }

        @Override
        public @NotNull Local popLocal() {
            val local = this.locals.remove(this.locals.size() - 1);

            this.localSize -= local.getName().getSize();

            return local;
        }

        @Override
        public @NotNull Local getLocal(final @NonNull LocalIndex index) {
            if (!index.isInitialized()) {
                throw new IllegalStateException("Index should be initiailized!");
            }

            return locals.get(index.getValue());
        }

        @Override
        public @NotNull Local getLocal(final int index) {
            return locals.get(index);
        }

        @Override
        public void callCast(final @NonNull TypeName from, final @NonNull TypeName to) {
            if (from.equals(to)) return;

            if (from.isPrimitive() && to.isPrimitive()) {
                switch (from.getPrimitive()) {
                    case Types.BOOL_TYPE:
                    case Types.BYTE_TYPE:
                    case Types.SHORT_TYPE:
                    case Types.CHAR_TYPE:
                    case Types.INT_TYPE:
                        switch (to.getPrimitive()) {
                            case Types.FLOAT_TYPE:
                                methodVisitor.visitInsn(I2F);
                                break;
                            case Types.DOUBLE_TYPE:
                                methodVisitor.visitInsn(I2D);
                                break;
                            case Types.LONG_TYPE:
                                methodVisitor.visitInsn(I2L);
                                break;
                        }
                        break;
                    case Types.FLOAT_TYPE:
                        switch (to.getPrimitive()) {
                            case Types.BYTE_TYPE:
                                methodVisitor.visitInsn(F2I);
                                methodVisitor.visitInsn(I2B);
                                break;
                            case Types.CHAR_TYPE:
                                methodVisitor.visitInsn(F2I);
                                methodVisitor.visitInsn(I2C);
                                break;
                            case Types.SHORT_TYPE:
                                methodVisitor.visitInsn(F2I);
                                methodVisitor.visitInsn(I2S);
                                break;
                            case Types.BOOL_TYPE:
                            case Types.INT_TYPE:
                                methodVisitor.visitInsn(F2I);
                                break;
                            case Types.DOUBLE_TYPE:
                                methodVisitor.visitInsn(F2D);
                                break;
                            case Types.LONG_TYPE:
                                methodVisitor.visitInsn(F2L);
                                break;
                        }
                        break;
                    case Types.DOUBLE_TYPE:
                        switch (to.getPrimitive()) {
                            case Types.BYTE_TYPE:
                                methodVisitor.visitInsn(D2I);
                                methodVisitor.visitInsn(I2B);
                                break;
                            case Types.CHAR_TYPE:
                                methodVisitor.visitInsn(D2I);
                                methodVisitor.visitInsn(I2C);
                                break;
                            case Types.SHORT_TYPE:
                                methodVisitor.visitInsn(D2I);
                                methodVisitor.visitInsn(I2S);
                                break;
                            case Types.BOOL_TYPE:
                            case Types.INT_TYPE:
                                methodVisitor.visitInsn(D2I);
                                break;
                            case Types.FLOAT_TYPE:
                                methodVisitor.visitInsn(D2F);
                                break;
                            case Types.LONG_TYPE:
                                methodVisitor.visitInsn(D2L);
                                break;
                        }
                        break;
                    case Types.LONG_TYPE:
                        switch (to.getPrimitive()) {
                            case Types.BYTE_TYPE:
                                methodVisitor.visitInsn(L2I);
                                methodVisitor.visitInsn(I2B);
                                break;
                            case Types.CHAR_TYPE:
                                methodVisitor.visitInsn(L2I);
                                methodVisitor.visitInsn(I2C);
                                break;
                            case Types.SHORT_TYPE:
                                methodVisitor.visitInsn(L2I);
                                methodVisitor.visitInsn(I2S);
                                break;
                            case Types.BOOL_TYPE:
                            case Types.INT_TYPE:
                                methodVisitor.visitInsn(L2I);
                                break;
                            case Types.FLOAT_TYPE:
                                methodVisitor.visitInsn(L2F);
                                break;
                            case Types.DOUBLE_TYPE:
                                methodVisitor.visitInsn(L2D);
                                break;
                        }
                        break;
                }
            } else if (!from.isPrimitive() && !to.isPrimitive()) {
                methodVisitor.visitTypeInsn(CHECKCAST, to.isArray() ? to.getDescriptor() : to.getInternalName());
            } else {
                throw new IllegalStateException("Cannot cast " + from + " to " + to);
            }
        }

    }

}
