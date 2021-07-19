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

package javabyte;

import javabyte.make.MakeClass;
import javabyte.opcode.MathOpcode;
import javabyte.type.Access;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Random;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author whilein
 */
final class Tests {

    Random random;

    @BeforeEach
    void init() {
        random = new Random();
    }

    @SneakyThrows
    private MultifunctionalInterface<?> makeInterface(
            final Consumer<MakeClass> init
    ) {
        val impl = Javabyte.make("gen.MultifunctionInterfaceImpl");

        impl.addInterface(MultifunctionalInterface.class);
        impl.setPublicFinal();

        init.accept(impl);

        impl.writeClass(new File("MultifunctionInterfaceImpl.class"));

        val classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());

        return (MultifunctionalInterface<?>) impl.load(classLoader)
                .getDeclaredConstructor().newInstance();
    }

    @Test
    void calc() {
        val x = random.nextInt(10000) - 5000;
        val y = random.nextInt(10000) - 5000;

        val result = makeInterface(impl -> {
            val method = impl.addMethod("calc");
            method.setPublic();

            method.setReturnType(int.class);
            method.setParameterTypes(int.class, int.class);

            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);
            code.loadLocal(2);
            code.callMath(MathOpcode.IADD);
            code.loadInt(100);
            code.callMath(MathOpcode.IMUL);
            code.callReturn();
        });

        assertEquals((x + y) * 100, result.calc(x, y));
    }

    @Test
    void switchCaseInts_0() {
        val result = makeInterface(impl -> {
            val method = impl.addMethod("switchCaseInts");
            method.setPublic();

            method.setReturnType(String.class);
            method.addParameter(int.class);

            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);

            val switchCase = code.intsSwitchCaseInsn();

            for (int i = 0; i < 100; i++) {
                val branch = switchCase.branch(i);
                branch.loadString(String.valueOf(i) + (char) ('A' + i));
                branch.callReturn();
            }

            val defaultBranch = switchCase.defaultBranch();
            defaultBranch.loadString("Default");
            defaultBranch.callReturn();
        });

        for (int i = 0; i < 100; i++) {
            assertEquals(String.valueOf(i) + (char) ('A' + i), result.switchCaseInts(i));
        }

        assertEquals("Default", result.switchCaseInts(100));
    }

    @Test
    void switchCaseInts_1() {
        val result = makeInterface(impl -> {
            val method = impl.addMethod("switchCaseInts");
            method.setPublic();
            method.setReturnType(String.class);
            method.addParameter(int.class);

            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);

            val switchCase = code.intsSwitchCaseInsn();

            switchCase.branch(1);
            switchCase.branch(2);
            val branch123 = switchCase.branch(3);
            branch123.loadString("123");
            branch123.callReturn();

            switchCase.branch(1000);
            switchCase.branch(100);
            val branch100010010 = switchCase.branch(10);
            branch100010010.loadString("100010010");
            branch100010010.callReturn();

            val defaultBranch = switchCase.defaultBranch();
            defaultBranch.loadString("Default");
            defaultBranch.callReturn();
        });

        assertEquals("123", result.switchCaseInts(1));
        assertEquals("123", result.switchCaseInts(2));
        assertEquals("123", result.switchCaseInts(3));
    }

    @Test
    void switchCaseInts_2() {
        val branches = new String[] {
                "AaAaAa", "AaAaBB", "AaBBAa", "AaBBBB",
                "BBAaAa", "BBAaBB", "BBBBAa", "BBBBBB",
                "A", "B", "C", "D", "E", "F"
        };

        val result = makeInterface(impl -> {
            val method = impl.addMethod("switchCaseStrings");
            method.setPublic();

            method.setReturnType(String.class);
            method.addParameter(String.class);

            method.setAccess(Access.PUBLIC);
            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);

            val switchCase = code.stringsSwitchCaseInsn();

            for (int i = 0; i < branches.length; i++) {
                val branch = switchCase.branch(branches[i]);
                branch.loadString(Integer.toString(i));
                branch.callReturn();
            }

            val defaultBranch = switchCase.defaultBranch();
            defaultBranch.loadString("Default");
            defaultBranch.callReturn();
        });

        for (int i = 0; i < branches.length; i++) {
            assertEquals(String.valueOf(i), result.switchCaseStrings(branches[i]));
        }

        assertEquals("Default", result.switchCaseStrings("123"));
    }

    @Test
    void castLongToByte() {
        val value = random.nextLong();

        val result = makeInterface(impl -> {
            val method = impl.addMethod("castLongToByte");
            method.setPublic();

            method.setReturnType(byte.class);
            method.addParameter(long.class);

            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);
            code.callCast(byte.class);
            code.callReturn();
        });

        assertEquals((byte) value, result.castLongToByte(value));
    }

    @Test
    void box() {
        val value = random.nextInt();

        val result = makeInterface(impl -> {
            val method = impl.addMethod("box");
            method.setPublic();

            method.setReturnType(Object.class);
            method.addParameter(int.class);

            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);
            code.callBox();
            code.callReturn();
        });

        assertEquals(value, result.box(value));
    }

    @Test
    void unbox() {
        val value = random.nextInt();

        val result = makeInterface(impl -> {
            val method = impl.addMethod("unbox");
            method.setPublic();

            method.setReturnType(Integer.class);
            method.addParameter(Integer.class);

            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);
            code.callUnbox();
            code.callReturn();
        });

        assertEquals(value, result.unbox(value));
    }

}
