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

import org.ballerina.compiler.api.impl.BallerinaSymbol;
import org.ballerinalang.model.elements.PackageID;

import java.util.List;

/**
 * Represent a Ballerina Service.
 * 
 * @since 1.3.0
 */
public class ServiceSymbol extends BallerinaSymbol {
    // TODO: implement the meta data representation
    List<VariableSymbol> attachedExpressions;
    List<FunctionSymbol> resourceFunctions;
    
    public ServiceSymbol(String name,
                         PackageID moduleID,
                         List<VariableSymbol> attachedExprs,
                         List<FunctionSymbol> resourceFunctions) {
        super(name, moduleID);
        this.attachedExpressions = attachedExprs;
        this.resourceFunctions = resourceFunctions;
    }

    public List<VariableSymbol> getAttachedExpressions() {
        return attachedExpressions;
    }

    public List<FunctionSymbol> getResourceFunctions() {
        return resourceFunctions;
    }
}
