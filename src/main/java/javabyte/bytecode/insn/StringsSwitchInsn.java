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

import javabyte.bytecode.LocalIndex;
import javabyte.bytecode.branch.CaseBranch;
import javabyte.opcode.StringsSwitchImplementation;
import org.jetbrains.annotations.NotNull;

/**
 * @author whilein
 */
public interface StringsSwitchInsn extends SwitchInsn {

    @NotNull StringsSwitchInsn source(int value);
    @NotNull StringsSwitchInsn source(@NotNull LocalIndex index);
    @NotNull StringsSwitchInsn impl(@NotNull StringsSwitchImplementation impl);
    @NotNull CaseBranch branch(@NotNull String value);
    @NotNull CaseBranch branch(@NotNull String @NotNull ... values);
}
