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
package org.ballerinalang.langserver.common.utils;

import com.google.common.collect.Lists;
import org.antlr.v4.runtime.CommonToken;
import org.apache.commons.lang3.tuple.Pair;
import org.ballerina.compiler.api.model.AccessModifier;
import org.ballerina.compiler.api.model.BCompiledSymbol;
import org.ballerina.compiler.api.model.BallerinaField;
import org.ballerina.compiler.api.model.BallerinaFunctionSymbol;
import org.ballerina.compiler.api.model.BallerinaModule;
import org.ballerina.compiler.api.model.BallerinaObjectVarSymbol;
import org.ballerina.compiler.api.model.BallerinaSymbolKind;
import org.ballerina.compiler.api.model.BallerinaVariable;
import org.ballerina.compiler.api.semantic.BallerinaTypeDesc;
import org.ballerina.compiler.api.semantic.SemanticModel;
import org.ballerina.compiler.api.types.ObjectTypeDescriptor;
import org.ballerina.compiler.api.types.RecordTypeDescriptor;
import org.ballerina.compiler.api.types.TypeDescKind;
import org.ballerina.compiler.api.types.TypeDescriptor;
import org.ballerina.compiler.api.types.UnionTypeDescriptor;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.CompletionKeys;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.sourceprune.SourcePruneKeys;
import org.wso2.ballerinalang.compiler.parser.antlr4.BallerinaParser;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.tree.BLangFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.wso2.ballerinalang.compiler.semantics.model.Scope.NOT_FOUND_ENTRY;

/**
 * Utilities for filtering the symbols from completion context and symbol information lists.
 */
public class FilterUtils {

    /**
     * Get the invocations and fields against an identifier (functions, actions and fields).
     *
     * @param context       Text Document Service context (Completion Context)
     * @param varName       Variable name to evaluate against (Can be package alias or defined variable)
     * @param delimiter     delimiter type either dot or action invocation symbol
     * @param defaultTokens List of Default tokens
     * @param delimIndex    Delimiter index
     * @return {@link ArrayList}    List of filtered symbol info
     */
    public static List<? extends BCompiledSymbol> filterVariableEntriesOnDelimiter(LSContext context, String varName,
                                                                                   int delimiter,
                                                                                   List<CommonToken> defaultTokens,
                                                                                   int delimIndex) {
        List<BCompiledSymbol> visibleSymbols = new ArrayList<>(context.get(CommonKeys.VISIBLE_SYMBOLS_KEY));
        visibleSymbols.removeIf(CommonUtil.invalidSymbolsPredicate());
        if (BallerinaParser.RARROW == delimiter) {
            BCompiledSymbol variable = getVariableByName(varName, visibleSymbols);
            if (variable != null && variable.getKind() == BallerinaSymbolKind.CLIENT) {
                // Handling action invocations ep -> ...
                return ((BallerinaObjectVarSymbol) variable).getClientActions();
            }
            // Handling worker-interactions eg. msg -> ...
            visibleSymbols.removeIf(symbol -> symbol.getKind() == BallerinaSymbolKind.TYPE_DEF);
            return visibleSymbols;
        }
        if (BallerinaParser.LARROW == delimiter) {
            // Handling worker-interaction msg <- ...
            visibleSymbols.removeIf(symbol -> symbol.getKind() == BallerinaSymbolKind.TYPE_DEF);
            return visibleSymbols;
        }
        if (BallerinaParser.DOT == delimiter || BallerinaParser.OPTIONAL_FIELD_ACCESS == delimiter
                || BallerinaParser.NOT == delimiter) {
            return getInvocationsAndFields(context, defaultTokens, delimIndex);
        }
        if (BallerinaParser.COLON == delimiter) {
            String moduleName = varName;
            for (BLangImportPackage importModule : context.get(DocumentServiceKeys.CURRENT_DOC_IMPORTS_KEY)) {
                if (importModule.alias.getValue().equals(varName)) {
                    moduleName = CommonUtil.getSymbolName(importModule.symbol);
                    break;
                }
            }
            String finalModuleName = moduleName;
            Optional<BCompiledSymbol> pkgSymbol =
                    Optional.ofNullable(FilterUtils.getVariableByName(varName, visibleSymbols));

            if (!pkgSymbol.isPresent() || pkgSymbol.get().getKind() != BallerinaSymbolKind.MODULE) {
                return new ArrayList<>();
            }

//            Map<Name, BCompiledSymbol> scopeEntryMap = pkgSymbol.get().symbol.scope.entries;
//            List<BCompiledSymbol> functionsAndTypes
//                    = new ArrayList<>(((BallerinaModule) pkgSymbol.get()).getFunctions());
            List<BCompiledSymbol> functionsAndTypes = ((BallerinaModule) pkgSymbol.get()).getAllSymbols();
//            return loadActionsFunctionsAndTypesFromScope(scopeEntryMap);
            return functionsAndTypes;
        }

        return new ArrayList<>();
    }

