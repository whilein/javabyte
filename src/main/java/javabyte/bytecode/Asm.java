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

import javabyte.bytecode.insn.FieldInsn;
import javabyte.bytecode.insn.IntsSwitchInsn;
import javabyte.bytecode.insn.MethodInsn;
import javabyte.bytecode.macro.Macro;
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
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author whilein
 */
@UtilityClass
public class Asm {

    public @NotNull Bytecode bytecode() {
        return new BytecodeImpl(new ArrayList<>());
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class LocalIndexImpl implements LocalIndex {

        @Getter
        @Setter
        int value;

        @Override
        public boolean isInitialized() {
            return value != 0;
        }

    }

    private static final class BytecodeImpl extends InstructionSetImpl implements Bytecode {
        private BytecodeImpl(final List<Consumer<Compile>> instructions) {
            super(instructions);
        }

        @Override
        public void compile(@NotNull final MakeExecutable executable, @NotNull final MethodVisitor visitor) {
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
    }

    @FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class InstructionSetImpl implements InstructionSet {

        List<Consumer<Compile>> instructions;

        protected void insn(final Consumer<Compile> instruction) {
            instructions.add(instruction);
        }

        @Override
        public void loadLocal(final @NonNull LocalIndex index) {
            insn(compile -> {
                if (index.isInitialized()) {
                    throw new IllegalStateException("Index isn't initialized");
                }

                _loadLocal(compile, index.getValue());
            });
        }

        private void _loadLocal(final Compile compile, final int index) {
            val local = compile.locals.get(index);
            val localName = local.name;
            val localType = localName.getType();

            compile.mv.visitVarInsn(localType.getOpcode(ILOAD), local.offset);
            compile.pushStack(localName);
        }

        @Override
        public @NotNull LocalIndex storeLocal() {
            val index = new LocalIndexImpl();

            insn(compile -> {
                val stack = compile.popStack();
                val stackSize = stack.getSize();

                val locals = compile.locals;
                val localIndex = locals.size();
                index.setValue(localIndex);

                val lastLocal = locals.get(localIndex - 1);

                val offset = lastLocal != null
                        ? lastLocal.offset + stackSize
                        : stackSize;

                locals.add(new Local(stack, offset));

                compile.mv.visitVarInsn(stack.getType().getOpcode(ISTORE), offset);
            });

            return index;
        }

        @Override
        public void loadLocal(final int index) {
            insn(compile -> _loadLocal(compile, index));
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
                if (value >= -1 && value <= 5) {
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
        public void callMacro(@NotNull final Macro macro) {
            insn(compile -> {
                switch (macro) {
                    case SOUT:
                        val stack = compile.popStack();

                        val descriptor = stack.isPrimitive()
                                ? stack.getDescriptor()
                                : "Ljava/lang/Object;";

                        compile.mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
                                "Ljava/io/PrintStream;");

                        compile.mv.visitInsn(SWAP);

                        compile.mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                                "(" + descriptor + ")V", false);

                        break;
                }
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
        public @NotNull IntsSwitchInsn intsSwitchCaseInsn() {
            val endLabel = new Label();
            val defaultLabel = new Label();

            val switchInsn = new IntsSwitchInsnImpl(new HashMap<>(), CaseBranchImpl.create(defaultLabel),
                    endLabel);

            insn(switchInsn);

            return switchInsn;
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class CaseBranchImpl extends InstructionSetImpl implements CaseBranch {

        @Getter
        Label label;

        Label endLabel;

        private CaseBranchImpl(
                final List<Consumer<Compile>> instructions,
                final Label label,
                final Label endLabel
        ) {
            super(instructions);

            this.label = label;
            this.endLabel = endLabel;
        }

        private static CaseBranchImpl create(final Label endLabel) {
            return new CaseBranchImpl(new ArrayList<>(), new Label(), endLabel);
        }

        private void write(final Compile compile) {
            compile.mv.visitLabel(label);

            for (val insn : instructions) {
                insn.accept(compile);
            }
        }

        @Override
        public void callBreak() {
            insn(compiler -> compiler.mv.visitJumpInsn(GOTO, endLabel));
        }
    }

    @FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    private static abstract class AbstractSwitchInsn {
        Map<Integer, CaseBranchImpl> branches;
        CaseBranchImpl defaultBranch;
        Label endLabel;
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class IntsSwitchInsnImpl
            extends AbstractSwitchInsn
            implements Consumer<Compile>, IntsSwitchInsn {

        private IntsSwitchInsnImpl(
                final Map<Integer, CaseBranchImpl> branches,
                final CaseBranchImpl defaultBranch,
                final Label endLabel
        ) {
            super(branches, defaultBranch, endLabel);
        }

        /**
         * http://hg.openjdk.java.net/jdk8/jdk8/langtools/file/30db5e0aaf83/src/share/classes/com/sun/tools/javac/jvm/Gen.java#l1153
         */
        private boolean isTableSwitchInsn(final int lo, final int hi, final int nLabels) {
            val tableSpaceCost = 4 + ((long) hi - lo + 1);
            val lookupSpaceCost = 3 + 2 * (long) nLabels;

            return nLabels > 0 && tableSpaceCost + 9 <= lookupSpaceCost + 3 * (long) nLabels;
        }

        @Override
        public void accept(final @NonNull Compile compile) {
            compile.popStack();

            val defaultLabel = defaultBranch.getLabel();

            val sortedBranches = new TreeMap<>(branches);

            final int lo = sortedBranches.firstKey();
            final int hi = sortedBranches.lastKey();
            final int nLabels = sortedBranches.size();

            if (branches.size() > 1 && isTableSwitchInsn(lo, hi, nLabels)) {
                val branches = new Label[nLabels];

                for (int i = 0, j = branches.length; i < j; i++) {
                    val branch = _branch(lo + i);

                    branches[i] = branch != null
                            ? branch.getLabel()
                            : defaultLabel;
                }

                compile.mv.visitTableSwitchInsn(lo, hi, defaultLabel, branches);
            } else {
                val keys = sortedBranches.keySet().stream()
                        .mapToInt(Integer::intValue)
                        .toArray();

                val values = sortedBranches.values().stream()
                        .map(CaseBranchImpl::getLabel)
                        .toArray(Label[]::new);

                compile.mv.visitLookupSwitchInsn(defaultLabel, keys,
                        values);
            }

            for (val branch : branches.values()) {
                branch.write(compile);
            }

            defaultBranch.write(compile);

            compile.mv.visitLabel(endLabel);
        }

        public CaseBranchImpl _branch(final int value) {
            return branches.get(value);
        }

        @Override
        public @NotNull CaseBranch branch(final int value) {
            return branches.computeIfAbsent(value, __ -> CaseBranchImpl.create(endLabel));
        }

        @Override
        public @NotNull CaseBranch defaultBranch() {
            return defaultBranch;
        }
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
