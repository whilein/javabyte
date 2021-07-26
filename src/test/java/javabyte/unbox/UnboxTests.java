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
        val unbox = generate(UnboxBoolean.class);
        assertEquals(Boolean.TRUE, unbox.unbox(true));
        assertEquals(Boolean.FALSE, unbox.unbox(false));
    }

    @Test
    void unboxByte() {
        val unbox = generate(UnboxByte.class);

        val valueToBox = (byte) 100;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxShort() {
        val unbox = generate(UnboxShort.class);

        val valueToBox = (short) 100;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxChar() {
        val unbox = generate(UnboxChar.class);

        val valueToBox = (char) 100;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxInt() {
        val unbox = generate(UnboxInt.class);

        val valueToBox = 100;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxLong() {
        val unbox = generate(UnboxLong.class);

        val valueToBox = 100L;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxDouble() {
        val unbox = generate(UnboxDouble.class);

        val valueToBox = 100D;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }

    @Test
    void unboxFloat() {
        val unbox = generate(UnboxFloat.class);

        val valueToBox = 100F;
        assertEquals(valueToBox, unbox.unbox(valueToBox));
    }


    @SneakyThrows
    private <T> T generate(final Class<T> unboxInterface) {
        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(unboxInterface);

        val method = type.addMethod("unbox");
        method.setPublic();
        method.copySignatureFrom(unboxInterface);

        val code = method.getBytecode();
        code.loadLocal(1);
        code.callUnbox();
        code.callReturn();

        return type.load(TestClassLoader.create())
                .asSubclass(unboxInterface)
                .newInstance();
    }

}
