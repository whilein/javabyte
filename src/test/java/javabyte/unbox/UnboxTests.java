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

package javabyte.unbox;

import javabyte.Javabyte;
import javabyte.TestClassLoader;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author whilein
 */
final class UnboxTests {

    String testName;

    @BeforeEach
    void setup(final TestInfo testInfo) {
        testName = testInfo.getDisplayName();
    }

    @Test
    void unboxBoolean() {
        val unbox = unbox(UnboxBoolean.class, boolean.class, Boolean.class);
        assertEquals(Boolean.TRUE, unbox.unbox(true));
        assertEquals(Boolean.FALSE, unbox.unbox(false));
    }

    @Test
    void unboxByte() {
        val unbox = unbox(UnboxByte.class, byte.class, Byte.class);

        val valueToBox = (byte) 100;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxShort() {
        val unbox = unbox(UnboxShort.class, short.class, Short.class);

        val valueToBox = (short) 100;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxChar() {
        val unbox = unbox(UnboxChar.class, char.class, Character.class);

        val valueToBox = (char) 100;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxInt() {
        val unbox = unbox(UnboxInt.class, int.class, Integer.class);

        val valueToBox = 100;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxLong() {
        val unbox = unbox(UnboxLong.class, long.class, Long.class);

        val valueToBox = 100L;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxDouble() {
        val unbox = unbox(UnboxDouble.class, double.class, Double.class);

        val valueToBox = 100D;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxFloat() {
        val unbox = unbox(UnboxFloat.class, float.class, Float.class);

        val valueToBox = 100F;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }


    @SneakyThrows
    private <T> T unbox(final Class<T> unboxInterface, final Class<?> primitive, final Class<?> wrapper) {
        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(unboxInterface);

        val method = type.addMethod("unbox");
        method.setPublic();
        method.setReturnType(primitive);
        method.addParameter(wrapper);
        method.setOverrides(unboxInterface);

        val code = method.getBytecode();
        code.loadLocal(1);
        code.callUnbox();
        code.callReturn();

        return type.load(TestClassLoader.create())
                .asSubclass(unboxInterface)
                .newInstance();
    }

}
