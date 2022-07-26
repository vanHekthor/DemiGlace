package org.javaparser.examples;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.Optional;

public class VoidVisitorStarter {
    private static final String FILE_PATH = "software_system_examples/Helper.java";
    public static void main(String[] args) throws Exception {
        CompilationUnit cu = StaticJavaParser.parse(new File(FILE_PATH));

        ClassOrInterfaceDeclaration classHelper = cu.getClassByName("Helper").get();

        VoidVisitor<Void> methodNameVisitor = new MethodNamePrinter();

        for (MethodDeclaration method : classHelper.getMethods()) {
            System.out.println("Method: " + method.getName());
            method.accept(new MethodNamePrinter(), null);
        }

        // methodNameVisitor.visit(cu, null);
    }

    private static class MethodNamePrinter extends VoidVisitorAdapter<Void> {
        @Override
        public void visit(MethodDeclaration md, Void arg) {
            super.visit(md, arg);
            // System.out.println("Method Name Printed: " + md.getName());
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            // Found a method call
            System.out.println(n.getScope().get() + "." + n.getName());
            // Don't forget to call super, it may find more method calls inside the arguments of this method call, for example.
            super.visit(n, arg);
        }
    }
}


