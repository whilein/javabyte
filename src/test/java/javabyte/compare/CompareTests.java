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

package javabyte.compare;

import javabyte.EqualityStrategy;
import javabyte.Javabyte;
import javabyte.TestClassLoader;
import javabyte.bytecode.Asm;
import javabyte.opcode.JumpOpcode;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author whilein
 */
final class CompareTests {
    String testName;

    @BeforeEach
    void setup(final TestInfo testInfo) {
        testName = testInfo.getDisplayName();
    }

    @Test
    @DisplayName("CompareArrays")
    void compareArrays() {
        val compare = generate(CompareArrays.class, EqualityStrategy.CONTENTS);
        assertTrue(compare.compare(new String[] { "1", "2", "3" }, new String[] { "1", "2", "3" }));
        assertFalse(compare.compare(new String[] { "1", "2", "3" }, new String[] { "3", "2", "1" }));
    }

    @Test
    @DisplayName("CompareObjects_Safe")
    void compareObjects_Safe() {
        val compare = generate(CompareObjects.class, EqualityStrategy.SAFE);

        assertTrue(compare.compare(1, 1));
        assertFalse(compare.compare(1, null));
        assertFalse(compare.compare(null, 1));
    }

    @Test
    @DisplayName("CompareObjects_Ref")
    void compareObjects_Ref() {
        val compare = generate(CompareObjects.class, EqualityStrategy.REF);

        Integer a = 123123;
        Integer b = 123123;

        assertTrue(compare.compare(a, a));
        assertFalse(compare.compare(a, b));
    }

    @Test
    @DisplayName("compareObjects_Default")
    void compareObjects_Default() {
        val compare = generate(CompareObjects.class, EqualityStrategy.DEFAULT);
        assertTrue(compare.compare("ABC", "ABC"));
        assertFalse(compare.compare("ABC", "АБВ"));

        try {
            assertFalse(compare.compare("ABC", null));
            assertFalse(compare.compare(null, "АБВ"));

            fail();
        } catch (NullPointerException ignored) {} // null.equals("") throws NPE
    }

    @Test
    @DisplayName("CompareDeepArrays_Default")
    void compareDeepArrays_Default() {
        val compare = generate(CompareDeepArrays.class, EqualityStrategy.CONTENTS);

        assertTrue(compare.compare(
                new String[][] { new String[] { "1", "2", "3" } },
                new String[][] { new String[] { "1", "2", "3" } }
        ));

        assertFalse(compare.compare(
                new String[][] { new String[] { "1", "2", "3" } },
                new String[][] { new String[] { "3", "2", "1" } }
        ));
    }

    @Test
    @DisplayName("CompareByteWithInt_Default")
    void compareByteWithInt_Default() {
        val compare = generate(CompareByteWithInt.class, EqualityStrategy.DEFAULT);
        assertTrue(compare.compare((byte) 1, 1));
        assertFalse(compare.compare((byte) 0, 1));
    }

    @Test
    @DisplayName("CompareInts_Default")
    void compareInts_Default() {
        val compare = generate(CompareInts.class, EqualityStrategy.DEFAULT);
        assertTrue(compare.compare(1, 1));
        assertFalse(compare.compare(0, 1));
    }

    @SneakyThrows
    private <T> T generate(final Class<T> compareInterface, final EqualityStrategy strategy) {
        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(compareInterface);

        val method = type.addMethod("compare");
        method.setPublic();
        method.copySignatureFrom(compareInterface);

        val code = method.getBytecode();
        code.loadLocal(1);
        code.loadLocal(2);

        val position = Asm.position();
        val endPosition = Asm.position();
        code.jumpIfNotEquals(strategy, position);
        code.pushInt(1);
        code.jump(JumpOpcode.GOTO, endPosition);
        code.visit(position);
        code.pushInt(0);
        code.visit(endPosition);
        code.callReturn();

        return type.load(TestClassLoader.create())
                .asSubclass(compareInterface)
                .newInstance();
    }

}
