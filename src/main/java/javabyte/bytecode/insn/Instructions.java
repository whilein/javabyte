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

import javabyte.EqualityStrategy;
import javabyte.bytecode.AbstractInstructionSet;
import javabyte.bytecode.Bytecode;
import javabyte.bytecode.CompileContext;
import javabyte.bytecode.InstructionSet;
import javabyte.bytecode.Local;
import javabyte.bytecode.LocalIndex;
import javabyte.bytecode.Position;
import javabyte.bytecode.SimpleInstructionSet;
import javabyte.bytecode.StackItem;
import javabyte.bytecode.branch.CaseBranch;
import javabyte.bytecode.branch.LoopBranch;
import javabyte.opcode.CompareOpcode;
import javabyte.opcode.FieldOpcode;
import javabyte.opcode.JumpOpcode;
import javabyte.opcode.MathOpcode;
import javabyte.opcode.MethodOpcode;
import javabyte.opcode.StringsSwitchImplementation;
import javabyte.signature.MethodSignature;
import javabyte.signature.Signatures;
import javabyte.type.TypeName;
import javabyte.type.Types;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static javabyte.bytecode.Bytecode.INT;
import static javabyte.bytecode.Bytecode.REF;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DCMPL;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ACMPEQ;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LCMP;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SWAP;

/**
 * @author whilein
 */
@UtilityClass
public final class Instructions {

    public @NotNull Instruction newArrayInsn(final @NonNull TypeName name, final int knownDims) {
        if (!name.isArray()) {
            throw new IllegalArgumentException("Name should be an array");
        }

        if (knownDims > name.getDimensions()) {
            throw new IllegalArgumentException("knownDims > array.getDimensions");
        }

        return new NewArrayInsn(name, knownDims);
    }

    public @NotNull Instruction throwInsn() {
        return ThrowInsn.INSTANCE;
    }

    public @NotNull Instruction castInsn(final @NonNull TypeName name) {
        return new CastInsn(name);
    }

