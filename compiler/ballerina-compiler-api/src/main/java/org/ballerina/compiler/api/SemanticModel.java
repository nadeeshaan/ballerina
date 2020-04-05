package org.ballerina.compiler.api;

import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangNode;

import java.util.List;

/**
 * Holds the semantic information related to a given source file.
 * 
 * @since 1.3.0
 */
public interface SemanticModel {
    /**
     * Get the {@link BSemanticNode} at a given position with line and the offset
     * 
     * @param position reference position to retrieve the symbol information
     * @return {@link BSymbol} at the 
     */
    BSemanticNode getSemanticNode(Position position);

    /**
     * Get the associated Semantic node with the given {@link BLangNode}.
     * 
     * @param node Node to evaluate
     * @return {@link BSymbol} associated to the node 
     */
    BSemanticNode getSemanticNode(BLangNode node);

    /**
     * Get all the visible members to  given position.
     * 
     * @param position position to get the visible members
     * @return {@link List} of visible members
     */
    List<BSemanticNode> getVisibleMembers(Position position);
}
