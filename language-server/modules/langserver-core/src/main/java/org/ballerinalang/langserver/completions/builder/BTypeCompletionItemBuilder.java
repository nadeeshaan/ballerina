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

import org.ballerina.compiler.api.model.BCompiledSymbol;
import org.ballerina.compiler.api.model.BallerinaModule;
import org.ballerina.compiler.api.model.BallerinaSymbolKind;
import org.ballerina.compiler.api.model.BallerinaTypeDefinition;
import org.ballerina.compiler.api.types.TypeDescKind;
import org.ballerina.compiler.api.types.TypeDescriptor;
import org.ballerina.compiler.api.types.UnionTypeDescriptor;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;

import java.util.List;
import java.util.Locale;

/**
 * This class is being used to build BType completion item.
 *
 * @since 0.983.0
 */
public class BTypeCompletionItemBuilder {
    private BTypeCompletionItemBuilder() {
    }

    /**
     * Creates and returns a completion item.
     *
     * @param bSymbol Ballerina Symbol to evaluate
     * @param label   label
     * @return {@link CompletionItem}
     */
    public static CompletionItem build(BCompiledSymbol bSymbol, String label) {
        CompletionItem item = new CompletionItem();
        item.setLabel(label);
        String[] delimiterSeparatedTokens = (label).split("\\.");
        item.setInsertText(delimiterSeparatedTokens[delimiterSeparatedTokens.length - 1]);
        setMeta(item, bSymbol);
        return item;
    }

    private static void setMeta(CompletionItem item, BCompiledSymbol bSymbol) {
        if (bSymbol == null) {
            item.setKind(CompletionItemKind.Class);
            return;
        }
        //Or, else
        if (bSymbol instanceof BallerinaModule) {
            // package
            item.setKind(CompletionItemKind.Module);
        }
        else if (bSymbol.getKind() == BallerinaSymbolKind.TYPE_DEF
                && ((BallerinaTypeDefinition) bSymbol).getTypeDescriptor().isPresent()) {
            TypeDescriptor typeDescriptor = ((BallerinaTypeDefinition) bSymbol).getTypeDescriptor().get();
//            if (bSymbol.getKind() == BallerinaSymbolKind.TYPE_DEF /*&& (BallerinaTypeDefinition) bSymbol*/) {
//                // Finite types
//                item.setKind(CompletionItemKind.TypeParameter);
//            }
            if (((BallerinaTypeDefinition) bSymbol).getTypeDescriptor().isPresent()
                    && ((BallerinaTypeDefinition) bSymbol).getTypeDescriptor().get().getKind() == TypeDescKind.UNION) {
                // Union types
                TypeDescriptor typeDesc = ((BallerinaTypeDefinition) bSymbol).getTypeDescriptor().get();
                List<TypeDescriptor> memberTypes = ((UnionTypeDescriptor) typeDesc).getMemberTypes();
                boolean allMatch = memberTypes.stream().allMatch(bType -> bType.getKind() == memberTypes.get(0).getKind());
                if (allMatch) {
                    switch (memberTypes.get(0).getKind()) {
                        case ERROR:
                            item.setKind(CompletionItemKind.Event);
                            break;
                        case RECORD:
                            item.setKind(CompletionItemKind.Struct);
                            break;
                        case OBJECT:
                            item.setKind(CompletionItemKind.Interface);
                            break;
                        default:
                            break;
                    }
                } else {
                    item.setKind(CompletionItemKind.Enum);
                }
            } else if (typeDescriptor.getKind() == TypeDescKind.RECORD) {
                item.setKind(CompletionItemKind.Struct);
            } else if (typeDescriptor.getKind() == TypeDescKind.OBJECT) {
                item.setKind(CompletionItemKind.Interface);
            }  else if (typeDescriptor.getKind() == TypeDescKind.ERROR) {
                item.setKind(CompletionItemKind.Event);
            }
    //        else if (bSymbol.kind != null) {
    //            // class / objects
    //            item.setKind(CompletionItemKind.Class);
    //        } 
            else {
                // default
                item.setKind(CompletionItemKind.Unit);
            }

            // set sub bType
            String name = typeDescriptor.getKind() == TypeDescKind.BUILTIN ? typeDescriptor.getSignature()
                    : typeDescriptor.getKind().name();
            String detail = name.substring(0, 1).toUpperCase(Locale.ENGLISH)
                    + name.substring(1).toLowerCase(Locale.ENGLISH);
            item.setDetail(detail);
        }
        if (bSymbol.getDocAttachment().isPresent() && bSymbol.getDocAttachment().get().getDescription().isPresent()) {
            item.setDocumentation(bSymbol.getDocAttachment().get().getDescription().get());
        }
    }
}
