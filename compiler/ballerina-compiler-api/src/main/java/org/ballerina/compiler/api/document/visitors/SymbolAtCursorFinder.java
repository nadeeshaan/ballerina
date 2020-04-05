package org.ballerina.compiler.api.document.visitors;

import org.ballerina.compiler.api.Position;
import org.ballerinalang.util.diagnostic.Diagnostic;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;

public class SymbolAtCursorFinder extends BLangNodeVisitor {

    private final Position position;
    private BSymbol symbolAtCursor;
    private boolean terminate = false;
    
    public SymbolAtCursorFinder(Position position) {
        this.position = position;
    }
    
    public BSymbol getSymbol(BLangPackage bLangPackage) {
//        bLangPackage.compUnits.stream().filter(bLangCompilationUnit -> bLangCompilationUnit.)
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
            this.symbolAtCursor = funcNode.symbol;
            return;
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
}
