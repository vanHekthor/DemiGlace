package demiglace.callgraph;

import java.util.Objects;

public class CallGraphEdge {
    private String type;
    private String label;
    private String declaration;
    private Region from;
    private Region to;
    private int value;

    public CallGraphEdge(String type, String label, String declaration, Region from, Region to, int value) {
        this.type = type;
        this.label = label;
        this.declaration = declaration;
        this.from = from;
        this.to = to;
        this.value = value;
    }

    @Override
    public String toString() {
        return "CallGraphEdge{" +
                "type='" + type + '\'' +
                ", label='" + label + '\'' +
                ", declaration='" + declaration +
                ", from=" + from +
                ", to=" + to +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CallGraphEdge that = (CallGraphEdge) o;

        return this.from.getFile().equals(that.from.getFile()) &&
                this.to.getFile().equals(that.to.getFile()) &&
                this.from.getLines().getStart() == that.from.getLines().getStart() &&
                this.from.getLines().getEnd() == that.from.getLines().getEnd() &&
                this.from.getColumns().getStart() == that.from.getColumns().getStart() &&
                this.from.getColumns().getEnd() == that.from.getColumns().getEnd() &&
                this.to.getLines().getStart() == that.to.getLines().getStart() &&
                this.to.getLines().getEnd() == that.to.getLines().getEnd() &&
                this.to.getColumns().getStart() == that.to.getColumns().getStart() &&
                this.to.getColumns().getEnd() == that.to.getColumns().getEnd();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                from.getFile(),
                from.getLines().getStart(),
                from.getLines().getEnd(),
                from.getColumns().getStart(),
                from.getColumns().getEnd(),
                to.getFile(),
                to.getLines().getStart(),
                to.getLines().getEnd(),
                to.getColumns().getStart(),
                to.getColumns().getEnd());
    }
}