/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.ballerinalang.compiler.desugar;

import org.ballerinalang.compiler.CompilerPhase;
import org.ballerinalang.model.TreeBuilder;
import org.ballerinalang.model.elements.Flag;
import org.ballerinalang.model.elements.PackageID;
import org.ballerinalang.model.elements.TableColumnFlag;
import org.ballerinalang.model.symbols.SymbolKind;
import org.ballerinalang.model.tree.BlockFunctionBodyNode;
import org.ballerinalang.model.tree.NodeKind;
import org.ballerinalang.model.tree.OperatorKind;
import org.ballerinalang.model.tree.SequenceStatementNode;
import org.ballerinalang.model.tree.expressions.NamedArgNode;
import org.ballerinalang.model.tree.expressions.RecordLiteralNode;
import org.ballerinalang.model.tree.statements.BlockNode;
import org.ballerinalang.model.tree.statements.VariableDefinitionNode;
import org.ballerinalang.model.tree.types.TypeNode;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.util.BLangCompilerConstants;
import org.ballerinalang.util.Transactions;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolEnter;
import org.wso2.ballerinalang.compiler.semantics.analyzer.SymbolResolver;
import org.wso2.ballerinalang.compiler.semantics.analyzer.TaintAnalyzer;
import org.wso2.ballerinalang.compiler.semantics.analyzer.TypeParamAnalyzer;
import org.wso2.ballerinalang.compiler.semantics.analyzer.Types;
import org.wso2.ballerinalang.compiler.semantics.model.Scope;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolEnv;
import org.wso2.ballerinalang.compiler.semantics.model.SymbolTable;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BAttachedFunction;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BConstantSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BErrorTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BObjectTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BOperatorSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BRecordTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BTypeSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BVarSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BXMLNSSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.SymTag;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.Symbols;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.TaintRecord;
import org.wso2.ballerinalang.compiler.semantics.model.types.BArrayType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BErrorType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BMapType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BRecordType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTupleType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BTypedescType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotation;
import org.wso2.ballerinalang.compiler.tree.BLangAnnotationAttachment;
import org.wso2.ballerinalang.compiler.tree.BLangBlockFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangErrorVariable;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangFunctionBody;
import org.wso2.ballerinalang.compiler.tree.BLangIdentifier;
import org.wso2.ballerinalang.compiler.tree.BLangImportPackage;
import org.wso2.ballerinalang.compiler.tree.BLangInvokableNode;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangNodeVisitor;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangRecordVariable;
import org.wso2.ballerinalang.compiler.tree.BLangRecordVariable.BLangRecordVariableKeyValue;
import org.wso2.ballerinalang.compiler.tree.BLangResource;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTupleVariable;
import org.wso2.ballerinalang.compiler.tree.BLangTypeDefinition;
import org.wso2.ballerinalang.compiler.tree.BLangVariable;
import org.wso2.ballerinalang.compiler.tree.BLangXMLNS;
import org.wso2.ballerinalang.compiler.tree.BLangXMLNS.BLangLocalXMLNS;
import org.wso2.ballerinalang.compiler.tree.BLangXMLNS.BLangPackageXMLNS;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangFromClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangSelectClause;
import org.wso2.ballerinalang.compiler.tree.clauses.BLangWhereClause;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAccessExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangAnnotAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangArrowFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangBinaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCheckPanickedExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangCheckedExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangConstant;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangElvisExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangErrorVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangFieldBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangFieldBasedAccess.BLangStructFunctionVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangGroupExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIgnoreExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangArrayAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangJSONAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangMapAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangStringAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangStructFieldAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangTupleAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIndexBasedAccess.BLangXMLAccessExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIntRangeExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation.BFunctionPointerInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangInvocation.BLangAttachedFunctionInvocation;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIsAssignableExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangIsLikeExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLambdaFunction;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr.BLangArrayLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr.BLangJSONArrayLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangListConstructorExpr.BLangTupleLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangMatchExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangMatchExpression.BLangMatchExprPatternClause;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangNamedArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangQueryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangJSONLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangMapLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordLiteral.BLangStructLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRecordVarRef.BLangRecordVarRefKeyValue;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangRestArgsExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangServiceConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangConstRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangFieldVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangFunctionVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangLocalVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangPackageVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangSimpleVarRef.BLangTypeLoad;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStatementExpression;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangStringTemplateLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTableLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTernaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTrapExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTupleVarRef;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeConversionExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeTestExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypedescExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangUnaryExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangVariableReference;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWaitExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWaitForAllExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerFlushExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerReceive;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangWorkerSyncSendExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLAttribute;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLAttributeAccess;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLCommentLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLElementLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLProcInsLiteral;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQName;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLQuotedString;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangXMLTextLiteral;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAbort;
import org.wso2.ballerinalang.compiler.tree.statements.BLangAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBlockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangBreak;
import org.wso2.ballerinalang.compiler.tree.statements.BLangCompoundAssignment;
import org.wso2.ballerinalang.compiler.tree.statements.BLangContinue;
import org.wso2.ballerinalang.compiler.tree.statements.BLangErrorDestructure;
import org.wso2.ballerinalang.compiler.tree.statements.BLangErrorVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangExpressionStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForeach;
import org.wso2.ballerinalang.compiler.tree.statements.BLangForkJoin;
import org.wso2.ballerinalang.compiler.tree.statements.BLangIf;
import org.wso2.ballerinalang.compiler.tree.statements.BLangLock;
import org.wso2.ballerinalang.compiler.tree.statements.BLangLock.BLangLockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangLock.BLangUnLockStmt;
import org.wso2.ballerinalang.compiler.tree.statements.BLangMatch;
import org.wso2.ballerinalang.compiler.tree.statements.BLangMatch.BLangMatchBindingPatternClause;
import org.wso2.ballerinalang.compiler.tree.statements.BLangMatch.BLangMatchStaticBindingPatternClause;
import org.wso2.ballerinalang.compiler.tree.statements.BLangMatch.BLangMatchStructuredBindingPatternClause;
import org.wso2.ballerinalang.compiler.tree.statements.BLangMatch.BLangMatchTypedBindingPatternClause;
import org.wso2.ballerinalang.compiler.tree.statements.BLangPanic;
import org.wso2.ballerinalang.compiler.tree.statements.BLangRecordDestructure;
import org.wso2.ballerinalang.compiler.tree.statements.BLangRecordVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangRetry;
import org.wso2.ballerinalang.compiler.tree.statements.BLangReturn;
import org.wso2.ballerinalang.compiler.tree.statements.BLangSimpleVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangStatement;
import org.wso2.ballerinalang.compiler.tree.statements.BLangStatement.BLangStatementLink;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTransaction;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTupleDestructure;
import org.wso2.ballerinalang.compiler.tree.statements.BLangTupleVariableDef;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWhile;
import org.wso2.ballerinalang.compiler.tree.statements.BLangWorkerSend;
import org.wso2.ballerinalang.compiler.tree.statements.BLangXMLNSStatement;
import org.wso2.ballerinalang.compiler.tree.types.BLangErrorType;
import org.wso2.ballerinalang.compiler.tree.types.BLangObjectTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangRecordTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangStructureTypeNode;
import org.wso2.ballerinalang.compiler.tree.types.BLangType;
import org.wso2.ballerinalang.compiler.tree.types.BLangUserDefinedType;
import org.wso2.ballerinalang.compiler.tree.types.BLangValueType;
import org.wso2.ballerinalang.compiler.util.ClosureVarSymbol;
import org.wso2.ballerinalang.compiler.util.CompilerContext;
import org.wso2.ballerinalang.compiler.util.DefaultValueLiteral;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.Names;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Flags;
import org.wso2.ballerinalang.util.Lists;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import static org.wso2.ballerinalang.compiler.util.Constants.INIT_METHOD_SPLIT_SIZE;
import static org.wso2.ballerinalang.compiler.util.Names.GEN_VAR_PREFIX;
import static org.wso2.ballerinalang.compiler.util.Names.IGNORE;
import static org.wso2.ballerinalang.compiler.util.Names.TRX_INITIATOR_BEGIN_FUNCTION;
import static org.wso2.ballerinalang.compiler.util.Names.TRX_LOCAL_PARTICIPANT_BEGIN_FUNCTION;
import static org.wso2.ballerinalang.compiler.util.Names.TRX_REMOTE_PARTICIPANT_BEGIN_FUNCTION;

/**
 * @since 0.94
 */
public class Desugar extends BLangNodeVisitor {

    private static final CompilerContext.Key<Desugar> DESUGAR_KEY =
            new CompilerContext.Key<>();
    private static final String QUERY_TABLE_WITH_JOIN_CLAUSE = "queryTableWithJoinClause";
    private static final String QUERY_TABLE_WITHOUT_JOIN_CLAUSE = "queryTableWithoutJoinClause";
    private static final String BASE_64 = "base64";
    private static final String ERROR_REASON_FUNCTION_NAME = "reason";
    private static final String ERROR_DETAIL_FUNCTION_NAME = "detail";
    private static final String TO_STRING_FUNCTION_NAME = "toString";
    private static final String LENGTH_FUNCTION_NAME = "length";
    private static final String ERROR_REASON_NULL_REFERENCE_ERROR = "NullReferenceException";
    private static final String CONSTRUCT_FROM = "constructFrom";

    private SymbolTable symTable;
    private SymbolResolver symResolver;
    private final SymbolEnter symbolEnter;
    private ClosureDesugar closureDesugar;
    private AnnotationDesugar annotationDesugar;
    private Types types;
    private Names names;
    private ServiceDesugar serviceDesugar;
    private BLangNode result;

    private BLangStatementLink currentLink;
    public Stack<BLangLockStmt> enclLocks = new Stack<>();

    private SymbolEnv env;
    private int lambdaFunctionCount = 0;
    private int transactionIndex = 0;
    private int recordCount = 0;
    private int errorCount = 0;
    private int annonVarCount = 0;
    private int initFuncIndex = 0;
    private int indexExprCount = 0;

    // Safe navigation related variables
    private Stack<BLangMatch> matchStmtStack = new Stack<>();
    Stack<BLangExpression> accessExprStack = new Stack<>();
    private BLangMatchTypedBindingPatternClause successPattern;
    private BLangAssignment safeNavigationAssignment;
    static boolean isJvmTarget = false;

    public static Desugar getInstance(CompilerContext context) {
        Desugar desugar = context.get(DESUGAR_KEY);
        if (desugar == null) {
            desugar = new Desugar(context);
        }

        return desugar;
    }

    private Desugar(CompilerContext context) {
        // This is a temporary flag to differentiate desugaring to BVM vs BIR
        // TODO: remove this once bootstraping is added.
        isJvmTarget = true;

        context.put(DESUGAR_KEY, this);
        this.symTable = SymbolTable.getInstance(context);
        this.symResolver = SymbolResolver.getInstance(context);
        this.symbolEnter = SymbolEnter.getInstance(context);
        this.closureDesugar = ClosureDesugar.getInstance(context);
        this.annotationDesugar = AnnotationDesugar.getInstance(context);
        this.types = Types.getInstance(context);
        this.names = Names.getInstance(context);
        this.names = Names.getInstance(context);
        this.serviceDesugar = ServiceDesugar.getInstance(context);
    }

    public BLangPackage perform(BLangPackage pkgNode) {
        // Initialize the annotation map
        annotationDesugar.initializeAnnotationMap(pkgNode);
        return rewrite(pkgNode, env);
    }

    private void addAttachedFunctionsToPackageLevel(BLangPackage pkgNode, SymbolEnv env) {
        for (BLangTypeDefinition typeDef : pkgNode.typeDefinitions) {
            if (typeDef.typeNode.getKind() == NodeKind.USER_DEFINED_TYPE) {
                continue;
            }
            if (typeDef.symbol.tag == SymTag.OBJECT) {
                BLangObjectTypeNode objectTypeNode = (BLangObjectTypeNode) typeDef.typeNode;

                objectTypeNode.functions.forEach(f -> {
                    if (!pkgNode.objAttachedFunctions.contains(f.symbol)) {
                        pkgNode.functions.add(f);
                        pkgNode.topLevelNodes.add(f);
                    }
                });

                if (objectTypeNode.flagSet.contains(Flag.ABSTRACT)) {
                    continue;
                }

                objectTypeNode.generatedInitFunction = createGeneratedInitializerFunction(objectTypeNode, env);

                // Add generated init function to the attached function list
                pkgNode.functions.add(objectTypeNode.generatedInitFunction);
                pkgNode.topLevelNodes.add(objectTypeNode.generatedInitFunction);

                // Add init function to the attached function list
                if (objectTypeNode.initFunction != null) {
                    pkgNode.functions.add(objectTypeNode.initFunction);
                    pkgNode.topLevelNodes.add(objectTypeNode.initFunction);
                }
            } else if (typeDef.symbol.tag == SymTag.RECORD) {
                BLangRecordTypeNode recordTypeNode = (BLangRecordTypeNode) typeDef.typeNode;
                recordTypeNode.initFunction = createInitFunctionForRecordType(recordTypeNode, env);
                pkgNode.functions.add(recordTypeNode.initFunction);
                pkgNode.topLevelNodes.add(recordTypeNode.initFunction);
            }
        }
    }

    private BLangFunction createGeneratedInitializerFunction(BLangObjectTypeNode objectTypeNode, SymbolEnv env) {
        BLangFunction generatedInitFunc = createInitFunctionForObjectType(objectTypeNode, env);
        if (objectTypeNode.initFunction == null) {
            return generatedInitFunc;
        }

        BAttachedFunction initializerFunc = ((BObjectTypeSymbol) objectTypeNode.symbol).initializerFunc;
        BAttachedFunction generatedInitializerFunc =
                ((BObjectTypeSymbol) objectTypeNode.symbol).generatedInitializerFunc;
        addRequiredParamsToGeneratedInitFunction(objectTypeNode.initFunction, generatedInitFunc,
                generatedInitializerFunc);
        addRestParamsToGeneratedInitFunction(objectTypeNode.initFunction, generatedInitFunc, generatedInitializerFunc);

        generatedInitFunc.returnTypeNode = objectTypeNode.initFunction.returnTypeNode;
        generatedInitializerFunc.symbol.retType = generatedInitFunc.returnTypeNode.type;

        ((BInvokableType) generatedInitFunc.symbol.type).paramTypes = initializerFunc.type.paramTypes;
        ((BInvokableType) generatedInitFunc.symbol.type).retType = initializerFunc.type.retType;
        ((BInvokableType) generatedInitFunc.symbol.type).restType = initializerFunc.type.restType;

        generatedInitializerFunc.type = initializerFunc.type;
        generatedInitFunc.desugared = false;
        return generatedInitFunc;
    }

    private void addRequiredParamsToGeneratedInitFunction(BLangFunction initFunction, BLangFunction generatedInitFunc,
                                                          BAttachedFunction generatedInitializerFunc) {
        if (initFunction.requiredParams.isEmpty()) {
            return;
        }
        for (BLangSimpleVariable requiredParameter : initFunction.requiredParams) {
            BLangSimpleVariable var =
                    ASTBuilderUtil.createVariable(initFunction.pos,
                            requiredParameter.name.getValue(), requiredParameter.type, requiredParameter.expr,
                            new BVarSymbol(0, names.fromString(requiredParameter.name.getValue()),
                                    requiredParameter.symbol.pkgID,
                                    requiredParameter.type, requiredParameter.symbol.owner));
            generatedInitFunc.requiredParams.add(var);
            generatedInitializerFunc.symbol.params.add(var.symbol);
        }
    }

    private void addRestParamsToGeneratedInitFunction(BLangFunction initFunction, BLangFunction generatedInitFunc,
                                                      BAttachedFunction generatedInitializerFunc) {
        if (initFunction.restParam == null) {
            return;
        }
        BLangSimpleVariable restParam = initFunction.restParam;
        generatedInitFunc.restParam =
                ASTBuilderUtil.createVariable(initFunction.pos,
                        restParam.name.getValue(), restParam.type, null, new BVarSymbol(0,
                                names.fromString(restParam.name.getValue()), restParam.symbol.pkgID,
                                restParam.type, restParam.symbol.owner));
        generatedInitializerFunc.symbol.restParam = generatedInitFunc.restParam.symbol;
    }

    /**
     * Create package init functions.
     *
     * @param pkgNode package node
     * @param env     symbol environment of package
     */
    private void createPackageInitFunctions(BLangPackage pkgNode, SymbolEnv env) {
        String alias = pkgNode.symbol.pkgID.toString();
        pkgNode.initFunction = ASTBuilderUtil.createInitFunctionWithErrorOrNilReturn(pkgNode.pos, alias,
                                                                                     Names.INIT_FUNCTION_SUFFIX,
                                                                                     symTable);
        // Add package level namespace declarations to the init function
        BLangBlockFunctionBody initFnBody = (BLangBlockFunctionBody) pkgNode.initFunction.funcBody;
        for (BLangXMLNS xmlns : pkgNode.xmlnsList) {
            initFnBody.addStatement(createNamespaceDeclrStatement(xmlns));
        }
        pkgNode.startFunction = ASTBuilderUtil.createInitFunctionWithErrorOrNilReturn(pkgNode.pos, alias,
                                                                                      Names.START_FUNCTION_SUFFIX,
                                                                                      symTable);
        pkgNode.stopFunction = ASTBuilderUtil.createInitFunctionWithNilReturn(pkgNode.pos, alias,
                                                                              Names.STOP_FUNCTION_SUFFIX);
        // Create invokable symbol for init function
        createInvokableSymbol(pkgNode.initFunction, env);
        // Create invokable symbol for start function
        createInvokableSymbol(pkgNode.startFunction, env);
        // Create invokable symbol for stop function
        createInvokableSymbol(pkgNode.stopFunction, env);
    }

    private void addUserDefinedModuleInitInvocationAndReturn(BLangPackage pkgNode) {
        Optional<BLangFunction> userDefInitOptional = pkgNode.functions.stream()
                .filter(bLangFunction -> !bLangFunction.attachedFunction &&
                            bLangFunction.name.value.equals(Names.USER_DEFINED_INIT_SUFFIX.value))
                .findFirst();

        BLangBlockFunctionBody initFnBody = (BLangBlockFunctionBody) pkgNode.initFunction.funcBody;
        if (!userDefInitOptional.isPresent()) {
            // Assumption: compiler generated module init function body is always a block function body.
            addNilReturnStatement(initFnBody);
            return;
        }

        BLangFunction userDefInit = userDefInitOptional.get();

        BLangInvocation userDefInitInvocation = (BLangInvocation) TreeBuilder.createInvocationNode();
        userDefInitInvocation.pos = pkgNode.initFunction.pos;
        BLangIdentifier name = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        name.setLiteral(false);
        name.setValue(userDefInit.name.value);
        userDefInitInvocation.name = name;
        userDefInitInvocation.symbol = userDefInit.symbol;

        BLangIdentifier pkgAlias = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        pkgAlias.setLiteral(false);
        pkgAlias.setValue(pkgNode.packageID.name.value);
        userDefInitInvocation.pkgAlias = pkgAlias;

        userDefInitInvocation.type = userDefInit.returnTypeNode.type;
        userDefInitInvocation.requiredArgs = Collections.emptyList();

        BLangReturn returnStmt = (BLangReturn) TreeBuilder.createReturnNode();
        returnStmt.pos = pkgNode.initFunction.pos;
        returnStmt.expr = userDefInitInvocation;
        initFnBody.stmts.add(returnStmt);
    }

    /**
     * Create invokable symbol for function.
     *
     * @param bLangFunction function node
     * @param env           Symbol environment
     */
    private void createInvokableSymbol(BLangFunction bLangFunction, SymbolEnv env) {
        BType returnType = bLangFunction.returnTypeNode.type == null ?
                symResolver.resolveTypeNode(bLangFunction.returnTypeNode, env) : bLangFunction.returnTypeNode.type;
        BInvokableType invokableType = new BInvokableType(new ArrayList<>(), getRestType(bLangFunction),
                returnType, null);
        BInvokableSymbol functionSymbol = Symbols.createFunctionSymbol(Flags.asMask(bLangFunction.flagSet),
                new Name(bLangFunction.name.value), env.enclPkg.packageID, invokableType, env.enclPkg.symbol, true);
        functionSymbol.retType = returnType;
        // Add parameters
        for (BLangVariable param : bLangFunction.requiredParams) {
            functionSymbol.params.add(param.symbol);
        }

        functionSymbol.scope = new Scope(functionSymbol);
        bLangFunction.symbol = functionSymbol;
    }

    /**
     * Add nil return statement.
     *
     * @param bLangBlockStmt block statement node
     */
    private void addNilReturnStatement(SequenceStatementNode bLangBlockStmt) {
        BLangReturn returnStmt = ASTBuilderUtil.createNilReturnStmt(((BLangNode) bLangBlockStmt).pos, symTable.nilType);
        bLangBlockStmt.addStatement(returnStmt);
    }

    /**
     * Create namespace declaration statement for XMNLNS.
     *
     * @param xmlns XMLNS node
     * @return XMLNS statement
     */
    private BLangXMLNSStatement createNamespaceDeclrStatement(BLangXMLNS xmlns) {
        BLangXMLNSStatement xmlnsStmt = (BLangXMLNSStatement) TreeBuilder.createXMLNSDeclrStatementNode();
        xmlnsStmt.xmlnsDecl = xmlns;
        xmlnsStmt.pos = xmlns.pos;
        return xmlnsStmt;
    }
    // visitors

    @Override
    public void visit(BLangPackage pkgNode) {
        if (pkgNode.completedPhases.contains(CompilerPhase.DESUGAR)) {
            result = pkgNode;
            return;
        }
        SymbolEnv env = this.symTable.pkgEnvMap.get(pkgNode.symbol);
        createPackageInitFunctions(pkgNode, env);
        // Adding object functions to package level.
        addAttachedFunctionsToPackageLevel(pkgNode, env);

        pkgNode.constants.stream()
                .filter(constant -> constant.expr.getKind() == NodeKind.LITERAL ||
                        constant.expr.getKind() == NodeKind.NUMERIC_LITERAL)
                .forEach(constant -> pkgNode.typeDefinitions.add(constant.associatedTypeDefinition));

        BLangBlockStmt serviceAttachments = serviceDesugar.rewriteServiceVariables(pkgNode.services, env);
        BLangBlockFunctionBody initFnBody = (BLangBlockFunctionBody) pkgNode.initFunction.funcBody;

        for (BLangConstant constant : pkgNode.constants) {
            if (constant.symbol.type.tag == TypeTags.MAP) {
                BLangSimpleVarRef constVarRef = ASTBuilderUtil.createVariableRef(constant.pos, constant.symbol);
                constant.expr = rewriteExpr(constant.expr);
                BLangInvocation frozenConstValExpr =
                        createLangLibInvocationNode(
                                "cloneReadOnly", constant.expr, new ArrayList<>(), constant.expr.type, constant.pos);
                BLangAssignment constInit =
                        ASTBuilderUtil.createAssignmentStmt(constant.pos, constVarRef, frozenConstValExpr);
                initFnBody.stmts.add(constInit);
            }
        }

        pkgNode.globalVars.forEach(globalVar -> {
            BLangAssignment assignment = createAssignmentStmt(globalVar);
            if (assignment.expr != null) {
                initFnBody.stmts.add(assignment);
            }
        });

        pkgNode.services.forEach(service -> serviceDesugar.engageCustomServiceDesugar(service, env));

        annotationDesugar.rewritePackageAnnotations(pkgNode, env);

        // Add invocation for user specified module init function (`__init()`) if present and return.
        addUserDefinedModuleInitInvocationAndReturn(pkgNode);

        //Sort type definitions with precedence
        pkgNode.typeDefinitions.sort(Comparator.comparing(t -> t.precedence));

        pkgNode.typeDefinitions = rewrite(pkgNode.typeDefinitions, env);
        pkgNode.xmlnsList = rewrite(pkgNode.xmlnsList, env);
        pkgNode.constants = rewrite(pkgNode.constants, env);
        pkgNode.globalVars = rewrite(pkgNode.globalVars, env);

        pkgNode.functions = rewrite(pkgNode.functions, env);

        serviceDesugar.rewriteListeners(pkgNode.globalVars, env);
        serviceDesugar.rewriteServiceAttachments(serviceAttachments, env);

        addNilReturnStatement((BLangBlockFunctionBody) pkgNode.startFunction.funcBody);
        addNilReturnStatement((BLangBlockFunctionBody) pkgNode.stopFunction.funcBody);

        pkgNode.initFunction = splitInitFunction(pkgNode, env);
        pkgNode.initFunction = rewrite(pkgNode.initFunction, env);
        pkgNode.startFunction = rewrite(pkgNode.startFunction, env);
        pkgNode.stopFunction = rewrite(pkgNode.stopFunction, env);

        // Invoke closure desugar.
        closureDesugar.visit(pkgNode);

        pkgNode.getTestablePkgs().forEach(testablePackage -> visit((BLangPackage) testablePackage));
        pkgNode.completedPhases.add(CompilerPhase.DESUGAR);
        initFuncIndex = 0;
        result = pkgNode;
    }

    @Override
    public void visit(BLangImportPackage importPkgNode) {
        BPackageSymbol pkgSymbol = importPkgNode.symbol;
        SymbolEnv pkgEnv = this.symTable.pkgEnvMap.get(pkgSymbol);
        rewrite(pkgEnv.node, pkgEnv);
        result = importPkgNode;
    }

    @Override
    public void visit(BLangTypeDefinition typeDef) {
        if (typeDef.typeNode.getKind() == NodeKind.OBJECT_TYPE
                || typeDef.typeNode.getKind() == NodeKind.RECORD_TYPE) {
            typeDef.typeNode = rewrite(typeDef.typeNode, env);
        }

        typeDef.annAttachments.forEach(attachment ->  rewrite(attachment, env));
        result = typeDef;
    }

    @Override
    public void visit(BLangObjectTypeNode objectTypeNode) {
        // Merge the fields defined within the object and the fields that
        // get inherited via the type references.
        objectTypeNode.fields.addAll(objectTypeNode.referencedFields);

        if (objectTypeNode.flagSet.contains(Flag.ABSTRACT)) {
            result = objectTypeNode;
            return;
        }

        // Add object level variables to the init function.
        Map<BSymbol, BLangStatement> initFunctionStmts = objectTypeNode.generatedInitFunction.initFunctionStmts;
        objectTypeNode.fields.stream()
                // skip if the field is already have an value set by the constructor.
                .filter(field -> !initFunctionStmts.containsKey(field.symbol))
                .filter(field -> field.expr != null)
                .forEachOrdered(field -> {
                    initFunctionStmts.put(field.symbol,
                                          createStructFieldUpdate(objectTypeNode.generatedInitFunction, field));
                });

        // Adding init statements to the init function.
        BLangStatement[] initStmts = initFunctionStmts.values().toArray(new BLangStatement[0]);
        BLangBlockFunctionBody generatedInitFnBody =
                (BLangBlockFunctionBody) objectTypeNode.generatedInitFunction.funcBody;
        int i;
        for (i = 0; i < initFunctionStmts.size(); i++) {
            generatedInitFnBody.stmts.add(i, initStmts[i]);
        }

        if (objectTypeNode.initFunction != null) {
            ((BLangReturn) generatedInitFnBody.stmts.get(i)).expr =
                    createUserDefinedInitInvocation(objectTypeNode);
        }
        result = objectTypeNode;
    }

    private BLangInvocation createUserDefinedInitInvocation(BLangObjectTypeNode objectTypeNode) {
        ArrayList<BLangExpression> paramRefs = new ArrayList<>();
        for (BLangSimpleVariable var : objectTypeNode.generatedInitFunction.requiredParams) {
            paramRefs.add(ASTBuilderUtil.createVariableRef(objectTypeNode.pos, var.symbol));
        }

        BLangInvocation invocation = ASTBuilderUtil.createInvocationExprMethod(objectTypeNode.pos,
                ((BObjectTypeSymbol) objectTypeNode.symbol).initializerFunc.symbol,
                paramRefs, Collections.emptyList(), symResolver);
        if (objectTypeNode.generatedInitFunction.restParam != null) {
            BLangSimpleVarRef restVarRef = ASTBuilderUtil.createVariableRef(objectTypeNode.pos,
                    objectTypeNode.generatedInitFunction.restParam.symbol);
            BLangRestArgsExpression bLangRestArgsExpression = new BLangRestArgsExpression();
            bLangRestArgsExpression.expr = restVarRef;
            bLangRestArgsExpression.pos = objectTypeNode.generatedInitFunction.pos;
            bLangRestArgsExpression.type = objectTypeNode.generatedInitFunction.restParam.type;
            bLangRestArgsExpression.expectedType = bLangRestArgsExpression.type;
            invocation.restArgs.add(bLangRestArgsExpression);
        }
        invocation.exprSymbol =
                ((BObjectTypeSymbol) objectTypeNode.symbol).generatedInitializerFunc.symbol.receiverSymbol;

        return rewriteExpr(invocation);
    }

    @Override
    public void visit(BLangRecordTypeNode recordTypeNode) {
        recordTypeNode.fields.addAll(recordTypeNode.referencedFields);
        // Add struct level variables to the init function.
        recordTypeNode.fields.stream()
                // Only add a field if it is required. Checking if it's required is enough since non-defaultable
                // required fields will have been caught in the type checking phase.
                .filter(field -> !recordTypeNode.initFunction.initFunctionStmts.containsKey(field.symbol) &&
                        !Symbols.isOptional(field.symbol))
                .filter(field -> field.expr != null)
                .forEachOrdered(field -> {
                    recordTypeNode.initFunction.initFunctionStmts.put(field.symbol,
                            createStructFieldUpdate(recordTypeNode.initFunction, field));
                });

        //Adding init statements to the init function.
        BLangStatement[] initStmts = recordTypeNode.initFunction.initFunctionStmts
                .values().toArray(new BLangStatement[0]);
        BLangBlockFunctionBody initFnBody = (BLangBlockFunctionBody) recordTypeNode.initFunction.funcBody;
        for (int i = 0; i < recordTypeNode.initFunction.initFunctionStmts.size(); i++) {
            initFnBody.stmts.add(i, initStmts[i]);
        }

        // TODO:
        // Add invocations for the initializers of each of the type referenced records. Here, the initializers of the
        // referenced types are invoked on the current record type.

        result = recordTypeNode;
    }

    @Override
    public void visit(BLangFunction funcNode) {
        SymbolEnv fucEnv = SymbolEnv.createFunctionEnv(funcNode, funcNode.symbol.scope, env);
        if (!funcNode.interfaceFunction) {
            addReturnIfNotPresent(funcNode);
        }

        // Duplicate the invokable symbol and the invokable type.
        funcNode.originalFuncSymbol = funcNode.symbol;
        funcNode.symbol = ASTBuilderUtil.duplicateInvokableSymbol(funcNode.symbol);
        funcNode.requiredParams = rewrite(funcNode.requiredParams, fucEnv);
        funcNode.workers = rewrite(funcNode.workers, fucEnv);

        List<BLangAnnotationAttachment> participantAnnotation
                = funcNode.annAttachments.stream()
                                         .filter(a -> Transactions.isTransactionsAnnotation(a.pkgAlias.value,
                                                                                            a.annotationName.value))
                                         .collect(Collectors.toList());
        funcNode.funcBody = rewrite(funcNode.funcBody, fucEnv);
        funcNode.annAttachments.forEach(attachment -> rewrite(attachment, env));
        if (funcNode.returnTypeNode != null) {
            funcNode.returnTypeAnnAttachments.forEach(attachment -> rewrite(attachment, env));
        }
        if (Symbols.isNative(funcNode.symbol)) {
            funcNode.externalAnnAttachments.forEach(attachment -> rewrite(attachment, env));
        }
        if (participantAnnotation.isEmpty()) {
            // function not annotated for transaction participation.
            result = funcNode;
            return;
        }

        // If function has transaction participant annotations it will be desugar either to beginLocalParticipant or
        // beginRemoteParticipant function in transaction package.
        //
        //function beginLocalParticipant(string transactionBlockId, function () committedFunc,
        //                               function () abortedFunc, function () trxFunc) returns error|any {
        result = desugarParticipantFunction(funcNode, participantAnnotation);
    }

    private BLangFunction desugarParticipantFunction(BLangFunction funcNode,
                                                     List<BLangAnnotationAttachment> participantAnnotation) {
        BLangAnnotationAttachment annotation = participantAnnotation.get(0);
        BLangBlockFunctionBody onCommitBody = null;
        BLangBlockFunctionBody onAbortBody = null;
        funcNode.requiredParams.forEach(bLangSimpleVariable -> bLangSimpleVariable.symbol.closure = true);
        // If it is attached function set the receiver symbol as a closure variable.
        if (funcNode.receiver != null) {
            funcNode.receiver.symbol.closure = true;
        }
        BType trxReturnType = BUnionType.create(null, symTable.errorType, symTable.anyType);
        BLangType trxReturnNode = ASTBuilderUtil.createTypeNode(trxReturnType);
        // Desuger onCommit and onAbort functions
        BLangLambdaFunction commitFunc = createLambdaFunction(funcNode.pos, "$anonOnCommitFunc$",
                                                              ASTBuilderUtil.createTypeNode(symTable.nilType));
        BLangLambdaFunction abortFunc = createLambdaFunction(funcNode.pos, "$anonOnAbortFunc$",
                                                             ASTBuilderUtil.createTypeNode(symTable.nilType));

        BLangSimpleVariable onCommitTrxVar = ASTBuilderUtil
                .createVariable(funcNode.pos, "$trxId$0", symTable.stringType, null,
                                new BVarSymbol(0, names.fromString("$trxId$0"), this.env.scope.owner.pkgID,
                                               symTable.stringType, commitFunc.function.symbol));
        BLangSimpleVariable onAbortTrxVar = ASTBuilderUtil
                .createVariable(funcNode.pos, "$trxId$0", symTable.stringType, null,
                                new BVarSymbol(0, names.fromString("$trxId$0"), this.env.scope.owner.pkgID,
                                               symTable.stringType, abortFunc.function.symbol));

        BLangSimpleVarRef trxIdOnCommitRef = ASTBuilderUtil.createVariableRef(funcNode.pos, onCommitTrxVar.symbol);

        BLangSimpleVarRef trxIdOnAbortRef = ASTBuilderUtil.createVariableRef(funcNode.pos, onAbortTrxVar.symbol);
        for (RecordLiteralNode.RecordField field : ((BLangRecordLiteral) annotation.expr).getFields()) {
            BLangRecordLiteral.BLangRecordKeyValueField keyValuePair =
                    (BLangRecordLiteral.BLangRecordKeyValueField) field;
            String func = (String) ((BLangLiteral) keyValuePair.getKey()).value;
            switch (func) {
                case Transactions.TRX_ONCOMMIT_FUNC:
                    BInvokableSymbol commitSym = (BInvokableSymbol) ((BLangSimpleVarRef) keyValuePair.valueExpr).symbol;
                    BLangInvocation onCommit = ASTBuilderUtil
                            .createInvocationExprMethod(funcNode.pos, commitSym, Lists.of(trxIdOnCommitRef),
                                                        Collections.emptyList(), symResolver);
                    BLangStatement onCommitStmt = ASTBuilderUtil.createReturnStmt(funcNode.pos, onCommit);
                    onCommitBody = ASTBuilderUtil.createBlockFunctionBody(funcNode.pos, Lists.of(onCommitStmt));
                    break;
                case Transactions.TRX_ONABORT_FUNC:
                    BInvokableSymbol abortSym = (BInvokableSymbol) ((BLangSimpleVarRef) keyValuePair.valueExpr).symbol;
                    BLangInvocation onAbort = ASTBuilderUtil
                            .createInvocationExprMethod(funcNode.pos, abortSym, Lists.of(trxIdOnAbortRef),
                                                        Collections.emptyList(), symResolver);
                    BLangStatement onAbortStmt = ASTBuilderUtil.createReturnStmt(funcNode.pos, onAbort);
                    onAbortBody = ASTBuilderUtil.createBlockFunctionBody(funcNode.pos, Lists.of(onAbortStmt));
                    break;
            }
        }

        if (onCommitBody == null) {
            onCommitBody = ASTBuilderUtil.createBlockFunctionBody(funcNode.pos);
            BLangReturn returnStmt = ASTBuilderUtil.createReturnStmt(funcNode.pos, onCommitBody);
            returnStmt.expr = ASTBuilderUtil.createLiteral(funcNode.pos, symTable.nilType, Names.NIL_VALUE);

        }
        if (onAbortBody == null) {
            onAbortBody = ASTBuilderUtil.createBlockFunctionBody(funcNode.pos);
            BLangReturn returnStmt = ASTBuilderUtil.createReturnStmt(funcNode.pos, onAbortBody);
            returnStmt.expr = ASTBuilderUtil.createLiteral(funcNode.pos, symTable.nilType, Names.NIL_VALUE);
        }

        commitFunc.function.funcBody = onCommitBody;
        commitFunc.function.requiredParams.add(onCommitTrxVar);
        commitFunc.type = new BInvokableType(Lists.of(onCommitTrxVar.symbol.type),
                                             commitFunc.function.symbol.type.getReturnType(), null);
        commitFunc.function.symbol.type = commitFunc.type;
        commitFunc.function.symbol.params = Lists.of(onCommitTrxVar.symbol);

        abortFunc.function.funcBody = onAbortBody;
        abortFunc.function.requiredParams.add(onAbortTrxVar);
        abortFunc.type = new BInvokableType(Lists.of(onAbortTrxVar.symbol.type),
                                            abortFunc.function.symbol.type.getReturnType(), null);
        abortFunc.function.symbol.type = abortFunc.type;
        abortFunc.function.symbol.params = Lists.of(onAbortTrxVar.symbol);

        BSymbol trxModSym = env.enclPkg.imports
                .stream()
                .filter(importPackage -> importPackage.symbol.
                        pkgID.toString().equals(Names.TRANSACTION_ORG.value + Names.ORG_NAME_SEPARATOR.value
                                                        + Names.TRANSACTION_PACKAGE.value))
                .findAny().get().symbol;
        BInvokableSymbol invokableSymbol =
                (BInvokableSymbol) symResolver.lookupSymbol(symTable.pkgEnvMap.get(trxModSym),
                                                            getParticipantFunctionName(funcNode), SymTag.FUNCTION);
        BLangLiteral transactionBlockId = ASTBuilderUtil.createLiteral(funcNode.pos, symTable.stringType,
                                                                       getTransactionBlockId());

        // Wrapper function will be created with transaction participant function body to handle conditional return 
        // values.
        // Then wrapper lambda function will be call through another lambda function which return anydata or 
        // error. They that will be passed to beginParticipant function. Original transaction participant body will 
        // be replace by beginParticipant invocation.
        //  ex .
        //  function participant(){
        //      *participant body*
        // }
        //
        //  function participant(){
        //      beginParticipant(transactionBlockId, trxMainFunc, commintFunct, abortFunc);
        // }
        //
        //  function trxMainFunc() returns anydata|error{
        //      var wrapper = function () retruns <particpantReturnType> {
        //          *participant body*
        //       }
        //      return <anydata|error>wrapper();
        // }
        //
        BLangLambdaFunction trxMainWrapperFunc = createLambdaFunction(funcNode.pos, "$anonTrxWrapperFunc$",
                                                                      Collections.emptyList(),
                                                                      funcNode.returnTypeNode,
                                                                      funcNode.funcBody);

        for (BLangSimpleVariable var : funcNode.requiredParams) {
            trxMainWrapperFunc.function.closureVarSymbols.add(new ClosureVarSymbol(var.symbol, var.pos));
        }

        BLangBlockFunctionBody trxMainBody = ASTBuilderUtil.createBlockFunctionBody(funcNode.pos);
        BLangLambdaFunction trxMainFunc
                = createLambdaFunction(funcNode.pos, "$anonTrxParticipantFunc$", Collections.emptyList(),
                                       trxReturnNode, trxMainBody);
        trxMainWrapperFunc.cachedEnv = trxMainFunc.function.clonedEnv;
        commitFunc.cachedEnv = env.createClone();
        abortFunc.cachedEnv = env.createClone();
        BVarSymbol wrapperSym = new BVarSymbol(0, names.fromString("$wrapper$1"), this.env.scope.owner.pkgID,
                                               trxMainWrapperFunc.type, trxMainFunc.function.symbol);
        BLangSimpleVariable wrapperFuncVar = ASTBuilderUtil.createVariable(funcNode.pos, "$wrapper$1",
                                                                           trxMainWrapperFunc.type, trxMainWrapperFunc,
                                                                           wrapperSym);

        BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDefStmt(funcNode.pos, trxMainBody);
        variableDef.var = wrapperFuncVar;

        BLangSimpleVarRef wrapperVarRef = rewrite(ASTBuilderUtil.createVariableRef(variableDef.pos, 
                                                                         wrapperFuncVar.symbol), env);
        BLangInvocation wrapperInvocation = new BFunctionPointerInvocation(trxMainWrapperFunc.pos, wrapperVarRef, 
                                                                           wrapperFuncVar.symbol, 
                                                                           trxMainWrapperFunc.function.symbol.retType);
        BLangReturn wrapperReturn = ASTBuilderUtil.createReturnStmt(funcNode.pos, addConversionExprIfRequired
                (wrapperInvocation, trxReturnNode.type));

        trxMainWrapperFunc.function.receiver = funcNode.receiver;
        trxMainFunc.function.receiver = funcNode.receiver;
        trxMainBody.stmts.add(wrapperReturn);
        rewrite(trxMainFunc.function, env);

        List<BLangExpression> requiredArgs = Lists.of(transactionBlockId, trxMainFunc, commitFunc, abortFunc);
        BLangInvocation participantInvocation
                = ASTBuilderUtil.createInvocationExprMethod(funcNode.pos, invokableSymbol, requiredArgs,
                                                            Collections.emptyList(), symResolver);
        participantInvocation.type = ((BInvokableType) invokableSymbol.type).retType;
        BLangStatement stmt = ASTBuilderUtil.createReturnStmt(funcNode.pos, addConversionExprIfRequired
                (participantInvocation, funcNode.symbol.retType));
        funcNode.funcBody = ASTBuilderUtil.createBlockFunctionBody(funcNode.pos, Lists.of(rewrite(stmt, env)));
        
        return funcNode;
    }

