package callgraph;

import javaparser.JavaParsingResult;
import javaparser.ResolvedMethodCall;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.List;

public class GraphBuilder {
    private HashMap<String, List<ResolvedMethodCall>> methodMap;

    public void generateCallGraphNodes(Node node, JavaParsingResult pr) {
        methodMap = pr.getMethodMap();
        traverseTree(node);
    }

    private void traverseTree(Node node) {
        // if it is a node that properly references a method
        matchNodeToMethod(node);

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                //calls this method for all the children which is Element
                traverseTree(currentNode);
            }
        }
    }

    private CallGraphEdge matchNodeToMethod(Node node) {
        if ((node.getAttributes().getNamedItem("class") != null)
                && (node.getAttributes().getNamedItem("methodName") != null)
                && (node.getAttributes().getNamedItem("methodName") != null)) {

            String qualifiedName = node.getAttributes().getNamedItem("class").getNodeValue()
                    + "." + node.getAttributes().getNamedItem("methodName").getNodeValue();
            String descriptor = node.getAttributes().getNamedItem("methodSignature").getNodeValue();

            if (methodMap.containsKey(qualifiedName+descriptor)) {
                System.out.println("Successfully matched " + qualifiedName + descriptor + "!");
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
        return null;
    }
}
