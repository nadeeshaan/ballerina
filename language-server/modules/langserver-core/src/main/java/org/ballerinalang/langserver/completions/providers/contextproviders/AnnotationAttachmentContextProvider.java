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
package org.ballerinalang.langserver.completions.providers.contextproviders;

import org.antlr.v4.runtime.CommonToken;
import org.ballerina.compiler.api.model.AnnotationAttachPoint;
import org.ballerina.compiler.api.model.BallerinaAnnotationSymbol;
import org.ballerina.compiler.api.model.ModuleID;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.LSAnnotationCache;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.AnnotationNodeKind;
import org.ballerinalang.langserver.commons.completion.CompletionKeys;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.completions.providers.AbstractCompletionProvider;
import org.ballerinalang.langserver.sourceprune.SourcePruneKeys;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.tree.NodeKind;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParser;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAnnotationSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotation;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.util.AttachPoints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Annotation Attachment Resolver to resolve the corresponding annotation attachments.
 */
@JavaSPIService("org.ballerinalang.langserver.commons.completion.spi.LSCompletionProvider")
public class AnnotationAttachmentContextProvider extends AbstractCompletionProvider {

    public AnnotationAttachmentContextProvider() {
        this.attachmentPoints.add(AnnotationAttachmentContextProvider.class);
    }

    @Override
    public List<LSCompletionItem> getCompletions(LSContext ctx) {
        List<Integer> rhsTokenTypes = ctx.get(SourcePruneKeys.RHS_DEFAULT_TOKEN_TYPES_KEY);
        AnnotationNodeKind annotationNodeKind = ctx.get(CompletionKeys.NEXT_NODE_KEY);
        if (annotationNodeKind == null && rhsTokenTypes.contains(BallerinaParser.EXTERNAL)) {
            annotationNodeKind = AnnotationNodeKind.EXTERNAL;
        } else if (annotationNodeKind == null) {
            return new ArrayList<>();
        }
        return filterAnnotations(annotationNodeKind, ctx);
    }

    /**
     * Filter the annotations from the data model.
     * 
     * @return {@link List}
     */
    private List<LSCompletionItem> filterAnnotations(AnnotationNodeKind attachmentPoint, LSContext ctx) {
        List<LSCompletionItem> completionItems = new ArrayList<>();
        List<Integer> lhsTokenTypes = ctx.get(SourcePruneKeys.LHS_DEFAULT_TOKEN_TYPES_KEY);
        List<CommonToken> lhsDefaultTokens = ctx.get(SourcePruneKeys.LHS_DEFAULT_TOKENS_KEY);
        Map<String, String> pkgAliasMap = ctx.get(DocumentServiceKeys.CURRENT_DOC_IMPORTS_KEY).stream()
                .collect(Collectors.toMap(pkg -> pkg.symbol.pkgID.toString(), pkg -> pkg.alias.value));
        CommonToken pkgAlias = null;
        if (lhsTokenTypes == null) {
            return completionItems;
        }
        int atTokenIndex = lhsTokenTypes.indexOf(BallerinaParser.AT);
        int colonTokenIndex = lhsTokenTypes.indexOf(BallerinaParser.COLON);
        if (atTokenIndex == -1) {
            return completionItems;
        }
        if (colonTokenIndex > 0) {
            pkgAlias = lhsDefaultTokens.get(colonTokenIndex - 1);
        }

        CommonToken finalAlias = pkgAlias;
        LSAnnotationCache.getInstance().getAnnotationMapForType(attachmentPoint, ctx)
                .forEach((key, value) -> value.forEach(annotation -> {
                    String annotationPkgAlias = annotation.pkgID.nameComps
                            .get(annotation.pkgID.nameComps.size() - 1).value;
                    String annotationPkg = annotation.pkgID.toString();
                    // compare with the import statements' package alias
                    if (finalAlias == null || finalAlias.getText().equals(annotationPkgAlias)
                            || finalAlias.getText().equals(pkgAliasMap.get(annotationPkg))) {
                        // todo: Fix
//                        completionItems.add(CommonUtil.getAnnotationCompletionItem(key, annotation, ctx, finalAlias,
//                                pkgAliasMap));
                    }
                }));
        
        completionItems.addAll(this.getAnnotationsInModule(ctx, attachmentPoint, pkgAliasMap));
        
        return completionItems;
    }
    
    private List<LSCompletionItem> getAnnotationsInModule(LSContext ctx, AnnotationNodeKind kind,
                                                        Map<String, String> pkgAliasMap) {
        List<LSCompletionItem> completionItems = new ArrayList<>();
        List<BallerinaAnnotationSymbol> annotations = CommonUtil.getAnnotationsInModule(ctx);
        BLangNode scopeNode = ctx.get(CompletionKeys.SCOPE_NODE_KEY);

        annotations.forEach(symbol -> {
            ModuleID pkgId = symbol.getModuleID();
            List<AnnotationAttachPoint> maskedPoints = symbol.getAttachPoints();
            switch (kind) {
                case ANNOTATION:
                    if (maskedPoints.contains(AnnotationAttachPoint.ANNOTATION)) {
                        completionItems.add(CommonUtil.getAnnotationCompletionItem(pkgId, symbol, ctx, pkgAliasMap));
                    }
                    break;
                case FUNCTION:
                    if (maskedPoints.contains(AnnotationAttachPoint.FUNCTION)
                            || (maskedPoints.contains(AnnotationAttachPoint.OBJECT_METHOD)
                            && scopeNode.getKind() == NodeKind.OBJECT_TYPE)) {
                        completionItems.add(CommonUtil.getAnnotationCompletionItem(pkgId, symbol, ctx, pkgAliasMap));
                    }
                    break;
                case LISTENER:
                    if (maskedPoints.contains(AnnotationAttachPoint.LISTENER)) {
                        completionItems.add(CommonUtil.getAnnotationCompletionItem(pkgId, symbol, ctx, pkgAliasMap));
                    }
                    break;
                case OBJECT:
                    if (maskedPoints.contains(AnnotationAttachPoint.TYPE)
                            || maskedPoints.contains(AnnotationAttachPoint.OBJECT)) {
                        completionItems.add(CommonUtil.getAnnotationCompletionItem(pkgId, symbol, ctx, pkgAliasMap));
                    }
                    break;
                case RESOURCE:
                    if (maskedPoints.contains(AnnotationAttachPoint.RESOURCE)
                            || maskedPoints.contains(AnnotationAttachPoint.FUNCTION)) {
                        completionItems.add(CommonUtil.getAnnotationCompletionItem(pkgId, symbol, ctx, pkgAliasMap));
                    }
                    break;
                case SERVICE:
                    if (maskedPoints.contains(AnnotationAttachPoint.SERVICE)) {
                        completionItems.add(CommonUtil.getAnnotationCompletionItem(pkgId, symbol, ctx, pkgAliasMap));
                    }
                    break;
                case RECORD:
                case TYPE:
                    if (maskedPoints.contains(AnnotationAttachPoint.TYPE)) {
                        completionItems.add(CommonUtil.getAnnotationCompletionItem(pkgId, symbol, ctx, pkgAliasMap));
                    }
                    break;
                case WORKER:
                    if (maskedPoints.contains(AnnotationAttachPoint.WORKER)) {
                        completionItems.add(CommonUtil.getAnnotationCompletionItem(pkgId, symbol, ctx, pkgAliasMap));
                    }
                    break;
                default:
                    break;
            }
        });
        
        return completionItems;
    }
}
