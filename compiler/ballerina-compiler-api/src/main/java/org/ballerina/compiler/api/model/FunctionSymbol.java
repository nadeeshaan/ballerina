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
package org.ballerina.compiler.api.model;

import org.ballerina.compiler.api.SymbolFactory;
import org.ballerina.compiler.api.impl.BallerinaSymbol;
import org.ballerina.compiler.api.model.types.BTypeDescriptor;
import org.ballerinalang.model.elements.PackageID;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Function Symbol.
 * 
 * @since 1.3.0
 */
public class FunctionSymbol extends BallerinaSymbol {
    private BInvokableSymbol invokableSymbol;
    // TODO: add the flags such as public, attached and etc.
    
    public FunctionSymbol(String name, BInvokableSymbol invokableSymbol, PackageID moduleID) {
        super(name, moduleID);
        this.invokableSymbol = invokableSymbol;
    }
    
    public List<VariableSymbol> getParams() {
        List<VariableSymbol> paramList = new ArrayList<>();
        for (BVarSymbol varSymbol : invokableSymbol.params) {
            PackageID varModuleID = varSymbol.pkgID;
            VariableSymbol variableSymbol = new VariableSymbol(varSymbol.getName().getValue(), varModuleID);
            paramList.add(variableSymbol);
        }
        return paramList;
    }
    
    public VariableSymbol getRestParam() {
        return new VariableSymbol(invokableSymbol.getName().getValue(), invokableSymbol.pkgID);
    }
    
    public BTypeDescriptor getReturnType() {
        BTypeSymbol typeSymbol = this.invokableSymbol.getReturnType().tsymbol;
        return SymbolFactory.createTypeDescriptor(typeSymbol.type);
    }
    
    // TODO: Add the markdown documentation getter
    // TODO: Add the builder
}
