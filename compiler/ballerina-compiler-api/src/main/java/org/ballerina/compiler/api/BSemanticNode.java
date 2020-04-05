package org.ballerina.compiler.api;

import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;

/**
 * Represents the Ballerina Semantic node.
 * 
 * @since 1.3.0
 */
public interface BSemanticNode {
    /**
     * Get the name of the node.
     * 
     * @return {@link String} name of node
     */
    String getName();
    
    /**
     * Get the position of the symbol in the test source.
     *
     * @return {@link DiagnosticPos} node position
     */
    DiagnosticPos getPosition();

    /**
     * Get the symbol of the node.
     * 
     * @return {@link BSymbol} symbol for the corresponding node
     */
    BSymbol getSymbol();

    /**
     * Get the tree node.
     * 
     * @return {@link BLangNode} corresponding tree node
     */
    BLangNode getTreeNode();
}
