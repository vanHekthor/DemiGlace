import profilerparser.JProfilerParser;

import javax.swing.*;
import java.io.File;

public class DemiGlace {

    public static void main(String[] args) {
//        JavaProjectParser javaProjectParser = new JavaProjectParser();
//        ParsingResult pr = javaProjectParser.parseProject(openDirFileChooser().toPath());

        new JProfilerParser().parseProfilerXML("C:\\Users\\Quoc Duong Bui\\Documents\\Arbeit\\WHK\\sopro-vr\\Tools\\control_flow\\profiler_data\\catena\\tree.xml");
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
