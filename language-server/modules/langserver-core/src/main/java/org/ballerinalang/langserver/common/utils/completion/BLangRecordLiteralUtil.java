/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.langserver.common.utils.completion;

import org.antlr.v4.runtime.CommonToken;
import org.ballerina.compiler.api.model.BCompiledSymbol;
import org.ballerina.compiler.api.model.BallerinaField;
import org.ballerina.compiler.api.model.BallerinaFunctionSymbol;
import org.ballerina.compiler.api.model.BallerinaSymbolKind;
import org.ballerina.compiler.api.model.BallerinaVariable;
import org.ballerina.compiler.api.types.MapTypeDescriptor;
import org.ballerina.compiler.api.types.RecordTypeDescriptor;
import org.ballerina.compiler.api.types.TypeDescKind;
import org.ballerina.compiler.api.types.TypeDescriptor;
import org.ballerina.compiler.api.types.UnionTypeDescriptor;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.ballerinalang.langserver.completions.SymbolCompletionItem;
import org.ballerinalang.langserver.completions.builder.BFunctionCompletionItemBuilder;
import org.ballerinalang.langserver.completions.builder.BVariableCompletionItemBuilder;
import org.ballerinalang.langserver.sourceprune.SourcePruneKeys;
import org.eclipse.lsp4j.CompletionItem;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility operations on the BLangRecordLiterals.
 */
public class BLangRecordLiteralUtil {

    private static final String ELLIPSIS = "...";

    private BLangRecordLiteralUtil() {
    }

    /**
     * Get the record field completion items.
     *
     * @param context       Language server operation Context
     * @param recordLiteral Record Literal
     * @return {@link LSCompletionItem}   List of Completion Items
     */
    public static List<LSCompletionItem> getFieldsForMatchingRecord(LSContext context,
                                                                    BLangRecordLiteral recordLiteral) {
        BType expectedType = recordLiteral.expectedType;
        List<BCompiledSymbol> visibleSymbols = context.get(CommonKeys.VISIBLE_SYMBOLS_KEY);
        Optional<BType> evalType = expectedType instanceof BUnionType ? getRecordTypeFromUnionType(expectedType)
                : Optional.ofNullable(expectedType);
        if (!evalType.isPresent()) {
            return new ArrayList<>();
        }
        List<LSCompletionItem> completionItems = new ArrayList<>(getSpreadCompletionItems(context, evalType.get()));
        List<CommonToken> commonTokens = context.get(SourcePruneKeys.LHS_DEFAULT_TOKENS_KEY);
        String lastToken = (commonTokens.isEmpty()) ? "" : commonTokens.get(commonTokens.size() - 1).getText();
        long dotCount = lastToken.codePoints().filter(charVal -> charVal == '.').count();

        if (dotCount < 1 && evalType.get() instanceof BRecordType) {
            // Suggests fields only when the the ellipsis is not being typed
            List<BField> fields = new ArrayList<>(((BRecordType) evalType.get()).fields.values());
            // TODO: Fix This
//            completionItems.addAll(CommonUtil.getRecordFieldCompletionItems(context, fields));
//            completionItems.add(CommonUtil.getFillAllStructFieldsItem(context, fields));
//            completionItems.addAll(getVariableCompletionsForFields(context, visibleSymbols, fields));
        }

        return completionItems;
    }

    private static Optional<BType> getRecordTypeFromUnionType(BType bType) {
        if (!(bType instanceof BUnionType)) {
            return Optional.empty();
        }
        List<BType> filteredRecords = ((BUnionType) bType).getMemberTypes().stream()
                .filter(type -> type instanceof BRecordType)
                .collect(Collectors.toList());

        if (filteredRecords.size() == 1) {
            return Optional.ofNullable(filteredRecords.get(0));
        }
        return Optional.empty();
    }

//    private static List<LSCompletionItem> getVariableCompletionsForFields(LSContext ctx,
//                                                                          List<Scope.ScopeEntry> visibleSymbols,
//                                                                          List<BField> recordFields) {
//        Map<String, BType> filedTypeMap = new HashMap<>();
//        recordFields.forEach(bField -> filedTypeMap.put(bField.name.value, bField.type));
//        List<LSCompletionItem> completionItems = new ArrayList<>();
//        visibleSymbols.forEach(scopeEntry -> {
//            BSymbol symbol = scopeEntry.symbol;
//            BType type = symbol instanceof BConstantSymbol ? ((BConstantSymbol) symbol).literalType : symbol.type;
//            String symbolName = symbol.name.getValue();
//            if (filedTypeMap.containsKey(symbolName) && filedTypeMap.get(symbolName) == type
//                    && symbol instanceof BVarSymbol) {
//                String bTypeName = CommonUtil.getBTypeName(type, ctx, false);
//                CompletionItem cItem = BVariableCompletionItemBuilder.build((BVarSymbol) symbol, symbolName, bTypeName);
//                completionItems.add(new SymbolCompletionItem(ctx, symbol, cItem));
//            }
//        });
//
//        return completionItems;
//    }

