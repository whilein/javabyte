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

package javabyte.cast;

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
final class CastTests {

    String testName;

    @BeforeEach
    void setup(final TestInfo testInfo) {
        testName = testInfo.getDisplayName();
    }

    @Test
    @DisplayName("CastFloatToByte")
    void castFloatToByte() {
        val cast = generate(CastFloatToByte.class);
        val value = 100F;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastFloatToShort")
    void castFloatToShort() {
        val cast = generate(CastFloatToShort.class);
        val value = 100F;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastFloatToInt")
    void castFloatToInt() {
        val cast = generate(CastFloatToInt.class);
        val value = 100F;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastFloatToLong")
    void castFloatToLong() {
        val cast = generate(CastFloatToLong.class);
        val value = 100F;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastFloatToDouble")
    void castFloatToDouble() {
        val cast = generate(CastFloatToDouble.class);
        val value = 100F;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToByte")
    void castDoubleToByte() {
        val cast = generate(CastDoubleToByte.class);
        val value = 100D;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToShort")
    void castDoubleToShort() {
        val cast = generate(CastDoubleToShort.class);
        val value = 100D;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToInt")
    void castDoubleToInt() {
        val cast = generate(CastDoubleToInt.class);
        val value = 100D;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToLong")
    void castDoubleToLong() {
        val cast = generate(CastDoubleToLong.class);
        val value = 100D;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToFloat")
    void castDoubleToFloat() {
        val cast = generate(CastDoubleToFloat.class);
        val value = 100D;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToByte")
    void castLongToByte() {
        val cast = generate(CastLongToByte.class);
        val value = 100L;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToShort")
    void castLongToShort() {
        val cast = generate(CastLongToShort.class);
        val value = 100L;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToInt")
    void castLongToInt() {
        val cast = generate(CastLongToInt.class);
        val value = 100L;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToFloat")
    void castLongToFloat() {
        val cast = generate(CastLongToFloat.class);
        val value = 100L;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToDouble")
    void castLongToDouble() {
        val cast = generate(CastLongToDouble.class);
        val value = 100L;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }
    
    @Test
    @DisplayName("CastIntToByte")
    void castIntToByte() {
        val cast = generate(CastIntToByte.class);
        val value = 100;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastIntToShort")
    void castIntToShort() {
        val cast = generate(CastIntToShort.class);
        val value = 100;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastIntToLong")
    void castIntToLong() {
        val cast = generate(CastIntToLong.class);
        val value = 100;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastIntToFloat")
    void castIntToFloat() {
        val cast = generate(CastIntToFloat.class);
        val value = 100;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastIntToDouble")
    void castIntToDouble() {
        val cast = generate(CastIntToDouble.class);
        val value = 100;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }
    
    @Test
    @DisplayName("CastShortToByte")
    void castShortToByte() {
        val cast = generate(CastShortToByte.class);
        val value = (short) 100;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastShortToInt")
    void castShortToInt() {
        val cast = generate(CastShortToInt.class);
        val value = (short) 100;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastShortToLong")
    void castShortToLong() {
        val cast = generate(CastShortToLong.class);
        val value = (short) 100;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastShortToFloat")
    void castShortToFloat() {
        val cast = generate(CastShortToFloat.class);
        val value = (short) 100;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastShortToDouble")
    void castShortToDouble() {
        val cast = generate(CastShortToDouble.class);
        val value = (short) 100;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }
    
    @Test
    @DisplayName("CastByteToShort")
    void castByteToShort() {
        val cast = generate(CastByteToShort.class);
        val value = (byte) 100;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastByteToInt")
    void castByteToInt() {
        val cast = generate(CastByteToInt.class);
        val value = (byte) 100;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastByteToLong")
    void castByteToLong() {
        val cast = generate(CastByteToLong.class);
        val value = (byte) 100;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastByteToFloat")
    void castByteToFloat() {
        val cast = generate(CastByteToFloat.class);
        val value = (byte) 100;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastByteToDouble")
    void castByteToDouble() {
        val cast = generate(CastByteToDouble.class);
        val value = (byte) 100;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }

    @SneakyThrows
    private <T> T generate(final Class<T> castInterface) {
        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(castInterface);

        val method = type.addMethod("cast");
        method.setPublic();
        method.copySignatureFrom(castInterface);

        val code = method.getBytecode();
        code.loadLocal(1);
        code.callCast(method.getReturnType());
        code.callReturn();

        return type.load(TestClassLoader.create())
                .asSubclass(castInterface)
                .newInstance();
    }

}
