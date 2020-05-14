/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.langserver.completions.builder;

import org.ballerina.compiler.api.model.BallerinaVariable;
import org.ballerinalang.langserver.completions.util.ItemResolverConstants;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;

/**
 * This class is being used to build variable type completion item.
 *
 * @since 0.983.0
 */
public final class BVariableCompletionItemBuilder {
    private BVariableCompletionItemBuilder() {
    }

    /**
     * Creates and returns a completion item.
     *
     * @param varSymbol Variable Symbol
     * @param label   label
     * @param type    variable type
     * @return {@link CompletionItem}
     */
    public static CompletionItem build(BallerinaVariable varSymbol, String label, String type) {
        CompletionItem item = new CompletionItem();
        item.setLabel(label);
        String[] delimiterSeparatedTokens = (label).split("\\.");
        item.setInsertText(delimiterSeparatedTokens[delimiterSeparatedTokens.length - 1]);
        item.setDetail((type.equals("")) ? ItemResolverConstants.NONE : type);
        setMeta(item, varSymbol);
        return item;
    }

    private static void setMeta(CompletionItem item, BallerinaVariable varSymbol) {
        item.setKind(CompletionItemKind.Variable);
        if (varSymbol == null) {
            return;
        }
        if (varSymbol.getDocAttachment().isPresent()
                && varSymbol.getDocAttachment().get().getDescription().isPresent()) {
            item.setDocumentation(varSymbol.getDocAttachment().get().getDescription().get());
        }
    }
}
