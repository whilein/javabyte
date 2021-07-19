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

import javabyte.type.Access;
import org.jetbrains.annotations.NotNull;

/**
 * @author whilein
 */
public interface MakeElement {

    int getModifiers();
    void setModifiers(int modifiers);

    void setAccess(@NotNull Access access);
    @NotNull Access getAccess();

    void setFinal(boolean flag);
    void setStatic(boolean flag);

    void setPublic();
    void setPublicFinal();
    void setPrivate();
    void setPrivateFinal();

    boolean isFinal();
    boolean isStatic();

}
