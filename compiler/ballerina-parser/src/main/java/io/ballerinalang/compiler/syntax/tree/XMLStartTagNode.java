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

/**
 * This is a generated syntax tree node.
 *
 * @since 1.3.0
 */
public class XMLStartTagNode extends XMLElementTagNode {

    public XMLStartTagNode(STNode internalNode, int position, NonTerminalNode parent) {
        super(internalNode, position, parent);
    }

    public Token ltToken() {
        return childInBucket(0);
    }

    public XMLNameNode name() {
        return childInBucket(1);
    }

    public NodeList<XMLAttributeNode> attributes() {
        return new NodeList<>(childInBucket(2));
    }

    public Token getToken() {
        return childInBucket(3);
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
                "ltToken",
                "name",
                "attributes",
                "getToken"};
    }

    public XMLStartTagNode modify(
            Token ltToken,
            XMLNameNode name,
            NodeList<XMLAttributeNode> attributes,
            Token getToken) {
        if (checkForReferenceEquality(
                ltToken,
                name,
                attributes.underlyingListNode(),
                getToken)) {
            return this;
        }

        return NodeFactory.createXMLStartTagNode(
                ltToken,
                name,
                attributes,
                getToken);
    }
}
