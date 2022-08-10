package demiglace;

import demiglace.callgraph.GraphBuilder;
import demiglace.javaparser.JavaParsingResult;
import demiglace.javaparser.JavaProjectParser;
import org.w3c.dom.Node;
import demiglace.profilerparser.JProfilerParser;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DemiGlace {

    public static Path PROJECT_PATH;
    public static void main(String[] args) {
        JavaProjectParser javaProjectParser = new JavaProjectParser();
        PROJECT_PATH = Paths.get("C:\\Users\\qb19zexe\\Git\\catena-java");
        JavaParsingResult pr = javaProjectParser.parseProject(PROJECT_PATH);

        Node node = new JProfilerParser().parseProfilerXML("C:\\Users\\qb19zexe\\Git\\sopro-vr\\Tools\\control_flow\\profiler_data\\catena\\tree.xml");

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
