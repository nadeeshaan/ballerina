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
public class FunctionDefinitionNode extends ModuleMemberDeclarationNode {

    public FunctionDefinitionNode(STNode internalNode, int position, NonTerminalNode parent) {
        super(internalNode, position, parent);
    }

    public MetadataNode metadata() {
        return childInBucket(0);
    }

    public Optional<Token> visibilityQualifier() {
        return optionalChildInBucket(1);
    }

    public Token functionKeyword() {
        return childInBucket(2);
    }

    public IdentifierToken functionName() {
        return childInBucket(3);
    }

    public FunctionSignatureNode functionSignature() {
        return childInBucket(4);
    }

    public Node functionBody() {
        return childInBucket(5);
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
                "metadata",
                "visibilityQualifier",
                "functionKeyword",
                "functionName",
                "functionSignature",
                "functionBody"};
    }

    public FunctionDefinitionNode modify(
            MetadataNode metadata,
            Token visibilityQualifier,
            Token functionKeyword,
            IdentifierToken functionName,
            FunctionSignatureNode functionSignature,
            Node functionBody) {
        if (checkForReferenceEquality(
                metadata,
                visibilityQualifier,
                functionKeyword,
                functionName,
                functionSignature,
                functionBody)) {
            return this;
        }

        return NodeFactory.createFunctionDefinitionNode(
                metadata,
                visibilityQualifier,
                functionKeyword,
                functionName,
                functionSignature,
                functionBody);
    }
}
