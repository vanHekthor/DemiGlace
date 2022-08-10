package profilerparser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;

public class JProfilerParser {

    public Node parseProfilerXML(String filePath) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();

        try {
            // optional, but recommended
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filePath));
            doc.getDocumentElement().normalize();

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            XPathExpression expr = xpath.compile("//node[@class=\"main.java.Catena\" and @methodName=\"catena\"]");
            Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);

            return node;

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void printTree(Node node, int depth) {
        // do something with the current node instead of System.out
        String prefix = "n";
        if ((node.getAttributes().getNamedItem("class") != null)
                && (node.getAttributes().getNamedItem("methodName") != null)
                && (node.getAttributes().getNamedItem("methodName") != null)) {

            System.out.print("  ".repeat(depth) + "<node ");
            System.out.print(node.getAttributes().getNamedItem("class").toString()
                    + " " + node.getAttributes().getNamedItem("methodName").toString()
                    + " " + node.getAttributes().getNamedItem("methodSignature").toString());
            System.out.println(">");
            prefix = "n";
        } else {
            System.out.print("  ".repeat(depth) + "<noNode ");
            for (int i = 0; i < node.getAttributes().getLength() - 1; i++) {
                System.out.print(node.getAttributes().item(i) + " ");
            }
            System.out.print(node.getAttributes().item(node.getAttributes().getLength() - 1));
            System.out.println(">");
            prefix = "noN";
        }

        int oldDepth = depth;
        NodeList nodeList = node.getChildNodes();
        depth++;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                //calls this method for all the children which is Element
                printTree(currentNode, depth);
            }
        }
        System.out.println("  ".repeat(oldDepth) + "</" + prefix + "ode>");
    }
}
