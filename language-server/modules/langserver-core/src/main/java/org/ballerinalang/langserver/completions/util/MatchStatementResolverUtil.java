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
package org.ballerinalang.langserver.completions.util;

import org.ballerina.compiler.api.model.BallerinaField;
import org.ballerina.compiler.api.types.RecordTypeDescriptor;
import org.ballerina.compiler.api.types.TupleTypeDescriptor;
import org.ballerina.compiler.api.types.TypeDescKind;
import org.ballerina.compiler.api.types.TypeDescriptor;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consists of the match statement resolving utilities.
 * 
 * @since 0.985.0
 */
public class MatchStatementResolverUtil {

    /**
     * Get the variable value destruture pattern.
     *
     * @return {@link String}   Generated Pattern clause
     */
    public static String getVariableValueDestructurePattern() {
        String valueHolder;
        String bodyPlaceHolder;
        valueHolder = "${1:value}";
        bodyPlaceHolder = "${2}";
        return valueHolder + " => " + CommonKeys.OPEN_BRACE_KEY + bodyPlaceHolder + CommonKeys.CLOSE_BRACE_KEY;
    }

    /**
     * Get the Structured fixed value match.
     *
     * @param typeDesc Structured type descriptor, either record type descriptor  or a tuple type descriptor
     * @return {@link String}   Generate pattern clause
     */
    public static String getStructuredFixedValueMatch(TypeDescriptor typeDesc) {
        StringBuilder fixedValPattern = new StringBuilder();
        if (typeDesc.getKind() == TypeDescKind.TUPLE) {
            List<TypeDescriptor> tupleTypes = ((TupleTypeDescriptor) typeDesc).getMemberTypes();
            List<String> defaultValues = tupleTypes.stream()
                    .map(MatchStatementResolverUtil::getStructuredFixedValueMatch)
                    .collect(Collectors.toList());
            fixedValPattern
                    .append(CommonKeys.OPEN_BRACKET_KEY)
                    .append(String.join(", ", defaultValues))
                    .append(CommonKeys.CLOSE_BRACKET_KEY);
        } else if (typeDesc.getKind() == TypeDescKind.RECORD) {
            List<BallerinaField> fields = ((RecordTypeDescriptor) typeDesc).getFieldDescriptors();
            List<String> defaultValues = fields.stream()
                    .map(field -> field.getName() + ":" + getStructuredFixedValueMatch(field.getTypeDescriptor()))
                    .collect(Collectors.toList());
            fixedValPattern
                    .append(CommonKeys.OPEN_BRACE_KEY)
                    .append(String.join(", ", defaultValues))
                    .append(CommonKeys.CLOSE_BRACE_KEY);
        } else {
            fixedValPattern.append(CommonUtil.getDefaultValueForType(typeDesc));
        }

        return fixedValPattern.toString();
    }

    /**
     * Generate the match pattern clause for the given value.
     *
     * @param matchValue        Match pattern value
     * @return {@link String}   Generated clause
     */
    public static String generateMatchPattern(String matchValue) {
        String valueHolder;
        String bodyPlaceHolder;
        valueHolder = "${1:" + matchValue + "}";
        bodyPlaceHolder = "${2}";
        return valueHolder + " => " + CommonKeys.OPEN_BRACE_KEY + bodyPlaceHolder + CommonKeys.CLOSE_BRACE_KEY;
    }
}
