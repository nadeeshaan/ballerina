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
package org.ballerina.compiler.api.document.visitors;

import org.ballerina.compiler.api.Position;
import org.ballerina.compiler.api.impl.BallerinaSymbol;
import org.ballerina.compiler.api.model.BCompiledSymbol;
import org.ballerina.compiler.api.model.FunctionSymbol;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;

/**
 * Visitor to find the symbol at a given cursor position.
 * 
 * @since 1.3.0
 */
public class SymbolAtCursorFinder extends BLangNodeVisitor {
    private final Position position;
    private boolean terminate = false;
    private BallerinaSymbol symbolAtCursor = null;
    
    public SymbolAtCursorFinder(Position position) {
        this.position = position;
    }
    
    public BCompiledSymbol getBallerinaSymbol(BLangCompilationUnit cUnit) {
        this.visit(cUnit);
        return this.symbolAtCursor;
    }

    @Override
    public void visit(BLangCompilationUnit compUnit) {
        compUnit.getTopLevelNodes().forEach(topLevelNode -> this.acceptNode((BLangNode) topLevelNode));
    }

    @Override
    public void visit(BLangFunction funcNode) {
        DiagnosticPos funcNamePos = funcNode.getName().getPosition();
        // Captures the function symbol if the given position is on the function name
        if (onIdentifier(funcNamePos)) {
            this.symbolAtCursor = new FunctionSymbol(funcNode.getName().getValue(), funcNode.symbol,
                    funcNode.symbol.pkgID);
        }
    }

    private void acceptNode(BLangNode node) {
        if (node == null || this.terminate) {
            return;
        }
        node.accept(this);
    }
    
    private boolean onIdentifier(Diagnostic.DiagnosticPosition symbolPosition) {
        int zeroBasedStartLine = symbolPosition.getStartLine() - 1;
        int zeroBasedEndLine = symbolPosition.getEndLine() - 1;
        int zeroBasedStartCol = symbolPosition.getStartColumn() - 1;
        int zeroBasedEndCol = symbolPosition.getEndColumn() - 1;
        
        boolean matched =  zeroBasedStartLine == zeroBasedEndLine 
                && zeroBasedStartLine == this.position.getLine() 
                && zeroBasedStartCol <= this.position.getCharacter()
                && zeroBasedEndCol >= this.position.getCharacter();
        
        this.terminate = matched;
        
        return matched;
    }

    private boolean withinBlock(Diagnostic.DiagnosticPosition symbolPosition) {
        int zeroBasedStartLine = symbolPosition.getStartLine() - 1;
        int zeroBasedEndLine = symbolPosition.getEndLine() - 1;
        int zeroBasedStartCol = symbolPosition.getStartColumn() - 1;
        int zeroBasedEndCol = symbolPosition.getEndColumn() - 1;
        int line = this.position.getLine();
        int col = this.position.getCharacter();

        return (zeroBasedStartLine < line && zeroBasedEndLine > line)
                || (zeroBasedStartLine < line && zeroBasedEndLine == line && zeroBasedEndCol > col)
                || (zeroBasedStartLine == line && zeroBasedStartCol < col && zeroBasedEndLine > line)
                || (zeroBasedStartLine == zeroBasedEndLine && zeroBasedStartLine == line
                && zeroBasedStartCol <= col && zeroBasedEndCol >= col);
    }
    
//    private BSemanticNode getSemanticModel(BLangNode node, BSymbol symbol) {
//        return new BSemanticNodeImpl.SemanticNodeBuilder()
//                .withNode(node)
//                .withSymbol(symbol)
//                .build();
//    }
}
