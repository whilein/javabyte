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

package javabyte.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * @author whilein
 */
@UtilityClass
public class AsmUtils {

    public @NotNull Type getType(final @NotNull String @NonNull [] name) {
        if (name.length == 1) {
            switch (name[0]) {
                case "void": return Type.VOID_TYPE;
                case "boolean": return Type.BOOLEAN_TYPE;
                case "byte": return Type.BYTE_TYPE;
                case "char": return Type.CHAR_TYPE;
                case "short": return Type.SHORT_TYPE;
                case "int": return Type.INT_TYPE;
                case "long": return Type.LONG_TYPE;
                case "float": return Type.FLOAT_TYPE;
                case "double": return Type.DOUBLE_TYPE;
            }
        }

        return Type.getObjectType(String.join("/", name));
    }

}
