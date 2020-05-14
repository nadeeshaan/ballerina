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
import org.antlr.v4.runtime.Token;
import org.ballerina.compiler.api.model.BCompiledSymbol;
import org.ballerina.compiler.api.model.BallerinaFunctionSymbol;
import org.ballerina.compiler.api.model.BallerinaSymbolKind;
import org.ballerina.compiler.api.model.BallerinaVariable;
import org.ballerina.compiler.api.types.TypeDescKind;
import org.ballerina.compiler.api.types.TypeDescriptor;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.FilterUtils;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.CompletionKeys;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.ballerinalang.langserver.completions.SymbolCompletionItem;
import org.ballerinalang.langserver.completions.builder.BFunctionCompletionItemBuilder;
import org.ballerinalang.langserver.completions.builder.BTypeCompletionItemBuilder;
import org.ballerinalang.langserver.completions.builder.BVariableCompletionItemBuilder;
import org.ballerinalang.langserver.completions.providers.AbstractCompletionProvider;
import org.ballerinalang.langserver.completions.util.filters.DelimiterBasedContentFilter;
import org.ballerinalang.langserver.completions.util.filters.SymbolFilters;
import org.ballerinalang.langserver.sourceprune.SourcePruneKeys;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.ballerinalang.langserver.common.utils.CommonUtil.LINE_SEPARATOR;
import static org.ballerinalang.langserver.completions.util.MatchStatementResolverUtil.generateMatchPattern;
import static org.ballerinalang.langserver.completions.util.MatchStatementResolverUtil.getStructuredFixedValueMatch;
import static org.ballerinalang.langserver.completions.util.MatchStatementResolverUtil.getVariableValueDestructurePattern;

/**
 * Completion Item provider for the match statement parser rule context.
 */
@JavaSPIService("org.ballerinalang.langserver.commons.completion.spi.LSCompletionProvider")
public class MatchStatementContextProvider extends AbstractCompletionProvider {

    public MatchStatementContextProvider() {
        this.attachmentPoints.add(BallerinaParser.MatchStatementContext.class);
    }

    @Override
    public List<LSCompletionItem> getCompletions(LSContext ctx) {
        ArrayList<LSCompletionItem> completionItems = new ArrayList<>();
        List<CommonToken> defaultTokens = ctx.get(SourcePruneKeys.LHS_TOKENS_KEY).stream()
                .filter(commonToken -> commonToken.getChannel() == Token.DEFAULT_CHANNEL)
                .collect(Collectors.toList());
        List<Integer> defaultTokenTypes = defaultTokens.stream().map(CommonToken::getType).collect(Collectors.toList());
        List<BCompiledSymbol> visibleSymbols = new ArrayList<>(ctx.get(CommonKeys.VISIBLE_SYMBOLS_KEY));
        int delimiter = ctx.get(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY);
        if (delimiter == BallerinaParser.COLON) {
            Either<List<LSCompletionItem>, List<BCompiledSymbol>> moduleContent =
                    SymbolFilters.get(DelimiterBasedContentFilter.class).filterItems(ctx);
            return this.getCompletionItemList(moduleContent, ctx);
        } else if (delimiter > -1) {
            String varName = defaultTokens.get(defaultTokenTypes.indexOf(delimiter) - 1).getText();
            List<? extends BCompiledSymbol> filteredList = FilterUtils.filterVariableEntriesOnDelimiter(ctx, varName,
                    delimiter, defaultTokens, defaultTokenTypes.lastIndexOf(delimiter));
            filteredList.removeIf(CommonUtil.invalidSymbolsPredicate());
            filteredList.forEach(symbol -> {
                if (symbol.getKind() == BallerinaSymbolKind.FUNCTION) {
//                    BSymbol scopeEntrySymbol = scopeEntry.symbol;
                    completionItems.add(this.fillInvokableSymbolMatchSnippet((BallerinaFunctionSymbol) symbol, ctx));
                }
            });
        } else {
            visibleSymbols.removeIf(CommonUtil.invalidSymbolsPredicate());
            visibleSymbols.forEach(symbol -> {
                if (symbol.getKind() == BallerinaSymbolKind.FUNCTION) {
                    completionItems.add(this.fillInvokableSymbolMatchSnippet((BallerinaFunctionSymbol) symbol, ctx));
                } else if (symbol instanceof BallerinaVariable
                        && symbol.getKind() != BallerinaSymbolKind.TYPE_DEF
                        && symbol.getKind() != BallerinaSymbolKind.ANNOTATION
                        && ((BallerinaVariable) symbol).getTypeDescriptor().isPresent()) {
                    fillVarSymbolMatchSnippet(ctx, (BallerinaVariable) symbol, completionItems);
                    String typeName = ((BallerinaVariable) symbol).getTypeDescriptor().get().getSignature();
                    CompletionItem cItem = BVariableCompletionItemBuilder.build((BallerinaVariable) symbol,
                            symbol.getName(), typeName);
                    completionItems.add(new SymbolCompletionItem(ctx, symbol, cItem));
                } else if (symbol.getKind() == BallerinaSymbolKind.MODULE) {
                    CompletionItem cItem = BTypeCompletionItemBuilder.build(symbol, symbol.getName());
                    completionItems.add(new SymbolCompletionItem(ctx, symbol, cItem));
                }
            });
        }
        return completionItems;
    }

