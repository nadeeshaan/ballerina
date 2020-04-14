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

import io.ballerinalang.compiler.syntax.tree.Node;
import org.ballerina.compiler.api.model.BCompiledSymbol;

import java.util.List;

/**
 * Compiler API exposed for the external users.
 * 
 * @since 1.3.0
 */
public interface CompilerAPI {
    BCompiledSymbol getSymbol(Node tree, Position position);
    
    List<BCompiledSymbol> getVisibleSymbols(Node tree, Position position);
    
    // remove
    void getDiagnostic(Node tree, Position position);
    
    void getDiagnostics(Node tree, Position position);
    
    // TODO: Expose a References API 
}