    private Name getParticipantFunctionName(BLangFunction function) {
        if (Symbols.isFlagOn((function).symbol.flags, Flags.RESOURCE)) {
            return TRX_REMOTE_PARTICIPANT_BEGIN_FUNCTION;
        }
        return TRX_LOCAL_PARTICIPANT_BEGIN_FUNCTION;
    }

    @Override
    public void visit(BLangResource resourceNode) {
    }

    public void visit(BLangAnnotation annotationNode) {
        annotationNode.annAttachments.forEach(attachment ->  rewrite(attachment, env));
    }

    public void visit(BLangAnnotationAttachment annAttachmentNode) {
        annAttachmentNode.expr = rewrite(annAttachmentNode.expr, env);
        result = annAttachmentNode;
    }

    @Override
    public void visit(BLangSimpleVariable varNode) {
        if ((varNode.symbol.owner.tag & SymTag.INVOKABLE) != SymTag.INVOKABLE) {
            varNode.expr = null;
            result = varNode;
            return;
        }

        // Return if this assignment is not a safe assignment
        BLangExpression bLangExpression = rewriteExpr(varNode.expr);
        if (bLangExpression != null) {
            bLangExpression = addConversionExprIfRequired(bLangExpression, varNode.type);
        }
        varNode.expr = bLangExpression;

        varNode.annAttachments.forEach(attachment ->  rewrite(attachment, env));

        result = varNode;
    }

    @Override
    public void visit(BLangTupleVariable varNode) {
        result = varNode;
    }

    @Override
    public void visit(BLangRecordVariable varNode) {
        result = varNode;
    }

    @Override
    public void visit(BLangErrorVariable varNode) {
        result = varNode;
    }

    // Statements

    @Override
    public void visit(BLangBlockStmt block) {
        SymbolEnv blockEnv = SymbolEnv.createBlockEnv(block, env);
        block.stmts = rewriteStmt(block.stmts, blockEnv);
        result = block;
    }

    @Override
    public void visit(BLangSimpleVariableDef varDefNode) {
        varDefNode.var = rewrite(varDefNode.var, env);
        result = varDefNode;
    }

    @Override
    public void visit(BLangTupleVariableDef varDefNode) {
        //  case 1:
        //  (string, int) (a, b) = (tuple)
        //
        //  any[] x = (tuple);
        //  string a = x[0];
        //  int b = x[1];
        //
        //  case 2:
        //  ((string, float) int)) ((a, b), c)) = (tuple)
        //
        //  any[] x = (tuple);
        //  string a = x[0][0];
        //  float b = x[0][1];
        //  int c = x[1];
        varDefNode.var = rewrite(varDefNode.var, env);
        BLangTupleVariable tupleVariable = varDefNode.var;

        //create tuple destruct block stmt
        final BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(varDefNode.pos);

        //create a array of any-type based on the dimension
        BType runTimeType = new BArrayType(symTable.anyType);

        //create a simple var for the array 'any[] x = (tuple)' based on the dimension for x
        String name = "tuple";
        final BLangSimpleVariable tuple = ASTBuilderUtil.createVariable(varDefNode.pos, name, runTimeType, null,
                new BVarSymbol(0, names.fromString(name), this.env.scope.owner.pkgID, runTimeType,
                        this.env.scope.owner));
        tuple.expr = tupleVariable.expr;
        final BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDefStmt(varDefNode.pos, blockStmt);
        variableDef.var = tuple;

        //create the variable definition statements using the root block stmt created
        createVarDefStmts(tupleVariable, blockStmt, tuple.symbol, null);
        createRestFieldVarDefStmts(tupleVariable, blockStmt, tuple.symbol);

        //finally rewrite the populated block statement
        result = rewrite(blockStmt, env);
    }

    private void createRestFieldVarDefStmts(BLangTupleVariable parentTupleVariable, BLangBlockStmt blockStmt,
                                            BVarSymbol tupleVarSymbol) {
        final BLangSimpleVariable arrayVar = (BLangSimpleVariable) parentTupleVariable.restVariable;
        boolean isTupleType = parentTupleVariable.type.tag == TypeTags.TUPLE;
        DiagnosticPos pos = blockStmt.pos;
        if (arrayVar != null) {
            // T[] t = [];
            BLangArrayLiteral arrayExpr = createArrayLiteralExprNode();
            arrayExpr.type = arrayVar.type;
            arrayVar.expr = arrayExpr;
            BLangSimpleVariableDef arrayVarDef = ASTBuilderUtil.createVariableDefStmt(arrayVar.pos, blockStmt);
            arrayVarDef.var = arrayVar;

            // foreach var $foreach$i in tupleTypes.length()...tupleLiteral.length() {
            //     t[t.length()] = <T> tupleLiteral[$foreach$i];
            // }
            BLangExpression tupleExpr = parentTupleVariable.expr;
            BLangSimpleVarRef arrayVarRef = ASTBuilderUtil.createVariableRef(pos, arrayVar.symbol);

            BLangLiteral startIndexLiteral = (BLangLiteral) TreeBuilder.createLiteralExpression();
            startIndexLiteral.value = (long) (isTupleType ? ((BTupleType) parentTupleVariable.type).tupleTypes.size()
                    : parentTupleVariable.memberVariables.size());
            startIndexLiteral.type = symTable.intType;
            BLangInvocation lengthInvocation = createLengthInvocation(pos, tupleExpr);
            BLangInvocation intRangeInvocation = replaceWithIntRange(pos, startIndexLiteral,
                    getModifiedIntRangeEndExpr(lengthInvocation));

            BLangForeach foreach = (BLangForeach) TreeBuilder.createForeachNode();
            foreach.pos = pos;
            foreach.collection = intRangeInvocation;
            types.setForeachTypedBindingPatternType(foreach);

            final BLangSimpleVariable foreachVariable = ASTBuilderUtil.createVariable(pos,
                    "$foreach$i", foreach.varType);
            foreachVariable.symbol = new BVarSymbol(0, names.fromIdNode(foreachVariable.name),
                    this.env.scope.owner.pkgID, foreachVariable.type, this.env.scope.owner);
            BLangSimpleVarRef foreachVarRef = ASTBuilderUtil.createVariableRef(pos, foreachVariable.symbol);
            foreach.variableDefinitionNode = ASTBuilderUtil.createVariableDef(pos, foreachVariable);
            foreach.isDeclaredWithVar = true;
            BLangBlockStmt foreachBody = ASTBuilderUtil.createBlockStmt(pos);

            // t[t.length()] = <T> tupleLiteral[$foreach$i];
            BLangIndexBasedAccess indexAccessExpr = ASTBuilderUtil.createIndexAccessExpr(arrayVarRef,
                    createLengthInvocation(pos, arrayVarRef));
            indexAccessExpr.type = (isTupleType ? ((BTupleType) parentTupleVariable.type).restType : symTable.anyType);
            createSimpleVarRefAssignmentStmt(indexAccessExpr, foreachBody, foreachVarRef, tupleVarSymbol, null);
            foreach.body = foreachBody;
            blockStmt.addStatement(foreach);
        }
    }

    @Override
    public void visit(BLangRecordVariableDef varDefNode) {

        BLangRecordVariable varNode = varDefNode.var;

        final BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(varDefNode.pos);

        BType runTimeType = new BMapType(TypeTags.MAP, symTable.anyType, null);

        final BLangSimpleVariable mapVariable = ASTBuilderUtil.createVariable(varDefNode.pos, "$map$0", runTimeType,
                null, new BVarSymbol(0, names.fromString("$map$0"), this.env.scope.owner.pkgID,
                        runTimeType, this.env.scope.owner));
        mapVariable.expr = varDefNode.var.expr;
        final BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDefStmt(varDefNode.pos, blockStmt);
        variableDef.var = mapVariable;

        createVarDefStmts(varNode, blockStmt, mapVariable.symbol, null);

        result = rewrite(blockStmt, env);
    }

    @Override
    public void visit(BLangErrorVariableDef varDefNode) {
        BLangErrorVariable errorVariable = varDefNode.errorVariable;

        // Create error destruct block stmt.
        final BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(varDefNode.pos);

        // Create a simple var for the error 'error x = ($error$)'.
        BVarSymbol errorVarSymbol = new BVarSymbol(0, names.fromString("$error$"),
                this.env.scope.owner.pkgID, symTable.errorType, this.env.scope.owner);
        final BLangSimpleVariable error = ASTBuilderUtil.createVariable(varDefNode.pos, errorVarSymbol.name.value,
                symTable.errorType, null, errorVarSymbol);
        error.expr = errorVariable.expr;
        final BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDefStmt(varDefNode.pos, blockStmt);
        variableDef.var = error;

        // Create the variable definition statements using the root block stmt created.
        createVarDefStmts(errorVariable, blockStmt, error.symbol, null);

        // Finally rewrite the populated block statement.
        result = rewrite(blockStmt, env);
    }

