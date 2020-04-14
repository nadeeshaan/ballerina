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
import org.ballerina.compiler.api.impl.BallerinaField;
import org.ballerina.compiler.api.model.FunctionSymbol;
import org.ballerina.compiler.api.model.types.BTypeDescriptor;
import org.ballerinalang.model.types.TypeKind;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAttachedFunction;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an object-type-descriptor.
 * 
 * @since 1.3.0
 */
public class ObjectTypeDescriptor extends BTypeDescriptor {
    private static final String OBJECT = "object";
    private static final String ABSTRACT = "abstract";
    private static final String CLIENT = "client";
    private static final String SPACE = " ";
    private static final String DOLLAR = "$";
    private static final String PUBLIC = "public";
    private static final String PRIVATE = "private";
    private static final String LEFT_CURL = "{";
    private static final String RIGHT_CURL = "}";
    private static final String SEMI_COLON = ";";
    
    private BObjectType objectType;
    
    public ObjectTypeDescriptor(BObjectType objectType) {
        super(TypeKind.OBJECT);
        this.objectType = objectType;
    }
    
    public List<FunctionSymbol> getAttachedFunctions() {
        List<FunctionSymbol> attachedFunctions = new ArrayList<>();
        for (BAttachedFunction bAttachedFunction : ((BObjectTypeSymbol) objectType.tsymbol).attachedFuncs) {
            FunctionSymbol functionSymbol = SymbolFactory.createFunctionSymbol(bAttachedFunction.symbol);
            attachedFunctions.add(functionSymbol);
        }

        return attachedFunctions;
    }
    
    public FunctionSymbol getInitFunction() {
        return SymbolFactory.createFunctionSymbol(((BObjectTypeSymbol) objectType.tsymbol).initializerFunc.symbol);
    }
    
    public List<BallerinaField> getFields() {
        List<BallerinaField> fields = new ArrayList<>();
        for (BField bField : objectType.fields) {
            BallerinaField field = SymbolFactory.createField(bField.getName().getValue(), bField.getType());
            fields.add(field);
        }

        return fields;
    }

    @Override
    public String getSignature() {
        StringBuilder signature = new StringBuilder();
        if ((objectType.flags & Flags.ABSTRACT) == Flags.ABSTRACT) {
            signature.append(ABSTRACT).append(SPACE);
        }
        if ((objectType.flags & Flags.CLIENT) == Flags.CLIENT) {
            signature.append(CLIENT).append(SPACE);
        }
        signature.append(this.typeKind.typeName())
                .append(SPACE).append(LEFT_CURL);

        for (BField field : this.objectType.fields) {
            BallerinaField bField = SymbolFactory.createField(field.getName().getValue(), field.getType());
            if (Symbols.isFlagOn(field.symbol.flags, Flags.PUBLIC)) {
                signature.append(PUBLIC).append(SPACE);
            } else if (Symbols.isFlagOn(field.symbol.flags, Flags.PRIVATE)) {
                signature.append(PRIVATE).append(SPACE);
            }
            signature.append(bField.getType().getSignature()).append(SPACE)
                    .append(bField.getName()).append(SEMI_COLON).append(SPACE);
        }

        for (BAttachedFunction attachedFunc : ((BObjectTypeSymbol) this.objectType.tsymbol).attachedFuncs) {
            String functionTypeSignature = SymbolFactory.createTypeDescriptor(attachedFunc.type).getSignature();
            if (Symbols.isFlagOn(attachedFunc.symbol.flags, Flags.PUBLIC)) {
                signature.append(PUBLIC).append(SPACE);
            } else if (Symbols.isFlagOn(attachedFunc.symbol.flags, Flags.PRIVATE)) {
                signature.append(PRIVATE).append(SPACE);
            }

            signature.append(functionTypeSignature).append(SEMI_COLON).append(SPACE);
        }
        signature.append(RIGHT_CURL);
        
        return signature.toString();
    }
}
