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

import javabyte.name.Names;
import javabyte.signature.Signatures;
import javabyte.type.Access;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

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

    @Test
    @SneakyThrows
    void box() {
        val value = random.nextInt();

        val example = Javabyte.make("generated.Example");

        example.addInterface(Names.exact(Supplier.class).parameterized(Integer.class));

        example.setAccess(Access.PUBLIC);
        example.setFinal(true);

        val getMethod = example.addMethod("get", Integer.class);
        getMethod.setAccess(Access.PUBLIC);
        getMethod.setOverrides(Supplier.class, "get");

        val getCode = getMethod.getBytecode();
        getCode.pushInt(value);
        getCode.callBox();
        getCode.callReturn();

        val exampleType = example.load(getClass().getClassLoader());
        val exampleInstance = (Supplier<?>) exampleType.getConstructor().newInstance();

        assertEquals(value, exampleInstance.get());
    }

    @Test
    @SneakyThrows
    void unbox() {
        val value = random.nextInt();

        val example = Javabyte.make("generated.Example");

        example.addInterface(IntSupplier.class);

        example.setAccess(Access.PUBLIC);
        example.setFinal(true);

        val getMethod = example.addMethod("getAsInt", int.class);
        getMethod.setAccess(Access.PUBLIC);
        getMethod.setOverrides(IntSupplier.class, "getAsInt");

        val getCode = getMethod.getBytecode();
        getCode.pushString(String.valueOf(value));
        getCode.invokeStatic(Integer.class, "valueOf", Signatures.methodSignature(Integer.class, String.class));
        getCode.callUnbox();
        getCode.callReturn();

        val exampleType = example.load(getClass().getClassLoader());
        val exampleInstance = (IntSupplier) exampleType.getConstructor().newInstance();

        assertEquals(value, exampleInstance.getAsInt());
    }


}