    private static List<LSCompletionItem> getSpreadCompletionItems(LSContext context, BType evalType) {
        List<LSCompletionItem> completionItems = new ArrayList<>();
        List<BType> typeList = new ArrayList<>();
        // TODO: Fix this
//        List<BType> typeList = getTypeListForMapAndRecords(evalType);
        List<BCompiledSymbol> visibleSymbols = new ArrayList<>(context.get(CommonKeys.VISIBLE_SYMBOLS_KEY));
        visibleSymbols.removeIf(CommonUtil.invalidSymbolsPredicate());

        for (BCompiledSymbol visibleSymbol : visibleSymbols) {
//            BSymbol symbol = visibleSymbol.symbol;
            getSpreadableCompletionItem(context, visibleSymbol, typeList).ifPresent(completionItems::add);
        }

        return completionItems;
    }

    private static Optional<LSCompletionItem> getSpreadableCompletionItem(LSContext context, BCompiledSymbol symbol,
                                                                          List<BType> refTypeList) {
        Optional<TypeDescriptor> bType;
        if (symbol.getKind() == BallerinaSymbolKind.FUNCTION) {
            bType = ((BallerinaFunctionSymbol) symbol).getReturnType();
        } else if (symbol.getKind() == BallerinaSymbolKind.VARIABLE) {
            bType = ((BallerinaVariable) symbol).getTypeDescriptor();
        } else {
            return Optional.empty();
        }

        List<TypeDescriptor> symbolTypeList = getTypeListForMapAndRecords(bType.get());
        // if bType is not a map or record, then the symbol type list is empty 
        boolean canSpread = !symbolTypeList.isEmpty() && refTypeList.containsAll(symbolTypeList);

        CompletionItem cItem;
        if (canSpread && symbol.getKind() == BallerinaSymbolKind.FUNCTION) {
            cItem = BFunctionCompletionItemBuilder.build((BallerinaFunctionSymbol) symbol, context);
        } else if (canSpread) {
            cItem = BVariableCompletionItemBuilder.build((BallerinaVariable) symbol, symbol.getName(),
                    bType.get().getSignature());
        } else {
            return Optional.empty();
        }
        // Modify the spread completion item to automatically determine the prefixed number of dots to complete ellipsis
        modifySpreadCompletionItem(context, cItem);

        return Optional.of(new SymbolCompletionItem(context, symbol, cItem));
    }

    private static List<TypeDescriptor> getTypeListForMapAndRecords(TypeDescriptor bType) {
        if (bType.getKind() == TypeDescKind.MAP
                && ((MapTypeDescriptor) bType).getMemberTypeDescriptor().isPresent()) {
            TypeDescriptor constraint = ((MapTypeDescriptor) bType).getMemberTypeDescriptor().get();
            if (bType.getKind() == TypeDescKind.UNION) {
                return new ArrayList<>(((UnionTypeDescriptor) constraint).getMemberTypes());
            }
            return Collections.singletonList(constraint);
        } else if (bType.getKind() == TypeDescKind.RECORD) {
            return ((RecordTypeDescriptor) bType).getFieldDescriptors().stream()
                    .map(BallerinaField::getTypeDescriptor)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private static void modifySpreadCompletionItem(LSContext context, CompletionItem cItem) {
        List<CommonToken> commonTokens = context.get(SourcePruneKeys.LHS_DEFAULT_TOKENS_KEY);
        String lastToken = (commonTokens.isEmpty()) ? "" : commonTokens.get(commonTokens.size() - 1).getText();
        long dotCount = lastToken.codePoints().filter(charVal -> charVal == '.').count();
        String prefix = String.join("", Collections.nCopies(ELLIPSIS.length() - Math.toIntExact(dotCount), "."));

        cItem.setInsertText(prefix + cItem.getInsertText());
        cItem.setLabel(ELLIPSIS + cItem.getLabel());
    }
}
