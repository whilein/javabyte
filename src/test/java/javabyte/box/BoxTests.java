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

package javabyte.box;

import javabyte.Javabyte;
import javabyte.TestClassLoader;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author whilein
 */
final class BoxTests {

    String testName;

    @BeforeEach
    void setup(final TestInfo testInfo) {
        testName = testInfo.getDisplayName();
    }

    @Test
    @DisplayName("BoxBoolean")
    void boxBoolean() {
        val box = box(BoxBoolean.class, boolean.class, Boolean.class);
        assertEquals(Boolean.TRUE, box.box(true));
        assertEquals(Boolean.FALSE, box.box(false));
    }

    @Test
    @DisplayName("BoxByte")
    void boxByte() {
        val box = box(BoxByte.class, byte.class, Byte.class);

        val valueToBox = (byte) 100;
        assertEquals(valueToBox, box.box(valueToBox));
    }

    @Test
    @DisplayName("BoxShort")
    void boxShort() {
        val box = box(BoxShort.class, short.class, Short.class);

        val valueToBox = (short) 100;
        assertEquals(valueToBox, box.box(valueToBox));
    }

    @Test
    @DisplayName("BoxChar")
    void boxChar() {
        val box = box(BoxChar.class, char.class, Character.class);

        val valueToBox = (char) 100;
        assertEquals(valueToBox, box.box(valueToBox));
    }

    @Test
    @DisplayName("BoxInt")
    void boxInt() {
        val box = box(BoxInt.class, int.class, Integer.class);

        val valueToBox = 100;
        assertEquals(valueToBox, box.box(valueToBox));
    }

    @Test
    @DisplayName("BoxLong")
    void boxLong() {
        val box = box(BoxLong.class, long.class, Long.class);

        val valueToBox = 100L;
        assertEquals(valueToBox, box.box(valueToBox));
    }

    @Test
    @DisplayName("BoxDouble")
    void boxDouble() {
        val box = box(BoxDouble.class, double.class, Double.class);

        val valueToBox = 100D;
        assertEquals(valueToBox, box.box(valueToBox));
    }

    @Test
    @DisplayName("BoxFloat")
    void boxFloat() {
        val box = box(BoxFloat.class, float.class, Float.class);

        val valueToBox = 100F;
        assertEquals(valueToBox, box.box(valueToBox));
    }

    @SneakyThrows
    private <T> T box(final Class<T> boxInterface, final Class<?> primitive, final Class<?> wrapper) {
        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(boxInterface);

        val method = type.addMethod("box");
        method.setPublic();
        method.setReturnType(wrapper);
        method.addParameter(primitive);
        method.setOverrides(boxInterface);

        val code = method.getBytecode();
        code.loadLocal(1);
        code.callBox();
        code.callReturn();

        return type.load(TestClassLoader.create())
                .asSubclass(boxInterface)
                .newInstance();
    }

}
