package javaparser;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import javassist.CtMethod;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class JavaProjectParser {

    public ParsingResult parseProject(Path path) {
        ParsingResult parsingResult = new ParsingResult();
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(path);

        parsingResult.setSourceRoots(projectRoot.getSourceRoots());
        parsingResult.setMethodMap(new HashMap<>());

        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            sourceRoot.getParserConfiguration().setAttributeComments(false); // Ignore comments

            try {
               parsingResult.addToMethodMap(collectMethodsInsideSourceRoot(sourceRoot));
            } catch (IOException e) {
                System.err.println("Was not able to parse " + sourceRoot.getRoot() + "!");
            }
        }

        return parsingResult;
    }

    private List<ParseResult<CompilationUnit>> collectCompilationUnits(SourceRoot sourceRoot) throws IOException {
        return sourceRoot.tryToParse();
    }

    private HashMap<String, List<ResolvedMethodCall>> collectMethodsInsideSourceRoot(SourceRoot sourceRoot) throws IOException {
        HashMap<String, List<ResolvedMethodCall>> methodMap = new HashMap<>();

        for (ParseResult<CompilationUnit> result : sourceRoot.tryToParse()) {
            result.getResult().ifPresent(compilationUnit -> {
                for (ClassOrInterfaceDeclaration cid : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)) {
                    System.out.println("##### [CLASS/INTERFACE] " + cid.getNameAsString() + " #####");

                    List<MethodDeclaration> methods = cid.getMethods();
                    for (MethodDeclaration method : methods) {
                        try {
                            System.out.println("# [Method] " + method.getNameAsString() + " " + method.toDescriptor());
                            ResolvedMethodDeclaration resolvedMethod = method.resolve();
                            System.out.println("--- Method Calls ---");
                            methodMap.put(resolvedMethod.getQualifiedName() + method.toDescriptor(), collectMethodCalls(method));

                        } catch (UnsolvedSymbolException exception) {
                            System.err.println("Couldn't resolve " + method.getNameAsString() + "!");
                        }
                    }
                }
            });
        }

        return methodMap;
    }

    private List<ResolvedMethodCall> collectMethodCalls(MethodDeclaration method) {
        List<ResolvedMethodCall> resolvedMethodCalls = new LinkedList<>();

        for (MethodCallExpr methodCall : method.findAll(MethodCallExpr.class)) {
            try {
                ResolvedMethodDeclaration rmd = methodCall.resolve();
                String descriptor = "descriptor not found";

                if (rmd instanceof JavaParserMethodDeclaration) {
                    JavaParserMethodDeclaration jpmd = (JavaParserMethodDeclaration) rmd;
                    MethodDeclaration methodDeclaration = jpmd.getWrappedNode();
                    descriptor = methodDeclaration.toDescriptor();
                } else if (rmd instanceof ReflectionMethodDeclaration) {
                    try {
                        Field privateField = ReflectionMethodDeclaration.class.getDeclaredField("method");
                        privateField.setAccessible(true);
                        Method reflectMethod = (Method) privateField.get(rmd);

                        descriptor = Type.getMethodDescriptor(reflectMethod);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        System.err.println("Unable to get descriptor form of reflection method "
                                + rmd.getQualifiedName() + " " + rmd.getQualifiedSignature());
                    }
                } else if (rmd instanceof JavassistMethodDeclaration) {
                    try {
                        Field privateField = JavassistMethodDeclaration.class.getDeclaredField("ctMethod");
                        privateField.setAccessible(true);
                        CtMethod javassistMethod = (CtMethod) privateField.get(rmd);

                        descriptor = javassistMethod.getMethodInfo().getDescriptor();

                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        System.err.println("Unable to get descriptor form of javassist method "
                                + rmd.getQualifiedName() + " " + rmd.getQualifiedSignature());
                    }
                }
                else {
                    if (methodCall.getScope().isPresent())
                        System.err.println("Method call " + methodCall.getScope().get()+ methodCall.getNameAsString()
                                + " is neither a regular nor a reflection nor a javassist method declaration");
                    else
                        System.err.println("Method call " + methodCall.getNameAsString()
                                + " is neither a regular nor a reflection nor a javassist method declaration.");
                }

                if (methodCall.getScope().isPresent())
                    System.out.println("- [Method] " + methodCall.getScope().get() + "."
                            + methodCall.getNameAsString() + " " + descriptor);
                else
                    System.out.println("- [Method] " + methodCall.getNameAsString() + " " + descriptor);

                resolvedMethodCalls.add(new ResolvedMethodCall(methodCall, rmd, descriptor));

            } catch( UnsolvedSymbolException | IllegalStateException e) {
                System.err.println("Method Call Expr " + methodCall.getScope().get() + "." + methodCall.getNameAsString()
                        + " in " + methodCall.getNameAsString() +  " could not be resolved!");
            }
        }

        return resolvedMethodCalls;
    }
}
