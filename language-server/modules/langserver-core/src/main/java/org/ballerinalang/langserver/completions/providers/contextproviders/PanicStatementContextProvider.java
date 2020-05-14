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
import org.ballerina.compiler.api.model.BallerinaVariable;
import org.ballerina.compiler.api.types.TypeDescKind;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.ballerinalang.langserver.completions.providers.AbstractCompletionProvider;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Completion Item Resolver for the panic statement context.
 */
@JavaSPIService("org.ballerinalang.langserver.commons.completion.spi.LSCompletionProvider")
public class PanicStatementContextProvider extends AbstractCompletionProvider {
    public PanicStatementContextProvider() {
        this.attachmentPoints.add(BallerinaParser.PanicStatementContext.class);
    }

    @Override
    public List<LSCompletionItem> getCompletions(LSContext context) {
        List<BCompiledSymbol> visibleSymbols = new ArrayList<>(context.get(CommonKeys.VISIBLE_SYMBOLS_KEY));
        List<BCompiledSymbol> filteredList = visibleSymbols.stream()
                .filter(symbol -> symbol instanceof BallerinaVariable
                        && ((BallerinaVariable) symbol).getTypeDescriptor().isPresent()
                        && CommonUtil.getRawType(((BallerinaVariable) symbol).getTypeDescriptor().get()).getKind()
                        == TypeDescKind.ERROR)
                .collect(Collectors.toList());

        return this.getCompletionItemList(new ArrayList<>(filteredList), context);
    }
}