    /**
     * This method iterate through each member of the tupleVar and create the relevant var def statements. This method
     * does the check for node kind of each member and call the related var def creation method.
     *
     * Example:
     * ((string, float) int)) ((a, b), c)) = (tuple)
     *
     * (a, b) is again a tuple, so it is a recursive var def creation.
     *
     * c is a simple var, so a simple var def will be created.
     *
     */
    private void createVarDefStmts(BLangTupleVariable parentTupleVariable, BLangBlockStmt parentBlockStmt,
                                   BVarSymbol tupleVarSymbol, BLangIndexBasedAccess parentIndexAccessExpr) {

        final List<BLangVariable> memberVars = parentTupleVariable.memberVariables;
        for (int index = 0; index < memberVars.size(); index++) {
            BLangVariable variable = memberVars.get(index);
            BLangLiteral indexExpr = ASTBuilderUtil.createLiteral(variable.pos, symTable.intType, (long) index);

            if (NodeKind.VARIABLE == variable.getKind()) { //if this is simple var, then create a simple var def stmt
                createSimpleVarDefStmt((BLangSimpleVariable) variable, parentBlockStmt, indexExpr, tupleVarSymbol,
                        parentIndexAccessExpr);
                continue;
            }

            if (variable.getKind() == NodeKind.TUPLE_VARIABLE) { // Else recursively create the var def statements.
                BLangTupleVariable tupleVariable = (BLangTupleVariable) variable;
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(tupleVariable.pos,
                        new BArrayType(symTable.anyType), tupleVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarDefStmts((BLangTupleVariable) variable, parentBlockStmt, tupleVarSymbol, arrayAccessExpr);
                continue;
            }

            if (variable.getKind() == NodeKind.RECORD_VARIABLE) {
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(
                        parentTupleVariable.pos, symTable.mapType, tupleVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarDefStmts((BLangRecordVariable) variable, parentBlockStmt, tupleVarSymbol, arrayAccessExpr);
                continue;
            }

            if (variable.getKind() == NodeKind.ERROR_VARIABLE) {

                BType accessedElemType = symTable.errorType;
                if (tupleVarSymbol.type.tag == TypeTags.ARRAY) {
                    BArrayType arrayType = (BArrayType) tupleVarSymbol.type;
                    accessedElemType = arrayType.eType;
                }
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(
                        parentTupleVariable.pos, accessedElemType, tupleVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarDefStmts((BLangErrorVariable) variable, parentBlockStmt, tupleVarSymbol, arrayAccessExpr);
            }
        }
    }

    /**
     * Overloaded method to handle record variables.
     * This method iterate through each member of the recordVar and create the relevant var def statements. This method
     * does the check for node kind of each member and call the related var def creation method.
     *
     * Example:
     * type Foo record {
     *     string name;
     *     (int, string) age;
     *     Address address;
     * };
     *
     * Foo {name: a, age: (b, c), address: d} = {record literal}
     *
     *  a is a simple var, so a simple var def will be created.
     *
     * (b, c) is a tuple, so it is a recursive var def creation.
     *
     * d is a record, so it is a recursive var def creation.
     *
     */
    private void createVarDefStmts(BLangRecordVariable parentRecordVariable, BLangBlockStmt parentBlockStmt,
                                   BVarSymbol recordVarSymbol, BLangIndexBasedAccess parentIndexAccessExpr) {

        List<BLangRecordVariableKeyValue> variableList = parentRecordVariable.variableList;
        for (BLangRecordVariableKeyValue recordFieldKeyValue : variableList) {
            BLangVariable variable = recordFieldKeyValue.valueBindingPattern;
            BLangLiteral indexExpr = ASTBuilderUtil.createLiteral(variable.pos, symTable.stringType,
                    recordFieldKeyValue.key.value);

            if (recordFieldKeyValue.valueBindingPattern.getKind() == NodeKind.VARIABLE) {
                createSimpleVarDefStmt((BLangSimpleVariable) recordFieldKeyValue.valueBindingPattern, parentBlockStmt,
                        indexExpr, recordVarSymbol, parentIndexAccessExpr);
                continue;
            }

            if (recordFieldKeyValue.valueBindingPattern.getKind() == NodeKind.TUPLE_VARIABLE) {
                BLangTupleVariable tupleVariable = (BLangTupleVariable) recordFieldKeyValue.valueBindingPattern;
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(tupleVariable.pos,
                        new BArrayType(symTable.anyType), recordVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarDefStmts((BLangTupleVariable) recordFieldKeyValue.valueBindingPattern,
                        parentBlockStmt, recordVarSymbol, arrayAccessExpr);
                continue;
            }

            if (recordFieldKeyValue.valueBindingPattern.getKind() == NodeKind.RECORD_VARIABLE) {
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(
                        parentRecordVariable.pos, symTable.mapType, recordVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarDefStmts((BLangRecordVariable) recordFieldKeyValue.valueBindingPattern, parentBlockStmt,
                        recordVarSymbol, arrayAccessExpr);
                continue;
            }

            if (variable.getKind() == NodeKind.ERROR_VARIABLE) {
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(
                        parentRecordVariable.pos, variable.type, recordVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarDefStmts((BLangErrorVariable) variable, parentBlockStmt, recordVarSymbol, arrayAccessExpr);
            }
        }

        if (parentRecordVariable.restParam != null) {
            // The restParam is desugared to a filter iterable operation that filters out the fields provided in the
            // record variable
            // map<any> restParam = $map$0.filter($lambdaArg$0);

            DiagnosticPos pos = parentBlockStmt.pos;
            BMapType restParamType = (BMapType) ((BLangVariable) parentRecordVariable.restParam).type;
            BLangSimpleVarRef variableReference;

            if (parentIndexAccessExpr != null) {
                BLangSimpleVariable mapVariable = ASTBuilderUtil.createVariable(pos, "$map$1",
                        parentIndexAccessExpr.type, null, new BVarSymbol(0, names.fromString("$map$1"),
                                this.env.scope.owner.pkgID, parentIndexAccessExpr.type, this.env.scope.owner));
                mapVariable.expr = parentIndexAccessExpr;
                BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDefStmt(pos, parentBlockStmt);
                variableDef.var = mapVariable;

                variableReference = ASTBuilderUtil.createVariableRef(pos, mapVariable.symbol);
            } else {
                variableReference = ASTBuilderUtil.createVariableRef(pos,
                        ((BLangSimpleVariableDef) parentBlockStmt.stmts.get(0)).var.symbol);
            }

            List<String> keysToRemove = parentRecordVariable.variableList.stream()
                    .map(var -> var.getKey().getValue())
                    .collect(Collectors.toList());

            BLangSimpleVariable filteredDetail = generateRestFilter(variableReference, pos,
                    keysToRemove, restParamType, parentBlockStmt);

            BLangSimpleVarRef varRef = ASTBuilderUtil.createVariableRef(pos, filteredDetail.symbol);

            // Create rest param variable definition
            BLangSimpleVariable restParam = (BLangSimpleVariable) parentRecordVariable.restParam;
            BLangSimpleVariableDef restParamVarDef = ASTBuilderUtil.createVariableDefStmt(pos,
                    parentBlockStmt);
            restParamVarDef.var = restParam;
            restParamVarDef.var.type = restParamType;
            restParam.expr = varRef;
        }
    }

    /**
     * This method will create the relevant var def statements for reason and details of the error variable.
     * The var def statements are created by creating the reason() and detail() builtin methods.
     */
    private void createVarDefStmts(BLangErrorVariable parentErrorVariable, BLangBlockStmt parentBlockStmt,
                                   BVarSymbol errorVariableSymbol, BLangIndexBasedAccess parentIndexBasedAccess) {

        BVarSymbol convertedErrorVarSymbol;
        if (parentIndexBasedAccess != null) {
            BType prevType = parentIndexBasedAccess.type;
            parentIndexBasedAccess.type = symTable.anyType;
            BLangSimpleVariableDef errorVarDef = createVarDef("$error$" + errorCount++,
                    symTable.errorType,
                    addConversionExprIfRequired(parentIndexBasedAccess, symTable.errorType),
                    parentErrorVariable.pos);
            parentIndexBasedAccess.type = prevType;
            parentBlockStmt.addStatement(errorVarDef);
            convertedErrorVarSymbol = errorVarDef.var.symbol;
        } else {
            convertedErrorVarSymbol = errorVariableSymbol;
        }

        parentErrorVariable.reason.expr = generateErrorReasonBuiltinFunction(parentErrorVariable.reason.pos,
                parentErrorVariable.reason.type, convertedErrorVarSymbol, null);

        if (names.fromIdNode((parentErrorVariable.reason).name) == Names.IGNORE) {
            parentErrorVariable.reason = null;
        } else {
            BLangSimpleVariableDef reasonVariableDef =
                    ASTBuilderUtil.createVariableDefStmt(parentErrorVariable.reason.pos, parentBlockStmt);
            reasonVariableDef.var = parentErrorVariable.reason;
        }

        if ((parentErrorVariable.detail == null || parentErrorVariable.detail.isEmpty())
            && parentErrorVariable.restDetail == null) {
            return;
        }

        BType detailMapType;
        BType detailType = ((BErrorType) parentErrorVariable.type).detailType;
        if (detailType.tag == TypeTags.MAP) {
            detailMapType = detailType;
        } else {
            detailMapType = symTable.detailType;
        }

        parentErrorVariable.detailExpr = generateErrorDetailBuiltinFunction(
                parentErrorVariable.pos,
                convertedErrorVarSymbol, null);

        BLangSimpleVariableDef detailTempVarDef = createVarDef("$error$detail",
                parentErrorVariable.detailExpr.type, parentErrorVariable.detailExpr, parentErrorVariable.pos);
        detailTempVarDef.type = parentErrorVariable.detailExpr.type;
        parentBlockStmt.addStatement(detailTempVarDef);
        this.env.scope.define(names.fromIdNode(detailTempVarDef.var.name), detailTempVarDef.var.symbol);

        for (BLangErrorVariable.BLangErrorDetailEntry detailEntry : parentErrorVariable.detail) {
            BLangExpression detailEntryVar = createErrorDetailVar(detailEntry, detailTempVarDef.var.symbol);

            // create the bound variable, and final rewrite will define them in sym table.
            createAndAddBoundVariableDef(parentBlockStmt, detailEntry, detailEntryVar);

        }
        if (parentErrorVariable.restDetail != null && !parentErrorVariable.restDetail.name.value.equals(IGNORE.value)) {
            DiagnosticPos pos = parentErrorVariable.restDetail.pos;
            BLangSimpleVarRef detailVarRef = ASTBuilderUtil.createVariableRef(
                    pos, detailTempVarDef.var.symbol);
            List<String> keysToRemove = parentErrorVariable.detail.stream()
                    .map(detail -> detail.key.getValue())
                    .collect(Collectors.toList());

            BLangSimpleVariable filteredDetail = generateRestFilter(detailVarRef, parentErrorVariable.pos, keysToRemove,
                    parentErrorVariable.restDetail.type, parentBlockStmt);
            BLangSimpleVariableDef variableDefStmt = ASTBuilderUtil.createVariableDefStmt(pos, parentBlockStmt);
            variableDefStmt.var = ASTBuilderUtil.createVariable(pos,
                    parentErrorVariable.restDetail.name.value,
                    filteredDetail.type,
                    ASTBuilderUtil.createVariableRef(pos, filteredDetail.symbol),
                    parentErrorVariable.restDetail.symbol);
            BLangAssignment assignmentStmt = ASTBuilderUtil.createAssignmentStmt(pos,
                    ASTBuilderUtil.createVariableRef(pos, parentErrorVariable.restDetail.symbol),
                    ASTBuilderUtil.createVariableRef(pos, filteredDetail.symbol));
            parentBlockStmt.addStatement(assignmentStmt);
        }
        rewrite(parentBlockStmt, env);
    }

    private BLangSimpleVariableDef forceCastIfApplicable(BVarSymbol errorVarySymbol, DiagnosticPos pos,
                                                         BType targetType) {
        BVarSymbol errorVarSym = new BVarSymbol(Flags.PUBLIC, names.fromString("$cast$temp$"),
                this.env.enclPkg.packageID, targetType, this.env.scope.owner);
        BLangSimpleVarRef variableRef = ASTBuilderUtil.createVariableRef(pos, errorVarySymbol);

        BLangExpression expr;
        if (targetType.tag == TypeTags.RECORD) {
            expr = variableRef;
        } else {
            expr = addConversionExprIfRequired(variableRef, targetType);
        }
        BLangSimpleVariable errorVar = ASTBuilderUtil.createVariable(pos, errorVarSym.name.value, targetType, expr,
                errorVarSym);
        return ASTBuilderUtil.createVariableDef(pos, errorVar);
    }

    private BLangSimpleVariable generateRestFilter(BLangSimpleVarRef mapVarRef, DiagnosticPos pos,
                                                   List<String> keysToRemove, BType targetType,
                                                   BLangBlockStmt parentBlockStmt) {
        //      restVar = (<map<T>mapVarRef)
        //                       .entries()
        //                       .filter([key, val] => isKeyTakenLambdaInvoke())
        //                       .map([key, val] => val)
        //                       .constructFrom(errorDetail);

        BLangExpression typeCastExpr = addConversionExprIfRequired(mapVarRef, targetType);

        int restNum = annonVarCount++;
        String name = "$map$ref$" + restNum;
        BLangSimpleVariable mapVariable = defVariable(pos, targetType, parentBlockStmt, typeCastExpr, name);

        BLangInvocation entriesInvocation = generateMapEntriesInvocation(pos, typeCastExpr, mapVariable);
        String entriesVarName = "$map$ref$entries$" + restNum;
        BType entriesType = new BMapType(TypeTags.MAP,
                new BTupleType(Arrays.asList(symTable.stringType, ((BMapType) targetType).constraint)), null);
        BLangSimpleVariable entriesInvocationVar = defVariable(pos, entriesType, parentBlockStmt,
                addConversionExprIfRequired(entriesInvocation, entriesType),
                entriesVarName);

        BLangLambdaFunction filter = createFuncToFilterOutRestParam(keysToRemove, pos);

        BLangInvocation filterInvocation = generateMapFilterInvocation(pos, entriesInvocationVar, filter);
        String filteredEntriesName = "$filtered$detail$entries" + restNum;
        BLangSimpleVariable filteredVar = defVariable(pos, entriesType, parentBlockStmt, filterInvocation,
                filteredEntriesName);

        String filteredVarName = "$detail$filtered" + restNum;
        BLangLambdaFunction backToMapLambda = generateEntriesToMapLambda(pos);
        BLangInvocation mapInvocation = generateMapMapInvocation(pos, filteredVar, backToMapLambda);
        BLangSimpleVariable filtered = defVariable(pos, targetType, parentBlockStmt,
                mapInvocation,
                filteredVarName);

        String filteredRestVarName = "$restVar$" + restNum;
        BLangInvocation constructed = generateConstructFromInvocation(pos, targetType, filtered.symbol);
        return defVariable(pos, targetType, parentBlockStmt,
                addConversionExprIfRequired(constructed, targetType),
                filteredRestVarName);
    }

    private BLangInvocation generateMapEntriesInvocation(DiagnosticPos pos, BLangExpression typeCastExpr,
                                                         BLangSimpleVariable detailMap) {
        BLangInvocation invocationNode = createInvocationNode("entries", new ArrayList<>(), typeCastExpr.type);

        invocationNode.expr = ASTBuilderUtil.createVariableRef(pos, detailMap.symbol);
        invocationNode.symbol = symResolver.lookupLangLibMethod(typeCastExpr.type, names.fromString("entries"));
        invocationNode.requiredArgs = Lists.of(ASTBuilderUtil.createVariableRef(pos, detailMap.symbol));
        invocationNode.type = invocationNode.symbol.type.getReturnType();
        return invocationNode;
    }

    private BLangInvocation generateMapMapInvocation(DiagnosticPos pos, BLangSimpleVariable filteredVar,
                                                     BLangLambdaFunction backToMapLambda) {
        BLangInvocation invocationNode = createInvocationNode("map", new ArrayList<>(), filteredVar.type);

        invocationNode.expr = ASTBuilderUtil.createVariableRef(pos, filteredVar.symbol);
        invocationNode.symbol = symResolver.lookupLangLibMethod(filteredVar.type, names.fromString("map"));
        invocationNode.requiredArgs = Lists.of(ASTBuilderUtil.createVariableRef(pos, filteredVar.symbol));
        invocationNode.type = invocationNode.symbol.type.getReturnType();
        invocationNode.requiredArgs.add(backToMapLambda);
        return invocationNode;
    }

    private BLangLambdaFunction generateEntriesToMapLambda(DiagnosticPos pos) {
        // var.map([key, val] => val)

        String anonfuncName = "$anonGetValFunc$" + lambdaFunctionCount++;
        BLangFunction function = ASTBuilderUtil.createFunction(pos, anonfuncName);

        BVarSymbol keyValSymbol = new BVarSymbol(0, names.fromString("$lambdaArg$0"), this.env.scope.owner.pkgID,
                getStringAnyTupleType(), this.env.scope.owner);

        BLangSimpleVariable inputParameter = ASTBuilderUtil.createVariable(pos, null, getStringAnyTupleType(),
                                                                           null, keyValSymbol);
        function.requiredParams.add(inputParameter);

        BLangValueType anyType = new BLangValueType();
        anyType.typeKind = TypeKind.ANY;
        anyType.type = symTable.anyType;
        function.returnTypeNode = anyType;

        BLangBlockFunctionBody functionBlock = ASTBuilderUtil.createBlockFunctionBody(pos, new ArrayList<>());
        function.funcBody = functionBlock;

        BLangIndexBasedAccess indexBasesAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(pos,
                                                                                               symTable.anyType,
                                                                                               keyValSymbol,
                                                                                               ASTBuilderUtil
                                                                                                       .createLiteral(
                                                                                                               pos,
                                                                                                               symTable.intType,
                                                                                                               (long) 1));
        BLangSimpleVariableDef tupSecondElem = createVarDef("val", indexBasesAccessExpr.type,
                                                            indexBasesAccessExpr, pos);
        functionBlock.addStatement(tupSecondElem);

        // Create return stmt.
        BLangReturn returnStmt = ASTBuilderUtil.createReturnStmt(pos, functionBlock);
        returnStmt.expr = ASTBuilderUtil.createVariableRef(pos, tupSecondElem.var.symbol);

        // Create function symbol before visiting desugar phase for the function
        BInvokableSymbol functionSymbol = Symbols.createFunctionSymbol(Flags.asMask(function.flagSet),
                new Name(function.name.value), env.enclPkg.packageID, function.type, env.enclEnv.enclVarSym, true);
        functionSymbol.retType = function.returnTypeNode.type;
        functionSymbol.params = function.requiredParams.stream()
                .map(param -> param.symbol)
                .collect(Collectors.toList());
        functionSymbol.scope = env.scope;
        functionSymbol.type = new BInvokableType(Collections.singletonList(getStringAnyTupleType()),
                symTable.anyType, null);
        function.symbol = functionSymbol;
        rewrite(function, env);
        env.enclPkg.addFunction(function);

        // Create and return a lambda function
        return createLambdaFunction(function, functionSymbol);
    }

    private BLangInvocation generateMapFilterInvocation(DiagnosticPos pos,
                                                        BLangSimpleVariable entriesInvocationVar,
                                                        BLangLambdaFunction filter) {
        BLangInvocation invocationNode = createInvocationNode("filter", new ArrayList<>(), entriesInvocationVar.type);

        invocationNode.expr = ASTBuilderUtil.createVariableRef(pos, entriesInvocationVar.symbol);
        invocationNode.symbol = symResolver.lookupLangLibMethod(entriesInvocationVar.type, names.fromString("filter"));
        invocationNode.requiredArgs = Lists.of(ASTBuilderUtil.createVariableRef(pos, entriesInvocationVar.symbol));
        invocationNode.type = invocationNode.symbol.type.getReturnType();
        invocationNode.requiredArgs.add(filter);

        return invocationNode;
    }

    private BLangSimpleVariable defVariable(DiagnosticPos pos, BType varType, BLangBlockStmt parentBlockStmt,
                                            BLangExpression expression, String name) {
        Name varName = names.fromString(name);
        BLangSimpleVariable detailMap = ASTBuilderUtil.createVariable(pos, name, varType, expression,
                new BVarSymbol(Flags.PUBLIC, varName, env.enclPkg.packageID, varType, env.scope.owner));
        BLangSimpleVariableDef constructedMap = ASTBuilderUtil.createVariableDef(pos, detailMap);
        constructedMap.type = varType;
        parentBlockStmt.addStatement(constructedMap);
        env.scope.define(varName, detailMap.symbol);
        return detailMap;
    }

    private void createAndAddBoundVariableDef(BLangBlockStmt parentBlockStmt,
                                              BLangErrorVariable.BLangErrorDetailEntry detailEntry,
                                              BLangExpression detailEntryVar) {
        if (detailEntry.valueBindingPattern.getKind() == NodeKind.VARIABLE) {
            BLangSimpleVariableDef errorDetailVar = createVarDef(
                    ((BLangSimpleVariable) detailEntry.valueBindingPattern).name.value,
                    detailEntry.valueBindingPattern.type,
                    detailEntryVar,
                    detailEntry.valueBindingPattern.pos);
            parentBlockStmt.addStatement(errorDetailVar);

        } else if (detailEntry.valueBindingPattern.getKind() == NodeKind.RECORD_VARIABLE) {
            BLangRecordVariableDef recordVariableDef = ASTBuilderUtil.createRecordVariableDef(
                    detailEntry.valueBindingPattern.pos,
                    (BLangRecordVariable) detailEntry.valueBindingPattern);
            recordVariableDef.var.expr = detailEntryVar;
            recordVariableDef.type = symTable.recordType;
            parentBlockStmt.addStatement(recordVariableDef);

        } else if (detailEntry.valueBindingPattern.getKind() == NodeKind.TUPLE_VARIABLE) {
            BLangTupleVariableDef tupleVariableDef = ASTBuilderUtil.createTupleVariableDef(
                    detailEntry.valueBindingPattern.pos, (BLangTupleVariable) detailEntry.valueBindingPattern);
            parentBlockStmt.addStatement(tupleVariableDef);
        }
    }

    private BLangExpression createErrorDetailVar(BLangErrorVariable.BLangErrorDetailEntry detailEntry,
                                                 BVarSymbol tempDetailVarSymbol) {
        BLangExpression detailEntryVar = createIndexBasedAccessExpr(
                detailEntry.valueBindingPattern.type,
                detailEntry.valueBindingPattern.pos,
                createStringLiteral(detailEntry.key.pos, detailEntry.key.value),
                tempDetailVarSymbol, null);
        if (detailEntryVar.getKind() == NodeKind.INDEX_BASED_ACCESS_EXPR) {
            BLangIndexBasedAccess bLangIndexBasedAccess = (BLangIndexBasedAccess) detailEntryVar;
            bLangIndexBasedAccess.originalType = symTable.pureType;
        }
        return detailEntryVar;
    }

    private BLangExpression constructStringTemplateConcatExpression(List<BLangExpression> exprs) {
        BLangExpression concatExpr = null;
        BLangExpression currentExpr;
        for (BLangExpression expr : exprs) {
            currentExpr = expr;
            if (expr.type.tag != TypeTags.STRING && expr.type.tag != TypeTags.XML) {
                currentExpr = getToStringInvocationOnExpr(expr);
            }

            if (concatExpr == null) {
                concatExpr = currentExpr;
                continue;
            }

            concatExpr =
                    ASTBuilderUtil.createBinaryExpr(concatExpr.pos, concatExpr, currentExpr,
                                                    concatExpr.type.tag == TypeTags.XML ||
                                                            currentExpr.type.tag == TypeTags.XML ?
                                                            symTable.xmlType : symTable.stringType,
                                                    OperatorKind.ADD, null);
        }
        return concatExpr;
    }

    private BLangInvocation getToStringInvocationOnExpr(BLangExpression expression) {
        BInvokableSymbol symbol = (BInvokableSymbol) symTable.langValueModuleSymbol.scope
                .lookup(names.fromString(TO_STRING_FUNCTION_NAME)).symbol;

        List<BLangExpression> requiredArgs = new ArrayList<BLangExpression>() {{
            add(addConversionExprIfRequired(expression, symbol.params.get(0).type));
        }};
        return ASTBuilderUtil.createInvocationExprMethod(expression.pos, symbol, requiredArgs, new ArrayList<>(),
                                                         symResolver);
    }

    // TODO: Move the logic on binding patterns to a seperate class
    private BLangInvocation generateErrorDetailBuiltinFunction(DiagnosticPos pos, BVarSymbol errorVarySymbol,
                                                               BLangIndexBasedAccess parentIndexBasedAccess) {
        BLangExpression onExpr =
                parentIndexBasedAccess != null
                        ? parentIndexBasedAccess : ASTBuilderUtil.createVariableRef(pos, errorVarySymbol);

        return createLangLibInvocationNode(ERROR_DETAIL_FUNCTION_NAME, onExpr, new ArrayList<>(), null, pos);
    }

    private BLangInvocation generateErrorReasonBuiltinFunction(DiagnosticPos pos, BType reasonType,
                                                               BVarSymbol errorVarSymbol,
                                                               BLangIndexBasedAccess parentIndexBasedAccess) {
        BLangExpression onExpr =
                parentIndexBasedAccess != null
                        ? parentIndexBasedAccess : ASTBuilderUtil.createVariableRef(pos, errorVarSymbol);
        return createLangLibInvocationNode(ERROR_REASON_FUNCTION_NAME, onExpr, new ArrayList<>(), reasonType, pos);
    }

    private BLangInvocation generateConstructFromInvocation(DiagnosticPos pos,
                                                            BType targetType,
                                                            BVarSymbol source) {
        BType typedescType = new BTypedescType(targetType, symTable.typeDesc.tsymbol);
        BLangInvocation invocationNode = createInvocationNode(CONSTRUCT_FROM, new ArrayList<>(), typedescType);

        BLangTypedescExpr typedescExpr = new BLangTypedescExpr();
        typedescExpr.resolvedType = targetType;
        typedescExpr.type = typedescType;

        invocationNode.expr = typedescExpr;
        invocationNode.symbol = symResolver.lookupLangLibMethod(typedescType, names.fromString(CONSTRUCT_FROM));
        invocationNode.requiredArgs = Lists.of(typedescExpr, ASTBuilderUtil.createVariableRef(pos, source));
        invocationNode.type = BUnionType.create(null, targetType, symTable.errorType);
        return invocationNode;
    }

    private BLangLambdaFunction createFuncToFilterOutRestParam(List<String> toRemoveList, DiagnosticPos pos) {

        // Creates following anonymous function
        //
        // function ((string, any) $lambdaArg$0) returns boolean {
        //     Following if block is generated for all parameters given in the record variable
        //     if ($lambdaArg$0[0] == "name") {
        //         return false;
        //     }
        //     if ($lambdaArg$0[0] == "age") {
        //         return false;
        //     }
        //      return true;
        // }

        String anonfuncName = "$anonRestParamFilterFunc$" + lambdaFunctionCount++;
        BLangFunction function = ASTBuilderUtil.createFunction(pos, anonfuncName);

        BVarSymbol keyValSymbol = new BVarSymbol(0, names.fromString("$lambdaArg$0"), this.env.scope.owner.pkgID,
                                                 getStringAnyTupleType(), this.env.scope.owner);
        BLangBlockFunctionBody functionBlock = createAnonymousFunctionBlock(pos, function, keyValSymbol);

        BLangIndexBasedAccess indexBasesAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(pos,
                                                                                               symTable.anyType,
                                                                                               keyValSymbol,
                                                                                               ASTBuilderUtil
                                                                                                       .createLiteral(
                                                                                                               pos,
                                                                                                               symTable.intType,
                                                                                                               (long) 0));
        BLangSimpleVariableDef tupFirstElem = createVarDef("key", indexBasesAccessExpr.type,
                                                           indexBasesAccessExpr, pos);
        functionBlock.addStatement(tupFirstElem);

        // Create the if statements
        for (String toRemoveItem : toRemoveList) {
            createIfStmt(pos, tupFirstElem.var.symbol, functionBlock, toRemoveItem);
        }

        // Create the final return true statement
        BInvokableSymbol functionSymbol = createReturnTrueStatement(pos, function, functionBlock);

        // Create and return a lambda function
        return createLambdaFunction(function, functionSymbol);
    }

    private BLangLambdaFunction createFuncToFilterOutRestParam(BLangRecordVariable recordVariable, DiagnosticPos pos) {
        List<String> fieldNamesToRemove = recordVariable.variableList.stream()
                .map(var -> var.getKey().getValue())
                .collect(Collectors.toList());
        return createFuncToFilterOutRestParam(fieldNamesToRemove, pos);
    }

    private void createIfStmt(DiagnosticPos pos, BVarSymbol inputParamSymbol, BLangBlockFunctionBody blockStmt,
                              String key) {
        BLangSimpleVarRef firstElemRef = ASTBuilderUtil.createVariableRef(pos, inputParamSymbol);
        BLangExpression converted = addConversionExprIfRequired(firstElemRef, symTable.stringType);

        BLangIf ifStmt = ASTBuilderUtil.createIfStmt(pos, blockStmt);

        BLangBlockStmt ifBlock = ASTBuilderUtil.createBlockStmt(pos, new ArrayList<>());
        BLangReturn returnStmt = ASTBuilderUtil.createReturnStmt(pos, ifBlock);
        returnStmt.expr = ASTBuilderUtil.createLiteral(pos, symTable.booleanType, false);
        ifStmt.body = ifBlock;

        BLangGroupExpr groupExpr = new BLangGroupExpr();
        groupExpr.type = symTable.booleanType;

        BLangBinaryExpr binaryExpr = ASTBuilderUtil.createBinaryExpr(pos, converted,
                ASTBuilderUtil.createLiteral(pos, symTable.stringType, key),
                symTable.booleanType, OperatorKind.EQUAL, null);

        binaryExpr.opSymbol = (BOperatorSymbol) symResolver.resolveBinaryOperator(
                binaryExpr.opKind, binaryExpr.lhsExpr.type, binaryExpr.rhsExpr.type);

        groupExpr.expression = binaryExpr;
        ifStmt.expr = groupExpr;
    }

    BLangLambdaFunction createLambdaFunction(BLangFunction function, BInvokableSymbol functionSymbol) {
        BLangLambdaFunction lambdaFunction = (BLangLambdaFunction) TreeBuilder.createLambdaFunctionNode();
        lambdaFunction.function = function;
        lambdaFunction.type = functionSymbol.type;
        return lambdaFunction;
    }

    private BInvokableSymbol createReturnTrueStatement(DiagnosticPos pos, BLangFunction function,
                                                       BLangBlockFunctionBody functionBlock) {
        BLangReturn trueReturnStmt = ASTBuilderUtil.createReturnStmt(pos, functionBlock);
        trueReturnStmt.expr = ASTBuilderUtil.createLiteral(pos, symTable.booleanType, true);

        // Create function symbol before visiting desugar phase for the function
        BInvokableSymbol functionSymbol = Symbols.createFunctionSymbol(Flags.asMask(function.flagSet),
                                                                       new Name(function.name.value),
                                                                       env.enclPkg.packageID, function.type,
                                                                       env.enclEnv.enclVarSym, true);
        functionSymbol.retType = function.returnTypeNode.type;
        functionSymbol.params = function.requiredParams.stream()
                .map(param -> param.symbol)
                .collect(Collectors.toList());
        functionSymbol.scope = env.scope;
        functionSymbol.type = new BInvokableType(Collections.singletonList(getStringAnyTupleType()),
                                                 getRestType(functionSymbol), symTable.booleanType, null);
        function.symbol = functionSymbol;
        rewrite(function, env);
        env.enclPkg.addFunction(function);
        return functionSymbol;
    }

    private BLangBlockFunctionBody createAnonymousFunctionBlock(DiagnosticPos pos, BLangFunction function,
                                                                BVarSymbol keyValSymbol) {
        BLangSimpleVariable inputParameter = ASTBuilderUtil.createVariable(pos, null, getStringAnyTupleType(),
                                                                           null, keyValSymbol);
        function.requiredParams.add(inputParameter);
        BLangValueType booleanTypeKind = new BLangValueType();
        booleanTypeKind.typeKind = TypeKind.BOOLEAN;
        booleanTypeKind.type = symTable.booleanType;
        function.returnTypeNode = booleanTypeKind;

        BLangBlockFunctionBody functionBlock = ASTBuilderUtil.createBlockFunctionBody(pos, new ArrayList<>());
        function.funcBody = functionBlock;
        return functionBlock;
    }

    private BTupleType getStringAnyTupleType() {
        ArrayList<BType> typeList = new ArrayList<BType>() {{
            add(symTable.stringType);
            add(symTable.anyType);
        }};
        return new BTupleType(typeList);
    }

    /**
     * This method creates a simple variable def and assigns and array expression based on the given indexExpr.
     *
     *  case 1: when there is no parent array access expression, but with the indexExpr : 1
     *  string s = x[1];
     *
     *  case 2: when there is a parent array expression : x[2] and indexExpr : 3
     *  string s = x[2][3];
     *
     *  case 3: when there is no parent array access expression, but with the indexExpr : name
     *  string s = x[name];
     *
     *  case 4: when there is a parent map expression : x[name] and indexExpr : fName
     *  string s = x[name][fName]; // record variable inside record variable
     *
     *  case 5: when there is a parent map expression : x[name] and indexExpr : 1
     *  string s = x[name][1]; // tuple variable inside record variable
     */
    private void createSimpleVarDefStmt(BLangSimpleVariable simpleVariable, BLangBlockStmt parentBlockStmt,
                                        BLangLiteral indexExpr, BVarSymbol tupleVarSymbol,
                                        BLangIndexBasedAccess parentArrayAccessExpr) {

        Name varName = names.fromIdNode(simpleVariable.name);
        if (varName == Names.IGNORE) {
            return;
        }

        final BLangSimpleVariableDef simpleVariableDef = ASTBuilderUtil.createVariableDefStmt(simpleVariable.pos,
                parentBlockStmt);
        simpleVariableDef.var = simpleVariable;

        simpleVariable.expr = createIndexBasedAccessExpr(simpleVariable.type, simpleVariable.pos,
                indexExpr, tupleVarSymbol, parentArrayAccessExpr);
    }

    @Override
    public void visit(BLangAssignment assignNode) {
        if (safeNavigateLHS(assignNode.varRef)) {
            BLangAccessExpression accessExpr = (BLangAccessExpression) assignNode.varRef;
            accessExpr.leafNode = true;
            result = rewriteSafeNavigationAssignment(accessExpr, assignNode.expr, assignNode.safeAssignment);
            result = rewrite(result, env);
            return;
        }

        assignNode.varRef = rewriteExpr(assignNode.varRef);
        assignNode.expr = rewriteExpr(assignNode.expr);
        assignNode.expr = addConversionExprIfRequired(rewriteExpr(assignNode.expr), assignNode.varRef.type);
        result = assignNode;
    }

    @Override
    public void visit(BLangTupleDestructure tupleDestructure) {
        //  case 1:
        //  a is string, b is float
        //  (a, b) = (tuple)
        //
        //  any[] x = (tuple);
        //  string a = x[0];
        //  int b = x[1];
        //
        //  case 2:
        //  a is string, b is float, c is int
        //  ((a, b), c)) = (tuple)
        //
        //  any[] x = (tuple);
        //  string a = x[0][0];
        //  float b = x[0][1];
        //  int c = x[1];


        //create tuple destruct block stmt
        final BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(tupleDestructure.pos);

        //create a array of any-type based on the dimension
        BType runTimeType = new BArrayType(symTable.anyType);

        //create a simple var for the array 'any[] x = (tuple)' based on the dimension for x
        String name = "tuple";
        final BLangSimpleVariable tuple = ASTBuilderUtil.createVariable(tupleDestructure.pos, name, runTimeType, null,
                new BVarSymbol(0, names.fromString(name), this.env.scope.owner.pkgID, runTimeType,
                        this.env.scope.owner));
        tuple.expr = tupleDestructure.expr;
        final BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDefStmt(tupleDestructure.pos,
                blockStmt);
        variableDef.var = tuple;

        //create the variable definition statements using the root block stmt created
        createVarRefAssignmentStmts(tupleDestructure.varRef, blockStmt, tuple.symbol, null);
        createRestFieldAssignmentStmt(tupleDestructure, blockStmt, tuple.symbol);

        //finally rewrite the populated block statement
        result = rewrite(blockStmt, env);
    }

    private void createRestFieldAssignmentStmt(BLangTupleDestructure tupleDestructure, BLangBlockStmt blockStmt,
                                               BVarSymbol tupleVarSymbol) {
        BLangTupleVarRef tupleVarRef = tupleDestructure.varRef;
        DiagnosticPos pos = blockStmt.pos;
        if (tupleVarRef.restParam != null) {
            BLangExpression tupleExpr = tupleDestructure.expr;

            // T[] t = [];
            BLangSimpleVarRef restParam = (BLangSimpleVarRef) tupleVarRef.restParam;
            BArrayType restParamType = (BArrayType) restParam.type;
            BLangArrayLiteral arrayExpr = createArrayLiteralExprNode();
            arrayExpr.type = restParamType;

            BLangAssignment restParamAssignment = ASTBuilderUtil.createAssignmentStmt(pos, blockStmt);
            restParamAssignment.varRef = restParam;
            restParamAssignment.varRef.type = restParamType;
            restParamAssignment.expr = arrayExpr;

            // foreach var $foreach$i in tupleTypes.length()...tupleLiteral.length() {
            //     t[t.length()] = <T> tupleLiteral[$foreach$i];
            // }
            BLangLiteral startIndexLiteral = (BLangLiteral) TreeBuilder.createLiteralExpression();
            startIndexLiteral.value = (long) tupleVarRef.expressions.size();
            startIndexLiteral.type = symTable.intType;
            BLangInvocation lengthInvocation = createLengthInvocation(pos, tupleExpr);
            BLangInvocation intRangeInvocation = replaceWithIntRange(pos, startIndexLiteral,
                    getModifiedIntRangeEndExpr(lengthInvocation));

            BLangForeach foreach = (BLangForeach) TreeBuilder.createForeachNode();
            foreach.pos = pos;
            foreach.collection = intRangeInvocation;
            types.setForeachTypedBindingPatternType(foreach);

            final BLangSimpleVariable foreachVariable = ASTBuilderUtil.createVariable(pos,
                    "$foreach$i", foreach.varType);
            foreachVariable.symbol = new BVarSymbol(0, names.fromIdNode(foreachVariable.name),
                    this.env.scope.owner.pkgID, foreachVariable.type, this.env.scope.owner);
            BLangSimpleVarRef foreachVarRef = ASTBuilderUtil.createVariableRef(pos, foreachVariable.symbol);
            foreach.variableDefinitionNode = ASTBuilderUtil.createVariableDef(pos, foreachVariable);
            foreach.isDeclaredWithVar = true;
            BLangBlockStmt foreachBody = ASTBuilderUtil.createBlockStmt(pos);

            // t[t.length()] = <T> tupleLiteral[$foreach$i];
            BLangIndexBasedAccess indexAccessExpr = ASTBuilderUtil.createIndexAccessExpr(restParam,
                    createLengthInvocation(pos, restParam));
            indexAccessExpr.type = restParamType.eType;
            createSimpleVarRefAssignmentStmt(indexAccessExpr, foreachBody, foreachVarRef, tupleVarSymbol, null);

            foreach.body = foreachBody;
            blockStmt.addStatement(foreach);
        }
    }

    private BLangInvocation createLengthInvocation(DiagnosticPos pos, BLangExpression collection) {
        BInvokableSymbol lengthInvokableSymbol = (BInvokableSymbol) symResolver
                .lookupLangLibMethod(collection.type, names.fromString(LENGTH_FUNCTION_NAME));
        BLangInvocation lengthInvocation = ASTBuilderUtil.createInvocationExprForMethod(pos, lengthInvokableSymbol,
                Lists.of(collection), symResolver);
        lengthInvocation.argExprs = lengthInvocation.requiredArgs;
        lengthInvocation.type = lengthInvokableSymbol.type.getReturnType();
        return lengthInvocation;
    }

    /**
     * This method iterate through each member of the tupleVarRef and create the relevant var ref assignment statements.
     * This method does the check for node kind of each member and call the related var ref creation method.
     *
     * Example:
     * ((a, b), c)) = (tuple)
     *
     * (a, b) is again a tuple, so it is a recursive var ref creation.
     *
     * c is a simple var, so a simple var def will be created.
     *
     */
    private void createVarRefAssignmentStmts(BLangTupleVarRef parentTupleVariable, BLangBlockStmt parentBlockStmt,
                                             BVarSymbol tupleVarSymbol, BLangIndexBasedAccess parentIndexAccessExpr) {

        final List<BLangExpression> expressions = parentTupleVariable.expressions;
        for (int index = 0; index < expressions.size(); index++) {
            BLangExpression expression = expressions.get(index);
            if (NodeKind.SIMPLE_VARIABLE_REF == expression.getKind() ||
                    NodeKind.FIELD_BASED_ACCESS_EXPR == expression.getKind() ||
                    NodeKind.INDEX_BASED_ACCESS_EXPR == expression.getKind() ||
                    NodeKind.XML_ATTRIBUTE_ACCESS_EXPR == expression.getKind()) {
                //if this is simple var, then create a simple var def stmt
                BLangLiteral indexExpr = ASTBuilderUtil.createLiteral(expression.pos, symTable.intType, (long) index);
                createSimpleVarRefAssignmentStmt((BLangVariableReference) expression, parentBlockStmt, indexExpr,
                        tupleVarSymbol, parentIndexAccessExpr);
                continue;
            }

            if (expression.getKind() == NodeKind.TUPLE_VARIABLE_REF) {
                //else recursively create the var def statements for tuple var ref
                BLangTupleVarRef tupleVarRef = (BLangTupleVarRef) expression;
                BLangLiteral indexExpr = ASTBuilderUtil.createLiteral(tupleVarRef.pos, symTable.intType, (long) index);
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(tupleVarRef.pos,
                        new BArrayType(symTable.anyType), tupleVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarRefAssignmentStmts((BLangTupleVarRef) expression, parentBlockStmt, tupleVarSymbol,
                        arrayAccessExpr);
                continue;
            }

            if (expression.getKind() == NodeKind.RECORD_VARIABLE_REF) {
                //else recursively create the var def statements for record var ref
                BLangRecordVarRef recordVarRef = (BLangRecordVarRef) expression;
                BLangLiteral indexExpr = ASTBuilderUtil.createLiteral(recordVarRef.pos, symTable.intType,
                                                                      (long) index);
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(
                        parentTupleVariable.pos, symTable.mapType, tupleVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarRefAssignmentStmts((BLangRecordVarRef) expression, parentBlockStmt, tupleVarSymbol,
                        arrayAccessExpr);

                createTypeDefinition(recordVarRef.type, recordVarRef.type.tsymbol,
                        createRecordTypeNode((BRecordType) recordVarRef.type));

                continue;
            }

            if (expression.getKind() == NodeKind.ERROR_VARIABLE_REF) {
                // Else recursively create the var def statements for record var ref.
                BLangErrorVarRef errorVarRef = (BLangErrorVarRef) expression;
                BLangLiteral indexExpr = ASTBuilderUtil.createLiteral(errorVarRef.pos, symTable.intType,
                        (long) index);
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(
                        parentTupleVariable.pos, expression.type, tupleVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarRefAssignmentStmts((BLangErrorVarRef) expression, parentBlockStmt, tupleVarSymbol,
                        arrayAccessExpr);
            }
        }
    }

    /**
     * This method creates a assignment statement and assigns and array expression based on the given indexExpr.
     *
     */
    private void createSimpleVarRefAssignmentStmt(BLangVariableReference simpleVarRef, BLangBlockStmt parentBlockStmt,
                                                  BLangExpression indexExpr, BVarSymbol tupleVarSymbol,
                                                  BLangIndexBasedAccess parentArrayAccessExpr) {

        if (simpleVarRef.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
            Name varName = names.fromIdNode(((BLangSimpleVarRef) simpleVarRef).variableName);
            if (varName == Names.IGNORE) {
                return;
            }
        }

        BLangExpression assignmentExpr = createIndexBasedAccessExpr(simpleVarRef.type, simpleVarRef.pos,
                indexExpr, tupleVarSymbol, parentArrayAccessExpr);

        assignmentExpr = addConversionExprIfRequired(assignmentExpr, simpleVarRef.type);

        final BLangAssignment assignmentStmt = ASTBuilderUtil.createAssignmentStmt(parentBlockStmt.pos,
                parentBlockStmt);
        assignmentStmt.varRef = simpleVarRef;
        assignmentStmt.expr = assignmentExpr;
    }

    private BLangExpression createIndexBasedAccessExpr(BType varType, DiagnosticPos varPos, BLangExpression indexExpr,
                                                       BVarSymbol tupleVarSymbol, BLangIndexBasedAccess parentExpr) {

        BLangIndexBasedAccess arrayAccess = ASTBuilderUtil.createIndexBasesAccessExpr(varPos,
                symTable.anyType, tupleVarSymbol, indexExpr);
        arrayAccess.originalType = varType;

        if (parentExpr != null) {
            arrayAccess.expr = parentExpr;
        }

        final BLangExpression assignmentExpr;
        if (types.isValueType(varType)) {
            BLangTypeConversionExpr castExpr = (BLangTypeConversionExpr) TreeBuilder.createTypeConversionNode();
            castExpr.expr = arrayAccess;
            castExpr.conversionSymbol = Symbols.createUnboxValueTypeOpSymbol(symTable.anyType, varType);
            castExpr.type = varType;
            assignmentExpr = castExpr;
        } else {
            assignmentExpr = arrayAccess;
        }
        return assignmentExpr;
    }

    private BLangRecordTypeNode createRecordTypeNode(BRecordType recordType) {
        List<BLangSimpleVariable> fieldList = new ArrayList<>();
        for (BField field : recordType.fields) {
            BVarSymbol symbol = field.symbol;
            if (symbol == null) {
                symbol = new BVarSymbol(Flags.PUBLIC, field.name,
                        this.env.enclPkg.packageID, symTable.pureType, null);
            }

            BLangSimpleVariable fieldVar = ASTBuilderUtil.createVariable(
                    field.pos, symbol.name.value, field.type, null, symbol);
            fieldList.add(fieldVar);
        }
        return createRecordTypeNode(fieldList, recordType);
    }

    @Override
    public void visit(BLangRecordDestructure recordDestructure) {

        final BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(recordDestructure.pos);

        BType runTimeType = new BMapType(TypeTags.MAP, symTable.anyType, null);

        String name = "$map$0";
        final BLangSimpleVariable mapVariable = ASTBuilderUtil.createVariable(recordDestructure.pos, name, runTimeType,
                null, new BVarSymbol(0, names.fromString(name), this.env.scope.owner.pkgID,
                        runTimeType, this.env.scope.owner));
        mapVariable.expr = recordDestructure.expr;
        final BLangSimpleVariableDef variableDef = ASTBuilderUtil.
                createVariableDefStmt(recordDestructure.pos, blockStmt);
        variableDef.var = mapVariable;

        //create the variable definition statements using the root block stmt created
        createVarRefAssignmentStmts(recordDestructure.varRef, blockStmt, mapVariable.symbol, null);

        //finally rewrite the populated block statement
        result = rewrite(blockStmt, env);
    }

    @Override
    public void visit(BLangErrorDestructure errorDestructure) {
        final BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(errorDestructure.pos);
        String name = "$error$";
        final BLangSimpleVariable errorVar = ASTBuilderUtil.createVariable(errorDestructure.pos, name,
                symTable.errorType, null, new BVarSymbol(0, names.fromString(name),
                        this.env.scope.owner.pkgID, symTable.errorType, this.env.scope.owner));
        errorVar.expr = errorDestructure.expr;
        final BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDefStmt(errorDestructure.pos,
                blockStmt);
        variableDef.var = errorVar;
        createVarRefAssignmentStmts(errorDestructure.varRef, blockStmt, errorVar.symbol, null);
        result = rewrite(blockStmt, env);
    }

    private void createVarRefAssignmentStmts(BLangRecordVarRef parentRecordVarRef, BLangBlockStmt parentBlockStmt,
                                             BVarSymbol recordVarSymbol, BLangIndexBasedAccess parentIndexAccessExpr) {
        final List<BLangRecordVarRefKeyValue> variableRefList = parentRecordVarRef.recordRefFields;
        for (BLangRecordVarRefKeyValue varRefKeyValue : variableRefList) {
            BLangExpression variableReference = varRefKeyValue.variableReference;
            BLangLiteral indexExpr = ASTBuilderUtil.createLiteral(variableReference.pos, symTable.stringType,
                    varRefKeyValue.variableName.getValue());

            if (NodeKind.SIMPLE_VARIABLE_REF == variableReference.getKind() ||
                    NodeKind.FIELD_BASED_ACCESS_EXPR == variableReference.getKind() ||
                    NodeKind.INDEX_BASED_ACCESS_EXPR == variableReference.getKind() ||
                    NodeKind.XML_ATTRIBUTE_ACCESS_EXPR == variableReference.getKind()) {
                createSimpleVarRefAssignmentStmt((BLangVariableReference) variableReference, parentBlockStmt,
                        indexExpr, recordVarSymbol, parentIndexAccessExpr);
                continue;
            }

            if (NodeKind.RECORD_VARIABLE_REF == variableReference.getKind()) {
                BLangRecordVarRef recordVariable = (BLangRecordVarRef) variableReference;
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(
                        parentRecordVarRef.pos, symTable.mapType, recordVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarRefAssignmentStmts(recordVariable, parentBlockStmt, recordVarSymbol, arrayAccessExpr);
                continue;
            }

            if (NodeKind.TUPLE_VARIABLE_REF == variableReference.getKind()) {
                BLangTupleVarRef tupleVariable = (BLangTupleVarRef) variableReference;
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(tupleVariable.pos,
                        symTable.tupleType, recordVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarRefAssignmentStmts(tupleVariable, parentBlockStmt, recordVarSymbol, arrayAccessExpr);
                continue;
            }

            if (NodeKind.ERROR_VARIABLE_REF == variableReference.getKind()) {
                BLangIndexBasedAccess arrayAccessExpr = ASTBuilderUtil.createIndexBasesAccessExpr(variableReference.pos,
                        symTable.errorType, recordVarSymbol, indexExpr);
                if (parentIndexAccessExpr != null) {
                    arrayAccessExpr.expr = parentIndexAccessExpr;
                }
                createVarRefAssignmentStmts((BLangErrorVarRef) variableReference, parentBlockStmt, recordVarSymbol,
                        arrayAccessExpr);
            }
        }

        if (parentRecordVarRef.restParam != null) {
            // The restParam is desugared to a filter iterable operation that filters out the fields provided in the
            // record variable
            // map<any> restParam = $map$0.filter($lambdaArg$0);

            DiagnosticPos pos = parentBlockStmt.pos;
            BMapType restParamType = (BMapType) ((BLangSimpleVarRef) parentRecordVarRef.restParam).type;
            BLangSimpleVarRef variableReference;

            if (parentIndexAccessExpr != null) {
                BLangSimpleVariable mapVariable = ASTBuilderUtil.createVariable(pos, "$map$1", restParamType,
                        null, new BVarSymbol(0, names.fromString("$map$1"), this.env.scope.owner.pkgID,
                                restParamType, this.env.scope.owner));
                mapVariable.expr = parentIndexAccessExpr;
                BLangSimpleVariableDef variableDef = ASTBuilderUtil.createVariableDefStmt(pos, parentBlockStmt);
                variableDef.var = mapVariable;

                variableReference = ASTBuilderUtil.createVariableRef(pos, mapVariable.symbol);
            } else {
                variableReference = ASTBuilderUtil.createVariableRef(pos,
                        ((BLangSimpleVariableDef) parentBlockStmt.stmts.get(0)).var.symbol);
            }

            BLangSimpleVarRef restParam = (BLangSimpleVarRef) parentRecordVarRef.restParam;

            List<String> keysToRemove = parentRecordVarRef.recordRefFields.stream()
                    .map(field -> field.variableName.value)
                    .collect(Collectors.toList());

            BLangSimpleVariable filteredDetail = generateRestFilter(variableReference, pos,
                    keysToRemove, restParamType, parentBlockStmt);

            BLangSimpleVarRef varRef = ASTBuilderUtil.createVariableRef(pos, filteredDetail.symbol);

            // Create rest param variable definition
            BLangAssignment restParamAssignment = ASTBuilderUtil.createAssignmentStmt(pos, parentBlockStmt);
            restParamAssignment.varRef = restParam;
            restParamAssignment.varRef.type = restParamType;
            restParamAssignment.expr = varRef;
        }
    }

    private void createVarRefAssignmentStmts(BLangErrorVarRef parentErrorVarRef, BLangBlockStmt parentBlockStmt,
                                             BVarSymbol errorVarySymbol, BLangIndexBasedAccess parentIndexAccessExpr) {
        if (parentErrorVarRef.reason.getKind() != NodeKind.SIMPLE_VARIABLE_REF ||
                names.fromIdNode(((BLangSimpleVarRef) parentErrorVarRef.reason).variableName) != Names.IGNORE) {
            BLangAssignment reasonAssignment = ASTBuilderUtil
                    .createAssignmentStmt(parentBlockStmt.pos, parentBlockStmt);
            reasonAssignment.expr = generateErrorReasonBuiltinFunction(parentErrorVarRef.reason.pos,
                    symTable.stringType, errorVarySymbol, parentIndexAccessExpr);
            reasonAssignment.expr = addConversionExprIfRequired(reasonAssignment.expr, parentErrorVarRef.reason.type);
            reasonAssignment.varRef = parentErrorVarRef.reason;
        }

        // When no detail nor rest detail are to be destructured, we don't need to generate the detail invocation.
        if (parentErrorVarRef.detail.isEmpty() && isIgnoredErrorRefRestVar(parentErrorVarRef)) {
            return;
        }

        BLangInvocation errorDetailBuiltinFunction = generateErrorDetailBuiltinFunction(parentErrorVarRef.pos,
                errorVarySymbol,
                parentIndexAccessExpr);

        BLangSimpleVariableDef detailTempVarDef = createVarDef("$error$detail$" + errorCount++,
                                                               symTable.detailType, errorDetailBuiltinFunction,
                                                               parentErrorVarRef.pos);
        detailTempVarDef.type = symTable.detailType;
        parentBlockStmt.addStatement(detailTempVarDef);
        this.env.scope.define(names.fromIdNode(detailTempVarDef.var.name), detailTempVarDef.var.symbol);

        List<String> extractedKeys = new ArrayList<>();
        for (BLangNamedArgsExpression detail : parentErrorVarRef.detail) {
            extractedKeys.add(detail.name.value);
            BLangVariableReference ref = (BLangVariableReference) detail.expr;

            // create a index based access
            BLangExpression detailEntryVar = createIndexBasedAccessExpr(ref.type, ref.pos,
                    createStringLiteral(detail.name.pos, detail.name.value),
                    detailTempVarDef.var.symbol, null);
            if (detailEntryVar.getKind() == NodeKind.INDEX_BASED_ACCESS_EXPR) {
                BLangIndexBasedAccess bLangIndexBasedAccess = (BLangIndexBasedAccess) detailEntryVar;
                bLangIndexBasedAccess.originalType = symTable.pureType;
            }

            BLangAssignment detailAssignment = ASTBuilderUtil.createAssignmentStmt(ref.pos, parentBlockStmt);
            detailAssignment.varRef = ref;
            detailAssignment.expr = detailEntryVar;
        }

        if (!isIgnoredErrorRefRestVar(parentErrorVarRef)) {
            BLangSimpleVarRef detailVarRef = ASTBuilderUtil.createVariableRef(parentErrorVarRef.restVar.pos,
                    detailTempVarDef.var.symbol);

            BLangSimpleVariable filteredDetail = generateRestFilter(detailVarRef, parentErrorVarRef.restVar.pos,
                    extractedKeys,
                    parentErrorVarRef.restVar.type, parentBlockStmt);
            BLangAssignment restAssignment = ASTBuilderUtil.createAssignmentStmt(parentErrorVarRef.restVar.pos,
                    parentBlockStmt);
            restAssignment.varRef = parentErrorVarRef.restVar;
            restAssignment.expr = ASTBuilderUtil.createVariableRef(parentErrorVarRef.restVar.pos,
                    filteredDetail.symbol);
        }

        BErrorType errorType = (BErrorType) parentErrorVarRef.type;
        if (errorType.detailType.getKind() == TypeKind.RECORD) {
            // Create empty record init attached func
            BRecordTypeSymbol tsymbol = (BRecordTypeSymbol) errorType.detailType.tsymbol;
            tsymbol.initializerFunc = createRecordInitFunc();
            tsymbol.scope.define(tsymbol.initializerFunc.funcName, tsymbol.initializerFunc.symbol);
        }
    }

    private boolean isIgnoredErrorRefRestVar(BLangErrorVarRef parentErrorVarRef) {
        if (parentErrorVarRef.restVar == null) {
            return true;
        }
        if (parentErrorVarRef.restVar.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
            return (((BLangSimpleVarRef) parentErrorVarRef.restVar).variableName.value.equals(IGNORE.value));
        }
        return false;
    }

    @Override
    public void visit(BLangAbort abortNode) {
        BLangReturn returnStmt = ASTBuilderUtil.createReturnStmt(abortNode.pos, symTable.intType, -1L);
        result = rewrite(returnStmt, env);
    }

    @Override
    public void visit(BLangRetry retryNode) {
        BLangReturn returnStmt = ASTBuilderUtil.createReturnStmt(retryNode.pos, symTable.intType, 1L);
        result = rewrite(returnStmt, env);
    }

    @Override
    public void visit(BLangContinue nextNode) {
        result = nextNode;
    }

    @Override
    public void visit(BLangBreak breakNode) {
        result = breakNode;
    }

    @Override
    public void visit(BLangReturn returnNode) {
        // If the return node do not have an expression, we add `done` statement instead of a return statement. This is
        // to distinguish between returning nil value specifically and not returning any value.
        if (returnNode.expr != null) {
            returnNode.expr = rewriteExpr(returnNode.expr);
        }
        result = returnNode;
    }

    @Override
    public void visit(BLangPanic panicNode) {
        panicNode.expr = rewriteExpr(panicNode.expr);
        result = panicNode;
    }

    @Override
    public void visit(BLangXMLNSStatement xmlnsStmtNode) {
        xmlnsStmtNode.xmlnsDecl = rewrite(xmlnsStmtNode.xmlnsDecl, env);
        result = xmlnsStmtNode;
    }

    @Override
    public void visit(BLangXMLNS xmlnsNode) {
        BLangXMLNS generatedXMLNSNode;
        xmlnsNode.namespaceURI = rewriteExpr(xmlnsNode.namespaceURI);
        BSymbol ownerSymbol = xmlnsNode.symbol.owner;

        // Local namespace declaration in a function/resource/action/worker
        if ((ownerSymbol.tag & SymTag.INVOKABLE) == SymTag.INVOKABLE ||
                (ownerSymbol.tag & SymTag.SERVICE) == SymTag.SERVICE) {
            generatedXMLNSNode = new BLangLocalXMLNS();
        } else {
            generatedXMLNSNode = new BLangPackageXMLNS();
        }

        generatedXMLNSNode.namespaceURI = xmlnsNode.namespaceURI;
        generatedXMLNSNode.prefix = xmlnsNode.prefix;
        generatedXMLNSNode.symbol = xmlnsNode.symbol;
        result = generatedXMLNSNode;
    }

    public void visit(BLangCompoundAssignment compoundAssignment) {

        BLangVariableReference varRef = compoundAssignment.varRef;
        if (compoundAssignment.varRef.getKind() != NodeKind.INDEX_BASED_ACCESS_EXPR) {
            // Create a new varRef if this is a simpleVarRef. Because this can be a
            // narrowed type var. In that case, lhs and rhs must be visited in two
            // different manners.
            if (varRef.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
                varRef = ASTBuilderUtil.createVariableRef(compoundAssignment.varRef.pos, varRef.symbol);
                varRef.lhsVar = true;
            }

            result = ASTBuilderUtil.createAssignmentStmt(compoundAssignment.pos, rewriteExpr(varRef),
                    rewriteExpr(compoundAssignment.modifiedExpr));
            return;
        }
        // If compound Assignment is an index based expression such as a[f(1, foo)][3][2] += y,
        // should return a block statement which is equivalent to
        // var $temp3$ = a[f(1, foo)];
        // var $temp2$ = 3;
        // var $temp1$ = 2;
        // a[$temp3$][$temp2$][$temp1$] = a[$temp3$][$temp2$][$temp1$] + y;
        List<BLangStatement> statements = new ArrayList<>();
        List<BLangSimpleVarRef> varRefs = new ArrayList<>();
        List<BType> types = new ArrayList<>();

        // Extract the index Expressions from compound assignment and create variable definitions. ex:
        // var $temp3$ = a[f(1, foo)];
        // var $temp2$ = 3;
        // var $temp1$ = 2;
        do {
            BLangSimpleVariableDef tempIndexVarDef = createVarDef("$temp" + ++indexExprCount + "$",
                    ((BLangIndexBasedAccess) varRef).indexExpr.type, ((BLangIndexBasedAccess) varRef).indexExpr,
                    compoundAssignment.pos);
            BLangSimpleVarRef tempVarRef = ASTBuilderUtil.createVariableRef(tempIndexVarDef.pos,
                    tempIndexVarDef.var.symbol);
            statements.add(0, tempIndexVarDef);
            varRefs.add(0, tempVarRef);
            types.add(0, varRef.type);

            varRef = (BLangVariableReference) ((BLangIndexBasedAccess) varRef).expr;
        } while (varRef.getKind() == NodeKind.INDEX_BASED_ACCESS_EXPR);

        // Create the index access expression. ex: c[$temp3$][$temp2$][$temp1$]
        BLangVariableReference var = varRef;
        for (int ref = 0; ref < varRefs.size(); ref++) {
            var = ASTBuilderUtil.createIndexAccessExpr(var, varRefs.get(ref));
            var.type = types.get(ref);
        }
        var.type = compoundAssignment.varRef.type;

        // Create the right hand side binary expression of the assignment. ex: c[$temp3$][$temp2$][$temp1$] + y
        BLangExpression rhsExpression = ASTBuilderUtil.createBinaryExpr(compoundAssignment.pos, var,
                compoundAssignment.expr, compoundAssignment.type, compoundAssignment.opKind, null);
        rhsExpression.type = compoundAssignment.modifiedExpr.type;

        // Create assignment statement. ex: a[$temp3$][$temp2$][$temp1$] = a[$temp3$][$temp2$][$temp1$] + y;
        BLangAssignment assignStmt = ASTBuilderUtil.createAssignmentStmt(compoundAssignment.pos, var,
                rhsExpression);

        statements.add(assignStmt);
        // Create block statement. ex: var $temp3$ = a[f(1, foo)];var $temp2$ = 3;var $temp1$ = 2;
        // a[$temp3$][$temp2$][$temp1$] = a[$temp3$][$temp2$][$temp1$] + y;
        BLangBlockStmt bLangBlockStmt = ASTBuilderUtil.createBlockStmt(compoundAssignment.pos, statements);
        result = rewrite(bLangBlockStmt, env);
    }

    @Override
    public void visit(BLangExpressionStmt exprStmtNode) {
        exprStmtNode.expr = rewriteExpr(exprStmtNode.expr);
        result = exprStmtNode;
    }

    @Override
    public void visit(BLangIf ifNode) {
        ifNode.expr = rewriteExpr(ifNode.expr);
        ifNode.body = rewrite(ifNode.body, env);
        ifNode.elseStmt = rewrite(ifNode.elseStmt, env);

        result = ifNode;
    }

    @Override
    public void visit(BLangMatch matchStmt) {
        // Here we generate an if-else statement for the match statement
        // Here is an example match statement
        //
        //  case 1 (old match)
        //
        //      match expr {
        //          int k => io:println("int value: " + k);
        //          string s => io:println("string value: " + s);
        //          json j => io:println("json value: " + s);
        //
        //      }
        //
        //  Here is how we convert the match statement to an if-else statement. The last clause should always be the
        //  else clause
        //
        //  string | int | json | any _$$_matchexpr = expr;
        //  if ( _$$_matchexpr isassignable int ){
        //      int k = (int) _$$_matchexpr; // unbox
        //      io:println("int value: " + k);
        //
        //  } else if (_$$_matchexpr isassignable string ) {
        //      string s = (string) _$$_matchexpr; // unbox
        //      io:println("string value: " + s);
        //
        //  } else if ( _$$_matchexpr isassignable float ||    // should we consider json[] as well
        //                  _$$_matchexpr isassignable boolean ||
        //                  _$$_matchexpr isassignable json) {
        //
        //  } else {
        //      // handle the last pattern
        //      any case..
        //  }
        //
        //  case 2 (new match)
        //      match expr {
        //          12 => io:println("Matched Int Value 12");
        //          35 => io:println("Matched Int Value 35");
        //          true => io:println("Matched Boolean Value true");
        //          "Hello" => io:println("Matched String Value Hello");
        //      }
        //
        //  This will be desugared as below :
        //
        //  string | int | boolean _$$_matchexpr = expr;
        //  if ((<int>_$$_matchexpr) == 12){
        //      io:println("Matched Int Value 12");
        //
        //  } else if ((<int>_$$_matchexpr) == 35) {
        //      io:println("Matched Int Value 35");
        //
        //  } else if ((<boolean>_$$_matchexpr) == true) {
        //      io:println("Matched Boolean Value true");
        //
        //  } else if ((<string>_$$_matchexpr) == "Hello") {
        //      io:println("Matched String Value Hello");
        //
        //  }

        // First create a block statement to hold generated statements
        BLangBlockStmt matchBlockStmt = (BLangBlockStmt) TreeBuilder.createBlockNode();
        matchBlockStmt.pos = matchStmt.pos;

        // Create a variable definition to store the value of the match expression
        String matchExprVarName = GEN_VAR_PREFIX.value;
        BLangSimpleVariable matchExprVar = ASTBuilderUtil.createVariable(matchStmt.expr.pos,
                matchExprVarName, matchStmt.expr.type, matchStmt.expr, new BVarSymbol(0,
                        names.fromString(matchExprVarName),
                        this.env.scope.owner.pkgID, matchStmt.expr.type, this.env.scope.owner));

        // Now create a variable definition node
        BLangSimpleVariableDef matchExprVarDef = ASTBuilderUtil.createVariableDef(matchBlockStmt.pos, matchExprVar);

        // Add the var def statement to the block statement
        //      string | int _$$_matchexpr = expr;
        matchBlockStmt.stmts.add(matchExprVarDef);

        // Create if/else blocks with typeof binary expressions for each pattern
        matchBlockStmt.stmts.add(generateIfElseStmt(matchStmt, matchExprVar));

        rewrite(matchBlockStmt, this.env);
        result = matchBlockStmt;
    }

    @Override
    public void visit(BLangForeach foreach) {
        BLangBlockStmt blockNode;

        // We need to create a new variable for the expression as well. This is needed because integer ranges can be
        // added as the expression so we cannot get the symbol in such cases.
        BVarSymbol dataSymbol = new BVarSymbol(0, names.fromString("$data$"), this.env.scope.owner.pkgID,
                foreach.collection.type, this.env.scope.owner);
        BLangSimpleVariable dataVariable = ASTBuilderUtil.createVariable(foreach.pos, "$data$",
                foreach.collection.type, foreach.collection, dataSymbol);
        BLangSimpleVariableDef dataVarDef = ASTBuilderUtil.createVariableDef(foreach.pos, dataVariable);

        // Get the symbol of the variable (collection).
        BVarSymbol collectionSymbol = dataVariable.symbol;
        switch (foreach.collection.type.tag) {
            case TypeTags.STRING:
            case TypeTags.ARRAY:
            case TypeTags.TUPLE:
            case TypeTags.XML:
            case TypeTags.MAP:
            case TypeTags.TABLE:
            case TypeTags.RECORD:
                BInvokableSymbol iteratorSymbol = getLangLibIteratorInvokableSymbol(collectionSymbol);
                blockNode = desugarForeachWithIteratorDef(foreach, dataVarDef, collectionSymbol, iteratorSymbol, true);
                break;
            case TypeTags.OBJECT: //We know for sure, the object is an iterable from TypeChecker phase.
                iteratorSymbol = getIterableObjectIteratorInvokableSymbol(collectionSymbol);
                blockNode = desugarForeachWithIteratorDef(foreach, dataVarDef, collectionSymbol, iteratorSymbol, false);
                break;
            default:
                blockNode = ASTBuilderUtil.createBlockStmt(foreach.pos);
                blockNode.stmts.add(0, dataVarDef);
                break;
        }

        // Rewrite the block.
        rewrite(blockNode, this.env);
        result = blockNode;
    }

    private BLangBlockStmt desugarForeachWithIteratorDef(BLangForeach foreach,
                                                         BLangSimpleVariableDef dataVariableDefinition,
                                                         BVarSymbol collectionSymbol,
                                                         BInvokableSymbol iteratorInvokableSymbol,
                                                         boolean isIteratorFuncFromLangLib) {
        BLangSimpleVariableDef iteratorVarDef = getIteratorVariableDefinition(foreach.pos, collectionSymbol,
                iteratorInvokableSymbol, isIteratorFuncFromLangLib);
        BLangBlockStmt blockNode = desugarForeachToWhile(foreach, iteratorVarDef);
        blockNode.stmts.add(0, dataVariableDefinition);
        return blockNode;
    }

    private BInvokableSymbol getIterableObjectIteratorInvokableSymbol(BVarSymbol collectionSymbol) {
        BObjectTypeSymbol typeSymbol = (BObjectTypeSymbol) collectionSymbol.type.tsymbol;
        // We know for sure at this point, the object symbol should have the __iterator method
        BAttachedFunction iteratorFunc = null;
        for (BAttachedFunction func : typeSymbol.attachedFuncs) {
            if (func.funcName.value.equals(BLangCompilerConstants.ITERABLE_OBJECT_ITERATOR_FUNC)) {
                iteratorFunc = func;
                break;
            }
        }
        BAttachedFunction function = iteratorFunc;
        return function.symbol;
    }

    private BInvokableSymbol getLangLibIteratorInvokableSymbol(BVarSymbol collectionSymbol) {
        return (BInvokableSymbol) symResolver.lookupLangLibMethod(collectionSymbol.type,
                names.fromString(BLangCompilerConstants.ITERABLE_COLLECTION_ITERATOR_FUNC));
    }

    private BLangBlockStmt desugarForeachToWhile(BLangForeach foreach, BLangSimpleVariableDef varDef) {

        // We desugar the foreach statement to a while loop here.
        //
        // int[] data = [1, 2, 3];
        //
        // // Before desugaring.
        // foreach int i in data {
        //     io:println(i);
        // }
        //
        // ---------- After desugaring -------------
        //
        // int[] $data$ = data;
        //
        // abstract object {public function next() returns record {|int value;|}? $iterator$ = $data$.iterator();
        // record {|int value;|}? $result$ = $iterator$.next();
        //
        // while $result$ is record {|int value;|} {
        //     int i = $result$.value;
        //     $result$ = $iterator$.next();
        //     ....
        //     [foreach node body]
        //     ....
        // }

        // Note - any $iterator$ = $data$.iterator();
        // -------------------------------------------------------------------

        // Note - $data$.iterator();

        BVarSymbol iteratorSymbol = varDef.var.symbol;

        // Create a new symbol for the $result$.
        BVarSymbol resultSymbol = new BVarSymbol(0, names.fromString("$result$"), this.env.scope.owner.pkgID,
                foreach.nillableResultType, this.env.scope.owner);

        // Note - map<T>? $result$ = $iterator$.next();
        BLangSimpleVariableDef resultVariableDefinition =
                getIteratorNextVariableDefinition(foreach, iteratorSymbol, resultSymbol);

        // Note - $result$ != ()
        BLangSimpleVarRef resultReferenceInWhile = ASTBuilderUtil.createVariableRef(foreach.pos, resultSymbol);
        BLangTypeTestExpr typeTestExpr = ASTBuilderUtil
                .createTypeTestExpr(foreach.pos, resultReferenceInWhile, getUserDefineTypeNode(foreach.resultType));
        // create while loop: while ($result$ != ())
        BLangWhile whileNode = (BLangWhile) TreeBuilder.createWhileNode();
        whileNode.pos = foreach.pos;
        whileNode.expr = typeTestExpr;
        whileNode.body = foreach.body;

        // Generate $result$.value
        // However, we are within the while loop. hence the $result$ can never be nil. Therefore
        // cast $result$ to non-nilable  type.
        BLangFieldBasedAccess valueAccessExpr = getValueAccessExpression(foreach, resultSymbol);
        valueAccessExpr.expr =
                addConversionExprIfRequired(valueAccessExpr.expr, types.getSafeType(valueAccessExpr.expr.type,
                                                                                    true, false));

        VariableDefinitionNode variableDefinitionNode = foreach.variableDefinitionNode;
        variableDefinitionNode.getVariable()
                .setInitialExpression(addConversionExprIfRequired(valueAccessExpr, foreach.varType));
        whileNode.body.stmts.add(0, (BLangStatement) variableDefinitionNode);

        // Note - $result$ = $iterator$.next();
        BLangAssignment resultAssignment =
                getIteratorNextAssignment(foreach, iteratorSymbol, resultSymbol);
        whileNode.body.stmts.add(1, resultAssignment);

        // Create a new block statement node.
        BLangBlockStmt blockNode = ASTBuilderUtil.createBlockStmt(foreach.pos);

        // Add iterator variable to the block.
        blockNode.addStatement(varDef);

        // Add result variable to the block.
        blockNode.addStatement(resultVariableDefinition);

        // Add the while node to the block.
        blockNode.addStatement(whileNode);
        return blockNode;
    }

    private BLangType getUserDefineTypeNode(BType type) {
        BLangUserDefinedType recordType =
                new BLangUserDefinedType(ASTBuilderUtil.createIdentifier(null, ""),
                                         ASTBuilderUtil.createIdentifier(null, ""));
        recordType.type = type;
        return recordType;
    }

    @Override
    public void visit(BLangWhile whileNode) {
        whileNode.expr = rewriteExpr(whileNode.expr);
        whileNode.body = rewrite(whileNode.body, env);
        result = whileNode;
    }

    @Override
    public void visit(BLangLock lockNode) {
        // Lock nodes will get desugared to below code
        // before desugar -
        //
        // lock {
        //      a = a + 7;
        // }
        //
        // after desugar -
        //
        // lock a;
        // var res = trap a = a + 7;
        // unlock a;
        // if (res is error) {
        //      panic res;
        // }
        BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(lockNode.pos);

        BLangLockStmt lockStmt = new BLangLockStmt(lockNode.pos);
        blockStmt.addStatement(lockStmt);

        enclLocks.push(lockStmt);

        BLangLiteral nilLiteral = ASTBuilderUtil.createLiteral(lockNode.pos, symTable.nilType, Names.NIL_VALUE);
        BType nillableError = BUnionType.create(null, symTable.errorType, symTable.nilType);
        BLangStatementExpression statementExpression = ASTBuilderUtil
                .createStatementExpression(lockNode.body, nilLiteral);
        statementExpression.type = symTable.nilType;

        BLangTrapExpr trapExpr = (BLangTrapExpr) TreeBuilder.createTrapExpressionNode();
        trapExpr.type = nillableError;
        trapExpr.expr = statementExpression;
        BVarSymbol nillableErrorVarSymbol = new BVarSymbol(0, names.fromString("$errorResult"),
                this.env.scope.owner.pkgID, nillableError, this.env.scope.owner);
        BLangSimpleVariable simpleVariable = ASTBuilderUtil.createVariable(lockNode.pos, "$errorResult",
                nillableError, trapExpr, nillableErrorVarSymbol);
        BLangSimpleVariableDef simpleVariableDef = ASTBuilderUtil.createVariableDef(lockNode.pos, simpleVariable);
        blockStmt.addStatement(simpleVariableDef);

        BLangUnLockStmt unLockStmt = new BLangUnLockStmt(lockNode.pos);
        blockStmt.addStatement(unLockStmt);
        BLangSimpleVarRef varRef = ASTBuilderUtil.createVariableRef(lockNode.pos, nillableErrorVarSymbol);

        BLangBlockStmt ifBody = ASTBuilderUtil.createBlockStmt(lockNode.pos);
        BLangPanic panicNode = (BLangPanic) TreeBuilder.createPanicNode();
        panicNode.pos = lockNode.pos;
        panicNode.expr = addConversionExprIfRequired(varRef, symTable.errorType);
        ifBody.addStatement(panicNode);

        BLangTypeTestExpr isErrorTest =
                ASTBuilderUtil.createTypeTestExpr(lockNode.pos, varRef, getErrorTypeNode());
        isErrorTest.type = symTable.booleanType;

        BLangIf ifelse = ASTBuilderUtil.createIfElseStmt(lockNode.pos, isErrorTest, ifBody, null);
        blockStmt.addStatement(ifelse);
        result = rewrite(blockStmt, env);
        enclLocks.pop();

        //check both a field and parent are in locked variables
        if (!lockStmt.lockVariables.isEmpty()) {
            lockStmt.fieldVariables.entrySet().removeIf(entry -> lockStmt.lockVariables.contains(entry.getKey()));
        }
    }

    @Override
    public void visit(BLangLockStmt lockStmt) {
        result = lockStmt;
    }

    @Override
    public void visit(BLangUnLockStmt unLockStmt) {
        result = unLockStmt;
    }

    @Override
    public void visit(BLangTransaction transactionNode) {

        // Transaction node will be desugar to beginTransactionInitiator function  in transaction package.
        // function beginTransactionInitiator(string transactionBlockId, int rMax, function () returns int trxFunc,
        //                                    function () retryFunc, function () committedFunc,
        //                                    function () abortedFunc) {

        DiagnosticPos pos = transactionNode.pos;
        BType trxReturnType = symTable.intType;
        BType otherReturnType = symTable.nilType;
        BLangType trxReturnNode = ASTBuilderUtil.createTypeNode(trxReturnType);
        BLangType otherReturnNode = ASTBuilderUtil.createTypeNode(otherReturnType);
        DiagnosticPos invPos = transactionNode.pos;
        /* transaction block code will be desugar to function which returns int. Return value determines the status of
         the transaction code.
         ex.
            0 = successful
            1 = retry
            -1 = abort

            Since transaction block code doesn't return anything, we need to add return statement at end of the
            block unless we have abort or retry statement.
        */
        DiagnosticPos returnStmtPos = new DiagnosticPos(invPos.src,
                                                        invPos.eLine, invPos.eLine, invPos.sCol, invPos.sCol);
        BLangStatement statement = null;
        if (!transactionNode.transactionBody.stmts.isEmpty()) {
            statement = transactionNode.transactionBody.stmts.get(transactionNode.transactionBody.stmts.size() - 1);
        }
        if (statement == null || !(statement.getKind() == NodeKind.ABORT) && !(statement.getKind() == NodeKind.ABORT)) {
            BLangReturn returnStmt = ASTBuilderUtil.createReturnStmt(returnStmtPos, trxReturnType, 0L);
            transactionNode.transactionBody.addStatement(returnStmt);
        }

        // Need to add empty block statement if on abort or on retry blocks do not exsist.
        if (transactionNode.abortedBody == null) {
            transactionNode.abortedBody = ASTBuilderUtil.createBlockStmt(transactionNode.pos);
        }
        if (transactionNode.committedBody == null) {
            transactionNode.committedBody = ASTBuilderUtil.createBlockStmt(transactionNode.pos);
        }
        if (transactionNode.onRetryBody == null) {
            transactionNode.onRetryBody = ASTBuilderUtil.createBlockStmt(transactionNode.pos);
        }

        if (transactionNode.retryCount == null) {
            transactionNode.retryCount = ASTBuilderUtil.createLiteral(pos, symTable.intType, 3L);
        }

        // Desugar transaction code, on retry and on abort code to separate functions.
        BLangLambdaFunction trxMainFunc = createLambdaFunction(pos, "$anonTrxMainFunc$",
                                                               Collections.emptyList(),
                                                               trxReturnNode,
                                                               rewrite(transactionNode.transactionBody.stmts, env));
        BLangLambdaFunction trxOnRetryFunc = createLambdaFunction(pos, "$anonTrxOnRetryFunc$",
                                                                  Collections.emptyList(),
                                                                  otherReturnNode,
                                                                  rewrite(transactionNode.onRetryBody.stmts, env));
        BLangLambdaFunction trxCommittedFunc = createLambdaFunction(pos, "$anonTrxCommittedFunc$",
                                                                    Collections.emptyList(),
                                                                    otherReturnNode,
                                                                    rewrite(transactionNode.committedBody.stmts, env));
        BLangLambdaFunction trxAbortedFunc = createLambdaFunction(pos, "$anonTrxAbortedFunc$",
                                                                  Collections.emptyList(),
                                                                  otherReturnNode,
                                                                  rewrite(transactionNode.abortedBody.stmts, env));
        trxMainFunc.cachedEnv = env.createClone();
        trxOnRetryFunc.cachedEnv = env.createClone();
        trxCommittedFunc.cachedEnv = env.createClone();
        trxAbortedFunc.cachedEnv = env.createClone();

        // Retrive the symbol for beginTransactionInitiator function.
        PackageID packageID = new PackageID(Names.BALLERINA_ORG, Names.TRANSACTION_PACKAGE, Names.EMPTY);
        BPackageSymbol transactionPkgSymbol = new BPackageSymbol(packageID, null, 0);
        BInvokableSymbol invokableSymbol =
                (BInvokableSymbol) symResolver.lookupSymbol(symTable.pkgEnvMap.get(transactionPkgSymbol),
                        TRX_INITIATOR_BEGIN_FUNCTION,
                        SymTag.FUNCTION);
        BLangLiteral transactionBlockId = ASTBuilderUtil.createLiteral(pos, symTable.stringType,
                                                                       getTransactionBlockId());
        List<BLangExpression> requiredArgs = Lists.of(transactionBlockId, transactionNode.retryCount, trxMainFunc,
                                                      trxOnRetryFunc,
                                                      trxCommittedFunc, trxAbortedFunc);
        BLangInvocation trxInvocation = ASTBuilderUtil.createInvocationExprMethod(pos, invokableSymbol,
                                                                                  requiredArgs,
                                                                                  Collections.emptyList(),
                                                                                  symResolver);
        BLangExpressionStmt stmt = ASTBuilderUtil.createExpressionStmt(pos, ASTBuilderUtil.createBlockStmt(pos));
        stmt.expr = trxInvocation;
        result = rewrite(stmt, env);
    }

    private String getTransactionBlockId() {
        return env.enclPkg.packageID.orgName + "$" + env.enclPkg.packageID.name + "$"
                + transactionIndex++;
    }

    private BLangLambdaFunction createLambdaFunction(DiagnosticPos pos, String functionNamePrefix,
                                                     List<BLangSimpleVariable> lambdaFunctionVariable,
                                                     TypeNode returnType, BLangFunctionBody lambdaBody) {
        BLangLambdaFunction lambdaFunction = (BLangLambdaFunction) TreeBuilder.createLambdaFunctionNode();
        BLangFunction func = ASTBuilderUtil.createFunction(pos, functionNamePrefix + lambdaFunctionCount++);
        lambdaFunction.function = func;
        func.requiredParams.addAll(lambdaFunctionVariable);
        func.setReturnTypeNode(returnType);
        func.desugaredReturnType = true;
        defineFunction(func, env.enclPkg);
        lambdaFunctionVariable = func.requiredParams;

        func.funcBody = lambdaBody;
        func.desugared = false;
        lambdaFunction.pos = pos;
        List<BType> paramTypes = new ArrayList<>();
        lambdaFunctionVariable.forEach(variable -> paramTypes.add(variable.symbol.type));
        lambdaFunction.type = new BInvokableType(paramTypes, func.symbol.type.getReturnType(),
                                                 null);
        return lambdaFunction;
    }

    private BLangLambdaFunction createLambdaFunction(DiagnosticPos pos, String functionNamePrefix,
                                                     List<BLangSimpleVariable> lambdaFunctionVariable,
                                                     TypeNode returnType, List<BLangStatement> fnBodyStmts) {
        BLangBlockFunctionBody body = (BLangBlockFunctionBody) TreeBuilder.createBlockFunctionBodyNode();
        body.stmts = fnBodyStmts;
        return createLambdaFunction(pos, functionNamePrefix, lambdaFunctionVariable, returnType, body);
    }

    private BLangLambdaFunction createLambdaFunction(DiagnosticPos pos, String functionNamePrefix,
                                                     TypeNode returnType) {
        BLangLambdaFunction lambdaFunction = (BLangLambdaFunction) TreeBuilder.createLambdaFunctionNode();
        BLangFunction func = ASTBuilderUtil.createFunction(pos, functionNamePrefix + lambdaFunctionCount++);
        lambdaFunction.function = func;
        func.setReturnTypeNode(returnType);
        func.desugaredReturnType = true;
        defineFunction(func, env.enclPkg);
        func.desugared = false;
        lambdaFunction.pos = pos;
        return lambdaFunction;
    }

    private void defineFunction(BLangFunction funcNode, BLangPackage targetPkg) {
        final BPackageSymbol packageSymbol = targetPkg.symbol;
        final SymbolEnv packageEnv = this.symTable.pkgEnvMap.get(packageSymbol);
        symbolEnter.defineNode(funcNode, packageEnv);
        packageEnv.enclPkg.functions.add(funcNode);
        packageEnv.enclPkg.topLevelNodes.add(funcNode);
    }

    @Override
    public void visit(BLangForkJoin forkJoin) {
         result = forkJoin;
    }

    // Expressions

    @Override
    public void visit(BLangLiteral literalExpr) {
        if (literalExpr.type.tag == TypeTags.ARRAY && ((BArrayType) literalExpr.type).eType.tag == TypeTags.BYTE) {
            // this is blob literal as byte array
            result = rewriteBlobLiteral(literalExpr);
            return;
        }
        result = literalExpr;
    }

    private BLangNode rewriteBlobLiteral(BLangLiteral literalExpr) {
        String[] result = getBlobTextValue((String) literalExpr.value);
        byte[] values;
        if (BASE_64.equals(result[0])) {
            values = Base64.getDecoder().decode(result[1].getBytes(StandardCharsets.UTF_8));
        } else {
            values = hexStringToByteArray(result[1]);
        }
        BLangArrayLiteral arrayLiteralNode = (BLangArrayLiteral) TreeBuilder.createArrayLiteralExpressionNode();
        arrayLiteralNode.type = literalExpr.type;
        arrayLiteralNode.pos = literalExpr.pos;
        arrayLiteralNode.exprs = new ArrayList<>();
        for (byte b : values) {
            arrayLiteralNode.exprs.add(createByteLiteral(literalExpr.pos, b));
        }
        return arrayLiteralNode;
    }

    private String[] getBlobTextValue(String blobLiteralNodeText) {
        String nodeText = blobLiteralNodeText.replaceAll(" ", "");
        String[] result = new String[2];
        result[0] = nodeText.substring(0, nodeText.indexOf('`'));
        result[1] = nodeText.substring(nodeText.indexOf('`') + 1, nodeText.lastIndexOf('`'));
        return result;
    }

    private static byte[] hexStringToByteArray(String str) {
        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public void visit(BLangListConstructorExpr listConstructor) {
        listConstructor.exprs = rewriteExprs(listConstructor.exprs);
        BLangExpression expr;
        if (listConstructor.type.tag == TypeTags.TUPLE) {
            expr = new BLangTupleLiteral(listConstructor.pos, listConstructor.exprs, listConstructor.type);
            result = rewriteExpr(expr);
        } else if (listConstructor.type.tag == TypeTags.JSON) {
            expr = new BLangJSONArrayLiteral(listConstructor.exprs, new BArrayType(listConstructor.type));
            result = rewriteExpr(expr);
        } else if (getElementType(listConstructor.type).tag == TypeTags.JSON) {
            expr = new BLangJSONArrayLiteral(listConstructor.exprs, listConstructor.type);
            result = rewriteExpr(expr);
        } else if (listConstructor.type.tag == TypeTags.TYPEDESC) {
            final BLangTypedescExpr typedescExpr = new BLangTypedescExpr();
            typedescExpr.resolvedType = listConstructor.typedescType;
            typedescExpr.type = symTable.typeDesc;
            result = rewriteExpr(typedescExpr);
        } else {
            expr = new BLangArrayLiteral(listConstructor.pos, listConstructor.exprs, listConstructor.type);
            result = rewriteExpr(expr);
        }
    }

    @Override
    public void visit(BLangArrayLiteral arrayLiteral) {
        arrayLiteral.exprs = rewriteExprs(arrayLiteral.exprs);

        if (arrayLiteral.type.tag == TypeTags.JSON) {
            result = new BLangJSONArrayLiteral(arrayLiteral.exprs, new BArrayType(arrayLiteral.type));
            return;
        } else if (getElementType(arrayLiteral.type).tag == TypeTags.JSON) {
            result = new BLangJSONArrayLiteral(arrayLiteral.exprs, arrayLiteral.type);
            return;
        }
        result = arrayLiteral;
    }

    @Override
    public void visit(BLangTupleLiteral tupleLiteral) {
        if (tupleLiteral.isTypedescExpr) {
            final BLangTypedescExpr typedescExpr = new BLangTypedescExpr();
            typedescExpr.resolvedType = tupleLiteral.typedescType;
            typedescExpr.type = symTable.typeDesc;
            result = rewriteExpr(typedescExpr);
            return;
        }
        tupleLiteral.exprs.forEach(expr -> {
            BType expType = expr.impConversionExpr == null ? expr.type : expr.impConversionExpr.type;
            types.setImplicitCastExpr(expr, expType, symTable.anyType);
        });
        tupleLiteral.exprs = rewriteExprs(tupleLiteral.exprs);
        result = tupleLiteral;
    }

    @Override
    public void visit(BLangGroupExpr groupExpr) {
        if (groupExpr.isTypedescExpr) {
            final BLangTypedescExpr typedescExpr = new BLangTypedescExpr();
            typedescExpr.resolvedType = groupExpr.typedescType;
            typedescExpr.type = symTable.typeDesc;
            result = rewriteExpr(typedescExpr);
        } else {
            result = rewriteExpr(groupExpr.expression);
        }
    }

    @Override
    public void visit(BLangRecordLiteral recordLiteral) {
        rewriteVarRefFields(recordLiteral);
        List<RecordLiteralNode.RecordField> fields =  recordLiteral.fields;
        fields.sort((v1, v2) -> Boolean.compare(isComputedKey(v1), isComputedKey(v2)));

        // Process the key-val pairs in the record literal
        for (RecordLiteralNode.RecordField field : fields) {
            BLangRecordLiteral.BLangRecordKeyValueField keyValue = (BLangRecordLiteral.BLangRecordKeyValueField) field;
            BLangExpression keyExpr = keyValue.key.expr;
            if (!keyValue.key.computedKey && keyExpr.getKind() == NodeKind.SIMPLE_VARIABLE_REF) {
                BLangSimpleVarRef varRef = (BLangSimpleVarRef) keyExpr;
                keyValue.key.expr = createStringLiteral(varRef.pos, varRef.variableName.value);
            } else {
                keyValue.key.expr = rewriteExpr(keyValue.key.expr);
            }

            keyValue.valueExpr = rewriteExpr(keyValue.valueExpr);
        }

        BLangExpression expr;
        if (recordLiteral.type.tag == TypeTags.RECORD) {
            expr = new BLangStructLiteral(recordLiteral.pos, fields, recordLiteral.type);
        } else if (recordLiteral.type.tag == TypeTags.MAP) {
            expr = new BLangMapLiteral(recordLiteral.pos, fields, recordLiteral.type);
        } else {
            expr = new BLangJSONLiteral(recordLiteral.pos, fields, recordLiteral.type);
        }

        result = rewriteExpr(expr);
    }

    @Override
    public void visit(BLangTableLiteral tableLiteral) {
        tableLiteral.tableDataRows = rewriteExprs(tableLiteral.tableDataRows);
        //Generate key columns Array
        List<String> keyColumns = new ArrayList<>();
        for (BLangTableLiteral.BLangTableColumn column : tableLiteral.columns) {
            if (column.flagSet.contains(TableColumnFlag.PRIMARYKEY)) {
                keyColumns.add(column.columnName);
            }
        }
        BLangArrayLiteral keyColumnsArrayLiteral = createArrayLiteralExprNode();
        keyColumnsArrayLiteral.exprs = keyColumns.stream()
                .map(expr -> ASTBuilderUtil.createLiteral(tableLiteral.pos, symTable.stringType, expr))
                .collect(Collectors.toList());
        keyColumnsArrayLiteral.type = new BArrayType(symTable.stringType);
        tableLiteral.keyColumnsArrayLiteral = keyColumnsArrayLiteral;
        //Generate index columns Array
        List<String> indexColumns = new ArrayList<>();
        for (BLangTableLiteral.BLangTableColumn column : tableLiteral.columns) {
            if (column.flagSet.contains(TableColumnFlag.INDEX)) {
                indexColumns.add(column.columnName);
            }
        }
        BLangArrayLiteral indexColumnsArrayLiteral = createArrayLiteralExprNode();
        indexColumnsArrayLiteral.exprs = indexColumns.stream()
                .map(expr -> ASTBuilderUtil.createLiteral(tableLiteral.pos, symTable.stringType, expr))
                .collect(Collectors.toList());
        indexColumnsArrayLiteral.type = new BArrayType(symTable.stringType);
        tableLiteral.indexColumnsArrayLiteral = indexColumnsArrayLiteral;
        result = tableLiteral;
    }

    @Override
    public void visit(BLangSimpleVarRef varRefExpr) {
        BLangSimpleVarRef genVarRefExpr = varRefExpr;

        // XML qualified name reference. e.g: ns0:foo
        if (varRefExpr.pkgSymbol != null && varRefExpr.pkgSymbol.tag == SymTag.XMLNS) {
            BLangXMLQName qnameExpr = new BLangXMLQName(varRefExpr.variableName);
            qnameExpr.nsSymbol = (BXMLNSSymbol) varRefExpr.pkgSymbol;
            qnameExpr.localname = varRefExpr.variableName;
            qnameExpr.prefix = varRefExpr.pkgAlias;
            qnameExpr.namespaceURI = qnameExpr.nsSymbol.namespaceURI;
            qnameExpr.isUsedInXML = false;
            qnameExpr.pos = varRefExpr.pos;
            qnameExpr.type = symTable.stringType;
            result = qnameExpr;
            return;
        }

        if (varRefExpr.symbol == null) {
            result = varRefExpr;
            return;
        }

        // Restore the original symbol
        if ((varRefExpr.symbol.tag & SymTag.VARIABLE) == SymTag.VARIABLE) {
            BVarSymbol varSymbol = (BVarSymbol) varRefExpr.symbol;
            if (varSymbol.originalSymbol != null) {
                varRefExpr.symbol = varSymbol.originalSymbol;
            }
        }

        BSymbol ownerSymbol = varRefExpr.symbol.owner;
        if ((varRefExpr.symbol.tag & SymTag.FUNCTION) == SymTag.FUNCTION &&
                varRefExpr.symbol.type.tag == TypeTags.INVOKABLE) {
            genVarRefExpr = new BLangFunctionVarRef((BVarSymbol) varRefExpr.symbol);
        } else if ((varRefExpr.symbol.tag & SymTag.TYPE) == SymTag.TYPE) {
            genVarRefExpr = new BLangTypeLoad(varRefExpr.symbol);
        } else if ((ownerSymbol.tag & SymTag.INVOKABLE) == SymTag.INVOKABLE) {
            // Local variable in a function/resource/action/worker
            genVarRefExpr = new BLangLocalVarRef((BVarSymbol) varRefExpr.symbol);
        } else if ((ownerSymbol.tag & SymTag.STRUCT) == SymTag.STRUCT) {
            genVarRefExpr = new BLangFieldVarRef((BVarSymbol) varRefExpr.symbol);
        } else if ((ownerSymbol.tag & SymTag.PACKAGE) == SymTag.PACKAGE ||
                (ownerSymbol.tag & SymTag.SERVICE) == SymTag.SERVICE) {
            // If the var ref is a const-ref of value type, then replace the ref
            // from a simple literal
            if ((varRefExpr.symbol.tag & SymTag.CONSTANT) == SymTag.CONSTANT) {
                BConstantSymbol constSymbol = (BConstantSymbol) varRefExpr.symbol;
                if (constSymbol.literalType.tag <= TypeTags.BOOLEAN || constSymbol.literalType.tag == TypeTags.NIL) {
                    BLangLiteral literal = ASTBuilderUtil.createLiteral(varRefExpr.pos, constSymbol.literalType,
                            constSymbol.value.value);
                    result = rewriteExpr(addConversionExprIfRequired(literal, varRefExpr.type));
                    return;
                }
            }

            // Package variable | service variable.
            // We consider both of them as package level variables.
            genVarRefExpr = new BLangPackageVarRef((BVarSymbol) varRefExpr.symbol);

            if (!enclLocks.isEmpty()) {
                enclLocks.peek().addLockVariable((BVarSymbol) varRefExpr.symbol);
            }
        }

        genVarRefExpr.type = varRefExpr.type;
        genVarRefExpr.pos = varRefExpr.pos;

        if ((varRefExpr.lhsVar)
                || genVarRefExpr.symbol.name.equals(IGNORE)) { //TODO temp fix to get this running in bvm
            genVarRefExpr.lhsVar = varRefExpr.lhsVar;
            genVarRefExpr.type = varRefExpr.symbol.type;
            result = genVarRefExpr;
            return;
        }

        // If the the variable is not used in lhs, then add a conversion if required.
        // This is done to make the types compatible for narrowed types.
        genVarRefExpr.lhsVar = varRefExpr.lhsVar;
        BType targetType = genVarRefExpr.type;
        genVarRefExpr.type = genVarRefExpr.symbol.type;
        BLangExpression expression = addConversionExprIfRequired(genVarRefExpr, targetType);
        result = expression.impConversionExpr != null ? expression.impConversionExpr : expression;
    }

    @Override
    public void visit(BLangFieldBasedAccess fieldAccessExpr) {
        if (safeNavigate(fieldAccessExpr)) {
            result = rewriteExpr(rewriteSafeNavigationExpr(fieldAccessExpr));
            return;
        }

        BLangAccessExpression targetVarRef = fieldAccessExpr;

        // First get the type and then visit the expr. Order matters, since the desugar
        // can change the type of the expression, if it is type narrowed.
        BType varRefType = fieldAccessExpr.expr.type;
        fieldAccessExpr.expr = rewriteExpr(fieldAccessExpr.expr);
        if (!types.isSameType(fieldAccessExpr.expr.type, varRefType)) {
            fieldAccessExpr.expr = addConversionExprIfRequired(fieldAccessExpr.expr, varRefType);
        }

        BLangLiteral stringLit = createStringLiteral(fieldAccessExpr.pos, fieldAccessExpr.field.value);
        int varRefTypeTag = varRefType.tag;
        if (varRefTypeTag == TypeTags.OBJECT ||
                (varRefTypeTag == TypeTags.UNION &&
                         ((BUnionType) varRefType).getMemberTypes().iterator().next().tag == TypeTags.OBJECT)) {
            if (fieldAccessExpr.symbol != null && fieldAccessExpr.symbol.type.tag == TypeTags.INVOKABLE &&
                    ((fieldAccessExpr.symbol.flags & Flags.ATTACHED) == Flags.ATTACHED)) {
                targetVarRef = new BLangStructFunctionVarRef(fieldAccessExpr.expr, (BVarSymbol) fieldAccessExpr.symbol);
            } else {
                targetVarRef = new BLangStructFieldAccessExpr(fieldAccessExpr.pos, fieldAccessExpr.expr, stringLit,
                        (BVarSymbol) fieldAccessExpr.symbol, false);
                // Only supporting object field lock at the moment
                addToLocks((BLangStructFieldAccessExpr) targetVarRef);
            }
        } else if (varRefTypeTag == TypeTags.RECORD ||
                (varRefTypeTag == TypeTags.UNION &&
                         ((BUnionType) varRefType).getMemberTypes().iterator().next().tag == TypeTags.RECORD)) {
            if (fieldAccessExpr.symbol != null && fieldAccessExpr.symbol.type.tag == TypeTags.INVOKABLE
                    && ((fieldAccessExpr.symbol.flags & Flags.ATTACHED) == Flags.ATTACHED)) {
                targetVarRef = new BLangStructFunctionVarRef(fieldAccessExpr.expr, (BVarSymbol) fieldAccessExpr.symbol);
            } else {
                targetVarRef = new BLangStructFieldAccessExpr(fieldAccessExpr.pos, fieldAccessExpr.expr, stringLit,
                        (BVarSymbol) fieldAccessExpr.symbol, false);
            }
        } else if (types.isLax(varRefType)) {
            // Handle unions of lax types such as json|map<json>, by casting to json and creating a BLangJSONAccessExpr.
            fieldAccessExpr.expr = addConversionExprIfRequired(fieldAccessExpr.expr, symTable.jsonType);
            targetVarRef = new BLangJSONAccessExpr(fieldAccessExpr.pos, fieldAccessExpr.expr, stringLit);
        } else if (varRefTypeTag == TypeTags.MAP) {
            // TODO: 7/1/19 remove once foreach field access usage is removed.
            targetVarRef = new BLangMapAccessExpr(fieldAccessExpr.pos, fieldAccessExpr.expr, stringLit);
        } else if (varRefTypeTag == TypeTags.XML) {
            targetVarRef = new BLangXMLAccessExpr(fieldAccessExpr.pos, fieldAccessExpr.expr, stringLit,
                    fieldAccessExpr.fieldKind);
        }

        targetVarRef.lhsVar = fieldAccessExpr.lhsVar;
        targetVarRef.type = fieldAccessExpr.type;
        targetVarRef.optionalFieldAccess = fieldAccessExpr.optionalFieldAccess;
        result = targetVarRef;
    }

    private void addToLocks(BLangStructFieldAccessExpr targetVarRef) {
        if (enclLocks.isEmpty()) {
            return;
        }

        if (targetVarRef.expr.getKind() != NodeKind.SIMPLE_VARIABLE_REF
                || ((BLangSimpleVarRef) targetVarRef.expr).symbol.owner.getKind() == SymbolKind.PACKAGE
                || !Names.SELF.equals(((BLangLocalVarRef) targetVarRef.expr).symbol.name)) {
            return;
        }

        // expr symbol is null when their is a array as the field
        if (targetVarRef.indexExpr.getKind() == NodeKind.LITERAL) {
            String field = (String) ((BLangLiteral) targetVarRef.indexExpr).value;
            enclLocks.peek().addFieldVariable((BVarSymbol) ((BLangLocalVarRef) targetVarRef.expr).varSymbol, field);
        }
    }

    @Override
    public void visit(BLangIndexBasedAccess indexAccessExpr) {
        if (safeNavigate(indexAccessExpr)) {
            result = rewriteExpr(rewriteSafeNavigationExpr(indexAccessExpr));
            return;
        }

        BLangVariableReference targetVarRef = indexAccessExpr;
        indexAccessExpr.indexExpr = rewriteExpr(indexAccessExpr.indexExpr);

        // First get the type and then visit the expr. Order matters, since the desugar
        // can change the type of the expression, if it is type narrowed.
        BType varRefType = indexAccessExpr.expr.type;
        indexAccessExpr.expr = rewriteExpr(indexAccessExpr.expr);
        if (!types.isSameType(indexAccessExpr.expr.type, varRefType)) {
            indexAccessExpr.expr = addConversionExprIfRequired(indexAccessExpr.expr, varRefType);
        }

        if (varRefType.tag == TypeTags.MAP) {
            targetVarRef = new BLangMapAccessExpr(indexAccessExpr.pos, indexAccessExpr.expr, indexAccessExpr.indexExpr);
        } else if (types.isSubTypeOfMapping(types.getSafeType(varRefType, true, false))) {
            targetVarRef = new BLangStructFieldAccessExpr(indexAccessExpr.pos, indexAccessExpr.expr,
                    indexAccessExpr.indexExpr, (BVarSymbol) indexAccessExpr.symbol, false);
        } else if (types.isSubTypeOfList(varRefType)) {
            targetVarRef = new BLangArrayAccessExpr(indexAccessExpr.pos, indexAccessExpr.expr,
                    indexAccessExpr.indexExpr);
        } else if (types.isAssignable(varRefType, symTable.stringType)) {
            indexAccessExpr.expr = addConversionExprIfRequired(indexAccessExpr.expr, symTable.stringType);
            targetVarRef = new BLangStringAccessExpr(indexAccessExpr.pos, indexAccessExpr.expr,
                                                     indexAccessExpr.indexExpr);
        } else if (varRefType.tag == TypeTags.XML) {
            targetVarRef = new BLangXMLAccessExpr(indexAccessExpr.pos, indexAccessExpr.expr,
                    indexAccessExpr.indexExpr);
        }

        targetVarRef.lhsVar = indexAccessExpr.lhsVar;
        targetVarRef.type = indexAccessExpr.type;
        result = targetVarRef;
    }

    @Override
    public void visit(BLangInvocation iExpr) {
        BLangInvocation genIExpr = iExpr;

        if (iExpr.symbol != null && iExpr.symbol.kind == SymbolKind.ERROR_CONSTRUCTOR) {
            result = rewriteErrorConstructor(iExpr);
        }

        // Reorder the arguments to match the original function signature.
        reorderArguments(iExpr);
        iExpr.requiredArgs = rewriteExprs(iExpr.requiredArgs);
        iExpr.restArgs = rewriteExprs(iExpr.restArgs);

        annotationDesugar.defineStatementAnnotations(iExpr.annAttachments, iExpr.pos, iExpr.symbol.pkgID,
                iExpr.symbol.owner);

        if (iExpr.functionPointerInvocation) {
            visitFunctionPointerInvocation(iExpr);
            return;
        }
        iExpr.expr = rewriteExpr(iExpr.expr);
        result = genIExpr;


        if (iExpr.expr == null) {
            fixTypeCastInTypeParamInvocation(iExpr, genIExpr);
            if (iExpr.exprSymbol == null) {
                return;
            }
            iExpr.expr = ASTBuilderUtil.createVariableRef(iExpr.pos, iExpr.exprSymbol);
            iExpr.expr = rewriteExpr(iExpr.expr);
        }
        switch (iExpr.expr.type.tag) {
            case TypeTags.OBJECT:
            case TypeTags.RECORD:
                if (!iExpr.langLibInvocation) {
                    List<BLangExpression> argExprs = new ArrayList<>(iExpr.requiredArgs);
                    argExprs.add(0, iExpr.expr);
                    BLangAttachedFunctionInvocation attachedFunctionInvocation =
                            new BLangAttachedFunctionInvocation(iExpr.pos, argExprs, iExpr.restArgs, iExpr.symbol,
                                                                iExpr.type, iExpr.expr, iExpr.async);
                    attachedFunctionInvocation.actionInvocation = iExpr.actionInvocation;
                    attachedFunctionInvocation.name = iExpr.name;
                    attachedFunctionInvocation.annAttachments = iExpr.annAttachments;
                    result = genIExpr = attachedFunctionInvocation;
                }
                break;
        }

        fixTypeCastInTypeParamInvocation(iExpr, genIExpr);
    }

    private void fixTypeCastInTypeParamInvocation(BLangInvocation iExpr, BLangInvocation genIExpr) {

        if (iExpr.langLibInvocation || TypeParamAnalyzer.containsTypeParam(((BInvokableSymbol) iExpr.symbol).retType)) {
            BType originalInvType = genIExpr.type;
            genIExpr.type = ((BInvokableSymbol) genIExpr.symbol).retType;
            BLangExpression expr = addConversionExprIfRequired(genIExpr, originalInvType);

            // Prevent adding another type conversion
            if (expr.getKind() == NodeKind.TYPE_CONVERSION_EXPR) {
                this.result = expr;
                return;
            }

            BOperatorSymbol conversionSymbol = Symbols
                    .createCastOperatorSymbol(genIExpr.type, originalInvType, symTable.errorType, false, true,
                            null, null);
            BLangTypeConversionExpr conversionExpr = (BLangTypeConversionExpr) TreeBuilder.createTypeConversionNode();
            conversionExpr.expr = genIExpr;
            conversionExpr.targetType = originalInvType;
            conversionExpr.conversionSymbol = conversionSymbol;
            conversionExpr.type = originalInvType;
            conversionExpr.pos = genIExpr.pos;

            this.result = conversionExpr;
        }
    }

    private BLangInvocation rewriteErrorConstructor(BLangInvocation iExpr) {
        BLangExpression reasonExpr = iExpr.requiredArgs.get(0);
        if (reasonExpr.impConversionExpr != null &&
                reasonExpr.impConversionExpr.targetType.tag != TypeTags.STRING) {
            // Override casts to constants/finite types.
            // For reason expressions of any form, the cast has to be to string.
            reasonExpr.impConversionExpr = null;
        }
        reasonExpr = addConversionExprIfRequired(reasonExpr, symTable.stringType);
        reasonExpr = rewriteExpr(reasonExpr);
        iExpr.requiredArgs.remove(0);
        iExpr.requiredArgs.add(reasonExpr);

        BLangExpression errorDetail;
        BLangRecordLiteral recordLiteral = ASTBuilderUtil.createEmptyRecordLiteral(iExpr.pos,
                ((BErrorType) iExpr.symbol.type).detailType);
        List<BLangExpression> namedArgs = iExpr.requiredArgs.stream()
                .filter(a -> a.getKind() == NodeKind.NAMED_ARGS_EXPR)
                .collect(Collectors.toList());
        if (namedArgs.isEmpty()) {
            errorDetail = visitCloneReadonly(rewriteExpr(recordLiteral), recordLiteral.type);
        } else {
            for (BLangExpression arg : namedArgs) {
                BLangNamedArgsExpression namedArg = (BLangNamedArgsExpression) arg;
                BLangRecordLiteral.BLangRecordKeyValueField member = new BLangRecordLiteral.BLangRecordKeyValueField();
                member.key = new BLangRecordLiteral.BLangRecordKey(ASTBuilderUtil.createLiteral(namedArg.name.pos,
                        symTable.stringType, namedArg.name.value));

                if (recordLiteral.type.tag == TypeTags.RECORD) {
                    member.valueExpr = addConversionExprIfRequired(namedArg.expr, symTable.anyType);
                } else {
                    member.valueExpr = addConversionExprIfRequired(namedArg.expr, namedArg.expr.type);
                }
                recordLiteral.fields.add(member);
                iExpr.requiredArgs.remove(arg);
            }
            recordLiteral = rewriteExpr(recordLiteral);
            errorDetail = visitCloneReadonly(recordLiteral, ((BErrorType) iExpr.symbol.type).detailType);
        }
        iExpr.requiredArgs.add(errorDetail);
        return iExpr;
    }

    public void visit(BLangTypeInit typeInitExpr) {
        result = rewrite(desugarObjectTypeInit(typeInitExpr), env);
    }

    private BLangStatementExpression desugarObjectTypeInit(BLangTypeInit typeInitExpr) {
        typeInitExpr.desugared = true;
        BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(typeInitExpr.pos);

        // Person $obj$ = new;
        BType objType = getObjectType(typeInitExpr.type);
        BLangSimpleVariableDef objVarDef = createVarDef("$obj$", objType, typeInitExpr, typeInitExpr.pos);
        BLangSimpleVarRef objVarRef = ASTBuilderUtil.createVariableRef(typeInitExpr.pos, objVarDef.var.symbol);
        blockStmt.addStatement(objVarDef);
        typeInitExpr.initInvocation.exprSymbol = objVarDef.var.symbol;
        typeInitExpr.initInvocation.symbol = ((BObjectTypeSymbol) objType.tsymbol).generatedInitializerFunc.symbol;

        // __init() returning nil is the common case and the type test is not needed for it.
        if (typeInitExpr.initInvocation.type.tag == TypeTags.NIL) {
            BLangExpressionStmt initInvExpr = ASTBuilderUtil.createExpressionStmt(typeInitExpr.pos, blockStmt);
            initInvExpr.expr = typeInitExpr.initInvocation;
            typeInitExpr.initInvocation.name.value = Names.GENERATED_INIT_SUFFIX.value;
            BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(blockStmt, objVarRef);
            stmtExpr.type = objVarRef.symbol.type;
            return stmtExpr;
        }

        // var $temp$ = $obj$.__init();
        BLangSimpleVariableDef initInvRetValVarDef = createVarDef("$temp$", typeInitExpr.initInvocation.type,
                                                                  typeInitExpr.initInvocation, typeInitExpr.pos);
        blockStmt.addStatement(initInvRetValVarDef);

        // Person|error $result$;
        BLangSimpleVariableDef resultVarDef = createVarDef("$result$", typeInitExpr.type, null, typeInitExpr.pos);
        blockStmt.addStatement(resultVarDef);

        // if ($temp$ is error) {
        //      $result$ = $temp$;
        // } else {
        //      $result$ = $obj$;
        // }

        // Condition
        BLangSimpleVarRef initRetValVarRefInCondition =
                ASTBuilderUtil.createVariableRef(typeInitExpr.pos, initInvRetValVarDef.var.symbol);
        BLangBlockStmt thenStmt = ASTBuilderUtil.createBlockStmt(typeInitExpr.pos);
        BLangTypeTestExpr isErrorTest =
                ASTBuilderUtil.createTypeTestExpr(typeInitExpr.pos, initRetValVarRefInCondition, getErrorTypeNode());
        isErrorTest.type = symTable.booleanType;

        // If body
        BLangSimpleVarRef thenInitRetValVarRef =
                ASTBuilderUtil.createVariableRef(typeInitExpr.pos, initInvRetValVarDef.var.symbol);
        BLangSimpleVarRef thenResultVarRef =
                ASTBuilderUtil.createVariableRef(typeInitExpr.pos, resultVarDef.var.symbol);
        BLangAssignment errAssignment =
                ASTBuilderUtil.createAssignmentStmt(typeInitExpr.pos, thenResultVarRef, thenInitRetValVarRef);
        thenStmt.addStatement(errAssignment);

        // Else body
        BLangSimpleVarRef elseResultVarRef =
                ASTBuilderUtil.createVariableRef(typeInitExpr.pos, resultVarDef.var.symbol);
        BLangAssignment objAssignment =
                ASTBuilderUtil.createAssignmentStmt(typeInitExpr.pos, elseResultVarRef, objVarRef);
        BLangBlockStmt elseStmt = ASTBuilderUtil.createBlockStmt(typeInitExpr.pos);
        elseStmt.addStatement(objAssignment);

        BLangIf ifelse = ASTBuilderUtil.createIfElseStmt(typeInitExpr.pos, isErrorTest, thenStmt, elseStmt);
        blockStmt.addStatement(ifelse);

        BLangSimpleVarRef resultVarRef =
                ASTBuilderUtil.createVariableRef(typeInitExpr.pos, resultVarDef.var.symbol);
        BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(blockStmt, resultVarRef);
        stmtExpr.type = resultVarRef.symbol.type;
        return stmtExpr;
    }

    private BLangSimpleVariableDef createVarDef(String name, BType type, BLangExpression expr, DiagnosticPos pos) {
        BSymbol objSym = symResolver.lookupSymbol(env, names.fromString(name), SymTag.VARIABLE);
        if (objSym == null || objSym == symTable.notFoundSymbol) {
            objSym = new BVarSymbol(0, names.fromString(name), this.env.scope.owner.pkgID, type, this.env.scope.owner);
        }
        BLangSimpleVariable objVar = ASTBuilderUtil.createVariable(pos, "$" + name + "$", type, expr,
                (BVarSymbol) objSym);
        BLangSimpleVariableDef objVarDef = ASTBuilderUtil.createVariableDef(pos);
        objVarDef.var = objVar;
        objVarDef.type = objVar.type;
        return objVarDef;
    }

    private BType getObjectType(BType type) {
        if (type.tag == TypeTags.OBJECT) {
            return type;
        } else if (type.tag == TypeTags.UNION) {
            return ((BUnionType) type).getMemberTypes().stream()
                    .filter(t -> t.tag == TypeTags.OBJECT)
                    .findFirst()
                    .orElse(symTable.noType);
        }

        throw new IllegalStateException("None object type '" + type.toString() + "' found in object init context");
    }

    private BLangErrorType getErrorTypeNode() {
        BLangErrorType errorTypeNode = (BLangErrorType) TreeBuilder.createErrorTypeNode();
        errorTypeNode.type = symTable.errorType;
        return errorTypeNode;
    }

    @Override
    public void visit(BLangTernaryExpr ternaryExpr) {
        /*
         * First desugar to if-else:
         * 
         * T $result$;
         * if () {
         *    $result$ = thenExpr;
         * } else {
         *    $result$ = elseExpr;
         * }
         * 
         */
        BLangSimpleVariableDef resultVarDef = createVarDef("$ternary_result$", ternaryExpr.type, null, ternaryExpr.pos);
        BLangBlockStmt thenBody = ASTBuilderUtil.createBlockStmt(ternaryExpr.pos);
        BLangBlockStmt elseBody = ASTBuilderUtil.createBlockStmt(ternaryExpr.pos);

        // Create then assignment
        BLangSimpleVarRef thenResultVarRef = ASTBuilderUtil.createVariableRef(ternaryExpr.pos, resultVarDef.var.symbol);
        BLangAssignment thenAssignment =
                ASTBuilderUtil.createAssignmentStmt(ternaryExpr.pos, thenResultVarRef, ternaryExpr.thenExpr);
        thenBody.addStatement(thenAssignment);

        // Create else assignment
        BLangSimpleVarRef elseResultVarRef = ASTBuilderUtil.createVariableRef(ternaryExpr.pos, resultVarDef.var.symbol);
        BLangAssignment elseAssignment =
                ASTBuilderUtil.createAssignmentStmt(ternaryExpr.pos, elseResultVarRef, ternaryExpr.elseExpr);
        elseBody.addStatement(elseAssignment);

        // Then make it a expression-statement, with expression being the $result$
        BLangSimpleVarRef resultVarRef = ASTBuilderUtil.createVariableRef(ternaryExpr.pos, resultVarDef.var.symbol);
        BLangIf ifElse = ASTBuilderUtil.createIfElseStmt(ternaryExpr.pos, ternaryExpr.expr, thenBody, elseBody);

        BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(ternaryExpr.pos, Lists.of(resultVarDef, ifElse));
        BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(blockStmt, resultVarRef);
        stmtExpr.type = ternaryExpr.type;

        result = rewriteExpr(stmtExpr);
    }

    @Override
    public void visit(BLangWaitExpr waitExpr) {
        // Wait for any
        if (waitExpr.getExpression().getKind() == NodeKind.BINARY_EXPR) {
            waitExpr.exprList = collectAllBinaryExprs((BLangBinaryExpr) waitExpr.getExpression(), new ArrayList<>());
        } else { // Wait for one
            waitExpr.exprList = Collections.singletonList(rewriteExpr(waitExpr.getExpression()));
        }
        result = waitExpr;
    }

    private List<BLangExpression> collectAllBinaryExprs(BLangBinaryExpr binaryExpr, List<BLangExpression> exprs) {
        visitBinaryExprOfWait(binaryExpr.lhsExpr, exprs);
        visitBinaryExprOfWait(binaryExpr.rhsExpr, exprs);
        return exprs;
    }

    private void visitBinaryExprOfWait(BLangExpression expr, List<BLangExpression> exprs) {
        if (expr.getKind() == NodeKind.BINARY_EXPR) {
            collectAllBinaryExprs((BLangBinaryExpr) expr, exprs);
        } else {
            expr = rewriteExpr(expr);
            exprs.add(expr);
        }
    }

    @Override
    public void visit(BLangWaitForAllExpr waitExpr) {
        waitExpr.keyValuePairs.forEach(keyValue -> {
            if (keyValue.valueExpr != null) {
                keyValue.valueExpr = rewriteExpr(keyValue.valueExpr);
            } else {
                keyValue.keyExpr = rewriteExpr(keyValue.keyExpr);
            }
        });
        BLangExpression expr = new BLangWaitForAllExpr.BLangWaitLiteral(waitExpr.keyValuePairs, waitExpr.type);
        result = rewriteExpr(expr);
    }

    @Override
    public void visit(BLangTrapExpr trapExpr) {
        trapExpr.expr = rewriteExpr(trapExpr.expr);
        if (trapExpr.expr.type.tag != TypeTags.NIL) {
            trapExpr.expr = addConversionExprIfRequired(trapExpr.expr, trapExpr.type);
        }
        result = trapExpr;
    }

    @Override
    public void visit(BLangBinaryExpr binaryExpr) {
        if (binaryExpr.opKind == OperatorKind.HALF_OPEN_RANGE || binaryExpr.opKind == OperatorKind.CLOSED_RANGE) {
            if (binaryExpr.opKind == OperatorKind.HALF_OPEN_RANGE) {
                binaryExpr.rhsExpr = getModifiedIntRangeEndExpr(binaryExpr.rhsExpr);
            }
            result = rewriteExpr(replaceWithIntRange(binaryExpr.pos, binaryExpr.lhsExpr, binaryExpr.rhsExpr));
            return;
        }

        if (binaryExpr.opKind == OperatorKind.AND || binaryExpr.opKind == OperatorKind.OR) {
            visitBinaryLogicalExpr(binaryExpr);
            return;
        }

        OperatorKind binaryOpKind = binaryExpr.opKind;

        if (binaryOpKind == OperatorKind.ADD || binaryOpKind == OperatorKind.SUB ||
                binaryOpKind == OperatorKind.MUL || binaryOpKind == OperatorKind.DIV ||
                binaryOpKind == OperatorKind.MOD || binaryOpKind == OperatorKind.BITWISE_AND ||
                binaryOpKind == OperatorKind.BITWISE_OR || binaryOpKind == OperatorKind.BITWISE_XOR) {
            checkByteTypeIncompatibleOperations(binaryExpr);
        }

        binaryExpr.lhsExpr = rewriteExpr(binaryExpr.lhsExpr);
        binaryExpr.rhsExpr = rewriteExpr(binaryExpr.rhsExpr);
        result = binaryExpr;

        int rhsExprTypeTag = binaryExpr.rhsExpr.type.tag;
        int lhsExprTypeTag = binaryExpr.lhsExpr.type.tag;

        // Check for int and byte ==, != or === comparison and add type conversion to int for byte
        if (rhsExprTypeTag != lhsExprTypeTag && (binaryExpr.opKind == OperatorKind.EQUAL ||
                                                         binaryExpr.opKind == OperatorKind.NOT_EQUAL ||
                                                         binaryExpr.opKind == OperatorKind.REF_EQUAL ||
                                                         binaryExpr.opKind == OperatorKind.REF_NOT_EQUAL)) {
            if (lhsExprTypeTag == TypeTags.INT && rhsExprTypeTag == TypeTags.BYTE) {
                binaryExpr.rhsExpr = createTypeCastExpr(binaryExpr.rhsExpr, binaryExpr.rhsExpr.type,
                                                        symTable.intType);
                return;
            }

            if (lhsExprTypeTag == TypeTags.BYTE && rhsExprTypeTag == TypeTags.INT) {
                binaryExpr.lhsExpr = createTypeCastExpr(binaryExpr.lhsExpr, binaryExpr.lhsExpr.type,
                                                        symTable.intType);
                return;
            }
        }

        // Check lhs and rhs type compatibility
        if (lhsExprTypeTag == rhsExprTypeTag) {
            return;
        }

        if (lhsExprTypeTag == TypeTags.STRING && binaryExpr.opKind == OperatorKind.ADD) {
            // string + xml ==> (xml string) + xml
            if (rhsExprTypeTag == TypeTags.XML) {
                binaryExpr.lhsExpr = ASTBuilderUtil.createXMLTextLiteralNode(binaryExpr, binaryExpr.lhsExpr,
                        binaryExpr.lhsExpr.pos, symTable.xmlType);
                return;
            }
            binaryExpr.rhsExpr = createTypeCastExpr(binaryExpr.rhsExpr, binaryExpr.rhsExpr.type,
                                                    binaryExpr.lhsExpr.type);
            return;
        }

        if (rhsExprTypeTag == TypeTags.STRING && binaryExpr.opKind == OperatorKind.ADD) {
            // xml + string ==> xml + (xml string)
            if (lhsExprTypeTag == TypeTags.XML) {
                binaryExpr.rhsExpr = ASTBuilderUtil.createXMLTextLiteralNode(binaryExpr, binaryExpr.rhsExpr,
                        binaryExpr.rhsExpr.pos, symTable.xmlType);
                return;
            }
            binaryExpr.lhsExpr = createTypeCastExpr(binaryExpr.lhsExpr, binaryExpr.lhsExpr.type,
                    binaryExpr.rhsExpr.type);
            return;
        }

        if (lhsExprTypeTag == TypeTags.DECIMAL) {
            binaryExpr.rhsExpr = createTypeCastExpr(binaryExpr.rhsExpr, binaryExpr.rhsExpr.type,
                                                    binaryExpr.lhsExpr.type);
            return;
        }

        if (rhsExprTypeTag == TypeTags.DECIMAL) {
            binaryExpr.lhsExpr = createTypeCastExpr(binaryExpr.lhsExpr, binaryExpr.lhsExpr.type,
                    binaryExpr.rhsExpr.type);
            return;
        }

        if (lhsExprTypeTag == TypeTags.FLOAT) {
            binaryExpr.rhsExpr = createTypeCastExpr(binaryExpr.rhsExpr, binaryExpr.rhsExpr.type,
                                                    binaryExpr.lhsExpr.type);
            return;
        }

        if (rhsExprTypeTag == TypeTags.FLOAT) {
            binaryExpr.lhsExpr = createTypeCastExpr(binaryExpr.lhsExpr, binaryExpr.lhsExpr.type,
                                                    binaryExpr.rhsExpr.type);
        }
    }

    private BLangInvocation replaceWithIntRange(DiagnosticPos pos, BLangExpression lhsExpr, BLangExpression rhsExpr) {
        BInvokableSymbol symbol = (BInvokableSymbol) symTable.langInternalModuleSymbol.scope
                .lookup(Names.CREATE_INT_RANGE).symbol;
        BLangInvocation createIntRangeInvocation = ASTBuilderUtil.createInvocationExprForMethod(pos, symbol,
                new ArrayList<>(Lists.of(lhsExpr, rhsExpr)), symResolver);
        createIntRangeInvocation.type = symTable.intRangeType;
        return createIntRangeInvocation;
    }

    private void checkByteTypeIncompatibleOperations(BLangBinaryExpr binaryExpr) {
        if (binaryExpr.parent == null || binaryExpr.parent.type == null) {
            return;
        }

        int rhsExprTypeTag = binaryExpr.rhsExpr.type.tag;
        int lhsExprTypeTag = binaryExpr.lhsExpr.type.tag;
        if (rhsExprTypeTag != TypeTags.BYTE && lhsExprTypeTag != TypeTags.BYTE) {
            return;
        }

        int resultTypeTag = binaryExpr.type.tag;
        if (resultTypeTag == TypeTags.INT) {
            if (rhsExprTypeTag == TypeTags.BYTE) {
                binaryExpr.rhsExpr = addConversionExprIfRequired(binaryExpr.rhsExpr, symTable.intType);
            }

            if (lhsExprTypeTag == TypeTags.BYTE) {
                binaryExpr.lhsExpr = addConversionExprIfRequired(binaryExpr.lhsExpr, symTable.intType);
            }
        }
    }

    /**
     * This method checks whether given binary expression is related to shift operation.
     * If its true, then both lhs and rhs of the binary expression will be converted to 'int' type.
     * <p>
     * byte a = 12;
     * byte b = 34;
     * int i = 234;
     * int j = -4;
     * <p>
     * true: where binary expression's expected type is 'int'
     * int i1 = a >> b;
     * int i2 = a << b;
     * int i3 = a >> i;
     * int i4 = a << i;
     * int i5 = i >> j;
     * int i6 = i << j;
     */
    private boolean isBitwiseShiftOperation(BLangBinaryExpr binaryExpr) {
        return binaryExpr.opKind == OperatorKind.BITWISE_LEFT_SHIFT ||
                binaryExpr.opKind == OperatorKind.BITWISE_RIGHT_SHIFT ||
                binaryExpr.opKind == OperatorKind.BITWISE_UNSIGNED_RIGHT_SHIFT;
    }

    public void visit(BLangElvisExpr elvisExpr) {
        BLangMatchExpression matchExpr = ASTBuilderUtil.createMatchExpression(elvisExpr.lhsExpr);
        matchExpr.patternClauses.add(getMatchNullPatternGivenExpression(elvisExpr.pos,
                rewriteExpr(elvisExpr.rhsExpr)));
        matchExpr.type = elvisExpr.type;
        matchExpr.pos = elvisExpr.pos;
        result = rewriteExpr(matchExpr);
    }

    @Override
    public void visit(BLangUnaryExpr unaryExpr) {
        if (OperatorKind.BITWISE_COMPLEMENT == unaryExpr.operator) {
            // If this is a bitwise complement (~) expression, then we desugar it to a binary xor expression with -1,
            // which is same as doing a bitwise 2's complement operation.
            rewriteBitwiseComplementOperator(unaryExpr);
            return;
        }
        unaryExpr.expr = rewriteExpr(unaryExpr.expr);
        result = unaryExpr;
    }

    /**
     * This method desugar a bitwise complement (~) unary expressions into a bitwise xor binary expression as below.
     * Example : ~a  -> a ^ -1;
     * ~ 11110011 -> 00001100
     * 11110011 ^ 11111111 -> 00001100
     *
     * @param unaryExpr the bitwise complement expression
     */
    private void rewriteBitwiseComplementOperator(BLangUnaryExpr unaryExpr) {
        final DiagnosticPos pos = unaryExpr.pos;
        final BLangBinaryExpr binaryExpr = (BLangBinaryExpr) TreeBuilder.createBinaryExpressionNode();
        binaryExpr.pos = pos;
        binaryExpr.opKind = OperatorKind.BITWISE_XOR;
        binaryExpr.lhsExpr = unaryExpr.expr;
        if (TypeTags.BYTE == unaryExpr.type.tag) {
            binaryExpr.type = symTable.byteType;
            binaryExpr.rhsExpr = ASTBuilderUtil.createLiteral(pos, symTable.byteType, 0xffL);
            binaryExpr.opSymbol = (BOperatorSymbol) symResolver.resolveBinaryOperator(OperatorKind.BITWISE_XOR,
                    symTable.byteType, symTable.byteType);
        } else {
            binaryExpr.type = symTable.intType;
            binaryExpr.rhsExpr = ASTBuilderUtil.createLiteral(pos, symTable.intType, -1L);
            binaryExpr.opSymbol = (BOperatorSymbol) symResolver.resolveBinaryOperator(OperatorKind.BITWISE_XOR,
                    symTable.intType, symTable.intType);
        }
        result = rewriteExpr(binaryExpr);
    }

    @Override
    public void visit(BLangTypeConversionExpr conversionExpr) {
        // Usually the parameter for a type-cast-expr includes a type-descriptor.
        // However, it is also allowed for the parameter to consist only of annotations; in
        // this case, the only effect of the type cast is for the contextually expected
        // type for expression to be augmented with the specified annotations.

        // No actual type-cast is implied here.
        if (conversionExpr.typeNode == null && !conversionExpr.annAttachments.isEmpty()) {
            result = rewriteExpr(conversionExpr.expr);
            return;
        }
        conversionExpr.expr = rewriteExpr(conversionExpr.expr);
        result = conversionExpr;
    }

    @Override
    public void visit(BLangLambdaFunction bLangLambdaFunction) {
        // Collect all the lambda functions.
        env.enclPkg.lambdaFunctions.add(bLangLambdaFunction);
        result = bLangLambdaFunction;
    }

    @Override
    public void visit(BLangArrowFunction bLangArrowFunction) {
        BLangFunction bLangFunction = (BLangFunction) TreeBuilder.createFunctionNode();
        bLangFunction.setName(bLangArrowFunction.functionName);

        BLangLambdaFunction lambdaFunction = (BLangLambdaFunction) TreeBuilder.createLambdaFunctionNode();
        lambdaFunction.pos = bLangArrowFunction.pos;
        bLangFunction.addFlag(Flag.LAMBDA);
        lambdaFunction.function = bLangFunction;

        // Create function body with return node
        BLangValueType returnType = (BLangValueType) TreeBuilder.createValueTypeNode();
        returnType.type = bLangArrowFunction.expression.type;
        bLangFunction.setReturnTypeNode(returnType);
        bLangFunction.setBody(populateArrowExprBodyBlock(bLangArrowFunction));

        bLangArrowFunction.params.forEach(bLangFunction::addParameter);
        lambdaFunction.parent = bLangArrowFunction.parent;
        lambdaFunction.type = bLangArrowFunction.funcType;

        // Create function symbol.
        BLangFunction funcNode = lambdaFunction.function;
        BInvokableSymbol funcSymbol = Symbols.createFunctionSymbol(Flags.asMask(funcNode.flagSet),
                new Name(funcNode.name.value), env.enclPkg.symbol.pkgID, bLangArrowFunction.funcType,
                env.enclEnv.enclVarSym, true);
        SymbolEnv invokableEnv = SymbolEnv.createFunctionEnv(funcNode, funcSymbol.scope, env);
        defineInvokableSymbol(funcNode, funcSymbol, invokableEnv);

        List<BVarSymbol> paramSymbols = funcNode.requiredParams.stream().peek(varNode -> {
            Scope enclScope = invokableEnv.scope;
            varNode.symbol.kind = SymbolKind.FUNCTION;
            varNode.symbol.owner = invokableEnv.scope.owner;
            enclScope.define(varNode.symbol.name, varNode.symbol);
        }).map(varNode -> varNode.symbol).collect(Collectors.toList());

        funcSymbol.params = paramSymbols;
        funcSymbol.restParam = getRestSymbol(funcNode);
        funcSymbol.retType = funcNode.returnTypeNode.type;

        // Create function type.
        List<BType> paramTypes = paramSymbols.stream().map(paramSym -> paramSym.type).collect(Collectors.toList());
        funcNode.type = new BInvokableType(paramTypes, getRestType(funcSymbol), funcNode.returnTypeNode.type, null);

        lambdaFunction.function.pos = bLangArrowFunction.pos;
        lambdaFunction.function.funcBody.pos = bLangArrowFunction.pos;
        rewrite(lambdaFunction.function, env);
        env.enclPkg.addFunction(lambdaFunction.function);
        bLangArrowFunction.function = lambdaFunction.function;
        result = rewriteExpr(lambdaFunction);
    }

    private void defineInvokableSymbol(BLangInvokableNode invokableNode, BInvokableSymbol funcSymbol,
                                       SymbolEnv invokableEnv) {
        invokableNode.symbol = funcSymbol;
        funcSymbol.scope = new Scope(funcSymbol);
        invokableEnv.scope = funcSymbol.scope;
    }

    @Override
    public void visit(BLangXMLQName xmlQName) {
        result = xmlQName;
    }

    @Override
    public void visit(BLangXMLAttribute xmlAttribute) {
        xmlAttribute.name = rewriteExpr(xmlAttribute.name);
        xmlAttribute.value = rewriteExpr(xmlAttribute.value);
        result = xmlAttribute;
    }

    @Override
    public void visit(BLangXMLElementLiteral xmlElementLiteral) {
        xmlElementLiteral.startTagName = rewriteExpr(xmlElementLiteral.startTagName);
        xmlElementLiteral.endTagName = rewriteExpr(xmlElementLiteral.endTagName);
        xmlElementLiteral.modifiedChildren = rewriteExprs(xmlElementLiteral.modifiedChildren);
        xmlElementLiteral.attributes = rewriteExprs(xmlElementLiteral.attributes);

        // Separate the in-line namepsace declarations and attributes.
        Iterator<BLangXMLAttribute> attributesItr = xmlElementLiteral.attributes.iterator();
        while (attributesItr.hasNext()) {
            BLangXMLAttribute attribute = attributesItr.next();
            if (!attribute.isNamespaceDeclr) {
                continue;
            }

            // Create namepace declaration for all in-line namespace declarations
            BLangXMLNS xmlns;
            if ((xmlElementLiteral.scope.owner.tag & SymTag.PACKAGE) == SymTag.PACKAGE) {
                xmlns = new BLangPackageXMLNS();
            } else {
                xmlns = new BLangLocalXMLNS();
            }
            xmlns.namespaceURI = attribute.value.concatExpr;
            xmlns.prefix = ((BLangXMLQName) attribute.name).localname;
            xmlns.symbol = attribute.symbol;

            xmlElementLiteral.inlineNamespaces.add(xmlns);
            attributesItr.remove();
        }

        result = xmlElementLiteral;
    }

    @Override
    public void visit(BLangXMLTextLiteral xmlTextLiteral) {
        xmlTextLiteral.concatExpr = rewriteExpr(constructStringTemplateConcatExpression(xmlTextLiteral.textFragments));
        result = xmlTextLiteral;
    }

    @Override
    public void visit(BLangXMLCommentLiteral xmlCommentLiteral) {
        xmlCommentLiteral.concatExpr = rewriteExpr(
                constructStringTemplateConcatExpression(xmlCommentLiteral.textFragments));
        result = xmlCommentLiteral;
    }

    @Override
    public void visit(BLangXMLProcInsLiteral xmlProcInsLiteral) {
        xmlProcInsLiteral.target = rewriteExpr(xmlProcInsLiteral.target);
        xmlProcInsLiteral.dataConcatExpr =
                rewriteExpr(constructStringTemplateConcatExpression(xmlProcInsLiteral.dataFragments));
        result = xmlProcInsLiteral;
    }

    @Override
    public void visit(BLangXMLQuotedString xmlQuotedString) {
        xmlQuotedString.concatExpr = rewriteExpr(
                constructStringTemplateConcatExpression(xmlQuotedString.textFragments));
        result = xmlQuotedString;
    }

    @Override
    public void visit(BLangStringTemplateLiteral stringTemplateLiteral) {
        result = rewriteExpr(constructStringTemplateConcatExpression(stringTemplateLiteral.exprs));
    }

    @Override
    public void visit(BLangWorkerSend workerSendNode) {
        workerSendNode.expr = visitCloneInvocation(rewriteExpr(workerSendNode.expr), workerSendNode.expr.type);
        if (workerSendNode.keyExpr != null) {
            workerSendNode.keyExpr = rewriteExpr(workerSendNode.keyExpr);
        }
        result = workerSendNode;
    }

    @Override
    public void visit(BLangWorkerSyncSendExpr syncSendExpr) {
        syncSendExpr.expr = visitCloneInvocation(rewriteExpr(syncSendExpr.expr), syncSendExpr.expr.type);
        result = syncSendExpr;
    }

    @Override
    public void visit(BLangWorkerReceive workerReceiveNode) {
        if (workerReceiveNode.keyExpr != null) {
            workerReceiveNode.keyExpr = rewriteExpr(workerReceiveNode.keyExpr);
        }
        result = workerReceiveNode;
    }

    @Override
    public void visit(BLangWorkerFlushExpr workerFlushExpr) {
        workerFlushExpr.workerIdentifierList = workerFlushExpr.cachedWorkerSendStmts
                .stream().map(send -> send.workerIdentifier).distinct().collect(Collectors.toList());
        result = workerFlushExpr;
    }

    @Override
    public void visit(BLangXMLAttributeAccess xmlAttributeAccessExpr) {
        xmlAttributeAccessExpr.indexExpr = rewriteExpr(xmlAttributeAccessExpr.indexExpr);
        xmlAttributeAccessExpr.expr = rewriteExpr(xmlAttributeAccessExpr.expr);

        if (xmlAttributeAccessExpr.indexExpr != null
                && xmlAttributeAccessExpr.indexExpr.getKind() == NodeKind.XML_QNAME) {
            ((BLangXMLQName) xmlAttributeAccessExpr.indexExpr).isUsedInXML = true;
        }

        xmlAttributeAccessExpr.desugared = true;

        // When XmlAttributeAccess expression is not a LHS target of a assignment and not a part of a index access
        // it will be converted to a 'map<string>.convert(xmlRef@)'
        if (xmlAttributeAccessExpr.lhsVar || xmlAttributeAccessExpr.indexExpr != null) {
            result = xmlAttributeAccessExpr;
        } else {
            result = rewriteExpr(xmlAttributeAccessExpr);
        }
    }

    // Generated expressions. Following expressions are not part of the original syntax
    // tree which is coming out of the parser

    @Override
    public void visit(BLangLocalVarRef localVarRef) {
        result = localVarRef;
    }

    @Override
    public void visit(BLangFieldVarRef fieldVarRef) {
        result = fieldVarRef;
    }

    @Override
    public void visit(BLangPackageVarRef packageVarRef) {
        result = packageVarRef;
    }

    @Override
    public void visit(BLangFunctionVarRef functionVarRef) {
        result = functionVarRef;
    }

    @Override
    public void visit(BLangStructFieldAccessExpr fieldAccessExpr) {
        result = fieldAccessExpr;
    }

    @Override
    public void visit(BLangStructFunctionVarRef functionVarRef) {
        result = functionVarRef;
    }

    @Override
    public void visit(BLangMapAccessExpr mapKeyAccessExpr) {
        result = mapKeyAccessExpr;
    }

    @Override
    public void visit(BLangArrayAccessExpr arrayIndexAccessExpr) {
        result = arrayIndexAccessExpr;
    }

    @Override
    public void visit(BLangTupleAccessExpr arrayIndexAccessExpr) {
        result = arrayIndexAccessExpr;
    }

    @Override
    public void visit(BLangJSONLiteral jsonLiteral) {
        result = jsonLiteral;
    }

    @Override
    public void visit(BLangMapLiteral mapLiteral) {
        result = mapLiteral;
    }

    @Override
    public void visit(BLangStructLiteral structLiteral) {
        result = structLiteral;
    }

    @Override
    public void visit(BLangWaitForAllExpr.BLangWaitLiteral waitLiteral) {
        result = waitLiteral;
    }

    @Override
    public void visit(BLangIsAssignableExpr assignableExpr) {
        assignableExpr.lhsExpr = rewriteExpr(assignableExpr.lhsExpr);
        result = assignableExpr;
    }

    @Override
    public void visit(BFunctionPointerInvocation fpInvocation) {
        result = fpInvocation;
    }

    @Override
    public void visit(BLangTypedescExpr accessExpr) {
        result = accessExpr;
    }

    @Override
    public void visit(BLangIntRangeExpression intRangeExpression) {
        if (!intRangeExpression.includeStart) {
            intRangeExpression.startExpr = getModifiedIntRangeStartExpr(intRangeExpression.startExpr);
        }
        if (!intRangeExpression.includeEnd) {
            intRangeExpression.endExpr = getModifiedIntRangeEndExpr(intRangeExpression.endExpr);
        }

        intRangeExpression.startExpr = rewriteExpr(intRangeExpression.startExpr);
        intRangeExpression.endExpr = rewriteExpr(intRangeExpression.endExpr);
        result = intRangeExpression;
    }

    @Override
    public void visit(BLangRestArgsExpression bLangVarArgsExpression) {
        result = rewriteExpr(bLangVarArgsExpression.expr);
    }

    @Override
    public void visit(BLangNamedArgsExpression bLangNamedArgsExpression) {
        bLangNamedArgsExpression.expr = rewriteExpr(bLangNamedArgsExpression.expr);
        result = bLangNamedArgsExpression.expr;
    }

    @Override
    public void visit(BLangMatchExpression bLangMatchExpression) {
        // Add the implicit default pattern, that returns the original expression's value.
        addMatchExprDefaultCase(bLangMatchExpression);

        // Create a temp local var to hold the temp result of the match expression
        // eg: T a;
        String matchTempResultVarName = GEN_VAR_PREFIX.value + "temp_result";
        BLangSimpleVariable tempResultVar = ASTBuilderUtil.createVariable(bLangMatchExpression.pos,
                matchTempResultVarName, bLangMatchExpression.type, null,
                new BVarSymbol(0, names.fromString(matchTempResultVarName), this.env.scope.owner.pkgID,
                        bLangMatchExpression.type, this.env.scope.owner));

        BLangSimpleVariableDef tempResultVarDef =
                ASTBuilderUtil.createVariableDef(bLangMatchExpression.pos, tempResultVar);
        tempResultVarDef.desugared = true;

        BLangBlockStmt stmts = ASTBuilderUtil.createBlockStmt(bLangMatchExpression.pos, Lists.of(tempResultVarDef));
        List<BLangMatchTypedBindingPatternClause> patternClauses = new ArrayList<>();

        for (int i = 0; i < bLangMatchExpression.patternClauses.size(); i++) {
            BLangMatchExprPatternClause pattern = bLangMatchExpression.patternClauses.get(i);
            pattern.expr = rewriteExpr(pattern.expr);

            // Create var ref for the temp result variable
            // eg: var ref for 'a'
            BLangVariableReference tempResultVarRef =
                    ASTBuilderUtil.createVariableRef(bLangMatchExpression.pos, tempResultVar.symbol);

            // Create an assignment node. Add a conversion from rhs to lhs of the pattern, if required.
            pattern.expr = addConversionExprIfRequired(pattern.expr, tempResultVarRef.type);
            BLangAssignment assignmentStmt =
                    ASTBuilderUtil.createAssignmentStmt(pattern.pos, tempResultVarRef, pattern.expr);
            BLangBlockStmt patternBody = ASTBuilderUtil.createBlockStmt(pattern.pos, Lists.of(assignmentStmt));

            // Create the pattern
            // R b => a = b;
            patternClauses.add(ASTBuilderUtil.createMatchStatementPattern(pattern.pos, pattern.variable, patternBody));
        }

        stmts.addStatement(ASTBuilderUtil.createMatchStatement(bLangMatchExpression.pos, bLangMatchExpression.expr,
                patternClauses));
        BLangVariableReference tempResultVarRef =
                ASTBuilderUtil.createVariableRef(bLangMatchExpression.pos, tempResultVar.symbol);
        BLangStatementExpression statementExpr = ASTBuilderUtil.createStatementExpression(stmts, tempResultVarRef);
        statementExpr.type = bLangMatchExpression.type;
        result = rewriteExpr(statementExpr);
    }

    @Override
    public void visit(BLangCheckedExpr checkedExpr) {
        visitCheckAndCheckPanicExpr(checkedExpr, false);
    }

    @Override
    public void visit(BLangCheckPanickedExpr checkedExpr) {
        visitCheckAndCheckPanicExpr(checkedExpr, true);
    }

    private void visitCheckAndCheckPanicExpr(BLangCheckedExpr checkedExpr, boolean isCheckPanic) {
        //
        //  person p = bar(check foo()); // foo(): person | error
        //
        //    ==>
        //
        //  person _$$_;
        //  switch foo() {
        //      person p1 => _$$_ = p1;
        //      error e1 => return e1 or throw e1
        //  }
        //  person p = bar(_$$_);

        // Create a temporary variable to hold the checked expression result value e.g. _$$_
        String checkedExprVarName = GEN_VAR_PREFIX.value;
        BLangSimpleVariable checkedExprVar = ASTBuilderUtil.createVariable(checkedExpr.pos,
                checkedExprVarName, checkedExpr.type, null, new BVarSymbol(0,
                        names.fromString(checkedExprVarName),
                        this.env.scope.owner.pkgID, checkedExpr.type, this.env.scope.owner));
        BLangSimpleVariableDef checkedExprVarDef = ASTBuilderUtil.createVariableDef(checkedExpr.pos, checkedExprVar);
        checkedExprVarDef.desugared = true;

        // Create the pattern to match the success case
        BLangMatchTypedBindingPatternClause patternSuccessCase =
                getSafeAssignSuccessPattern(checkedExprVar.pos, checkedExprVar.symbol.type, true,
                        checkedExprVar.symbol, null);
        BLangMatchTypedBindingPatternClause patternErrorCase = getSafeAssignErrorPattern(checkedExpr.pos,
                this.env.scope.owner, checkedExpr.equivalentErrorTypeList, isCheckPanic);

        // Create the match statement
        BLangMatch matchStmt = ASTBuilderUtil.createMatchStatement(checkedExpr.pos, checkedExpr.expr,
                new ArrayList<BLangMatchTypedBindingPatternClause>() {{
                    add(patternSuccessCase);
                    add(patternErrorCase);
                }});

        // Create the block statement
        BLangBlockStmt generatedStmtBlock = ASTBuilderUtil.createBlockStmt(checkedExpr.pos,
                new ArrayList<BLangStatement>() {{
                    add(checkedExprVarDef);
                    add(matchStmt);
                }});

        // Create the variable ref expression for the checkedExprVar
        BLangSimpleVarRef tempCheckedExprVarRef = ASTBuilderUtil.createVariableRef(
                checkedExpr.pos, checkedExprVar.symbol);

        BLangStatementExpression statementExpr = ASTBuilderUtil.createStatementExpression(
                generatedStmtBlock, tempCheckedExprVarRef);
        statementExpr.type = checkedExpr.type;
        result = rewriteExpr(statementExpr);
    }

    @Override
    public void visit(BLangServiceConstructorExpr serviceConstructorExpr) {
        final BLangTypeInit typeInit = ASTBuilderUtil.createEmptyTypeInit(serviceConstructorExpr.pos,
                serviceConstructorExpr.serviceNode.serviceTypeDefinition.symbol.type);
        serviceConstructorExpr.serviceNode.annAttachments.forEach(attachment ->  rewrite(attachment, env));
        result = rewriteExpr(typeInit);
    }

    @Override
    public void visit(BLangTypeTestExpr typeTestExpr) {
        BLangExpression expr = typeTestExpr.expr;
        if (types.isValueType(expr.type)) {
            addConversionExprIfRequired(expr, symTable.anyType);
        }
        typeTestExpr.expr = rewriteExpr(expr);
        result = typeTestExpr;
    }

    @Override
    public void visit(BLangAnnotAccessExpr annotAccessExpr) {
        BLangBinaryExpr binaryExpr = (BLangBinaryExpr) TreeBuilder.createBinaryExpressionNode();
        binaryExpr.pos = annotAccessExpr.pos;
        binaryExpr.opKind = OperatorKind.ANNOT_ACCESS;
        binaryExpr.lhsExpr = annotAccessExpr.expr;
        binaryExpr.rhsExpr = ASTBuilderUtil.createLiteral(annotAccessExpr.pkgAlias.pos, symTable.stringType,
                                                          annotAccessExpr.annotationSymbol.bvmAlias());
        binaryExpr.type = annotAccessExpr.type;
        binaryExpr.opSymbol = new BOperatorSymbol(names.fromString(OperatorKind.ANNOT_ACCESS.value()), null,
                                                  new BInvokableType(Lists.of(binaryExpr.lhsExpr.type,
                                                                              binaryExpr.rhsExpr.type),
                                                          annotAccessExpr.type, null), null);
        result = rewriteExpr(binaryExpr);
    }

    @Override
    public void visit(BLangIsLikeExpr isLikeExpr) {
        isLikeExpr.expr = rewriteExpr(isLikeExpr.expr);
        result = isLikeExpr;
    }

    @Override
    public void visit(BLangStatementExpression bLangStatementExpression) {
        bLangStatementExpression.expr = rewriteExpr(bLangStatementExpression.expr);
        bLangStatementExpression.stmt = rewrite(bLangStatementExpression.stmt, env);
        result = bLangStatementExpression;
    }

    @Override
    public void visit(BLangQueryExpr queryExpr) {
        List<BLangFromClause> fromClauseList = queryExpr.fromClauseList;
        BLangFromClause fromClause = fromClauseList.get(0);
        BLangSelectClause selectClause = queryExpr.selectClause;
        List<BLangWhereClause> whereClauseList = queryExpr.whereClauseList;
        DiagnosticPos pos = fromClause.pos;

        // Create Foreach statement
        //
        // Below query expression :
        //      from var person in personList
        //
        // changes as,
        //      foreach var person in personList {
        //          ....
        //      }

        BLangForeach leafForEach = null;
        BLangForeach parentForEach = null;

        for (BLangFromClause bLangFromClause : fromClauseList) {
            BLangForeach foreach = (BLangForeach) TreeBuilder.createForeachNode();
            foreach.pos = queryExpr.pos;
            foreach.collection = bLangFromClause.collection;
            types.setForeachTypedBindingPatternType(foreach);

            foreach.variableDefinitionNode = bLangFromClause.variableDefinitionNode;
            foreach.isDeclaredWithVar = fromClause.isDeclaredWithVar;

            if (leafForEach != null) {
                BLangBlockStmt foreachBody = ASTBuilderUtil.createBlockStmt(pos);
                foreachBody.addStatement(foreach);
                leafForEach.setBody(foreachBody);
            } else {
                parentForEach = foreach;
            }

            leafForEach = foreach;
        }

        BLangBlockStmt foreachBody = ASTBuilderUtil.createBlockStmt(pos);

        BType outputArrayType;
        if (selectClause.expression  != null && selectClause.expression.type != null) {
            outputArrayType = new BArrayType(selectClause.expression.type);
        } else {
            outputArrayType = fromClause.varType;
        }

        BLangListConstructorExpr emptyArrayExpr = ASTBuilderUtil.createEmptyArrayLiteral(pos,
                (BArrayType) outputArrayType);
        BVarSymbol emptyArrayVarSymbol = new BVarSymbol(0, new Name("$outputDataArray$"),
                this.env.scope.owner.pkgID, outputArrayType, env.scope.owner);
        BLangSimpleVariable outputArrayVariable =
                ASTBuilderUtil.createVariable(pos, "$outputDataArray$", outputArrayType,
                        emptyArrayExpr, emptyArrayVarSymbol);

        // Create temp array variable
        //      Person[] x = [];

        BLangSimpleVariableDef outputVariableDef =
                ASTBuilderUtil.createVariableDef(pos, outputArrayVariable);
        BLangSimpleVarRef outputVarRef = ASTBuilderUtil.createVariableRef(pos, outputArrayVariable.symbol);

        // Create indexed based access expression statement
        //      x[x.length()] = {
        //         firstName: person.firstName,
        //         lastName: person.lastName
        //      };

        if (selectClause.expression.type == null) {
            selectClause.expression.type = fromClause.varType;
        }

        BLangInvocation lengthInvocation = createLengthInvocation(pos, outputArrayVariable.symbol);
        lengthInvocation.expr = outputVarRef;
        BLangIndexBasedAccess indexAccessExpr = ASTBuilderUtil.createIndexAccessExpr(outputVarRef, lengthInvocation);
        indexAccessExpr.type = selectClause.expression.type;

        BLangAssignment outputVarAssignment = ASTBuilderUtil.createAssignmentStmt(pos, indexAccessExpr,
                selectClause.expression);
        // Set the indexed based access expression statement as foreach body
        foreachBody.addStatement(outputVarAssignment);

        if (whereClauseList.size() > 0) {
            // Create If Statement with Where expression and foreach body
            BLangIf outerIf = null;
            BLangIf innerIf = null;
            for (BLangWhereClause whereClause : whereClauseList) {
                BLangIf bLangIf = (BLangIf) TreeBuilder.createIfElseStatementNode();
                bLangIf.pos = queryExpr.pos;
                bLangIf.expr = whereClause.expression;
                if (innerIf != null) {
                    BLangBlockStmt bLangBlockStmt = ASTBuilderUtil.createBlockStmt(pos);
                    bLangBlockStmt.addStatement(bLangIf);
                    innerIf.setBody(bLangBlockStmt);
                } else {
                    outerIf = bLangIf;
                }
                innerIf = bLangIf;
            }
            innerIf.setBody(foreachBody);
            BLangBlockStmt bLangBlockStmt = ASTBuilderUtil.createBlockStmt(pos);
            bLangBlockStmt.addStatement(outerIf);
            leafForEach.setBody(bLangBlockStmt);
        } else {
            leafForEach.setBody(foreachBody);
        }

        // Create block statement with temp variable definition statement & foreach statement
        BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(pos);
        blockStmt.addStatement(outputVariableDef);
        blockStmt.addStatement(parentForEach);
        BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(blockStmt, outputVarRef);

        stmtExpr.type = outputArrayType;
        result = rewrite(stmtExpr, env);
    }


    private BLangInvocation createLengthInvocation(DiagnosticPos pos, BVarSymbol collectionSymbol) {
        BInvokableSymbol lengthInvokableSymbol =
                (BInvokableSymbol) symResolver.lookupLangLibMethod(collectionSymbol.type,
                        names.fromString("length"));
        BLangSimpleVarRef collection = ASTBuilderUtil.createVariableRef(pos, collectionSymbol);
        BLangInvocation lengthInvocation = ASTBuilderUtil.createInvocationExprForMethod(pos, lengthInvokableSymbol,
                Lists.of(collection), symResolver);
        lengthInvocation.type = lengthInvokableSymbol.type.getReturnType();
        // Note: No need to set lengthInvocation.expr for langLib functions as they are in requiredArgs
        return lengthInvocation;
    }

    @Override
    public void visit(BLangJSONArrayLiteral jsonArrayLiteral) {
        jsonArrayLiteral.exprs = rewriteExprs(jsonArrayLiteral.exprs);
        result = jsonArrayLiteral;
    }

    @Override
    public void visit(BLangConstant constant) {

        BConstantSymbol constSymbol = constant.symbol;
        if (constSymbol.literalType.tag <= TypeTags.BOOLEAN || constSymbol.literalType.tag == TypeTags.NIL) {
            if (constSymbol.literalType.tag != TypeTags.NIL && constSymbol.value.value == null) {
                throw new IllegalStateException();
            }
            BLangLiteral literal = ASTBuilderUtil.createLiteral(constant.expr.pos, constSymbol.literalType,
                    constSymbol.value.value);
            constant.expr = rewriteExpr(literal);
        } else {
            constant.expr = rewriteExpr(constant.expr);
        }
        constant.annAttachments.forEach(attachment ->  rewrite(attachment, env));
        result = constant;
    }

    @Override
    public void visit(BLangIgnoreExpr ignoreExpr) {
        result = ignoreExpr;
    }

    @Override
    public void visit(BLangConstRef constantRef) {
        result = constantRef;
    }

    // private functions

    // Foreach desugar helper method.
    private BLangSimpleVariableDef getIteratorVariableDefinition(DiagnosticPos pos, BVarSymbol collectionSymbol,
                                                                 BInvokableSymbol iteratorInvokableSymbol,
                                                                 boolean isIteratorFuncFromLangLib) {


        BLangSimpleVarRef dataReference = ASTBuilderUtil.createVariableRef(pos, collectionSymbol);
        BLangInvocation iteratorInvocation = (BLangInvocation) TreeBuilder.createInvocationNode();
        iteratorInvocation.pos = pos;
        iteratorInvocation.expr = dataReference;
        iteratorInvocation.symbol = iteratorInvokableSymbol;
        iteratorInvocation.type = iteratorInvokableSymbol.retType;
        iteratorInvocation.argExprs = Lists.of(dataReference);
        iteratorInvocation.requiredArgs = iteratorInvocation.argExprs;
        iteratorInvocation.langLibInvocation = isIteratorFuncFromLangLib;
        BVarSymbol iteratorSymbol = new BVarSymbol(0, names.fromString("$iterator$"), this.env.scope.owner.pkgID,
                iteratorInvokableSymbol.retType, this.env.scope.owner);

        // Note - any $iterator$ = $data$.iterator();
        BLangSimpleVariable iteratorVariable = ASTBuilderUtil.createVariable(pos, "$iterator$",
                iteratorInvokableSymbol.retType, iteratorInvocation, iteratorSymbol);
        return ASTBuilderUtil.createVariableDef(pos, iteratorVariable);
    }

    // Foreach desugar helper method.
    private BLangSimpleVariableDef getIteratorNextVariableDefinition(BLangForeach foreach,
                                                                     BVarSymbol iteratorSymbol,
                                                                     BVarSymbol resultSymbol) {
        BLangInvocation nextInvocation = createIteratorNextInvocation(foreach, iteratorSymbol);
        BLangSimpleVariable resultVariable = ASTBuilderUtil.createVariable(foreach.pos, "$result$",
                foreach.nillableResultType, nextInvocation, resultSymbol);
        return ASTBuilderUtil.createVariableDef(foreach.pos, resultVariable);
    }

    // Foreach desugar helper method.
    private BLangAssignment getIteratorNextAssignment(BLangForeach foreach,
                                                      BVarSymbol iteratorSymbol, BVarSymbol resultSymbol) {
        BLangSimpleVarRef resultReferenceInAssignment = ASTBuilderUtil.createVariableRef(foreach.pos, resultSymbol);

        // Note - $iterator$.next();
        BLangInvocation nextInvocation = createIteratorNextInvocation(foreach, iteratorSymbol);

        // we are inside the while loop. hence the iterator cannot be nil. hence remove nil from iterator's type
        nextInvocation.expr.type = types.getSafeType(nextInvocation.expr.type, true, false);

        return ASTBuilderUtil.createAssignmentStmt(foreach.pos, resultReferenceInAssignment, nextInvocation, false);
    }

    private BLangInvocation createIteratorNextInvocation(BLangForeach foreach, BVarSymbol iteratorSymbol) {
        BLangIdentifier nextIdentifier = ASTBuilderUtil.createIdentifier(foreach.pos, "next");
        BLangSimpleVarRef iteratorReferenceInNext = ASTBuilderUtil.createVariableRef(foreach.pos, iteratorSymbol);
        BInvokableSymbol nextFuncSymbol = getNextFunc((BObjectType) iteratorSymbol.type).symbol;
        BLangInvocation nextInvocation = (BLangInvocation) TreeBuilder.createInvocationNode();
        nextInvocation.pos = foreach.pos;
        nextInvocation.name = nextIdentifier;
        nextInvocation.expr = iteratorReferenceInNext;
        nextInvocation.requiredArgs = Lists.of(ASTBuilderUtil.createVariableRef(foreach.pos, iteratorSymbol));
        nextInvocation.argExprs = nextInvocation.requiredArgs;
        nextInvocation.symbol = nextFuncSymbol;
        nextInvocation.type = nextFuncSymbol.retType;
        return nextInvocation;
    }

    private BAttachedFunction getNextFunc(BObjectType iteratorType) {
        BObjectTypeSymbol iteratorSymbol = (BObjectTypeSymbol) iteratorType.tsymbol;
        for (BAttachedFunction bAttachedFunction : iteratorSymbol.attachedFuncs) {
            if (bAttachedFunction.funcName.value.equals("next")) {
                return bAttachedFunction;
            }
        }
        return null;
    }

    // Foreach desugar helper method.
    private BLangFieldBasedAccess getValueAccessExpression(BLangForeach foreach, BVarSymbol resultSymbol) {
        BLangSimpleVarRef resultReferenceInVariableDef = ASTBuilderUtil.createVariableRef(foreach.pos, resultSymbol);
        BLangIdentifier valueIdentifier = ASTBuilderUtil.createIdentifier(foreach.pos, "value");

        BLangFieldBasedAccess fieldBasedAccessExpression =
                ASTBuilderUtil.createFieldAccessExpr(resultReferenceInVariableDef, valueIdentifier);
        fieldBasedAccessExpression.pos = foreach.pos;
        fieldBasedAccessExpression.type = foreach.varType;
        fieldBasedAccessExpression.originalType = fieldBasedAccessExpression.type;
        return fieldBasedAccessExpression;
    }

    private BlockFunctionBodyNode populateArrowExprBodyBlock(BLangArrowFunction bLangArrowFunction) {
        BlockFunctionBodyNode blockNode = TreeBuilder.createBlockFunctionBodyNode();
        BLangReturn returnNode = (BLangReturn) TreeBuilder.createReturnNode();
        returnNode.pos = bLangArrowFunction.expression.pos;
        returnNode.setExpression(bLangArrowFunction.expression);
        blockNode.addStatement(returnNode);
        return blockNode;
    }

    private BLangInvocation createInvocationNode(String functionName, List<BLangExpression> args, BType retType) {
        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        BLangIdentifier name = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        name.setLiteral(false);
        name.setValue(functionName);
        invocationNode.name = name;
        invocationNode.pkgAlias = (BLangIdentifier) TreeBuilder.createIdentifierNode();

        // TODO: 2/28/18 need to find a good way to refer to symbols
        invocationNode.symbol = symTable.rootScope.lookup(new Name(functionName)).symbol;
        invocationNode.type = retType;
        invocationNode.requiredArgs = args;
        return invocationNode;
    }

    private BLangInvocation createLangLibInvocationNode(String functionName,
                                                        BLangExpression onExpr,
                                                        List<BLangExpression> args,
                                                        BType retType,
                                                        DiagnosticPos pos) {
        BLangInvocation invocationNode = (BLangInvocation) TreeBuilder.createInvocationNode();
        invocationNode.pos = pos;
        BLangIdentifier name = (BLangIdentifier) TreeBuilder.createIdentifierNode();
        name.setLiteral(false);
        name.setValue(functionName);
        name.pos = pos;
        invocationNode.name = name;
        invocationNode.pkgAlias = (BLangIdentifier) TreeBuilder.createIdentifierNode();

        invocationNode.expr = onExpr;
        invocationNode.symbol = symResolver.lookupLangLibMethod(onExpr.type, names.fromString(functionName));
        ArrayList<BLangExpression> requiredArgs = new ArrayList<>();
        requiredArgs.add(onExpr);
        requiredArgs.addAll(args);
        invocationNode.requiredArgs = requiredArgs;
        invocationNode.type = retType != null ? retType : ((BInvokableSymbol) invocationNode.symbol).retType;
        invocationNode.langLibInvocation = true;
        return invocationNode;
    }

    private BLangArrayLiteral createArrayLiteralExprNode() {
        BLangArrayLiteral expr = (BLangArrayLiteral) TreeBuilder.createArrayLiteralExpressionNode();
        expr.exprs = new ArrayList<>();
        expr.type = new BArrayType(symTable.anyType);
        return expr;
    }

    private void visitFunctionPointerInvocation(BLangInvocation iExpr) {
        BLangVariableReference expr;
        if (iExpr.expr == null) {
            expr = new BLangSimpleVarRef();
        } else {
            BLangFieldBasedAccess fieldBasedAccess = new BLangFieldBasedAccess();
            fieldBasedAccess.expr = iExpr.expr;
            fieldBasedAccess.field = iExpr.name;
            expr = fieldBasedAccess;
        }
        expr.symbol = iExpr.symbol;
        expr.type = iExpr.symbol.type;

        BLangExpression rewritten = rewriteExpr(expr);
        result = new BFunctionPointerInvocation(iExpr, rewritten);
    }

    private BLangExpression visitCloneInvocation(BLangExpression expr, BType lhsType) {
        if (types.isValueType(expr.type)) {
            return expr;
        }
        if (expr.type.tag == TypeTags.ERROR) {
            return expr;
        }
        BLangInvocation cloneInvok = createLangLibInvocationNode("clone", expr, new ArrayList<>(), expr.type, expr.pos);
        return addConversionExprIfRequired(cloneInvok, lhsType);
    }

    private BLangExpression visitCloneReadonly(BLangExpression expr, BType lhsType) {
        if (types.isValueType(expr.type)) {
            return expr;
        }
        if (expr.type.tag == TypeTags.ERROR) {
            return expr;
        }
        BLangInvocation cloneInvok = createLangLibInvocationNode("cloneReadOnly", expr, new ArrayList<>(), expr.type,
                expr.pos);
        return addConversionExprIfRequired(cloneInvok, lhsType);
    }

    @SuppressWarnings("unchecked")
    <E extends BLangNode> E rewrite(E node, SymbolEnv env) {
        if (node == null) {
            return null;
        }

        if (node.desugared) {
            return node;
        }

        SymbolEnv previousEnv = this.env;
        this.env = env;

        node.accept(this);
        BLangNode resultNode = this.result;
        this.result = null;
        resultNode.desugared = true;

        this.env = previousEnv;
        return (E) resultNode;
    }

    @SuppressWarnings("unchecked")
    <E extends BLangExpression> E rewriteExpr(E node) {
        if (node == null) {
            return null;
        }

        if (node.desugared) {
            return node;
        }

        BLangExpression expr = node;
        if (node.impConversionExpr != null) {
            expr = node.impConversionExpr;
            node.impConversionExpr = null;
        }

        expr.accept(this);
        BLangNode resultNode = this.result;
        this.result = null;
        resultNode.desugared = true;
        return (E) resultNode;
    }

    @SuppressWarnings("unchecked")
    <E extends BLangStatement> E rewrite(E statement, SymbolEnv env) {
        if (statement == null) {
            return null;
        }
        BLangStatementLink link = new BLangStatementLink();
        link.parent = currentLink;
        currentLink = link;
        BLangStatement stmt = (BLangStatement) rewrite((BLangNode) statement, env);
        // Link Statements.
        link.statement = stmt;
        stmt.statementLink = link;
        currentLink = link.parent;
        return (E) stmt;
    }

    private <E extends BLangStatement> List<E> rewriteStmt(List<E> nodeList, SymbolEnv env) {
        for (int i = 0; i < nodeList.size(); i++) {
            nodeList.set(i, rewrite(nodeList.get(i), env));
        }
        return nodeList;
    }

    private <E extends BLangNode> List<E> rewrite(List<E> nodeList, SymbolEnv env) {
        for (int i = 0; i < nodeList.size(); i++) {
            nodeList.set(i, rewrite(nodeList.get(i), env));
        }
        return nodeList;
    }

    private <E extends BLangExpression> List<E> rewriteExprs(List<E> nodeList) {
        for (int i = 0; i < nodeList.size(); i++) {
            nodeList.set(i, rewriteExpr(nodeList.get(i)));
        }
        return nodeList;
    }

    private BLangLiteral createStringLiteral(DiagnosticPos pos, String value) {
        BLangLiteral stringLit = new BLangLiteral(value, symTable.stringType);
        stringLit.pos = pos;
        return stringLit;
    }

    private BLangLiteral createByteLiteral(DiagnosticPos pos, Byte value) {
        BLangLiteral byteLiteral = new BLangLiteral(Byte.toUnsignedInt(value), symTable.byteType);
        byteLiteral.pos = pos;
        return byteLiteral;
    }

    private BLangExpression createTypeCastExpr(BLangExpression expr, BType sourceType, BType targetType) {
        BOperatorSymbol symbol = (BOperatorSymbol) symResolver.resolveConversionOperator(sourceType, targetType);
        return createTypeCastExpr(expr, targetType, symbol);
    }

    private BLangExpression createTypeCastExpr(BLangExpression expr, BType targetType,
                                               BOperatorSymbol symbol) {
        BLangTypeConversionExpr conversionExpr = (BLangTypeConversionExpr) TreeBuilder.createTypeConversionNode();
        conversionExpr.pos = expr.pos;
        conversionExpr.expr = expr;
        conversionExpr.type = targetType;
        conversionExpr.targetType = targetType;
        conversionExpr.conversionSymbol = symbol;
        return conversionExpr;
    }

    private BType getElementType(BType type) {
        if (type.tag != TypeTags.ARRAY) {
            return type;
        }

        return getElementType(((BArrayType) type).getElementType());
    }

    // TODO: See if this is needed at all. Can't this be done when rewriting the function body?
    private void addReturnIfNotPresent(BLangInvokableNode invokableNode) {
        if (Symbols.isNative(invokableNode.symbol) ||
                (invokableNode.hasBody() && invokableNode.funcBody.getKind() != NodeKind.BLOCK_FUNCTION_BODY)) {
            return;
        }
        //This will only check whether last statement is a return and just add a return statement.
        //This won't analyse if else blocks etc to see whether return statements are present
        BLangBlockFunctionBody funcBody = (BLangBlockFunctionBody) invokableNode.funcBody;
        if (invokableNode.workers.size() == 0 && invokableNode.symbol.type.getReturnType().isNullable()
                && (funcBody.stmts.size() < 1
                || funcBody.stmts.get(funcBody.stmts.size() - 1).getKind() != NodeKind.RETURN)) {
            DiagnosticPos invPos = invokableNode.pos;
            DiagnosticPos returnStmtPos = new DiagnosticPos(invPos.src, invPos.eLine, invPos.eLine, invPos.sCol,
                                                            invPos.sCol);
            BLangReturn returnStmt = ASTBuilderUtil.createNilReturnStmt(returnStmtPos, symTable.nilType);
            funcBody.addStatement(returnStmt);
        }
    }

    /**
     * Reorder the invocation arguments to match the original function signature.
     *
     * @param iExpr Function invocation expressions to reorder the arguments
     */
    private void reorderArguments(BLangInvocation iExpr) {
        BSymbol symbol = iExpr.symbol;

        if (symbol == null || symbol.type.tag != TypeTags.INVOKABLE) {
            return;
        }

        BInvokableSymbol invokableSymbol = (BInvokableSymbol) symbol;
        if (!invokableSymbol.params.isEmpty()) {
            // Re-order the arguments
            reorderNamedArgs(iExpr, invokableSymbol);
        }

        if (invokableSymbol.restParam == null) {
            return;
        }

        // Create an array out of all the rest arguments, and pass it as a single argument.
        // If there is only one optional argument and its type is restArg (i.e: ...x), then
        // leave it as is.
        if (iExpr.restArgs.size() == 1 && iExpr.restArgs.get(0).getKind() == NodeKind.REST_ARGS_EXPR) {
            return;
        }
        BLangArrayLiteral arrayLiteral = (BLangArrayLiteral) TreeBuilder.createArrayLiteralExpressionNode();
        arrayLiteral.exprs = iExpr.restArgs;
        arrayLiteral.type = invokableSymbol.restParam.type;
        iExpr.restArgs = new ArrayList<>();
        iExpr.restArgs.add(arrayLiteral);
    }

    private void reorderNamedArgs(BLangInvocation iExpr, BInvokableSymbol invokableSymbol) {
        List<BLangExpression> args = new ArrayList<>();
        Map<String, BLangExpression> namedArgs = new HashMap<>();
        iExpr.requiredArgs.stream()
                .filter(expr -> expr.getKind() == NodeKind.NAMED_ARGS_EXPR)
                .forEach(expr -> namedArgs.put(((NamedArgNode) expr).getName().value, expr));

        List<BVarSymbol> params = invokableSymbol.params;

        // Iterate over the required args.
        int i = 0;
        for (; i < params.size(); i++) {
            BVarSymbol param = params.get(i);
            if (iExpr.requiredArgs.size() > i && iExpr.requiredArgs.get(i).getKind() != NodeKind.NAMED_ARGS_EXPR) {
                // If a positional arg is given in the same position, it will be used.
                args.add(iExpr.requiredArgs.get(i));
            } else if (namedArgs.containsKey(param.name.value)) {
                // Else check if named arg is given.
                args.add(namedArgs.get(param.name.value));
            } else {
                // Else create a dummy expression with an ignore flag.
                BLangExpression expr = new BLangIgnoreExpr();
                expr.type = param.type;
                args.add(expr);
            }
        }
        iExpr.requiredArgs = args;
    }

    private BLangMatchTypedBindingPatternClause getSafeAssignErrorPattern(
            DiagnosticPos pos, BSymbol invokableSymbol, List<BType> equivalentErrorTypes, boolean isCheckPanicExpr) {
        // From here onwards we assume that this function has only one return type
        // Owner of the variable symbol must be an invokable symbol
        BType enclosingFuncReturnType = ((BInvokableType) invokableSymbol.type).retType;
        Set<BType> returnTypeSet = enclosingFuncReturnType.tag == TypeTags.UNION ?
                ((BUnionType) enclosingFuncReturnType).getMemberTypes() :
                new LinkedHashSet<BType>() {{
                    add(enclosingFuncReturnType);
                }};

        // For each error type, there has to be at least one equivalent return type in the enclosing function
        boolean returnOnError = equivalentErrorTypes.stream()
                .allMatch(errorType -> returnTypeSet.stream()
                        .anyMatch(retType -> types.isAssignable(errorType, retType)));

        // Create the pattern to match the error type
        //      1) Create the pattern variable
        String patternFailureCaseVarName = GEN_VAR_PREFIX.value + "t_failure";
        BLangSimpleVariable patternFailureCaseVar = ASTBuilderUtil.createVariable(pos,
                patternFailureCaseVarName, symTable.errorType, null, new BVarSymbol(0,
                        names.fromString(patternFailureCaseVarName),
                        this.env.scope.owner.pkgID, symTable.errorType, this.env.scope.owner));

        //      2) Create the pattern block
        BLangVariableReference patternFailureCaseVarRef = ASTBuilderUtil.createVariableRef(pos,
                patternFailureCaseVar.symbol);

        BLangBlockStmt patternBlockFailureCase = (BLangBlockStmt) TreeBuilder.createBlockNode();
        patternBlockFailureCase.pos = pos;
        if (!isCheckPanicExpr && returnOnError) {
            //return e;
            BLangReturn returnStmt = (BLangReturn) TreeBuilder.createReturnNode();
            returnStmt.pos = pos;
            returnStmt.expr = patternFailureCaseVarRef;
            patternBlockFailureCase.stmts.add(returnStmt);
        } else {
            // throw e
            BLangPanic panicNode = (BLangPanic) TreeBuilder.createPanicNode();
            panicNode.pos = pos;
            panicNode.expr = patternFailureCaseVarRef;
            patternBlockFailureCase.stmts.add(panicNode);
        }

        return ASTBuilderUtil.createMatchStatementPattern(pos, patternFailureCaseVar, patternBlockFailureCase);
    }

    private BLangMatchTypedBindingPatternClause getSafeAssignSuccessPattern(DiagnosticPos pos, BType lhsType,
            boolean isVarDef, BVarSymbol varSymbol, BLangExpression lhsExpr) {
        //  File _$_f1 => f = _$_f1;
        // 1) Create the pattern variable
        String patternSuccessCaseVarName = GEN_VAR_PREFIX.value + "t_match";
        BLangSimpleVariable patternSuccessCaseVar = ASTBuilderUtil.createVariable(pos,
                patternSuccessCaseVarName, lhsType, null, new BVarSymbol(0,
                        names.fromString(patternSuccessCaseVarName),
                        this.env.scope.owner.pkgID, lhsType, this.env.scope.owner));

        //2) Create the pattern body
        BLangExpression varRefExpr;
        if (isVarDef) {
            varRefExpr = ASTBuilderUtil.createVariableRef(pos, varSymbol);
        } else {
            varRefExpr = lhsExpr;
        }

        BLangVariableReference patternSuccessCaseVarRef = ASTBuilderUtil.createVariableRef(pos,
                patternSuccessCaseVar.symbol);
        BLangAssignment assignmentStmtSuccessCase = ASTBuilderUtil.createAssignmentStmt(pos,
                varRefExpr, patternSuccessCaseVarRef, false);

        BLangBlockStmt patternBlockSuccessCase = ASTBuilderUtil.createBlockStmt(pos,
                new ArrayList<BLangStatement>() {{
                    add(assignmentStmtSuccessCase);
                }});
        return ASTBuilderUtil.createMatchStatementPattern(pos,
                patternSuccessCaseVar, patternBlockSuccessCase);
    }

    private BLangStatement generateIfElseStmt(BLangMatch matchStmt, BLangSimpleVariable matchExprVar) {
        List<BLangMatchBindingPatternClause> patterns = matchStmt.patternClauses;

        BLangIf parentIfNode = generateIfElseStmt(patterns.get(0), matchExprVar);
        BLangIf currentIfNode = parentIfNode;
        for (int i = 1; i < patterns.size(); i++) {
            BLangMatchBindingPatternClause patternClause = patterns.get(i);
            if (i == patterns.size() - 1 && patternClause.isLastPattern) { // This is the last pattern
                currentIfNode.elseStmt = getMatchPatternElseBody(patternClause, matchExprVar);
            } else {
                currentIfNode.elseStmt = generateIfElseStmt(patternClause, matchExprVar);
                currentIfNode = (BLangIf) currentIfNode.elseStmt;
            }
        }

        // TODO handle json and any
        // only one pattern no if just a block
        // last one just a else block..
        // json handle it specially
        //
        return parentIfNode;
    }


    /**
     * Generate an if-else statement from the given match statement.
     *
     * @param pattern match pattern statement node
     * @param matchExprVar  variable node of the match expression
     * @return if else statement node
     */
    private BLangIf generateIfElseStmt(BLangMatchBindingPatternClause pattern, BLangSimpleVariable matchExprVar) {
        BLangExpression ifCondition = createPatternIfCondition(pattern, matchExprVar.symbol);
        if (NodeKind.MATCH_TYPED_PATTERN_CLAUSE == pattern.getKind()) {
            BLangBlockStmt patternBody = getMatchPatternBody(pattern, matchExprVar);
            return ASTBuilderUtil.createIfElseStmt(pattern.pos, ifCondition, patternBody, null);
        }

        // Cast matched expression into matched type.
        BType expectedType = matchExprVar.type;
        if (pattern.getKind() == NodeKind.MATCH_STRUCTURED_PATTERN_CLAUSE) {
            BLangMatchStructuredBindingPatternClause matchPattern = (BLangMatchStructuredBindingPatternClause) pattern;
            expectedType = getStructuredBindingPatternType(matchPattern.bindingPatternVariable);
        }

        if (NodeKind.MATCH_STRUCTURED_PATTERN_CLAUSE == pattern.getKind()) { // structured match patterns
            BLangMatchStructuredBindingPatternClause structuredPattern =
                    (BLangMatchStructuredBindingPatternClause) pattern;
            BLangSimpleVariableDef varDef = forceCastIfApplicable(matchExprVar.symbol, pattern.pos, expectedType);

            // Create a variable reference for _$$_
            BLangSimpleVarRef matchExprVarRef = ASTBuilderUtil.createVariableRef(pattern.pos, varDef.var.symbol);
            structuredPattern.bindingPatternVariable.expr = matchExprVarRef;

            BLangStatement varDefStmt;
            if (NodeKind.TUPLE_VARIABLE == structuredPattern.bindingPatternVariable.getKind()) {
                varDefStmt = ASTBuilderUtil.createTupleVariableDef(pattern.pos,
                        (BLangTupleVariable) structuredPattern.bindingPatternVariable);
            } else if (NodeKind.RECORD_VARIABLE == structuredPattern.bindingPatternVariable.getKind()) {
                varDefStmt = ASTBuilderUtil.createRecordVariableDef(pattern.pos,
                        (BLangRecordVariable) structuredPattern.bindingPatternVariable);
            } else if (NodeKind.ERROR_VARIABLE == structuredPattern.bindingPatternVariable.getKind()) {
                varDefStmt = ASTBuilderUtil.createErrorVariableDef(pattern.pos,
                        (BLangErrorVariable) structuredPattern.bindingPatternVariable);
            } else {
                varDefStmt = ASTBuilderUtil
                        .createVariableDef(pattern.pos, (BLangSimpleVariable) structuredPattern.bindingPatternVariable);
            }

            if (structuredPattern.typeGuardExpr != null) {
                BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(structuredPattern.pos);
                blockStmt.addStatement(varDef);
                blockStmt.addStatement(varDefStmt);
                BLangStatementExpression stmtExpr = ASTBuilderUtil
                        .createStatementExpression(blockStmt, structuredPattern.typeGuardExpr);
                stmtExpr.type = symTable.booleanType;

                ifCondition = ASTBuilderUtil
                        .createBinaryExpr(pattern.pos, ifCondition, stmtExpr, symTable.booleanType, OperatorKind.AND,
                                (BOperatorSymbol) symResolver
                                        .resolveBinaryOperator(OperatorKind.AND, symTable.booleanType,
                                                symTable.booleanType));
            } else {
                structuredPattern.body.stmts.add(0, varDef);
                structuredPattern.body.stmts.add(1, varDefStmt);
            }
        }

        return ASTBuilderUtil.createIfElseStmt(pattern.pos, ifCondition, pattern.body, null);
    }

    private BLangBlockStmt getMatchPatternBody(BLangMatchBindingPatternClause pattern,
                                               BLangSimpleVariable matchExprVar) {

        BLangBlockStmt body;

        BLangMatchTypedBindingPatternClause patternClause = (BLangMatchTypedBindingPatternClause) pattern;
        // Add the variable definition to the body of the pattern` clause
        if (patternClause.variable.name.value.equals(Names.IGNORE.value)) {
            return patternClause.body;
        }

        // create TypeName i = <TypeName> _$$_
        // Create a variable reference for _$$_
        BLangSimpleVarRef matchExprVarRef = ASTBuilderUtil.createVariableRef(patternClause.pos,
                matchExprVar.symbol);
        BLangExpression patternVarExpr = addConversionExprIfRequired(matchExprVarRef, patternClause.variable.type);

        // Add the variable def statement
        BLangSimpleVariable patternVar = ASTBuilderUtil.createVariable(patternClause.pos, "",
                patternClause.variable.type, patternVarExpr, patternClause.variable.symbol);
        BLangSimpleVariableDef patternVarDef = ASTBuilderUtil.createVariableDef(patternVar.pos, patternVar);
        patternClause.body.stmts.add(0, patternVarDef);
        body = patternClause.body;

        return body;
    }

    private BLangBlockStmt getMatchPatternElseBody(BLangMatchBindingPatternClause pattern,
            BLangSimpleVariable matchExprVar) {

        BLangBlockStmt body = pattern.body;

        if (NodeKind.MATCH_STRUCTURED_PATTERN_CLAUSE == pattern.getKind()) { // structured match patterns

            // Create a variable reference for _$$_
            BLangSimpleVarRef matchExprVarRef = ASTBuilderUtil.createVariableRef(pattern.pos, matchExprVar.symbol);

            BLangMatchStructuredBindingPatternClause structuredPattern =
                    (BLangMatchStructuredBindingPatternClause) pattern;

            structuredPattern.bindingPatternVariable.expr = matchExprVarRef;

            BLangStatement varDefStmt;
            if (NodeKind.TUPLE_VARIABLE == structuredPattern.bindingPatternVariable.getKind()) {
                varDefStmt = ASTBuilderUtil.createTupleVariableDef(pattern.pos,
                        (BLangTupleVariable) structuredPattern.bindingPatternVariable);
            } else if (NodeKind.RECORD_VARIABLE == structuredPattern.bindingPatternVariable.getKind()) {
                varDefStmt = ASTBuilderUtil.createRecordVariableDef(pattern.pos,
                        (BLangRecordVariable) structuredPattern.bindingPatternVariable);
            } else if (NodeKind.ERROR_VARIABLE == structuredPattern.bindingPatternVariable.getKind()) {
                varDefStmt = ASTBuilderUtil.createErrorVariableDef(pattern.pos,
                        (BLangErrorVariable) structuredPattern.bindingPatternVariable);
            } else {
                varDefStmt = ASTBuilderUtil
                        .createVariableDef(pattern.pos, (BLangSimpleVariable) structuredPattern.bindingPatternVariable);
            }
            structuredPattern.body.stmts.add(0, varDefStmt);
            body = structuredPattern.body;
        }

        return body;
    }

    BLangExpression addConversionExprIfRequired(BLangExpression expr, BType lhsType) {
        if (lhsType.tag == TypeTags.NONE) {
            return expr;
        }

        BType rhsType = expr.type;
        if (types.isSameType(rhsType, lhsType)) {
            return expr;
        }

        types.setImplicitCastExpr(expr, rhsType, lhsType);
        if (expr.impConversionExpr != null) {
            return expr;
        }

        if (lhsType.tag == TypeTags.JSON && rhsType.tag == TypeTags.NIL) {
            return expr;
        }

        if (lhsType.tag == TypeTags.NIL && rhsType.isNullable()) {
            return expr;
        }

        if (lhsType.tag == TypeTags.ARRAY && rhsType.tag == TypeTags.TUPLE) {
            return expr;
        }

        BOperatorSymbol conversionSymbol;
        if (types.isValueType(lhsType)) {
            conversionSymbol = Symbols.createUnboxValueTypeOpSymbol(rhsType, lhsType);
        } else if (lhsType.tag == TypeTags.UNION && types.isSubTypeOfBaseType(lhsType, TypeTags.ERROR)) {
            conversionSymbol = Symbols.createCastOperatorSymbol(rhsType, symTable.errorType, symTable.errorType, false,
                    true, null, null);
            lhsType = symTable.errorType;
        } else if (lhsType.tag == TypeTags.UNION || rhsType.tag == TypeTags.UNION) {
            conversionSymbol = Symbols.createCastOperatorSymbol(rhsType, lhsType, symTable.errorType, false, true,
                    null, null);
        } else if (lhsType.tag == TypeTags.MAP || rhsType.tag == TypeTags.MAP) {
            conversionSymbol = Symbols.createCastOperatorSymbol(rhsType, lhsType, symTable.errorType, false, true,
                    null, null);
        } else if (lhsType.tag == TypeTags.TABLE || rhsType.tag == TypeTags.TABLE) {
            conversionSymbol = Symbols.createCastOperatorSymbol(rhsType, lhsType, symTable.errorType, false, true,
                    null, null);
        } else {
            conversionSymbol = (BOperatorSymbol) symResolver.resolveCastOperator(expr, rhsType, lhsType);
        }

        // Create a type cast expression
        BLangTypeConversionExpr conversionExpr = (BLangTypeConversionExpr)
                TreeBuilder.createTypeConversionNode();
        conversionExpr.expr = expr;
        conversionExpr.targetType = lhsType;
        conversionExpr.conversionSymbol = conversionSymbol;
        conversionExpr.type = lhsType;
        conversionExpr.pos = expr.pos;
        conversionExpr.checkTypes = false;
        return conversionExpr;
    }

    private BLangExpression createPatternIfCondition(BLangMatchBindingPatternClause patternClause,
                                                     BVarSymbol varSymbol) {
        BType patternType;

        switch (patternClause.getKind()) {
            case MATCH_STATIC_PATTERN_CLAUSE:
                BLangMatchStaticBindingPatternClause staticPattern =
                        (BLangMatchStaticBindingPatternClause) patternClause;
                patternType = staticPattern.literal.type;
                break;
            case MATCH_STRUCTURED_PATTERN_CLAUSE:
                BLangMatchStructuredBindingPatternClause structuredPattern =
                        (BLangMatchStructuredBindingPatternClause) patternClause;
                patternType = getStructuredBindingPatternType(structuredPattern.bindingPatternVariable);
                break;
            default:
                BLangMatchTypedBindingPatternClause simplePattern = (BLangMatchTypedBindingPatternClause) patternClause;
                patternType = simplePattern.variable.type;
                break;
        }

        BLangExpression binaryExpr;
        BType[] memberTypes;
        if (patternType.tag == TypeTags.UNION) {
            BUnionType unionType = (BUnionType) patternType;
            memberTypes = unionType.getMemberTypes().toArray(new BType[0]);
        } else {
            memberTypes = new BType[1];
            memberTypes[0] = patternType;
        }

        if (memberTypes.length == 1) {
            binaryExpr = createPatternMatchBinaryExpr(patternClause, varSymbol, memberTypes[0]);
        } else {
            BLangExpression lhsExpr = createPatternMatchBinaryExpr(patternClause, varSymbol, memberTypes[0]);
            BLangExpression rhsExpr = createPatternMatchBinaryExpr(patternClause, varSymbol, memberTypes[1]);
            binaryExpr = ASTBuilderUtil.createBinaryExpr(patternClause.pos, lhsExpr, rhsExpr,
                    symTable.booleanType, OperatorKind.OR,
                    (BOperatorSymbol) symResolver.resolveBinaryOperator(OperatorKind.OR,
                            lhsExpr.type, rhsExpr.type));
            for (int i = 2; i < memberTypes.length; i++) {
                lhsExpr = createPatternMatchBinaryExpr(patternClause, varSymbol, memberTypes[i]);
                rhsExpr = binaryExpr;
                binaryExpr = ASTBuilderUtil.createBinaryExpr(patternClause.pos, lhsExpr, rhsExpr,
                        symTable.booleanType, OperatorKind.OR,
                        (BOperatorSymbol) symResolver.resolveBinaryOperator(OperatorKind.OR,
                                lhsExpr.type, rhsExpr.type));
            }
        }
        return binaryExpr;
    }

    private BType getStructuredBindingPatternType(BLangVariable bindingPatternVariable) {
        if (NodeKind.TUPLE_VARIABLE == bindingPatternVariable.getKind()) {
            BLangTupleVariable tupleVariable = (BLangTupleVariable) bindingPatternVariable;
            List<BType> memberTypes = new ArrayList<>();
            for (int i = 0; i < tupleVariable.memberVariables.size(); i++) {
                memberTypes.add(getStructuredBindingPatternType(tupleVariable.memberVariables.get(i)));
            }
            BTupleType tupleType = new BTupleType(memberTypes);
            if (tupleVariable.restVariable != null) {
                BArrayType restArrayType = (BArrayType) getStructuredBindingPatternType(tupleVariable.restVariable);
                tupleType.restType = restArrayType.eType;
            }
            return tupleType;
        }

        if (NodeKind.RECORD_VARIABLE == bindingPatternVariable.getKind()) {
            BLangRecordVariable recordVariable = (BLangRecordVariable) bindingPatternVariable;

            BRecordTypeSymbol recordSymbol =
                    Symbols.createRecordSymbol(0, names.fromString("$anonRecordType$" + recordCount++),
                                               env.enclPkg.symbol.pkgID, null, env.scope.owner);
            recordSymbol.initializerFunc = createRecordInitFunc();
            recordSymbol.scope = new Scope(recordSymbol);
            recordSymbol.scope.define(
                    names.fromString(recordSymbol.name.value + "." + recordSymbol.initializerFunc.funcName.value),
                    recordSymbol.initializerFunc.symbol);

            List<BField> fields = new ArrayList<>();
            List<BLangSimpleVariable> typeDefFields = new ArrayList<>();

            for (int i = 0; i < recordVariable.variableList.size(); i++) {
                String fieldNameStr = recordVariable.variableList.get(i).key.value;
                Name fieldName = names.fromString(fieldNameStr);
                BType fieldType = getStructuredBindingPatternType(
                        recordVariable.variableList.get(i).valueBindingPattern);
                BVarSymbol fieldSymbol = new BVarSymbol(Flags.REQUIRED, fieldName,
                        env.enclPkg.symbol.pkgID, fieldType, recordSymbol);

                //TODO check below field position
                fields.add(new BField(fieldName, bindingPatternVariable.pos, fieldSymbol));
                typeDefFields.add(ASTBuilderUtil.createVariable(null, fieldNameStr, fieldType, null, fieldSymbol));
                recordSymbol.scope.define(fieldName, fieldSymbol);
            }

            BRecordType recordVarType = new BRecordType(recordSymbol);
            recordVarType.fields = fields;

            // if rest param is null we treat it as an open record with anydata rest param
            recordVarType.restFieldType = recordVariable.restParam != null ?
                        ((BMapType) ((BLangSimpleVariable) recordVariable.restParam).type).constraint :
                    symTable.anydataType;

            BLangRecordTypeNode recordTypeNode = createRecordTypeNode(typeDefFields, recordVarType);
            recordTypeNode.pos = bindingPatternVariable.pos;
            recordSymbol.type = recordVarType;
            recordVarType.tsymbol = recordSymbol;
            recordTypeNode.symbol = recordSymbol;
            recordTypeNode.initFunction = createInitFunctionForRecordType(recordTypeNode, env);
            recordSymbol.scope.define(recordSymbol.initializerFunc.symbol.name, recordSymbol.initializerFunc.symbol);
            createTypeDefinition(recordVarType, recordSymbol, recordTypeNode);

            return recordVarType;
        }

        if (NodeKind.ERROR_VARIABLE == bindingPatternVariable.getKind()) {
            BLangErrorVariable errorVariable = (BLangErrorVariable) bindingPatternVariable;
            BErrorTypeSymbol errorTypeSymbol = new BErrorTypeSymbol(
                    SymTag.ERROR,
                    Flags.PUBLIC,
                    names.fromString("$anonErrorType$" + errorCount++),
                    env.enclPkg.symbol.pkgID,
                    null, null);
            BType detailType;
            if ((errorVariable.detail == null || errorVariable.detail.isEmpty()) && errorVariable.restDetail != null) {
                detailType = symTable.detailType;
            } else {
                detailType = createDetailType(errorVariable.detail, errorVariable.restDetail, errorCount++);

                BLangRecordTypeNode recordTypeNode = createRecordTypeNode(errorVariable, (BRecordType) detailType);
                createTypeDefinition(detailType, detailType.tsymbol, recordTypeNode);
            }
            BErrorType errorType = new BErrorType(errorTypeSymbol,
                    ((BErrorType) errorVariable.type).reasonType,
                    detailType);
            errorTypeSymbol.type = errorType;

            createTypeDefinition(errorType, errorTypeSymbol, createErrorTypeNode(errorType));
            return errorType;
        }

        return bindingPatternVariable.type;
    }

    private BLangRecordTypeNode createRecordTypeNode(BLangErrorVariable errorVariable, BRecordType detailType) {
        List<BLangSimpleVariable> fieldList = new ArrayList<>();
        for (BLangErrorVariable.BLangErrorDetailEntry field : errorVariable.detail) {
            BVarSymbol symbol = field.valueBindingPattern.symbol;
            if (symbol == null) {
                symbol = new BVarSymbol(
                        Flags.PUBLIC,
                        names.fromString(field.key.value + "$"),
                        this.env.enclPkg.packageID,
                        symTable.pureType,
                        null);
            }
            BLangSimpleVariable fieldVar = ASTBuilderUtil.createVariable(
                    field.valueBindingPattern.pos,
                    symbol.name.value,
                    field.valueBindingPattern.type,
                    field.valueBindingPattern.expr,
                    symbol);
            fieldList.add(fieldVar);
        }
        return createRecordTypeNode(fieldList, detailType);
    }

    private BType createDetailType(List<BLangErrorVariable.BLangErrorDetailEntry> detail,
                                   BLangSimpleVariable restDetail, int errorNo) {
        BRecordTypeSymbol detailRecordTypeSymbol = new BRecordTypeSymbol(
                SymTag.RECORD,
                Flags.PUBLIC,
                names.fromString("$anonErrorType$" + errorNo + "$detailType"),
                env.enclPkg.symbol.pkgID, null, null);

        detailRecordTypeSymbol.initializerFunc = createRecordInitFunc();
        detailRecordTypeSymbol.scope = new Scope(detailRecordTypeSymbol);
        detailRecordTypeSymbol.scope.define(
                names.fromString(detailRecordTypeSymbol.name.value + "." +
                        detailRecordTypeSymbol.initializerFunc.funcName.value),
                detailRecordTypeSymbol.initializerFunc.symbol);

        BRecordType detailRecordType = new BRecordType(detailRecordTypeSymbol);
        detailRecordType.restFieldType = symTable.anydataType;

        if (restDetail == null) {
            detailRecordType.sealed = true;
        }

        for (BLangErrorVariable.BLangErrorDetailEntry detailEntry : detail) {
            Name fieldName = names.fromIdNode(detailEntry.key);
            BType fieldType = getStructuredBindingPatternType(detailEntry.valueBindingPattern);
            BVarSymbol fieldSym = new BVarSymbol(
                        Flags.PUBLIC, fieldName, detailRecordTypeSymbol.pkgID, fieldType, detailRecordTypeSymbol);
            detailRecordType.fields.add(new BField(fieldName, detailEntry.key.pos, fieldSym));
            detailRecordTypeSymbol.scope.define(fieldName, fieldSym);
        }

        return detailRecordType;
    }

    private BAttachedFunction createRecordInitFunc() {
        BInvokableType bInvokableType = new BInvokableType(new ArrayList<>(), symTable.nilType, null);
        BInvokableSymbol initFuncSymbol = Symbols.createFunctionSymbol(
                Flags.PUBLIC, Names.EMPTY, env.enclPkg.symbol.pkgID, bInvokableType, env.scope.owner, false);
        initFuncSymbol.retType = symTable.nilType;
        return new BAttachedFunction(Names.INIT_FUNCTION_SUFFIX, initFuncSymbol, bInvokableType);
    }

    private BLangRecordTypeNode createRecordTypeNode(List<BLangSimpleVariable> typeDefFields,
            BRecordType recordVarType) {
        BLangRecordTypeNode recordTypeNode = (BLangRecordTypeNode) TreeBuilder.createRecordTypeNode();
        recordTypeNode.type = recordVarType;
        recordTypeNode.fields = typeDefFields;
        return recordTypeNode;
    }

    private BLangErrorType createErrorTypeNode(BErrorType errorType) {
        BLangErrorType errorTypeNode = (BLangErrorType) TreeBuilder.createErrorTypeNode();
        errorTypeNode.type = errorType;
        return errorTypeNode;
    }

    private void createTypeDefinition(BType type, BTypeSymbol symbol, BLangType typeNode) {
        BLangTypeDefinition typeDefinition = (BLangTypeDefinition) TreeBuilder.createTypeDefinition();
        env.enclPkg.addTypeDefinition(typeDefinition);
        typeDefinition.typeNode = typeNode;
        typeDefinition.type = type;
        typeDefinition.symbol = symbol;
    }

    private BLangExpression createPatternMatchBinaryExpr(BLangMatchBindingPatternClause patternClause,
                                                         BVarSymbol varSymbol, BType patternType) {
        DiagnosticPos pos = patternClause.pos;

        BLangSimpleVarRef varRef = ASTBuilderUtil.createVariableRef(pos, varSymbol);

        if (NodeKind.MATCH_STATIC_PATTERN_CLAUSE == patternClause.getKind()) {
            BLangMatchStaticBindingPatternClause pattern = (BLangMatchStaticBindingPatternClause) patternClause;
            return createBinaryExpression(pos, varRef, pattern.literal);
        }

        if (NodeKind.MATCH_STRUCTURED_PATTERN_CLAUSE == patternClause.getKind()) {
            return createIsLikeExpression(pos, ASTBuilderUtil.createVariableRef(pos, varSymbol), patternType);
        }

        if (patternType == symTable.nilType) {
            BLangLiteral bLangLiteral = ASTBuilderUtil.createLiteral(pos, symTable.nilType, null);
            return ASTBuilderUtil.createBinaryExpr(pos, varRef, bLangLiteral, symTable.booleanType,
                    OperatorKind.EQUAL, (BOperatorSymbol) symResolver.resolveBinaryOperator(OperatorKind.EQUAL,
                            symTable.anyType, symTable.nilType));
        } else {
            return createIsAssignableExpression(pos, varSymbol, patternType);
        }
    }

    private BLangExpression createBinaryExpression(DiagnosticPos pos, BLangSimpleVarRef varRef,
            BLangExpression expression) {

        BLangBinaryExpr binaryExpr;
        if (NodeKind.GROUP_EXPR == expression.getKind()) {
            return createBinaryExpression(pos, varRef, ((BLangGroupExpr) expression).expression);
        }

        if (NodeKind.BINARY_EXPR == expression.getKind()) {
            binaryExpr = (BLangBinaryExpr) expression;
            BLangExpression lhsExpr = createBinaryExpression(pos, varRef, binaryExpr.lhsExpr);
            BLangExpression rhsExpr = createBinaryExpression(pos, varRef, binaryExpr.rhsExpr);

            binaryExpr = ASTBuilderUtil.createBinaryExpr(pos, lhsExpr, rhsExpr, symTable.booleanType, OperatorKind.OR,
                    (BOperatorSymbol) symResolver
                            .resolveBinaryOperator(OperatorKind.OR, symTable.booleanType, symTable.booleanType));
        } else if (expression.getKind() == NodeKind.SIMPLE_VARIABLE_REF
                && ((BLangSimpleVarRef) expression).variableName.value.equals(IGNORE.value)) {
            BLangValueType anyType = (BLangValueType) TreeBuilder.createValueTypeNode();
            anyType.type = symTable.anyType;
            anyType.typeKind = TypeKind.ANY;
            return ASTBuilderUtil.createTypeTestExpr(pos, varRef, anyType);
        } else {
            binaryExpr = ASTBuilderUtil
                    .createBinaryExpr(pos, varRef, expression, symTable.booleanType, OperatorKind.EQUAL, null);

            BSymbol opSymbol = symResolver.resolveBinaryOperator(OperatorKind.EQUAL, varRef.type, expression.type);
            if (opSymbol == symTable.notFoundSymbol) {
                opSymbol = symResolver
                        .getBinaryEqualityForTypeSets(OperatorKind.EQUAL, symTable.anydataType, expression.type,
                                binaryExpr);
            }
            binaryExpr.opSymbol = (BOperatorSymbol) opSymbol;
        }
        return binaryExpr;
    }

    private BLangIsAssignableExpr createIsAssignableExpression(DiagnosticPos pos,
                                                               BVarSymbol varSymbol,
                                                               BType patternType) {
        //  _$$_ isassignable patternType
        // Create a variable reference for _$$_
        BLangSimpleVarRef varRef = ASTBuilderUtil.createVariableRef(pos, varSymbol);

        // Binary operator for equality
        return ASTBuilderUtil.createIsAssignableExpr(pos, varRef, patternType, symTable.booleanType, names);
    }

    private BLangIsLikeExpr createIsLikeExpression(DiagnosticPos pos, BLangExpression expr, BType type) {
        return ASTBuilderUtil.createIsLikeExpr(pos, expr, ASTBuilderUtil.createTypeNode(type), symTable.booleanType);
    }

    private BLangAssignment createAssignmentStmt(BLangSimpleVariable variable) {
        BLangSimpleVarRef varRef = (BLangSimpleVarRef) TreeBuilder.createSimpleVariableReferenceNode();
        varRef.pos = variable.pos;
        varRef.variableName = variable.name;
        varRef.symbol = variable.symbol;
        varRef.type = variable.type;

        BLangAssignment assignmentStmt = (BLangAssignment) TreeBuilder.createAssignmentNode();
        assignmentStmt.expr = variable.expr;
        assignmentStmt.pos = variable.pos;
        assignmentStmt.setVariable(varRef);
        return assignmentStmt;
    }

    private BLangAssignment createStructFieldUpdate(BLangFunction function, BLangSimpleVariable variable) {
        BLangSimpleVarRef selfVarRef = ASTBuilderUtil.createVariableRef(variable.pos, function.receiver.symbol);
        BLangFieldBasedAccess fieldAccess = ASTBuilderUtil.createFieldAccessExpr(selfVarRef, variable.name);
        fieldAccess.symbol = variable.symbol;
        fieldAccess.type = variable.type;

        BLangAssignment assignmentStmt = (BLangAssignment) TreeBuilder.createAssignmentNode();
        assignmentStmt.expr = variable.expr;
        assignmentStmt.pos = variable.pos;
        assignmentStmt.setVariable(fieldAccess);

        SymbolEnv initFuncEnv = SymbolEnv.createFunctionEnv(function, function.symbol.scope, env);
        return rewrite(assignmentStmt, initFuncEnv);
    }

    private void addMatchExprDefaultCase(BLangMatchExpression bLangMatchExpression) {
        List<BType> exprTypes;
        List<BType> unmatchedTypes = new ArrayList<>();

        if (bLangMatchExpression.expr.type.tag == TypeTags.UNION) {
            BUnionType unionType = (BUnionType) bLangMatchExpression.expr.type;
            exprTypes = new ArrayList<>(unionType.getMemberTypes());
        } else {
            exprTypes = Lists.of(bLangMatchExpression.type);
        }

        // find the types that do not match to any of the patterns.
        for (BType type : exprTypes) {
            boolean assignable = false;
            for (BLangMatchExprPatternClause pattern : bLangMatchExpression.patternClauses) {
                if (this.types.isAssignable(type, pattern.variable.type)) {
                    assignable = true;
                    break;
                }
            }

            if (!assignable) {
                unmatchedTypes.add(type);
            }
        }

        if (unmatchedTypes.isEmpty()) {
            return;
        }

        BType defaultPatternType;
        if (unmatchedTypes.size() == 1) {
            defaultPatternType = unmatchedTypes.get(0);
        } else {
            defaultPatternType = BUnionType.create(null, new LinkedHashSet<>(unmatchedTypes));
        }

        String patternCaseVarName = GEN_VAR_PREFIX.value + "t_match_default";
        BLangSimpleVariable patternMatchCaseVar = ASTBuilderUtil.createVariable(bLangMatchExpression.pos,
                patternCaseVarName, defaultPatternType, null, new BVarSymbol(0, names.fromString(patternCaseVarName),
                        this.env.scope.owner.pkgID, defaultPatternType, this.env.scope.owner));

        BLangMatchExprPatternClause defaultPattern =
                (BLangMatchExprPatternClause) TreeBuilder.createMatchExpressionPattern();
        defaultPattern.variable = patternMatchCaseVar;
        defaultPattern.expr = ASTBuilderUtil.createVariableRef(bLangMatchExpression.pos, patternMatchCaseVar.symbol);
        defaultPattern.pos = bLangMatchExpression.pos;
        bLangMatchExpression.patternClauses.add(defaultPattern);
    }

    private boolean safeNavigate(BLangAccessExpression accessExpr) {
        if (accessExpr.lhsVar || accessExpr.expr == null) {
            return false;
        }

        if (accessExpr.errorSafeNavigation || accessExpr.nilSafeNavigation) {
            return true;
        }

        NodeKind kind = accessExpr.expr.getKind();
        if (kind == NodeKind.FIELD_BASED_ACCESS_EXPR ||
                kind == NodeKind.INDEX_BASED_ACCESS_EXPR) {
            return safeNavigate((BLangAccessExpression) accessExpr.expr);
        }

        return false;
    }

    private BLangExpression rewriteSafeNavigationExpr(BLangAccessExpression accessExpr) {
        BType originalExprType = accessExpr.type;
        // Create a temp variable to hold the intermediate result of the acces expression.
        String matchTempResultVarName = GEN_VAR_PREFIX.value + "temp_result";
        BLangSimpleVariable tempResultVar = ASTBuilderUtil.createVariable(accessExpr.pos, matchTempResultVarName,
                accessExpr.type, null, new BVarSymbol(0, names.fromString(matchTempResultVarName),
                        this.env.scope.owner.pkgID, accessExpr.type, this.env.scope.owner));
        BLangSimpleVariableDef tempResultVarDef = ASTBuilderUtil.createVariableDef(accessExpr.pos, tempResultVar);
        BLangVariableReference tempResultVarRef =
                ASTBuilderUtil.createVariableRef(accessExpr.pos, tempResultVar.symbol);

        // Create a chain of match statements
        handleSafeNavigation(accessExpr, accessExpr.type, tempResultVar);

        // Create a statement-expression including the match statement
        BLangMatch matcEXpr = this.matchStmtStack.firstElement();
        BLangBlockStmt blockStmt =
                ASTBuilderUtil.createBlockStmt(accessExpr.pos, Lists.of(tempResultVarDef, matcEXpr));
        BLangStatementExpression stmtExpression = ASTBuilderUtil.createStatementExpression(blockStmt, tempResultVarRef);
        stmtExpression.type = originalExprType;

        // Reset the variables
        this.matchStmtStack = new Stack<>();
        this.accessExprStack = new Stack<>();
        this.successPattern = null;
        this.safeNavigationAssignment = null;
        return stmtExpression;
    }

    private void handleSafeNavigation(BLangAccessExpression accessExpr, BType type, BLangSimpleVariable tempResultVar) {
        if (accessExpr.expr == null) {
            return;
        }

        // If the parent of current expr is the root, terminate
        NodeKind kind = accessExpr.expr.getKind();
        if (kind == NodeKind.FIELD_BASED_ACCESS_EXPR ||
                kind == NodeKind.INDEX_BASED_ACCESS_EXPR ||
                kind == NodeKind.INVOCATION) {
            handleSafeNavigation((BLangAccessExpression) accessExpr.expr, type, tempResultVar);
        }

        if (!accessExpr.errorSafeNavigation && !accessExpr.nilSafeNavigation) {
            accessExpr.type = accessExpr.originalType;
            if (this.safeNavigationAssignment != null) {
                this.safeNavigationAssignment.expr = addConversionExprIfRequired(accessExpr, tempResultVar.type);
            }
            return;
        }

        /*
         * If the field access is a safe navigation, create a match expression.
         * Then chain the current expression as the success-pattern of the parent
         * match expr, if available.
         * eg:
         * x but {              <--- parent match expr
         *   error e => e,
         *   T t => t.y but {   <--- current expr
         *      error e => e,
         *      R r => r.z
         *   }
         * }
         */

        BLangMatch matchStmt = ASTBuilderUtil.createMatchStatement(accessExpr.pos, accessExpr.expr, new ArrayList<>());

        // Add pattern to lift nil
        if (accessExpr.nilSafeNavigation) {
            matchStmt.patternClauses.add(getMatchNullPattern(accessExpr, tempResultVar));
            matchStmt.type = type;
        }

        // Add pattern to lift error, only if the safe navigation is used
        if (accessExpr.errorSafeNavigation) {
            matchStmt.patternClauses.add(getMatchErrorPattern(accessExpr, tempResultVar));
            matchStmt.type = type;
            matchStmt.pos = accessExpr.pos;

        }

        // Create the pattern for success scenario. i.e: not null and not error (if applicable).
        BLangMatchTypedBindingPatternClause successPattern =
                getSuccessPattern(accessExpr, tempResultVar, accessExpr.errorSafeNavigation);
        matchStmt.patternClauses.add(successPattern);
        this.matchStmtStack.push(matchStmt);
        if (this.successPattern != null) {
            this.successPattern.body = ASTBuilderUtil.createBlockStmt(accessExpr.pos, Lists.of(matchStmt));
        }
        this.successPattern = successPattern;
    }

    private BLangMatchTypedBindingPatternClause getMatchErrorPattern(BLangExpression expr,
                                                                         BLangSimpleVariable tempResultVar) {
        String errorPatternVarName = GEN_VAR_PREFIX.value + "t_match_error";
        BLangSimpleVariable errorPatternVar = ASTBuilderUtil.createVariable(expr.pos, errorPatternVarName,
                symTable.errorType, null, new BVarSymbol(0, names.fromString(errorPatternVarName),
                        this.env.scope.owner.pkgID, symTable.errorType, this.env.scope.owner));

        // Create assignment to temp result
        BLangSimpleVarRef assignmentRhsExpr = ASTBuilderUtil.createVariableRef(expr.pos, errorPatternVar.symbol);
        BLangVariableReference tempResultVarRef = ASTBuilderUtil.createVariableRef(expr.pos, tempResultVar.symbol);
        BLangAssignment assignmentStmt =
                ASTBuilderUtil.createAssignmentStmt(expr.pos, tempResultVarRef, assignmentRhsExpr, false);
        BLangBlockStmt patternBody = ASTBuilderUtil.createBlockStmt(expr.pos, Lists.of(assignmentStmt));

        // Create the pattern
        // R b => a = b;
        BLangMatchTypedBindingPatternClause errorPattern = ASTBuilderUtil
                .createMatchStatementPattern(expr.pos, errorPatternVar, patternBody);
        return errorPattern;
    }

    private BLangMatchExprPatternClause getMatchNullPatternGivenExpression(DiagnosticPos pos,
                                                                           BLangExpression expr) {
        String nullPatternVarName = IGNORE.toString();
        BLangSimpleVariable errorPatternVar = ASTBuilderUtil.createVariable(pos, nullPatternVarName, symTable.nilType,
                null, new BVarSymbol(0, names.fromString(nullPatternVarName),
                        this.env.scope.owner.pkgID, symTable.nilType, this.env.scope.owner));

        BLangMatchExprPatternClause nullPattern =
                (BLangMatchExprPatternClause) TreeBuilder.createMatchExpressionPattern();
        nullPattern.variable = errorPatternVar;
        nullPattern.expr = expr;
        nullPattern.pos = pos;
        return nullPattern;
    }

    private BLangMatchTypedBindingPatternClause getMatchNullPattern(BLangExpression expr,
            BLangSimpleVariable tempResultVar) {
        // TODO: optimize following by replacing var with underscore, and assigning null literal
        String nullPatternVarName = GEN_VAR_PREFIX.value + "t_match_null";
        BLangSimpleVariable nullPatternVar = ASTBuilderUtil.createVariable(expr.pos, nullPatternVarName,
                symTable.nilType, null, new BVarSymbol(0, names.fromString(nullPatternVarName),
                        this.env.scope.owner.pkgID, symTable.nilType, this.env.scope.owner));

        // Create assignment to temp result
        BLangSimpleVarRef assignmentRhsExpr = ASTBuilderUtil.createVariableRef(expr.pos, nullPatternVar.symbol);
        BLangVariableReference tempResultVarRef = ASTBuilderUtil.createVariableRef(expr.pos, tempResultVar.symbol);
        BLangAssignment assignmentStmt =
                ASTBuilderUtil.createAssignmentStmt(expr.pos, tempResultVarRef, assignmentRhsExpr, false);
        BLangBlockStmt patternBody = ASTBuilderUtil.createBlockStmt(expr.pos, Lists.of(assignmentStmt));

        // Create the pattern
        // R b => a = b;
        BLangMatchTypedBindingPatternClause nullPattern = ASTBuilderUtil
                .createMatchStatementPattern(expr.pos, nullPatternVar, patternBody);
        return nullPattern;
    }

    private BLangMatchTypedBindingPatternClause getSuccessPattern(BLangAccessExpression accessExpr,
            BLangSimpleVariable tempResultVar, boolean liftError) {
        BType type = types.getSafeType(accessExpr.expr.type, true, liftError);
        String successPatternVarName = GEN_VAR_PREFIX.value + "t_match_success";

        BVarSymbol  successPatternSymbol;
        if (type.tag == TypeTags.INVOKABLE) {
            successPatternSymbol = new BInvokableSymbol(SymTag.VARIABLE, 0, names.fromString(successPatternVarName),
                    this.env.scope.owner.pkgID, type, this.env.scope.owner);
        } else {
            successPatternSymbol = new BVarSymbol(0, names.fromString(successPatternVarName),
                    this.env.scope.owner.pkgID, type, this.env.scope.owner);
        }

        BLangSimpleVariable successPatternVar = ASTBuilderUtil.createVariable(accessExpr.pos, successPatternVarName,
                type, null, successPatternSymbol);

        // Create x.foo, by replacing the varRef expr of the current expression, with the new temp var ref
        accessExpr.expr = ASTBuilderUtil.createVariableRef(accessExpr.pos, successPatternVar.symbol);
        accessExpr.errorSafeNavigation = false;
        accessExpr.nilSafeNavigation = false;

        // Type of the field access expression should be always taken from the child type.
        // Because the type assigned to expression contains the inherited error/nil types,
        // and may not reflect the actual type of the child/field expr.
        accessExpr.type = accessExpr.originalType;

        BLangVariableReference tempResultVarRef =
                ASTBuilderUtil.createVariableRef(accessExpr.pos, tempResultVar.symbol);

        BLangExpression assignmentRhsExpr = addConversionExprIfRequired(accessExpr, tempResultVarRef.type);
        BLangAssignment assignmentStmt =
                ASTBuilderUtil.createAssignmentStmt(accessExpr.pos, tempResultVarRef, assignmentRhsExpr, false);
        BLangBlockStmt patternBody = ASTBuilderUtil.createBlockStmt(accessExpr.pos, Lists.of(assignmentStmt));

        // Create the pattern
        // R b => a = x.foo;
        BLangMatchTypedBindingPatternClause successPattern =
                ASTBuilderUtil.createMatchStatementPattern(accessExpr.pos, successPatternVar, patternBody);
        this.safeNavigationAssignment = assignmentStmt;
        return successPattern;
    }

    private boolean safeNavigateLHS(BLangExpression expr) {
        if (expr.getKind() != NodeKind.FIELD_BASED_ACCESS_EXPR && expr.getKind() != NodeKind.INDEX_BASED_ACCESS_EXPR) {
            return false;
        }

        BLangExpression varRef = ((BLangAccessExpression) expr).expr;
        if (varRef.type.isNullable()) {
            return true;
        }

        return safeNavigateLHS(varRef);
    }

    private BLangStatement rewriteSafeNavigationAssignment(BLangAccessExpression accessExpr, BLangExpression rhsExpr,
                                                           boolean safeAssignment) {
        // --- original code ---
        // A? a = ();
        // a.b = 4;
        // --- desugared code ---
        // A? a = ();
        // if(a is ()) {
        //    panic error("NullReferenceException");
        // }
        // (<A> a).b = 4;
        // This will get chained and will get added more if cases as required,
        // For invocation exprs, this will create a temp var to store that, so it won't get executed
        // multiple times.
        this.accessExprStack = new Stack<>();
        List<BLangStatement> stmts = new ArrayList<>();
        createLHSSafeNavigation(stmts, accessExpr.expr);
        BLangAssignment assignment = ASTBuilderUtil.createAssignmentStmt(accessExpr.pos,
                cloneExpression(accessExpr), rhsExpr);
        stmts.add(assignment);
        return ASTBuilderUtil.createBlockStmt(accessExpr.pos, stmts);
    }

    private void createLHSSafeNavigation(List<BLangStatement> stmts, BLangExpression expr) {
        NodeKind kind = expr.getKind();
        boolean root = false;
        if (kind == NodeKind.FIELD_BASED_ACCESS_EXPR || kind == NodeKind.INDEX_BASED_ACCESS_EXPR ||
                kind == NodeKind.INVOCATION) {
            BLangAccessExpression accessExpr = (BLangAccessExpression) expr;
            createLHSSafeNavigation(stmts, accessExpr.expr);
            accessExpr.expr = accessExprStack.pop();
        } else {
            root = true;
        }

        // If expression is an invocation, then create a temp var to store the invocation value, so that
        // invocation will happen only one time
        if (expr.getKind() == NodeKind.INVOCATION) {
            BLangInvocation invocation = (BLangInvocation) expr;
            BVarSymbol interMediateSymbol = new BVarSymbol(0, names.fromString(GEN_VAR_PREFIX.value
                    + "i_intermediate"), this.env.scope.owner.pkgID, invocation.type, this.env.scope.owner);
            BLangSimpleVariable intermediateVariable = ASTBuilderUtil.createVariable(expr.pos,
                    interMediateSymbol.name.value, invocation.type, invocation, interMediateSymbol);
            BLangSimpleVariableDef intermediateVariableDefinition = ASTBuilderUtil.createVariableDef(invocation.pos,
                    intermediateVariable);
            stmts.add(intermediateVariableDefinition);

            expr = ASTBuilderUtil.createVariableRef(invocation.pos, interMediateSymbol);
        }

        if (expr.type.isNullable()) {
            BLangTypeTestExpr isNillTest = ASTBuilderUtil.createTypeTestExpr(expr.pos, expr, getNillTypeNode());
            isNillTest.type = symTable.booleanType;

            BLangBlockStmt thenStmt = ASTBuilderUtil.createBlockStmt(expr.pos);

            //Cloning the expression and set the nil lifted type.
            expr = cloneExpression(expr);
            expr.type = types.getSafeType(expr.type, true, false);

            if (isDefaultableMappingType(expr.type) && !root) { // TODO for records, type should be defaultable as well
                // This will properly get desugered later to a json literal
                BLangRecordLiteral jsonLiteral = (BLangRecordLiteral) TreeBuilder.createRecordLiteralNode();
                jsonLiteral.type = expr.type;
                jsonLiteral.pos = expr.pos;
                BLangAssignment assignment = ASTBuilderUtil.createAssignmentStmt(expr.pos,
                        expr, jsonLiteral);
                thenStmt.addStatement(assignment);
            } else {
                BLangLiteral literal = (BLangLiteral) TreeBuilder.createLiteralExpression();
                literal.value = ERROR_REASON_NULL_REFERENCE_ERROR;
                literal.type = symTable.stringType;

                BLangInvocation errorCtorInvocation = (BLangInvocation) TreeBuilder.createInvocationNode();
                errorCtorInvocation.pos = expr.pos;
                errorCtorInvocation.argExprs.add(literal);
                errorCtorInvocation.requiredArgs.add(literal);
                errorCtorInvocation.type = symTable.errorType;
                errorCtorInvocation.symbol = symTable.errorConstructor;

                BLangPanic panicNode = (BLangPanic) TreeBuilder.createPanicNode();
                panicNode.expr = errorCtorInvocation;
                panicNode.pos = expr.pos;
                thenStmt.addStatement(panicNode);
            }

            BLangIf ifelse = ASTBuilderUtil.createIfElseStmt(expr.pos, isNillTest, thenStmt, null);
            stmts.add(ifelse);
        }

        accessExprStack.push(expr);
    }

    private BLangValueType getNillTypeNode() {
        BLangValueType nillTypeNode = (BLangValueType) TreeBuilder.createValueTypeNode();
        nillTypeNode.typeKind = TypeKind.NIL;
        nillTypeNode.type = symTable.nilType;
        return nillTypeNode;
    }

    private BLangVariableReference cloneExpression(BLangExpression expr) {
        switch (expr.getKind()) {
            case SIMPLE_VARIABLE_REF:
                return ASTBuilderUtil.createVariableRef(expr.pos, ((BLangSimpleVarRef) expr).symbol);
            case FIELD_BASED_ACCESS_EXPR:
            case INDEX_BASED_ACCESS_EXPR:
            case INVOCATION:
                return cloneAccessExpr((BLangAccessExpression) expr);
            default:
                throw new IllegalStateException();
        }
    }

    private BLangAccessExpression cloneAccessExpr(BLangAccessExpression originalAccessExpr) {
        if (originalAccessExpr.expr == null) {
            return originalAccessExpr;
        }

        BLangVariableReference varRef;
        NodeKind kind = originalAccessExpr.expr.getKind();
        if (kind == NodeKind.FIELD_BASED_ACCESS_EXPR || kind == NodeKind.INDEX_BASED_ACCESS_EXPR ||
                kind == NodeKind.INVOCATION) {
            varRef = cloneAccessExpr((BLangAccessExpression) originalAccessExpr.expr);
        } else {
            varRef = cloneExpression(originalAccessExpr.expr);
        }
        varRef.type = types.getSafeType(originalAccessExpr.expr.type, true, false);

        BLangAccessExpression accessExpr;
        switch (originalAccessExpr.getKind()) {
            case FIELD_BASED_ACCESS_EXPR:
                accessExpr = ASTBuilderUtil.createFieldAccessExpr(varRef,
                        ((BLangFieldBasedAccess) originalAccessExpr).field);
                break;
            case INDEX_BASED_ACCESS_EXPR:
                accessExpr = ASTBuilderUtil.createIndexAccessExpr(varRef,
                        ((BLangIndexBasedAccess) originalAccessExpr).indexExpr);
                break;
            case INVOCATION:
                // TODO
                accessExpr = null;
                break;
            default:
                throw new IllegalStateException();
        }

        accessExpr.originalType = originalAccessExpr.originalType;
        accessExpr.pos = originalAccessExpr.pos;
        accessExpr.lhsVar = originalAccessExpr.lhsVar;
        accessExpr.symbol = originalAccessExpr.symbol;
        accessExpr.errorSafeNavigation = false;
        accessExpr.nilSafeNavigation = false;

        // Type of the field access expression should be always taken from the child type.
        // Because the type assigned to expression contains the inherited error/nil types,
        // and may not reflect the actual type of the child/field expr.
        accessExpr.type = originalAccessExpr.originalType;
        return accessExpr;
    }

    private BLangBinaryExpr getModifiedIntRangeStartExpr(BLangExpression expr) {
        BLangLiteral constOneLiteral = ASTBuilderUtil.createLiteral(expr.pos, symTable.intType, 1L);
        return ASTBuilderUtil.createBinaryExpr(expr.pos, expr, constOneLiteral, symTable.intType, OperatorKind.ADD,
                (BOperatorSymbol) symResolver.resolveBinaryOperator(OperatorKind.ADD,
                        symTable.intType,
                        symTable.intType));
    }

    private BLangBinaryExpr getModifiedIntRangeEndExpr(BLangExpression expr) {
        BLangLiteral constOneLiteral = ASTBuilderUtil.createLiteral(expr.pos, symTable.intType, 1L);
        return ASTBuilderUtil.createBinaryExpr(expr.pos, expr, constOneLiteral, symTable.intType, OperatorKind.SUB,
                (BOperatorSymbol) symResolver.resolveBinaryOperator(OperatorKind.SUB,
                        symTable.intType,
                        symTable.intType));
    }

    private BLangExpression getDefaultValueExpr(BLangAccessExpression accessExpr) {
        BType fieldType = accessExpr.originalType;
        BType type = types.getSafeType(accessExpr.expr.type, true, false);
        switch (type.tag) {
            case TypeTags.JSON:
                if (accessExpr.getKind() == NodeKind.INDEX_BASED_ACCESS_EXPR &&
                        ((BLangIndexBasedAccess) accessExpr).indexExpr.type.tag == TypeTags.INT) {
                    return new BLangJSONArrayLiteral(new ArrayList<>(), new BArrayType(fieldType));
                }
                return new BLangJSONLiteral(accessExpr.pos, new ArrayList<>(), fieldType);
            case TypeTags.MAP:
                return new BLangMapLiteral(accessExpr.pos, new ArrayList<>(), type);
            case TypeTags.RECORD:
                return new BLangRecordLiteral(accessExpr.pos, type);
            default:
                throw new IllegalStateException();
        }
    }

    private BLangExpression getDefaultValueLiteral(DefaultValueLiteral defaultValue, int paramTypeTag) {
        if (defaultValue == null || defaultValue.getValue() == null) {
            return getNullLiteral();
        }
        Object value = defaultValue.getValue();
        int literalTypeTag = defaultValue.getLiteralTypeTag();

        if (value instanceof Long) {
            switch (paramTypeTag) {
                case TypeTags.FLOAT:
                    return getFloatLiteral(((Long) value).doubleValue());
                case TypeTags.DECIMAL:
                    return getDecimalLiteral(String.valueOf(value));
                default:
                    return getIntLiteral((Long) value);
            }
        }
        if (value instanceof String) {
            switch (paramTypeTag) {
                case TypeTags.FLOAT:
                    return getFloatLiteral(Double.parseDouble((String) value));
                case TypeTags.DECIMAL:
                    return getDecimalLiteral(String.valueOf(value));
                case TypeTags.FINITE:
                case TypeTags.UNION:
                    if (literalTypeTag == TypeTags.FLOAT) {
                        return getFloatLiteral(Double.parseDouble((String) value));
                    }
                    return getStringLiteral((String) value);
                default:
                    return getStringLiteral((String) value);
            }
        }
        if (value instanceof Boolean) {
            return getBooleanLiteral((Boolean) value);
        }
        throw new IllegalStateException("Unsupported default value type " + paramTypeTag);
    }

    private BLangExpression getDefaultValue(int paramTypeTag) {
        switch (paramTypeTag) {
            case TypeTags.STRING:
                return getStringLiteral("");
            case TypeTags.BOOLEAN:
                return getBooleanLiteral(false);
            case TypeTags.FLOAT:
                return getFloatLiteral(0.0);
            case TypeTags.BYTE:
            case TypeTags.INT:
                return getIntLiteral(0);
            case TypeTags.DECIMAL:
                return getDecimalLiteral("0.0");
            case TypeTags.FINITE:
            case TypeTags.RECORD:
            case TypeTags.OBJECT:
            case TypeTags.UNION:
            default:
                return getNullLiteral();
        }
    }

    private BLangLiteral getStringLiteral(String value) {
        BLangLiteral literal = (BLangLiteral) TreeBuilder.createLiteralExpression();
        literal.value = value;
        literal.type = symTable.stringType;
        return literal;
    }

    private BLangLiteral getIntLiteral(long value) {
        BLangLiteral literal = (BLangLiteral) TreeBuilder.createLiteralExpression();
        literal.value = value;
        literal.type = symTable.intType;
        return literal;
    }

    private BLangLiteral getFloatLiteral(double value) {
        BLangLiteral literal = (BLangLiteral) TreeBuilder.createLiteralExpression();
        literal.value = value;
        literal.type = symTable.floatType;
        return literal;
    }

    private BLangLiteral getDecimalLiteral(String value) {
        BLangLiteral literal = (BLangLiteral) TreeBuilder.createLiteralExpression();
        literal.value = value;
        literal.type = symTable.decimalType;
        return literal;
    }

    private BLangLiteral getBooleanLiteral(boolean value) {
        BLangLiteral literal = (BLangLiteral) TreeBuilder.createLiteralExpression();
        literal.value = value;
        literal.type = symTable.booleanType;
        return literal;
    }

    private BLangLiteral getNullLiteral() {
        BLangLiteral literal = (BLangLiteral) TreeBuilder.createLiteralExpression();
        literal.type = symTable.nilType;
        return literal;
    }

    private boolean isDefaultableMappingType(BType type) {
        switch (types.getSafeType(type, true, false).tag) {
            case TypeTags.JSON:
            case TypeTags.MAP:
            case TypeTags.RECORD:
                return true;
            default:
                return false;
        }
    }

    private BLangFunction createInitFunctionForStructureType(BLangStructureTypeNode structureTypeNode, SymbolEnv env,
                                                             Name suffix) {
        String structTypeName = structureTypeNode.type.tsymbol.name.value;
        BLangFunction initFunction = ASTBuilderUtil
                .createInitFunctionWithNilReturn(structureTypeNode.pos, structTypeName, suffix);

        // Create the receiver and add receiver details to the node
        initFunction.receiver = ASTBuilderUtil.createReceiver(structureTypeNode.pos, structureTypeNode.type);
        BVarSymbol receiverSymbol = new BVarSymbol(Flags.asMask(EnumSet.noneOf(Flag.class)),
                                                   names.fromIdNode(initFunction.receiver.name),
                                                   env.enclPkg.symbol.pkgID, structureTypeNode.type, null);
        initFunction.receiver.symbol = receiverSymbol;
        initFunction.attachedFunction = true;
        initFunction.flagSet.add(Flag.ATTACHED);

        // Create the function type
        initFunction.type = new BInvokableType(new ArrayList<>(), symTable.nilType, null);

        // Create the function symbol
        Name funcSymbolName = names.fromString(Symbols.getAttachedFuncSymbolName(structTypeName, suffix.value));
        initFunction.symbol = Symbols
                .createFunctionSymbol(Flags.asMask(initFunction.flagSet), funcSymbolName, env.enclPkg.symbol.pkgID,
                                      initFunction.type, structureTypeNode.symbol, initFunction.funcBody != null);
        initFunction.symbol.scope = new Scope(initFunction.symbol);
        initFunction.symbol.scope.define(receiverSymbol.name, receiverSymbol);
        initFunction.symbol.receiverSymbol = receiverSymbol;
        initFunction.name = ASTBuilderUtil.createIdentifier(structureTypeNode.pos, funcSymbolName.value);

        // Create the function type symbol
        BInvokableTypeSymbol tsymbol = Symbols.createInvokableTypeSymbol(SymTag.FUNCTION_TYPE,
                                                                         initFunction.symbol.flags,
                                                                         env.enclPkg.packageID, initFunction.type,
                                                                         initFunction.symbol);
        tsymbol.params = initFunction.symbol.params;
        tsymbol.restParam = initFunction.symbol.restParam;
        tsymbol.returnType = initFunction.symbol.retType;
        initFunction.type.tsymbol = tsymbol;

        receiverSymbol.owner = initFunction.symbol;

        // Add return type as nil to the symbol
        initFunction.symbol.retType = symTable.nilType;

        // Set the taint information to the constructed init function
        initFunction.symbol.taintTable = new HashMap<>();
        TaintRecord taintRecord = new TaintRecord(TaintRecord.TaintedStatus.UNTAINTED, new ArrayList<>());
        initFunction.symbol.taintTable.put(TaintAnalyzer.ALL_UNTAINTED_TABLE_ENTRY_INDEX, taintRecord);

        return initFunction;
    }

    private BLangFunction createInitFunctionForObjectType(BLangObjectTypeNode structureTypeNode, SymbolEnv env) {
        BLangFunction initFunction = createInitFunctionForStructureType(structureTypeNode, env,
                Names.GENERATED_INIT_SUFFIX);
        BObjectTypeSymbol typeSymbol = ((BObjectTypeSymbol) structureTypeNode.type.tsymbol);
        typeSymbol.generatedInitializerFunc = new BAttachedFunction(Names.GENERATED_INIT_SUFFIX, initFunction.symbol,
                (BInvokableType) initFunction.type);
        structureTypeNode.generatedInitFunction = initFunction;
        return rewrite(initFunction, env);
    }

    private BLangFunction createInitFunctionForRecordType(BLangRecordTypeNode recordTypeNode, SymbolEnv env) {
        BLangFunction initFunction = createInitFunctionForStructureType(recordTypeNode, env,
                                                                        Names.INIT_FUNCTION_SUFFIX);
        BRecordTypeSymbol typeSymbol = ((BRecordTypeSymbol) recordTypeNode.type.tsymbol);
        typeSymbol.initializerFunc = new BAttachedFunction(initFunction.symbol.name, initFunction.symbol,
                                                           (BInvokableType) initFunction.type);
        recordTypeNode.initFunction = initFunction;
        return rewrite(initFunction, env);
    }

    private void visitBinaryLogicalExpr(BLangBinaryExpr binaryExpr) {
        /*
         * Desugar (lhsExpr && rhsExpr) to following if-else:
         * 
         * logical AND:
         * -------------
         * T $result$;
         * if (lhsExpr) {
         *    $result$ = rhsExpr;
         * } else {
         *    $result$ = false;
         * }
         * 
         * logical OR:
         * -------------
         * T $result$;
         * if (lhsExpr) {
         *    $result$ = true;
         * } else {
         *    $result$ = rhsExpr;
         * }
         * 
         */
        BLangSimpleVariableDef resultVarDef = createVarDef("$result$", binaryExpr.type, null, binaryExpr.pos);
        BLangBlockStmt thenBody = ASTBuilderUtil.createBlockStmt(binaryExpr.pos);
        BLangBlockStmt elseBody = ASTBuilderUtil.createBlockStmt(binaryExpr.pos);

        // Create then assignment
        BLangSimpleVarRef thenResultVarRef = ASTBuilderUtil.createVariableRef(binaryExpr.pos, resultVarDef.var.symbol);
        BLangExpression thenResult;
        if (binaryExpr.opKind == OperatorKind.AND) {
            thenResult = binaryExpr.rhsExpr;
        } else {
            thenResult = getBooleanLiteral(true);
        }
        BLangAssignment thenAssignment =
                ASTBuilderUtil.createAssignmentStmt(binaryExpr.pos, thenResultVarRef, thenResult);
        thenBody.addStatement(thenAssignment);

        // Create else assignment
        BLangExpression elseResult;
        BLangSimpleVarRef elseResultVarRef = ASTBuilderUtil.createVariableRef(binaryExpr.pos, resultVarDef.var.symbol);
        if (binaryExpr.opKind == OperatorKind.AND) {
            elseResult = getBooleanLiteral(false);
        } else {
            elseResult = binaryExpr.rhsExpr;
        }
        BLangAssignment elseAssignment =
                ASTBuilderUtil.createAssignmentStmt(binaryExpr.pos, elseResultVarRef, elseResult);
        elseBody.addStatement(elseAssignment);

        // Then make it a expression-statement, with expression being the $result$
        BLangSimpleVarRef resultVarRef = ASTBuilderUtil.createVariableRef(binaryExpr.pos, resultVarDef.var.symbol);
        BLangIf ifElse = ASTBuilderUtil.createIfElseStmt(binaryExpr.pos, binaryExpr.lhsExpr, thenBody, elseBody);

        BLangBlockStmt blockStmt = ASTBuilderUtil.createBlockStmt(binaryExpr.pos, Lists.of(resultVarDef, ifElse));
        BLangStatementExpression stmtExpr = ASTBuilderUtil.createStatementExpression(blockStmt, resultVarRef);
        stmtExpr.type = binaryExpr.type;

        result = rewriteExpr(stmtExpr);
    }

    /**
     * Split packahe init function into several smaller functions.
     *
     * @param packageNode package node
     * @param env symbol environment
     * @return initial init function but trimmed in size
     */
    private BLangFunction splitInitFunction(BLangPackage packageNode, SymbolEnv env) {
        int methodSize = INIT_METHOD_SPLIT_SIZE;
        BLangBlockFunctionBody funcBody = (BLangBlockFunctionBody) packageNode.initFunction.funcBody;
        if (funcBody.stmts.size() < methodSize || !isJvmTarget) {
            return packageNode.initFunction;
        }
        BLangFunction initFunction = packageNode.initFunction;

        List<BLangFunction> generatedFunctions = new ArrayList<>();
        List<BLangStatement> stmts = new ArrayList<>(funcBody.stmts);
        funcBody.stmts.clear();
        BLangFunction newFunc = initFunction;
        BLangBlockFunctionBody newFuncBody = (BLangBlockFunctionBody) newFunc.funcBody;

        // until we get to a varDef, stmts are independent, divide it based on methodSize
        int varDefIndex = 0;
        for (int i = 0; i < stmts.size(); i++) {
            if (stmts.get(i).getKind() == NodeKind.VARIABLE_DEF) {
                break;
            }
            varDefIndex++;
            if (i > 0 && i % methodSize == 0) {
                generatedFunctions.add(newFunc);
                newFunc = createIntermediateInitFunction(packageNode, env);
                symTable.rootScope.define(names.fromIdNode(newFunc.name) , newFunc.symbol);
            }

            newFuncBody.stmts.add(stmts.get(i));
        }

        // from a varDef to a service constructor, those stmts should be within single method
        List<BLangStatement> chunkStmts = new ArrayList<>();
        for (int i = varDefIndex; i < stmts.size(); i++) {
            BLangStatement stmt = stmts.get(i);
            chunkStmts.add(stmt);
            varDefIndex++;
            if ((stmt.getKind() == NodeKind.ASSIGNMENT) &&
                    (((BLangAssignment) stmt).expr.getKind() == NodeKind.SERVICE_CONSTRUCTOR) &&
                    (newFuncBody.stmts.size() + chunkStmts.size() > methodSize)) {
                // enf of current chunk
                if (newFuncBody.stmts.size() + chunkStmts.size() > methodSize) {
                    generatedFunctions.add(newFunc);
                    newFunc = createIntermediateInitFunction(packageNode, env);
                    symTable.rootScope.define(names.fromIdNode(newFunc.name), newFunc.symbol);
                }
                newFuncBody.stmts.addAll(chunkStmts);
                chunkStmts.clear();
            } else if ((stmt.getKind() == NodeKind.ASSIGNMENT) &&
                    (((BLangAssignment) stmt).varRef instanceof BLangPackageVarRef) &&
                    Symbols.isFlagOn(((BLangPackageVarRef) ((BLangAssignment) stmt).varRef).varSymbol.flags,
                            Flags.LISTENER)
            ) {
                // this is where listener registrations starts, they are independent stmts
                break;
            }
        }
        newFuncBody.stmts.addAll(chunkStmts);

        // rest of the statements can be split without chunks
        for (int i = varDefIndex; i < stmts.size(); i++) {
            if (i > 0 && i % methodSize == 0) {
                generatedFunctions.add(newFunc);
                newFunc = createIntermediateInitFunction(packageNode, env);
                newFuncBody = (BLangBlockFunctionBody) newFunc.funcBody;
                symTable.rootScope.define(names.fromIdNode(newFunc.name), newFunc.symbol);
            }
            newFuncBody.stmts.add(stmts.get(i));
        }

        generatedFunctions.add(newFunc);

        for (int j = 0; j < generatedFunctions.size() - 1; j++) {
            BLangFunction thisFunction = generatedFunctions.get(j);

            BLangCheckedExpr checkedExpr =
                    ASTBuilderUtil.createCheckExpr(initFunction.pos,
                                                   createInvocationNode(generatedFunctions.get(j + 1).name.value,
                                                                        new ArrayList<>(), symTable.errorOrNilType),
                                                   symTable.nilType);
            checkedExpr.equivalentErrorTypeList.add(symTable.errorType);

            BLangExpressionStmt expressionStmt = ASTBuilderUtil
                    .createExpressionStmt(thisFunction.pos, (BLangBlockFunctionBody) thisFunction.funcBody);
            expressionStmt.expr = checkedExpr;
            expressionStmt.expr.pos = initFunction.pos;

            if (j > 0) { // skip init func
                thisFunction = rewrite(thisFunction, env);
                packageNode.functions.add(thisFunction);
                packageNode.topLevelNodes.add(thisFunction);
            }
        }

        if (generatedFunctions.size() > 1) {
            // add last func
            BLangFunction lastFunc = generatedFunctions.get(generatedFunctions.size() - 1);
            lastFunc = rewrite(lastFunc, env);
            packageNode.functions.add(lastFunc);
            packageNode.topLevelNodes.add(lastFunc);
        }

        return generatedFunctions.get(0);
    }

    /**
     * Create an intermediate package init function.
     *
     * @param pkgNode package node
     * @param env     symbol environment of package
     */
    private BLangFunction createIntermediateInitFunction(BLangPackage pkgNode, SymbolEnv env) {
        String alias = pkgNode.symbol.pkgID.toString();
        BLangFunction initFunction = ASTBuilderUtil
                .createInitFunctionWithErrorOrNilReturn(pkgNode.pos, alias,
                                                        new Name(Names.INIT_FUNCTION_SUFFIX.value
                                                                + this.initFuncIndex++), symTable);
        // Create invokable symbol for init function
        createInvokableSymbol(initFunction, env);
        return initFunction;
    }

    private BType getRestType(BInvokableSymbol invokableSymbol) {
        if (invokableSymbol != null && invokableSymbol.restParam != null) {
            return invokableSymbol.restParam.type;
        }
        return null;
    }

    private BType getRestType(BLangFunction function) {
        if (function != null && function.restParam != null) {
            return function.restParam.type;
        }
        return null;
    }

    private BVarSymbol getRestSymbol(BLangFunction function) {
        if (function != null && function.restParam != null) {
            return function.restParam.symbol;
        }
        return null;
    }

    private boolean isComputedKey(RecordLiteralNode.RecordField field) {
        if (!field.isKeyValueField()) {
            return false;
        }
        return ((BLangRecordLiteral.BLangRecordKeyValueField) field).key.computedKey;
    }

    private void rewriteVarRefFields(BLangRecordLiteral recordLiteral) {
        List<RecordLiteralNode.RecordField> fields =  recordLiteral.fields;
        List<RecordLiteralNode.RecordField> updatedFields = new ArrayList<>(fields.size());

        for (RecordLiteralNode.RecordField field : fields) {
            if (field.isKeyValueField()) {
                updatedFields.add(field);
                continue;
            }

            BLangRecordLiteral.BLangRecordVarNameField varNameField =
                    (BLangRecordLiteral.BLangRecordVarNameField) field;
            updatedFields.add(ASTBuilderUtil.createBLangRecordKeyValue(varNameField, varNameField));
        }
        recordLiteral.fields = updatedFields;
    }
}
