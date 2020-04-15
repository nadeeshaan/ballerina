/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package io.ballerinalang.compiler.internal.treegen.model.json;

/**
 * Represents a syntax node attribute in the syntax tree descriptor.
 *
 * @since 1.3.0
 */
public class SyntaxNodeAttribute {
    private String name;
    private String type;
    private String occurrences;

    public SyntaxNodeAttribute(String name, String type, String occurrences) {
        this.name = name;
        this.type = type;
        this.occurrences = occurrences;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public OccurrenceKind getOccurrenceKind() {
        if (occurrences == null) {
            return OccurrenceKind.SINGLE;
        } else {
            return OccurrenceKind.valueOf(occurrences);
        }
    }

    /**
     * Indicates whether a given attribute repeats or not.
     *
     * @since 1.3.0
     */
    public enum OccurrenceKind {
        MULTIPLE,
        SINGLE
    }
}
