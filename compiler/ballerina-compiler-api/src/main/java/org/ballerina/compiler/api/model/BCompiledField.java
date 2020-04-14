package org.ballerina.compiler.api.model;

import org.ballerina.compiler.api.model.types.BTypeDescriptor;

public interface BCompiledField {
    String getName();
    
    BTypeDescriptor getType();
}
