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

import javabyte.type.TypeName;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * @author whilein
 */
public interface FieldInsn extends Instruction {

    @NotNull FieldInsn descriptor(@NotNull Type type);
    @NotNull FieldInsn descriptor(@NotNull TypeName type);

    @NotNull FieldInsn in(@NotNull Type owner);
    @NotNull FieldInsn in(@NotNull TypeName owner);

    @NotNull FieldInsn inCurrent();

}
