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

import io.ballerinalang.compiler.syntax.tree.LocalVariableDeclaration;
import io.ballerinalang.compiler.syntax.tree.Node;
import io.ballerinalang.compiler.syntax.tree.NonTerminalNode;

public class STAssignmentStatement extends STStatement {

    public final STNode lhsExpression;
    public final STNode equalsToken;
    public final STNode expr;
    public final STNode semicolonToken;

    public STAssignmentStatement(SyntaxKind kind,
                                 STNode lhsExpression,
                                 STNode equalsToken,
                                 STNode expr,
                                 STNode semicolonToken) {
        super(kind);
        this.lhsExpression = lhsExpression;
        this.equalsToken = equalsToken;
        this.expr = expr;
        this.semicolonToken = semicolonToken;

        this.bucketCount = 4;
        this.childBuckets = new STNode[this.bucketCount];
        this.addChildNode(lhsExpression, 0);
        this.addChildNode(equalsToken, 1);
        this.addChildNode(expr, 2);
        this.addChildNode(semicolonToken, 3);
    }

    @Override
    public Node createFacade(int position, NonTerminalNode parent) {
        return new LocalVariableDeclaration(this, position, parent);
    }
}