    public @NotNull Instruction instanceOfInsn(final @NonNull TypeName name) {
        return new InstanceOfInsn(name);
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

    public @NotNull Instruction swapInsn() {
        return SwapInsn.INSTANCE;
    }

    public @NotNull Instruction pushNullInsn() {
        return PushNullInsn.INSTANCE;
    }

    public @NotNull Instruction pushDefaultInsn(final @NonNull TypeName type) {
        return new PushDefaultInsn(type);
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

    public @NotNull Instruction compareInsn(final @NonNull CompareOpcode opcode) {
        return new CompareInsn(opcode);
    }

    public @NotNull Instruction jumpInsn(final @NonNull JumpOpcode opcode, final @NonNull Position position) {
        return new JumpInsn(opcode, position);
    }

    public @NotNull Instruction jumpIfEqualsInsn(
            final @NonNull EqualityStrategy strategy,
            final @NonNull Position position,
            final boolean inverted
    ) {
        return new JumpIfEqualsInsn(strategy, position, inverted);
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

    public @NotNull InitInsn initInsn(final @NonNull TypeName name) {
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
                Bytecode.index(),
                new LoopBranchImpl(parent, new ArrayList<>(), new Label(), new Label(), new Label())
        );
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class InitInsnImpl implements InitInsn {

        final TypeName type;

        Consumer<InstructionSet> init;
        TypeName[] parameters;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();
            
            val parameters = this.parameters != null
                    ? this.parameters
                    : new TypeName[0];

            val mv = ctx.getMethodVisitor();

            stack.push(Types.OBJECT);
            stack.push(Types.OBJECT);

            mv.visitTypeInsn(NEW, type.getInternalName());
            mv.visitInsn(DUP);

            if (this.init != null) {
                val init = SimpleInstructionSet.create();
                this.init.accept(init);

                init.compile(ctx);
            }

            stack.pop();
            stack.pop();

            for (int i = 0; i < parameters.length; i++) {
                stack.pop();
            }

            mv.visitMethodInsn(
                    INVOKESPECIAL, type.getInternalName(), "<init>",
                    Signatures.methodSignature(Types.VOID, parameters).getDescriptor(), false
            );

            stack.push(type);
        }

        @Override
        public @NotNull String toString() {
            return "[INIT " + type + "(" + (parameters != null
                    ? Arrays.stream(parameters).map(TypeName::toString).collect(Collectors.joining(", "))
                    : "") + ")";
        }

        @Override
        public @NotNull InitInsn init(@NotNull final Consumer<@NotNull InstructionSet> init) {
            this.init = init;
            return this;
        }

        @Override
        public @NotNull InitInsn parameters(final @NotNull Type @NonNull ... parameters) {
            this.parameters = Types.of(parameters);
            return this;
        }

        @Override
        public @NotNull InitInsn parameters(final @NotNull TypeName @NonNull ... parameters) {
            this.parameters = parameters;
            return this;
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class SoutInsn implements Instruction {

        public static Instruction INSTANCE = new SoutInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();
            stack.ensureSize(1);

            val mv = ctx.getMethodVisitor();
            val item = stack.pop();
            val itemType = item.getType();

            val descriptor = itemType.isPrimitive()
                    ? itemType.getDescriptor()
                    : "Ljava/lang/Object;";

            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out",
                    "Ljava/io/PrintStream;");

            stack.push(Types.of("java/lang/PrintStream"));

            mv.visitInsn(SWAP);

            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                    "(" + descriptor + ")V", false);

            stack.pop();

        }

        @Override
        public @NotNull String toString() {
            return "[SYSTEM.OUT.PRINTLN]";
        }
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class NewArrayInsn implements Instruction {

        TypeName name;
        int knownDims;

        private int arrayCode(final TypeName componentType) {
            if (componentType.isArray())
                return 1;

            if (!componentType.isPrimitive())
                return 0;

            switch (componentType.getPrimitive()) {
                case Types.BOOL_TYPE:
                    return 4;
                case Types.CHAR_TYPE:
                    return 5;
                case Types.FLOAT_TYPE:
                    return 6;
                case Types.DOUBLE_TYPE:
                    return 7;
                case Types.BYTE_TYPE:
                    return 8;
                case Types.SHORT_TYPE:
                    return 9;
                case Types.INT_TYPE:
                    return 10;
                case Types.LONG_TYPE:
                    return 11;
                default:
                    throw new IllegalArgumentException("Cannot create array: " + name);
            }
        }

        /**
         * http://hg.openjdk.java.net/jdk8/jdk8/langtools/file/30db5e0aaf83/src/share/classes/com/sun/tools/javac/jvm/Gen.java#l1750
         */
        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val mv = ctx.getMethodVisitor();

            val stack = ctx.getStack();
            stack.ensure(INT, knownDims);

            for (int i = 0; i < knownDims; i++) {
                stack.pop();
            }

            val component = name.getComponent();
            val arrayCode = arrayCode(component);

            if (arrayCode == 0 || (arrayCode == 1 && knownDims == 1)) {
                mv.visitTypeInsn(ANEWARRAY, component.getInternalName());
            } else if (arrayCode == 1) {
                mv.visitMultiANewArrayInsn(name.getInternalName(), knownDims);
            } else {
                mv.visitIntInsn(NEWARRAY, arrayCode);
            }

            stack.push(name);
        }

        @Override
        public String toString() {
            return "[NEWARRAY " + name.toString() + " (knownDims: " + knownDims + ")]";
        }
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class StoreLocalInsn implements Instruction {

        LocalIndex index;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();

            val item = stack.pop();
            val itemType = item.getType();

            val local = index.isInitialized()
                    ? ctx.replaceLocal(index, itemType)
                    : ctx.pushLocal(index, itemType);

            ctx.getMethodVisitor().visitVarInsn(itemType.toType().getOpcode(ISTORE), local.getOffset());
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
            val stack = ctx.getStack();

            val local = ctx.getLocal(index);
            val localName = local.getName();
            val localType = localName.toType();

            ctx.getMethodVisitor().visitVarInsn(localType.getOpcode(ILOAD), local.getOffset());
            stack.push(localName);
        }

        @Override
        public String toString() {
            return "[LOADLOCAL " + index + "]";
        }
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class CompareInsn implements Instruction {

        CompareOpcode opcode;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();

            final StackItem type;

            switch (opcode) {
                default:
                case LCMP:
                    type = Bytecode.LONG;
                    break;
                case FCMPL: case FCMPG:
                    type = Bytecode.FLOAT;
                    break;
                case DCMPL: case DCMPG:
                    type = Bytecode.DOUBLE;
                    break;
            }

            stack.ensure(type, 2);
            stack.pop();
            stack.pop();
            ctx.getMethodVisitor().visitInsn(opcode.getOpcode());
            stack.push(Types.INT);
        }

        @Override
        public String toString() {
            return "[" + opcode + "]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MathInsn implements Instruction {

        MathOpcode opcode;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();

            final StackItem type;

            switch (opcode) {
                default:
                case IADD: case ISUB: case IMUL: case IDIV: case IREM: case INEG:
                    type = Bytecode.INT;
                    break;
                case LADD: case LSUB: case LMUL: case LDIV: case LREM: case LNEG:
                    type = Bytecode.LONG;
                    break;
                case FADD: case FSUB: case FMUL: case FDIV: case FREM: case FNEG:
                    type = Bytecode.FLOAT;
                    break;
                case DADD: case DSUB: case DMUL: case DDIV: case DREM: case DNEG:
                    type = Bytecode.DOUBLE;
                    break;
            }

            stack.ensure(type, type);
            stack.pop();
            stack.pop();
            ctx.getMethodVisitor().visitInsn(opcode.getOpcode());
            stack.push(type);
        }

        @Override
        public String toString() {
            return "[" + opcode + "]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class SwapInsn implements Instruction {

        public static Instruction INSTANCE = new SwapInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.getMethodVisitor().visitInsn(SWAP);
            ctx.getStack().swap();
        }

        @Override
        public String toString() {
            return "[SWAP]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class DupInsn implements Instruction {

        public static Instruction INSTANCE = new DupInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.getMethodVisitor().visitInsn(DUP);
            ctx.getStack().dup();
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
            ctx.getStack().pop();
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
            if (ctx.getExecutable().getReturnType().equals(Types.VOID)) {
                ctx.getMethodVisitor().visitInsn(RETURN);
            } else {
                val stack = ctx.getStack();
                val item = stack.pop();
                val itemType = item.getType();

                ctx.getMethodVisitor().visitInsn(itemType.toType().getOpcode(IRETURN));
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
    private static final class JumpIfEqualsInsn implements Instruction {

        EqualityStrategy strategy;
        Position position;
        boolean inverted;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();

            val firstItem = stack.pop();
            val secondItem = stack.pop();

            val first = firstItem.getType();
            val second = secondItem.getType();

            val mv = ctx.getMethodVisitor();

            if (first.isPrimitive() && second.isPrimitive()) {
                ctx.callCast(second, first);

                final int INT_JUMP = inverted ? IF_ICMPNE : IF_ICMPEQ;
                final int JUMP = inverted ? IFNE : IFEQ;

                if (firstItem.equals(Bytecode.INT)) {
                    position.jump(mv, INT_JUMP);
                } else if (firstItem.equals(Bytecode.LONG)) {
                    mv.visitInsn(LCMP);
                    position.jump(mv, JUMP);
                } else if (firstItem.equals(Bytecode.FLOAT)) {
                    mv.visitInsn(FCMPL);
                    position.jump(mv, JUMP);
                } else if (firstItem.equals(Bytecode.DOUBLE)) {
                    mv.visitInsn(DCMPL);
                    position.jump(mv, JUMP);
                }
            } else if (!first.isPrimitive() && !second.isPrimitive()) {
                if (strategy == EqualityStrategy.CONTENTS && first.isArray() && second.isArray()) {
                    val cmpf = first.getComponent();
                    val cmps = second.getComponent();

                    if (cmpf.isPrimitive() && cmps.isPrimitive() && !cmps.equals(cmpf)) {
                        throw new IllegalStateException("Cannot compare two primitive arrays with types "
                                + cmpf + " and " + cmps);
                    }

                    if (cmpf.isPrimitive() != cmps.isPrimitive()) {
                        throw new IllegalStateException("Cannot compare two arrays, "
                                + "because one of them is primitive and second is not: "
                                + cmpf + " ~ " + cmps);
                    }

                    if (cmpf.isPrimitive()) {
                        val descriptor = "([" + cmpf.getDescriptor() + "[" + cmps.getDescriptor() + ")Z";

                        mv.visitMethodInsn(
                                INVOKESTATIC,
                                "java/util/Arrays",
                                "equals", descriptor, false
                        );
                    } else {
                        val name = cmpf.isArray() && cmps.isArray() ? "deepEquals" : "equals";

                        mv.visitMethodInsn(
                                INVOKESTATIC,
                                "java/util/Arrays",
                                name, "([Ljava/lang/Object;[Ljava/lang/Object;)Z", false
                        );
                    }
                } else if (strategy == EqualityStrategy.REF) {
                    position.jump(mv, inverted ? IF_ACMPNE : IF_ACMPEQ);
                    return;
                } else if (strategy == EqualityStrategy.SAFE){
                    mv.visitMethodInsn(
                            INVOKESTATIC,
                            "java/util/Objects",
                            "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false
                    );
                } else {
                    mv.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/lang/Object",
                            "equals", "(Ljava/lang/Object;)Z", false
                    );
                }

                stack.push(Types.INT);
                position.jump(mv, inverted ? IFEQ : IFNE);
                stack.pop();
            } else {
                throw new IllegalStateException("Cannot compare " + first + " and " + second);
            }
        }

        @Override
        public String toString() {
            return "[IFEQUALS \"" + position + "\"]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class JumpInsn implements Instruction {

        JumpOpcode opcode;
        Position position;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();

            switch (opcode) {
                case IF_ICMPEQ:
                case IF_ICMPNE:
                case IF_ICMPLT:
                case IF_ICMPGE:
                case IF_ICMPGT:
                case IF_ICMPLE:
                case IF_ACMPEQ:
                case IF_ACMPNE:
                    stack.ensureSize(2);
                    stack.pop();
                    stack.pop();
                    break;
                case IFEQ:
                case IFNE:
                case IFLT:
                case IFGE:
                case IFGT:
                case IFLE:
                    stack.ensure(Bytecode.INT);
                    stack.pop();
                    break;
                case IFNULL:
                case IFNONNULL:
                    stack.ensure(Bytecode.REF);
                    stack.pop();
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
            return Bytecode.position(continueLoop);
        }

        @Override
        public @NotNull Position getBreak() {
            return Bytecode.position(afterLoop);
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

        TypeName elementType;

        LocalIndex counterLocal;
        LocalIndex lengthLocal;

        LocalIndex iterableIndex;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            final TypeName iterable;
            final LocalIndex iterableIndex;

            val mv = ctx.getMethodVisitor();
            val stack = ctx.getStack();
            
            if (this.iterableIndex != null) {
                iterable = ctx.getLocal(this.iterableIndex.getValue()).getName();
                iterableIndex = this.iterableIndex;
            } else {
                iterable = stack.pop().getType();
                iterableIndex = Bytecode.index();

                if (iterable.isArray() || lengthLocal != null) {
                    ctx.pushLocal(iterableIndex, iterable);
                    mv.visitVarInsn(ASTORE, iterableIndex.getValue());
                }
            }

            if (iterable.isArray()) {
                val counter = counterLocal != null
                        ? ctx.pushLocal(counterLocal, Types.INT)
                        : ctx.pushLocal(Types.INT);

                ctx.visitInt(0);
                stack.push(Types.INT);
                stack.pop();
                mv.visitVarInsn(ISTORE, counter.getOffset());

                val length = lengthLocal != null
                        ? ctx.pushLocal(lengthLocal, Types.INT)
                        : ctx.pushLocal(Types.INT);

                stack.push(iterable);
                mv.visitVarInsn(ALOAD, iterableIndex.getValue());
                stack.pop();
                stack.push(Types.INT);
                mv.visitInsn(ARRAYLENGTH);
                stack.pop();
                mv.visitVarInsn(ISTORE, length.getOffset());

                mv.visitLabel(body.insideLoop);

                stack.push(Types.INT);
                stack.push(Types.INT);
                mv.visitVarInsn(ILOAD, counter.getOffset());
                mv.visitVarInsn(ILOAD, length.getOffset());

                stack.pop();
                stack.pop();
                mv.visitJumpInsn(IF_ICMPGE, body.afterLoop);

                stack.push(iterable);
                stack.push(Types.INT);

                mv.visitVarInsn(ALOAD, iterableIndex.getValue());
                mv.visitVarInsn(ILOAD, counter.getOffset());

                stack.pop();
                stack.pop();

                final TypeName component = ctx.callArrayLoad(iterable);
                final Local local;

                if (elementType != null && !component.equals(elementType)) {
                    ctx.callCast(component, elementType);
                    stack.push(elementType);
                    local = ctx.pushLocal(elementLocal, elementType);
                } else {
                    stack.push(component);
                    local = ctx.pushLocal(elementLocal, component);
                }

                stack.pop();
                mv.visitVarInsn(component.toType().getOpcode(ISTORE), local.getOffset());

                body.compile(ctx);

                ctx.popLocal(); // position
                ctx.popLocal(); // length
                ctx.popLocal(); // element

                mv.visitLabel(body.continueLoop);
                mv.visitIincInsn(counter.getOffset(), 1);
            } else {
                val counter = counterLocal != null
                        ? ctx.pushLocal(counterLocal, Types.INT)
                        : null;

                val length = lengthLocal != null
                        ? ctx.pushLocal(lengthLocal, Types.INT)
                        : null;

                if (counter != null) {
                    stack.push(Types.INT);
                    ctx.visitInt(0);
                    mv.visitVarInsn(ISTORE, counter.getOffset());
                }

                final LocalIndex iteratorIndex;

                if (iterableIndex.isInitialized()) {
                    // load from local
                    val iterableLocal = ctx.getLocal(iterableIndex);

                    if (length != null) {
                        stack.push(iterableLocal.getName());

                        mv.visitVarInsn(ALOAD, iterableLocal.getOffset());

                        ctx.visitMethodInsn(
                                MethodOpcode.INTERFACE, Types.COLLECTION, "size",
                                Signatures.methodSignature(Types.INT)
                        );

                        stack.pop();

                        mv.visitVarInsn(ISTORE, length.getOffset());
                    }

                    stack.push(iterableLocal.getName());
                    mv.visitVarInsn(ALOAD, iterableLocal.getOffset());

                    iteratorIndex = Bytecode.index();
                } else {
                    // already in stack
                    stack.push(iterable);
                    iteratorIndex = iterableIndex;
                }

                ctx.visitMethodInsn(
                        MethodOpcode.INTERFACE, Types.ITERABLE, "iterator",
                        Signatures.methodSignature(Types.ITERATOR)
                );

                ctx.pushLocal(iteratorIndex, Types.ITERABLE);
                mv.visitVarInsn(ASTORE, iteratorIndex.getValue());

                mv.visitLabel(body.insideLoop);
                mv.visitVarInsn(ALOAD, iteratorIndex.getValue());

                mv.visitMethodInsn(
                        INVOKEINTERFACE,
                        Types.ITERATOR.getInternalName(),
                        "hasNext", "()Z",
                        true
                );

                mv.visitJumpInsn(IFEQ, body.afterLoop);
                mv.visitVarInsn(ALOAD, iteratorIndex.getValue());

                mv.visitMethodInsn(
                        INVOKEINTERFACE,
                        Types.ITERATOR.getInternalName(),
                        "next", "()" + Types.OBJECT.getDescriptor(),
                        true
                );

                if (elementType != null) {
                    mv.visitTypeInsn(CHECKCAST, elementType.getInternalName());
                    ctx.pushLocal(elementLocal, elementType);
                } else {
                    ctx.pushLocal(elementLocal, Types.OBJECT);
                }

                mv.visitVarInsn(ASTORE, elementLocal.getValue());
                body.compile(ctx);

                if (counter != null) ctx.popLocal(); // counter
                ctx.popLocal(); // iterator
                ctx.popLocal(); // element

                mv.visitLabel(body.continueLoop);

                if (counter != null) {
                    mv.visitIincInsn(counter.getOffset(), 1);
                }
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
            this.iterableIndex = Bytecode.indexOf(index);

            return this;
        }

        @Override
        public @NotNull IterateOverInsn element(final @NonNull TypeName type) {
            this.elementType = type;

            return this;
        }

        @Override
        public @NotNull IterateOverInsn element(final @NonNull Type type) {
            this.elementType = Types.of(type);

            return this;
        }

        @Override
        public @NotNull LocalIndex getCounterLocal() {
            if (counterLocal == null) {
                counterLocal = Bytecode.index();
            }

            return counterLocal;
        }

        @Override
        public @NotNull LocalIndex getLengthLocal() {
            if (lengthLocal == null) {
                lengthLocal = Bytecode.index();
            }

            return lengthLocal;
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
            return Bytecode.position(endLabel);
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
            val stack = ctx.getStack();
            val mv = ctx.getMethodVisitor();

            final Local switchItemLocal;

            if (source != null && source.isInitialized()) {
                switchItemLocal = ctx.getLocal(source);

                val name = switchItemLocal.getName();

                mv.visitVarInsn(name.toType().getOpcode(ILOAD), switchItemLocal.getOffset());
                stack.push(name);
            } else {
                val switchItem = stack.pop();

                stack.push(switchItem);
                mv.visitInsn(DUP);

                switchItemLocal = ctx.pushLocal(Bytecode.index(), switchItem.getType());
                mv.visitVarInsn(ASTORE, switchItemLocal.getOffset());
            }

            stack.pop();
            stack.push(Types.INT);

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

            stack.pop();

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
                    stack.push(Types.STRING);
                    stack.push(Types.STRING);

                    mv.visitVarInsn(ALOAD, switchItemLocal.getOffset());
                    mv.visitLdcInsn(branch);

                    stack.pop();
                    stack.pop();

                    stack.push(Types.INT);

                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals",
                            "(Ljava/lang/Object;)Z", false);

                    stack.pop();

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
            val stack = ctx.getStack();
            val mv = ctx.getMethodVisitor();

            final Local switchItemLocal;

            if (source != null && source.isInitialized()) {
                switchItemLocal = ctx.getLocal(source);
            } else {
                val switchItem = stack.pop();

                switchItemLocal = ctx.pushLocal(Bytecode.index(), switchItem.getType());
                mv.visitVarInsn(ASTORE, switchItemLocal.getOffset());
            }

            ctx.visitInt(-1); // push switch index
            stack.push(Types.INT);

            val switchIndexLocal = ctx.pushLocal(Bytecode.index(), Types.INT);
            mv.visitVarInsn(ISTORE, switchIndexLocal.getOffset());
            stack.pop(); // pop switch index

            stack.push(Types.STRING); // push switch subject

            mv.visitVarInsn(ALOAD, switchItemLocal.getOffset());

            stack.pop(); // pop switch subject
            stack.push(Types.INT); // push switch subject hashCode

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

            stack.pop();

            int hashCounter = 0;
            int counter = 0;

            val hashIfBranches = new Label[nLabels];

            for (int i = 0; i < hashIfBranches.length; i++)
                hashIfBranches[i] = new Label();

            for (val branches : hashes.values()) {
                mv.visitLabel(hashBranches[hashCounter]);

                for (val branch : branches) {
                    mv.visitLabel(hashIfBranches[counter]);

                    stack.push(Types.STRING);
                    stack.push(Types.STRING);

                    mv.visitVarInsn(ALOAD, switchItemLocal.getOffset());
                    mv.visitLdcInsn(branch);

                    stack.pop();
                    stack.pop();

                    stack.push(Types.INT);

                    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals",
                            "(Ljava/lang/Object;)Z", false);

                    stack.pop();

                    mv.visitJumpInsn(IFEQ, counter == hashIfBranches.length - 1
                            ? endFirstSwitchLabel
                            : hashIfBranches[counter + 1]);

                    ctx.visitInt(counter);
                    stack.push(Types.INT);

                    mv.visitVarInsn(ISTORE, switchIndexLocal.getOffset());
                    stack.pop();

                    mv.visitJumpInsn(GOTO, endFirstSwitchLabel);

                    counter++;
                }

                hashCounter++;
            }

            mv.visitLabel(endFirstSwitchLabel);
            mv.visitVarInsn(ILOAD, switchIndexLocal.getOffset());

            stack.push(Types.INT);

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

            stack.pop();

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
            this.source = Bytecode.indexOf(value);
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

        @Override
        public @NotNull CaseBranch branch(final @NotNull String @NonNull ... values) {
            CaseBranch last = null;

            for (val value : values) {
                last = branch(value);
            }

            if (last == null) {
                throw new IllegalArgumentException("values is empty");
            }

            return last;
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
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();
            val mv = ctx.getMethodVisitor();

            if (source != null && source.isInitialized()) {
                val local = ctx.getLocal(source);
                mv.visitVarInsn(local.getName().toType().getOpcode(ILOAD), local.getOffset());
                stack.push(local.getName());
            }

            stack.pop();

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
                branch.compile(ctx);
            }

            defaultBranch.compile(ctx);
            mv.visitLabel(endLabel);
        }

        @Override
        public @NotNull IntsSwitchInsn source(final int value) {
            this.source = Bytecode.indexOf(value);
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

        @Override
        public @NotNull CaseBranch branch(final int @NonNull ... values) {
            CaseBranch last = null;

            for (int value : values) {
                last = branch(value);
            }

            if (last == null) {
                throw new IllegalArgumentException("values is empty");
            }

            return last;
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class FieldInsnImpl implements FieldInsn {
        final String name;
        final FieldOpcode opcode;

        Function<CompileContext, TypeName> owner;
        TypeName descriptor;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            if (owner == null) {
                throw new IllegalStateException("You should to specify owner using FieldInsn#in method!");
            }

            if (descriptor == null) {
                throw new IllegalStateException("You should to specify descriptor using FieldInsn#descriptor method!");
            }
            
            val stack = ctx.getStack();

            if (opcode == FieldOpcode.PUT || opcode == FieldOpcode.PUT_STATIC) {
                stack.pop(); // pop new field value
            }

            if (opcode == FieldOpcode.PUT || opcode == FieldOpcode.GET) {
                stack.pop(); // pop instance
            }

            ctx.getMethodVisitor().visitFieldInsn(
                    opcode.getOpcode(), owner.apply(ctx).getInternalName(),
                    name, descriptor.getDescriptor()
            );

            if (opcode == FieldOpcode.GET || opcode == FieldOpcode.GET_STATIC)
                stack.push(descriptor);
        }

        @Override
        public @NotNull FieldInsn descriptor(final @NonNull TypeName descriptor) {
            this.descriptor = descriptor;
            return this;
        }

        @Override
        public @NotNull FieldInsn descriptor(final @NonNull Type type) {
            return descriptor(Types.of(type));
        }

        @Override
        public @NotNull FieldInsn in(final @NonNull Type owner) {
            return in(Types.of(owner));
        }

        @Override
        public @NotNull FieldInsn in(final @NonNull TypeName owner) {
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

        Function<CompileContext, TypeName> owner;
        MethodSignature descriptor;

        @Override
        public void compile(final @NonNull CompileContext compile) {
            if (owner == null) {
                throw new IllegalStateException("You should to specify owner using MethodInsn#in method!");
            }

            if (descriptor == null) {
                throw new IllegalStateException("You should to specify descriptor using MethodInsn#descriptor method!");
            }

            compile.visitMethodInsn(opcode, owner.apply(compile), name, descriptor);
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
        public @NotNull MethodInsn descriptor(final @NonNull TypeName returnType, final @NotNull TypeName @NotNull ... parameters) {
            return descriptor(Signatures.methodSignature(returnType, parameters));
        }

        @Override
        public @NotNull MethodInsn in(final @NonNull Type owner) {
            return in(Types.of(owner));
        }

        @Override
        public @NotNull MethodInsn in(final @NonNull TypeName owner) {
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
            val stack = ctx.getStack();

            val item = stack.pop().getType();

            if (!Types.isWrapper(item) || item.isPrimitive() || item.isArray()) {
                stack.push(item);
                return;
            }

            final String methodName;

            val primitive = Types.getPrimitive(item);

            switch (primitive.getPrimitive()) {
                case Types.BOOL_TYPE:
                    methodName = "booleanValue";
                    break;
                case Types.BYTE_TYPE:
                    methodName = "byteValue";
                    break;
                case Types.CHAR_TYPE:
                    methodName = "charValue";
                    break;
                case Types.SHORT_TYPE:
                    methodName = "shortValue";
                    break;
                case Types.INT_TYPE:
                    methodName = "intValue";
                    break;
                case Types.LONG_TYPE:
                    methodName = "longValue";
                    break;
                case Types.FLOAT_TYPE:
                    methodName = "floatValue";
                    break;
                case Types.DOUBLE_TYPE:
                    methodName = "doubleValue";
                    break;
                default:
                    throw new IllegalStateException("Unsupported type: " + item);
            }

            ctx.getMethodVisitor().visitMethodInsn(INVOKEVIRTUAL, item.getInternalName(), methodName,
                    "()" + primitive.getDescriptor(),
                    false);

            stack.push(primitive);
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
            val stack = ctx.getStack();
            stack.ensureSize(1);

            val item = stack.pop().getType();

            if (!item.isPrimitive() || item.isArray()) {
                stack.push(item);
                return;
            }

            val wrapper = Types.getWrapper(item);

            ctx.getMethodVisitor().visitMethodInsn(INVOKESTATIC, wrapper.getInternalName(), "valueOf",
                    "(" + item.getDescriptor() + ")" + wrapper.getDescriptor(),
                    false);

            stack.push(wrapper);
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
            ctx.getStack().push(Types.OBJECT);
            ctx.visitNull();
        }

        @Override
        public String toString() {
            return "[PUSHNULL]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PushDefaultInsn implements Instruction {

        TypeName type;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.getStack().push(type);

            if (type.isPrimitive()) {
                switch (type.getPrimitive()) {
                    default:
                        ctx.visitInt(0);
                        break;
                    case Types.FLOAT_TYPE:
                        ctx.visitFloat(0);
                        break;
                    case Types.DOUBLE_TYPE:
                        ctx.visitDouble(0);
                    case Types.LONG_TYPE:
                        ctx.visitLong(0);
                        break;
                }
            } else {
                ctx.visitNull();
            }
        }

        @Override
        public String toString() {
            return "[PUSHDEFAULT " + type + "]";
        }
    }


    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class PushStringInsn implements Instruction {

        String value;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            ctx.getStack().push(Types.STRING);
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
            ctx.getStack().push(Bytecode.FLOAT);
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
            ctx.getStack().push(Bytecode.DOUBLE);
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
            ctx.getStack().push(Bytecode.INT);
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
            ctx.getStack().push(Bytecode.LONG);
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
            val stack = ctx.getStack();
            stack.ensure(INT, Bytecode.REF);
            stack.pop(); // index

            val array = stack.pop();
            stack.push(ctx.callArrayLoad(array.getType()));
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
            val stack = ctx.getStack();

            stack.pop();
            ctx.getMethodVisitor().visitInsn(ARRAYLENGTH);
            stack.push(INT);
        }

        @Override
        public String toString() {
            return "[ARRAYLENGTH]";
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class ThrowInsn implements Instruction {

        private static final Instruction INSTANCE = new ThrowInsn();

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();
            stack.ensure(REF);
            stack.pop();

            ctx.getMethodVisitor().visitInsn(ATHROW);
        }

        @Override
        public String toString() {
            return "[ARRAYLENGTH]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class InstanceOfInsn implements Instruction {
        TypeName name;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();
            stack.ensure(REF);
            stack.pop();

            val mv = ctx.getMethodVisitor();
            mv.visitTypeInsn(INSTANCEOF, name.getInternalName());

            stack.push(INT);
        }

        @Override
        public String toString() {
            return "[INSTANCEOF \"" + name + "\"]";
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class CastInsn implements Instruction {
        TypeName name;

        @Override
        public void compile(final @NonNull CompileContext ctx) {
            val stack = ctx.getStack();
            stack.ensureSize(1);

            ctx.callCast(stack.pop().getType(), name);
            stack.push(name);
        }

        @Override
        public String toString() {
            return "[CHECKCAST \"" + name + "\"]";
        }
    }

}
