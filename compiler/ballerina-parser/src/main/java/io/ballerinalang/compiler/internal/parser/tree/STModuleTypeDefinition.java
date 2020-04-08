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
package io.ballerinalang.compiler.internal.parser.tree;

import io.ballerinalang.compiler.syntax.tree.Node;
import io.ballerinalang.compiler.syntax.tree.NonTerminalNode;
import io.ballerinalang.compiler.syntax.tree.TypeDefinitionNode;

public class STModuleTypeDefinition extends STNode {
    // TODO metadata goes here
    public final STNode visibilityQualifier;
    public final STNode typeKeyword;
    public final STNode typeName;
    public final STNode typeDescriptor;
    public final STNode semicolon;

    STModuleTypeDefinition(STNode visibilityQualifier,
                           STNode typeKeyword,
                           STNode typeName,
                           STNode typeDescriptor,
                           STNode semicolon) {

        super(SyntaxKind.TYPE_DEFINITION);
        this.visibilityQualifier = visibilityQualifier;
        this.typeKeyword = typeKeyword;
        this.typeName = typeName;
        this.typeDescriptor = typeDescriptor;
        this.semicolon = semicolon;

        addChildren(visibilityQualifier, typeKeyword, typeName, typeDescriptor, semicolon);
    }

    @Override
    public Node createFacade(int position, NonTerminalNode parent) {
        return new TypeDefinitionNode(this, position, parent);
    }
}
