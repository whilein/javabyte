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
import javabyte.signature.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Type;

/**
 * @author whilein
 */
public interface Bytecode {

    void pushLocal(int index);
    void pushThis();

    void invokeSpecial(@NotNull Name owner, @NotNull String name, @NotNull MethodSignature descriptor);
    void invokeSpecial(@NotNull Type owner, @NotNull String name, @NotNull MethodSignature descriptor);
    void invokeSpecial(@NotNull String owner, @NotNull String name, @NotNull MethodSignature descriptor);

    void invokeVirtual(@NotNull Name owner, @NotNull String name, @NotNull MethodSignature descriptor);
    void invokeVirtual(@NotNull Type owner, @NotNull String name, @NotNull MethodSignature descriptor);
    void invokeVirtual(@NotNull String owner, @NotNull String name, @NotNull MethodSignature descriptor);

    void invokeStatic(@NotNull Name owner, @NotNull String name, @NotNull MethodSignature descriptor);
    void invokeStatic(@NotNull Type owner, @NotNull String name, @NotNull MethodSignature descriptor);
    void invokeStatic(@NotNull String owner, @NotNull String name, @NotNull MethodSignature descriptor);

    void pushString(@NotNull String string);
    void pushInt(int value);
    void pushNull();

    void callBox();
    void callUnbox();

    void callReturn();

    void compile(
            @NotNull MakeExecutable executable,
            @NotNull MethodVisitor visitor
    );

}
