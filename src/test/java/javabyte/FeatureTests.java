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
import javabyte.make.MakeMethod;
import javabyte.opcode.JumpOpcode;
import javabyte.opcode.MathOpcode;
import javabyte.opcode.MethodOpcode;
import javabyte.opcode.StringsSwitchImplementation;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author whilein
 */
final class FeatureTests {

    Random random;
    char[] alphabet;

    @BeforeEach
    void init() {
        random = new Random();
        alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    }

    private int[] randomInts(final int max, final int min, final int count) {
        val ints = new int[]{count};

        for (int i = 0; i < ints.length; i++) {
            ints[i] = random.nextInt(max - min) + min;
        }

        return ints;
    }

    private String randomText(final int length) {
        val chars = new char[length];

        for (int i = 0; i < length; i++)
            chars[i] = alphabet[random.nextInt(alphabet.length)];

        return new String(chars);
    }

    private String[] randomTexts(final int textLength, final int texts) {
        val randomTexts = new String[texts];

        for (int i = 0; i < texts; i++)
            randomTexts[i] = randomText(textLength);

        return randomTexts;
    }

    @SneakyThrows
    private MultifunctionalInterface<?> makeInterface(
            final String testName,
            final Consumer<MakeClass> init
    ) {
        val impl = Javabyte.make("gen.Test_" + testName);

        impl.addInterface(MultifunctionalInterface.class);
        impl.setPublicFinal();

        init.accept(impl);

        if ("true".equals(System.getenv("WRITE_CLASSES"))) {
            val out = Optional.ofNullable(System.getenv("WRITE_LOCATION"))
                    .orElse("out");
            val outDir = new File(out);

            if (outDir.isDirectory() || outDir.mkdirs()) {
                impl.writeTo(outDir);
            }
        }
        val classLoader = new URLClassLoader(new URL[0], getClass().getClassLoader());

        return (MultifunctionalInterface<?>) impl.load(classLoader)
                .getDeclaredConstructor().newInstance();
    }


    private void initSearchCode(final MakeMethod method) {
        val code = method.getBytecode();
        code.pushInt(0);

        val index = code.storeLocal();

        val loop = code.iterateOverInsn()
                .element(String.class)
                .source(1);

        val loopCode = loop.getBody();
        loopCode.loadLocal(loop.getElementLocal());
        loopCode.loadLocal(2);

        loopCode.methodInsn(MethodOpcode.VIRTUAL, "equals")
                .descriptor(boolean.class, Object.class)
                .in(String.class);

        loopCode.jump(JumpOpcode.IFEQ, loopCode.getContinue());
        loopCode.pushInt(1);
        loopCode.storeLocal(index);
        loopCode.callBreak();

        code.loadLocal(index);
        code.callReturn();
    }

    @Test
    void searchInList() {
        val strings = Arrays.asList(randomTexts(256, 100));

        val result = makeInterface("searchInList", impl -> {
            val method = impl.addMethod("searchInList");
            method.setPublic();

            method.setReturnType(boolean.class);
            method.setParameterTypes(List.class, String.class);

            method.setOverrides(MultifunctionalInterface.class);

            initSearchCode(method);
        });

        for (val text : strings) {
            assertTrue(result.searchInList(strings, text));
        }

        for (val text : randomTexts(256, 100)) {
            if (strings.contains(text)) continue;
            assertFalse(result.searchInList(strings, text));
        }
    }

    @Test
    void searchInIntArray() {
        val ints = randomInts(10000, -10000, 100);

        val intList = IntStream.of(ints).boxed()
                .collect(Collectors.toList());

        val result = makeInterface("searchInIntArray", impl -> {
            val method = impl.addMethod("searchInIntArray");
            method.setPublic();

            method.setReturnType(boolean.class);
            method.setParameterTypes(int[].class, int.class);

            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.pushInt(0);

            val index = code.storeLocal();

            val loop = code.iterateOverInsn()
                    .element(int.class)
                    .source(1);

            val loopCode = loop.getBody();
            loopCode.loadLocal(loop.getElementLocal());
            loopCode.loadLocal(2);

            loopCode.jump(JumpOpcode.IF_ICMPNE, loopCode.getContinue());
            loopCode.pushInt(1);
            loopCode.storeLocal(index);
            loopCode.callBreak();

            code.loadLocal(index);
            code.callReturn();
        });

        for (val i : ints) {
            assertTrue(result.searchInIntArray(ints, i));
        }

        for (val i : ints) {
            if (intList.contains(i)) continue;
            assertFalse(result.searchInIntArray(ints, i));
        }
    }

