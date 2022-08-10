package demiglace.javaparser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.symbolsolver.utils.SymbolSolverCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.github.javaparser.utils.SourceRoot;
import demiglace.DemiGlace;
import javassist.CtMethod;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JavaProjectParser {

    public static Set<String> resolveAncestorTypes(String qualifiedClassName) {
        Set<String> ancestorSet = new HashSet<>();
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(DemiGlace.PROJECT_PATH);

        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            sourceRoot.getParserConfiguration().setAttributeComments(false); // Ignore comments

            JavaParser parser = new JavaParser(sourceRoot.getParserConfiguration());
            ParseResult<CompilationUnit> pr = parser.parse(qualifiedClassName.replace('.', '/'));

            if (pr.getResult().isPresent()) {
                CompilationUnit compilationUnit = pr.getResult().get();
                List<ClassOrInterfaceDeclaration> cil =
                        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream().filter(cid -> {
                            if (cid.getFullyQualifiedName().isPresent()) {
                                return cid.getFullyQualifiedName().get().equals(qualifiedClassName);
                            }
                            return false;
                        }).collect(Collectors.toList());

                if (cil.size() == 1){
                    ancestorSet = cil.get(0).resolve().getAllAncestors()
                            .stream().map(ResolvedReferenceType::getQualifiedName)
                            .collect(Collectors.toSet());
                }
            }
        }
        return ancestorSet;
    }

    public JavaParsingResult parseProject(Path path) {
        JavaParsingResult parsingResult = new JavaParsingResult();
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
                MethodDeclaration methodDeclaration = null;
                String descriptor = "descriptor not found";

                if (rmd instanceof JavaParserMethodDeclaration) {
                    JavaParserMethodDeclaration jpmd = (JavaParserMethodDeclaration) rmd;
                    methodDeclaration = jpmd.getWrappedNode();
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

                if (methodDeclaration != null) {
                    ResolvedMethodCall rmc = new ResolvedMethodCall(methodDeclaration, rmd, methodCall, descriptor);
                    resolvedMethodCalls.add(rmc);
                } else {
                    resolvedMethodCalls.add(new ResolvedMethodCall(rmd, methodCall, descriptor));
                }

            } catch( UnsolvedSymbolException | IllegalStateException e) {
                System.err.println("Method Call Expr " + methodCall.getScope().get() + "." + methodCall.getNameAsString()
                        + " in " + methodCall.getNameAsString() +  " could not be resolved!");
            }
        }

        return resolvedMethodCalls;
    }
}
