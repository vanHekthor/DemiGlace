package demiglace.callgraph;

import demiglace.javaparser.JavaParsingResult;
import demiglace.javaparser.JavaProjectParser;
import demiglace.javaparser.ResolvedMethodCall;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class GraphBuilder {
    private HashMap<String, List<ResolvedMethodCall>> methodMap;
    private List<CallGraphEdge> matchedCallGraphEdges;

    public void generateCallGraphNodes(Node node, JavaParsingResult pr) {
        methodMap = pr.getMethodMap();
        traverseTree(node);

        for (CallGraphEdge cge : matchedCallGraphEdges) {
            System.out.println(cge);
        }
    }

    private void traverseTree(Node node) {
        if (matchedCallGraphEdges == null) {
            matchedCallGraphEdges = new LinkedList<>();
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
        String parentDescriptor = parentNode.getAttributes().getNamedItem("methodSignature").getNodeValue();

        String methodCallName = childNode.getAttributes().getNamedItem("methodName").getNodeValue();
        String qualifiedChildNodeName = parentNode.getAttributes().getNamedItem("class").getNodeValue()
                + "." + methodCallName;
        String methodCallDescriptor = childNode.getAttributes().getNamedItem("methodSignature").getNodeValue();

        List<ResolvedMethodCall> methodCalls = methodMap.get(qualifiedParentName + parentDescriptor);
        if (methodCalls.size() == 1) {
            matched.add(createCallGraphEdge(qualifiedParentName, methodCalls.get(0)));
        } else if (methodCalls.size() > 1) {

            // filter by method name
            List<ResolvedMethodCall> filteredByName = methodCalls.stream().filter(call -> {
                return call.getName().equals(methodCallName);
            }).collect(Collectors.toList());

            if (filteredByName.size() == 1) {
                matched.add(createCallGraphEdge(qualifiedParentName, filteredByName.get(0)));
            }

            // filter by method declaration qualified name and method descriptor
            List<ResolvedMethodCall> filteredByQualiNameAndDesc = filteredByName.stream().filter(call -> {
                return call.getResolvedMethodDeclaration().getQualifiedName().equals(qualifiedChildNodeName)
                        && call.getDescriptor().equals(methodCallDescriptor);
            }).collect(Collectors.toList());

            for (ResolvedMethodCall call : filteredByQualiNameAndDesc) {
                matched.add(createCallGraphEdge(qualifiedParentName, call));
            }

            // remove calls with method declaration qualified name and method descriptor
            // filter by checking if interface/abstract method is implemented
            List<ResolvedMethodCall> filteredOutNameAndDesc = filteredByName.stream().filter(call -> {
                return !call.getResolvedMethodDeclaration().getQualifiedName().equals(qualifiedChildNodeName)
                        || !call.getDescriptor().equals(methodCallDescriptor);
            }).collect(Collectors.toList());

            List<ResolvedMethodCall> filteredByInterfaceImplementation =
                    filteredOutNameAndDesc.stream()
                            .filter(call -> checkIfInterfaceIsImplemented(childNode, call))
                            .collect(Collectors.toList());

            for (ResolvedMethodCall call : filteredByInterfaceImplementation) {
                matched.add(createCallGraphEdge(qualifiedParentName, call));
            }
        }

        return matched;
    }

    private CallGraphEdge createCallGraphEdge(String qualifiedClassName, ResolvedMethodCall rmc) {
        String type = "external method call";
        String label = rmc.getScope() + "." + rmc.getName();

        int startLine = rmc.getExpression().getRange().get().begin.line;
        int endLine = rmc.getExpression().getRange().get().end.line;
        String file = qualifiedClassName.replace('.', '/') + ".java";

        Region from = new Region(file, startLine, endLine);

        if (rmc.getMethodDeclaration() != null && rmc.getMethodDeclaration().getRange().isPresent()) {
            startLine = rmc.getMethodDeclaration().getRange().get().begin.line;
            endLine = rmc.getMethodDeclaration().getRange().get().end.line;
            String[] qualifiedMethodName = rmc.getResolvedMethodDeclaration().getQualifiedName().split("\\.");
            String qualifiedName =
                    String.join("/", Arrays.copyOf(qualifiedMethodName, qualifiedMethodName.length - 1));

            file = qualifiedName + ".java";
            Region to = new Region(file, startLine, endLine);

            int value = 1;

            return new CallGraphEdge(type, label, from, to, value);
        }
        return null;
    }

    private boolean checkIfInterfaceIsImplemented(Node node, ResolvedMethodCall resolvedMethodCall) {
        if (resolvedMethodCall.getMethodDeclaration() != null) {
            String qualifiedNodeClassName = node.getAttributes().getNamedItem("class").getNodeValue()
                    + "." + node.getAttributes().getNamedItem("methodName").getNodeValue();

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
