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

package javabyte.type;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author whilein
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
class TypesTests {

    static ExactTypeName intWrapperClass;
    static ExactTypeName floatWrapperClass;

    static ExactTypeName intClass;
    static ExactTypeName intArray;
    static ExactTypeName stringArray;
    static ExactTypeName colClass;
    static ParameterizedTypeName colOfStrings;
    static ParameterizedTypeName colOfStringsArray;

    @BeforeAll
    static void init() {
        intWrapperClass = Types.of(Integer.class);
        floatWrapperClass = Types.of(Float.class);
        intClass = Types.of(int.class);
        intArray = Types.of(int[].class);
        stringArray = Types.of(String[].class);
        colClass = Types.of(Collection.class);
        colOfStrings = colClass.parameterized(String.class);
        colOfStringsArray = colOfStrings.dimensions(1);
    }

    @Test
    void isWrapper() {
        assertTrue(Types.isWrapper(intWrapperClass));
        assertTrue(Types.isWrapper(floatWrapperClass));
        assertFalse(Types.isWrapper(intClass));
    }

    @Test
    void array_isArray() {
        assertTrue(intArray.isArray());
        assertTrue(stringArray.isArray());
        assertTrue(colOfStringsArray.isArray());
    }

    @Test
    void array_isPrimitive() {
        assertFalse(intArray.isPrimitive());
        assertFalse(stringArray.isPrimitive());
        assertFalse(colOfStringsArray.isPrimitive());
    }

    @Test
    void array_toString() {
        assertEquals("int[]", intArray.toString());
        assertEquals("java.lang.String[]", stringArray.toString());
        assertEquals("java.util.Collection<java.lang.String>[]", colOfStringsArray.toString());
    }

    @Test
    void array_getName() {
        assertEquals("int[]", intArray.getName());
        assertEquals("java.lang.String[]", stringArray.getName());
        assertEquals("java.util.Collection[]", colOfStringsArray.getName());
    }

    @Test
    void array_getInternalName() {
        assertEquals("[I", intArray.getInternalName());
        assertEquals("[Ljava/lang/String;", stringArray.getInternalName());
        assertEquals("[Ljava/util/Collection;", colOfStringsArray.getInternalName());
    }

    @Test
    void class_toString() {
        assertEquals("int", intClass.toString());
        assertEquals("java.util.Collection", colClass.toString());
    }

    @Test
    void class_getName() {
        assertEquals("int", intClass.getName());
        assertEquals("java.util.Collection", colClass.getName());
    }

    @Test
    void class_getInternalName() {
        assertEquals("int", intClass.getInternalName());
        assertEquals("java/util/Collection", colClass.getInternalName());
    }

    @Test
    void class_isPrimitive() {
        assertTrue(intClass.isPrimitive());
        assertFalse(colClass.isPrimitive());
    }

    @Test
    void class_isArray() {
        assertFalse(intClass.isArray());
        assertFalse(colClass.isArray());
    }

}
