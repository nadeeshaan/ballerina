package org.ballerina.compiler.api.document;

import org.ballerina.compiler.api.Position;
import org.wso2.ballerinalang.compiler.PackageCache;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.util.CompilerContext;

public class Symbols {
    
    private final CompilerContext.Key<Symbols> SYMBOLS_KEY = new CompilerContext.Key<>();
    
    private final PackageCache packageCache;
    
    private Symbols(CompilerContext context) {
        this.packageCache = PackageCache.getInstance(context);
    }
    
    public Symbols getInstance(CompilerContext context) {
        Symbols symbols = context.get(SYMBOLS_KEY);
        if (symbols == null) {
            symbols = new Symbols(context);
        }
        return symbols;
    }
    
    public BSymbol getSymbol(String uri, Position position) {
        return null;
    }
}
