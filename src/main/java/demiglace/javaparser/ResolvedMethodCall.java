package demiglace.javaparser;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

import java.util.Set;

public class ResolvedMethodCall {

    private MethodDeclaration methodDeclaration;
    private ResolvedMethodDeclaration resolvedMethodDeclaration;
    private MethodCallExpr expression;
    private String descriptor;
    private Set<String> ancestorTypes;

    public ResolvedMethodCall(MethodDeclaration methodDeclaration, ResolvedMethodDeclaration resolvedMethodDeclaration, MethodCallExpr expression, String descriptor) {
        this.methodDeclaration = methodDeclaration;
        this.resolvedMethodDeclaration = resolvedMethodDeclaration;
        this.expression = expression;
        this.descriptor = descriptor;
    }

    public ResolvedMethodCall(ResolvedMethodDeclaration resolvedMethodDeclaration, MethodCallExpr expression, String descriptor) {
        this.resolvedMethodDeclaration = resolvedMethodDeclaration;
        this.expression = expression;
        this.descriptor = descriptor;
    }

    public String getName() {
        return expression.getNameAsString();
    }

    public String getScope() {
        return expression.getScope().isPresent() ? expression.getScope().get().toString() : "";
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public ResolvedMethodDeclaration getResolvedMethodDeclaration() {
        return resolvedMethodDeclaration;
    }

    public MethodCallExpr getExpression() {
        return expression;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Set<String> getAncestorTypes() {
        return ancestorTypes;
    }

    public void setAncestorTypes(Set<String> ancestorTypes) {
        this.ancestorTypes = ancestorTypes;
    }

    public String getQualifiedClassName() {
        return resolvedMethodDeclaration.getPackageName() + "." + resolvedMethodDeclaration.getClassName();
    }
}
