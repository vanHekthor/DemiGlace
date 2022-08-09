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
import javaparser.JavaProjectParser;
import javaparser.ParsingResult;
import javassist.CtMethod;
import org.objectweb.asm.Type;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

public class DemiGlace {

    public static void main(String[] args) {

//        JavaProjectParser javaProjectParser = new JavaProjectParser();
//        ParsingResult pr = javaProjectParser.parseProject(openDirFileChooser().toPath());
//        System.out.println("Ende");
        // select the project folder
        Path path = openDirFileChooser().toPath();
        ProjectRoot projectRoot = new SymbolSolverCollectionStrategy().collect(path);

        tryToParseSourceRoots(projectRoot);
    }

    private static int methodCount = 1;
    private static void tryToParseSourceRoots(ProjectRoot projectRoot) {
        for (SourceRoot sourceRoot : projectRoot.getSourceRoots()) {
            sourceRoot.getParserConfiguration().setAttributeComments(false); // Ignore comments

            try {
                for (ParseResult<CompilationUnit> result : sourceRoot.tryToParse()) {
                    result.getResult().ifPresent(compilationUnit -> {
                        for (ClassOrInterfaceDeclaration cid : compilationUnit.findAll(ClassOrInterfaceDeclaration.class)){

                            System.out.println("##### [CLASS/INTERFACE] " + cid.getNameAsString() + " #####");
                            printAllMethods(cid);
                        }
                    });
                }
            } catch (IOException e) {
                System.err.println("Was not able to parse " + sourceRoot.getRoot() + "!");
            }
        }
    }

    private static void printAllMethods(ClassOrInterfaceDeclaration cid) {
        List<MethodDeclaration> methods = cid.getMethods();

        for (MethodDeclaration method : methods) {
            try {
                System.out.println(methodCount + " [Method] " + method.getNameAsString() + " " + method.toDescriptor());
                methodCount++;
            } catch (UnsolvedSymbolException exception) {
                System.err.println("Couldn't resolve " + method.getNameAsString() + "!");
            }
            System.out.println("--- Method Calls ---");

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
                        return;
                    }

                    if (methodCall.getScope().isPresent())
                        System.out.println("- [Method] " + methodCall.getScope().get() + "."
                                + methodCall.getNameAsString() + " " + descriptor);
                    else
                        System.out.println("- [Method] " + methodCall.getNameAsString() + " " + descriptor);

                } catch( UnsolvedSymbolException | IllegalStateException e) {
                    System.err.println("Method Call Expr " + methodCall.getScope().get() + "." + methodCall.getNameAsString()
                            + " in " + methodCall.getNameAsString() +  " could not be resolved!");
                }
            }

        }

    }

    private static File openDirFileChooser() {
        File file = null;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            file = fileChooser.getSelectedFile();
        }

        return file;
    }
}
