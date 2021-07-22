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
import javabyte.opcode.FieldOpcode;
import javabyte.opcode.JumpOpcode;
import javabyte.opcode.MathOpcode;
import javabyte.opcode.MethodOpcode;
import javabyte.type.TypeName;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * @author whilein
 */
public interface InstructionSet extends Instruction {

    void callInsn(@NotNull Instruction instruction);
    void callSout();

    void pop();
    void dup();

    @NotNull Position initPosition();
    void visit(@NotNull Position position);
    void jump(@NotNull JumpOpcode opcode, @NotNull Position position);

    void loadLocal(int index);
    void loadLocal(@NotNull LocalIndex index);
    void storeLocal(int index);
    void storeLocal(@NotNull LocalIndex index);
    @NotNull LocalIndex storeLocal();

    @NotNull FieldInsn fieldInsn(@NotNull FieldOpcode opcode, @NotNull String name);
    @NotNull MethodInsn methodInsn(@NotNull MethodOpcode opcode, @NotNull String name);

    void pushString(@NotNull String string);

    void pushInt(int value);
    void pushFloat(float value);
    void pushDouble(double value);
    void pushLong(long value);
    void pushNull();

    void pushDefault(@NotNull TypeName type);

    void loadFromArray();
    void loadArrayLength();

    @NotNull IterateOverInsn iterateOverInsn();

    void callBox();

    void callMath(@NotNull MathOpcode opcode);

    void callNewArray(@NotNull TypeName arrayType, int knownDims);
    void callNewArray(@NotNull Type arrayType, int knownDims);

    void callThrow();

    void callNewArray(@NotNull TypeName arrayType);
    void callNewArray(@NotNull Type arrayType);

    void callCast(@NotNull Type to);
    void callCast(@NotNull TypeName to);

    @NotNull InitInsn callInit(@NotNull Type type);
    @NotNull InitInsn callInit(@NotNull TypeName name);

    void callUnbox();

    void callReturn();

    @NotNull IntsSwitchInsn intsSwitchCaseInsn();
    @NotNull StringsSwitchInsn stringsSwitchCaseInsn();

}