    private LSCompletionItem getVariableCompletionItem(LSContext context, BallerinaVariable varSymbol,
                                                       String matchFieldSnippet, String label) {
        CompletionItem completionItem = BVariableCompletionItemBuilder.build(varSymbol, label,
                varSymbol.getTypeDescriptor().get().getSignature());
        completionItem.setInsertText(varSymbol.getName() + " " + matchFieldSnippet);
        completionItem.setInsertTextFormat(InsertTextFormat.Snippet);
        return new SymbolCompletionItem(context, varSymbol, completionItem);
    }

    private String getFunctionSignature(BallerinaFunctionSymbol func) {
        String[] nameComps = func.getName().split("\\.");
        StringBuilder signature = new StringBuilder(nameComps[nameComps.length - 1]);
        List<String> params = new ArrayList<>();
        signature.append(CommonKeys.OPEN_PARENTHESES_KEY);
        func.getParams().forEach(param -> {
            if (param.getName().isPresent()) {
                params.add(param.getName().get());
            }
        });
        signature.append(String.join(",", params)).append(")");

        return signature.toString();
    }

    private LSCompletionItem fillInvokableSymbolMatchSnippet(BallerinaFunctionSymbol func, LSContext ctx) {
        String functionSignature = getFunctionSignature(func);
        String variableValuePattern = getVariableValueDestructurePattern();
        String variableValueSnippet = this.generateMatchSnippet(variableValuePattern);

        CompletionItem completionItem = BFunctionCompletionItemBuilder.build(func, functionSignature,
                functionSignature + " " + variableValueSnippet, ctx);

        return new SymbolCompletionItem(ctx, func, completionItem);
    }

    private void fillVarSymbolMatchSnippet(LSContext context, BallerinaVariable varSymbol,
                                           List<LSCompletionItem> completionItems) {
        String varName = varSymbol.getName();
        if (!varSymbol.getTypeDescriptor().isPresent()) {
            return;
        }
        TypeDescriptor typeDesc = CommonUtil.getRawType(varSymbol.getTypeDescriptor().get());
        if (typeDesc.getKind() == TypeDescKind.TUPLE || typeDesc.getKind() == TypeDescKind.RECORD) {
            String fixedValuePattern = "\t" +
                    generateMatchPattern(getStructuredFixedValueMatch(typeDesc));
            String fixedValueSnippet = this.generateMatchSnippet(fixedValuePattern);
            completionItems.add(this.getVariableCompletionItem(context, varSymbol, fixedValueSnippet, varName));
        } else {
            String variableValuePattern = "\t" + getVariableValueDestructurePattern();
            String variableValueSnippet = this.generateMatchSnippet(variableValuePattern);
            completionItems.add(this.getVariableCompletionItem(context, varSymbol, variableValueSnippet, varName));
        }
    }

    private String generateMatchSnippet(String patternClause) {
        return CommonKeys.OPEN_BRACE_KEY + LINE_SEPARATOR + patternClause + LINE_SEPARATOR
                + CommonKeys.CLOSE_BRACE_KEY;
    }
}
