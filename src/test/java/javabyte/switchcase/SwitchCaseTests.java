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

package javabyte.switchcase;

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
final class SwitchCaseTests {

    String testName;

    @BeforeEach
    void setup(final TestInfo testInfo) {
        testName = testInfo.getDisplayName();
    }

    @Test
    @DisplayName("SwitchCaseStrings")
    @SneakyThrows
    void switchCaseStrings() {
        val branches = new String[]{
                "A", "B", "C", "D", "E", "F",
                "AaAaAa", "AaAaBB", "AaBBAa", "AaBBBB",
                "BBAaAa", "BBAaBB", "BBBBAa", "BBBBBB"
        };

        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(SwitchCaseStrings.class);

        val method = type.addMethod("switchValue");
        method.setPublic();

        method.setReturnType(String.class);
        method.addParameter(String.class);

        method.setOverrides(SwitchCaseStrings.class);

        val code = method.getBytecode();

        val switchCase = code.stringsSwitchCaseInsn()
                .source(1);

        for (int i = 0; i < branches.length; i++) {
            val branch = switchCase.branch(branches[i]);
            branch.pushString(Integer.toString(i));
            branch.callReturn();
        }

        val defaultBranch = switchCase.defaultBranch();
        defaultBranch.pushString("Default");
        defaultBranch.callReturn();

        val instance = type.load(TestClassLoader.create())
                .asSubclass(SwitchCaseStrings.class)
                .newInstance();

        for (int i = 0; i < branches.length; i++) {
            assertEquals(Integer.toString(i), instance.switchValue(branches[i]));
        }

        assertEquals("Default", instance.switchValue("123"));
    }


    @Test
    @DisplayName("SwitchCaseInts")
    @SneakyThrows
    void switchCaseInts() {
        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(SwitchCaseInts.class);

        val method = type.addMethod("switchValue");
        method.setPublic();

        method.setReturnType(String.class);
        method.addParameter(int.class);

        method.setOverrides(SwitchCaseInts.class);

        val code = method.getBytecode();

        val switchCase = code.intsSwitchCaseInsn()
                .source(1);

        for (int i = 0; i < 100; i++) {
            val branch = switchCase.branch(i);
            branch.pushString(String.valueOf(i) + (char) ('A' + i));
            branch.callReturn();
        }

        val defaultBranch = switchCase.defaultBranch();
        defaultBranch.pushString("Default");
        defaultBranch.callReturn();

        val instance = type.load(TestClassLoader.create())
                .asSubclass(SwitchCaseInts.class)
                .newInstance();

        for (int i = 0; i < 100; i++) {
            assertEquals(String.valueOf(i) + (char) ('A' + i), instance.switchValue(i));
        }

        assertEquals("Default", instance.switchValue(100));
    }

}
