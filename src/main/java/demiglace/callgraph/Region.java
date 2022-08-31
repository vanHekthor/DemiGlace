package demiglace.callgraph;

public class Region {
    private String file;
    private Range lines;
    private Range columns;

    public Region(String file, int startLine, int startColumn, int endLine, int endColumn) {
        this.file = file;
        this.lines = new Range(startLine, endLine);
        this.columns = new Range(startColumn, endColumn);
    }

    public Region(String file, Range lines, Range columns) {
        this.file = file;
        this.lines = lines;
        this.columns = columns;
    }

    public Region(String file, int startLine, int endLine) {
        this.file = file;
        this.lines = new Range(startLine, endLine);
    }

    public Region(String file, Range lines) {
        this.file = file;
        this.lines = lines;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Range getLines() {
        return lines;
    }

    public void setLines(Range lines) {
        this.lines = lines;
    }

    public Range getColumns() {
        return columns;
    }

    public void setColumns(Range columns) {
        this.columns = columns;
    }

    @Override
    public String toString() {
        return "Region{" +
                "file='" + file + '\'' +
                ", lines=" + lines +
                ", columns=" + columns +
                '}';
    }
}