    /**
     * Get the actions defined over and endpoint.
     *
     * @param objectTypeSymbol Endpoint variable symbol to evaluate
     * @return {@link List} List of extracted actions as Symbol Info
     */
    public static List<Scope.ScopeEntry> getClientActions(BObjectTypeSymbol objectTypeSymbol) {
        return objectTypeSymbol.methodScope.entries.values().stream()
                .filter(scopeEntry -> (scopeEntry.symbol.flags & Flags.REMOTE) == Flags.REMOTE)
                .collect(Collectors.toList());
    }

    /**
     * Get invocations and fields.
     * Get the invocations and fields for a dot separated chained statement
     * Eg: int x = var1.field1.testFunction()
     *
     * @param ctx           Language server operation context
     * @param defaultTokens List of default tokens removed (LHS Tokens)
     * @param delimIndex    Delimiter index
     * @return {@link List} List of extracted symbols
     */
    private static List<BCompiledSymbol> getInvocationsAndFields(LSContext ctx, List<CommonToken> defaultTokens,
                                                                 int delimIndex) {
        List<BCompiledSymbol> resultList = new ArrayList<>();
        List<ChainedFieldModel> invocationFieldList = getInvocationFieldList(defaultTokens, delimIndex, false);
        SymbolTable symbolTable = SymbolTable.getInstance(ctx.get(DocumentServiceKeys.COMPILER_CONTEXT_KEY));

        ChainedFieldModel startField = invocationFieldList.get(0);
        List<BCompiledSymbol> symbolList = new ArrayList<>(ctx.get(CommonKeys.VISIBLE_SYMBOLS_KEY));
        BCompiledSymbol bSymbol = getVariableByName(startField.name.getText(), symbolList);
        if (bSymbol.getKind() == BallerinaSymbolKind.MODULE) {
            /*
            Above common filter extract package symbols as well. Hence we skip since dot delimiter is not valid over a 
            module 
             */
            return resultList;
        }
        Optional<TypeDescriptor> modifiedBType = getModifiedBType(bSymbol, ctx, startField.fieldType);
        if (!modifiedBType.isPresent()) {
            return resultList;
        }
        
        List<BCompiledSymbol> symbolsOnType = getDefinedSymbolsOnType(ctx, modifiedBType.get());
        for (int itr = 1; itr < invocationFieldList.size(); itr++) {
            ChainedFieldModel fieldModel = invocationFieldList.get(itr);
            if (modifiedBType.get().getKind() == TypeDescKind.JSON) {
                /*
                Specially handle the json type to support json field access
                Eg: myJson.test_field.toString()
                 */
//                modifiedBType = BUnionType.create(null, modifiedBType, symbolTable.errorType);
//                scopeEntries = getInvocationsAndFieldsForSymbol(fieldModel.name.getText(), modifiedBType, ctx);
                continue;
            }
            if (symbolsOnType == null) {
                break;
            }
            Optional<BCompiledSymbol> entry = getCompiledSymbolWithName(symbolsOnType, fieldModel);
            if (!entry.isPresent()) {
                break;
            }
            bSymbol = entry.get();
            modifiedBType = getModifiedBType(bSymbol, ctx, fieldModel.fieldType);
            symbolsOnType = getInvocationsAndFieldsForSymbol(fieldModel.name.getText(), modifiedBType.get(), ctx);
        }
        if (symbolsOnType == null) {
            return new ArrayList<>();
        }
        resultList.addAll(symbolsOnType);

        return resultList;
    }

