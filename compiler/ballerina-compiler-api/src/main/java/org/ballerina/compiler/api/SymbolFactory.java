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
package org.ballerina.compiler.api;

import org.ballerina.compiler.api.impl.BallerinaField;
import org.ballerina.compiler.api.model.FunctionSymbol;
import org.ballerina.compiler.api.model.ServiceSymbol;
import org.ballerina.compiler.api.model.VariableSymbol;
import org.ballerina.compiler.api.model.types.BTypeDescriptor;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.ServiceNode;
import org.ballerinalang.model.tree.expressions.ExpressionNode;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory implementation to convert the {@link org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol}.
 * 
 * @since 1.3.0
 */
public class SymbolFactory {
    private SymbolFactory() {
    }
    
    public static FunctionSymbol createFunctionSymbol(BInvokableSymbol invokableSymbol) {
        String name = invokableSymbol.getName().getValue();
        PackageID moduleID = invokableSymbol.pkgID;
        return new FunctionSymbol(name, invokableSymbol, moduleID);
    }
    
    public static ServiceSymbol createServiceSymbol(ServiceNode serviceNode, BLangObjectTypeNode objectTypeNode,
                                                       PackageID moduleID) {
        String name = serviceNode.getName().getValue();
        List<VariableSymbol> attachedSymbols = new ArrayList<>();
        List<FunctionSymbol> resourceFunctions = new ArrayList<>();

        for (ExpressionNode expr : serviceNode.getAttachedExprs()) {
            VariableSymbol variableSymbol = getVariableSymbol((BLangSimpleVarRef) expr);
            attachedSymbols.add(variableSymbol);
        }

        for (BLangFunction function : objectTypeNode.functions) {
            FunctionSymbol functionSymbol = createFunctionSymbol(function.symbol);
            resourceFunctions.add(functionSymbol);
        }
        
        ServiceSymbol serviceSymbol = new ServiceSymbol(name, moduleID, attachedSymbols, resourceFunctions);
        
        return serviceSymbol;
    }
    
    public static VariableSymbol getVariableSymbol(BLangSimpleVarRef varRef) {
        String name = varRef.getVariableName().getValue();
        PackageID moduleID = varRef.symbol.pkgID;
        return new VariableSymbol(name, moduleID, createTypeDescriptor(varRef.type));
    }
    
    public static BallerinaField createField(String name, BType fieldType) {
        return new BallerinaField(name, createTypeDescriptor(fieldType));
    }
    
    /**
     * Get the {@link BTypeDescriptor} for the given {@link BType}.
     *
     * @param bType type to convert
     * @return generated type descriptor
     */
    public static BTypeDescriptor createTypeDescriptor(BType bType) {
        return null;
    }
    
}
