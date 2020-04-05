package org.ballerina.compiler.api;

import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.util.List;

/**
 * Generates the Semantic models.
 * 
 * @since 1.3.0
 */
public interface SemanticModelFactory {
    /**
     * Get the semantic model for the given BLang package and the uri.
     * 
     * @param uri Document URI
     * @param bLangPackage BLang Package to build the semantic model
     * @return {@link SemanticModel} generated Semantic model
     */
     SemanticModel getModel(String uri, BLangPackage bLangPackage);
     
     
     List<SemanticModel> getModels(BLangPackage bLangPackage);
}
