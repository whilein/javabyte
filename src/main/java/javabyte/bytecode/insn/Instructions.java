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

package javabyte.bytecode.insn;

import javabyte.bytecode.*;
import javabyte.bytecode.branch.CaseBranch;
import javabyte.bytecode.branch.LoopBranch;
import javabyte.name.Name;
import javabyte.name.Names;
import javabyte.opcode.*;
import javabyte.signature.MethodSignature;
import javabyte.signature.Signatures;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author whilein
 */
@UtilityClass
public final class Instructions {

    public @NotNull Instruction castInsn(final @NonNull Name name) {
        return new CastInsn(name);
    }

    public @NotNull Instruction arrayLengthInsn() {
        return ArrayLengthInsn.INSTANCE;
    }

    public @NotNull Instruction arrayLoadInsn() {
        return ArrayLoadInsn.INSTANCE;
    }

    public @NotNull Instruction popInsn() {
        return PopInsn.INSTANCE;
    }

    public @NotNull Instruction dupInsn() {
        return DupInsn.INSTANCE;
    }

    public @NotNull Instruction pushNullInsn() {
        return PushNullInsn.INSTANCE;
    }

    public @NotNull Instruction pushIntInsn(final int value) {
        return new PushIntInsn(value);
    }

    public @NotNull Instruction pushLongInsn(final long value) {
        return new PushLongInsn(value);
    }

    public @NotNull Instruction pushFloatInsn(final float value) {
        return new PushFloatInsn(value);
    }

    public @NotNull Instruction pushDoubleInsn(final double value) {
        return new PushDoubleInsn(value);
    }

    public @NotNull Instruction pushStringInsn(final @NonNull String value) {
        return new PushStringInsn(value);
    }

    public @NotNull Instruction mathInsn(final @NonNull MathOpcode opcode) {
        return new MathInsn(opcode);
    }

    public @NotNull Instruction jumpInsn(final @NonNull JumpOpcode opcode, final @NonNull Position position) {
        return new JumpInsn(opcode, position);
    }

    public @NotNull Instruction visitInsn(final @NonNull Position position) {
        return new VisitInsn(position);
    }

    public @NotNull Instruction returnInsn() {
        return ReturnInsn.INSTANCE;
    }

    public @NotNull Instruction boxInsn() {
        return BoxInsn.INSTANCE;
    }

    public @NotNull Instruction unboxInsn() {
        return UnboxInsn.INSTANCE;
    }

    public @NotNull InitInsn initInsn(final @NonNull Name name) {
        return new InitInsnImpl(name);
    }

    public @NotNull StringsSwitchInsn stringsSwitchInsn(final @NonNull InstructionSet parent) {
        return new StringsSwitchInsnImpl(
                parent, new HashMap<>(),
                CaseBranchImpl.create(new Label(), parent), new Label()
        );
    }

    public @NotNull IntsSwitchInsn intsSwitchInsn(final @NonNull InstructionSet parent) {
        return new IntsSwitchInsnImpl(
                parent, new HashMap<>(),
                CaseBranchImpl.create(new Label(), parent), new Label()
        );
    }

    public @NotNull FieldInsn fieldInsn(final @NonNull FieldOpcode opcode, final @NonNull String name) {
        return new FieldInsnImpl(name, opcode);
    }

    public @NotNull MethodInsn methodInsn(final @NonNull MethodOpcode opcode, final @NonNull String name) {
        return new MethodInsnImpl(name, opcode);
    }

    public @NotNull Instruction loadLocalInsn(final @NonNull LocalIndex index) {
        return new LoadLocalInsn(index);
    }

    public @NotNull Instruction storeLocalInsn(final @NonNull LocalIndex index) {
        return new StoreLocalInsn(index);
    }

    public @NotNull Instruction sout() {
        return SoutInsn.INSTANCE;
    }

    public @NotNull IterateOverInsn iterateOverInsn(final @NonNull InstructionSet parent) {
        return new IterateOverInsnImpl(
                Asm.index(),
                new LoopBranchImpl(parent, new ArrayList<>(), new Label(), new Label(), new Label())
        );
    }

    private static final class Init extends AbstractInstructionSet {

