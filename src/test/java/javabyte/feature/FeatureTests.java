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

package javabyte.feature;

import javabyte.Javabyte;
import javabyte.TestClassLoader;
import javabyte.make.MakeClass;
import javabyte.opcode.FieldOpcode;
import javabyte.opcode.MethodOpcode;
import javabyte.type.Types;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author whilein
 */
final class FeatureTests {

    String testName;

    @BeforeEach
    void setup(final TestInfo testInfo) {
        testName = testInfo.getDisplayName();
    }

    @Test
    @DisplayName("EqualsAndHashCode")
    void equalsAndHashCodeTest() {
        val type = generate(MakeClass::addHashCodeAndEquals);
        assertNotEquals(null, type);
    }

    @Test
    @DisplayName("ToString")
    void toStringTest() {
        val type = generate(MakeClass::addToString);
        assertEquals("ToString[A=true, B=1, C=2, D=A, E=4, F=5, G=6.0, H=7.0, X=Message, Y=[Message A, Message B, Message C], Z=[[Message A, Message B], [Message C, Message D]]]", type.toString());
    }

    @SneakyThrows
    private Object generate(final Consumer<MakeClass> initializer) {
        val type = Javabyte.make(testName);
        type.setPublicFinal();

        val constructor = type.addConstructor();
        constructor.setPublic();

        for (int i = Types.BOOL_TYPE; i <= Types.DOUBLE_TYPE; i++) {
            type.addField(String.valueOf((char) ('A' + i - 1)), Types.getPrimitive(i));
        }

        type.addField("X", Types.OBJECT);
        type.addField("Y", Types.OBJECT.dimensions(1));
        type.addField("Z", Types.OBJECT.dimensions(2));

        val code = constructor.getBytecode();
        code.loadLocal(0);
        code.methodInsn(MethodOpcode.SPECIAL, "<init>").inSuper().descriptor(void.class);

        int cnt = 0;

        for (val field : type.getFields()) {
            constructor.addParameter(field.getType());

            code.loadLocal(0);
            code.loadLocal(++cnt);
            code.fieldInsn(FieldOpcode.PUT, field.getName()).inCurrent().descriptor(field.getType());
        }

        code.callReturn();

        initializer.accept(type);

        return type.load(TestClassLoader.create())
                .getDeclaredConstructor(boolean.class, byte.class, short.class, char.class, int.class,
                        long.class, float.class, double.class, Object.class, Object[].class, Object[][].class)
                .newInstance(
                        true, (byte) 1, (short) 2, 'A', 4, 5, 6, 7, "Message",
                        new String[]{"Message A", "Message B", "Message C"},
                        new String[][]{
                                new String[]{"Message A", "Message B"},
                                new String[]{"Message C", "Message D"}
                        }
                );
    }

}
