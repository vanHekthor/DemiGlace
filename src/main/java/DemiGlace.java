import callgraph.GraphBuilder;
import javaparser.JavaParsingResult;
import javaparser.JavaProjectParser;
import org.w3c.dom.Node;
import profilerparser.JProfilerParser;

import javax.swing.*;
import java.io.File;

public class DemiGlace {

    public static void main(String[] args) {
        JavaProjectParser javaProjectParser = new JavaProjectParser();
        JavaParsingResult pr = javaProjectParser.parseProject(openDirFileChooser().toPath());

        Node node = JProfilerParser.parseProfilerXML("C:\\Users\\Quoc Duong Bui\\Documents\\Arbeit\\WHK\\sopro-vr\\Tools\\control_flow\\profiler_data\\catena\\tree.xml");

        GraphBuilder graphBuilder = new GraphBuilder();
        graphBuilder.generateCallGraphNodes(node, pr);
    }

    private static File openDirFileChooser() {
        File file = null;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
            file = fileChooser.getSelectedFile();
        }

        return file;
    }
}
