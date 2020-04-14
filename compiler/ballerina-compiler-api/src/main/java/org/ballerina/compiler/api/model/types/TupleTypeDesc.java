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
package org.ballerina.compiler.api.model.types;

import com.sun.istack.internal.NotNull;
import org.ballerina.compiler.api.model.types.BTypeDescriptor;
import org.ballerinalang.model.types.TypeKind;

import java.util.List;
import java.util.StringJoiner;

/**
 * Represent array type descriptor.
 * 
 * @since 1.3.0
 */
public class TupleTypeDesc extends BTypeDescriptor {
    private List<BTypeDescriptor> memberTypes;
    private BTypeDescriptor restType;

    public TupleTypeDesc(@NotNull List<BTypeDescriptor> memberTypeDescs, BTypeDescriptor restTypeDesc) {
        super(TypeKind.TUPLE);
    }

    public List<BTypeDescriptor> getMemberTypes() {
        return this.memberTypes;
    }

    public BTypeDescriptor getRestType() {
        return this.restType;
    }

    @Override
    public String getSignature() {
        StringBuilder signature = new StringBuilder("(");
        StringJoiner joiner = new StringJoiner(", ");
        for (BTypeDescriptor memberType : memberTypes) {
            String name = memberType.getSignature();
            joiner.add(name);
        }
        if (this.restType != null) {
            joiner.add(this.restType.getSignature() + "...");
        }
        signature.append(joiner.toString()).append(")");
        return signature.toString();
    }
}
