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

import javabyte.EqualityStrategy;
import javabyte.bytecode.insn.*;
import javabyte.opcode.*;
import javabyte.type.TypeName;
import javabyte.type.Types;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * @author whilein
 */
@FieldDefaults(level = AccessLevel.PROTECTED)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractInstructionSet implements InstructionSet {

    Deque<Instruction> inserted;

    final List<Instruction> instructions;

    protected final void _callInsn(final Instruction instruction) {
        (inserted == null ? instructions : inserted).add(instruction);
    }

    @Override
    public void whenCompile(final @NonNull Runnable runnable) {
        _callInsn(context -> runnable.run());
    }

    @Override
    public final void pop() {
        _callInsn(Instructions.popInsn());
    }

    @Override
    public final void dup() {
        _callInsn(Instructions.dupInsn());
    }

    @Override
    public final void swap() {
        _callInsn(Instructions.swapInsn());
    }

    @Override
    public final @NotNull InitInsn callInit(
            final @NonNull Type type
    ) {
        val init = Instructions.initInsn(Types.of(type));
        _callInsn(init);

        return init;
    }

    @Override
    public final @NotNull InitInsn callInit(
            final @NonNull TypeName name
    ) {
        val init = Instructions.initInsn(name);
        _callInsn(init);

        return init;
    }

    @Override
    public void compile(final @NonNull CompileContext ctx) {
        inserted = new ArrayDeque<>();

        for (val instruction : instructions) {
            instruction.compile(ctx);

            Instruction insertedEntry;

            while ((insertedEntry = inserted.poll()) != null) {
                insertedEntry.compile(ctx);
            }
        }

        inserted = null;
    }

    @Override
    public final void callInsn(final @NonNull Instruction instruction) {
        _callInsn(instruction);
    }

    @Override
    public final void callSout() {
        instructions.add(Instructions.sout());
    }

    @Override
    public final @NotNull IterateOverInsn iterateOverInsn() {
        val insn = Instructions.iterateOverInsn(this);
        _callInsn(insn);

        return insn;
    }

    @Override
    public final void loadArrayLength() {
        _callInsn(Instructions.arrayLengthInsn());
    }

    @Override
    public final void loadFromArray() {
        _callInsn(Instructions.arrayLoadInsn());
    }

    @Override
    public final void visit(final @NonNull Position position) {
        _callInsn(Instructions.visitInsn(position));
    }

    @Override
    public final void jump(final @NonNull JumpOpcode opcode, final @NonNull Position position) {
        _callInsn(Instructions.jumpInsn(opcode, position));
    }

    private void _jumpIfEquals(
            final EqualityStrategy strategy,
            final Position position,
            final boolean inverted
    ) {
        _callInsn(Instructions.jumpIfEqualsInsn(strategy, position, inverted));
    }

    @Override
    public final void jumpIfEquals(final @NonNull Position position) {
        _jumpIfEquals(EqualityStrategy.DEFAULT, position, false);
    }

    @Override
    public final void jumpIfNotEquals(final @NonNull Position position) {
        _jumpIfEquals(EqualityStrategy.DEFAULT, position, true);
    }

    @Override
    public final void jumpIfEquals(final @NonNull EqualityStrategy strategy, final @NonNull Position position) {
        _jumpIfEquals(strategy, position, false);
    }

    @Override
    public final void jumpIfNotEquals(final @NonNull EqualityStrategy strategy, final @NonNull Position position) {
        _jumpIfEquals(strategy, position, true);
    }
    
    @Override
    public final void loadLocal(final @NonNull LocalIndex index) {
        _callInsn(Instructions.loadLocalInsn(index));
    }

    @Override
    public final void loadLocal(final int index) {
        _callInsn(Instructions.loadLocalInsn(Bytecode.indexOf(index)));
    }

    @Override
    public final void storeLocal(final int index) {
        _callInsn(Instructions.storeLocalInsn(Bytecode.indexOf(index)));
    }

    @Override
    public final void storeLocal(final @NonNull LocalIndex index) {
        _callInsn(Instructions.storeLocalInsn(index));
    }

    @Override
    public final @NotNull LocalIndex storeLocal() {
        val localIndex = Bytecode.index();

        _callInsn(Instructions.storeLocalInsn(localIndex));

        return localIndex;
    }

    @Override
    public final @NotNull FieldInsn fieldInsn(final @NonNull FieldOpcode opcode, final @NonNull String name) {
        val field = Instructions.fieldInsn(opcode, name);
        _callInsn(field);

        return field;
    }

    @Override
    public final @NotNull MethodInsn methodInsn(final @NonNull MethodOpcode opcode, final @NonNull String name) {
        val method = Instructions.methodInsn(opcode, name);
        _callInsn(method);

        return method;
    }

    @Override
    public final void pushString(final @NotNull String value) {
        _callInsn(Instructions.pushStringInsn(value));
    }

    @Override
    public final void pushInt(final int value) {
        _callInsn(Instructions.pushIntInsn(value));
    }

    @Override
    public final void pushFloat(final float value) {
        _callInsn(Instructions.pushFloatInsn(value));
    }

    @Override
    public final void pushDouble(final double value) {
        _callInsn(Instructions.pushDoubleInsn(value));
    }

    @Override
    public final void pushLong(final long value) {
        _callInsn(Instructions.pushLongInsn(value));
    }

    @Override
    public final void pushNull() {
        _callInsn(Instructions.pushNullInsn());
    }

    @Override
    public final void pushDefault(final @NonNull TypeName type) {
        _callInsn(Instructions.pushDefaultInsn(type));
    }

    @Override
    public final void callMath(final @NonNull MathOpcode opcode) {
        _callInsn(Instructions.mathInsn(opcode));
    }

    @Override
    public final void callCompare(final @NonNull CompareOpcode opcode) {
        _callInsn(Instructions.compareInsn(opcode));
    }

    @Override
    public final void callBox() {
        _callInsn(Instructions.boxInsn());
    }

    @Override
    public final void callCast(final @NonNull Type to) {
        _callInsn(Instructions.castInsn(Types.of(to)));
    }

    @Override
    public final void callNewArray(final @NonNull TypeName arrayType, final int knownDims) {
        _callInsn(Instructions.newArrayInsn(arrayType, knownDims));
    }

    @Override
    public final void callNewArray(final @NonNull Type arrayType, final int knownDims) {
        _callInsn(Instructions.newArrayInsn(Types.of(arrayType), knownDims));
    }

    @Override
    public final void callNewArray(final @NonNull TypeName arrayType) {
        _callInsn(Instructions.newArrayInsn(arrayType, arrayType.getDimensions()));
    }

    @Override
    public final void callThrow() {
        _callInsn(Instructions.throwInsn());
    }

    @Override
    public final void callNewArray(final @NonNull Type arrayType) {
        val name = Types.of(arrayType);

        _callInsn(Instructions.newArrayInsn(name, name.getDimensions()));
    }


    @Override
    public final void callCast(final @NonNull TypeName to) {
        _callInsn(Instructions.castInsn(to));
    }

    @Override
    public final void callInstanceOf(final @NonNull Type type) {
        _callInsn(Instructions.instanceOfInsn(Types.of(type)));

    }

    @Override
    public final void callInstanceOf(final @NonNull TypeName name) {
        _callInsn(Instructions.instanceOfInsn(name));
    }

    @Override
    public final void callUnbox() {
        _callInsn(Instructions.unboxInsn());
    }

    @Override
    public final void callReturn() {
        _callInsn(Instructions.returnInsn());
    }

    @Override
    public final @NotNull IntsSwitchInsn intsSwitchCaseInsn() {
        val switchInsn = Instructions.intsSwitchInsn(this);
        _callInsn(switchInsn);

        return switchInsn;
    }

    @Override
    public final @NotNull StringsSwitchInsn stringsSwitchCaseInsn() {
        val switchInsn = Instructions.stringsSwitchInsn(this);
        _callInsn(switchInsn);

        return switchInsn;
    }


}
