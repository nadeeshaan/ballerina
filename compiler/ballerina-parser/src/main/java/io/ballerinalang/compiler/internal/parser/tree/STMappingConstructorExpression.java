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

import io.ballerinalang.compiler.syntax.tree.MappingConstructorExpression;
import io.ballerinalang.compiler.syntax.tree.NonTerminalNode;

/**
 * @since 1.3.0
 */
public class STMappingConstructorExpression extends STNode {

    public final STNode openBrace;
    public final STNode fields;
    public final STNode closeBrace;

    STMappingConstructorExpression(STNode openBrace, STNode fields, STNode closeBrace) {
        super(SyntaxKind.MAPPING_CONSTRUCTOR);
        this.openBrace = openBrace;
        this.fields = fields;
        this.closeBrace = closeBrace;

        addChildren(openBrace, fields, closeBrace);
    }

    public NonTerminalNode createFacade(int position, NonTerminalNode parent) {
        return new MappingConstructorExpression(this, position, parent);
    }
}
