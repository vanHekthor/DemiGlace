package javaparser;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

public class ResolvedMethodCall {
    private ResolvedMethodDeclaration declaration;
    private MethodCallExpr expression;
    private String descriptor;

    public ResolvedMethodCall(MethodCallExpr expression, ResolvedMethodDeclaration declaration, String descriptor) {
        this.expression = expression;
        this.declaration = declaration;
        this.descriptor = descriptor;
    }

    public String getName() {
        return expression.getNameAsString();
    }

    public String getScope() {
        return expression.getScope().isPresent() ? expression.getScope().get().toString() : "";
    }

    public ResolvedMethodDeclaration getDeclaration() {
        return declaration;
    }

    public MethodCallExpr getExpression() {
        return expression;
    }

    public String getDescriptor() {
        return descriptor;
    }
}
