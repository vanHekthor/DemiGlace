package demiglace.callgraph;

public class Region {
    private String file;
    private Range lines;

    public Region(String file, int startLine, int endLine) {
        this.file = file;
        this.lines = new Range(startLine, endLine);
    }

    public Region(String file, Range lines) {
        this.file = file;
        this.lines = lines;
    }

    @Override
    public String toString() {
        return "Region{" +
                "file='" + file + '\'' +
                ", lines=" + lines +
                '}';
    }
}
