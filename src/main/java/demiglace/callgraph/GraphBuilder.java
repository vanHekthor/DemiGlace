package demiglace.callgraph;

import com.github.javaparser.ast.body.MethodDeclaration;
import demiglace.javaparser.FullyResolvedMethodDeclaration;
import demiglace.javaparser.JavaParsingResult;
import demiglace.javaparser.JavaProjectParser;
import demiglace.javaparser.ResolvedMethodCall;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.LineNumberReader;
import java.util.*;
import java.util.stream.Collectors;

public class GraphBuilder {
    private HashMap<String, List<ResolvedMethodCall>> methodMap;
    private HashSet<CallGraphEdge> matchedCallGraphEdges;

    public HashSet<CallGraphEdge> generateCallGraphNodes(Node node, JavaParsingResult pr) {
        methodMap = pr.getMethodMap();
        traverseTree(node);

        return matchedCallGraphEdges;
    }

    private void traverseTree(Node node) {
        if (matchedCallGraphEdges == null) {
            matchedCallGraphEdges = new HashSet<>();
        }

        // if it is a node that properly references a method
        matchedCallGraphEdges.addAll(matchNodeToMethod(node));

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                //calls this method for all the children which is Element
                traverseTree(currentNode);
            }
        }
    }

    private List<CallGraphEdge> matchNodeToMethod(Node node) {
        List<CallGraphEdge> matched = new LinkedList<>();
        if (isValidMethodNode(node)) {

            String qualifiedName = node.getAttributes().getNamedItem("class").getNodeValue()
                    + "." + node.getAttributes().getNamedItem("methodName").getNodeValue();
            String descriptor = node.getAttributes().getNamedItem("methodSignature").getNodeValue();

            if (methodMap.containsKey(qualifiedName+descriptor)) {
                matched.addAll(matchChildNodesToCalls(node));
            } else {
                System.out.println("FAILED to match " + qualifiedName + descriptor + "!");
            }

        } else {
            System.out.print("NOT a proper node: ");
            for (int i = 0; i < node.getAttributes().getLength() - 1; i++) {
                System.out.print(node.getAttributes().item(i) + " ");
            }
            System.out.println(node.getAttributes().item(node.getAttributes().getLength() - 1));
        }
        return matched;
    }

    private List<CallGraphEdge> matchChildNodesToCalls(Node parentNode) {
        Node childNode = parentNode.getFirstChild();

        LinkedList<CallGraphEdge> matched = new LinkedList<>();

        if (childNode != null) {
            while (childNode.getNextSibling() != null) {
                childNode = childNode.getNextSibling();
                if (isValidMethodNode(childNode)) {
                    if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                        if (isValidMethodNode(childNode))
                            matched.addAll(matchChildNodeToCalls(parentNode, childNode));
                    }
                }
            }
        }

        String qualifiedName = parentNode.getAttributes().getNamedItem("class").getNodeValue()
                + "." + parentNode.getAttributes().getNamedItem("methodName").getNodeValue();
        String descriptor = parentNode.getAttributes().getNamedItem("methodSignature").getNodeValue();
        System.out.println("Successfully matched " + qualifiedName + descriptor + "!");
        return matched;
    }

    private List<CallGraphEdge> matchChildNodeToCalls(Node parentNode, Node childNode) {
        LinkedList<CallGraphEdge> matched = new LinkedList<>();

        String qualifiedParentName = parentNode.getAttributes().getNamedItem("class").getNodeValue()
                + "." + parentNode.getAttributes().getNamedItem("methodName").getNodeValue();
        String qualifiedParentClassName = parentNode.getAttributes().getNamedItem("class").getNodeValue();
        String parentDescriptor = parentNode.getAttributes().getNamedItem("methodSignature").getNodeValue();

        String methodCallName = childNode.getAttributes().getNamedItem("methodName").getNodeValue();
        String qualifiedChildNodeClassName = childNode.getAttributes().getNamedItem("class").getNodeValue();
        String qualifiedChildNodeMethodName = qualifiedChildNodeClassName + "." + methodCallName;
        String methodCallDescriptor = childNode.getAttributes().getNamedItem("methodSignature").getNodeValue();

        List<ResolvedMethodCall> methodCalls = methodMap.get(qualifiedParentName + parentDescriptor);
        if (methodCalls.size() == 1) {
            try {
                updateResolvedMethodCall(methodCalls.get(0), qualifiedChildNodeMethodName, methodCallDescriptor);
                matched.add(createCallGraphEdge(qualifiedParentClassName, methodCalls.get(0)));

                return matched;

            } catch (Exception e) {
                System.err.println("Method declaration " + qualifiedChildNodeMethodName + methodCallDescriptor
                        + " not found in source code!");
            }
        } else if (methodCalls.size() > 1) {
            // filter by method name
            List<ResolvedMethodCall> filteredByName = methodCalls.stream().filter(call -> {
                return call.getName().equals(methodCallName);
            }).collect(Collectors.toList());

            if (filteredByName.size() == 1) {
                // if (filteredByName.get(0).getMethodDeclaration() !=null)
                try {
                    updateResolvedMethodCall(filteredByName.get(0), qualifiedChildNodeMethodName, methodCallDescriptor);
                    matched.add(createCallGraphEdge(qualifiedParentClassName, filteredByName.get(0)));
                } catch (Exception e) {
                    System.err.println("Method declaration " + qualifiedChildNodeMethodName + methodCallDescriptor
                            + " not found in source code!");
                }

                return matched;
            }

            // filter by method declaration qualified name and method descriptor
            List<ResolvedMethodCall> filteredByQualiNameAndDesc = filteredByName.stream().filter(call -> {
                return call.getResolvedMethodDeclaration().getQualifiedName().equals(qualifiedChildNodeMethodName)
                        && call.getDescriptor().equals(methodCallDescriptor);
            }).collect(Collectors.toList());

            for (ResolvedMethodCall call : filteredByQualiNameAndDesc) {
//                if (call.getMethodDeclaration() != null)
                try {
                    updateResolvedMethodCall(call, qualifiedChildNodeMethodName, methodCallDescriptor);
                    matched.add(createCallGraphEdge(qualifiedParentClassName, call));
                } catch (Exception e) {
                    System.err.println("Method declaration " + qualifiedChildNodeMethodName + methodCallDescriptor
                            + " not found in source code!");
                }
            }

            // remove calls with method declaration qualified name and method descriptor
            // filter by checking if interface/abstract method is implemented
            List<ResolvedMethodCall> filteredOutNameAndDesc = filteredByName.stream().filter(call -> {
                return !call.getResolvedMethodDeclaration().getQualifiedName().equals(qualifiedChildNodeMethodName)
                        || !call.getDescriptor().equals(methodCallDescriptor);
            }).collect(Collectors.toList());

            List<ResolvedMethodCall> filteredByInterfaceImplementation =
                    filteredOutNameAndDesc.stream()
                            .filter(call -> checkIfInterfaceIsImplemented(childNode, call))
                            .collect(Collectors.toList());

            for (ResolvedMethodCall call : filteredByInterfaceImplementation) {
                FullyResolvedMethodDeclaration fullyResolvedMethodDeclaration =
                        JavaProjectParser.findMethodDeclaration(qualifiedChildNodeMethodName, methodCallDescriptor);

                if (fullyResolvedMethodDeclaration != null)
                    call.updateValues(fullyResolvedMethodDeclaration);

                if (call.getMethodDeclaration() != null)
                    matched.add(createCallGraphEdge(qualifiedParentClassName, call));
            }
        }

        return matched;
    }

    private void updateResolvedMethodCall(ResolvedMethodCall rmc, String qualifiedMethodName, String methodCallDescriptor) throws Exception {
        if (rmc.getMethodDeclaration() == null) {
            throw new Exception();
        }
        FullyResolvedMethodDeclaration fullyResolvedMethodDeclaration =
                JavaProjectParser.findMethodDeclaration(qualifiedMethodName, methodCallDescriptor);

        if (fullyResolvedMethodDeclaration != null)
            rmc.updateValues(fullyResolvedMethodDeclaration);
    }

    private CallGraphEdge createCallGraphEdge(String qualifiedClassName, ResolvedMethodCall rmc) {
        String type = "external method call";
        String label = !rmc.getScope().equals("") ? rmc.getScope() + "." + rmc.getName() : rmc.getName();

        int startLine = rmc.getExpression().getRange().get().begin.line;
        int endLine = rmc.getExpression().getRange().get().end.line;

        // javaparser gives you the start column starting from the scope, but here only start column
        // of the actual method name is needed
        // e.g. in System.out.println("something") the start column of "println" i.e. the column of 'p' is needed
        // + 1 because without you would get the column of the '.' before the method name
        int startColumn = rmc.getExpression().getRange().get().begin.column;

        if (!rmc.getScope().equals("")) {
            startColumn += rmc.getScope().length() + 1;
        }

        int endColumn = startColumn + rmc.getName().length() - 1;

        String file = "src/" + qualifiedClassName.replace('.', '/') + ".java";

//        Region from = new Region(file, startLine, endLine);
        Region from = new Region(file, startLine, startColumn, endLine, endColumn);

        // handling location of the method declaration corresponding to the resolved method call
        if (rmc.getMethodDeclaration() != null && rmc.getMethodDeclaration().getRange().isPresent()) {
            startLine = rmc.getMethodDeclaration().getRange().get().begin.line;
            endLine = rmc.getMethodDeclaration().getRange().get().end.line;
            startColumn = rmc.getMethodDeclaration().getRange().get().begin.column;
            endColumn = rmc.getMethodDeclaration().getRange().get().end.column;

            String[] qualifiedMethodName = rmc.getResolvedMethodDeclaration().getQualifiedName().split("\\.");
            String qualifiedName =
                    String.join("/", Arrays.copyOf(qualifiedMethodName, qualifiedMethodName.length - 1));

            file = "src/" + qualifiedName + ".java";
//            Region to = new Region(file, startLine, endLine);
            Region to = new Region(file, startLine, startColumn, endLine, endColumn);

            int value = 1;

            CallGraphEdge callGraphEdge = new CallGraphEdge(type, label, rmc.getMethodDeclaration().getDeclarationAsString(), from, to, value);
            System.out.println(callGraphEdge);
            return callGraphEdge;
        }
        System.err.println(rmc.getMethodDeclaration());
        return null;
    }

    private boolean checkIfInterfaceIsImplemented(Node node, ResolvedMethodCall resolvedMethodCall) {
        if (resolvedMethodCall.getMethodDeclaration() != null) {
            String qualifiedNodeClassName = node.getAttributes().getNamedItem("class").getNodeValue();

            if (resolvedMethodCall.getMethodDeclaration().getParentNode().isPresent()) {
                return JavaProjectParser.resolveAncestorTypes(qualifiedNodeClassName)
                        .contains(resolvedMethodCall.getQualifiedClassName());
            }
        }
        return false;
    }

    private boolean isValidMethodNode(Node node) {
        if (node == null) {
            return false;
        }

        return (node.getAttributes() != null)
                && (node.getAttributes().getNamedItem("class") != null)
                && (node.getAttributes().getNamedItem("methodName") != null)
                && (node.getAttributes().getNamedItem("methodName") != null);
    }
}
