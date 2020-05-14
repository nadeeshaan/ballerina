/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.langserver.completions.providers.scopeproviders;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.ballerina.compiler.api.model.BCompiledSymbol;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.CompletionKeys;
import org.ballerinalang.langserver.commons.completion.LSCompletionException;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.ballerinalang.langserver.commons.completion.spi.LSCompletionProvider;
import org.ballerinalang.langserver.completions.CompletionSubRuleParser;
import org.ballerinalang.langserver.completions.providers.AbstractCompletionProvider;
import org.ballerinalang.langserver.completions.providers.contextproviders.AnnotationAttachmentContextProvider;
import org.ballerinalang.langserver.completions.util.filters.DelimiterBasedContentFilter;
import org.ballerinalang.langserver.completions.util.filters.SymbolFilters;
import org.ballerinalang.langserver.sourceprune.SourcePruneKeys;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParser;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangTestablePackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves all items that can appear as a top level element in the file.
 * 
 * @since 0.995.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.completion.spi.LSCompletionProvider")
public class TopLevelScopeProvider extends AbstractCompletionProvider {
    public TopLevelScopeProvider() {
        this.attachmentPoints.add(BLangPackage.class);
        this.attachmentPoints.add(BLangTestablePackage.class);
    }

    @Override
    public List<LSCompletionItem> getCompletions(LSContext ctx) throws LSCompletionException {
        ArrayList<LSCompletionItem> completionItems = new ArrayList<>();
        List<CommonToken> lhsTokens = ctx.get(SourcePruneKeys.LHS_TOKENS_KEY);
        Optional<String> subRule = this.getSubRule(lhsTokens);
        subRule.ifPresent(rule -> CompletionSubRuleParser.parseWithinCompilationUnit(rule, ctx));

        if (this.inFunctionReturnParameterContext(ctx)) {
            return this.getProvider(BallerinaParser.ReturnParameterContext.class).getCompletions(ctx);
        }

        Optional<LSCompletionProvider> contextProvider = this.getContextProvider(ctx);
        List<CommonToken> lhsDefaultTokens = ctx.get(SourcePruneKeys.LHS_DEFAULT_TOKENS_KEY);
        ParserRuleContext parserRuleContext = ctx.get(CompletionKeys.PARSER_RULE_CONTEXT_KEY);
        Boolean forcedRemoved = ctx.get(SourcePruneKeys.FORCE_REMOVED_STATEMENT_WITH_PARENTHESIS_KEY);

        if (forcedRemoved != null && forcedRemoved) {
            return this.getCompletionOnParameterContext(ctx);
        }
        if (parserRuleContext instanceof BallerinaParser.ConstantDefinitionContext) {
            return completionItems;
        }
        if (contextProvider.isPresent()) {
            return contextProvider.get().getCompletions(ctx);
        }

        if (!(lhsDefaultTokens != null && lhsDefaultTokens.size() >= 2
                && BallerinaParser.LT == lhsDefaultTokens.get(lhsDefaultTokens.size() - 1).getType())) {
            completionItems.addAll(addTopLevelItems(ctx));
        }
        List<BCompiledSymbol> visibleSymbols = new ArrayList<>(ctx.get(CommonKeys.VISIBLE_SYMBOLS_KEY));
        completionItems.addAll(getBasicTypesItems(ctx, visibleSymbols));
        completionItems.addAll(this.getPackagesCompletionItems(ctx));

        return completionItems;
    }

    private List<LSCompletionItem> getCompletionOnParameterContext(LSContext lsContext) {
        List<Integer> defaultTokenTypes = lsContext.get(SourcePruneKeys.LHS_DEFAULT_TOKEN_TYPES_KEY);
        List<CommonToken> defaultTokens = lsContext.get(SourcePruneKeys.LHS_DEFAULT_TOKENS_KEY);
        List<BCompiledSymbol> visibleSymbols = new ArrayList<>(lsContext.get(CommonKeys.VISIBLE_SYMBOLS_KEY));
        Integer invocationType = lsContext.get(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY);
        if (defaultTokenTypes.contains(BallerinaParser.FUNCTION)) {
            if (invocationType == BallerinaParser.COLON) {
                String pkgAlias = defaultTokens.get(defaultTokenTypes.indexOf(invocationType) - 1).getText();
                return this.getTypeItemsInPackage(new ArrayList<>(visibleSymbols), pkgAlias, lsContext);
            }
            if (invocationType > -1) {
                return new ArrayList<>();
            }
            /*
            Within the function definition's parameter context and we only suggest the packages and types 
             */
            List<LSCompletionItem> completionItems = new ArrayList<>();
            completionItems.addAll(getBasicTypesItems(lsContext, visibleSymbols));
            completionItems.addAll(this.getPackagesCompletionItems(lsContext));
            
            return completionItems;
        }
        
        if (invocationType > -1) {
            Either<List<LSCompletionItem>, List<BCompiledSymbol>> filteredSymbols =
                    SymbolFilters.get(DelimiterBasedContentFilter.class).filterItems(lsContext);
            return this.getCompletionItemList(filteredSymbols, lsContext);
        }

        List<LSCompletionItem> completionItems = new ArrayList<>();
        // TODO: Fix this
//        visibleSymbols.removeIf(this.attachedSymbolFilter());
        completionItems.addAll(getBasicTypesItems(lsContext, visibleSymbols));
        completionItems.addAll(this.getPackagesCompletionItems(lsContext));
        return completionItems;
    }

    @Override
    public Optional<LSCompletionProvider> getContextProvider(LSContext ctx) {
        List<Integer> lhsTokensTypes = ctx.get(SourcePruneKeys.LHS_DEFAULT_TOKEN_TYPES_KEY);
        Boolean forcedRemoved = ctx.get(SourcePruneKeys.FORCE_REMOVED_STATEMENT_WITH_PARENTHESIS_KEY);
        if (lhsTokensTypes == null || lhsTokensTypes.isEmpty() || (forcedRemoved != null && forcedRemoved)) {
            return Optional.empty();
        }
        if (this.isAnnotationAttachmentContext(ctx)) {
            return Optional.ofNullable(this.getProvider(AnnotationAttachmentContextProvider.class));
        }
        // Handle with the parser rule context
        int serviceTokenIndex = lhsTokensTypes.indexOf(BallerinaParser.SERVICE);
        int assignTokenIndex = lhsTokensTypes.indexOf(BallerinaParser.ASSIGN);
        ParserRuleContext parserRuleContext = ctx.get(CompletionKeys.PARSER_RULE_CONTEXT_KEY);

        if (serviceTokenIndex > -1 && assignTokenIndex == -1) {
            return Optional.ofNullable(this.getProvider(BallerinaParser.ServiceDefinitionContext.class));
        }

        if (parserRuleContext != null) {
            return Optional.ofNullable(this.getProvider(parserRuleContext.getClass()));
        }
        return super.getContextProvider(ctx);
    }
}