        private Init(final List<Instruction> instructions) {
            super(instructions);
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class InitInsnImpl implements InitInsn {

        final Name type;

        Consumer<InstructionSet> init;
        Name[] parameters;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val parameters = this.parameters != null
                    ? this.parameters
                    : new Name[0];

            val mv = ctx.getMethodVisitor();

            ctx.pushStack(Names.OBJECT);
            ctx.pushStack(Names.OBJECT);

            mv.visitTypeInsn(NEW, type.getInternalName());
            mv.visitInsn(DUP);

            if (this.init != null) {
                val init = new Init(new ArrayList<>());
                this.init.accept(init);

                init.compile(ctx);
            }

            ctx.popStack();
            ctx.popStack();

            for (int i = 0; i < parameters.length; i++) {
                ctx.popStack();
            }

            mv.visitMethodInsn(
                    INVOKESPECIAL, type.getInternalName(), "<init>",
                    Signatures.methodSignature(Names.VOID, parameters).getDescriptor(), false
            );

            ctx.pushStack(type);
        }

        @Override
        public @NotNull String toString() {
            return "[INIT " + type + "(" + (parameters != null
                    ? Arrays.stream(parameters).map(Name::toString).collect(Collectors.joining(", "))
                    : "") + ")";
        }

        @Override
        public @NotNull InitInsn init(@NotNull final Consumer<@NotNull InstructionSet> init) {
            this.init = init;
            return this;
        }

        @Override
        public @NotNull InitInsn parameters(final @NotNull Type @NonNull ... parameters) {
            this.parameters = Names.of(parameters);
            return this;
        }

        @Override
        public @NotNull InitInsn parameters(final @NotNull Name @NonNull ... parameters) {
            this.parameters = parameters;
            return this;
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class SoutInsn implements Instruction {

        public static Instruction INSTANCE = new SoutInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val mv = ctx.getMethodVisitor();
            val stack = ctx.popStack();

            val descriptor = stack.isPrimitive()
                    ? stack.getDescriptor()
                    : "Ljava/lang/Object;";

            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
                    "Ljava/io/PrintStream;");

            ctx.pushStack(Names.of("java/lang/PrintStream"));

            mv.visitInsn(SWAP);

            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(" + descriptor + ")V", false);

            ctx.popStack();

        }

        @Override
        public @NotNull String toString() {
            return "[SYSTEM.OUT.PRINTLN]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class StoreLocalInsn implements Instruction {

        LocalIndex index;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.popStack();

            if (index.isInitialized()) {
                ctx.replaceLocal(index, stack);
            } else {
                ctx.pushLocal(index, stack);
            }

            ctx.getMethodVisitor().visitVarInsn(stack.toType().getOpcode(ISTORE), index.getValue());
        }

        @Override
        public String toString() {
            return "[STORELOCAL " + index + "]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class LoadLocalInsn implements Instruction {

        LocalIndex index;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val local = ctx.getLocal(index);
            val localName = local.getName();
            val localType = localName.toType();

            ctx.getMethodVisitor().visitVarInsn(localType.getOpcode(ILOAD), local.getOffset());
            ctx.pushStack(localName);
        }

        @Override
        public String toString() {
            return "[LOADLOCAL " + index + "]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MathInsn implements Instruction {

        MathOpcode opcode;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
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

            ctx.requireStrictStack(type, type);
            ctx.popStack();
            ctx.popStack();
            ctx.getMethodVisitor().visitInsn(opcode.getOpcode());
            ctx.pushStack(type);
        }

        @Override
        public String toString() {
            return "[" + opcode + "]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class DupInsn implements Instruction {

        public static Instruction INSTANCE = new DupInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.getMethodVisitor().visitInsn(DUP);
            val old = ctx.popStack();
            ctx.pushStack(old);
            ctx.pushStack(old);
        }

        @Override
        public String toString() {
            return "[DUP]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PopInsn implements Instruction {

        public static Instruction INSTANCE = new PopInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.getMethodVisitor().visitInsn(POP);
            ctx.popStack();
        }

        @Override
        public String toString() {
            return "[POP]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ReturnInsn implements Instruction {

        public static Instruction INSTANCE = new ReturnInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            if (ctx.getExecutable().getReturnType().equals(Names.VOID)) {
                ctx.getMethodVisitor().visitInsn(RETURN);
            } else {
                val stack = ctx.popStack();
                ctx.getMethodVisitor().visitInsn(stack.toType().getOpcode(IRETURN));
            }
        }

        @Override
        public String toString() {
            return "[RETURN]";
        }
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class VisitInsn implements Instruction {

        Position position;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            position.visit(ctx.getMethodVisitor());
        }

        @Override
        public String toString() {
            return "[VISIT \"" + position + "\"]";
        }
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class JumpInsn implements Instruction {

        JumpOpcode opcode;
        Position position;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            switch (opcode) {
                case IF_ICMPEQ:
                case IF_ICMPNE:
                case IF_ICMPLT:
                case IF_ICMPGE:
                case IF_ICMPGT:
                case IF_ICMPLE:
                case IF_ACMPEQ:
                case IF_ACMPNE:
                    ctx.popStack();
                    ctx.popStack();
                    break;
            }

            ctx.jump(opcode.getOpcode(), position);
        }

        @Override
        public String toString() {
            return "[" + opcode + " \"" + position + "\"]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class LoopBranchImpl extends AbstractInstructionSet implements LoopBranch {

        @Getter
        InstructionSet parent;
        Label continueLoop;
        Label insideLoop;
        Label afterLoop;

        protected LoopBranchImpl(
                final InstructionSet parent,
                final List<Instruction> instructions,
                final Label continueLoop,
                final Label insideLoop,
                final Label afterLoop
        ) {
            super(instructions);

            this.parent = parent;
            this.continueLoop = continueLoop;
            this.insideLoop = insideLoop;
            this.afterLoop = afterLoop;
        }

        @Override
        public void callContinue() {
            _callInsn(Instructions.jumpInsn(JumpOpcode.GOTO, getContinue()));
        }

        @Override
        public void callBreak() {
            _callInsn(Instructions.jumpInsn(JumpOpcode.GOTO, getBreak()));
        }

        @Override
        public @NotNull Position getContinue() {
            return Asm.position(continueLoop);
        }

        @Override
        public @NotNull Position getBreak() {
            return Asm.position(afterLoop);
        }

        @Override
        public void callContinue(final int depth) {
            jump(JumpOpcode.GOTO, getContinue(depth));
        }

        @Override
        public @NotNull Position getContinue(final int depth) {
            return _getOuter(depth).getContinue();
        }

        @Override
        public void callBreak(final int depth) {
            jump(JumpOpcode.GOTO, getBreak(depth));
        }

        @Override
        public @NotNull Position getBreak(final int depth) {
            return _getOuter(depth).getBreak();
        }

        private LoopBranch _getOuter(final int depth) {
            if (depth < 1) {
                throw new IllegalArgumentException("depth < 1");
            }

            if (depth == 1) {
                return this;
            }

            LoopBranch current = this;
            int counter = 0;

            while (counter < depth && current.getParent() instanceof LoopBranch) {
                current = (LoopBranch) current.getParent();
                counter++;
            }

            return current;
        }

    }


    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class IterateOverInsnImpl implements IterateOverInsn {

        @Getter
        final LocalIndex elementLocal;

        @Getter
        final LoopBranchImpl body;

        Name elementType;
        LocalIndex iterableIndex;

        @Override
        public void compile(final @NonNull CompileContext compile) {
            final Name iterable;
            final LocalIndex iterableIndex;

            val mv = compile.getMethodVisitor();

            if (this.iterableIndex != null) {
                iterable = compile.getLocal(this.iterableIndex.getValue()).getName();
                iterableIndex = this.iterableIndex;
            } else {
                iterable = compile.popStack();
                iterableIndex = Asm.index();

                if (iterable.isArray()) {
                    compile.pushLocal(iterableIndex, iterable);
                    mv.visitVarInsn(ASTORE, iterableIndex.getValue());
                }
            }

            if (iterable.isArray()) {
                val position = compile.pushLocal(Names.INT);
                compile.visitInt(0);
                compile.pushStack(Names.INT);
                compile.popStack();
                mv.visitVarInsn(ISTORE, position.getOffset());

                val end = compile.pushLocal(Names.INT);
                compile.pushStack(iterable);
                mv.visitVarInsn(ALOAD, iterableIndex.getValue());
                compile.popStack();
                compile.pushStack(Names.INT);
                mv.visitInsn(ARRAYLENGTH);
                compile.popStack();
                mv.visitVarInsn(ISTORE, end.getOffset());

                mv.visitLabel(body.insideLoop);

                compile.pushStack(Names.INT);
                compile.pushStack(Names.INT);
                mv.visitVarInsn(ILOAD, position.getOffset());
                mv.visitVarInsn(ILOAD, end.getOffset());

                compile.popStack();
                compile.popStack();
                mv.visitJumpInsn(IF_ICMPGE, body.afterLoop);

                compile.pushStack(iterable);
                compile.pushStack(Names.INT);

                mv.visitVarInsn(ALOAD, iterableIndex.getValue());
                mv.visitVarInsn(ILOAD, position.getOffset());

                compile.popStack();
                compile.popStack();

                val component = compile.callArrayLoad(iterable);

                if (elementType != null && !component.equals(elementType)) {
                    compile.callCast(component, elementType);
                    compile.pushStack(elementType);
                    compile.pushLocal(elementLocal, elementType);
                } else {
                    compile.pushStack(component);
                    compile.pushLocal(elementLocal, component);
                }

                compile.popStack();
                mv.visitVarInsn(component.toType().getOpcode(ISTORE), elementLocal.getValue());

                body.compile(compile);

                compile.popLocal(); // position
                compile.popLocal(); // length
                compile.popLocal(); // element

                mv.visitLabel(body.continueLoop);
                mv.visitIincInsn(position.getOffset(), 1);
            } else {
                final LocalIndex iteratorIndex;

                if (iterableIndex.isInitialized()) {
                    // load from local
                    mv.visitVarInsn(ALOAD, iterableIndex.getValue());

                    iteratorIndex = Asm.index();
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

                compile.pushLocal(iteratorIndex, Names.ITERABLE);
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
                    compile.pushLocal(elementLocal, elementType);
                } else {
                    compile.pushLocal(elementLocal, Names.OBJECT);
                }

                mv.visitVarInsn(ASTORE, elementLocal.getValue());
                body.compile(compile);

                compile.popLocal(); // iterator
                compile.popLocal(); // element

                mv.visitLabel(body.continueLoop);
            }

            mv.visitJumpInsn(GOTO, body.insideLoop);
            mv.visitLabel(body.afterLoop);
        }

        @Override
        public @NotNull IterateOverInsn source(final @NonNull LocalIndex index) {
            this.iterableIndex = index;

            return this;
        }

        @Override
        public @NotNull IterateOverInsn source(final int index) {
            this.iterableIndex = Asm.indexOf(index);

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

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static final class CaseBranchImpl extends AbstractInstructionSet implements CaseBranch {

        @Getter
        Label label;

        Label endLabel;

        @Getter
        InstructionSet parent;

        private CaseBranchImpl(
                final List<Instruction> instructions,
                final Label label,
                final Label endLabel,
                final InstructionSet parent
        ) {
            super(instructions);

            this.label = label;
            this.endLabel = endLabel;

            this.parent = parent;
        }

        private static CaseBranchImpl create(final Label endLabel, final InstructionSet parent) {
            return new CaseBranchImpl(new ArrayList<>(), new Label(), endLabel, parent);
        }

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.getMethodVisitor().visitLabel(label);
            super.compile(ctx);
        }

        @Override
        public void callBreak() {
            _callInsn(Instructions.jumpInsn(JumpOpcode.GOTO, getBreak()));
        }

        @Override
        public @NotNull Position getBreak() {
            return Asm.position(endLabel);
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

    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static final class StringsSwitchInsnImpl
            extends AbstractSwitchInsn<String>
            implements StringsSwitchInsn {

        final InstructionSet parent;

        LocalIndex source;
        StringsSwitchImplementation impl = StringsSwitchImplementation.JAVAC;

        private StringsSwitchInsnImpl(
                final InstructionSet parent,
                final Map<String, CaseBranchImpl> branches,
                final CaseBranchImpl defaultBranch,
                final Label endLabel
        ) {
            super(branches, defaultBranch, endLabel);

            this.parent = parent;
        }

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            switch (impl) {
                case JAVAC:
                    _javac(ctx);
                    break;
                case ECJ:
                    _ecj(ctx);
                    break;
            }
        }

        private void _ecj(final CompileContext ctx) {
            val mv = ctx.getMethodVisitor();

            final Local switchItemLocal;

            if (source != null && source.isInitialized()) {
                switchItemLocal = ctx.getLocal(source);

                val name = switchItemLocal.getName();

                mv.visitVarInsn(name.toType().getOpcode(ILOAD), switchItemLocal.getOffset());
                ctx.pushStack(name);
            } else {
                val switchItem = ctx.popStack();

                ctx.pushStack(switchItem);
                mv.visitInsn(DUP);

                switchItemLocal = ctx.pushLocal(Asm.index(), switchItem);
                mv.visitVarInsn(ASTORE, switchItemLocal.getOffset());
            }

            ctx.popStack();
            ctx.pushStack(Names.INT);

            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode",
                    "()I", false);

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
            val hashBranches = new Label[nHashLabels];

            for (int i = 0, j = hashBranches.length; i < j; i++)
                hashBranches[i] = new Label();

            ctx.popStack();

            if (nHashLabels > 1 && isTableSwitchInsn(lo, hi, nHashLabels)) {
                val table = new Label[hi - lo + 1];

                int counter = 0;

                for (int i = 0; i < table.length; i++) {
                    val hash = hashes.get(lo + i);

                    table[i] = hash == null
                            ? defaultLabel
                            : hashBranches[counter++];
                }

                // eclipse compiler doesn't uses table switch,
                // but why not
                mv.visitTableSwitchInsn(lo, hi, defaultLabel, table);
            } else {
                mv.visitLookupSwitchInsn(defaultLabel, hashArray, hashBranches);
            }

            int hashCounter = 0;

            val end = new Label();

            for (val branches : hashes.values()) {
                mv.visitLabel(hashBranches[hashCounter]);

                for (val branch : branches) {
                    ctx.pushStack(Names.STRING);
                    ctx.pushStack(Names.STRING);

                    mv.visitVarInsn(ALOAD, switchItemLocal.getOffset());
                    mv.visitLdcInsn(branch);

                    ctx.popStack();
                    ctx.popStack();

                    ctx.pushStack(Names.INT);

                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals",
                            "(Ljava/lang/Object;)Z", false);

                    ctx.popStack();

                    mv.visitJumpInsn(IFNE, this.branches.get(branch).getLabel());
                }

                mv.visitJumpInsn(GOTO, defaultLabel);
                hashCounter++;
            }

            ctx.popLocal(); // pop switch item

            for (val branch : branches.values()) {
                branch.compile(ctx);
                mv.visitJumpInsn(GOTO, end);
            }

            defaultBranch.compile(ctx);

            mv.visitLabel(end);
        }

        private void _javac(final CompileContext ctx) {
            val mv = ctx.getMethodVisitor();

            final Local switchItemLocal;

            if (source != null && source.isInitialized()) {
                switchItemLocal = ctx.getLocal(source);
            } else {
                val switchItem = ctx.popStack();

                switchItemLocal = ctx.pushLocal(Asm.index(), switchItem);
                mv.visitVarInsn(ASTORE, switchItemLocal.getOffset());
            }

            ctx.visitInt(-1); // push switch index
            ctx.pushStack(Names.INT);

            val switchIndexLocal = ctx.pushLocal(Asm.index(), Names.INT);
            mv.visitVarInsn(ISTORE, switchIndexLocal.getOffset());
            ctx.popStack(); // pop switch index

            ctx.pushStack(Names.STRING); // push switch subject

            mv.visitVarInsn(ALOAD, switchItemLocal.getOffset());

            ctx.popStack(); // pop switch subject
            ctx.pushStack(Names.INT); // push switch subject hashCode

            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode",
                    "()I", false);

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

            val hashBranches = new Label[nHashLabels];

            for (int i = 0, j = hashBranches.length; i < j; i++)
                hashBranches[i] = new Label();

            val endFirstSwitchLabel = new Label();

            if (nHashLabels > 1 && isTableSwitchInsn(lo, hi, nHashLabels)) {
                val table = new Label[hi - lo + 1];

                int counter = 0;

                for (int i = 0; i < table.length; i++) {
                    val hash = hashes.get(lo + i);

                    table[i] = hash == null
                            ? endFirstSwitchLabel
                            : hashBranches[counter++];
                }

                mv.visitTableSwitchInsn(lo, hi, endFirstSwitchLabel, table);
            } else {
                mv.visitLookupSwitchInsn(endFirstSwitchLabel, hashArray, hashBranches);
            }

            ctx.popStack();

            int hashCounter = 0;
            int counter = 0;

            val hashIfBranches = new Label[nLabels];

            for (int i = 0; i < hashIfBranches.length; i++)
                hashIfBranches[i] = new Label();

            for (val branches : hashes.values()) {
                mv.visitLabel(hashBranches[hashCounter]);

                for (val branch : branches) {
                    mv.visitLabel(hashIfBranches[counter]);

                    ctx.pushStack(Names.STRING);
                    ctx.pushStack(Names.STRING);

                    mv.visitVarInsn(ALOAD, switchItemLocal.getOffset());
                    mv.visitLdcInsn(branch);

                    ctx.popStack();
                    ctx.popStack();

                    ctx.pushStack(Names.INT);

                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals",
                            "(Ljava/lang/Object;)Z", false);

                    ctx.popStack();

                    mv.visitJumpInsn(IFEQ, counter == hashIfBranches.length - 1
                            ? endFirstSwitchLabel
                            : hashIfBranches[counter + 1]);

                    ctx.visitInt(counter);
                    ctx.pushStack(Names.INT);

                    mv.visitVarInsn(ISTORE, switchIndexLocal.getOffset());
                    ctx.popStack();

                    mv.visitJumpInsn(GOTO, endFirstSwitchLabel);

                    counter++;
                }

                hashCounter++;
            }

            mv.visitLabel(endFirstSwitchLabel);
            mv.visitVarInsn(ILOAD, switchIndexLocal.getOffset());

            ctx.pushStack(Names.INT);

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

            ctx.popStack();

            ctx.popLocal(); // pop index
            ctx.popLocal(); // pop item

            for (val branch : branches.values()) {
                branch.compile(ctx);
            }

            defaultBranch.compile(ctx);

            mv.visitLabel(endLabel);
        }

