package demiglace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import demiglace.callgraph.CallGraphEdge;
import demiglace.callgraph.GraphBuilder;
import demiglace.javaparser.JavaParsingResult;
import demiglace.javaparser.JavaProjectParser;
import org.w3c.dom.Node;
import demiglace.profilerparser.JProfilerParser;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;

public class DemiGlace {

    public static Path PROJECT_PATH;
    public static void main(String[] args) {
        JavaProjectParser javaProjectParser = new JavaProjectParser();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch(Exception ex) {
            ex.printStackTrace();
        }

        PROJECT_PATH = Paths.get(openDirFileChooser().getPath());
        JavaParsingResult pr = javaProjectParser.parseProject(PROJECT_PATH);

        Node node = new JProfilerParser().parseProfilerXML(openDirFileChooser().getPath());

        GraphBuilder graphBuilder = new GraphBuilder();
        HashSet<CallGraphEdge> edgeList = graphBuilder.generateCallGraphNodes(node, pr);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        ParsingResult parsingResult = new ParsingResult(edgeList);

        String savePath = saveFileChooser().getPath();
        try {
            FileWriter fileWriter = new FileWriter(savePath);
            gson.toJson(parsingResult, fileWriter);
            fileWriter.flush();
        } catch (IOException e) {
            System.err.println("Unable to save call graph at path " + savePath + "!");
        }
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

    private static File saveFileChooser() {
        File file = null;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION){
            file = fileChooser.getSelectedFile();
        }

        return file;
    }
}
