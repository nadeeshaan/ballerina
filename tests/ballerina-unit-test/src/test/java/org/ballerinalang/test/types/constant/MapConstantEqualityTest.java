/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.test.types.constant;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BValue;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Constant test cases.
 */
public class MapConstantEqualityTest {

    private static CompileResult compileResult;

    @BeforeClass
    public void setup() {
        compileResult = BCompileUtil.compile("test-src/types/constant/map-literal-constant-equality.bal");
    }

    // boolean type.

    @Test
    public void testSimpleBooleanMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleBooleanMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleBooleanMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleBooleanMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleBooleanMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleBooleanMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleBooleanMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult,
                "testSimpleBooleanMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexBooleanMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexBooleanMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexBooleanMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexBooleanMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexBooleanMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexBooleanMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexBooleanMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult,
                "testComplexBooleanMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    // int type.

    @Test
    public void testSimpleIntMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleIntMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleIntMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleIntMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleIntMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleIntMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleIntMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleIntMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexIntMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexIntMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexIntMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexIntMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexIntMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexIntMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexIntMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexIntMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    // byte type.

    @Test
    public void testSimpleByteMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleByteMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleByteMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleByteMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleByteMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleByteMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleByteMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleByteMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexByteMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexByteMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexByteMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexByteMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexByteMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexByteMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexByteMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexByteMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    // float type.

    @Test
    public void testSimpleFloatMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleFloatMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleFloatMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleFloatMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleFloatMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleFloatMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleFloatMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleFloatMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexFloatMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexFloatMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexFloatMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexFloatMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexFloatMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexFloatMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexFloatMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult,
                "testComplexFloatMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    // decimal type.

    @Test
    public void testSimpleDecimalMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleDecimalMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleDecimalMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleDecimalMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleDecimalMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleDecimalMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleDecimalMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleDecimalMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexDecimalMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexDecimalMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexDecimalMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexDecimalMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexDecimalMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexDecimalMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexDecimalMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult,
                "testComplexDecimalMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    // string type.

    @Test
    public void testSimpleStringMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleStringMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleStringMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleStringMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleStringMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleStringMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleStringMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleStringMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexStringMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexStringMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexStringMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexStringMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexStringMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexStringMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexStringMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult,
                "testComplexStringMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    // nil type.

    @Test
    public void testSimpleNilMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleNilMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleNilMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleNilMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleNilMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleNilMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testSimpleNilMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testSimpleNilMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexNilMapValueEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexNilMapValueEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexNilMapValueEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexNilMapValueEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexNilMapReferenceEqualityUsingSameReference() {
        BValue[] returns = BRunUtil.invoke(compileResult, "testComplexNilMapReferenceEqualityUsingSameReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertTrue(((BBoolean) returns[0]).booleanValue());
    }

    @Test
    public void testComplexNilMapReferenceEqualityUsingDifferentReference() {
        BValue[] returns = BRunUtil.invoke(compileResult,
                "testComplexNilMapReferenceEqualityUsingDifferentReference");
        Assert.assertNotNull(returns[0]);
        Assert.assertFalse(((BBoolean) returns[0]).booleanValue());
    }
}
