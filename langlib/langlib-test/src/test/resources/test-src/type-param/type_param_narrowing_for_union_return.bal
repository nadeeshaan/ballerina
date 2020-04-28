// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

const ASSERTION_ERROR_REASON = "AssertionError";

function assertTrue(any|error actual) {
    if actual is boolean && actual {
        return;
    }
    panic error(ASSERTION_ERROR_REASON,
                message = "expected 'true', found '" + actual.toString () + "'");
}

function assertEqual(anydata|error expected, anydata|error actual) {
    if expected == actual {
        return;
    }

    panic error(ASSERTION_ERROR_REASON,
                message = "expected '" + expected.toString() + "', found '" + actual.toString () + "'");
}

function testSimpleUnionReturnParameterNarrowing() {
    int[]|float[] arr = <int[]>[1, 2];
    [int, (int|float)][] y = arr.enumerate();
    assertTrue(y[0][1] is int);
    assertEqual(1, y[0][1]);
    assertEqual(2, y[1][1]);
}

function testUnionOfMapsReturnParameterNarrowing() {
    map<int>|map<float> m = <map<int>>{"1": 1};
    int|float x = m.get("1");
    assertEqual(1, x);
}

function testStringIntFloatSimpleAndArrayUnionReturnParameterNarrowing() {
    string | int[] | int | float[] | float arr = <int[]>[1, 2];
    if (arr is int[] | float[]) {
        [int, (int|float)][] y = arr.enumerate();
        assertTrue(y[0][1] is int);
        assertEqual(1, y[0][1]);
        assertEqual(2, y[1][1]);
    } else {
        assertTrue(false);
    }
}

function testIntFloatSimpleAndMapUnionReturnParameterNarrowing() {
    map<int> | int | map<float> | float  m = <map<int>>{"1": 1};
    if (m is map<int> | map<float>) {
        int|float x = m.get("1");
        assertTrue(x is int);
        assertEqual(1, x);
    } else {
        assertTrue(false);
    }
}

function testIntFloatSimpleArrayMapUnionReturnParameterNarrowing() {
    map<int> | int[] | map<float> | float[]  m = <map<int>>{"1": 1};
    if (m is map<float> | map<int>) {
        int|float x = m.get("1");
        assertTrue(x is int);
        assertEqual(1, x);
    } else {
        assertTrue(false);
    }
}

type Foo record {
    string id;
};

type Bar record {
    int id;
};

//function testStreamTypeParamNarrowing() {
//    Foo[] fooList = [{id: "1234"}, {id: "5678"}];
//    stream<Foo> fooStream = fooList.toStream();
//    stream<Foo>|stream<Bar> fooBarStream = fooStream;
//
//    var res = fooBarStream.next();
//    Foo|Bar retValue = <Foo|Bar>res;
//}
