/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerinalang.compiler.parser.test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerinalang.compiler.internal.parser.BallerinaParser;
import io.ballerinalang.compiler.internal.parser.ParserFactory;
import io.ballerinalang.compiler.internal.parser.ParserRuleContext;
import io.ballerinalang.compiler.internal.parser.tree.STIdentifier;
import io.ballerinalang.compiler.internal.parser.tree.STLiteralValueToken;
import io.ballerinalang.compiler.internal.parser.tree.STMissingToken;
import io.ballerinalang.compiler.internal.parser.tree.STNode;
import io.ballerinalang.compiler.internal.parser.tree.STToken;
import io.ballerinalang.compiler.internal.parser.tree.STTypeToken;
import io.ballerinalang.compiler.internal.parser.tree.SyntaxTrivia;
import io.ballerinalang.compiler.syntax.BLModules;
import io.ballerinalang.compiler.syntax.tree.SyntaxKind;
import io.ballerinalang.compiler.syntax.tree.SyntaxTree;
import io.ballerinalang.compiler.text.TextDocument;
import io.ballerinalang.compiler.text.TextDocuments;
import org.testng.Assert;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerinalang.compiler.parser.test.ParserTestConstants.CHILDREN_FIELD;
import static io.ballerinalang.compiler.parser.test.ParserTestConstants.IS_MISSING_FIELD;
import static io.ballerinalang.compiler.parser.test.ParserTestConstants.KIND_FIELD;
import static io.ballerinalang.compiler.parser.test.ParserTestConstants.LEADING_TRIVIA;
import static io.ballerinalang.compiler.parser.test.ParserTestConstants.TRAILING_TRIVIA;
import static io.ballerinalang.compiler.parser.test.ParserTestConstants.VALUE_FIELD;

/**
 * Convenient methods for testing the parser.
 * 
 * @since 1.2.0
 */
public class ParserTestUtils {

    private static final Path RESOURCE_DIRECTORY = Paths.get("src/test/resources/");

    /**
     * Test parsing a valid source.
     * 
     * @param sourceFilePath Path to the ballerina file
     * @param context Context to start parsing the given source
     * @param assertFilePath File to assert the resulting tree after parsing
     */
    public static void test(Path sourceFilePath, ParserRuleContext context, Path assertFilePath) {
        String content = getSourceText(sourceFilePath);
        test(content, context, assertFilePath);
    }

    /**
     * Test parsing a valid source.
     * 
     * @param source Input source that represent a ballerina code
     * @param context Context to start parsing the given source
     * @param assertFilePath File to assert the resulting tree after parsing
     */
    public static void test(String source, ParserRuleContext context, Path assertFilePath) {
        // Parse the source
        BallerinaParser parser = ParserFactory.getParser(source);
        STNode syntaxTree = parser.parse(context);

        // Read the assertion file
        JsonObject assertJson = readAssertFile(RESOURCE_DIRECTORY.resolve(assertFilePath));

        // Validate the tree against the assertion file
        assertNode(syntaxTree, assertJson);
    }

