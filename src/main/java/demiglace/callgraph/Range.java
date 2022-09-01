package demiglace.callgraph;

public class Range {
    private int from;
    private int to;

    public Range(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public int getStart() {
        return from;
    }

    public int getEnd() {
        return to;
    }

    @Override
    public String toString() {
        return "Range{" +
                "from=" + from +
                ", to=" + to +
                '}';
    }
}
