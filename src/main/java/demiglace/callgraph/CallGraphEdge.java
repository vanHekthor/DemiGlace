package demiglace.callgraph;

public class CallGraphEdge {
    private String type;
    private String label;
    private Region from;
    private Region to;
    private int value;

    public CallGraphEdge(String type, String label, Region from, Region to, int value) {
        this.type = type;
        this.label = label;
        this.from = from;
        this.to = to;
        this.value = value;
    }

    @Override
    public String toString() {
        return "CallGraphEdge{" +
                "type='" + type + '\'' +
                ", label='" + label + '\'' +
                ", from=" + from +
                ", to=" + to +
                ", value=" + value +
                '}';
    }
}
