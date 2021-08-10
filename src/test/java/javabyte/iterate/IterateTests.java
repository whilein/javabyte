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

package javabyte.iterate;

import javabyte.Javabyte;
import javabyte.TestClassLoader;
import javabyte.opcode.MethodOpcode;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author whilein
 */
final class IterateTests {

    static String[] array;
    static List<String> list;

    String testName;

    @BeforeAll
    static void setup() {
        array = "1234567890".split("");
        list = Arrays.asList(array);
    }

    @BeforeEach
    void setup(final TestInfo testInfo) {
        testName = testInfo.getDisplayName();
    }

    @Test
    @DisplayName("IterateOverList_withSource")
    void iterateOverList_withSource() {
        val iterateOverList = iterate(IterateList.class, false, false, true);
        val calls = new AtomicInteger();

        iterateOverList.iterate(list, (x, y) -> calls.incrementAndGet());

        assertEquals(list.size(), calls.get());
    }

    @Test
    @DisplayName("IterateOverList_withSourceCounter")
    void iterateOverList_withSourceCounter() {
        val iterateOverList = iterate(IterateList.class, true, false, true);
        val indexes = new ArrayList<Integer>();

        iterateOverList.iterate(list, (x, y) -> indexes.add(x));

        val expectIndexes = IntStream.range(0, array.length).boxed()
                .collect(Collectors.toList());

        assertEquals(expectIndexes, indexes);
    }

    @Test
    @DisplayName("IterateOverList_withSourceCounterLength")
    void iterateOverList_withSourceCounterLength() {
        val iterateOverList = iterate(IterateList.class, true, true, true);
        val indexes = new ArrayList<Integer>();

        iterateOverList.iterate(list, (x, y) -> {
            indexes.add(x);

            if (y != array.length) {
                fail();
            }
        });

        val expectIndexes = IntStream.range(0, array.length).boxed()
                .collect(Collectors.toList());

        assertEquals(expectIndexes, indexes);
    }

    @Test
    @DisplayName("IterateOverList")
    void iterateOverList() {
        val iterateOverList = iterate(IterateList.class, false, false, false);
        val calls = new AtomicInteger();

        iterateOverList.iterate(list, (x, y) -> calls.incrementAndGet());

        assertEquals(array.length, calls.get());
    }

    @Test
    @DisplayName("IterateOverList_withCounter")
    void iterateOverList_withCounter() {
        val iterateOverList = iterate(IterateList.class, true, false, false);
        val indexes = new ArrayList<Integer>();

        iterateOverList.iterate(list, (x, y) -> indexes.add(x));

        val expectIndexes = IntStream.range(0, array.length).boxed()
                .collect(Collectors.toList());

        assertEquals(expectIndexes, indexes);
    }

    @Test
    @DisplayName("IterateOverList_withCounterLength")
    void iterateOverList_withCounterLength() {
        val iterateOverList = iterate(IterateList.class, true, true, false);
        val indexes = new ArrayList<Integer>();

        iterateOverList.iterate(list, (x, y) -> {
            indexes.add(x);

            if (y != list.size()) {
                fail();
            }
        });

        val expectIndexes = IntStream.range(0, list.size()).boxed()
                .collect(Collectors.toList());

        assertEquals(expectIndexes, indexes);
    }
    
    @Test
    @DisplayName("IterateOverArray_withSource")
    void iterateOverArray_withSource() {
        val iterateOverArray = iterate(IterateArray.class, false, false, true);
        val calls = new AtomicInteger();

        iterateOverArray.iterate(array, (x, y) -> calls.incrementAndGet());

        assertEquals(array.length, calls.get());
    }

    @Test
    @DisplayName("IterateOverArray_withSourceCounter")
    void iterateOverArray_withSourceCounter() {
        val iterateOverArray = iterate(IterateArray.class, true, false, true);
        val indexes = new ArrayList<Integer>();

        iterateOverArray.iterate(array, (x, y) -> indexes.add(x));

        val expectIndexes = IntStream.range(0, array.length).boxed()
                .collect(Collectors.toList());

        assertEquals(expectIndexes, indexes);
    }

    @Test
    @DisplayName("IterateOverArray_withSourceCounterLength")
    void iterateOverArray_withSourceCounterLength() {
        val iterateOverArray = iterate(IterateArray.class, true, true, true);
        val indexes = new ArrayList<Integer>();

        iterateOverArray.iterate(array, (x, y) -> {
            indexes.add(x);

            if (y != array.length) {
                fail();
            }
        });

        val expectIndexes = IntStream.range(0, array.length).boxed()
                .collect(Collectors.toList());

        assertEquals(expectIndexes, indexes);
    }

    @Test
    @DisplayName("IterateOverArray")
    void iterateOverArray() {
        val iterateOverArray = iterate(IterateArray.class, false, false, false);
        val calls = new AtomicInteger();

        iterateOverArray.iterate(array, (x, y) -> calls.incrementAndGet());

        assertEquals(array.length, calls.get());
    }

    @Test
    @DisplayName("IterateOverArray_withCounter")
    void iterateOverArray_withCounter() {
        val iterateOverArray = iterate(IterateArray.class, true, false, false);
        val indexes = new ArrayList<Integer>();

        iterateOverArray.iterate(array, (x, y) -> indexes.add(x));

        val expectIndexes = IntStream.range(0, array.length).boxed()
                .collect(Collectors.toList());

        assertEquals(expectIndexes, indexes);
    }

    @Test
    @DisplayName("IterateOverArray_withCounterLength")
    void iterateOverArray_withCounterLength() {
        val iterateOverArray = iterate(IterateArray.class, true, true, false);
        val indexes = new ArrayList<Integer>();

        iterateOverArray.iterate(array, (x, y) -> {
            indexes.add(x);

            if (y != array.length) {
                fail();
            }
        });

        val expectIndexes = IntStream.range(0, array.length).boxed()
                .collect(Collectors.toList());

        assertEquals(expectIndexes, indexes);
    }

    @SneakyThrows
    private <T> T iterate(
            final Class<T> iterateInterface,
            final boolean hasCounter,
            final boolean hasLength,
            final boolean hasSource
    ) {
        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(iterateInterface);

        val method = type.addMethod("iterate");
        method.setPublic();
        method.copySignatureFrom(iterateInterface);

        val code = method.getBytecode();

        if (!hasSource) code.loadLocal(1);
        val iterate = code.iterateOverInsn().element(String.class);
        if (hasSource) iterate.source(1);

        val iterateBody = iterate.getBody();
        iterateBody.loadLocal(2);

        if (hasCounter) {
            iterateBody.loadLocal(iterate.getCounterLocal());
            iterateBody.callBox();
        } else {
            iterateBody.pushNull();
        }

        if (hasLength) {
            iterateBody.loadLocal(iterate.getLengthLocal());
            iterateBody.callBox();
        } else {
            iterateBody.pushNull();
        }

        iterateBody.methodInsn(MethodOpcode.INTERFACE, "accept")
                .descriptor(void.class, Object.class, Object.class)
                .in(BiConsumer.class);
        code.callReturn();

        return type.load(TestClassLoader.create())
                .asSubclass(iterateInterface)
                .newInstance();
    }

}
