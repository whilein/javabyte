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
import javabyte.name.Name;
import javabyte.opcode.FieldOpcode;
import javabyte.opcode.MathOpcode;
import javabyte.opcode.MethodOpcode;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * @author whilein
 */
public interface InstructionSet {
    void loadLocal(int index);

    @NotNull FieldInsn fieldInsn(@NotNull FieldOpcode opcode, @NotNull String name);
    @NotNull MethodInsn methodInsn(@NotNull MethodOpcode opcode, @NotNull String name);

    void loadString(@NotNull String string);

    void loadInt(int value);
    void loadFloat(float value);
    void loadDouble(double value);
    void loadLong(long value);

    void loadNull();

    void callBox();
    void callMacro(@NotNull Macro macro);
    void callMath(@NotNull MathOpcode opcode);

    void callCast(@NotNull Type to);
    void callCast(@NotNull Name to);

    void callUnbox();

    void callReturn();

    @NotNull IntsSwitchInsn intsSwitchCaseInsn();

}
