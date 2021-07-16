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
import javabyte.type.Field;
import javabyte.type.Invoke;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Type;

/**
 * @author whilein
 */
public interface Bytecode {

    void pushLocal(int index);

    void pushThis();

    void invoke(@NotNull Invoke action, @NotNull Name owner,
                @NotNull String name, @NotNull MethodSignature descriptor);

    void invoke(@NotNull Invoke action, @NotNull Type owner,
                @NotNull String name, @NotNull MethodSignature descriptor);

    void invokeOwn(@NotNull Invoke action, @NotNull String name,
                   @NotNull MethodSignature descriptor);

    void invokeSuper(@NotNull Invoke action, @NotNull String name,
                     @NotNull MethodSignature descriptor);

    void fieldOwn(@NotNull Field action, @NotNull String name, @NotNull Name type);

    void fieldOwn(@NotNull Field action, @NotNull String name, @NotNull Type type);

    void field(@NotNull Field action, @NotNull Name owner, @NotNull String name, @NotNull Name type);

    void field(@NotNull Field action, @NotNull Name owner, @NotNull String name, @NotNull Type type);

    void field(@NotNull Field action, @NotNull Type owner, @NotNull String name, @NotNull Name type);

    void field(@NotNull Field action, @NotNull Type owner, @NotNull String name, @NotNull Type type);

    // TODO void fieldOwn(@NotNull String name);
    // TODO void fieldOwn(@NotNull String name);

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
