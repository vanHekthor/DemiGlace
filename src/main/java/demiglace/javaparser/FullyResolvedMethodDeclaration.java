package demiglace.javaparser;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

public class FullyResolvedMethodDeclaration {

    private MethodDeclaration methodDeclaration;
    private ResolvedMethodDeclaration resolvedMethodDeclaration;

    public FullyResolvedMethodDeclaration(MethodDeclaration methodDeclaration, ResolvedMethodDeclaration resolvedMethodDeclaration) {
        this.methodDeclaration = methodDeclaration;
        this.resolvedMethodDeclaration = resolvedMethodDeclaration;
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
        this.methodDeclaration = methodDeclaration;
    }

    public ResolvedMethodDeclaration getResolvedMethodDeclaration() {
        return resolvedMethodDeclaration;
    }

    public void setResolvedMethodDeclaration(ResolvedMethodDeclaration resolvedMethodDeclaration) {
        this.resolvedMethodDeclaration = resolvedMethodDeclaration;
    }
}
