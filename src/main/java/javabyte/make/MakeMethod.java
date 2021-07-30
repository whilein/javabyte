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

package javabyte.make;

import javabyte.signature.MethodSignature;
import javabyte.type.TypeName;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author whilein
 */
public interface MakeMethod extends MakeExecutable {

    void setReturnType(@NotNull TypeName type);

    void setReturnType(@NotNull Type type);

    void setSignature(@NotNull MethodSignature signature);

    void copySignatureFrom(
            @NotNull Class<?> type
    );

    void copySignatureFrom(
            @NotNull Method method
    );

    void setOverrides(
            @NotNull Class<?> type
    );

    void setOverrides(
            @NotNull Class<?> type,
            @NotNull Class<?> returnType,
            @NotNull Class<?> @NotNull ... parameterTypes
    );

    void setOverrides(
            @NotNull Class<?> type,
            @NotNull TypeName returnType,
            @NotNull TypeName @NotNull ... parameterTypes
    );

}
