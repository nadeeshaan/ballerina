/*
*  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.ballerina.compiler.api.model.BCompiledSymbol;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.CompletionKeys;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.ballerinalang.langserver.completions.SnippetCompletionItem;
import org.ballerinalang.langserver.completions.providers.AbstractCompletionProvider;
import org.ballerinalang.langserver.completions.util.Snippet;
import org.ballerinalang.langserver.completions.util.filters.DelimiterBasedContentFilter;
import org.ballerinalang.langserver.completions.util.filters.SymbolFilters;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Completion provider for the return parser rule context.
 * 
 * @since 1.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.completion.spi.LSCompletionProvider")
public class ReturnStatementContextProvider extends AbstractCompletionProvider {

    public ReturnStatementContextProvider() {
        this.attachmentPoints.add(BallerinaParser.ReturnStatementContext.class);
    }

    @Override
    public List<LSCompletionItem> getCompletions(LSContext ctx) {
        ArrayList<LSCompletionItem> completionItems = new ArrayList<>();
        List<BCompiledSymbol> visibleSymbols = new ArrayList<>(ctx.get(CommonKeys.VISIBLE_SYMBOLS_KEY));
        Integer invocationType = ctx.get(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY);
        
        if (invocationType > -1) {
            Either<List<LSCompletionItem>, List<BCompiledSymbol>> filteredItems =
                    SymbolFilters.get(DelimiterBasedContentFilter.class).filterItems(ctx);
            return this.getCompletionItemList(filteredItems, ctx);
        }
        // Remove the functions without a receiver symbol, bTypes not being packages and attached functions
        // TODO: Fix
//        List<Scope.ScopeEntry> filteredList = visibleSymbols.stream().filter(symbol -> {
//            return !((bSymbol instanceof BInvokableSymbol
//                    && ((BInvokableSymbol) bSymbol).receiverSymbol != null
//                    && CommonUtil.isValidInvokableSymbol(bSymbol))
//                    || ((bSymbol instanceof BTypeSymbol)
//                    && !(bSymbol instanceof BPackageSymbol))
//                    || (bSymbol instanceof BInvokableSymbol
//                    && ((bSymbol.flags & Flags.ATTACHED) == Flags.ATTACHED)));
//        }).collect(Collectors.toList());

        completionItems.add(new SnippetCompletionItem(ctx, Snippet.KW_CHECK_PANIC.get()));
        completionItems.add(new SnippetCompletionItem(ctx, Snippet.KW_CHECK.get()));
//        completionItems.addAll(getCompletionItemList(filteredList, ctx));
        completionItems.addAll(this.getPackagesCompletionItems(ctx));
        
        return completionItems;
    }
}
