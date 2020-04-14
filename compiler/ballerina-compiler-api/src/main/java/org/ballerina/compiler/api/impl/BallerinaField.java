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
package org.ballerina.compiler.api.impl;

import com.sun.istack.internal.NotNull;
import org.ballerina.compiler.api.model.BCompiledField;
import org.ballerina.compiler.api.model.types.BTypeDescriptor;

/**
 * Represents a field.
 * 
 * @since 1.3.0
 */
public class BallerinaField implements BCompiledField {
    private String name;
    private BTypeDescriptor type;

    public BallerinaField(@NotNull String name, @NotNull BTypeDescriptor type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public BTypeDescriptor getType() {
        return this.type;
    }
    
    public String getFieldSignature() {
        return this.type.getSignature() + " " + this.name;
    }
}
