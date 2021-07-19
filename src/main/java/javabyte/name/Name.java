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

package javabyte.name;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * @author whilein
 */
public interface Name {

    int getDimensions();
    boolean isArray();

    int getPrimitive();
    boolean isPrimitive();

    @NotNull Type getType();

    @NotNull String getInternalName();
    @NotNull String getSignature();
    @NotNull String getDescriptor();
    @NotNull String getName();

    void getName(@NotNull StringBuilder out);
    void getInternalName(@NotNull StringBuilder out);
    void getSignature(@NotNull StringBuilder out);
    void getDescriptor(@NotNull StringBuilder out);
    void toString(@NotNull StringBuilder out);

    boolean hasParameterizedTypes();
    int getSize();

}
