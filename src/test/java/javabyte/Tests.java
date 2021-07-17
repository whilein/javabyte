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
import javabyte.type.Access;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
            final Object suffix,
            final Consumer<MakeClass> init
    ) {
        val impl = Javabyte.make("gen.MultifunctionInterfaceImpl" + suffix);

        impl.addInterface(MultifunctionalInterface.class);

        impl.setAccess(Access.PUBLIC);
        impl.setFinal(true);

        init.accept(impl);

        return (MultifunctionalInterface<?>) impl.load(getClass().getClassLoader())
                .getDeclaredConstructor().newInstance();
    }

    @Test
    void castLongToByte() {
        val value = random.nextLong();

        val result = makeInterface(random.nextInt(), impl -> {
            val method = impl.addMethod("castLongToByte", byte.class);
            method.addParameter(long.class);

            method.setAccess(Access.PUBLIC);
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

        val result = makeInterface(value, impl -> {
            val method = impl.addMethod("box", Integer.class);
            method.addParameter(int.class);

            method.setAccess(Access.PUBLIC);
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

        val result = makeInterface(value, impl -> {
            val method = impl.addMethod("unbox", int.class);
            method.addParameter(Integer.class);
            method.setAccess(Access.PUBLIC);
            method.setOverrides(MultifunctionalInterface.class);

            val code = method.getBytecode();
            code.loadLocal(1);
            code.callUnbox();
            code.callReturn();
        });

        assertEquals(value, result.unbox(value));
    }

}
