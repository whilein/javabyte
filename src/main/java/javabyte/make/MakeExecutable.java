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

import javabyte.bytecode.Bytecode;
import javabyte.signature.MethodSignature;
import javabyte.type.ExactTypeName;
import javabyte.type.TypeName;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * @author whilein
 */
public interface MakeExecutable extends MakeClassElement {

    @NotNull String getName();

    @NotNull TypeName getReturnType();

    @NotNull Bytecode getBytecode();

    @NotNull MethodSignature getSignature();

    void addException(@NotNull Class<?> type);
    void addException(@NotNull ExactTypeName name);

    void setExceptionTypes(@NotNull Collection<@NotNull Class<?>> types);
    void setExceptionTypes(@NotNull Class<?> @NotNull ... types);

    void setExceptions(@NotNull Collection<@NotNull ExactTypeName> names);
    void setExceptions(@NotNull ExactTypeName @NotNull ... names);

    void addParameter(@NotNull TypeName name);
    void addParameter(int i, @NotNull TypeName name);

    void addParameter(@NotNull Type type);
    void addParameter(int i, @NotNull Type type);

    void addParameter(@NotNull String name);
    void addParameter(int i, @NotNull String name);

    void setParameterTypes(@NotNull Collection<@NotNull Type> types);
    void setParameterTypes(@NotNull Type @NotNull ... types);

    void setParameters(@NotNull Collection<@NotNull TypeName> names);
    void setParameters(@NotNull TypeName @NotNull ... names);

    /**
     * Return modifiable list of parameters
     * @return Parameters
     */
    @NotNull List<@NotNull TypeName> getParameters();

    /**
     * Return modifiable list of exceptions
     * @return Exceptions
     */
    @NotNull List<@NotNull ExactTypeName> getExceptions();

}
