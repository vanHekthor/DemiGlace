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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JavaProjectParser {

    public static Set<String> resolveAncestorTypes(String qualifiedClassName) throws IllegalStateException {
        Set<String> ancestorSet = new HashSet<>();
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(DemiGlace.PROJECT_PATH);

        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            sourceRoot.getParserConfiguration().setAttributeComments(false); // Ignore comments
            sourceRoot.getParserConfiguration().setTabSize(4);


            String classPath = sourceRoot.getRoot().toAbsolutePath() + "/"
                    + qualifiedClassName.replace('.', '/') + ".java";

            JavaParser parser = new JavaParser(sourceRoot.getParserConfiguration());
            try {
                ParseResult<CompilationUnit> pr = parser.parse(new File(classPath));

                if (pr.getResult().isPresent()) {
                    CompilationUnit compilationUnit = pr.getResult().get();
                    List<ClassOrInterfaceDeclaration> cil =
                            compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream().filter(cid -> {
                                if (cid.getFullyQualifiedName().isPresent()) {
                                    return cid.getFullyQualifiedName().get().equals(qualifiedClassName);
                                }
                                return false;
                            }).collect(Collectors.toList());

                    if (cil.size() == 1) {
                        ancestorSet = cil.get(0).resolve().getAllAncestors()
                                .stream().map(ResolvedReferenceType::getQualifiedName)
                                .collect(Collectors.toSet());
                        return ancestorSet;
                    }

                    // TODO: handle cases where multiple classes with the same qualified name but with
                    //  different source roots exist
                    if (cil.size() > 1) {
                        throw new IllegalStateException("Multiple classes have the same fully qualified name: "
                                + qualifiedClassName + " inside "
                                + qualifiedClassName.replace('.', '/') + ".java");
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println("[resolveAncestorTypes] The file " + classPath + " does not exist!");
            }
        }
        // return empty set
        return ancestorSet;
    }

    public static FullyResolvedMethodDeclaration findMethodDeclaration(String qualifiedMethodName, String methodDescriptor) {
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(DemiGlace.PROJECT_PATH);

        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            sourceRoot.getParserConfiguration().setAttributeComments(false); // Ignore comments
            sourceRoot.getParserConfiguration().setTabSize(4);

            JavaParser parser = new JavaParser(sourceRoot.getParserConfiguration());

            String[] qualifiedClassNameArray = Arrays.copyOf(
                    qualifiedMethodName.split("\\."),
                    qualifiedMethodName.split("\\.").length - 1);

            String qualifiedClassName = String.join("/", qualifiedClassNameArray);
            String classPath = sourceRoot.getRoot().toAbsolutePath() + "/"
                    + qualifiedClassName + ".java";

            try {
                ParseResult<CompilationUnit> pr = parser.parse(new File(classPath));

                if (pr.getResult().isPresent()) {
                    CompilationUnit compilationUnit = pr.getResult().get();
                    List<MethodDeclaration> foundMethods =
                            compilationUnit.findAll(MethodDeclaration.class).stream().filter(md -> {
                                return md.resolve().getQualifiedName().equals(qualifiedMethodName);
                            }).collect(Collectors.toList());

                    // TODO: handle cases where multiple methods with the same qualified name but with
                    //  different source roots exist
                    if (foundMethods.size() == 1) {
                        return new FullyResolvedMethodDeclaration(foundMethods.get(0), foundMethods.get(0).resolve());

                    } else if (foundMethods.size() > 1) {
                        foundMethods = foundMethods.stream().filter(methodDeclaration -> {
                            return methodDeclaration.toDescriptor().equals(methodDescriptor);
                        }).collect(Collectors.toList());

                        if (foundMethods.size() == 1) {
                            return new FullyResolvedMethodDeclaration(foundMethods.get(0), foundMethods.get(0).resolve());
                        }

                        // throw exception
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println("[findMethodDeclaration] The file " + classPath + " does not exist!");
            }
        }
        //throw exception
        return null;
    }

    public JavaParsingResult parseProject(Path path) {
        JavaParsingResult parsingResult = new JavaParsingResult();
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(path);

        parsingResult.setSourceRoots(projectRoot.getSourceRoots());
        parsingResult.setMethodMap(new HashMap<>());

        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            sourceRoot.getParserConfiguration().setAttributeComments(false); // Ignore comments
            sourceRoot.getParserConfiguration().setTabSize(4);

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
                            + methodCall.getNameAsString() + " " + descriptor
                            + " begin: " + methodCall.getRange().get().begin
                            + " end: " + methodCall.getRange().get().end);
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
