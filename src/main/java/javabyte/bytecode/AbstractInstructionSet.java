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
import javabyte.name.Name;
import javabyte.name.Names;
import javabyte.opcode.FieldOpcode;
import javabyte.opcode.JumpOpcode;
import javabyte.opcode.MathOpcode;
import javabyte.opcode.MethodOpcode;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Label;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author whilein
 */
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractInstructionSet implements InstructionSet {

    List<Instruction> instructions;

    @Override
    public void compile(@NotNull final CompileContext ctx) {
        for (val instruction : instructions) {
            instruction.compile(ctx);
        }
    }

    @Override
    public final void callInsn(final @NonNull Instruction instruction) {
        _callInsn(instruction);
    }

    protected final void _callInsn(final Instruction instruction) {
        instructions.add(instruction);
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
    public final @NotNull Position initPosition() {
        return Asm.position(new Label());
    }

    @Override
    public final void visit(final @NonNull Position position) {
        _callInsn(Instructions.visitInsn(position));
    }

    @Override
    public final void jump(final @NonNull JumpOpcode opcode, final @NonNull Position position) {
        _callInsn(Instructions.jumpInsn(opcode, position));
    }

    @Override
    public final void loadLocal(final @NonNull LocalIndex index) {
        _callInsn(Instructions.loadLocalInsn(index));
    }

    @Override
    public final void loadLocal(final int index) {
        _callInsn(Instructions.loadLocalInsn(Asm.indexOf(index)));
    }

    @Override
    public final void storeLocal(final int index) {
        _callInsn(Instructions.storeLocalInsn(Asm.indexOf(index)));
    }

    @Override
    public final void storeLocal(final @NonNull LocalIndex index) {
        _callInsn(Instructions.storeLocalInsn(index));
    }

    @Override
    public final @NotNull LocalIndex storeLocal() {
        val localIndex = Asm.index();

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
    public final void loadNull() {
        _callInsn(Instructions.pushNullInsn());
    }

    @Override
    public final void callMath(final @NonNull MathOpcode opcode) {
        _callInsn(Instructions.mathInsn(opcode));
    }

    @Override
    public final void callBox() {
        _callInsn(Instructions.boxInsn());
    }

    @Override
    public final void callCast(final @NonNull Type to) {
        _callInsn(Instructions.castInsn(Names.of(to)));
    }

    @Override
    public final void callCast(final @NonNull Name to) {
        _callInsn(Instructions.castInsn(to));
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
