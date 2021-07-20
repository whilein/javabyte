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

import javabyte.bytecode.insn.*;
import javabyte.bytecode.macro.Macro;
import javabyte.make.MakeExecutable;
import javabyte.name.Name;
import javabyte.name.Names;
import javabyte.opcode.FieldOpcode;
import javabyte.opcode.JumpOpcode;
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

    }

    private static final class BytecodeImpl extends InstructionSetImpl implements Bytecode {
        private BytecodeImpl(final List<Consumer<Compile>> instructions) {
            super(instructions);
        }

        @Override
        public void compile(final @NonNull MakeExecutable executable, final @NonNull MethodVisitor visitor) {
            val locals = new ArrayList<Local>();

            int localSize = 0;

            if (!executable.isStatic()) {
                locals.add(new Local(executable.getDeclaringClass().getName(), localSize, new LocalIndexImpl(0)));
                localSize++;
            }

            for (val parameter : executable.getParameters()) {
                locals.add(new Local(parameter, localSize, new LocalIndexImpl(locals.size())));
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

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class LoopBranchImpl extends InstructionSetImpl implements LoopBranch {
        Label insideLoop;
        Label afterLoop;

        protected LoopBranchImpl(
                final List<Consumer<Compile>> instructions,
                final Label insideLoop,
                final Label afterLoop
        ) {
            super(instructions);

            this.insideLoop = insideLoop;
            this.afterLoop = afterLoop;
        }

        @Override
        public void callContinue() {
            insn(compile -> compile.mv.visitJumpInsn(GOTO, insideLoop));
        }

        @Override
        public void callBreak() {
            insn(compile -> compile.mv.visitJumpInsn(GOTO, afterLoop));
        }

        private void write(final Compile compile) {
            for (val insn : instructions) {
                insn.accept(compile);
            }
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class IterateOverInsnImpl implements IterateOverInsn, Consumer<Compile> {

        @Getter
        final LocalIndex elementLocal;

        @Getter
        final LoopBranchImpl body;

        Name elementType;
        LocalIndex iterableIndex;

        @Override
        public void accept(final Compile compile) {
            final Name iterable;
            final LocalIndex iterableIndex;

            val mv = compile.mv;

            if (this.iterableIndex.isInitialized()) {
                iterable = compile.locals.get(this.iterableIndex.getValue()).name;
                iterableIndex = this.iterableIndex;
            } else {
                iterable = compile.popStack();
                iterableIndex = new LocalIndexImpl();

                if (iterable.isArray()) {
                    compile.pushLocal(iterable, iterableIndex);
                    mv.visitVarInsn(ASTORE, iterableIndex.getValue());
                }
            }

            if (iterable.isArray()) {
                val position = compile.pushLocal(Names.INT, new LocalIndexImpl());
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, position.offset);

                val end = compile.pushLocal(Names.INT, new LocalIndexImpl());
                mv.visitVarInsn(ALOAD, iterableIndex.getValue());
                mv.visitInsn(ARRAYLENGTH);
                mv.visitVarInsn(ISTORE, end.offset);

                mv.visitLabel(body.insideLoop);

                mv.visitVarInsn(ILOAD, position.offset);
                mv.visitVarInsn(ILOAD, end.offset);

                mv.visitJumpInsn(IF_ICMPGE, body.afterLoop);

                mv.visitVarInsn(ALOAD, iterableIndex.getValue());
                mv.visitVarInsn(ILOAD, position.offset);

                val component = compile.loadFromArray(iterable);

                if (elementType != null) {
                    compile.callCast(component, elementType);

                    compile.popStack();
                    compile.pushStack(elementType);
                }

                compile.pushLocal(component, elementLocal);
                mv.visitVarInsn(component.toType().getOpcode(ISTORE), elementLocal.getValue());

                body.write(compile);

                mv.visitIincInsn(position.offset, 1);
                mv.visitJumpInsn(GOTO, body.insideLoop);
                compile.popLocal(); // position
                compile.popLocal(); // length
                compile.popLocal(); // element
            } else {
                final LocalIndex iteratorIndex;

                if (iterableIndex.isInitialized()) {
                    // load from local
                    mv.visitVarInsn(ALOAD, iterableIndex.getValue());

                    iteratorIndex = new LocalIndexImpl();
                } else {
                    // already in stack
                    iteratorIndex = iterableIndex;
                }

                mv.visitMethodInsn(
                        INVOKEINTERFACE,
                        Names.ITERABLE.getInternalName(),
                        "iterator", "()" + Names.ITERATOR.getDescriptor(),
                        true
                );

                compile.pushLocal(Names.ITERABLE, iteratorIndex);
                mv.visitVarInsn(ASTORE, iteratorIndex.getValue());

                mv.visitLabel(body.insideLoop);
                mv.visitVarInsn(ALOAD, iteratorIndex.getValue());

                mv.visitMethodInsn(
                        INVOKEINTERFACE,
                        Names.ITERATOR.getInternalName(),
                        "hasNext", "()Z",
                        true
                );

                mv.visitJumpInsn(IFEQ, body.afterLoop);
                mv.visitVarInsn(ALOAD, iteratorIndex.getValue());

                mv.visitMethodInsn(
                        INVOKEINTERFACE,
                        Names.ITERATOR.getInternalName(),
                        "next", "()" + Names.OBJECT.getDescriptor(),
                        true
                );

                if (elementType != null) {
                    mv.visitTypeInsn(CHECKCAST, elementType.getInternalName());
                    compile.pushLocal(elementType, elementLocal);
                } else {
                    compile.pushLocal(Names.OBJECT, elementLocal);
                }

                mv.visitVarInsn(ASTORE, elementLocal.getValue());
                body.write(compile);
                mv.visitJumpInsn(GOTO, body.insideLoop);

                compile.popLocal(); // iterator
                compile.popLocal(); // element
            }

            mv.visitLabel(body.afterLoop);
        }

        @Override
        public @NotNull IterateOverInsn source(final @NonNull LocalIndex index) {
            this.iterableIndex = index;

            return this;
        }

        @Override
        public @NotNull IterateOverInsn source(final int index) {
            this.iterableIndex = new LocalIndexImpl(index);

            return this;
        }

        @Override
        public @NotNull IterateOverInsn element(final @NonNull Name type) {
            this.elementType = type;

            return this;
        }

        @Override
        public @NotNull IterateOverInsn element(final @NonNull Type type) {
            this.elementType = Names.of(type);

            return this;
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
        public @NotNull IterateOverInsn iterateOverInsn() {
            val insn = new IterateOverInsnImpl(
                    new LocalIndexImpl(),
                    new LoopBranchImpl(new ArrayList<>(), new Label(), new Label())
            );
            insn(insn);

            return insn;
        }

        @Override
        public void loadArrayLength() {
            insn(compile -> {
                compile.popStack();
                compile.mv.visitInsn(ARRAYLENGTH);
                compile.pushStack(Names.INT);
            });
        }

        @Override
        public void loadFromArray() {
            insn(compile -> {
                compile.popStack(); // index

                val array = compile.popStack();
                compile.pushStack(compile.loadFromArray(array));
            });
        }

        @Override
        public @NotNull Position newPos() {
            return new PositionImpl(new Label());
        }

        @Override
        public void setPos(final @NonNull Position position) {
            insn(compile -> position.visit(compile.mv));
        }

        @Override
        public void jumpPos(final @NonNull JumpOpcode opcode, final @NonNull Position position) {
            insn(compile -> position.jump(compile.mv, opcode.getOpcode()));
        }

        @Override
        public void loadLocal(final @NonNull LocalIndex index) {
            insn(compile -> {
                if (!index.isInitialized()) {
                    throw new IllegalStateException("Index isn't initialized");
                }

                _loadLocal(compile, index.getValue());
            });
        }

        @Override
        public void storeLocal(final int index) {
            insn(compile -> {
                val stack = compile.popStack();
                val local = compile.replaceLocal(new LocalIndexImpl(index), stack);

                compile.mv.visitVarInsn(stack.toType().getOpcode(ISTORE), local.offset);
            });
        }

        @Override
        public void storeLocal(final @NonNull LocalIndex index) {
            insn(compile -> {
                val stack = compile.popStack();
                val local = compile.replaceLocal(index, stack);

                compile.mv.visitVarInsn(stack.toType().getOpcode(ISTORE), local.offset);
            });
        }

        private void _loadLocal(final Compile compile, final int index) {
            val local = compile.locals.get(index);
            val localName = local.name;
            val localType = localName.toType();

            compile.mv.visitVarInsn(localType.getOpcode(ILOAD), local.offset);
            compile.pushStack(localName);
        }

        @Override
        public @NotNull LocalIndex storeLocal() {
            val localIndex = new LocalIndexImpl();

            insn(compile -> {
                val stack = compile.popStack();
                val local = compile.pushLocal(stack, localIndex);

                compile.mv.visitVarInsn(stack.toType().getOpcode(ISTORE), local.offset);
            });

            return localIndex;
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
            insn(compile -> compile.pushInt(value));
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
        public void callMacro(final @NonNull Macro macro) {
            insn(compile -> {
                switch (macro) {
                    case SOUT:
                        val stack = compile.popStack();

                        val descriptor = stack.isPrimitive()
                                ? stack.getDescriptor()
                                : "Ljava/lang/Object;";

                        compile.mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
                                "Ljava/io/PrintStream;");

                        compile.pushStack(Names.of("java/lang/PrintStream"));

                        compile.mv.visitInsn(SWAP);

                        compile.mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                                "(" + descriptor + ")V", false);

                        compile.popStack();

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

                if (stack.isArray()) {
                    throw new IllegalStateException("Cannot box an array");
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
                val stack = compile.popStack();

                if (stack.equals(to)) {
                    compile.pushStack(stack);
                    return;
                }

                compile.callCast(stack, to);
                compile.pushStack(to);
            });
        }

        @Override
        public void callCast(final @NonNull Type to) {
            _callCast(Names.of(to));
        }

        @Override
        public void callCast(final @NonNull Name to) {
            _callCast(to);
        }

        @Override
        public void callUnbox() {
            insn(compile -> {
                val stack = compile.popStack();

                if (stack.isPrimitive()) {
                    throw new IllegalStateException("Stack item should not be a primitive!");
                }

                if (stack.isArray()) {
                    throw new IllegalStateException("Cannot unbox an array");
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
                    compile.mv.visitInsn(stack.toType().getOpcode(IRETURN));
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

        @Override
        public @NotNull StringsSwitchInsn stringsSwitchCaseInsn() {
            val endLabel = new Label();
            val defaultLabel = new Label();

            val switchInsn = new StringsSwitchInsnImpl(new HashMap<>(), CaseBranchImpl.create(defaultLabel),
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
    private static abstract class AbstractSwitchInsn<T> implements SwitchInsn {
        Map<T, CaseBranchImpl> branches;
        CaseBranchImpl defaultBranch;
        Label endLabel;

        @Override
        public @NotNull CaseBranch defaultBranch() {
            return defaultBranch;
        }

        /**
         * http://hg.openjdk.java.net/jdk8/jdk8/langtools/file/30db5e0aaf83/src/share/classes/com/sun/tools/javac/jvm/Gen.java#l1153
         */
        protected final boolean isTableSwitchInsn(final int lo, final int hi, final int nLabels) {
            val tableSpaceCost = 4 + ((long) hi - lo + 1);
            val lookupSpaceCost = 3 + 2 * (long) nLabels;

            return nLabels > 0 && tableSpaceCost + 9 <= lookupSpaceCost + 3 * (long) nLabels;
        }

        protected final CaseBranchImpl _branch(final T value) {
            return branches.get(value);
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class StringsSwitchInsnImpl
            extends AbstractSwitchInsn<String>
            implements Consumer<Compile>, StringsSwitchInsn {

        private StringsSwitchInsnImpl(
                final Map<String, CaseBranchImpl> branches,
                final CaseBranchImpl defaultBranch,
                final Label endLabel
        ) {
            super(branches, defaultBranch, endLabel);
        }


        @Override
        public void accept(final @NonNull Compile compile) {
            val switchItem = compile.popStack();

            val mv = compile.mv;

            val switchItemLocal = compile.pushLocal(switchItem, new LocalIndexImpl());

            compile.pushInt(-1);
            compile.popStack();
            mv.visitVarInsn(ISTORE, switchItemLocal.offset);

            val switchIndexLocal = compile.pushLocal(Names.INT, new LocalIndexImpl());

            compile.pushStack(Names.STRING);
            mv.visitVarInsn(ALOAD, switchIndexLocal.offset);

            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode",
                    "()I", false);

            compile.popStack();

            compile.pushStack(Names.INT);

            val defaultLabel = defaultBranch.getLabel();

            val hashes = new TreeMap<Integer, List<String>>();

            for (val branch : branches.keySet()) {
                hashes.computeIfAbsent(branch.hashCode(), __ -> new ArrayList<>())
                        .add(branch);
            }

            val hashArray = hashes.keySet().stream()
                    .mapToInt(Integer::intValue)
                    .toArray();

            val lo = hashes.firstKey();
            val hi = hashes.lastKey();

            val nHashLabels = hashes.size();
            val nLabels = branches.size();

            val hashBranches = new Label[hashes.size()];

            for (int i = 0, j = hashBranches.length; i < j; i++)
                hashBranches[i] = new Label();

            val endFirstSwitchLabel = new Label();

            compile.popStack();

            if (nHashLabels > 1 && isTableSwitchInsn(lo, hi, nHashLabels)) {
                mv.visitTableSwitchInsn(lo, hi, endFirstSwitchLabel, hashBranches);
            } else {
                mv.visitLookupSwitchInsn(endFirstSwitchLabel, hashArray, hashBranches);
            }

            int hashCounter = 0;
            int counter = 0;

            val hashIfBranches = new Label[branches.size()];

            for (int i = 0; i < hashIfBranches.length; i++)
                hashIfBranches[i] = new Label();

            for (val branches : hashes.values()) {
                mv.visitLabel(hashBranches[hashCounter]);

                for (val branch : branches) {
                    mv.visitLabel(hashIfBranches[counter]);

                    compile.pushStack(Names.STRING);
                    compile.pushStack(Names.STRING);

                    mv.visitVarInsn(ALOAD, switchItemLocal.offset);
                    mv.visitLdcInsn(branch);

                    compile.popStack();
                    compile.popStack();

                    compile.pushStack(Names.INT);

                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals",
                            "(Ljava/lang/Object;)Z", false);

                    compile.popStack();

                    mv.visitJumpInsn(IFEQ, counter == hashIfBranches.length - 1
                            ? endFirstSwitchLabel
                            : hashIfBranches[counter + 1]);

                    compile.pushInt(counter);
                    mv.visitVarInsn(ISTORE, switchIndexLocal.offset);
                    compile.popStack();

                    mv.visitJumpInsn(GOTO, endFirstSwitchLabel);

                    counter++;
                }

                hashCounter++;
            }

            mv.visitLabel(endFirstSwitchLabel);
            mv.visitVarInsn(ILOAD, switchIndexLocal.offset);

            compile.pushStack(Names.INT);

            if (nLabels > 1) {
                val branches = new Label[nLabels];

                counter = 0;

                for (val branchList : hashes.values()) {
                    for (val branch : branchList) {
                        branches[counter++] = _branch(branch).getLabel();
                    }
                }

                mv.visitTableSwitchInsn(0, nLabels - 1, defaultLabel, branches);
            } else {
                val firstBranch = branches.values().stream().findFirst()
                        .map(CaseBranchImpl::getLabel).orElse(null);

                mv.visitLookupSwitchInsn(defaultLabel,
                        new int[]{0},
                        new Label[]{firstBranch});
            }

            compile.popStack();

            compile.popLocal(); // pop index
            compile.popLocal(); // pop item

            for (val branch : branches.values()) {
                branch.write(compile);
            }

            defaultBranch.write(compile);

            mv.visitLabel(endLabel);
        }

        @Override
        public @NotNull CaseBranch branch(final @NonNull String value) {
            return branches.computeIfAbsent(value, __ -> CaseBranchImpl.create(endLabel));
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class IntsSwitchInsnImpl
            extends AbstractSwitchInsn<Integer>
            implements Consumer<Compile>, IntsSwitchInsn {

        private IntsSwitchInsnImpl(
                final Map<Integer, CaseBranchImpl> branches,
                final CaseBranchImpl defaultBranch,
                final Label endLabel
        ) {
            super(branches, defaultBranch, endLabel);
        }

        @Override
        public void accept(final @NonNull Compile compile) {
            compile.popStack();

            val defaultLabel = defaultBranch.getLabel();

            val sortedBranches = new TreeMap<>(branches);

            val lo = sortedBranches.firstKey();
            val hi = sortedBranches.lastKey();
            val nLabels = sortedBranches.size();

            if (nLabels > 1 && isTableSwitchInsn(lo, hi, nLabels)) {
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

        @Override
        public @NotNull CaseBranch branch(final int value) {
            return branches.computeIfAbsent(value, __ -> CaseBranchImpl.create(endLabel));
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

            if (opcode == FieldOpcode.PUT || opcode == FieldOpcode.GET) {
                compile.popStack(); // pop instance
            }

            if (opcode == FieldOpcode.PUT || opcode == FieldOpcode.PUT_STATIC) {
                compile.popStack(); // pop new field value
            }

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

        LocalIndex index;
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

        private void pushInt(final int value) {
            if (value >= -1 && value <= 5) {
                mv.visitInsn(ICONST_0 + value);
            } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                mv.visitIntInsn(BIPUSH, value);
            } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                mv.visitIntInsn(SIPUSH, value);
            } else {
                mv.visitLdcInsn(value);
            }

            pushStack(Names.INT);
        }

        private Name loadFromArray(final Name array) {
            if (array.getDimensions() > 1 || !array.isPrimitive()) {
                mv.visitInsn(AALOAD);
            } else {
                switch (array.getPrimitive()) {
                    case Names.BYTE_TYPE:
                    case Names.BOOL_TYPE:
                        mv.visitInsn(BALOAD);
                        break;
                    case Names.CHAR_TYPE:
                        mv.visitInsn(CALOAD);
                        break;
                    case Names.SHORT_TYPE:
                        mv.visitInsn(SALOAD);
                        break;
                    case Names.INT_TYPE:
                        mv.visitInsn(IALOAD);
                        break;
                    case Names.LONG_TYPE:
                        mv.visitInsn(LALOAD);
                        break;
                    case Names.FLOAT_TYPE:
                        mv.visitInsn(FALOAD);
                        break;
                    case Names.DOUBLE_TYPE:
                        mv.visitInsn(DALOAD);
                        break;
                }
            }

            return array.dimensions(array.getDimensions() - 1);
        }

        private void pushStack(final Name name) {
            this.stack.push(name);
            this.stackSize += name.getSize();

            this.maxStackSize = Math.max(maxStackSize, stackSize);
        }

        private Local replaceLocal(final LocalIndex localIndex, final Name name) {
            val index = localIndex.getValue();

            val oldLocal = this.locals.get(index);
            val local = new Local(name, oldLocal.offset, localIndex);

            this.locals.set(index, local);

            val offset = name.getSize();

            if (offset != oldLocal.offset) {
                val shift = offset - oldLocal.offset;

                for (int i = index + 1; i < locals.size(); i++) {
                    val unshiftedLocal = locals.get(i);

                    locals.set(i, new Local(unshiftedLocal.name, unshiftedLocal.offset + shift,
                            unshiftedLocal.index));
                }

                this.localSize += name.getSize();
                this.maxLocalSize = Math.max(maxLocalSize, localSize);
            }

            return local;
        }

        private Local pushLocal(final Name name, final LocalIndex index) {
            index.setValue(locals.size());

            val local = new Local(name, localSize, index);

            this.locals.add(local);

            this.localSize += name.getSize();
            this.maxLocalSize = Math.max(maxLocalSize, localSize);

            return local;
        }

        private Name popLocal() {
            val name = this.locals.remove(this.locals.size() - 1);

            this.localSize -= name.offset;

            return name.name;
        }

        private void callCast(final Name from, final Name to) {
            if (!from.isArray()
                    && from.isPrimitive() && to.isPrimitive()
                    && from.getPrimitive() != Names.BOOL_TYPE
                    && to.getPrimitive() != Names.BOOL_TYPE) {
                switch (from.getPrimitive()) {
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
            } else if (!from.isPrimitive() && !to.isPrimitive()) {
                mv.visitTypeInsn(CHECKCAST, to.isArray() ? to.getDescriptor() : to.getInternalName());
            } else {
                throw new IllegalStateException("Cannot cast " + stack + " to " + to);
            }
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
