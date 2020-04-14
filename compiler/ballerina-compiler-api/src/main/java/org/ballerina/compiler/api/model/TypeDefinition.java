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
package org.ballerina.compiler.api.model;

import org.ballerina.compiler.api.impl.BallerinaSymbol;
import org.ballerina.compiler.api.model.types.BTypeDescriptor;
import org.ballerinalang.model.elements.PackageID;

/**
 * Represent a module level type definition.
 * 
 * @since 1.3.0
 */
public class TypeDefinition extends BallerinaSymbol {
    private BTypeDescriptor typeDescriptor;
    private boolean isPublic;
    
    // TODO: represent the meta data
    public TypeDefinition(String name, PackageID moduleID, BTypeDescriptor typeDescriptor, boolean isPublic) {
        super(name, moduleID);
        this.typeDescriptor = typeDescriptor;
        this.isPublic = isPublic;
    }

    public BTypeDescriptor getTypeDescriptor() {
        return typeDescriptor;
    }
    
    public boolean isPublic() {
        return this.isPublic;
    }
}
