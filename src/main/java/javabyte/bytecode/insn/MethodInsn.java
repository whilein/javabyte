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

import javabyte.name.Name;
import javabyte.signature.MethodSignature;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

/**
 * @author whilein
 */
public interface MethodInsn {

    @NotNull MethodInsn descriptor(@NotNull MethodSignature signature);
    @NotNull MethodInsn descriptor(@NotNull Type returnType, @NotNull Type @NotNull ... parameters);
    @NotNull MethodInsn descriptor(@NotNull Name returnType, @NotNull Name @NotNull ... parameters);

    @NotNull MethodInsn in(@NotNull Type owner);
    @NotNull MethodInsn in(@NotNull Name owner);

    @NotNull MethodInsn inCurrent();
    @NotNull MethodInsn inSuper();

}
