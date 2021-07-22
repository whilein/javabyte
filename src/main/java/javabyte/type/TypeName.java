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

package javabyte.type;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * @author whilein
 */
public interface TypeName extends Parameter {

    int getDimensions();
    boolean isArray();

    @NotNull TypeName getComponent();
    @NotNull TypeName dimensions(int dimensions);

    int getPrimitive();
    boolean isPrimitive();

    @NotNull Type toType();
    @NotNull Class<?> toClass();

    @NotNull String getSimpleName();
    @NotNull String getInternalName();
    @NotNull String getName();

    void getName(@NotNull StringBuilder out);
    void getInternalName(@NotNull StringBuilder out);

    int getSize();

}
