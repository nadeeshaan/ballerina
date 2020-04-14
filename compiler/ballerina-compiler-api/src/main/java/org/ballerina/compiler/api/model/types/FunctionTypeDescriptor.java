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
import org.ballerina.compiler.api.model.types.BTypeDescriptor;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Represents a function type descriptor.
 * 
 * @since 1.3.0
 */
public class FunctionTypeDescriptor extends BTypeDescriptor {
    private BInvokableType invokableType;
    
    public FunctionTypeDescriptor(BInvokableType invokableType) {
        super(TypeKind.FUNCTION);
        this.invokableType = invokableType;
    }
    
    public List<BTypeDescriptor> getParameterTypes() {
        List<BTypeDescriptor> list = new ArrayList<>();
        for (BType paramType : invokableType.paramTypes) {
            BTypeDescriptor typeDescriptor = SymbolFactory.createTypeDescriptor(paramType);
            list.add(typeDescriptor);
        }
        return list;
    }
    
    public BTypeDescriptor getRestTypeDescriptor() {
        return SymbolFactory.createTypeDescriptor(invokableType.restType);
    }
    
    public BTypeDescriptor getReturnTypeDescriptor() {
        return SymbolFactory.createTypeDescriptor(invokableType.retType);
    }

    @Override
    public String getSignature() {
        StringBuilder signature = new StringBuilder();
        signature.append(this.typeKind.typeName())
                .append("(");
        StringJoiner paramsJoiner = new StringJoiner(", ");
        for (BTypeDescriptor bTypeDescriptor : this.getParameterTypes()) {
            String bTypeDescriptorSignature = bTypeDescriptor.getSignature();
            paramsJoiner.add(bTypeDescriptorSignature);
        }
        if (invokableType.restType != null) {
            paramsJoiner.add(this.getRestTypeDescriptor().getSignature() + "...");
        }
        String params = paramsJoiner.toString();
        signature.append(params).append(")");
        if (invokableType.retType != null) {
            String returnType = this.getReturnTypeDescriptor().getSignature();
            signature.append(" ").append(returnType);
        }
        
        return signature.toString();
    }
}