    /**
     * Returns Ballerina source code in the given file as a {@code String}.
     *
     * @param sourceFilePath Path to the ballerina file
     * @return source code as a {@code String}
     */
    public static String getSourceText(Path sourceFilePath) {
        try {
            return new String(Files.readAllBytes(RESOURCE_DIRECTORY.resolve(sourceFilePath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a {@code SyntaxTree} after parsing the give source code path.
     *
     * @param sourceFilePath Path to the ballerina file
     */
    public static SyntaxTree parseFile(Path sourceFilePath) {
        String text = getSourceText(sourceFilePath);
        TextDocument textDocument = TextDocuments.from(text);
        return BLModules.parse(textDocument);
    }

    private static JsonObject readAssertFile(Path filePath) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(new FileReader(filePath.toFile()), JsonObject.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertNode(STNode node, JsonObject json) {
        aseertNodeKind(json, node);

        if (isMissingToken(json)) {
            Assert.assertTrue(node instanceof STMissingToken,
                    "'" + node.toString().trim() + "' expected to be a STMissingToken, but found '" + node.kind + "'.");
            return;
        }

        // If the expected token is not a missing node, then validate it's content
        Assert.assertFalse(node instanceof STMissingToken, "Expected:" + json + ", but found: " + node);
        if (isTerminalNode(node.kind)) {
            assertTerminalNode(json, node);
        } else {
            assertNonTerminalNode(json, CHILDREN_FIELD, node);
        }
    }

    private static boolean isMissingToken(JsonObject json) {
        JsonElement isMissing = json.get(IS_MISSING_FIELD);
        return isMissing != null && isMissing.getAsBoolean();
    }

    private static void aseertNodeKind(JsonObject json, STNode node) {
        SyntaxKind expectedNodeKind = getNodeKind(json.get(KIND_FIELD).getAsString());
        SyntaxKind actualNodeKind = node.kind;
        Assert.assertEquals(actualNodeKind, expectedNodeKind, "error at node [" + node.toString() + "].");
    }

    private static void assertTerminalNode(JsonObject json, STNode node) {
        // If this is a terminal node, it has to be a STToken (i.e: lexeme)
        if (isTrivia(node.kind)) {
            Assert.assertTrue(node instanceof SyntaxTrivia);
        } else {
            Assert.assertTrue(node instanceof STToken);
        }

        // Validate the token text, if this is not a syntax token.
        // e.g: identifiers, basic-literals, etc.
        if (!isSyntaxToken(node.kind)) {
            String expectedText;
            if (node.kind == SyntaxKind.END_OF_LINE_TRIVIA) {
                expectedText = System.lineSeparator();
            } else {
                expectedText = json.get(VALUE_FIELD).getAsString();
            }
            String actualText = getTokenText(node);
            Assert.assertEquals(actualText, expectedText);
        }

        if (!ParserTestUtils.isTrivia(node.kind)) {
            validateTrivia(json, (STToken) node);
        }
    }

    private static void validateTrivia(JsonObject json, STToken token) {
        if (json.has(LEADING_TRIVIA)) {
            assertNonTerminalNode(json, LEADING_TRIVIA, token.leadingTrivia);
        }

        if (json.has(TRAILING_TRIVIA)) {
            assertNonTerminalNode(json, TRAILING_TRIVIA, token.trailingTrivia);
        }
    }

    private static void assertNonTerminalNode(JsonObject json, String keyInJson, STNode tree) {
        JsonArray children = json.getAsJsonArray(keyInJson);
        int size = children.size();
        int j = 0;

        Assert.assertEquals(getNonEmptyChildCount(tree), size, "mismatching child count for '" + tree.toString() + "'");

        for (int i = 0; i < size; i++) {
            // Skip the optional fields that are not present and get the next
            // available node.
            STNode nextChild = tree.childInBucket(j++);
            while (nextChild.kind == SyntaxKind.NONE) {
                nextChild = tree.childInBucket(j++);
            }

            // Assert the actual child node against the expected child node.
            assertNode(nextChild, (JsonObject) children.get(i));
        }
    }

    private static int getNonEmptyChildCount(STNode tree) {
        int count = 0;
        for (int i = 0; i < tree.bucketCount(); i++) {
            STNode nextChild = tree.childInBucket(i);
            if (nextChild.kind == SyntaxKind.NONE) {
                continue;
            }
            count++;
        }

        return count;
    }

    public static boolean isTerminalNode(SyntaxKind syntaxKind) {
        return SyntaxKind.IMPORT_DECLARATION.compareTo(syntaxKind) > 0 || syntaxKind == SyntaxKind.EOF_TOKEN;
    }

    public static boolean isSyntaxToken(SyntaxKind syntaxKind) {
        return SyntaxKind.IDENTIFIER_TOKEN.compareTo(syntaxKind) > 0 || syntaxKind == SyntaxKind.EOF_TOKEN;
    }

    public static boolean isTrivia(SyntaxKind syntaxKind) {
        switch (syntaxKind) {
            case WHITESPACE_TRIVIA:
            case END_OF_LINE_TRIVIA:
            case COMMENT:
            case INVALID:
                return true;
            default:
                return false;
        }
    }

    public static String getTokenText(STNode token) {
        switch (token.kind) {
            case IDENTIFIER_TOKEN:
                return ((STIdentifier) token).text;
            case STRING_LITERAL:
                String val = ((STLiteralValueToken) token).text;
                int stringLen = val.length();
                int lastCharPosition = val.endsWith("\"") ? stringLen - 1 : stringLen;
                return val.substring(1, lastCharPosition);
            case DECIMAL_INTEGER_LITERAL:
            case HEX_INTEGER_LITERAL:
                return ((STLiteralValueToken) token).text;
            case SIMPLE_TYPE:
                return ((STTypeToken) token).text;
            case WHITESPACE_TRIVIA:
            case END_OF_LINE_TRIVIA:
            case COMMENT:
            case INVALID:
                return ((SyntaxTrivia) token).text;
            default:
                return token.kind.toString();

        }
    }

    private static SyntaxKind getNodeKind(String kind) {
        switch (kind) {
            case "MODULE_PART":
                return SyntaxKind.MODULE_PART;
            case "TYPE_DEFINITION":
                return SyntaxKind.TYPE_DEFINITION;
            case "FUNCTION_DEFINITION":
                return SyntaxKind.FUNCTION_DEFINITION;
            case "IMPORT_DECLARATION":
                return SyntaxKind.IMPORT_DECLARATION;
            case "SERVICE_DECLARATION":
                return SyntaxKind.SERVICE_DECLARATION;
            case "LISTENER_DECLARATION":
                return SyntaxKind.LISTENER_DECLARATION;
            case "CONST_DECLARATION":
                return SyntaxKind.CONST_DECLARATION;

            // Keywords
            case "PUBLIC_KEYWORD":
                return SyntaxKind.PUBLIC_KEYWORD;
            case "PRIVATE_KEYWORD":
                return SyntaxKind.PRIVATE_KEYWORD;
            case "FUNCTION_KEYWORD":
                return SyntaxKind.FUNCTION_KEYWORD;
            case "TYPE_KEYWORD":
                return SyntaxKind.TYPE_KEYWORD;
            case "EXTERNAL_KEYWORD":
                return SyntaxKind.EXTERNAL_KEYWORD;
            case "RETURNS_KEYWORD":
                return SyntaxKind.RETURNS_KEYWORD;
            case "RECORD_KEYWORD":
                return SyntaxKind.RECORD_KEYWORD;
            case "OBJECT_KEYWORD":
                return SyntaxKind.OBJECT_KEYWORD;
            case "REMOTE_KEYWORD":
                return SyntaxKind.REMOTE_KEYWORD;
            case "CLIENT_KEYWORD":
                return SyntaxKind.CLIENT_KEYWORD;
            case "ABSTRACT_KEYWORD":
                return SyntaxKind.ABSTRACT_KEYWORD;
            case "IF_KEYWORD":
                return SyntaxKind.IF_KEYWORD;
            case "ELSE_KEYWORD":
                return SyntaxKind.ELSE_KEYWORD;
            case "WHILE_KEYWORD":
                return SyntaxKind.WHILE_KEYWORD;
            case "TRUE_KEYWORD":
                return SyntaxKind.TRUE_KEYWORD;
            case "FALSE_KEYWORD":
                return SyntaxKind.FALSE_KEYWORD;
            case "CHECK_KEYWORD":
                return SyntaxKind.CHECK_KEYWORD;
            case "CHECKPANIC_KEYWORD":
                return SyntaxKind.CHECKPANIC_KEYWORD;
            case "PANIC_KEYWORD":
                return SyntaxKind.PANIC_KEYWORD;
            case "IMPORT_KEYWORD":
                return SyntaxKind.IMPORT_KEYWORD;
            case "VERSION_KEYWORD":
                return SyntaxKind.VERSION_KEYWORD;
            case "AS_KEYWORD":
                return SyntaxKind.AS_KEYWORD;
            case "CONTINUE_KEYWORD":
                return SyntaxKind.CONTINUE_KEYWORD;
            case "BREAK_KEYWORD":
                return SyntaxKind.BREAK_KEYWORD;
            case "RETURN_KEYWORD":
                return SyntaxKind.RETURN_KEYWORD;
            case "SERVICE_KEYWORD":
                return SyntaxKind.SERVICE_KEYWORD;
            case "ON_KEYWORD":
                return SyntaxKind.ON_KEYWORD;
            case "RESOURCE_KEYWORD":
                return SyntaxKind.RESOURCE_KEYWORD;
            case "LISTENER_KEYWORD":
                return SyntaxKind.LISTENER_KEYWORD;
            case "CONST_KEYWORD":
                return SyntaxKind.CONST_KEYWORD;
            case "FINAL_KEYWORD":
                return SyntaxKind.FINAL_KEYWORD;
            case "TYPEOF_KEYWORD":
                return SyntaxKind.TYPEOF_KEYWORD;

            // Operators
            case "PLUS_TOKEN":
                return SyntaxKind.PLUS_TOKEN;
            case "MINUS_TOKEN":
                return SyntaxKind.MINUS_TOKEN;
            case "ASTERISK_TOKEN":
                return SyntaxKind.ASTERISK_TOKEN;
            case "SLASH_TOKEN":
                return SyntaxKind.SLASH_TOKEN;
            case "LT_TOKEN":
                return SyntaxKind.LT_TOKEN;
            case "EQUAL_TOKEN":
                return SyntaxKind.EQUAL_TOKEN;
            case "DOUBLE_EQUAL_TOKEN":
                return SyntaxKind.DOUBLE_EQUAL_TOKEN;
            case "TRIPPLE_EQUAL_TOKEN":
                return SyntaxKind.TRIPPLE_EQUAL_TOKEN;
            case "PERCENT_TOKEN":
                return SyntaxKind.PERCENT_TOKEN;
            case "GT_TOKEN":
                return SyntaxKind.GT_TOKEN;
            case "EQUAL_GT_TOKEN":
                return SyntaxKind.EQUAL_GT_TOKEN;
            case "QUESTION_MARK_TOKEN":
                return SyntaxKind.QUESTION_MARK_TOKEN;
            case "LT_EQUAL_TOKEN":
                return SyntaxKind.LT_EQUAL_TOKEN;
            case "GT_EQUAL_TOKEN":
                return SyntaxKind.GT_EQUAL_TOKEN;
            case "EXCLAMATION_MARK_TOKEN":
                return SyntaxKind.EXCLAMATION_MARK_TOKEN;
            case "NOT_EQUAL_TOKEN":
                return SyntaxKind.NOT_EQUAL_TOKEN;
            case "NOT_DOUBLE_EQUAL_TOKEN":
                return SyntaxKind.NOT_DOUBLE_EQUAL_TOKEN;
            case "BITWISE_AND_TOKEN":
                return SyntaxKind.BITWISE_AND_TOKEN;
            case "BITWISE_XOR_TOKEN":
                return SyntaxKind.BITWISE_XOR_TOKEN;
            case "LOGICAL_AND_TOKEN":
                return SyntaxKind.LOGICAL_AND_TOKEN;
            case "LOGICAL_OR_TOKEN":
                return SyntaxKind.LOGICAL_OR_TOKEN;
            case "NEGATION_TOKEN":
                return SyntaxKind.NEGATION_TOKEN;

            // Separators
            case "OPEN_BRACE_TOKEN":
                return SyntaxKind.OPEN_BRACE_TOKEN;
            case "CLOSE_BRACE_TOKEN":
                return SyntaxKind.CLOSE_BRACE_TOKEN;
            case "OPEN_PAREN_TOKEN":
                return SyntaxKind.OPEN_PAREN_TOKEN;
            case "CLOSE_PAREN_TOKEN":
                return SyntaxKind.CLOSE_PAREN_TOKEN;
            case "OPEN_BRACKET_TOKEN":
                return SyntaxKind.OPEN_BRACKET_TOKEN;
            case "CLOSE_BRACKET_TOKEN":
                return SyntaxKind.CLOSE_BRACKET_TOKEN;
            case "SEMICOLON_TOKEN":
                return SyntaxKind.SEMICOLON_TOKEN;
            case "DOT_TOKEN":
                return SyntaxKind.DOT_TOKEN;
            case "COLON_TOKEN":
                return SyntaxKind.COLON_TOKEN;
            case "COMMA_TOKEN":
                return SyntaxKind.COMMA_TOKEN;
            case "ELLIPSIS_TOKEN":
                return SyntaxKind.ELLIPSIS_TOKEN;
            case "OPEN_BRACE_PIPE_TOKEN":
                return SyntaxKind.OPEN_BRACE_PIPE_TOKEN;
            case "CLOSE_BRACE_PIPE_TOKEN":
                return SyntaxKind.CLOSE_BRACE_PIPE_TOKEN;
            case "PIPE_TOKEN":
                return SyntaxKind.PIPE_TOKEN;

            // Expressions
            case "IDENTIFIER_TOKEN":
                return SyntaxKind.IDENTIFIER_TOKEN;
            case "BRACED_EXPRESSION":
                return SyntaxKind.BRACED_EXPRESSION;
            case "BINARY_EXPRESSION":
                return SyntaxKind.BINARY_EXPRESSION;
            case "STRING_LITERAL":
                return SyntaxKind.STRING_LITERAL;
            case "DECIMAL_INTEGER_LITERAL":
                return SyntaxKind.DECIMAL_INTEGER_LITERAL;
            case "HEX_INTEGER_LITERAL":
                return SyntaxKind.HEX_INTEGER_LITERAL;
            case "DECIMAL_FLOATING_POINT_LITERAL":
                return SyntaxKind.DECIMAL_FLOATING_POINT_LITERAL;
            case "HEX_FLOATING_POINT_LITERAL":
                return SyntaxKind.HEX_FLOATING_POINT_LITERAL;
            case "FUNCTION_CALL":
                return SyntaxKind.FUNCTION_CALL;
            case "POSITIONAL_ARG":
                return SyntaxKind.POSITIONAL_ARG;
            case "NAMED_ARG":
                return SyntaxKind.NAMED_ARG;
            case "REST_ARG":
                return SyntaxKind.REST_ARG;
            case "QUALIFIED_IDENTIFIER":
                return SyntaxKind.QUALIFIED_IDENTIFIER;
            case "FIELD_ACCESS":
                return SyntaxKind.FIELD_ACCESS;
            case "METHOD_CALL":
                return SyntaxKind.METHOD_CALL;
            case "MEMBER_ACCESS":
                return SyntaxKind.MEMBER_ACCESS;
            case "CHECK_EXPRESSION":
                return SyntaxKind.CHECK_EXPRESSION;
            case "MAPPING_CONSTRUCTOR":
                return SyntaxKind.MAPPING_CONSTRUCTOR;
            case "TYPEOF_EXPRESSION":
                return SyntaxKind.TYPEOF_EXPRESSION;
            case "UNARY_EXPRESSION":
                return SyntaxKind.UNARY_EXPRESSION;

            // Statements
            case "BLOCK_STATEMENT":
                return SyntaxKind.BLOCK_STATEMENT;
            case "VARIABLE_DECL":
                return SyntaxKind.VARIABLE_DECL;
            case "ASSIGNMENT_STATEMENT":
                return SyntaxKind.ASSIGNMENT_STATEMENT;
            case "IF_ELSE_STATEMENT":
                return SyntaxKind.IF_ELSE_STATEMENT;
            case "ELSE_BLOCK":
                return SyntaxKind.ELSE_BLOCK;
            case "WHILE_STATEMENT":
                return SyntaxKind.WHILE_STATEMENT;
            case "CALL_STATEMENT":
                return SyntaxKind.CALL_STATEMENT;
            case "PANIC_STATEMENT":
                return SyntaxKind.PANIC_STATEMENT;
            case "CONTINUE_STATEMENT":
                return SyntaxKind.CONTINUE_STATEMENT;
            case "BREAK_STATEMENT":
                return SyntaxKind.BREAK_STATEMENT;
            case "RETURN_STATEMENT":
                return SyntaxKind.RETURN_STATEMENT;
            case "COMPOUND_ASSIGNMENT_STATEMENT":
                return SyntaxKind.COMPOUND_ASSIGNMENT_STATEMENT;

            // Others
            case "SIMPLE_TYPE":
                return SyntaxKind.SIMPLE_TYPE;
            case "LIST":
                return SyntaxKind.LIST;
            case "RETURN_TYPE_DESCRIPTOR":
                return SyntaxKind.RETURN_TYPE_DESCRIPTOR;
            case "EXTERNAL_FUNCTION_BODY":
                return SyntaxKind.EXTERNAL_FUNCTION_BODY;
            case "PARAMETER":
                return SyntaxKind.PARAMETER;
            case "RECORD_TYPE_DESCRIPTOR":
                return SyntaxKind.RECORD_TYPE_DESCRIPTOR;
            case "RECORD_FIELD":
                return SyntaxKind.RECORD_FIELD;
            case "RECORD_FIELD_WITH_DEFAULT_VALUE":
                return SyntaxKind.RECORD_FIELD_WITH_DEFAULT_VALUE;
            case "TYPE_REFERENCE":
                return SyntaxKind.TYPE_REFERENCE;
            case "RECORD_REST_TYPE":
                return SyntaxKind.RECORD_REST_TYPE;
            case "OBJECT_FIELD":
                return SyntaxKind.OBJECT_FIELD;
            case "OBJECT_TYPE_DESCRIPTOR":
                return SyntaxKind.OBJECT_TYPE_DESCRIPTOR;
            case "IMPORT_ORG_NAME":
                return SyntaxKind.IMPORT_ORG_NAME;
            case "MODULE_NAME":
                return SyntaxKind.MODULE_NAME;
            case "SUB_MODULE_NAME":
                return SyntaxKind.SUB_MODULE_NAME;
            case "IMPORT_VERSION":
                return SyntaxKind.IMPORT_VERSION;
            case "IMPORT_SUB_VERSION":
                return SyntaxKind.IMPORT_SUB_VERSION;
            case "IMPORT_PREFIX":
                return SyntaxKind.IMPORT_PREFIX;
            case "SPECIFIC_FIELD":
                return SyntaxKind.SPECIFIC_FIELD;
            case "COMPUTED_NAME_FIELD":
                return SyntaxKind.COMPUTED_NAME_FIELD;
            case "SPREAD_FIELD":
                return SyntaxKind.SPREAD_FIELD;
            case "SERVICE_BODY":
                return SyntaxKind.SERVICE_BODY;
            case "EXPRESSION_LIST_ITEM":
                return SyntaxKind.EXPRESSION_LIST_ITEM;
            case "NIL_TYPE":
                return SyntaxKind.NIL_TYPE;
            case "OPTIONAL_TYPE":
                return SyntaxKind.OPTIONAL_TYPE;

            // Trivia
            case "EOF_TOKEN":
                return SyntaxKind.EOF_TOKEN;
            case "END_OF_LINE_TRIVIA":
                return SyntaxKind.END_OF_LINE_TRIVIA;
            case "WHITESPACE_TRIVIA":
                return SyntaxKind.WHITESPACE_TRIVIA;
            case "COMMENT":
                return SyntaxKind.COMMENT;
            case "INVALID":
                return SyntaxKind.INVALID;

            // Unsupported
            default:
                throw new UnsupportedOperationException("cannot find syntax kind: " + kind);
        }
    }
}
