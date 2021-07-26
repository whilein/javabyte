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

package javabyte.contains;

import javabyte.Javabyte;
import javabyte.TestClassLoader;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author whilein
 */
final class ContainsTests {

    String testName;

    @BeforeEach
    void setup(final TestInfo testInfo) {
        testName = testInfo.getDisplayName();
    }

    @Test
    @DisplayName("ContainsInArray")
    void containsInArray() {
        val contains = contains(ContainsInArray.class, String[].class, String.class);

        val array = "ABCDEF1234567890".split("");

        for (val element : array) {
            assertTrue(contains.contains(array, element));
        }

        assertFalse(contains.contains(array, ""));
    }

    @Test
    @DisplayName("ContainsInByteArray")
    void containsInByteArray() {
        val contains = contains(ContainsInByteArray.class, byte[].class, byte.class);

        val array = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

        for (val element : array) {
            assertTrue(contains.contains(array, element));
        }

        assertFalse(contains.contains(array, (byte) 0));
    }

    @Test
    @DisplayName("ContainsInShortArray")
    void containsInShortArray() {
        val contains = contains(ContainsInShortArray.class, short[].class, short.class);

        val array = new short[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

        for (val element : array) {
            assertTrue(contains.contains(array, element));
        }

        assertFalse(contains.contains(array, (short) 0));
    }

    @Test
    @DisplayName("ContainsInIntArray")
    void containsInIntArray() {
        val contains = contains(ContainsInIntArray.class, int[].class, int.class);

        val array = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

        for (val element : array) {
            assertTrue(contains.contains(array, element));
        }

        assertFalse(contains.contains(array, 0));
    }

    @Test
    @DisplayName("ContainsInLongArray")
    void containsInLongArray() {
        val contains = contains(ContainsInLongArray.class, long[].class, long.class);

        val array = new long[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

        for (val element : array) {
            assertTrue(contains.contains(array, element));
        }

        assertFalse(contains.contains(array, 0L));
    }

    @Test
    @DisplayName("ContainsInFloatArray")
    void containsInFloatArray() {
        val contains = contains(ContainsInFloatArray.class, float[].class, float.class);

        val array = new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

        for (val element : array) {
            assertTrue(contains.contains(array, element));
        }

        assertFalse(contains.contains(array, 0f));
    }

    @Test
    @DisplayName("ContainsInDoubleArray")
    void containsInDoubleArray() {
        val contains = contains(ContainsInDoubleArray.class, double[].class, double.class);

        val array = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

        for (val element : array) {
            assertTrue(contains.contains(array, element));
        }

        assertFalse(contains.contains(array, 0d));
    }

    @Test
    @DisplayName("ContainsInList")
    void containsInList() {
        val contains = contains(ContainsInList.class, List.class, String.class);

        val array = Arrays.asList("ABCDEF1234567890".split(""));

        for (val element : array) {
            assertTrue(contains.contains(array, element));
        }

        assertFalse(contains.contains(array, ""));
    }

    @SneakyThrows
    private <T> T contains(final Class<T> containsInterface, final Class<?> iterable, final Class<?> element) {
        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(containsInterface);

        val method = type.addMethod("contains");
        method.setPublic();

        method.setReturnType(boolean.class);
        method.setParameterTypes(iterable, element);

        method.setOverrides(containsInterface);

        val code = method.getBytecode();
        code.pushInt(0);

        val index = code.storeLocal();

        val loop = code.iterateOverInsn()
                .element(element)
                .source(1);

        val loopCode = loop.getBody();
        loopCode.loadLocal(loop.getElementLocal());
        loopCode.loadLocal(2);

        loopCode.jumpIfNotEquals(loopCode.getContinue());
        loopCode.pushInt(1);
        loopCode.storeLocal(index);
        loopCode.callBreak();

        code.pushInt(0);
        code.loadLocal(index);
        code.callReturn();

        return type.load(TestClassLoader.create())
                .asSubclass(containsInterface)
                .newInstance();
    }

}
