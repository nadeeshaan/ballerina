/*
 * Copyright (c) 2018, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.langserver.completions.util;

import io.ballerinalang.compiler.text.LinePosition;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.ballerina.compiler.api.model.BCompiledSymbol;
import org.ballerina.compiler.api.semantic.SemanticModel;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.CompletionKeys;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.ballerinalang.langserver.commons.completion.spi.LSCompletionProvider;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentManager;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.completions.LSCompletionProviderHolder;
import org.ballerinalang.langserver.completions.TreeVisitor;
import org.ballerinalang.langserver.completions.sourceprune.CompletionsTokenTraverserFactory;
import org.ballerinalang.langserver.completions.util.sorters.ItemSorters;
import org.ballerinalang.langserver.sourceprune.SourcePruneKeys;
import org.ballerinalang.langserver.sourceprune.SourcePruner;
import org.ballerinalang.langserver.sourceprune.TokenTraverserFactory;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParser;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.CompilerContext;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.ballerinalang.langserver.common.utils.CommonUtil.FILE_SEPARATOR;

/**
 * Common utility methods for the completion operation.
 */
public class CompletionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompletionUtil.class);

    /**
     * Resolve the visible symbols from the given BLang Package and the current context.
     *
     * @param completionContext     Completion Service Context
     */
    public static void resolveSymbols(LSContext completionContext) {
        // Visit the package to resolve the symbols
        TreeVisitor treeVisitor = new TreeVisitor(completionContext);
        BLangPackage bLangPackage = completionContext.get(DocumentServiceKeys.CURRENT_BLANG_PACKAGE_CONTEXT_KEY);
        bLangPackage.accept(treeVisitor);
        // TODO: Use following accordingly
        captureBCompiledSymbols(completionContext);
    }

    /**
     * Get the completion Items for the context.
     *
     * @param ctx Completion context
     * @return {@link List}         List of resolved completion Items
     */
    public static List<CompletionItem>  getCompletionItems(LSContext ctx) {
        List<LSCompletionItem> items = new ArrayList<>();
        if (ctx == null) {
            return new ArrayList<>();
        }
        // Set the invocation or field access token type
        setInvocationOrInteractionOrFieldAccessToken(ctx);
        BLangNode scope = ctx.get(CompletionKeys.SCOPE_NODE_KEY);
        Map<Class, LSCompletionProvider> scopeProviders = LSCompletionProviderHolder.getInstance().getProviders();
        LSCompletionProvider completionProvider = scopeProviders.get(scope.getClass());
        try {
            items.addAll(completionProvider.getCompletions(ctx));
        } catch (Exception e) {
            LOGGER.error("Error while retrieving completions from: " + completionProvider.getClass());
        }

        return getPreparedCompletionItems(ctx, items);
    }

    private static List<CompletionItem> getPreparedCompletionItems(LSContext context, List<LSCompletionItem> items) {
        List<CompletionItem> completionItems = new ArrayList<>();
        boolean isSnippetSupported = context.get(CompletionKeys.CLIENT_CAPABILITIES_KEY).getCompletionItem()
                .getSnippetSupport();
        List<CompletionItem> sortedItems = ItemSorters.get(context.get(CompletionKeys.SCOPE_NODE_KEY).getClass())
                .sortItems(context, items);

        // TODO: Remove this
        for (CompletionItem item : sortedItems) {
            if (!isSnippetSupported) {
                item.setInsertText(CommonUtil.getPlainTextSnippet(item.getInsertText()));
                item.setInsertTextFormat(InsertTextFormat.PlainText);
            } else {
                item.setInsertTextFormat(InsertTextFormat.Snippet);
            }
            completionItems.add(item);
        }

        return completionItems;
    }

    /**
     * Check whether the token stream corresponds to a action invocation or a function invocation.
     *
     * @param context Completion operation context
     */
    private static void setInvocationOrInteractionOrFieldAccessToken(LSContext context) {
        List<CommonToken> lhsTokens = context.get(SourcePruneKeys.LHS_TOKENS_KEY);
        List<Integer> invocationTokens = Arrays.asList(
                BallerinaParser.COLON, BallerinaParser.DOT, BallerinaParser.RARROW, BallerinaParser.NOT,
                BallerinaParser.OPTIONAL_FIELD_ACCESS
        );
        context.put(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY, -1);
        if (lhsTokens == null) {
            return;
        }
        List<CommonToken> lhsDefaultTokens = lhsTokens.stream()
                .filter(commonToken -> commonToken.getChannel() == Token.DEFAULT_CHANNEL)
                .collect(Collectors.toList());
        if (lhsDefaultTokens.isEmpty()) {
            return;
        }
        int lastToken = CommonUtil.getLastItem(lhsDefaultTokens).getType();
        int tokenBeforeLast = lhsDefaultTokens.size() >= 2 ?
                lhsDefaultTokens.get(lhsDefaultTokens.size() - 2).getType() : -1;
        int resultToken = -1;
        if (invocationTokens.contains(lastToken)) {
            resultToken = lastToken;
        } else if (lhsDefaultTokens.size() >= 2 && invocationTokens.contains(tokenBeforeLast)) {
            resultToken = tokenBeforeLast;
        }
        context.put(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY, resultToken);
    }

    /**
     * Prune source if syntax errors exists.
     *
     * @param lsContext {@link LSContext}
     * @throws SourcePruneException when file uri is invalid
     * @throws WorkspaceDocumentException when document read error occurs
     */
    public static void pruneSource(LSContext lsContext) throws SourcePruneException, WorkspaceDocumentException {
        WorkspaceDocumentManager documentManager = lsContext.get(DocumentServiceKeys.DOC_MANAGER_KEY);
        String uri = lsContext.get(DocumentServiceKeys.FILE_URI_KEY);
        if (uri == null) {
            throw new SourcePruneException("fileUri cannot be null!");
        }

        Path filePath = Paths.get(URI.create(uri));
        TokenTraverserFactory tokenTraverserFactory = new CompletionsTokenTraverserFactory(filePath, documentManager,
                                                                                           SourcePruner.newContext());
        SourcePruner.pruneSource(lsContext, tokenTraverserFactory);

        // Update document manager
        documentManager.setPrunedContent(filePath, tokenTraverserFactory.getTokenStream().getText());
    }
    
    private static void captureBCompiledSymbols(LSContext ctx) {
        String relativeFilePath = ctx.get(DocumentServiceKeys.RELATIVE_FILE_PATH_KEY);
        BLangPackage pkgNode = ctx.get(DocumentServiceKeys.CURRENT_BLANG_PACKAGE_CONTEXT_KEY);
        Optional<BLangCompilationUnit> filteredCUnit = pkgNode.compUnits.stream()
                .filter(cUnit ->
                        cUnit.getPosition().getSource().cUnitName.replace("/", FILE_SEPARATOR)
                                .equals(relativeFilePath))
                .findAny();
        if (filteredCUnit.isPresent()) {
            CompilerContext compilerContext = ctx.get(DocumentServiceKeys.COMPILER_CONTEXT_KEY);
            SemanticModel semanticModel = new SemanticModel(filteredCUnit.get(), pkgNode, compilerContext);
            ctx.put(CommonKeys.SEMANTIC_MODEL_KEY, semanticModel);
            Position pos = ctx.get(DocumentServiceKeys.POSITION_KEY).getPosition();
            List<BCompiledSymbol> compiledSymbols = semanticModel.lookupSymbols(new LinePosition(pos.getLine(),
                    pos.getCharacter()));
            if (compiledSymbols.isEmpty()) {
                throw new AssertionError("Empty Symbols");
            }
            ctx.put(CommonKeys.VISIBLE_SYMBOLS_KEY, compiledSymbols);
        }
    }
}
