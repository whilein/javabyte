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
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

/**
 * @author whilein
 */
public interface CompileContext {

    void requireStack(@NotNull Name @NotNull ... types);
    void requireStrictStack(@NotNull Name @NotNull ... types);

    void jump(int opcode, @NotNull Position position);

    void visitInt(int value);
    void visitLong(long value);

    void visitFloat(float value);
    void visitDouble(double value);

    void visitString(@NotNull String value);
    void visitNull();

    void callCast(@NotNull Name from, @NotNull Name to);
    @NotNull Name callArrayLoad(@NotNull Name array);

    void pushStack(@NotNull Name name);
    @NotNull Name popStack();

    @NotNull Local replaceLocal(@NotNull LocalIndex index, @NotNull Name name);
    @NotNull Local pushLocal(@NotNull LocalIndex index, @NotNull Name name);
    @NotNull Local pushLocal(@NotNull Name name);
    @NotNull Local popLocal();

    @NotNull Local getLocal(@NotNull LocalIndex index);
    @NotNull Local getLocal(int index);

    @NotNull MakeExecutable getExecutable();
    @NotNull MethodVisitor getMethodVisitor();

}