    /**
     * Get the variable symbol info by the name.
     *
     * @param name    name of the variable
     * @param symbols list of symbol info
     * @return {@link BCompiledSymbol} Scope Entry extracted
     */
    public static BCompiledSymbol getVariableByName(String name, List<BCompiledSymbol> symbols) {
        return symbols.stream()
                .filter(symbol -> symbol.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static Optional<BSymbol> getBTypeEntry(Scope.ScopeEntry entry) {
        while (entry != NOT_FOUND_ENTRY) {
            if ((entry.symbol.tag & SymTag.TYPE) == SymTag.TYPE) {
                if (!CommonUtil.symbolContainsInvalidChars(entry.symbol) && entry.symbol instanceof BTypeSymbol) {
                    return Optional.of(entry.symbol);
                }
            }
            entry = entry.next;
        }
        return Optional.empty();
    }

    public static boolean isBTypeEntry(Scope.ScopeEntry entry) {
        return getBTypeEntry(entry).isPresent();
    }

    ///////////////////////////
    ///// Private Methods /////
    ///////////////////////////

    private static TypeDescriptor getBTypeForUnionType(UnionTypeDescriptor unionTypeDesc) {
        List<TypeDescriptor> memberTypeList = new ArrayList<>(unionTypeDesc.getMemberTypes());
        memberTypeList.removeIf(type -> CommonUtil.getRawType(type).getKind() == TypeDescKind.ERROR
                || type.getKind() == TypeDescKind.NIL);

        if (memberTypeList.size() == 1) {
            return memberTypeList.get(0);
        }

        return unionTypeDesc;
    }

    private static List<Scope.ScopeEntry> loadActionsFunctionsAndTypesFromScope(Map<Name, Scope.ScopeEntry> entryMap) {
        List<Scope.ScopeEntry> actionFunctionList = new ArrayList<>();
        entryMap.forEach((name, scopeEntry) -> {
            BSymbol symbol = scopeEntry.symbol;
            if (((symbol instanceof BInvokableSymbol && ((BInvokableSymbol) symbol).receiverSymbol == null)
                    || isBTypeEntry(scopeEntry)
                    || symbol instanceof BVarSymbol)
                    && (symbol.flags & Flags.PUBLIC) == Flags.PUBLIC) {
                actionFunctionList.add(scopeEntry);
            }
        });

        return actionFunctionList;
    }

    private static Optional<TypeDescriptor> getModifiedBType(BCompiledSymbol bSymbol, LSContext context,
                                                             InvocationFieldType fieldType) {
        Integer invocationType = context.get(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY);
        Optional<TypeDescriptor> actualType = Optional.empty();
        if (bSymbol instanceof BallerinaVariable) {
            if (bSymbol.getKind() == BallerinaSymbolKind.FUNCTION) {
                actualType = ((BallerinaFunctionSymbol) bSymbol).getReturnType();
            } else {
                actualType = ((BallerinaVariable) bSymbol).getTypeDescriptor();
            }
        } else if (bSymbol instanceof BallerinaField) {
            actualType = Optional.ofNullable(((BallerinaField) bSymbol).getTypeDescriptor());
        }
//        if ((bSymbol.tag & SymTag.TYPE) == SymTag.TYPE) {
//            actualType = new BTypedescType(bSymbol.type, null);
//        } else if (bSymbol instanceof BInvokableSymbol) {
//            actualType = ((BInvokableSymbol) bSymbol).retType;
//        } else if (bSymbol.type instanceof BArrayType && fieldType == InvocationFieldType.ARRAY_ACCESS) {
//            return ((BArrayType) bSymbol.type).eType;
//        } else if (bSymbol.type instanceof BMapType && fieldType == InvocationFieldType.ARRAY_ACCESS) {
//            LinkedHashSet<BType> types = new LinkedHashSet<>();
//            types.add(((BMapType) bSymbol.type).constraint);
//            types.add(new BNilType());
//            actualType = BUnionType.create(((BMapType) bSymbol.type).constraint.tsymbol, types);
//        } else {
//            actualType = bSymbol.t;
//        }
        if (!actualType.isPresent()) {
            return actualType;
        }
        actualType = Optional.ofNullable(CommonUtil.getRawType(actualType.get()));
        if (actualType.isPresent() && (actualType.get().getKind() == TypeDescKind.UNION
                && invocationType == BallerinaParser.NOT)) {
            TypeDescriptor bTypeForUnionType = getBTypeForUnionType((UnionTypeDescriptor) actualType.get());
            actualType =  Optional.ofNullable(CommonUtil.getRawType(bTypeForUnionType));
        }
        return actualType;
    }

    private static List<ChainedFieldModel> getInvocationFieldList(List<CommonToken> defaultTokens, int startIndex,
                                                                  boolean captureValidField) {
        int traverser = startIndex;
        int rightBracketCount = 0;
        int gtCount = 0;
        boolean invocation = false;
        boolean arrayAccess = false;
        List<ChainedFieldModel> fieldList = new ArrayList<>();
        Pattern pattern = Pattern.compile("^\\w+$");
        boolean captureNextValidField = captureValidField;
        while (traverser >= 0) {
            CommonToken token = defaultTokens.get(traverser);
            int type = token.getType();
            Matcher matcher = pattern.matcher(token.getText());
            boolean foundMatch = matcher.find();
            if (type == BallerinaParser.RIGHT_PARENTHESIS) {
                Pair<Boolean, Integer> isGroupedExpr = isGroupedExpression(defaultTokens, traverser - 1);
                if (isGroupedExpr.getLeft()) {
                    List<CommonToken> subList = defaultTokens.subList(isGroupedExpr.getRight() + 1, traverser);
                    List<ChainedFieldModel> groupedFields = getInvocationFieldList(subList, subList.size() - 1,
                            captureNextValidField);
                    fieldList.addAll(Lists.reverse(groupedFields));
                } else {
                    invocation = true;
                }
                traverser = isGroupedExpr.getRight() - 1;
            } else if (type == BallerinaParser.RIGHT_BRACKET) {
                // Mapped to both xml and array variables
                if (!arrayAccess) {
                    arrayAccess = true;
                }
                rightBracketCount++;
                traverser--;
            } else if (type == BallerinaParser.LEFT_PARENTHESIS
                    || type == BallerinaParser.DIV || type == BallerinaParser.MUL || (foundMatch && gtCount > 0)) {
                // DIV and MUL added in order to skip the xml navigation
                // Also we do not capture the tokens within the xml tags (when navigating xml)
                // ex: xmlVal/<hello>.*.
                traverser--;
            } else if (type == BallerinaParser.LEFT_BRACKET && rightBracketCount > 0) {
                // Mapped to array variables
                rightBracketCount--;
                traverser--;
            } else if (type == BallerinaParser.GT) {
                gtCount++;
                traverser--;
            } else if (type == BallerinaParser.LT) {
                gtCount--;
                traverser--;
            } else if (type == BallerinaParser.DOT || type == BallerinaParser.NOT
                    || type == BallerinaParser.OPTIONAL_FIELD_ACCESS || rightBracketCount > 0) {
                captureNextValidField = true;
                traverser--;
            } else if (foundMatch && rightBracketCount == 0
                    && captureNextValidField) {
                InvocationFieldType fieldType;
                CommonToken packageAlias = null;
                traverser--;

                if (invocation) {
                    fieldType = InvocationFieldType.INVOCATION;
                    invocation = false;
                } else if (arrayAccess) {
                    // Mapped to both xml and array variables
                    fieldType = InvocationFieldType.ARRAY_ACCESS;
                    arrayAccess = false;
                } else {
                    fieldType = InvocationFieldType.FIELD;
                }

                if (traverser > 0 && defaultTokens.get(traverser).getType() == BallerinaParser.COLON) {
                    packageAlias = defaultTokens.get(traverser - 1);
                }
                ChainedFieldModel model = new ChainedFieldModel(fieldType, token, packageAlias);
                fieldList.add(model);
                captureNextValidField = false;
            } else {
                break;
            }
        }

        return Lists.reverse(fieldList);
    }

    private static Pair<Boolean, Integer> isGroupedExpression(List<CommonToken> defaultTokens, int startIndex) {
        int traverser = startIndex;
        int rightParenCount = 0;
        Pattern pattern = Pattern.compile("^\\w+$");
        while (true) {
            int type = defaultTokens.get(traverser).getType();
            if (type == BallerinaParser.RIGHT_PARENTHESIS) {
                rightParenCount++;
            } else if (type == BallerinaParser.LEFT_PARENTHESIS) {
                if (rightParenCount == 0) {
                    if (traverser <= 0) {
                        return Pair.of(true, traverser);
                    } else {
                        CommonToken tokenBefore = defaultTokens.get(traverser - 1);
                        Matcher matcher = pattern.matcher(tokenBefore.getText());
                        return Pair.of(!matcher.find(), traverser);
                    }
                } else {
                    rightParenCount--;
                }
            }
            traverser--;
        }
    }

    /**
     * Analyze the given symbol type and extracts the invocations and fields from the scope entries.
     * When extracting the invocations, extract the type attached functions
     *
     * @param symbolName symbol name to evaluate
     * @param symbolType BType to evaluate
     * @param context    Language Server Operation Context
     * @return {@link Map} Scope Entry Map
     */
    private static List<BCompiledSymbol> getInvocationsAndFieldsForSymbol(String symbolName,
                                                                          TypeDescriptor symbolType,
                                                                          LSContext context) {
        Integer invocationToken = context.get(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY);
        CompilerContext compilerContext = context.get(DocumentServiceKeys.COMPILER_CONTEXT_KEY);
        SymbolTable symbolTable = SymbolTable.getInstance(compilerContext);
        Types types = Types.getInstance(compilerContext);
        List<BCompiledSymbol> symbols = new ArrayList<>();
        /*
        LangLib checks also contains a check for the object type tag. But we skip and instead extract the entries
        from the object symbol itself
         */
        if (symbolType.getKind() == TypeDescKind.OBJECT) {
            ObjectTypeDescriptor objectTypeDescriptor = (ObjectTypeDescriptor) symbolType;
            symbols.addAll(getObjectMethodsAndFields(context, (ObjectTypeDescriptor) symbolType, symbolName));
        } else if (symbolType instanceof UnionTypeDescriptor) {
            symbols.addAll(getInvocationsAndFieldsForUnionType((UnionTypeDescriptor) symbolType, context));
        } else if (symbolType instanceof RecordTypeDescriptor) {
            symbols.addAll(((RecordTypeDescriptor) symbolType).getFieldDescriptors());
        }
        // TODO: Suggest the langlib entries
        symbols.addAll(getLangLibEntriesOnType(context, symbolType));
//        else if (symbolType.tsymbol != null && symbolType.tsymbol.scope != null) {
////            entries.putAll(getLangLibScopeEntries(symbolType, symbolTable, types));
//            Map<Name, Scope.ScopeEntry> filteredEntries = symbolType.tsymbol.scope.entries.entrySet().stream()
//                    .filter(optionalFieldFilter(symbolType, invocationToken, context))
//                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//            entries.putAll(filteredEntries);
//        } 
//        else {
//            entries.putAll(getLangLibScopeEntries(symbolType, symbolTable, types));
//        }
        /*
        Here we add the BTypeSymbol check to skip the anyData and similar types suggested from lang lib scope entries
         */
        symbols.removeIf(symbol -> symbol.getKind() == BallerinaSymbolKind.REMOTE_FUNCTION);

        return symbols;
    }

    private static List<BCompiledSymbol> getObjectMethodsAndFields(LSContext context,
                                                                   ObjectTypeDescriptor objectType,
                                                                   String symbolName) {
        List<BCompiledSymbol> symbols = new ArrayList<>();
        String currentModule = context.get(DocumentServiceKeys.CURRENT_PKG_NAME_KEY);
        String objectOwnerModule = objectType.getModuleID().getModuleName();
        boolean symbolInCurrentModule = currentModule.equals(objectOwnerModule);

        // Extract the method entries
        List<BallerinaFunctionSymbol> functionSymbols = objectType.getMethods().stream()
                .filter(method -> {
                    List<AccessModifier> accessModifiers = method.getAccessModifiers();
                    boolean isPrivate = accessModifiers.contains(AccessModifier.PRIVATE);
                    boolean isPublic = accessModifiers.contains(AccessModifier.PUBLIC);

                    return !((method.getName().contains(".__init") && !"self".equals(symbolName))
                            || (isPrivate && !"self".equals(symbolName))
                            || (!isPrivate && !isPublic && !symbolInCurrentModule));
                })
                .collect(Collectors.toList());
        symbols.addAll(functionSymbols);

        // Extract Field Entries
        List<BallerinaField> fields = objectType.getObjectFields().stream()
                .filter(field -> {
                    Optional<AccessModifier> accessModifier = field.getAccessModifier();
                    boolean isPrivate = accessModifier.isPresent() && accessModifier.get() == AccessModifier.PRIVATE;
                    boolean isPublic = accessModifier.isPresent() && accessModifier.get() == AccessModifier.PUBLIC;
                    return !((isPrivate && !"self".equals(symbolName))
                            || (!isPrivate && !isPublic && !symbolInCurrentModule));
                })
                .collect(Collectors.toList());

        symbols.addAll(fields);

        return symbols;
    }

    /**
     * When given a union of record types, iterate over the member types to extract the fields with the same name.
     * If a certain field with the same name contains in all records we extract the field entries
     *
     * @param unionType union type to evaluate
     * @param context   Language server operation context
     * @return {@link Map} map of scope entries
     */
    private static List<BCompiledSymbol> getInvocationsAndFieldsForUnionType(UnionTypeDescriptor unionType,
                                                                             LSContext context) {
        ArrayList<TypeDescriptor> memberTypes = new ArrayList<>(unionType.getMemberTypes());
        List<BCompiledSymbol> resultEntries = new ArrayList<>();
        CompilerContext compilerContext = context.get(DocumentServiceKeys.COMPILER_CONTEXT_KEY);
        SymbolTable symbolTable = SymbolTable.getInstance(compilerContext);
        Types types = Types.getInstance(compilerContext);
        Integer invocationTokenType = context.get(CompletionKeys.INVOCATION_TOKEN_TYPE_KEY);
        // check whether union consists of same type tag symbols
        TypeDescriptor firstMemberType = memberTypes.get(0);
        boolean allMatch = memberTypes.stream().allMatch(bType -> bType.getKind() == firstMemberType.getKind());
        // If all the members are not same types we stop proceeding
        if (!allMatch) {
            // TODO: get the langlib entries
//            resultEntries.putAll(getLangLibScopeEntries(unionType, symbolTable, types));
            return resultEntries;
        }
        // TODO: get the langlib entries
//        resultEntries.addAll(getLangLibScopeEntries(firstMemberType, symbolTable, types));
        if (firstMemberType.getKind() == TypeDescKind.RECORD) {
            // Keep track of the occurrences of each of the field names
            LinkedHashMap<String, Integer> memberOccurrenceCounts = new LinkedHashMap<>();
            List<String> firstMemberFieldKeys = new ArrayList<>();
        /*
        We keep only the name fields of the first member type since a field has to appear in all the member types
         */
            for (int memberCounter = 0; memberCounter < memberTypes.size(); memberCounter++) {
                int finalMemberCounter = memberCounter;
                ((RecordTypeDescriptor) memberTypes.get(memberCounter)).getFieldDescriptors()
                        .forEach(field -> {
                            String fieldName = field.getName();
                            if (memberOccurrenceCounts.containsKey(fieldName)) {
                                memberOccurrenceCounts.put(fieldName, memberOccurrenceCounts.get(fieldName) + 1);
                            } else if (finalMemberCounter == 0) {
                                // Fields are only captured for the first member type, otherwise the count is increased
                                firstMemberFieldKeys.add(fieldName);
                                memberOccurrenceCounts.put(fieldName, 1);
                            }
                        });
            }
            if (memberOccurrenceCounts.size() == 0) {
                return resultEntries;
            }
            List<Integer> counts = new ArrayList<>(memberOccurrenceCounts.values());
            List<BallerinaField> firstMemberFields = ((RecordTypeDescriptor) firstMemberType).getFieldDescriptors();
            for (int i = 0; i < counts.size(); i++) {
                if (counts.get(i) != memberTypes.size()) {
                    continue;
                }
                String name = firstMemberFieldKeys.get(i);
                Optional<BallerinaField> bField = firstMemberFields.stream().filter(field -> field.getName().equals(name)).findAny();
                if (firstMemberType.getKind() == TypeDescKind.RECORD && (invocationTokenType == BallerinaParser.DOT
                        || invocationTokenType == BallerinaParser.NOT)
                        && bField.isPresent() && bField.get().isOptional()) {
                    continue;
                }
                resultEntries.add(bField.get());
            }
        }

        return resultEntries;
    }

    private static Optional<BCompiledSymbol> getCompiledSymbolWithName(List<BCompiledSymbol> entries,
                                                                    ChainedFieldModel fieldModel) {
        return entries.stream().filter(symbol -> {
            String[] symbolNameComponents = symbol.getName().split("\\.");
            String symbolName = symbolNameComponents[symbolNameComponents.length - 1];
            if (!symbolName.equals(fieldModel.name.getText())) {
                return false;
            }
            return (fieldModel.fieldType == InvocationFieldType.INVOCATION && symbol instanceof BInvokableSymbol)
                    || (fieldModel.fieldType == InvocationFieldType.FIELD && !(symbol instanceof BInvokableSymbol));

        }).findAny();
    }

    /**
     * Retrieve lang-lib scope entries for this typeTag.
     *
     * @param context language server operation context
     * @param typeDescriptor type descriptor to evalyate
     * @return map of scope entries
     */
    public static List<BCompiledSymbol> getLangLibEntriesOnType(LSContext context, TypeDescriptor typeDescriptor) {
        SemanticModel semanticModel = context.get(CommonKeys.SEMANTIC_MODEL_KEY);
        return semanticModel.getLangLibSymbols((BallerinaTypeDesc) typeDescriptor);
    }

    private static Predicate<Map.Entry<Name, Scope.ScopeEntry>> optionalFieldFilter(BType symbolType,
                                                                                    Integer invocationTkn,
                                                                                    LSContext context) {
        BLangNode scope = context.get(CompletionKeys.SCOPE_NODE_KEY);
        List<Integer> defaultTokenTypes = context.get(SourcePruneKeys.LHS_DEFAULT_TOKENS_KEY)
                .stream()
                .map(CommonToken::getType).collect(Collectors.toList());


        return entry -> {
            if (symbolType.tag == TypeTags.RECORD && (invocationTkn == BallerinaParser.DOT
                    || invocationTkn == BallerinaParser.NOT) && (scope instanceof BLangBlockStmt
                    || scope instanceof BLangFunctionBody) && defaultTokenTypes.contains(BallerinaParser.ASSIGN)) {
                return !org.ballerinalang.jvm.util.Flags.isFlagOn(entry.getValue().symbol.flags,
                        Flags.OPTIONAL);
            }
            return true;
        };
    }

    private static List<BCompiledSymbol> getDefinedSymbolsOnType(LSContext context, TypeDescriptor typeDescriptor) {
        List<BCompiledSymbol> entries = getLangLibEntriesOnType(context, typeDescriptor);
        switch (typeDescriptor.getKind()) {
            case RECORD:
                RecordTypeDescriptor recordType = (RecordTypeDescriptor) typeDescriptor;
                entries.addAll(recordType.getFieldDescriptors());
                break;
            case OBJECT:
                ObjectTypeDescriptor objectType = (ObjectTypeDescriptor) typeDescriptor;
                entries.addAll(objectType.getMethods());
                entries.addAll(objectType.getObjectFields());
                break;
            default:
                break;
        }
        
        return entries;
    }
    
    /**
     * Data model for the chained field.
     */
    private static class ChainedFieldModel {
        InvocationFieldType fieldType;
        CommonToken name;
        CommonToken pkgAlias;

        ChainedFieldModel(InvocationFieldType fieldType, CommonToken name, CommonToken pkgAlias) {
            this.fieldType = fieldType;
            this.name = name;
            this.pkgAlias = pkgAlias;
        }
    }

    /**
     * Enum for chained field type.
     */
    public enum InvocationFieldType {
        FIELD,
        INVOCATION,
        ARRAY_ACCESS
    }
}