    @Test
    void searchInArray() {
        val strings = randomTexts(256, 100);
        val stringList = Arrays.asList(strings);

        val result = makeInterface("searchInArray", impl -> {
            val method = impl.addMethod("searchInArray");
            method.setPublic();

            method.setReturnType(boolean.class);
            method.setParameterTypes(String[].class, String.class);

            method.setOverrides(MultifunctionalInterface.class);

            initSearchCode(method);
        });

        for (val text : strings) {
            assertTrue(result.searchInArray(strings, text));
        }

        for (val text : randomTexts(256, 100)) {
            if (stringList.contains(text)) continue;
            assertFalse(result.searchInArray(strings, text));
        }
    }

    @Test
    void calc() {
        val x = random.nextInt(10000) - 5000;
        val y = random.nextInt(10000) - 5000;

        val result = makeInterface("calc", impl -> {
            val method = impl.addMethod("calc");
            method.setPublic();

            method.setReturnType(int.class);
            method.setParameterTypes(int.class, int.class);

            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);
            code.loadLocal(2);
            code.callMath(MathOpcode.IADD);
            code.pushInt(100);
            code.callMath(MathOpcode.IMUL);
            code.callReturn();
        });

        assertEquals((x + y) * 100, result.calc(x, y));
    }

    @Test
    void switchCaseInts_0() {
        val result = makeInterface("switchCaseInts_0", impl -> {
            val method = impl.addMethod("switchCaseInts");
            method.setPublic();

            method.setReturnType(String.class);
            method.addParameter(int.class);

            method.setOverrides(MultifunctionalInterface.class);

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
        });

        for (int i = 0; i < 100; i++) {
            assertEquals(String.valueOf(i) + (char) ('A' + i), result.switchCaseInts(i));
        }

        assertEquals("Default", result.switchCaseInts(100));
    }

    @Test
    void switchCaseInts_1() {
        val result = makeInterface("switchCaseInts_1", impl -> {
            val method = impl.addMethod("switchCaseInts");
            method.setPublic();
            method.setReturnType(String.class);
            method.addParameter(int.class);

            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);

            val switchCase = code.intsSwitchCaseInsn();

            val branch123 = switchCase.branch(1, 2, 3);
            branch123.pushString("123");
            branch123.callReturn();

            val branch100010010 = switchCase.branch(1000, 100, 10);
            branch100010010.pushString("100010010");
            branch100010010.callReturn();

            val defaultBranch = switchCase.defaultBranch();
            defaultBranch.pushString("Default");
            defaultBranch.callReturn();
        });

        assertEquals("123", result.switchCaseInts(1));
        assertEquals("123", result.switchCaseInts(2));
        assertEquals("123", result.switchCaseInts(3));
    }

    @Test
    void switchCase_2() {
        val randomBranches = randomTexts(100, 100);
        val randomBranchList = Arrays.asList(randomBranches);

        val sameHashBranches = new String[]{
                "AaAaAa", "AaAaBB", "AaBBAa", "AaBBBB",
                "BBAaAa", "BBAaBB", "BBBBAa", "BBBBBB"
        };

        val result = makeInterface("switchCase_2", impl -> {
            val method = impl.addMethod("switchCaseStrings");
            method.setPublic();

            method.setReturnType(String.class);
            method.addParameter(String.class);

            method.setAccess(Access.PUBLIC);
            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();

            val switchCase = code.stringsSwitchCaseInsn()
                    .impl(StringsSwitchImplementation.JAVAC)
                    .source(1);

            for (int i = 0; i < randomBranches.length; i++) {
                val branch = switchCase.branch(randomBranches[i]);
                branch.pushString(Integer.toString(i));
                branch.callReturn();
            }

            for (int i = 0; i < sameHashBranches.length; i++) {
                val sameHashBranch = sameHashBranches[i];
                if (randomBranchList.contains(sameHashBranch)) continue;

                val branch = switchCase.branch(sameHashBranch);
                branch.pushString("SameHash#" + i);
                branch.callReturn();
            }

            val defaultBranch = switchCase.defaultBranch();
            defaultBranch.pushString("Default");
            defaultBranch.callReturn();
        });

        for (int i = 0; i < randomBranches.length; i++) {
            assertEquals(String.valueOf(i), result.switchCaseStrings(randomBranches[i]));
        }

        for (int i = 0; i < sameHashBranches.length; i++) {
            val sameHashBranch = sameHashBranches[i];
            if (randomBranchList.contains(sameHashBranch)) continue;

            assertEquals("SameHash#" + i, result.switchCaseStrings(sameHashBranch));
        }

        assertEquals("Default", result.switchCaseStrings("123"));
    }

    @Test
    void castLongToByte() {
        val value = random.nextLong();

        val result = makeInterface("castLongToByte", impl -> {
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

        val result = makeInterface("box", impl -> {
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

        val result = makeInterface("unbox", impl -> {
            val method = impl.addMethod("unbox");
            method.setPublic();

            method.setReturnType(int.class);
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
