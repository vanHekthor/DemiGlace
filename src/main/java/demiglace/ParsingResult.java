package demiglace;

import demiglace.callgraph.CallGraphEdge;

import java.util.List;


public class ParsingResult {
    List<CallGraphEdge> edges;

    public ParsingResult(List<CallGraphEdge> edges) {
        this.edges = edges;
    }

    public List<CallGraphEdge> getEdges() {
        return edges;
    }
}
