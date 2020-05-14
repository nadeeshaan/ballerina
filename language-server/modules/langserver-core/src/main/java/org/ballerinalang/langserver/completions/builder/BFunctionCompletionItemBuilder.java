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

import org.apache.commons.lang3.tuple.Pair;
import org.ballerina.compiler.api.model.BallerinaFunctionSymbol;
import org.ballerina.compiler.api.model.BallerinaParameter;
import org.ballerina.compiler.api.model.DocAttachment;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.common.utils.FunctionGenerator;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.CompletionKeys;
import org.ballerinalang.langserver.completions.util.ItemResolverConstants;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * This class is being used to build function type completion item.
 *
 * @since 0.983.0
 */
public final class BFunctionCompletionItemBuilder {
    private BFunctionCompletionItemBuilder() {
    }

    /**
     * Creates and returns a completion item.
     *
     * @param bSymbol    BSymbol or null
     * @param label      label
     * @param insertText insert text
     * @param context {@link LSContext}
     * @return {@link CompletionItem}
     */
    public static CompletionItem build(BallerinaFunctionSymbol bSymbol, String label, String insertText,
                                       LSContext context) {
        CompletionItem item = new CompletionItem();
        item.setLabel(label);
        item.setInsertText(insertText);
        setMeta(item, bSymbol, context);
        return item;
    }

    /**
     * Creates and returns a completion item.
     *
     * @param functionSymbol function symbol to evaluate
     * @param context LS context
     * @return {@link CompletionItem}
     */
    public static CompletionItem build(BallerinaFunctionSymbol functionSymbol, LSContext context) {
        CompletionItem item = new CompletionItem();
        setMeta(item, functionSymbol, context);
        if (functionSymbol != null) {
            // Override function signature
            String functionName = CommonUtil.getFunctionNameFromSymbol(functionSymbol);
            Pair<String, String> functionSignature = CommonUtil.getFunctionInvocationSignature(functionSymbol,
                    functionName, context);
            item.setInsertText(functionSignature.getLeft());
            item.setLabel(functionSignature.getRight());
        }
        return item;
    }

    private static void setMeta(CompletionItem item, BallerinaFunctionSymbol functionSymbol, LSContext ctx) {
        item.setInsertTextFormat(InsertTextFormat.Snippet);
        item.setDetail(ItemResolverConstants.FUNCTION_TYPE);
        item.setKind(CompletionItemKind.Function);
        if (functionSymbol != null) {
            List<String> funcArguments = FunctionGenerator.getFuncArguments(functionSymbol, ctx);
            if (!funcArguments.isEmpty()) {
                Command cmd = new Command("editor.action.triggerParameterHints", "editor.action.triggerParameterHints");
                item.setCommand(cmd);
            }
            int invocationType = (ctx == null || ctx.get(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY) == null) ? -1
                    : ctx.get(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY);
            boolean skipFirstParam = CommonUtil.skipFirstParam(functionSymbol, invocationType);
            if (functionSymbol.getDocAttachment().isPresent()) {
                item.setDocumentation(getDocumentation(functionSymbol, skipFirstParam, ctx));
            }
        }
    }

    private static Either<String, MarkupContent> getDocumentation(BallerinaFunctionSymbol functionSymbol,
                                                                  boolean skipFirstParam, LSContext ctx) {
        String pkgID = functionSymbol.getModuleID().toString();

        Optional<DocAttachment> docAttachment = functionSymbol.getDocAttachment();
        String description = docAttachment.isPresent() && docAttachment.get().getDescription().isPresent()
                ? docAttachment.get().getDescription().get() : "";
        Map<String, String> docParamsMap = docAttachment.isPresent() ? docAttachment.get().getParameterMap()
                : new HashMap<>();
//        for (MarkdownDocAttachment.Parameter parameter : docAttachment.parameters) {
//            docParamsMap.put(parameter.name, parameter.description);
//        }
        List<BallerinaParameter> defaultParams = functionSymbol.getParams()
                .stream()
                .filter(BallerinaParameter::isDefaultable)
                .collect(Collectors.toList());

        MarkupContent docMarkupContent = new MarkupContent();
        docMarkupContent.setKind(CommonUtil.MARKDOWN_MARKUP_KIND);
        String documentation = "**Package:** " + "_" + pkgID + "_" + CommonUtil.MD_LINE_SEPARATOR
                + CommonUtil.MD_LINE_SEPARATOR + description + CommonUtil.MD_LINE_SEPARATOR;
        StringJoiner joiner = new StringJoiner(CommonUtil.MD_LINE_SEPARATOR);
        List<BallerinaParameter> functionParameters = new ArrayList<>(functionSymbol.getParams());
        if (functionSymbol.getRestParam().isPresent()) {
            functionParameters.add(functionSymbol.getRestParam().get());
        }
        for (int i = 0; i < functionParameters.size(); i++) {
            BallerinaParameter balParameter = functionParameters.get(i);
            String paramType = /*CommonUtil.getBTypeName(balParameter..type, ctx, false);*/
                    balParameter.getTypeDescriptor().getSignature();
            if (i == 0 && skipFirstParam) {
                continue;
            }

            Optional<BallerinaParameter> defaultVal = defaultParams.stream()
                    .filter(parameter -> parameter.getName().get().equals(balParameter.getName().get()))
                    .findFirst();
            String paramDescription = "- " + "`" + paramType + "` " + balParameter.getName().get();
            if (docParamsMap.containsKey(balParameter.getName().get())) {
                paramDescription += ": " + docParamsMap.get(balParameter.getName().get());
            }
            if (defaultVal.isPresent()) {
                joiner.add(paramDescription + "(Defaultable)");
            } else {
                joiner.add(paramDescription);
            }
        }
        String paramsStr = joiner.toString();
        if (!paramsStr.isEmpty()) {
            documentation += "**Params**" + CommonUtil.MD_LINE_SEPARATOR + paramsStr;
        }
        if (functionSymbol.getReturnType().isPresent()) {
            String desc = "";
            if (docAttachment.get().getReturnDescription().isPresent()
                    && !docAttachment.get().getReturnDescription().get().isEmpty()) {
                desc = "- " + CommonUtil.MD_NEW_LINE_PATTERN.matcher(docAttachment.get().getReturnDescription().get())
                        .replaceAll(CommonUtil.MD_LINE_SEPARATOR) + CommonUtil.MD_LINE_SEPARATOR;
            }
            documentation += CommonUtil.MD_LINE_SEPARATOR + CommonUtil.MD_LINE_SEPARATOR + "**Returns**"
                    + " `" + functionSymbol.getReturnType().get().getSignature() + "` " +
                    CommonUtil.MD_LINE_SEPARATOR + desc + CommonUtil.MD_LINE_SEPARATOR;
        }
        docMarkupContent.setValue(documentation);

        return Either.forRight(docMarkupContent);
    }
}
