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
import javabyte.name.ExactName;
import javabyte.name.Name;
import javabyte.signature.MethodSignature;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

/**
 * @author whilein
 */
public interface MakeExecutable extends MakeClassElement {

    @NotNull String getName();

    @NotNull Name getReturnType();

    @NotNull Bytecode getBytecode();

    @NotNull MethodSignature getSignature();

    void addException(@NotNull Class<?> type);
    void addException(@NotNull ExactName name);
    void addException(@NotNull String name);

    void setExceptionTypes(@NotNull Collection<@NotNull Class<?>> types);
    void setExceptionTypes(@NotNull Class<?> @NotNull ... types);

    void setExceptionNames(@NotNull Collection<@NotNull String> names);
    void setExceptionNames(@NotNull String @NotNull ... names);

    void setExceptions(@NotNull Collection<@NotNull ExactName> names);
    void setExceptions(@NotNull ExactName @NotNull ... names);

    void addParameter(@NotNull Name name);
    void addParameter(int i, @NotNull Name name);

    void addParameter(@NotNull Type type);
    void addParameter(int i, @NotNull Type type);

    void addParameter(@NotNull String name);
    void addParameter(int i, @NotNull String name);

    void setParameterTypes(@NotNull Collection<@NotNull Type> types);
    void setParameterTypes(@NotNull Type @NotNull ... types);

    void setParameterNames(@NotNull Collection<@NotNull String> names);
    void setParameterNames(@NotNull String @NotNull ... names);

    void setParameters(@NotNull Collection<@NotNull Name> names);
    void setParameters(@NotNull Name @NotNull ... names);

    /**
     * Return modifiable list of parameters
     * @return Parameters
     */
    @NotNull List<@NotNull Name> getParameters();

    /**
     * Return modifiable list of exceptions
     * @return Exceptions
     */
    @NotNull List<@NotNull ExactName> getExceptions();

}