        @Override
        public @NotNull StringsSwitchInsn source(final int value) {
            this.source = Asm.indexOf(value);
            return this;
        }

        @Override
        public @NotNull StringsSwitchInsn source(final @NonNull LocalIndex index) {
            this.source = index;
            return this;
        }

        @Override
        public @NotNull StringsSwitchInsn impl(final @NonNull StringsSwitchImplementation impl) {
            this.impl = impl;

            return this;
        }

        @Override
        public @NotNull CaseBranch branch(final @NonNull String value) {
            return branches.computeIfAbsent(value, __ -> CaseBranchImpl.create(endLabel, parent));
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static final class IntsSwitchInsnImpl
            extends AbstractSwitchInsn<Integer>
            implements IntsSwitchInsn {

        final InstructionSet parent;
        LocalIndex source;

        private IntsSwitchInsnImpl(
                final InstructionSet parent,
                final Map<Integer, CaseBranchImpl> branches,
                final CaseBranchImpl defaultBranch,
                final Label endLabel
        ) {
            super(branches, defaultBranch, endLabel);

            this.parent = parent;
        }

        @Override
        public void compile(final @NonNull CompileContext compile) {
            val mv = compile.getMethodVisitor();

            if (source != null && source.isInitialized()) {
                val local = compile.getLocal(source);
                mv.visitVarInsn(local.getName().toType().getOpcode(ILOAD), local.getOffset());
                compile.pushStack(local.getName());
            }

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

                mv.visitTableSwitchInsn(lo, hi, defaultLabel, branches);
            } else {
                val keys = sortedBranches.keySet().stream()
                        .mapToInt(Integer::intValue)
                        .toArray();

                val values = sortedBranches.values().stream()
                        .map(CaseBranchImpl::getLabel)
                        .toArray(Label[]::new);

                mv.visitLookupSwitchInsn(defaultLabel, keys,
                        values);
            }

            for (val branch : branches.values()) {
                branch.compile(compile);
            }

            defaultBranch.compile(compile);
            mv.visitLabel(endLabel);
        }

        @Override
        public @NotNull IntsSwitchInsn source(final int value) {
            this.source = Asm.indexOf(value);
            return this;
        }

        @Override
        public @NotNull IntsSwitchInsn source(final @NonNull LocalIndex index) {
            this.source = index;
            return this;
        }

        @Override
        public @NotNull CaseBranch branch(final int value) {
            return branches.computeIfAbsent(value, __ -> CaseBranchImpl.create(endLabel, parent));
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class FieldInsnImpl implements FieldInsn {
        final String name;
        final FieldOpcode opcode;

        Function<CompileContext, Name> owner;
        Name descriptor;

        @Override
        public void compile(final @NonNull CompileContext compile) {
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

            compile.getMethodVisitor().visitFieldInsn(
                    opcode.getOpcode(), owner.apply(compile).getInternalName(),
                    name, descriptor.getDescriptor()
            );

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
            this.owner = compile -> compile.getExecutable().getDeclaringClass().getName();

            return this;
        }

    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MethodInsnImpl implements MethodInsn {
        final String name;
        final MethodOpcode opcode;

        Function<CompileContext, Name> owner;
        MethodSignature descriptor;

        @Override
        public void compile(final @NonNull CompileContext compile) {
            if (owner == null) {
                throw new IllegalStateException("You should to specify owner using MethodInsn#in method!");
            }

            if (descriptor == null) {
                throw new IllegalStateException("You should to specify descriptor using MethodInsn#descriptor method!");
            }

            compile.requireStack(descriptor.getParameterTypes());

            compile.getMethodVisitor().visitMethodInsn(
                    opcode.getOpcode(), owner.apply(compile).getInternalName(),
                    name, descriptor.getDescriptor(), opcode == MethodOpcode.INTERFACE
            );

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
            this.owner = compile -> compile.getExecutable().getDeclaringClass().getName();

            return this;
        }

        @Override
        public @NotNull MethodInsn inSuper() {
            this.owner = compile -> compile.getExecutable().getDeclaringClass().getSuperName();

            return this;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class UnboxInsn implements Instruction {

        private static final Instruction INSTANCE = new UnboxInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.popStack();

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

            ctx.getMethodVisitor().visitMethodInsn(INVOKEVIRTUAL, stack.getInternalName(), methodName,
                    "()" + primitive.getDescriptor(),
                    false);

            ctx.pushStack(primitive);
        }

        @Override
        public String toString() {
            return "[UNBOX]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class BoxInsn implements Instruction {

        private static final Instruction INSTANCE = new BoxInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.popStack();

            if (!stack.isPrimitive()) {
                throw new IllegalStateException("Stack item should be a primitive!");
            }

            if (stack.isArray()) {
                throw new IllegalStateException("Cannot box an array");
            }

            val wrapper = Names.getWrapper(stack);

            ctx.getMethodVisitor().visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf",
                    "(" + stack.getDescriptor() + ")" + wrapper.getDescriptor(),
                    false);

            ctx.pushStack(wrapper);
        }

        @Override
        public String toString() {
            return "[BOX]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PushNullInsn implements Instruction {

        private static final Instruction INSTANCE = new PushNullInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.pushStack(Names.OBJECT);
            ctx.visitNull();
        }

        @Override
        public String toString() {
            return "[PUSHNULL]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PushStringInsn implements Instruction {

        String value;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.pushStack(Names.STRING);
            ctx.visitString(value);
        }

        @Override
        public String toString() {
            return "[PUSHSTR \"" + value + "\"]";
        }
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PushFloatInsn implements Instruction {

        float value;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.pushStack(Names.FLOAT);
            ctx.visitFloat(value);
        }

        @Override
        public String toString() {
            return "[PUSHFLOAT \"" + value + "\"]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PushDoubleInsn implements Instruction {

        double value;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.pushStack(Names.DOUBLE);
            ctx.visitDouble(value);
        }

        @Override
        public String toString() {
            return "[PUSHDOUBLE \"" + value + "\"]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PushIntInsn implements Instruction {

        int value;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.pushStack(Names.INT);
            ctx.visitInt(value);
        }

        @Override
        public String toString() {
            return "[PUSHINT \"" + value + "\"]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PushLongInsn implements Instruction {

        long value;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.pushStack(Names.LONG);
            ctx.visitLong(value);
        }

        @Override
        public String toString() {
            return "[PUSHLONG \"" + value + "\"]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ArrayLoadInsn implements Instruction {

        private static final Instruction INSTANCE = new ArrayLoadInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.popStack(); // index

            val array = ctx.popStack();
            ctx.pushStack(ctx.callArrayLoad(array));
        }

        @Override
        public String toString() {
            return "[xALOAD]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ArrayLengthInsn implements Instruction {

        private static final Instruction INSTANCE = new ArrayLengthInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.popStack();
            ctx.getMethodVisitor().visitInsn(ARRAYLENGTH);
            ctx.pushStack(Names.INT);
        }

        @Override
        public String toString() {
            return "[ARRAYLENGTH]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class CastInsn implements Instruction {
        Name name;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.callCast(ctx.popStack(), name);
            ctx.pushStack(name);
        }

        @Override
        public String toString() {
            return "[CHECKCAST \"" + name + "\"]";
        }
    }

}
