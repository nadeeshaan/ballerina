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
package io.ballerinalang.compiler.syntax.tree;

import io.ballerinalang.compiler.internal.parser.tree.STNode;

import java.util.Optional;

/**
 * This is a generated syntax tree node.
 *
 * @since 1.3.0
 */
public class FunctionSignatureNode extends NonTerminalNode {

    public FunctionSignatureNode(STNode internalNode, int position, NonTerminalNode parent) {
        super(internalNode, position, parent);
    }

    public Token openParenToken() {
        return childInBucket(0);
    }

    public NodeList<ParameterNode> parameters() {
        return new NodeList<>(childInBucket(1));
    }

    public Token closeParenToken() {
        return childInBucket(2);
    }

    public Optional<ReturnTypeDescriptorNode> returnTypeDesc() {
        return optionalChildInBucket(3);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T apply(NodeTransformer<T> visitor) {
        return visitor.transform(this);
    }

    @Override
    protected String[] childNames() {
        return new String[]{
                "openParenToken",
                "parameters",
                "closeParenToken",
                "returnTypeDesc"};
    }

    public FunctionSignatureNode modify(
            Token openParenToken,
            NodeList<ParameterNode> parameters,
            Token closeParenToken,
            ReturnTypeDescriptorNode returnTypeDesc) {
        if (checkForReferenceEquality(
                openParenToken,
                parameters.underlyingListNode(),
                closeParenToken,
                returnTypeDesc)) {
            return this;
        }

        return NodeFactory.createFunctionSignatureNode(
                openParenToken,
                parameters,
                closeParenToken,
                returnTypeDesc);
    }
}
