/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.langserver.completions;

import org.ballerina.compiler.api.model.BallerinaField;
import org.ballerinalang.langserver.commons.LSContext;
import org.ballerinalang.langserver.commons.completion.LSCompletionItem;
import org.eclipse.lsp4j.CompletionItem;

/**
 * Represents a BField Completion Item.
 *
 * @since 1.2.0
 */
public class FieldCompletionItem implements LSCompletionItem {
    private LSContext lsContext;
    private CompletionItem completionItem;
    private BallerinaField bField;

    public FieldCompletionItem(LSContext lsContext, BallerinaField bField, CompletionItem completionItem) {
        this.lsContext = lsContext;
        this.completionItem = completionItem;
        this.bField = bField;
    }

    @Override
    public CompletionItem getCompletionItem() {
        return this.completionItem;
    }

    public LSContext getLsContext() {
        return lsContext;
    }

    public BallerinaField getBField() {
        return bField;
    }
}
