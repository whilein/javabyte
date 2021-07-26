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
        val cast = cast(CastFloatToByte.class, float.class, byte.class);
        val value = 100F;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastFloatToShort")
    void castFloatToShort() {
        val cast = cast(CastFloatToShort.class, float.class, short.class);
        val value = 100F;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastFloatToInt")
    void castFloatToInt() {
        val cast = cast(CastFloatToInt.class, float.class, int.class);
        val value = 100F;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastFloatToLong")
    void castFloatToLong() {
        val cast = cast(CastFloatToLong.class, float.class, long.class);
        val value = 100F;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastFloatToDouble")
    void castFloatToDouble() {
        val cast = cast(CastFloatToDouble.class, float.class, double.class);
        val value = 100F;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToByte")
    void castDoubleToByte() {
        val cast = cast(CastDoubleToByte.class, double.class, byte.class);
        val value = 100D;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToShort")
    void castDoubleToShort() {
        val cast = cast(CastDoubleToShort.class, double.class, short.class);
        val value = 100D;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToInt")
    void castDoubleToInt() {
        val cast = cast(CastDoubleToInt.class, double.class, int.class);
        val value = 100D;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToLong")
    void castDoubleToLong() {
        val cast = cast(CastDoubleToLong.class, double.class, long.class);
        val value = 100D;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastDoubleToFloat")
    void castDoubleToFloat() {
        val cast = cast(CastDoubleToFloat.class, double.class, float.class);
        val value = 100D;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToByte")
    void castLongToByte() {
        val cast = cast(CastLongToByte.class, long.class, byte.class);
        val value = 100L;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToShort")
    void castLongToShort() {
        val cast = cast(CastLongToShort.class, long.class, short.class);
        val value = 100L;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToInt")
    void castLongToInt() {
        val cast = cast(CastLongToInt.class, long.class, int.class);
        val value = 100L;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToFloat")
    void castLongToFloat() {
        val cast = cast(CastLongToFloat.class, long.class, float.class);
        val value = 100L;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastLongToDouble")
    void castLongToDouble() {
        val cast = cast(CastLongToDouble.class, long.class, double.class);
        val value = 100L;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }
    
    @Test
    @DisplayName("CastIntToByte")
    void castIntToByte() {
        val cast = cast(CastIntToByte.class, int.class, byte.class);
        val value = 100;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastIntToShort")
    void castIntToShort() {
        val cast = cast(CastIntToShort.class, int.class, short.class);
        val value = 100;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastIntToLong")
    void castIntToLong() {
        val cast = cast(CastIntToLong.class, int.class, long.class);
        val value = 100;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastIntToFloat")
    void castIntToFloat() {
        val cast = cast(CastIntToFloat.class, int.class, float.class);
        val value = 100;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastIntToDouble")
    void castIntToDouble() {
        val cast = cast(CastIntToDouble.class, int.class, double.class);
        val value = 100;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }
    
    @Test
    @DisplayName("CastShortToByte")
    void castShortToByte() {
        val cast = cast(CastShortToByte.class, short.class, byte.class);
        val value = (short) 100;
        val expect = (byte) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastShortToInt")
    void castShortToInt() {
        val cast = cast(CastShortToInt.class, short.class, int.class);
        val value = (short) 100;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastShortToLong")
    void castShortToLong() {
        val cast = cast(CastShortToLong.class, short.class, long.class);
        val value = (short) 100;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastShortToFloat")
    void castShortToFloat() {
        val cast = cast(CastShortToFloat.class, short.class, float.class);
        val value = (short) 100;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastShortToDouble")
    void castShortToDouble() {
        val cast = cast(CastShortToDouble.class, short.class, double.class);
        val value = (short) 100;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }
    
    @Test
    @DisplayName("CastByteToShort")
    void castByteToShort() {
        val cast = cast(CastByteToShort.class, byte.class, short.class);
        val value = (byte) 100;
        val expect = (short) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastByteToInt")
    void castByteToInt() {
        val cast = cast(CastByteToInt.class, byte.class, int.class);
        val value = (byte) 100;
        val expect = (int) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastByteToLong")
    void castByteToLong() {
        val cast = cast(CastByteToLong.class, byte.class, long.class);
        val value = (byte) 100;
        val expect = (long) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastByteToFloat")
    void castByteToFloat() {
        val cast = cast(CastByteToFloat.class, byte.class, float.class);
        val value = (byte) 100;
        val expect = (float) value;

        assertEquals(expect, cast.cast(value));
    }

    @Test
    @DisplayName("CastByteToDouble")
    void castByteToDouble() {
        val cast = cast(CastByteToDouble.class, byte.class, double.class);
        val value = (byte) 100;
        val expect = (double) value;

        assertEquals(expect, cast.cast(value));
    }

    @SneakyThrows
    private <T> T cast(final Class<T> castInterface, final Class<?> from, final Class<?> to) {
        val type = Javabyte.make(testName);
        type.setPublicFinal();
        type.addInterface(castInterface);

        val method = type.addMethod("cast");
        method.setPublic();
        method.setReturnType(to);
        method.addParameter(from);
        method.setOverrides(castInterface);

        val code = method.getBytecode();
        code.loadLocal(1);
        code.callCast(to);
        code.callReturn();

        return type.load(TestClassLoader.create())
                .asSubclass(castInterface)
                .newInstance();
    }

}
