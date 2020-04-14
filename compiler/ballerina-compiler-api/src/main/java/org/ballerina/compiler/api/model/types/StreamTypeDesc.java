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

import org.ballerina.compiler.api.SymbolFactory;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Represents the stream Type Descriptor.
 * 
 * @since 1.3.0
 */
public class StreamTypeDesc extends BTypeDescriptor {
    private List<BType> typeParameters;
    
    public StreamTypeDesc(List<BType> typeParameters) {
        super(TypeKind.STREAM);
        this.typeParameters = typeParameters;
    }
    
    public List<BTypeDescriptor> getTypeParameters() {
        List<BTypeDescriptor> typeParams = new ArrayList<>();
        for (BType bType : this.typeParameters) {
            BTypeDescriptor typeDescriptor = SymbolFactory.createTypeDescriptor(bType);
            typeParams.add(typeDescriptor);
        }
        return typeParams;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSignature() {
        StringBuilder signature = new StringBuilder();
        signature.append(this.typeKind.typeName());

        StringJoiner joiner = new StringJoiner(", ");
        for (BType typeParameter : this.typeParameters) {
            BTypeDescriptor tDesc = SymbolFactory.createTypeDescriptor(typeParameter);
            joiner.add(tDesc.getSignature());
        }
        signature.append("<").append(joiner.toString()).append(">");
        
        return signature.toString();
    }
}
